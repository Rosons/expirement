package cn.byteo.springaidemo.controller;

import cn.byteo.springaidemo.chat.dto.ChatHistoryQueryRequest;
import cn.byteo.springaidemo.chat.dto.ChatStreamQueryRequest;
import cn.byteo.springaidemo.chat.service.ChatService;
import cn.byteo.springaidemo.chat.vo.ChatConversationListVo;
import cn.byteo.springaidemo.chat.vo.ChatMessageHistoryPageVo;
import cn.byteo.springaidemo.common.web.ApiResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 聊天 API v1：会话列表与历史来自内存 {@link cn.byteo.springaidemo.chat.service.impl.MemoryChatService}；<strong>入参/出参与 v2 对齐</strong>，
 * 便于前端只改版本前缀、共用同一套 DTO 解析逻辑。
 */
@RestController
@RequestMapping("/v1/ai/chat")
public class ChatV1Controller {

    private final ChatClient chatClient;

    private final ChatService chatService;

    public ChatV1Controller(@Qualifier("inMemoryChatClient") ChatClient chatClient,
                            @Qualifier("inMemoryChatService") ChatService chatService) {
        this.chatClient = chatClient;
        this.chatService = chatService;
    }

    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chat(@ModelAttribute ChatStreamQueryRequest query) {
        return chatClient.prompt()
                .advisors(p -> p.param(ChatMemory.CONVERSATION_ID, query.getChatId()))
                .user(query.getMessage())
                .stream()
                .content();
    }

    @GetMapping("/conversations")
    public ApiResponse<List<ChatConversationListVo>> getConversations() {
        return ApiResponse.ok(chatService.listConversations());
    }

    /**
     * 与 v2 相同：分页参数 + {@link ChatMessageHistoryPageVo}；数据来自内存消息列表按顺序切片。
     */
    @GetMapping("/history")
    public ApiResponse<ChatMessageHistoryPageVo> getChatHistory(@ModelAttribute ChatHistoryQueryRequest query) {
        return ApiResponse.ok(chatService.pageHistory(
                query.requireChatId(),
                query.resolvePage(),
                query.resolveSize(),
                query.resolveNewestFirst()));
    }
}
