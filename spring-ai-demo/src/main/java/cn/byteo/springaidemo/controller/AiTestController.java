package cn.byteo.springaidemo.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * <p>这里描述类的主要功能</p>
 *
 * @author Roson
 * @file AiTestController
 * @since 2026/4/14 17:20
 */
@RestController
@RequestMapping("/ai/test")
@RequiredArgsConstructor
public class AiTestController {

    private final ChatClient chatClient;

    @GetMapping(value = "/hello", produces = "text/event-stream;charset=UTF-8")
    public Flux<String> hello() {
        return Flux.just("Hello", "World", "From", "Spring", "AI", "Demo");
    }

    @GetMapping(value = "/chat", produces = "text/html;charset=UTF-8")
    public Flux<String> chat(String chatId, String message) {
        return chatClient.prompt()
                .user(message)
                .stream()
                .content();
    }

}
