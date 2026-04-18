package cn.byteo.springaidemo.chat.advisor;

import cn.byteo.springaidemo.constant.FileTypeConstant;
import cn.byteo.springaidemo.exception.BusinessException;
import cn.byteo.springaidemo.util.FileExtensionMimeTypeUtils;
import cn.byteo.springaidemo.util.NormalFileUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.content.Media;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 将「文档类」{@link Media}（如 PDF、纯文本）在发往模型前解析为文本，拼入用户消息 {@code text}，
 * 并从 {@link UserMessage#getMedia()} 中移除对应项，避免经 OpenAI 兼容层以 {@code InputFile} 发送时被上游拒绝。
 * <p>
 * 需在 {@link org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor} 之后执行（更高
 * {@link #getOrder()}），以便处理已合并历史后的<strong>最后一条</strong>用户消息（与
 * {@link org.springframework.ai.chat.prompt.Prompt#augmentUserMessage} 行为一致）。
 * </p>
 */
@Slf4j
@Component
public final class DocumentMediaInliningAdvisor implements BaseAdvisor {

    /** 高于 {@link org.springframework.ai.chat.client.advisor.api.Advisor#DEFAULT_CHAT_MEMORY_PRECEDENCE_ORDER}，保证在记忆 Advisor 之后执行 */
    private static final int ORDER_AFTER_CHAT_MEMORY = 0;

    /** 文档抽取内容块上沿分隔线（与用户原文区分） */
    private static final String DOCUMENT_BLOCK_START = "---------------------------------------------------------------------";
    /** 文档抽取内容块下沿分隔线 */
    private static final String DOCUMENT_BLOCK_END = "---------------------------------------------------------------------";

    @Override
    public ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain) {
        return chatClientRequest.mutate()
                .prompt(chatClientRequest.prompt().augmentUserMessage(this::inlineDocumentMedia))
                .build();
    }

    @Override
    public ChatClientResponse after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain) {
        return chatClientResponse;
    }

    @Override
    public int getOrder() {
        return ORDER_AFTER_CHAT_MEMORY;
    }

    private UserMessage inlineDocumentMedia(UserMessage userMessage) {
        List<Media> media = userMessage.getMedia();
        if (media.isEmpty()) {
            return userMessage;
        }
        List<Media> kept = new ArrayList<>();
        StringBuilder docBlocks = new StringBuilder();
        for (Media m : media) {
            if (isDocumentToInline(m)) {
                String block = extractDocumentText(m);
                if (!docBlocks.isEmpty()) {
                    docBlocks.append("\n\n");
                }
                docBlocks.append(block);
            } else {
                kept.add(m);
            }
        }
        if (docBlocks.isEmpty()) {
            return userMessage;
        }
        String original = userMessage.getText().trim();
        String wrappedDocs = DOCUMENT_BLOCK_START + "\n" + docBlocks + "\n" + DOCUMENT_BLOCK_END;
        // 文档抽取内容在上，用户原话在下（docBlocks 非空时 wrappedDocs 必有字符，无需再兜底默认句）
        String combined = original.isEmpty() ? wrappedDocs : wrappedDocs + "\n\n" + original;
        if (kept.isEmpty()) {
            return UserMessage.builder().text(combined).build();
        }
        return UserMessage.builder().text(combined).media(kept).build();
    }

    private static boolean isDocumentToInline(Media media) {
        String fileName = media.getName();
        if (fileName == null) {
            return false;
        }
        String fileType = FileExtensionMimeTypeUtils.normalizeExtension(fileName);
        return FileTypeConstant.ALLOWED_FILE_EXTENSIONS.contains(fileType);
    }

    private static String extractDocumentText(Media media) {
        byte[] dataAsByteArray = media.getDataAsByteArray();
        ByteArrayResource resource = new ByteArrayResource(dataAsByteArray);
        String fileName = media.getName();
        String fileType = FileExtensionMimeTypeUtils.normalizeExtension(fileName);
        try {
            return NormalFileUtils.readString(resource, fileType);
        } catch (IOException e) {
            throw new BusinessException("处理文档文件消息时发生异常：" + e.getMessage());
        }
    }

    private static Resource resolveMediaResource(Media media) {
        Object data = media.getData();
        if (data instanceof Resource r) {
            return r;
        }
        if (data instanceof byte[] bytes) {
            return new ByteArrayResource(bytes);
        }
        throw new IllegalStateException("Media 数据无法作为 Resource 读取: " + (data == null ? "null" : data.getClass()));
    }

    private static String extractPdfBlock(String filename, Resource data) {
        try {
            PagePdfDocumentReader reader = new PagePdfDocumentReader(data);
            List<Document> docs = reader.read();
            StringBuilder sb = new StringBuilder();
            for (Document doc : docs) {
                String t = doc.getText();
                if (StringUtils.hasText(t)) {
                    if (sb.length() > 0) {
                        sb.append("\n\n");
                    }
                    sb.append(t);
                }
            }
            String body = sb.toString();
            if (!StringUtils.hasText(body)) {
                body = "（未能从该 PDF 中提取到可选中文本，若为扫描件请先 OCR。）";
            }
            return "【PDF：" + filename + "】\n" + body;
        } catch (RuntimeException e) {
            log.warn("PDF 解析失败: {}", filename, e);
            throw new IllegalStateException("PDF 解析失败: " + filename, e);
        }
    }

    private static String extractPlainTextBlock(String filename, Resource data) {
        try (InputStream in = data.getInputStream()) {
            String body = StreamUtils.copyToString(in, StandardCharsets.UTF_8);
            if (!StringUtils.hasText(body)) {
                body = "（文件为空）";
            }
            return "【文本：" + filename + "】\n" + body;
        } catch (IOException e) {
            log.warn("文本文件读取失败: {}", filename, e);
            throw new UncheckedIOException("文本文件读取失败: " + filename, e);
        }
    }
}
