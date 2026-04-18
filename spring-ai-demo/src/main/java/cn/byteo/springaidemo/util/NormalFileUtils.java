package cn.byteo.springaidemo.util;

import cn.byteo.springaidemo.constant.FileTypeConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.core.io.Resource;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * <p>这里描述类的主要功能</p>
 *
 * @author Roson
 * @file DocumentUtils
 * @since 2026/4/18
 */
@Slf4j
public final class NormalFileUtils {
    /**
     * 读取文件内容，返回Document列表
     */
    public static List<Document> readDocuments(Resource resource, String fileType) throws IOException {
        List<Document> documents;

        if (FileTypeConstant.isPdf(fileType)) {
            // 解析pdf
            log.info("使用 pdf-reader 读取 PDF 文档: {}", fileType);
            PagePdfDocumentReader reader = new PagePdfDocumentReader(resource,
                    PdfDocumentReaderConfig.builder()
                            .withPageExtractedTextFormatter(ExtractedTextFormatter.defaults())
                            .build());
            documents = reader.read();

        } else if (FileTypeConstant.isOfficeDocument(fileType)) {
            // Office 文档读取（使用 Tika）
            log.info("使用 Tika 读取 Office 文档: {}", fileType);
            TikaDocumentReader tikaReader = new TikaDocumentReader(resource);
            documents = tikaReader.get();

        } else if (FileTypeConstant.isPlainText(fileType)) {
            // 纯文本文档读取
            log.info("纯文本读取文档: {}", fileType);
            String content = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            documents = List.of(new Document(content));

        } else {
            throw new IllegalArgumentException("不支持的文件类型: " + fileType);
        }
        return documents;
    }

    /**
     * 读取文件的文本内容
     */
    public static String readString(Resource resource, String fileType) throws IOException {
        List<Document> documents = readDocuments(resource, fileType);
        StringBuilder sb = new StringBuilder();
        for (Document document : documents) {
            if (!sb.isEmpty()) {
                sb.append("\n");
            }
            sb.append(document.getText());
        }
        return sb.toString();
    }
}
