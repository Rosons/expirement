package cn.byteo.springaidemo.controller;

import cn.byteo.springaidemo.chat.dto.ChatHistoryQueryRequest;
import cn.byteo.springaidemo.chat.dto.ChatStreamQueryRequest;
import cn.byteo.springaidemo.chat.service.ChatConversationManageService;
import cn.byteo.springaidemo.chat.service.ChatService;
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

/**
 * <p>这里描述类的主要功能</p>
 *
 * @author Roson
 * @file GameChatController
 * @since 2026/4/15
 */
@RestController
@RequestMapping("/game/ai/chat")
public class GameChatController {

    private final ChatClient chatClient;

    private final ChatService chatService;

    private final ChatConversationManageService chatConversationManageService;

    public GameChatController(@Qualifier("gameChatClient") ChatClient chatClient,
                              @Qualifier(value = "persistentChatService") ChatService chatService,
                              ChatConversationManageService chatConversationManageService) {
        this.chatClient = chatClient;
        this.chatService = chatService;
        this.chatConversationManageService = chatConversationManageService;
    }

    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chat(@ModelAttribute ChatStreamQueryRequest query) {
        chatConversationManageService.ensureConversationType(query.getChatId(), query.requireType());
        return chatClient.prompt()
                .advisors(p -> p.param(ChatMemory.CONVERSATION_ID, query.getChatId()))
                .user(query.getMessage())
                .stream()
                .content();
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
