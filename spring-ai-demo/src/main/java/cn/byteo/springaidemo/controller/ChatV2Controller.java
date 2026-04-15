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
 * 聊天 API v2：会话与历史数据来自 {@link cn.byteo.springaidemo.chat.service.impl.PersistentChatService}；入参/出参与 v1 一致（便于前端只切换版本前缀）。
 * 流式对话仍使用内存 {@link ChatMemory}。
 */
@RestController
@RequestMapping("/v2/ai/chat")
public class ChatV2Controller {

    private final ChatClient chatClient;

    private final ChatService chatService;

    public ChatV2Controller(@Qualifier("persistentMemoryChatClient") ChatClient chatClient,
                            @Qualifier("persistentChatService") ChatService chatService) {
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
     * 分页查询会话内消息；每页 {@code records} 内按 {@code seq} 升序。
     * 支持 {@code order=desc}，见 {@link ChatHistoryQueryRequest}。
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
