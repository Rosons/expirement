package cn.byteo.springaidemo.chat.service.impl;

import cn.byteo.springaidemo.chat.entity.ChatConversation;
import cn.byteo.springaidemo.chat.entity.ChatMessage;
import cn.byteo.springaidemo.chat.entity.ChatMessagePart;
import cn.byteo.springaidemo.chat.mapper.ChatConversationMapper;
import cn.byteo.springaidemo.chat.mapper.ChatMessageMapper;
import cn.byteo.springaidemo.chat.mapper.ChatMessagePartMapper;
import cn.byteo.springaidemo.chat.service.ChatService;
import cn.byteo.springaidemo.chat.support.ChatConversationTypeSupport;
import cn.byteo.springaidemo.chat.support.SpringAiMessagesAdapter;
import cn.byteo.springaidemo.chat.vo.ChatConversationListVo;
import cn.byteo.springaidemo.chat.vo.ChatMessageHistoryPageVo;
import cn.byteo.springaidemo.chat.vo.ChatMessageHistoryVo;
import cn.byteo.springaidemo.chat.vo.ChatMessagePartVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 基于数据库的持久化实现（MyBatis-Plus）。
 */
@Service("persistentChatService")
@RequiredArgsConstructor
public class PersistentChatService implements ChatService {

    private static final int MAX_PAGE_SIZE = SpringAiMessagesAdapter.MAX_PAGE_SIZE;

    private final ChatConversationMapper chatConversationMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final ChatMessagePartMapper chatMessagePartMapper;

    @Override
    public List<ChatConversationListVo> listConversations(String type) {
        String normalizedType = ChatConversationTypeSupport.normalize(type);
        if (!StringUtils.hasText(normalizedType)) {
            return List.of();
        }
        LambdaQueryWrapper<ChatConversation> q = new LambdaQueryWrapper<>();
        q.eq(ChatConversation::getType, normalizedType);
        q.orderByDesc(ChatConversation::getUpdatedAt);
        return chatConversationMapper.selectList(q).stream()
                .map(c -> ChatConversationListVo.builder()
                        .conversationId(c.getConversationId())
                        .title(c.getTitle())
                        .type(c.getType())
                        .updatedAt(c.getUpdatedAt())
                        .build())
                .toList();
    }

    /**
     * @param page 页码，从 1 开始
     * @param size 每页条数，最大 {@link #MAX_PAGE_SIZE}
     */
    @Override
    public ChatMessageHistoryPageVo pageHistory(String conversationId, int page, int size, boolean newestFirst) {
        if (!StringUtils.hasText(conversationId)) {
            return emptyPage(page, size);
        }
        int p = Math.max(1, page);
        int s = Math.min(MAX_PAGE_SIZE, Math.max(1, size));

        LambdaQueryWrapper<ChatMessage> countW = new LambdaQueryWrapper<>();
        countW.eq(ChatMessage::getConversationId, conversationId);
        long total = chatMessageMapper.selectCount(countW);

        int offset = (p - 1) * s;
        LambdaQueryWrapper<ChatMessage> mq = new LambdaQueryWrapper<>();
        mq.eq(ChatMessage::getConversationId, conversationId);
        if (newestFirst) {
            mq.orderByDesc(ChatMessage::getSeq);
        } else {
            mq.orderByAsc(ChatMessage::getSeq);
        }
        mq.last("LIMIT " + s + " OFFSET " + offset);
        List<ChatMessage> messages = chatMessageMapper.selectList(mq);
        // DESC 仅用于「从新到旧」截取 LIMIT窗口；接口约定每页 records 仍按 seq 升序，故对当前页做一次倒序即可（与再 sort 等价，略省）。
        if (newestFirst && !messages.isEmpty()) {
            Collections.reverse(messages);
        }

        long pages = total == 0 ? 0 : (total + s - 1) / s;

        if (messages.isEmpty()) {
            return ChatMessageHistoryPageVo.builder()
                    .records(List.of())
                    .total(total)
                    .size(s)
                    .current(p)
                    .pages(pages)
                    .build();
        }

        // 批量查询当前页消息的所有片段，按 messageId 分组后构造 VO。
        List<Long> messageIds = messages.stream().map(ChatMessage::getId).toList();
        LambdaQueryWrapper<ChatMessagePart> pq = new LambdaQueryWrapper<>();
        pq.in(ChatMessagePart::getMessageId, messageIds)
                .orderByAsc(ChatMessagePart::getMessageId)
                .orderByAsc(ChatMessagePart::getPartIndex);
        List<ChatMessagePart> allParts = chatMessagePartMapper.selectList(pq);
        Map<Long, List<ChatMessagePart>> partsByMessage = allParts.stream()
                .collect(Collectors.groupingBy(ChatMessagePart::getMessageId));

        // 组装当前页消息 VO 列表，片段按 partIndex 升序。
        List<ChatMessageHistoryVo> records = messages.stream()
                .map(m -> toHistoryVo(m, partsByMessage.getOrDefault(m.getId(), Collections.emptyList())))
                .toList();

        return ChatMessageHistoryPageVo.builder()
                .records(records)
                .total(total)
                .size(s)
                .current(p)
                .pages(pages)
                .build();
    }

    private static ChatMessageHistoryPageVo emptyPage(int page, int size) {
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

    private static ChatMessageHistoryVo toHistoryVo(ChatMessage m, List<ChatMessagePart> parts) {
        String roleCode = m.getRole() != null ? m.getRole().getCode() : null;
        String text = m.getTextContent();
        List<ChatMessagePartVo> partVos = parts.stream()
                .sorted(Comparator.comparing(ChatMessagePart::getPartIndex))
                .map(PersistentChatService::toPartVo)
                .toList();
        return ChatMessageHistoryVo.builder()
                .id(m.getId())
                .seq(m.getSeq())
                .role(roleCode)
                .content(text)
                .parts(partVos)
                .createdAt(m.getCreatedAt())
                .build();
    }

    private static ChatMessagePartVo toPartVo(ChatMessagePart p) {
        String typeCode = p.getPartType() != null ? p.getPartType().getCode() : null;
        return ChatMessagePartVo.builder()
                .partIndex(p.getPartIndex())
                .partType(typeCode)
                .contentText(p.getContentText())
                .mediaUrl(p.getMediaUrl())
                .mimeType(p.getMimeType())
                .payload(p.getPayload())
                .build();
    }
}
