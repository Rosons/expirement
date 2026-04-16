package cn.byteo.springaidemo.chat.vo;

import lombok.Builder;
import lombok.Data;

/**
 * File summary when listing by conversation.
 */
@Data
@Builder
public class ChatFileListVo {

    private String fileId;

    private String conversationId;

    private String originalFilename;

    private String contentType;

    private Long fileSize;

    /** Created time as ISO-8601 string. */
    private String createdAt;
}
