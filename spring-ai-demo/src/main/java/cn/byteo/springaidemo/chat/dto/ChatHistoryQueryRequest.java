package cn.byteo.springaidemo.chat.dto;

import cn.byteo.springaidemo.chat.support.SpringAiMessagesAdapter;
import lombok.Data;
import org.springframework.util.StringUtils;

/**
 * v1 / v2 共用的历史查询入参（GET Query 绑定）。
 * <p>字段与查询参数同名：{@code chatId}、{@code page}、{@code size}、{@code order}。</p>
 * <p>{@code order=asc}（默认）：第 1 页为会话内最早的消息；{@code order=desc}：第 1 页为最近的消息，
 * 适合先渲染最新再向上翻更旧；每页内 {@code records} 始终按 {@code seq} 升序（时间从早到晚）。</p>
 */
@Data
public class ChatHistoryQueryRequest {

    private String chatId;

    /** 页码，从 1 开始；缺省或非法时按 1 处理 */
    private Integer page;

    /** 每页条数；缺省 20，上限 {@link SpringAiMessagesAdapter#MAX_PAGE_SIZE} */
    private Integer size;

    /**
     * 分页方向：{@code asc} 从旧往新分页；{@code desc} 从新往旧分页（第 1 页为最近一段）。
     * 缺省或非 {@code desc} 均视为 {@code asc}。
     */
    private String order;

    public String requireChatId() {
        return StringUtils.hasText(chatId) ? chatId.trim() : "";
    }

    public int resolvePage() {
        return (page == null || page < 1) ? 1 : page;
    }

    public int resolveSize() {
        int s = (size == null || size < 1) ? 20 : size;
        return Math.min(SpringAiMessagesAdapter.MAX_PAGE_SIZE, s);
    }

    /** {@code true} 表示 {@code order=desc}，第 1 页为最近消息。 */
    public boolean resolveNewestFirst() {
        return StringUtils.hasText(order) && "desc".equalsIgnoreCase(order.trim());
    }
}
