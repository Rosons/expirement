package cn.byteo.springaidemo.chat.service;

import cn.byteo.springaidemo.chat.entity.ChatConversation;
import cn.byteo.springaidemo.chat.mapper.ChatConversationMapper;
import cn.byteo.springaidemo.chat.support.ChatConversationTypeSupport;
import cn.byteo.springaidemo.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.Objects;

/**
 * 会话主数据维护（类型归属等）。
 */
@Service
@RequiredArgsConstructor
public class ChatConversationManageService {

    private final ChatConversationMapper chatConversationMapper;

    @Transactional
    public void ensureConversationType(String conversationId, String type) {
        if (!StringUtils.hasText(conversationId)) {
            throw new BusinessException("chatId 不能为空");
        }
        String normalizedConversationId = conversationId.trim();
        String normalizedType = ChatConversationTypeSupport.normalize(type);
        if (!StringUtils.hasText(normalizedType)) {
            throw new BusinessException("type 不能为空");
        }

        ChatConversation existing = chatConversationMapper.selectById(normalizedConversationId);
        if (existing == null) {
            ChatConversation conversation = new ChatConversation();
            conversation.setConversationId(normalizedConversationId);
            conversation.setType(normalizedType);
            conversation.setMetadata(Map.of());
            chatConversationMapper.insert(conversation);
            return;
        }

        if (!Objects.equals(existing.getType(), normalizedType)) {
            existing.setType(normalizedType);
            chatConversationMapper.updateById(existing);
        }
    }
}
