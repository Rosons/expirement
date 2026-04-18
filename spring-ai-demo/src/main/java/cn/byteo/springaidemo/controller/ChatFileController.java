package cn.byteo.springaidemo.controller;

import cn.byteo.springaidemo.chat.entity.ChatFile;
import cn.byteo.springaidemo.chat.enums.ChatFileType;
import cn.byteo.springaidemo.chat.service.ChatFileService;
import cn.byteo.springaidemo.chat.vo.ChatFileListVo;
import cn.byteo.springaidemo.chat.vo.ChatFileUploadVo;
import cn.byteo.springaidemo.common.web.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 聊天文件上传与下载接口。
 */
@RestController
@RequestMapping("/ai/files")
@RequiredArgsConstructor
public class ChatFileController {

    private final ChatFileService chatFileService;

    /**
     * 按会话列出已上传的知识库文件（不含已逻辑删除记录）。
     */
    @GetMapping(params = "conversationId")
    public ApiResponse<List<ChatFileListVo>> listKnowledgeByConversation(@RequestParam(value = "conversationId") String conversationId) {
        return ApiResponse.ok(chatFileService.listByConversationId(conversationId, ChatFileType.KNOWLEDGE));
    }

    /**
     * 按会话ID和类型上传文件，并将文件进行向量化存储
     */
    @PostMapping("/upload")
    public ApiResponse<ChatFileUploadVo> upload(@RequestParam(value = "conversationId") String conversationId,
                                                @RequestParam(value = "type", required = false) String type,
                                                @RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(chatFileService.upload(conversationId, type, file.getResource(), true));
    }

    /**
     * 下载或内联预览已存储文件。
     * <p>
     * {@code inline=true} 时使用 {@code Content-Disposition: inline} 与库内记录的 Content-Type，
     * 供 {@code <img>} / {@code <iframe>} 预览；默认 {@code attachment} + {@code application/octet-stream} 供「另存为」下载。
     * </p>
     */
    @GetMapping("/download/{fileId}")
    public ResponseEntity<Resource> download(
            @PathVariable(value = "fileId") String fileId,
            @RequestParam(value = "inline", defaultValue = "false") boolean inline) {
        ChatFile chatFile = chatFileService.getChatFile(fileId);
        Resource resource = chatFileService.loadResource(chatFile);
        String filename = resolveFilename(chatFile);
        ContentDisposition disposition = inline
                ? ContentDisposition.inline().filename(filename, StandardCharsets.UTF_8).build()
                : ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build();
        MediaType bodyType = inline ? resolveMediaType(chatFile) : MediaType.APPLICATION_OCTET_STREAM;
        try {
            return ResponseEntity.ok()
                    .contentType(bodyType)
                    .contentLength(resource.contentLength())
                    .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                    .body(resource);
        } catch (IOException e) {
            return ResponseEntity.ok()
                    .contentType(bodyType)
                    .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                    .body(resource);
        }
    }

    private static MediaType resolveMediaType(ChatFile chatFile) {
        if (StringUtils.hasText(chatFile.getContentType())) {
            try {
                return MediaType.parseMediaType(chatFile.getContentType().trim());
            } catch (Exception ignored) {
                // fall through
            }
        }
        return MediaType.APPLICATION_OCTET_STREAM;
    }

    /**
     * 删除文件记录并移除磁盘对象；须携带 conversationId 且与文件归属一致。
     */
    @DeleteMapping("/{fileId}")
    public ApiResponse<Void> delete(@PathVariable(value = "fileId") String fileId,
                                    @RequestParam(value = "conversationId") String conversationId) {
        chatFileService.deleteFile(fileId, conversationId);
        return ApiResponse.ok();
    }

    private static String resolveFilename(ChatFile chatFile) {
        if (StringUtils.hasText(chatFile.getOriginalFilename())) {
            return chatFile.getOriginalFilename().trim();
        }
        if (StringUtils.hasText(chatFile.getStoredFilename())) {
            return chatFile.getStoredFilename().trim();
        }
        return "download.bin";
    }
}
