package cn.byteo.springaidemo.config;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * <p>这里描述类的主要功能</p>
 *
 * @author Roson
 * @file ChatMemoryConfiguration
 * @since 2026/4/15 9:43
 */
@Configuration
public class ChatMemoryConfiguration {

    @Bean
    ChatMemoryRepository simpleChatMemoryRepository() {
        // 使用内存中的ChatMemoryRepository存储会话消息，适合测试和开发环境，生产环境可以替换为数据库实现
        return new InMemoryChatMemoryRepository();
    }

    @Bean
    ChatMemory simpleChatMemory(ChatMemoryRepository chatMemoryRepository) {
        // MessageWindowChatMemory 是基于消息窗口的记忆实现，可以根据窗口大小保留最近的消息，适合保持对话上下文而不占用过多内存
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                // 这里设置窗口大小为20，表示每次对话只保留最近的20条消息
                .maxMessages(20)
                .build();
    }
}
