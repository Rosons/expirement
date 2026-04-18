package cn.byteo.springaidemo.chat.vo;

import lombok.Builder;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * 文件上传结果。
 */
@Data
@Builder
public class ChatFileUploadVo {

    private String fileId;

    private String conversationId;

    private String originalFilename;

    private String storagePath;

    private String contentType;

    private Long fileSize;

    private String downloadUrl;

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("fileId", fileId);
        map.put("conversationId", conversationId);
        map.put("originalFilename", originalFilename);
        map.put("storagePath", storagePath);
        map.put("contentType", contentType);
        map.put("fileSize", fileSize);
        map.put("downloadUrl", downloadUrl);
        return map;
    }
}
