package cn.byteo.springaidemo.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * <p>这里描述类的主要功能</p>
 *
 * @author Roson
 * @file ModelConfiguration
 * @since 2026/4/14 18:34
 */
@Configuration
public class ModelConfiguration {

    @Bean
    ChatClient chatClient(OpenAiChatModel openAiChatModel) {
        return ChatClient.builder(openAiChatModel)
                // 这里可以配置默认的系统消息，或者其他全局参数
                .defaultSystem("你是一个通用的人工智能助手，协助用户解答任何问题，你叫小甜甜！！")
                // 添加打印日志的Advisor，方便观察对话过程
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();
    }

    @Bean
    ChatClient memoryChatClient(OpenAiChatModel openAiChatModel, ChatMemory chatMemory) {
        return ChatClient.builder(openAiChatModel)
                // 这里可以配置默认的系统消息，或者其他全局参数
                .defaultSystem("你是一个通用的人工智能助手，协助用户解答任何问题，你叫小甜甜！！")
                .defaultAdvisors(
                        // 添加打印日志的Advisor，方便观察对话过程
                        new SimpleLoggerAdvisor(),
                        // 添加基于消息窗口的记忆Advisor，保持对话上下文
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
                )
                .build();
    }

    @Bean
    ChatMemoryRepository chatMemoryRepository() {
        // 使用内存中的ChatMemoryRepository存储会话消息，适合测试和开发环境，生产环境可以替换为数据库实现
        return new InMemoryChatMemoryRepository();
    }

    @Bean
    ChatMemory chatMemory(ChatMemoryRepository chatMemoryRepository) {
        // MessageWindowChatMemory 是基于消息窗口的记忆实现，可以根据窗口大小保留最近的消息，适合保持对话上下文而不占用过多内存
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                // 这里设置窗口大小为20，表示每次对话只保留最近的20条消息
                .maxMessages(20)
                .build();
    }
}
