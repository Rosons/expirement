package cn.byteo.springaidemo.config;

import cn.byteo.springaidemo.constant.SystemConstant;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
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
                .defaultSystem(SystemConstant.SIMPLE_SYSTEM_PROMPT)
                // 添加打印日志的Advisor，方便观察对话过程
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();
    }

    @Bean
    ChatClient memoryChatClient(OpenAiChatModel openAiChatModel,
                                      @Qualifier(value = "simpleChatMemory") ChatMemory chatMemory) {
        return ChatClient.builder(openAiChatModel)
                // 这里可以配置默认的系统消息，或者其他全局参数
                .defaultSystem(SystemConstant.SIMPLE_SYSTEM_PROMPT)
                .defaultAdvisors(
                        // 添加打印日志的Advisor，方便观察对话过程
                        new SimpleLoggerAdvisor(),
                        // 添加基于消息窗口的记忆Advisor，保持对话上下文
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
                )
                .build();
    }
}
