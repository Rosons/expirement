package cn.byteo.springaidemo.controller;

import cn.byteo.springaidemo.chat.dto.ChatHistoryQueryRequest;
import cn.byteo.springaidemo.chat.dto.ChatStreamQueryRequest;
import cn.byteo.springaidemo.chat.service.ChatConversationManageService;
import cn.byteo.springaidemo.chat.service.ChatService;
import cn.byteo.springaidemo.chat.vo.ChatConversationListVo;
import cn.byteo.springaidemo.chat.vo.ChatMessageHistoryPageVo;
import cn.byteo.springaidemo.common.web.ApiResponse;
import cn.byteo.springaidemo.constant.SystemConstant;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * <p>这里描述类的主要功能</p>
 *
 * @author Roson
 * @file KnowledgeChatController
 * @since 2026/4/16
 */
@RestController
@RequestMapping("/knowledge/ai/chat")
public class KnowledgeChatController {

    private final ChatClient chatClient;

    private final ChatService chatService;

    private final ChatConversationManageService chatConversationManageService;

    public KnowledgeChatController(@Qualifier("knowledgeChatClient") ChatClient chatClient,
                                   @Qualifier("persistentChatService") ChatService chatService,
                                   ChatConversationManageService chatConversationManageService) {
        this.chatClient = chatClient;
        this.chatService = chatService;
        this.chatConversationManageService = chatConversationManageService;
    }

    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chat(@ModelAttribute ChatStreamQueryRequest query) {
        chatConversationManageService.ensureConversationType(query.getChatId(), query.requireType());
        return chatClient.prompt()
                // 传递会话ID
                .advisors(p -> p.param(ChatMemory.CONVERSATION_ID, query.getChatId()))
                // 动态传递向量查询条件
                .advisors(p -> p.param(QuestionAnswerAdvisor.FILTER_EXPRESSION,
                        SystemConstant.VECTOR_CHAT_ID + " == '"+ query.getChatId() +"'"))
                .user(query.getMessage())
                .stream()
                .content();
    }

    @GetMapping("/conversations")
    public ApiResponse<List<ChatConversationListVo>> getConversations(@RequestParam(value = "type") String type) {
        return ApiResponse.ok(chatService.listConversations(type));
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
