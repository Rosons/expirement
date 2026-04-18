package cn.byteo.springaidemo.config;

import cn.byteo.springaidemo.chat.advisor.DocumentMediaInliningAdvisor;
import cn.byteo.springaidemo.chat.model.AlibabaOpenAiChatModel;
import cn.byteo.springaidemo.constant.SystemConstant;
import cn.byteo.springaidemo.tools.CustomerTool;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.observation.ChatModelObservationConvention;
import org.springframework.ai.model.SimpleApiKey;
import org.springframework.ai.model.openai.autoconfigure.OpenAiChatProperties;
import org.springframework.ai.model.openai.autoconfigure.OpenAiConnectionProperties;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * <p>这里描述类的主要功能</p>
 *
 * @author Roson
 * @file ModelConfiguration
 * @since 2026/4/14 18:34
 */
@Configuration
public class ModelConfiguration {

    @Value("${app.multimodal-model-name:}")
    private String modelName;

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
                                @Qualifier(value = "persistentChatMemory") ChatMemory chatMemory,
                                DocumentMediaInliningAdvisor documentMediaInliningAdvisor) {
        return ChatClient.builder(openAiChatModel)
                // 这里可以配置默认的系统消息，或者其他全局参数
                .defaultSystem(SystemConstant.SIMPLE_SYSTEM_PROMPT)
                .defaultAdvisors(
                        // 添加打印日志的Advisor，方便观察对话过程
                        new SimpleLoggerAdvisor(),
                        // 添加基于消息窗口的记忆Advisor，保持对话上下文
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        // 文档文件解析，拼接到用户问题中，发送给大模型
                        documentMediaInliningAdvisor
                )
                .build();
    }

    @Bean
    ChatClient gameChatClient(OpenAiChatModel openAiChatModel,
                                  @Qualifier(value = "persistentChatMemory") ChatMemory chatMemory) {
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
                                        .similarityThreshold(0.5)
                                        .topK(3)
                                        .build())
                                .build()
                )
                .build();
    }

    @Bean
    ChatClient customerChatClient(OpenAiChatModel openAiChatModel,
                                  @Qualifier(value = "persistentChatMemory") ChatMemory chatMemory,
                                  CustomerTool customerTool) {
        return ChatClient.builder(openAiChatModel)
                // 这里可以配置默认的系统消息，或者其他全局参数
                .defaultSystem(SystemConstant.SERVICE_SYSTEM_PROMPT)
                // 添加一个自定义工具，提供大模型调用的接口，方便在对话中直接调用工具获取信息
                .defaultTools(customerTool)
                .defaultAdvisors(
                        // 添加打印日志的Advisor，方便观察对话过程
                        new SimpleLoggerAdvisor(),
                        // 添加基于消息窗口的记忆Advisor，保持对话上下文
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
                )
                .build();
    }

    @Bean
    ChatClient multiModalChatClient(AlibabaOpenAiChatModel alibabaOpenAiChatModel,
                                    @Qualifier(value = "persistentChatMemory") ChatMemory chatMemory,
                                    DocumentMediaInliningAdvisor documentMediaInliningAdvisor) {
        // 多模特模型
        AlibabaOpenAiChatModel multiModalChatModel = alibabaOpenAiChatModel.mutate()
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(modelName)
                        .temperature(0.7)
                        .build())
                .build();
        return ChatClient.builder(multiModalChatModel)
                // 这里可以配置默认的系统消息，或者其他全局参数
                .defaultSystem(SystemConstant.SIMPLE_SYSTEM_PROMPT)
                .defaultAdvisors(
                        // 添加打印日志的Advisor，方便观察对话过程
                        new SimpleLoggerAdvisor(),
                        // 添加基于消息窗口的记忆Advisor，保持对话上下文
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        // PDF 等文本文档：解析为文本并入 user text，避免有些多模态模型，不支持文件分析
                        documentMediaInliningAdvisor
                )
                .build();
    }


    /**
     * 注册「多模态对话」用的 {@link AlibabaOpenAiChatModel}，供带图片 / 音频 / 视频等附件的会话使用（例如 {@code ChatV2Controller} 在存在 {@code files} 时走
     * {@link #multiModalChatClient}）。
     * <p>
     * <b>为何不用框架自带的 {@link OpenAiChatModel}</b>：实现类以 Spring AI 自带的 OpenAI 兼容调用为准；部分厂商（如阿里云等 OpenAI 兼容网关）对
     * <b>音频</b>等媒体在请求体中的封装与官方协议存在偏差，默认序列化会导致音频无法被正确识别。{@link AlibabaOpenAiChatModel} 在继承/对齐原有
     * 多模态流程的前提下，仅对音频相关构造路径（如 {@code fromAudioData} 一带）做了适配，其余模态仍按既有逻辑处理。
     * <p>
     * <b>装配说明</b>：参数与 Spring Boot 自动配置 {@link OpenAiChatModel} 时常用的连接项一致——从 {@link OpenAiConnectionProperties} 与
     * {@link OpenAiChatProperties} 解析 baseUrl、apiKey、completionsPath、默认 {@link OpenAiChatOptions} 等，并注入
     * {@link ToolCallingManager}、{@link RetryTemplate}、观测与错误处理，保证与项目内其他 ChatModel Bean 行为可对照。
     * 具体模型名由 {@link #multiModalChatClient} 中通过 {@code app.multimodal-model-name} 在 {@code mutate()} 时覆盖，不在此 Bean 内写死。
     *
     * @return 已配置好 {@link OpenAiApi} 与默认选项的多模态聊天模型实例，可被注入到 {@link #multiModalChatClient} 等消费者
     */
    @Bean
    public AlibabaOpenAiChatModel alibabaOpenAiChatModel(OpenAiConnectionProperties commonProperties,
                                                            OpenAiChatProperties chatProperties,
                                                            ObjectProvider<RestClient.Builder> restClientBuilderProvider,
                                                            ObjectProvider<WebClient.Builder> webClientBuilderProvider,
                                                            ToolCallingManager toolCallingManager,
                                                            RetryTemplate retryTemplate,
                                                            ResponseErrorHandler responseErrorHandler,
                                                            ObjectProvider<ObservationRegistry> observationRegistry,
                                                            ObjectProvider<ChatModelObservationConvention> observationConvention) {
        String baseUrl = StringUtils.hasText(chatProperties.getBaseUrl()) ?
                chatProperties.getBaseUrl() : commonProperties.getBaseUrl();
        String apiKey = StringUtils.hasText(chatProperties.getApiKey()) ?
                chatProperties.getApiKey() : commonProperties.getApiKey();
        String projectId = StringUtils.hasText(chatProperties.getProjectId()) ?
                chatProperties.getProjectId() : commonProperties.getProjectId();
        String organizationId = StringUtils.hasText(chatProperties.getOrganizationId()) ?
                chatProperties.getOrganizationId() : commonProperties.getOrganizationId();
        Map<String, List<String>> connectionHeaders = new HashMap<>();
        if (StringUtils.hasText(projectId)) {
            connectionHeaders.put("OpenAI-Project", List.of(projectId));
        }

        if (StringUtils.hasText(organizationId)) {
            connectionHeaders.put("OpenAI-Organization", List.of(organizationId));
        }
        RestClient.Builder restClientBuilder = restClientBuilderProvider.getIfAvailable(RestClient::builder);
        WebClient.Builder webClientBuilder = webClientBuilderProvider.getIfAvailable(WebClient::builder);
        OpenAiApi openAiApi = OpenAiApi
                .builder()
                .baseUrl(baseUrl)
                .apiKey(new SimpleApiKey(apiKey))
                .headers(CollectionUtils.toMultiValueMap(connectionHeaders))
                .completionsPath(chatProperties.getCompletionsPath())
                .embeddingsPath("/v1/embeddings")
                .restClientBuilder(restClientBuilder)
                .webClientBuilder(webClientBuilder)
                .responseErrorHandler(responseErrorHandler).build();
        AlibabaOpenAiChatModel chatModel = AlibabaOpenAiChatModel
                .builder()
                .openAiApi(openAiApi)
                .defaultOptions(chatProperties.getOptions())
                .toolCallingManager(toolCallingManager)
                .retryTemplate(retryTemplate)
                .observationRegistry((ObservationRegistry) observationRegistry.getIfUnique(() -> ObservationRegistry.NOOP))
                .build();
        Objects.requireNonNull(chatModel);
        observationConvention.ifAvailable(chatModel::setObservationConvention);
        return chatModel;
    }
}
