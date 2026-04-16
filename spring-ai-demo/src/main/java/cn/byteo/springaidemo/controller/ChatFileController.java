package cn.byteo.springaidemo.controller;

import cn.byteo.springaidemo.chat.entity.ChatFile;
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
     * 按会话列出已上传文件（不含已逻辑删除记录）。
     */
    @GetMapping(params = "conversationId")
    public ApiResponse<List<ChatFileListVo>> listByConversation(@RequestParam(value = "conversationId") String conversationId) {
        return ApiResponse.ok(chatFileService.listByConversationId(conversationId));
    }

    @PostMapping("/upload")
    public ApiResponse<ChatFileUploadVo> upload(@RequestParam(value = "conversationId") String conversationId,
                                                @RequestParam(value = "type", required = false) String type,
                                                @RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(chatFileService.upload(conversationId, file.getResource(), type));
    }

    /** 下载已存储文件（二进制流）。 */
    @GetMapping("/download/{fileId}")
    public ResponseEntity<Resource> download(@PathVariable(value = "fileId") String fileId) {
        ChatFile chatFile = chatFileService.getChatFile(fileId);
        Resource resource = chatFileService.loadResource(chatFile);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(resolveFilename(chatFile), StandardCharsets.UTF_8)
                .build();
        try {
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .contentLength(resource.contentLength())
                    .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                    .body(resource);
        } catch (IOException e) {
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                    .body(resource);
        }
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
