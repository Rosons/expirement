package cn.byteo.springaidemo.controller;

import java.util.ArrayList;
import java.util.List;

import cn.byteo.springaidemo.constant.FileTypeConstant;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.content.Media;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import cn.byteo.springaidemo.chat.dto.ChatHistoryQueryRequest;
import cn.byteo.springaidemo.chat.dto.ChatStreamQueryRequest;
import cn.byteo.springaidemo.chat.service.ChatConversationManageService;
import cn.byteo.springaidemo.chat.service.ChatService;
import cn.byteo.springaidemo.chat.vo.ChatConversationListVo;
import cn.byteo.springaidemo.chat.vo.ChatMessageHistoryPageVo;
import cn.byteo.springaidemo.common.web.ApiResponse;
import cn.byteo.springaidemo.util.FileExtensionMimeTypeUtils;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

/**
 * 聊天 API v2：会话与历史数据来自 {@link cn.byteo.springaidemo.chat.service.impl.PersistentChatService}；入参/出参与 v1 一致（便于前端只切换版本前缀）。
 * 流式对话仍使用内存 {@link ChatMemory}。
 */
@RestController
@RequestMapping("/v2/ai/chat")
@Slf4j
public class ChatV2Controller {

    private final ChatClient chatClient;

    private final ChatClient multiModalChatClient;

    private final ChatService chatService;

    private final ChatConversationManageService chatConversationManageService;

    public ChatV2Controller(@Qualifier("persistentMemoryChatClient") ChatClient chatClient,
                            @Qualifier("multiModalChatClient") ChatClient multiModalChatClient,
                            @Qualifier("persistentChatService") ChatService chatService,
                            ChatConversationManageService chatConversationManageService) {
        this.chatClient = chatClient;
        this.multiModalChatClient = multiModalChatClient;
        this.chatService = chatService;
        this.chatConversationManageService = chatConversationManageService;
    }

    @RequestMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chat(@ModelAttribute ChatStreamQueryRequest query) {
        chatConversationManageService.ensureConversationType(query.getChatId(), query.requireType());
        List<MultipartFile> files = query.getFiles();
        if (files == null || files.isEmpty()) {
            // 普通模型文本对话
            return textChat(query);
        }
        // 多模态模型对话
        return multiModalChat(query);

    }

    private Flux<String> multiModalChat(ChatStreamQueryRequest query) {
        // 组装media
        List<MultipartFile> files = query.getFiles();
        List<Media> mediaList = new ArrayList<>();
        for (MultipartFile file : files) {
            // 获取mime类型
            String contentType = file.getContentType();
            if (!StringUtils.hasText(contentType)) {
                contentType = FileExtensionMimeTypeUtils.getMimeTypeFromExtension(file.getOriginalFilename());
            }
            // 转换媒体类型
            MediaType mediaType = MediaType.valueOf(contentType);
            mediaList.add(Media.builder()
                    .mimeType(mediaType)
                    .data(file.getResource())
                    .name(file.getOriginalFilename())
                    .build());
        }
        Media[] medias = mediaList.toArray(Media[]::new);
        // 获取上传的文件名称（前端限制了一次消息只能上传同一类型的文件）
        String originalFilename = files.get(0).getOriginalFilename();
        String fileType = FileExtensionMimeTypeUtils.normalizeExtension(originalFilename);
        // 文档文件
        if (FileTypeConstant.ALLOWED_FILE_EXTENSIONS.contains(fileType)) {
            // 文档文件，直接使用纯文本聊天模型进行分析回答，不使用真正的多模态模型进行分析，节省token
            // 纯文本聊天客户端中，文档Advisor(DocumentMediaInliningAdvisor)会将附件内容解析成文本，拼接到用户提示词中。
            return chatClient.prompt()
                    .advisors(p -> p.param(ChatMemory.CONVERSATION_ID, query.getChatId()))
                    .user(p -> p.text(query.getMessage()).media(medias))
                    .stream()
                    .content();
        }
        return multiModalChatClient.prompt()
                .advisors(p -> p.param(ChatMemory.CONVERSATION_ID, query.getChatId()))
                .user(p -> p.text(query.getMessage()).media(medias))
                .stream()
                .content();
    }

    private Flux<String> textChat(ChatStreamQueryRequest query) {
        return chatClient.prompt()
                .advisors(p -> p.param(ChatMemory.CONVERSATION_ID, query.getChatId()))
                .user(query.getMessage())
                .stream()
                .content();
    }

    @GetMapping("/conversations")
    public ApiResponse<List<ChatConversationListVo>> getConversations(@RequestParam(value = "type") String type) {
        return ApiResponse.ok(chatService.listConversations(type));
    }

    /**
     * 分页查询会话内消息；每页 {@code records} 内按 {@code seq} 升序。
     * 支持 {@code order=desc}，见 {@link ChatHistoryQueryRequest}。
     */
    @GetMapping("/history")
    public ApiResponse<ChatMessageHistoryPageVo> getChatHistory(@ModelAttribute ChatHistoryQueryRequest query) {
        return ApiResponse.ok(chatService.pageHistory(
                query.requireChatId(),
                query.resolvePage(),
                query.resolveSize(),
                query.resolveNewestFirst()));
    }
}
