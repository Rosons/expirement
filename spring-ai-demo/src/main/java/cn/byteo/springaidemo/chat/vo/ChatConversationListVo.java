package cn.byteo.springaidemo.chat.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会话列表项（v1 / v2 共用响应）。
 * <p>v1（内存）：{@code title}、{@code updatedAt} 一般为 {@code null}；v2（库表）通常有值。</p>
 */
@Data
@Builder
public class ChatConversationListVo {

    private String conversationId;

    private String title;

    private String type;

    private LocalDateTime updatedAt;
}
