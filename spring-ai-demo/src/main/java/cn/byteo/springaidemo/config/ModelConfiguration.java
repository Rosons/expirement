package cn.byteo.springaidemo.config;

import cn.byteo.springaidemo.constant.SystemConstant;
import cn.byteo.springaidemo.tools.CustomerTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
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
    ChatClient inMemoryChatClient(OpenAiChatModel openAiChatModel,
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

    @Bean
    ChatClient persistentMemoryChatClient(OpenAiChatModel openAiChatModel,
                                @Qualifier(value = "persistentChatMemory") ChatMemory chatMemory) {
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

    @Bean
    ChatClient gameChatClient(OpenAiChatModel openAiChatModel,
                                  @Qualifier(value = "simpleChatMemory") ChatMemory chatMemory) {
        return ChatClient.builder(openAiChatModel)
                .defaultSystem(SystemConstant.GAME_SYSTEM_PROMPT)
                .defaultAdvisors(
                        // 添加打印日志的Advisor，方便观察对话过程
                        new SimpleLoggerAdvisor(),
                        // 添加基于消息窗口的记忆Advisor，保持对话上下文
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
                )
                .build();
    }


    @Bean
    ChatClient knowledgeChatClient(OpenAiChatModel openAiChatModel,
                                   @Qualifier(value = "persistentChatMemory") ChatMemory chatMemory,
                                   VectorStore vectorStore) {
        return ChatClient.builder(openAiChatModel)
                .defaultSystem(SystemConstant.SIMPLE_SYSTEM_PROMPT)
                .defaultAdvisors(
                        // 添加打印日志的Advisor，方便观察对话过程
                        new SimpleLoggerAdvisor(),
                        // 添加基于消息窗口的记忆Advisor，保持对话上下文
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        // 添加支持问答（RAG）的Advisor
                        QuestionAnswerAdvisor.builder(vectorStore)
                                .searchRequest(SearchRequest
                                        .builder()
                                        .similarityThreshold(0.7)
                                        .topK(2)
                                        .build())
                                .build()
                )
                .build();
    }

    @Bean
    ChatClient customerChatClient(OpenAiChatModel openAiChatModel,
                                  @Qualifier(value = "persistentChatMemory") ChatMemory chatMemory) {
        return ChatClient.builder(openAiChatModel)
                // 这里可以配置默认的系统消息，或者其他全局参数
                .defaultSystem(SystemConstant.SIMPLE_SYSTEM_PROMPT)
                // 添加一个自定义工具，提供大模型调用的接口，方便在对话中直接调用工具获取信息
                // .defaultTools(customerTool)
                .defaultAdvisors(
                        // 添加打印日志的Advisor，方便观察对话过程
                        new SimpleLoggerAdvisor(),
                        // 添加基于消息窗口的记忆Advisor，保持对话上下文
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
                )
                .build();
    }
}
