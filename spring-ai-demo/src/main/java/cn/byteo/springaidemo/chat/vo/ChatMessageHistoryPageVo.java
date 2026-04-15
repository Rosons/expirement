package cn.byteo.springaidemo.chat.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 会话历史分页结果（v1 / v2 共用响应）。
 */
@Data
@Builder
public class ChatMessageHistoryPageVo {

    private List<ChatMessageHistoryVo> records;

    /** 总条数 */
    private long total;

    /** 每页条数 */
    private long size;

    /** 当前页，从 1 开始 */
    private long current;

    /** 总页数 */
    private long pages;
}
