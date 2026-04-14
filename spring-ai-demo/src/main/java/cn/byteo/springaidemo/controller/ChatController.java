package cn.byteo.springaidemo.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
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
@RequestMapping("/ai")
@RequiredArgsConstructor
public class ChatController {

    private final ChatClient memoryChatClient;

    private final ChatMemory chatMemory;

    @GetMapping(value = "/chat", produces = "text/html;charset=UTF-8")
    public Flux<String> chat(String chatId, String message) {
        return memoryChatClient.prompt()
                // 增加一个Advisor，动态设置当前对话的conversationId，这样ChatMemory就能正确地将消息关联到对应的会话中，实现多用户多会话的记忆功能
                .advisors(p -> p.param(ChatMemory.CONVERSATION_ID, chatId))
                .user(message)
                .stream()
                .content();
    }

}
