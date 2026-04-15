package cn.byteo.springaidemo.chat.memory;

import cn.byteo.springaidemo.chat.entity.ChatConversation;
import cn.byteo.springaidemo.chat.entity.ChatMessage;
import cn.byteo.springaidemo.chat.enums.ChatMessageRole;
import cn.byteo.springaidemo.chat.mapper.ChatConversationMapper;
import cn.byteo.springaidemo.chat.mapper.ChatMessageMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 基于数据库的会话记忆实现。
 * <p>
 * 当前版本先聚焦“纯文本聊天”主流程：
 * 1. 用户消息、助手消息、系统消息按顺序落到 {@code chat_message}；
 * 2. 读取历史时再还原为 Spring AI 的消息对象；
 * 3. 暂不处理媒体附件、工具调用链等复杂结构，优先保证主链路稳定可用。
 * </p>
 * <p>
 * 后续如需扩展，可按下面方向演进：
 * 1. 媒体消息：将图片/文件等附件落到 {@code chat_message_part}；
 * 2. 工具调用：为 {@code ToolResponseMessage}、助手工具调用记录设计专门持久化结构；
 * 3. 历史展示：按需补充消息片段、工具轨迹、附件元数据等查询能力。
 * </p>
 */
@RequiredArgsConstructor
public class PersistentChatMemory implements ChatMemory {

    private final ChatConversationMapper chatConversationMapper;
    private final ChatMessageMapper chatMessageMapper;

    @Override
    @Transactional
    public void add(String conversationId, List<Message> messages) {
        String normalizedConversationId = normalizeConversationId(conversationId);
        if (normalizedConversationId == null || CollectionUtils.isEmpty(messages)) {
            return;
        }

        List<Message> validMessages = messages.stream()
                .filter(Objects::nonNull)
                .toList();
        if (validMessages.isEmpty()) {
            return;
        }

        ensureConversationExists(normalizedConversationId);
        int nextSeq = findNextSeq(normalizedConversationId);
        for (Message message : validMessages) {
            chatMessageMapper.insert(toEntity(normalizedConversationId, nextSeq++, message));
        }
        touchConversation(normalizedConversationId);
    }

    @Override
    public List<Message> get(String conversationId) {
        String normalizedConversationId = normalizeConversationId(conversationId);
        if (normalizedConversationId == null) {
            return List.of();
        }

        LambdaQueryWrapper<ChatMessage> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ChatMessage::getConversationId, normalizedConversationId)
                .orderByAsc(ChatMessage::getSeq);
        return chatMessageMapper.selectList(queryWrapper).stream()
                .map(this::toSpringAiMessage)
                .toList();
    }

    @Override
    @Transactional
    public void clear(String conversationId) {
        String normalizedConversationId = normalizeConversationId(conversationId);
        if (normalizedConversationId == null) {
            return;
        }

        LambdaQueryWrapper<ChatMessage> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ChatMessage::getConversationId, normalizedConversationId);
        chatMessageMapper.delete(queryWrapper);
        touchConversation(normalizedConversationId);
    }

    /**
     * 确保会话主记录存在。
     * <p>
     * 当前会话表只保存最小信息，标题、归属用户、扩展属性等后续再按业务需要补齐。
     * </p>
     */
    private void ensureConversationExists(String conversationId) {
        if (chatConversationMapper.selectById(conversationId) != null) {
            return;
        }

        ChatConversation conversation = new ChatConversation();
        conversation.setConversationId(conversationId);
        conversation.setMetadata(Map.of());
        chatConversationMapper.insert(conversation);
    }

    /**
     * 计算当前会话下一条消息的顺序号。
     * <p>
     * 通过查询该会话下最大的 {@code seq} 实现顺序追加，保证数据库中的顺序与对话回放顺序一致。
     * </p>
     */
    private int findNextSeq(String conversationId) {
        LambdaQueryWrapper<ChatMessage> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.select(ChatMessage::getSeq)
                .eq(ChatMessage::getConversationId, conversationId)
                .orderByDesc(ChatMessage::getSeq)
                .last("LIMIT 1");
        ChatMessage latestMessage = chatMessageMapper.selectOne(queryWrapper);
        return latestMessage == null || latestMessage.getSeq() == null ? 1 : latestMessage.getSeq() + 1;
    }

    /**
     * 刷新会话的最近活跃时间。
     * <p>
     * 会话列表通常需要按最后更新时间排序，因此写入或清空消息后都要同步更新会话时间戳。
     * </p>
     */
    private void touchConversation(String conversationId) {
        LambdaUpdateWrapper<ChatConversation> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(ChatConversation::getConversationId, conversationId)
                .set(ChatConversation::getUpdatedAt, LocalDateTime.now());
        chatConversationMapper.update(null, updateWrapper);
    }

    /**
     * 将 Spring AI 消息转换为数据库实体。
     * <p>
     * 当前只保留主文本；媒体、工具调用、消息片段等结构暂不入库。
     * 消息级 metadata 通过 PostgreSQL jsonb 类型处理器持久化。
     * </p>
     */
    private ChatMessage toEntity(String conversationId, int seq, Message message) {
        ChatMessage entity = new ChatMessage();
        entity.setConversationId(conversationId);
        entity.setSeq(seq);
        entity.setRole(resolveRole(message));
        entity.setTextContent(resolveText(message));
        entity.setExtraMetadata(extractMessageMetadata(message));
        return entity;
    }

    /**
     * 将数据库消息还原为 Spring AI 消息对象。
     * <p>
     * 当前仅恢复纯文本上下文，以满足 {@link org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor}
     * 的历史回放需求。
     * </p>
     */
    private Message toSpringAiMessage(ChatMessage message) {
        String text = defaultText(message.getTextContent());
        Map<String, Object> metadata = new LinkedHashMap<>();
        if (message.getExtraMetadata() != null) {
            metadata.putAll(message.getExtraMetadata());
        }

        ChatMessageRole role = message.getRole();
        if (role == null) {
            throw new IllegalArgumentException("聊天消息角色不能为空");
        }

        return switch (role) {
            case USER -> UserMessage.builder()
                    .text(text)
                    .metadata(metadata)
                    .build();
            case SYSTEM -> SystemMessage.builder()
                    .text(text)
                    .metadata(metadata)
                    .build();
            case ASSISTANT -> AssistantMessage.builder()
                    .content(text)
                    .properties(metadata)
                    .build();
            case TOOL -> throw new IllegalStateException("当前持久化会话记忆暂不支持 TOOL 类型消息恢复");
        };
    }

    /**
     * 识别当前支持的消息角色。
     * <p>
     * 现阶段只接受用户、助手、系统三类文本消息；若后续接入工具调用，需要在这里恢复 TOOL 映射。
     * </p>
     */
    private ChatMessageRole resolveRole(Message message) {
        if (message instanceof UserMessage) {
            return ChatMessageRole.USER;
        }
        if (message instanceof AssistantMessage) {
            return ChatMessageRole.ASSISTANT;
        }
        if (message instanceof SystemMessage) {
            return ChatMessageRole.SYSTEM;
        }
        throw new IllegalArgumentException("暂不支持的 Spring AI 消息类型: " + message.getClass().getName());
    }

    /**
     * 提取消息主文本。
     * <p>
     * 当前采用最简单的纯文本策略；若未来支持多模态，可在这里定义“摘要文本”或“主显示文本”的提取规则。
     * </p>
     */
    private String resolveText(Message message) {
        return defaultText(message.getText());
    }

    /**
     * 抽取消息级 metadata。
     * <p>
     * 当前先原样保留 Spring AI 提供的 metadata，交由 PostgreSQL jsonb 类型处理器负责序列化。
     * 后续如有 provider 字段收敛需求，可再增加白名单过滤。
     * </p>
     */
    private Map<String, Object> extractMessageMetadata(Message message) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        if (message.getMetadata() != null) {
            metadata.putAll(message.getMetadata());
        }
        return metadata.isEmpty() ? Map.of() : metadata;
    }

    /** 统一清洗会话 ID，空白值视为无效会话。 */
    private String normalizeConversationId(String conversationId) {
        return StringUtils.hasText(conversationId) ? conversationId.trim() : null;
    }

    /** 将空文本归一化为空串，避免后续空指针分支。 */
    private String defaultText(String text) {
        return text != null ? text : "";
    }
}
