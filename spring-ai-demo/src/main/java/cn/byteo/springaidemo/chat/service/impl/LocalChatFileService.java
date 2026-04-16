package cn.byteo.springaidemo.chat.service.impl;

import cn.byteo.springaidemo.chat.entity.ChatFile;
import cn.byteo.springaidemo.chat.mapper.ChatFileMapper;
import cn.byteo.springaidemo.chat.service.ChatConversationManageService;
import cn.byteo.springaidemo.chat.service.ChatFileService;
import cn.byteo.springaidemo.chat.vo.ChatFileListVo;
import cn.byteo.springaidemo.chat.vo.ChatFileUploadVo;
import cn.byteo.springaidemo.exception.BusinessException;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 本地磁盘文件存储实现。
 */
@Service
@RequiredArgsConstructor
public class LocalChatFileService implements ChatFileService {

    private final ChatFileMapper chatFileMapper;

    private final ChatConversationManageService chatConversationManageService;

    @Value("${app.file-storage.root-dir:./data/chat-files}")
    private String storageRootDir;

    @Override
    @Transactional
    public ChatFileUploadVo upload(String conversationId, Resource resource, String ensureConversationType) {
        String normalizedConversationId = normalizeConversationId(conversationId);
        if (!StringUtils.hasText(normalizedConversationId)) {
            throw new BusinessException("conversationId 不能为空");
        }
        if (StringUtils.hasText(ensureConversationType)) {
            chatConversationManageService.ensureConversationType(normalizedConversationId, ensureConversationType);
        }
        if (resource == null) {
            throw new BusinessException("上传文件不能为空");
        }
        String originalName = StringUtils.hasText(resource.getFilename())
                ? resource.getFilename().trim()
                : "unknown";

        String extension = resolveExtension(originalName);
        String fileId = UUID.randomUUID().toString().replace("-", "");
        String storedFilename = fileId + extension;

        Path root = resolveStorageRoot();
        String safeConversationId = sanitizePathSegment(normalizedConversationId);
        Path targetDir = root.resolve(safeConversationId).normalize();
        Path targetFile = targetDir.resolve(storedFilename).normalize();

        if (!targetFile.startsWith(root)) {
            throw new BusinessException("非法存储路径");
        }

        try {
            Files.createDirectories(targetDir);
            try (InputStream inputStream = resource.getInputStream()) {
                Files.copy(inputStream, targetFile);
            }
        } catch (IOException e) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "文件写入失败");
        }

        ChatFile entity = new ChatFile();
        entity.setFileId(fileId);
        entity.setConversationId(normalizedConversationId);
        entity.setOriginalFilename(originalName);
        entity.setStoredFilename(storedFilename);
        entity.setStoragePath(root.relativize(targetFile).toString().replace('\\', '/'));
        entity.setContentType(resolveContentType(resource, targetFile));
        entity.setFileSize(resolveFileSize(resource, targetFile));
        entity.setMetadata(Map.of());
        chatFileMapper.insert(entity);

        return ChatFileUploadVo.builder()
                .fileId(fileId)
                .conversationId(normalizedConversationId)
                .originalFilename(originalName)
                .contentType(entity.getContentType())
                .fileSize(entity.getFileSize())
                .downloadUrl("/ai/files/download/" + fileId)
                .build();
    }

    @Override
    public ChatFile getChatFile(String fileId) {
        String normalizedFileId = StringUtils.hasText(fileId) ? fileId.trim() : "";
        if (!StringUtils.hasText(normalizedFileId)) {
            throw new BusinessException("fileId 不能为空");
        }
        ChatFile file = chatFileMapper.selectById(normalizedFileId);
        if (file == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "文件不存在");
        }
        return file;
    }

    @Override
    public Resource loadResource(ChatFile file) {
        Path root = resolveStorageRoot();
        Path path = root.resolve(file.getStoragePath()).normalize();
        if (!path.startsWith(root)) {
            throw new BusinessException("非法文件路径");
        }
        Resource resource = new PathResource(path);
        if (!resource.exists()) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "文件不存在或已被移除");
        }
        return resource;
    }

    @Override
    public List<ChatFileListVo> listByConversationId(String conversationId) {
        String normalizedConversationId = normalizeConversationId(conversationId);
        if (!StringUtils.hasText(normalizedConversationId)) {
            throw new BusinessException("conversationId 不能为空");
        }
        List<ChatFile> rows = chatFileMapper.selectList(
                Wrappers.<ChatFile>lambdaQuery()
                        .eq(ChatFile::getConversationId, normalizedConversationId)
                        .orderByDesc(ChatFile::getCreatedAt));
        return rows.stream().map(LocalChatFileService::toListVo).toList();
    }

    @Override
    @Transactional
    public void deleteFile(String fileId, String conversationId) {
        ChatFile chatFile = getChatFile(fileId);
        String norm = normalizeConversationId(conversationId);
        if (!StringUtils.hasText(norm)) {
            throw new BusinessException("conversationId 不能为空");
        }
        if (!norm.equals(chatFile.getConversationId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "文件不属于该会话");
        }
        Path root = resolveStorageRoot();
        Path path = root.resolve(chatFile.getStoragePath()).normalize();
        if (!path.startsWith(root)) {
            throw new BusinessException("非法文件路径");
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "删除磁盘文件失败");
        }
        chatFileMapper.deleteById(fileId);
    }

    private static ChatFileListVo toListVo(ChatFile entity) {
        return ChatFileListVo.builder()
                .fileId(entity.getFileId())
                .conversationId(entity.getConversationId())
                .originalFilename(entity.getOriginalFilename())
                .contentType(entity.getContentType())
                .fileSize(entity.getFileSize())
                .createdAt(entity.getCreatedAt() != null
                        ? entity.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                        : null)
                .build();
    }

    private static String normalizeConversationId(String conversationId) {
        return StringUtils.hasText(conversationId) ? conversationId.trim() : "";
    }

    private static String resolveExtension(String filename) {
        int idx = filename.lastIndexOf('.');
        if (idx < 0 || idx == filename.length() - 1) {
            return "";
        }
        return filename.substring(idx);
    }

    private static String resolveContentType(Resource resource, Path targetFile) {
        if (resource instanceof org.springframework.core.io.FileSystemResource fileSystemResource) {
            try {
                String detected = Files.probeContentType(fileSystemResource.getFile().toPath());
                if (StringUtils.hasText(detected)) {
                    return detected;
                }
            } catch (IOException ignore) {
            }
        }
        try {
            String detected = Files.probeContentType(targetFile);
            if (StringUtils.hasText(detected)) {
                return detected;
            }
        } catch (IOException ignore) {
        }
        return "application/octet-stream";
    }

    private static long resolveFileSize(Resource resource, Path targetFile) {
        try {
            long resourceSize = resource.contentLength();
            if (resourceSize >= 0) {
                return resourceSize;
            }
        } catch (IOException ignore) {
        }
        try {
            return Files.size(targetFile);
        } catch (IOException e) {
            return 0L;
        }
    }

    private static String sanitizePathSegment(String value) {
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return "_";
        }
        String safe = trimmed.replaceAll("[^a-zA-Z0-9._-]", "_");
        return safe.isEmpty() ? "_" : safe;
    }

    private Path resolveStorageRoot() {
        try {
            Path root = Paths.get(storageRootDir).toAbsolutePath().normalize();
            Files.createDirectories(root);
            return root;
        } catch (IOException e) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "初始化存储目录失败");
        }
    }
}
