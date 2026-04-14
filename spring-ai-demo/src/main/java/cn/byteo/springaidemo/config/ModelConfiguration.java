package cn.byteo.springaidemo.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
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
    @ConditionalOnMissingBean(value = ChatClient.class)
    ChatClient chatClient(OpenAiChatModel openAiChatModel) {
        return ChatClient.builder(openAiChatModel)
                // 这里可以配置默认的系统消息，或者其他全局参数
                .defaultSystem("你是一个通用的人工智能助手，协助用户解答任何问题，你叫小甜甜！！")
                .build();
    }
}
