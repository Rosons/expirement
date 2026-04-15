package cn.byteo.springaidemo.chat.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 单条历史消息（v1 / v2 共用响应，与 Spring AI {@code Message} 类型无关）。
 * <p>v1（内存）：{@link #parts} 多为空列表，{@link #createdAt} 多为 {@code null}；v2 可有片段与入库时间。</p>
 */
@Data
@Builder
public class ChatMessageHistoryVo {

    private Long id;

    private Integer seq;

    /** USER、ASSISTANT、SYSTEM、TOOL（与 {@link cn.byteo.springaidemo.chat.enums.ChatMessageRole} 存储值一致） */
    private String role;

    /**
     * 主文本；多模态场景可空，由前端结合 {@link #parts}。
     */
    private String content;

    private List<ChatMessagePartVo> parts;

    private LocalDateTime createdAt;
}
