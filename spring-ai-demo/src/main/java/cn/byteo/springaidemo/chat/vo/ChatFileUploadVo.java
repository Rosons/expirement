package cn.byteo.springaidemo.chat.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 文件上传结果。
 */
@Data
@Builder
public class ChatFileUploadVo {

    private String fileId;

    private String conversationId;

    private String originalFilename;

    private String contentType;

    private Long fileSize;

    private String downloadUrl;
}
