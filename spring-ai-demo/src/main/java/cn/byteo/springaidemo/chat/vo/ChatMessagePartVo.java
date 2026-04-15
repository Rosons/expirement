package cn.byteo.springaidemo.chat.vo;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * 消息片段（多模态等）
 */
@Data
@Builder
public class ChatMessagePartVo {

    private Integer partIndex;

    private String partType;

    private String contentText;

    private String mediaUrl;

    private String mimeType;

    private Map<String, Object> payload;
}
