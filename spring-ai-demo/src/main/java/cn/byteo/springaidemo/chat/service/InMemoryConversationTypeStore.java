package cn.byteo.springaidemo.chat.service;

import cn.byteo.springaidemo.chat.support.ChatConversationTypeSupport;
import cn.byteo.springaidemo.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * v1 内存会话的类型归属管理。
 */
@Service
public class InMemoryConversationTypeStore {

    private final ConcurrentMap<String, String> conversationTypeMap = new ConcurrentHashMap<>();

    public void ensureConversationType(String conversationId, String type) {
        String normalizedConversationId = normalizeConversationId(conversationId);
        String normalizedType = requireType(type);
        conversationTypeMap.put(normalizedConversationId, normalizedType);
    }

    public String requireType(String type) {
        String normalizedType = ChatConversationTypeSupport.normalize(type);
        if (!StringUtils.hasText(normalizedType)) {
            throw new BusinessException("type 不能为空");
        }
        return normalizedType;
    }

    public boolean matchesType(String conversationId, String type) {
        String normalizedConversationId = normalizeConversationId(conversationId);
        String normalizedType = requireType(type);
        String savedType = conversationTypeMap.get(normalizedConversationId);
        return normalizedType.equals(savedType);
    }

    private static String normalizeConversationId(String conversationId) {
        if (!StringUtils.hasText(conversationId)) {
            throw new BusinessException("chatId 不能为空");
        }
        return conversationId.trim();
    }
}
