package cn.byteo.springaidemo.controller;

import cn.byteo.springaidemo.chat.dto.ChatStreamQueryRequest;
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

    public GameChatController(@Qualifier("gameChatClient") ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chat(@ModelAttribute ChatStreamQueryRequest query) {
        return chatClient.prompt()
                .advisors(p -> p.param(ChatMemory.CONVERSATION_ID, query.getChatId()))
                .user(query.getMessage())
                .stream()
                .content();
    }
}
