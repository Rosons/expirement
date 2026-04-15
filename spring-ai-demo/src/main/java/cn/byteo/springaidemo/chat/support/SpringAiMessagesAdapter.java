package cn.byteo.springaidemo.chat.support;

import cn.byteo.springaidemo.chat.vo.ChatMessageHistoryPageVo;
import cn.byteo.springaidemo.chat.vo.ChatMessageHistoryVo;
import org.springframework.ai.chat.messages.Message;

import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Spring AI {@link Message} → 与 v2 一致的 {@link ChatMessageHistoryVo} / 分页结果。
 * <p>
 * {@link org.springframework.ai.chat.memory.ChatMemoryRepository} 只能按会话一次拉全量消息，
 * 故「按页截取」只能在本类完成；持久化路径则在 SQL 里 LIMIT/OFFSET。
 * </p>
 */
public final class SpringAiMessagesAdapter {

    public static final int MAX_PAGE_SIZE = 200;

    private SpringAiMessagesAdapter() {
    }

    /** 无会话或 0 条消息时的分页壳（total=0）。 */
    public static ChatMessageHistoryPageVo emptyHistoryPage(int page, int size) {
        int s = Math.min(MAX_PAGE_SIZE, Math.max(1, size));
        int p = Math.max(1, page);
        return ChatMessageHistoryPageVo.builder()
                .records(List.of())
                .total(0)
                .size(s)
                .current(p)
                .pages(0)
                .build();
    }

    /**
     * 列表下标 i 对应会话内第 i+1 条消息（与全量顺序一致）。
     */
    public static ChatMessageHistoryVo toHistoryVo(Message message, int seq) {
        String role = message.getMessageType().name();
        String text = message.getText() != null ? message.getText() : "";
        return ChatMessageHistoryVo.builder()
                .id((long) seq)
                .seq(seq)
                .role(role)
                .content(text)
                .parts(Collections.emptyList())
                .createdAt(null)
                .build();
    }

    /**
     * 全量消息列表（时间升序）+分页参数 → {@link ChatMessageHistoryPageVo}。
     *
     * @param newestFirst {@code false}：第 1 页为最早一段；{@code true}：第 1 页为最近一段（适合聊天先出最新再向上翻）
     */
    public static ChatMessageHistoryPageVo pageHistory(List<Message> all, int page, int size, boolean newestFirst) {
        if (all == null || all.isEmpty()) {
            return emptyHistoryPage(page, size);
        }
        int p = Math.max(1, page);
        int s = Math.min(MAX_PAGE_SIZE, Math.max(1, size));
        int n = all.size();
        long total = all.size();
        long pages = (total + s - 1) / s;

        int start;
        int end;
        if (!newestFirst) {
            start = (p - 1) * s;
            end = Math.min(start + s, n);
        } else {
            end = n - (p - 1) * s;
            start = Math.max(0, end - s);
            if (end <= 0) {
                return ChatMessageHistoryPageVo.builder()
                        .records(List.of())
                        .total(total)
                        .size(s)
                        .current(p)
                        .pages(pages)
                        .build();
            }
        }

        if (!newestFirst && start >= n) {
            return ChatMessageHistoryPageVo.builder()
                    .records(List.of())
                    .total(total)
                    .size(s)
                    .current(p)
                    .pages(pages)
                    .build();
        }

        List<ChatMessageHistoryVo> records = IntStream.range(start, end)
                .mapToObj(i -> toHistoryVo(all.get(i), i + 1))
                .toList();
        return ChatMessageHistoryPageVo.builder()
                .records(records)
                .total(total)
                .size(s)
                .current(p)
                .pages(pages)
                .build();
    }
}
