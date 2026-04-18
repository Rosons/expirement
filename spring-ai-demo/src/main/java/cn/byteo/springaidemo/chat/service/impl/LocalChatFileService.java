package cn.byteo.springaidemo.chat.service.impl;

import cn.byteo.springaidemo.chat.entity.ChatFile;
import cn.byteo.springaidemo.chat.enums.ChatFileType;
import cn.byteo.springaidemo.chat.mapper.ChatFileMapper;
import cn.byteo.springaidemo.chat.service.ChatConversationManageService;
import cn.byteo.springaidemo.chat.service.ChatFileService;
import cn.byteo.springaidemo.chat.vo.ChatFileListVo;
import cn.byteo.springaidemo.chat.vo.ChatFileUploadVo;
import cn.byteo.springaidemo.constant.FileTypeConstant;
import cn.byteo.springaidemo.constant.SystemConstant;
import cn.byteo.springaidemo.exception.BusinessException;
import cn.byteo.springaidemo.util.FileExtensionMimeTypeUtils;
import cn.byteo.springaidemo.util.NormalFileUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
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

    private static final Logger log = LoggerFactory.getLogger(LocalChatFileService.class);

    private static final String VECTOR_STORE_DATA_FILENAME = "simple-vector-store.json";

    private static final String VECTOR_STORE_DIR_NAME = "vector-store";

    private final ChatFileMapper chatFileMapper;

    private final ChatConversationManageService chatConversationManageService;

    private final VectorStore vectorStore;

    @Value("${app.file-storage.root-dir:./data/chat-files}")
    private String storageRootDir;

    @Value("${app.vector-store.delete-search-top-k:1000}")
    private int vectorDeleteSearchTopK;

    @Value("${app.vector-store.persist-root-dir:./data/vector-store}")
    private String vectorStorePersistRootDir;

    @Value("${app.vector-store.splitter.chunk-size:500}")
    private int splitterChunkSize;

    @Value("${app.vector-store.splitter.min-chunk-size-chars:100}")
    private int splitterMinChunkSizeChars;

    @Value("${app.vector-store.splitter.min-chunk-length-to-embed:30}")
    private int splitterMinChunkLengthToEmbed;

    @Value("${app.vector-store.splitter.max-num-chunks:10000}")
    private int splitterMaxNumChunks;

    @Value("${app.vector-store.splitter.keep-separator:true}")
    private boolean splitterKeepSeparator;

    @Override
    @Transactional
    public ChatFileUploadVo upload(String conversationId, String ensureConversationType,
                                   Resource resource, boolean isVectorStore) {
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
        // 如果需要向量化存储，需要判断文件类型是否允许上传，目前只支持特定文件类型
        if (isVectorStore && !FileTypeConstant.ALLOWED_FILE_EXTENSIONS.contains(extension)) {
            throw new BusinessException("不支持的文件类型");
        }

        String fileId = UUID.randomUUID().toString().replace("-", "");
        String storedFilename = fileId + SystemConstant.DOT + extension;

        Path root = resolveStorageRoot();
        String safeConversationId = sanitizePathSegment(normalizedConversationId);
        // 向量化存储的文件，添加个目录标识
        Path targetDir = isVectorStore ? root.resolve(safeConversationId)
                .resolve(VECTOR_STORE_DIR_NAME).normalize() : root.resolve(safeConversationId).normalize();
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
        ChatFileType chatFileType = isVectorStore ? ChatFileType.KNOWLEDGE : ChatFileType.NORMAL;
        entity.setFileType(chatFileType);
        // 存储的相对路径
        String storagePath = root.relativize(targetFile).toString().replace('\\', '/');
        entity.setStoragePath(storagePath);
        // 统一通过工具获取文件mimeType
        entity.setContentType(FileExtensionMimeTypeUtils.getMimeTypeFromExtension(originalName));
        entity.setFileSize(resolveFileSize(resource, targetFile));
        entity.setMetadata(Map.of());
        chatFileMapper.insert(entity);

        // 向量化存储文件
        if (isVectorStore) {
            // 读取文件内容并分片
            List<Document> documents;
            try {
                documents = readAndSplitDocument(resource, extension);
            } catch (IOException e) {
                throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "解析并切片文件失败");
            }
            // 添加自定义属性
            for (Document document : documents) {
                Map<String, Object> metadata = document.getMetadata();
                metadata.put(SystemConstant.VECTOR_CHAT_ID, conversationId);
                metadata.put(SystemConstant.VECTOR_FILE_ID, fileId);
            }
            // 文件向量化存储
            vectorStore.add(documents);
        }

        return ChatFileUploadVo.builder()
                .fileId(fileId)
                .conversationId(normalizedConversationId)
                .originalFilename(originalName)
                .storagePath(storagePath)
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
    public List<ChatFileListVo> listByConversationId(String conversationId, ChatFileType chatFileType) {
        String normalizedConversationId = normalizeConversationId(conversationId);
        if (!StringUtils.hasText(normalizedConversationId)) {
            throw new BusinessException("conversationId 不能为空");
        }
        List<ChatFile> rows = chatFileMapper.selectList(
                Wrappers.<ChatFile>lambdaQuery()
                        .eq(ChatFile::getConversationId, normalizedConversationId)
                        .eq(ChatFile::getFileType, chatFileType)
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
        // SimpleVectorStore 不支持按过滤条件直接删除，先查询命中记录再按文档 ID 删除。
        int deleteSearchTopK = Math.max(1, vectorDeleteSearchTopK);
        List<String> documentIds = vectorStore.similaritySearch(SearchRequest.builder()
                        .query(fileId)
                        .topK(deleteSearchTopK)
                        .filterExpression(SystemConstant.VECTOR_FILE_ID + " == '" + fileId + "'")
                        .build())
                .stream()
                .map(Document::getId)
                .filter(StringUtils::hasText)
                .toList();
        if (!documentIds.isEmpty()) {
            vectorStore.delete(documentIds);
        }
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
        return filename.substring(idx + 1);
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

    @PostConstruct
    public void loadVectorData() {
        if (!(vectorStore instanceof SimpleVectorStore simpleVectorStore)) {
            log.info("当前 VectorStore 类型为 [{}]，跳过本地向量持久化加载", vectorStore.getClass().getSimpleName());
            return;
        }
        Path dataFile = resolveVectorStoreDataFile();
        if (!Files.exists(dataFile)) {
            log.info("未找到向量持久化文件，跳过加载：{}", dataFile);
            return;
        }
        try {
            simpleVectorStore.load(dataFile.toFile());
            log.info("向量持久化数据加载完成：{}", dataFile);
        } catch (Exception e) {
            log.error("加载向量持久化数据失败：{}", dataFile, e);
        }
    }

    @PreDestroy
    public void saveVectorData() {
        if (!(vectorStore instanceof SimpleVectorStore simpleVectorStore)) {
            return;
        }
        Path dataFile = resolveVectorStoreDataFile();
        try {
            simpleVectorStore.save(dataFile.toFile());
            log.info("向量持久化数据保存完成：{}", dataFile);
        } catch (Exception e) {
            log.error("保存向量持久化数据失败：{}", dataFile, e);
        }
    }

    private Path resolveVectorStoreDataFile() {
        try {
            Path root = Paths.get(vectorStorePersistRootDir).toAbsolutePath().normalize();
            Files.createDirectories(root);
            return root.resolve(VECTOR_STORE_DATA_FILENAME).normalize();
        } catch (IOException e) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "初始化向量持久化目录失败");
        }
    }

    /**
     * 读取并分块文档
     */
    private List<Document> readAndSplitDocument(Resource resource, String fileType) throws IOException {
        // 读取文件内容
        List<Document> documents = NormalFileUtils.readDocuments(resource, fileType);
        // 文本分块（使用配置的分块参数）
        log.info("开始文档分片: fileType={}, sourceDocumentCount={}, chunkSize={}, minChunkSizeChars={}, minChunkLengthToEmbed={}, maxNumChunks={}, keepSeparator={}",
                fileType, documents.size(), splitterChunkSize, splitterMinChunkSizeChars, splitterMinChunkLengthToEmbed, splitterMaxNumChunks, splitterKeepSeparator);
        TokenTextSplitter tokenTextSplitter = TokenTextSplitter.builder()
                .withChunkSize(Math.max(1, splitterChunkSize))
                .withMinChunkSizeChars(Math.max(1, splitterMinChunkSizeChars))
                .withMinChunkLengthToEmbed(Math.max(1, splitterMinChunkLengthToEmbed))
                .withMaxNumChunks(Math.max(1, splitterMaxNumChunks))
                .withKeepSeparator(splitterKeepSeparator)
                .build();
        List<Document> destDocuments = tokenTextSplitter.apply(documents);
        log.info("完成文档分片: fileType={}, sourceDocumentCount={}, chunkDocumentCount={}",
                fileType, documents.size(), destDocuments.size());
        return destDocuments;
    }
}
