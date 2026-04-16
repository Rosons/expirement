package cn.byteo.springaidemo.chat.service;

import cn.byteo.springaidemo.chat.vo.ChatConversationListVo;
import cn.byteo.springaidemo.chat.vo.ChatMessageHistoryPageVo;

import java.util.List;

/**
 * 会话列表与历史消息的查询契约；实现见 {@code cn.byteo.springaidemo.chat.service.impl} 包下的
 * {@link cn.byteo.springaidemo.chat.service.impl.MemoryChatService} 与
 * {@link cn.byteo.springaidemo.chat.service.impl.PersistentChatService}。
 */
public interface ChatService {

    List<ChatConversationListVo> listConversations(String type);

    /**
     * @param conversationId 会话 ID；空白时返回空分页（与持久化实现行为一致）
     * @param page           页码，从 1 开始
     * @param size           每页条数，上限见 {@link cn.byteo.springaidemo.chat.support.SpringAiMessagesAdapter#MAX_PAGE_SIZE}
     * @param newestFirst    {@code true} 时第 1 页为最近消息；{@code false} 时第 1 页为最早消息
     */
    ChatMessageHistoryPageVo pageHistory(String conversationId, int page, int size, boolean newestFirst);
}
