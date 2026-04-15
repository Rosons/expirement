package cn.byteo.springaidemo.chat.service.impl;

import cn.byteo.springaidemo.chat.service.ChatService;
import cn.byteo.springaidemo.chat.support.SpringAiMessagesAdapter;
import cn.byteo.springaidemo.chat.vo.ChatConversationListVo;
import cn.byteo.springaidemo.chat.vo.ChatMessageHistoryPageVo;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * v1：会话与历史来自 Spring AI 内存仓库；历史分页逻辑见 {@link SpringAiMessagesAdapter#pageHistory}。
 */
@Service("inMemoryChatService")
public class MemoryChatService implements ChatService {

    private final ChatMemoryRepository chatMemoryRepository;

    public MemoryChatService(@Qualifier("simpleChatMemoryRepository") ChatMemoryRepository chatMemoryRepository) {
        this.chatMemoryRepository = chatMemoryRepository;
    }

    @Override
    public List<ChatConversationListVo> listConversations() {
        return chatMemoryRepository.findConversationIds().stream()
                .map(id -> ChatConversationListVo.builder()
                        .conversationId(id)
                        .title(null)
                        .updatedAt(null)
                        .build())
                .toList();
    }

    @Override
    public ChatMessageHistoryPageVo pageHistory(String conversationId, int page, int size, boolean newestFirst) {
        if (!StringUtils.hasText(conversationId)) {
            return SpringAiMessagesAdapter.emptyHistoryPage(page, size);
        }
        // 仓库无分页 API，此处拿到全量后再分页（与 PersistentChatService 对外契约一致）
        return SpringAiMessagesAdapter.pageHistory(
                chatMemoryRepository.findByConversationId(conversationId.trim()),
                page,
                size,
                newestFirst);
    }
}
