package cn.byteo.springaidemo.chat.model;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.metadata.EmptyUsage;
import org.springframework.ai.chat.metadata.RateLimit;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.model.MessageAggregator;
import org.springframework.ai.chat.observation.ChatModelObservationContext;
import org.springframework.ai.chat.observation.ChatModelObservationConvention;
import org.springframework.ai.chat.observation.ChatModelObservationDocumentation;
import org.springframework.ai.chat.observation.DefaultChatModelObservationConvention;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.ai.model.tool.DefaultToolExecutionEligibilityPredicate;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionEligibilityPredicate;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.model.tool.internal.ToolCallReactiveContextHolder;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.openai.api.OpenAiApi.OutputModality;
import org.springframework.ai.openai.api.OpenAiApi.ChatCompletionMessage.Role;
import org.springframework.ai.openai.api.OpenAiApi.ChatCompletionMessage.MediaContent.InputAudio.Format;
import org.springframework.ai.openai.api.common.OpenAiApiConstants;
import org.springframework.ai.openai.metadata.support.OpenAiResponseHeaderExtractor;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.ai.support.UsageCalculator;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;


public class AlibabaOpenAiChatModel implements ChatModel {
    private static final Logger logger = LoggerFactory.getLogger(AlibabaOpenAiChatModel.class);
    private static final ChatModelObservationConvention DEFAULT_OBSERVATION_CONVENTION = new DefaultChatModelObservationConvention();
    private static final ToolCallingManager DEFAULT_TOOL_CALLING_MANAGER = ToolCallingManager.builder().build();
    private final OpenAiChatOptions defaultOptions;
    private final RetryTemplate retryTemplate;
    private final OpenAiApi openAiApi;
    private final ObservationRegistry observationRegistry;
    private final ToolCallingManager toolCallingManager;
    private final ToolExecutionEligibilityPredicate toolExecutionEligibilityPredicate;
    private ChatModelObservationConvention observationConvention;

    public AlibabaOpenAiChatModel(OpenAiApi openAiApi, OpenAiChatOptions defaultOptions, ToolCallingManager toolCallingManager, RetryTemplate retryTemplate, ObservationRegistry observationRegistry) {
        this(openAiApi, defaultOptions, toolCallingManager, retryTemplate, observationRegistry, new DefaultToolExecutionEligibilityPredicate());
    }

    public AlibabaOpenAiChatModel(OpenAiApi openAiApi, OpenAiChatOptions defaultOptions, ToolCallingManager toolCallingManager, RetryTemplate retryTemplate, ObservationRegistry observationRegistry, ToolExecutionEligibilityPredicate toolExecutionEligibilityPredicate) {
        this.observationConvention = DEFAULT_OBSERVATION_CONVENTION;
        Assert.notNull(openAiApi, "openAiApi cannot be null");
        Assert.notNull(defaultOptions, "defaultOptions cannot be null");
        Assert.notNull(toolCallingManager, "toolCallingManager cannot be null");
        Assert.notNull(retryTemplate, "retryTemplate cannot be null");
        Assert.notNull(observationRegistry, "observationRegistry cannot be null");
        Assert.notNull(toolExecutionEligibilityPredicate, "toolExecutionEligibilityPredicate cannot be null");
        this.openAiApi = openAiApi;
        this.defaultOptions = defaultOptions;
        this.toolCallingManager = toolCallingManager;
        this.retryTemplate = retryTemplate;
        this.observationRegistry = observationRegistry;
        this.toolExecutionEligibilityPredicate = toolExecutionEligibilityPredicate;
    }

    public ChatResponse call(Prompt prompt) {
        Prompt requestPrompt = this.buildRequestPrompt(prompt);
        return this.internalCall(requestPrompt, (ChatResponse)null);
    }

    public ChatResponse internalCall(Prompt prompt, ChatResponse previousChatResponse) {
        OpenAiApi.ChatCompletionRequest request = this.createRequest(prompt, false);
        ChatModelObservationContext observationContext = ChatModelObservationContext.builder().prompt(prompt).provider(OpenAiApiConstants.PROVIDER_NAME).build();
        ChatResponse response = (ChatResponse)ChatModelObservationDocumentation.CHAT_MODEL_OPERATION.observation(this.observationConvention, DEFAULT_OBSERVATION_CONVENTION, () -> observationContext, this.observationRegistry).observe(() -> {
            ResponseEntity<OpenAiApi.ChatCompletion> completionEntity = (ResponseEntity)this.retryTemplate.execute((ctx) -> this.openAiApi.chatCompletionEntity(request, this.getAdditionalHttpHeaders(prompt)));
            OpenAiApi.ChatCompletion chatCompletion = (OpenAiApi.ChatCompletion)completionEntity.getBody();
            if (chatCompletion == null) {
                logger.warn("No chat completion returned for prompt: {}", prompt);
                return new ChatResponse(List.of());
            } else {
                List<OpenAiApi.ChatCompletion.Choice> choices = chatCompletion.choices();
                if (choices == null) {
                    logger.warn("No choices returned for prompt: {}", prompt);
                    return new ChatResponse(List.of());
                } else {
                    List<Generation> generations = choices.stream().map((choice) -> {
                        Map<String, Object> metadata = Map.of("id", chatCompletion.id() != null ? chatCompletion.id() : "", "role", choice.message().role() != null ? choice.message().role().name() : "", "index", choice.index() != null ? choice.index() : 0, "finishReason", this.getFinishReasonJson(choice.finishReason()), "refusal", StringUtils.hasText(choice.message().refusal()) ? choice.message().refusal() : "", "annotations", choice.message().annotations() != null ? choice.message().annotations() : List.of(Map.of()));
                        return this.buildGeneration(choice, metadata, request);
                    }).toList();
                    RateLimit rateLimit = OpenAiResponseHeaderExtractor.extractAiResponseHeaders(completionEntity);
                    OpenAiApi.Usage usage = chatCompletion.usage();
                    Usage currentChatResponseUsage = (Usage)(usage != null ? this.getDefaultUsage(usage) : new EmptyUsage());
                    Usage accumulatedUsage = UsageCalculator.getCumulativeUsage(currentChatResponseUsage, previousChatResponse);
                    ChatResponse chatResponse = new ChatResponse(generations, this.from(chatCompletion, rateLimit, accumulatedUsage));
                    observationContext.setResponse(chatResponse);
                    return chatResponse;
                }
            }
        });
        if (this.toolExecutionEligibilityPredicate.isToolExecutionRequired(prompt.getOptions(), response)) {
            ToolExecutionResult toolExecutionResult = this.toolCallingManager.executeToolCalls(prompt, response);
            return toolExecutionResult.returnDirect() ? ChatResponse.builder().from(response).generations(ToolExecutionResult.buildGenerations(toolExecutionResult)).build() : this.internalCall(new Prompt(toolExecutionResult.conversationHistory(), prompt.getOptions()), response);
        } else {
            return response;
        }
    }

    public Flux<ChatResponse> stream(Prompt prompt) {
        Prompt requestPrompt = this.buildRequestPrompt(prompt);
        return this.internalStream(requestPrompt, (ChatResponse)null);
    }

    public Flux<ChatResponse> internalStream(Prompt prompt, ChatResponse previousChatResponse) {
        return Flux.deferContextual((contextView) -> {
            OpenAiApi.ChatCompletionRequest request = this.createRequest(prompt, true);
            if (request.outputModalities() != null && request.outputModalities().contains(OutputModality.AUDIO)) {
                logger.warn("Audio output is not supported for streaming requests. Removing audio output.");
                throw new IllegalArgumentException("Audio output is not supported for streaming requests.");
            } else if (request.audioParameters() != null) {
                logger.warn("Audio parameters are not supported for streaming requests. Removing audio parameters.");
                throw new IllegalArgumentException("Audio parameters are not supported for streaming requests.");
            } else {
                Flux<OpenAiApi.ChatCompletionChunk> completionChunks = this.openAiApi.chatCompletionStream(request, this.getAdditionalHttpHeaders(prompt));
                ConcurrentHashMap<String, String> roleMap = new ConcurrentHashMap();
                ChatModelObservationContext observationContext = ChatModelObservationContext.builder().prompt(prompt).provider(OpenAiApiConstants.PROVIDER_NAME).build();
                Observation observation = ChatModelObservationDocumentation.CHAT_MODEL_OPERATION.observation(this.observationConvention, DEFAULT_OBSERVATION_CONVENTION, () -> observationContext, this.observationRegistry);
                observation.parentObservation((Observation)contextView.getOrDefault("micrometer.observation", (Object)null)).start();
                Flux<ChatResponse> chatResponse = completionChunks.map(this::chunkToChatCompletion).switchMap((chatCompletion) -> Mono.just(chatCompletion).map((chatCompletion2) -> {
                    try {
                        String id = chatCompletion2.id() == null ? "NO_ID" : chatCompletion2.id();
                        List<Generation> generations = chatCompletion2.choices().stream().map((choice) -> {
                            if (choice.message().role() != null) {
                                roleMap.putIfAbsent(id, choice.message().role().name());
                            }

                            Map<String, Object> metadata = Map.of("id", id, "role", roleMap.getOrDefault(id, ""), "index", choice.index() != null ? choice.index() : 0, "finishReason", this.getFinishReasonJson(choice.finishReason()), "refusal", StringUtils.hasText(choice.message().refusal()) ? choice.message().refusal() : "", "annotations", choice.message().annotations() != null ? choice.message().annotations() : List.of(), "reasoningContent", choice.message().reasoningContent() != null ? choice.message().reasoningContent() : "");
                            return this.buildGeneration(choice, metadata, request);
                        }).toList();
                        OpenAiApi.Usage usage = chatCompletion2.usage();
                        Usage currentChatResponseUsage = (Usage)(usage != null ? this.getDefaultUsage(usage) : new EmptyUsage());
                        Usage accumulatedUsage = UsageCalculator.getCumulativeUsage(currentChatResponseUsage, previousChatResponse);
                        return new ChatResponse(generations, this.from(chatCompletion2, (RateLimit)null, accumulatedUsage));
                    } catch (Exception e) {
                        logger.error("Error processing chat completion", e);
                        return new ChatResponse(List.of());
                    }
                })).buffer(2, 1).map((bufferList) -> {
                    ChatResponse firstResponse = (ChatResponse)bufferList.get(0);
                    if (request.streamOptions() != null && request.streamOptions().includeUsage() && bufferList.size() == 2) {
                        ChatResponse secondResponse = (ChatResponse)bufferList.get(1);
                        if (secondResponse != null && secondResponse.getMetadata() != null) {
                            Usage usage = secondResponse.getMetadata().getUsage();
                            if (!UsageCalculator.isEmpty(usage)) {
                                return new ChatResponse(firstResponse.getResults(), this.from(firstResponse.getMetadata(), usage));
                            }
                        }
                    }

                    return firstResponse;
                });
                Flux<ChatResponse> var10000 = chatResponse.flatMap((response) -> this.toolExecutionEligibilityPredicate.isToolExecutionRequired(prompt.getOptions(), response) ? Flux.deferContextual((ctx) -> {
                    ToolExecutionResult toolExecutionResult;
                    try {
                        ToolCallReactiveContextHolder.setContext(ctx);
                        toolExecutionResult = this.toolCallingManager.executeToolCalls(prompt, response);
                    } finally {
                        ToolCallReactiveContextHolder.clearContext();
                    }

                    return toolExecutionResult.returnDirect() ? Flux.just(ChatResponse.builder().from(response).generations(ToolExecutionResult.buildGenerations(toolExecutionResult)).build()) : this.internalStream(new Prompt(toolExecutionResult.conversationHistory(), prompt.getOptions()), response);
                }).subscribeOn(Schedulers.boundedElastic()) : Flux.just(response));
                Objects.requireNonNull(observation);
                Flux<ChatResponse> flux = var10000.doOnError(observation::error).doFinally((s) -> observation.stop()).contextWrite((ctx) -> ctx.put("micrometer.observation", observation));
                MessageAggregator var11 = new MessageAggregator();
                Objects.requireNonNull(observationContext);
                return var11.aggregate(flux, observationContext::setResponse);
            }
        });
    }

    private MultiValueMap<String, String> getAdditionalHttpHeaders(Prompt prompt) {
        Map<String, String> headers = new HashMap(this.defaultOptions.getHttpHeaders());
        if (prompt.getOptions() != null) {
            ChatOptions var4 = prompt.getOptions();
            if (var4 instanceof OpenAiChatOptions) {
                OpenAiChatOptions chatOptions = (OpenAiChatOptions)var4;
                headers.putAll(chatOptions.getHttpHeaders());
            }
        }

        return CollectionUtils.toMultiValueMap((Map)headers.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, (e) -> List.of((String)e.getValue()))));
    }

    private Generation buildGeneration(OpenAiApi.ChatCompletion.Choice choice, Map<String, Object> metadata, OpenAiApi.ChatCompletionRequest request) {
        List<AssistantMessage.ToolCall> toolCalls = choice.message().toolCalls() == null ? List.of() : choice.message().toolCalls().stream().map((toolCall) -> new AssistantMessage.ToolCall(toolCall.id(), "function", toolCall.function().name(), toolCall.function().arguments())).toList();
        ChatGenerationMetadata.Builder generationMetadataBuilder = ChatGenerationMetadata.builder().finishReason(this.getFinishReasonJson(choice.finishReason()));
        List<Media> media = new ArrayList();
        String textContent = choice.message().content();
        OpenAiApi.ChatCompletionMessage.AudioOutput audioOutput = choice.message().audioOutput();
        if (audioOutput != null && StringUtils.hasText(audioOutput.data()) && request.audioParameters() != null) {
            String mimeType = String.format("audio/%s", request.audioParameters().format().name().toLowerCase());
            byte[] audioData = Base64.getDecoder().decode(audioOutput.data());
            Resource resource = new ByteArrayResource(audioData);
            Media.builder().mimeType(MimeTypeUtils.parseMimeType(mimeType)).data(resource).id(audioOutput.id()).build();
            media.add(Media.builder().mimeType(MimeTypeUtils.parseMimeType(mimeType)).data(resource).id(audioOutput.id()).build());
            if (!StringUtils.hasText(textContent)) {
                textContent = audioOutput.transcript();
            }

            generationMetadataBuilder.metadata("audioId", audioOutput.id());
            generationMetadataBuilder.metadata("audioExpiresAt", audioOutput.expiresAt());
        }

        if (Boolean.TRUE.equals(request.logprobs())) {
            generationMetadataBuilder.metadata("logprobs", choice.logprobs());
        }

        AssistantMessage assistantMessage = AssistantMessage.builder().content(textContent).properties(metadata).toolCalls(toolCalls).media(media).build();
        return new Generation(assistantMessage, generationMetadataBuilder.build());
    }

    private String getFinishReasonJson(OpenAiApi.ChatCompletionFinishReason finishReason) {
        return finishReason == null ? "" : finishReason.name();
    }

    private ChatResponseMetadata from(OpenAiApi.ChatCompletion result, RateLimit rateLimit, Usage usage) {
        Assert.notNull(result, "OpenAI ChatCompletionResult must not be null");
        ChatResponseMetadata.Builder builder = ChatResponseMetadata.builder().id(result.id() != null ? result.id() : "").usage(usage).model(result.model() != null ? result.model() : "").keyValue("created", result.created() != null ? result.created() : 0L).keyValue("system-fingerprint", result.systemFingerprint() != null ? result.systemFingerprint() : "");
        if (rateLimit != null) {
            builder.rateLimit(rateLimit);
        }

        return builder.build();
    }

    private ChatResponseMetadata from(ChatResponseMetadata chatResponseMetadata, Usage usage) {
        Assert.notNull(chatResponseMetadata, "OpenAI ChatResponseMetadata must not be null");
        ChatResponseMetadata.Builder builder = ChatResponseMetadata.builder().id(chatResponseMetadata.getId() != null ? chatResponseMetadata.getId() : "").usage(usage).model(chatResponseMetadata.getModel() != null ? chatResponseMetadata.getModel() : "");
        if (chatResponseMetadata.getRateLimit() != null) {
            builder.rateLimit(chatResponseMetadata.getRateLimit());
        }

        return builder.build();
    }

    private OpenAiApi.ChatCompletion chunkToChatCompletion(OpenAiApi.ChatCompletionChunk chunk) {
        List<OpenAiApi.ChatCompletion.Choice> choices = chunk.choices().stream().map((chunkChoice) -> new OpenAiApi.ChatCompletion.Choice(chunkChoice.finishReason(), chunkChoice.index(), chunkChoice.delta(), chunkChoice.logprobs())).toList();
        return new OpenAiApi.ChatCompletion(chunk.id(), choices, chunk.created(), chunk.model(), chunk.serviceTier(), chunk.systemFingerprint(), "chat.completion", chunk.usage());
    }

    private DefaultUsage getDefaultUsage(OpenAiApi.Usage usage) {
        return new DefaultUsage(usage.promptTokens(), usage.completionTokens(), usage.totalTokens(), usage);
    }

    Prompt buildRequestPrompt(Prompt prompt) {
        OpenAiChatOptions runtimeOptions = null;
        if (prompt.getOptions() != null) {
            ChatOptions var4 = prompt.getOptions();
            if (var4 instanceof ToolCallingChatOptions) {
                ToolCallingChatOptions toolCallingChatOptions = (ToolCallingChatOptions)var4;
                runtimeOptions = (OpenAiChatOptions)ModelOptionsUtils.copyToTarget(toolCallingChatOptions, ToolCallingChatOptions.class, OpenAiChatOptions.class);
            } else {
                runtimeOptions = (OpenAiChatOptions)ModelOptionsUtils.copyToTarget(prompt.getOptions(), ChatOptions.class, OpenAiChatOptions.class);
            }
        }

        OpenAiChatOptions requestOptions = (OpenAiChatOptions)ModelOptionsUtils.merge(runtimeOptions, this.defaultOptions, OpenAiChatOptions.class);
        if (runtimeOptions != null) {
            if (runtimeOptions.getTopK() != null) {
                logger.warn("The topK option is not supported by OpenAI chat models. Ignoring.");
            }

            requestOptions.setHttpHeaders(this.mergeHttpHeaders(runtimeOptions.getHttpHeaders(), this.defaultOptions.getHttpHeaders()));
            requestOptions.setInternalToolExecutionEnabled((Boolean)ModelOptionsUtils.mergeOption(runtimeOptions.getInternalToolExecutionEnabled(), this.defaultOptions.getInternalToolExecutionEnabled()));
            requestOptions.setToolNames(ToolCallingChatOptions.mergeToolNames(runtimeOptions.getToolNames(), this.defaultOptions.getToolNames()));
            requestOptions.setToolCallbacks(ToolCallingChatOptions.mergeToolCallbacks(runtimeOptions.getToolCallbacks(), this.defaultOptions.getToolCallbacks()));
            requestOptions.setToolContext(ToolCallingChatOptions.mergeToolContext(runtimeOptions.getToolContext(), this.defaultOptions.getToolContext()));
            requestOptions.setExtraBody(this.mergeExtraBody(runtimeOptions.getExtraBody(), this.defaultOptions.getExtraBody()));
        } else {
            requestOptions.setHttpHeaders(this.defaultOptions.getHttpHeaders());
            requestOptions.setInternalToolExecutionEnabled(this.defaultOptions.getInternalToolExecutionEnabled());
            requestOptions.setToolNames(this.defaultOptions.getToolNames());
            requestOptions.setToolCallbacks(this.defaultOptions.getToolCallbacks());
            requestOptions.setToolContext(this.defaultOptions.getToolContext());
            requestOptions.setExtraBody(this.defaultOptions.getExtraBody());
        }

        ToolCallingChatOptions.validateToolCallbacks(requestOptions.getToolCallbacks());
        return new Prompt(prompt.getInstructions(), requestOptions);
    }

    private Map<String, String> mergeHttpHeaders(Map<String, String> runtimeHttpHeaders, Map<String, String> defaultHttpHeaders) {
        HashMap<String, String> mergedHttpHeaders = new HashMap(defaultHttpHeaders);
        mergedHttpHeaders.putAll(runtimeHttpHeaders);
        return mergedHttpHeaders;
    }

    private Map<String, Object> mergeExtraBody(Map<String, Object> runtimeExtraBody, Map<String, Object> defaultExtraBody) {
        if (defaultExtraBody == null && runtimeExtraBody == null) {
            return null;
        } else {
            HashMap<String, Object> merged = new HashMap();
            if (defaultExtraBody != null) {
                merged.putAll(defaultExtraBody);
            }

            if (runtimeExtraBody != null) {
                merged.putAll(runtimeExtraBody);
            }

            return merged.isEmpty() ? null : merged;
        }
    }

    OpenAiApi.ChatCompletionRequest createRequest(Prompt prompt, boolean stream) {
        List<OpenAiApi.ChatCompletionMessage> chatCompletionMessages = prompt.getInstructions().stream().map((message) -> {
            if (message.getMessageType() != MessageType.USER && message.getMessageType() != MessageType.SYSTEM) {
                if (message.getMessageType() == MessageType.ASSISTANT) {
                    AssistantMessage assistantMessage = (AssistantMessage)message;
                    List<OpenAiApi.ChatCompletionMessage.ToolCall> toolCalls = null;
                    if (!CollectionUtils.isEmpty(assistantMessage.getToolCalls())) {
                        toolCalls = assistantMessage.getToolCalls().stream().map((toolCall) -> {
                            OpenAiApi.ChatCompletionMessage.ChatCompletionFunction function = new OpenAiApi.ChatCompletionMessage.ChatCompletionFunction(toolCall.name(), toolCall.arguments());
                            return new OpenAiApi.ChatCompletionMessage.ToolCall(toolCall.id(), toolCall.type(), function);
                        }).toList();
                    }

                    OpenAiApi.ChatCompletionMessage.AudioOutput audioOutput = null;
                    if (!CollectionUtils.isEmpty(assistantMessage.getMedia())) {
                        Assert.isTrue(assistantMessage.getMedia().size() == 1, "Only one media content is supported for assistant messages");
                        audioOutput = new OpenAiApi.ChatCompletionMessage.AudioOutput(((Media)assistantMessage.getMedia().get(0)).getId(), (String)null, (Long)null, (String)null);
                    }

                    return List.of(new OpenAiApi.ChatCompletionMessage(assistantMessage.getText(), Role.ASSISTANT, (String)null, (String)null, toolCalls, (String)null, audioOutput, (List)null, (String)null));
                } else if (message.getMessageType() == MessageType.TOOL) {
                    ToolResponseMessage toolMessage = (ToolResponseMessage)message;
                    toolMessage.getResponses().forEach((response) -> Assert.isTrue(response.id() != null, "ToolResponseMessage must have an id"));
                    return toolMessage.getResponses().stream().map((tr) -> new OpenAiApi.ChatCompletionMessage(tr.responseData(), Role.TOOL, tr.name(), tr.id(), (List)null, (String)null, (OpenAiApi.ChatCompletionMessage.AudioOutput)null, (List)null, (String)null)).toList();
                } else {
                    throw new IllegalArgumentException("Unsupported message type: " + String.valueOf(message.getMessageType()));
                }
            } else {
                Object content = message.getText();
                if (message instanceof UserMessage) {
                    UserMessage userMessage = (UserMessage)message;
                    if (!CollectionUtils.isEmpty(userMessage.getMedia())) {
                        List<OpenAiApi.ChatCompletionMessage.MediaContent> contentList = new ArrayList(List.of(new OpenAiApi.ChatCompletionMessage.MediaContent(message.getText())));
                        contentList.addAll(userMessage.getMedia().stream().map(this::mapToMediaContent).toList());
                        content = contentList;
                    }
                }

                return List.of(new OpenAiApi.ChatCompletionMessage(content, Role.valueOf(message.getMessageType().name())));
            }
        }).flatMap(Collection::stream).toList();
        OpenAiApi.ChatCompletionRequest request = new OpenAiApi.ChatCompletionRequest(chatCompletionMessages, stream);
        OpenAiChatOptions requestOptions = (OpenAiChatOptions)prompt.getOptions();
        request = (OpenAiApi.ChatCompletionRequest)ModelOptionsUtils.merge(requestOptions, request, OpenAiApi.ChatCompletionRequest.class);
        List<ToolDefinition> toolDefinitions = this.toolCallingManager.resolveToolDefinitions(requestOptions);
        if (!CollectionUtils.isEmpty(toolDefinitions)) {
            request = (OpenAiApi.ChatCompletionRequest)ModelOptionsUtils.merge(OpenAiChatOptions.builder().tools(this.getFunctionTools(toolDefinitions)).build(), request, OpenAiApi.ChatCompletionRequest.class);
        }

        if (request.streamOptions() != null && !stream) {
            logger.warn("Removing streamOptions from the request as it is not a streaming request!");
            request = request.streamOptions((OpenAiApi.ChatCompletionRequest.StreamOptions)null);
        }

        return request;
    }

    private OpenAiApi.ChatCompletionMessage.MediaContent mapToMediaContent(Media media) {
        MimeType mimeType = media.getMimeType();
        String mimeTypeString = mimeType.toString();
        // 阿里巴巴的多模态模型，在兼容OpenAI协议时，对于音频类文件，format字段无所谓，需要在data中体现mime类型，故统一处理
        if (mimeTypeString.contains("audio/")) {
            return new OpenAiApi.ChatCompletionMessage.MediaContent(new OpenAiApi.ChatCompletionMessage.MediaContent.InputAudio(this.fromMediaData(mimeType, media.getData()), Format.MP3));
        } else {
            return MimeTypeUtils.parseMimeType("application/pdf").equals(mimeType) ? new OpenAiApi.ChatCompletionMessage.MediaContent(new OpenAiApi.ChatCompletionMessage.MediaContent.InputFile(media.getName(), this.fromMediaData(media.getMimeType(), media.getData()))) : new OpenAiApi.ChatCompletionMessage.MediaContent(new OpenAiApi.ChatCompletionMessage.MediaContent.ImageUrl(this.fromMediaData(media.getMimeType(), media.getData())));
        }
    }

    private String fromAudioData(Object audioData) {
        if (audioData instanceof byte[] bytes) {
            return Base64.getEncoder().encodeToString(bytes);
        } else {
            throw new IllegalArgumentException("Unsupported audio data type: " + audioData.getClass().getSimpleName());
        }
    }

    private String fromMediaData(MimeType mimeType, Object mediaContentData) {
        if (mediaContentData instanceof byte[] bytes) {
            return String.format("data:%s;base64,%s", mimeType.toString(), Base64.getEncoder().encodeToString(bytes));
        } else if (mediaContentData instanceof String text) {
            return text;
        } else {
            throw new IllegalArgumentException("Unsupported media data type: " + mediaContentData.getClass().getSimpleName());
        }
    }

    private List<OpenAiApi.FunctionTool> getFunctionTools(List<ToolDefinition> toolDefinitions) {
        return toolDefinitions.stream().map((toolDefinition) -> {
            OpenAiApi.FunctionTool.Function function = new OpenAiApi.FunctionTool.Function(toolDefinition.description(), toolDefinition.name(), toolDefinition.inputSchema());
            return new OpenAiApi.FunctionTool(function);
        }).toList();
    }

    public ChatOptions getDefaultOptions() {
        return OpenAiChatOptions.fromOptions(this.defaultOptions);
    }

    public String toString() {
        return "MultiModalOpenAiChatModel [defaultOptions=" + String.valueOf(this.defaultOptions) + "]";
    }

    public void setObservationConvention(ChatModelObservationConvention observationConvention) {
        Assert.notNull(observationConvention, "observationConvention cannot be null");
        this.observationConvention = observationConvention;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder mutate() {
        return new Builder(this);
    }

    public AlibabaOpenAiChatModel clone() {
        return this.mutate().build();
    }

    public static final class Builder {
        private OpenAiApi openAiApi;
        private OpenAiChatOptions defaultOptions;
        private ToolCallingManager toolCallingManager;
        private ToolExecutionEligibilityPredicate toolExecutionEligibilityPredicate;
        private RetryTemplate retryTemplate;
        private ObservationRegistry observationRegistry;

        public Builder(AlibabaOpenAiChatModel model) {
            this.defaultOptions = OpenAiChatOptions.builder().model(OpenAiApi.DEFAULT_CHAT_MODEL).temperature(0.7).build();
            this.toolExecutionEligibilityPredicate = new DefaultToolExecutionEligibilityPredicate();
            this.retryTemplate = RetryUtils.DEFAULT_RETRY_TEMPLATE;
            this.observationRegistry = ObservationRegistry.NOOP;
            this.openAiApi = model.openAiApi;
            this.defaultOptions = model.defaultOptions;
            this.toolCallingManager = model.toolCallingManager;
            this.toolExecutionEligibilityPredicate = model.toolExecutionEligibilityPredicate;
            this.retryTemplate = model.retryTemplate;
            this.observationRegistry = model.observationRegistry;
        }

        private Builder() {
            this.defaultOptions = OpenAiChatOptions.builder().model(OpenAiApi.DEFAULT_CHAT_MODEL).temperature(0.7).build();
            this.toolExecutionEligibilityPredicate = new DefaultToolExecutionEligibilityPredicate();
            this.retryTemplate = RetryUtils.DEFAULT_RETRY_TEMPLATE;
            this.observationRegistry = ObservationRegistry.NOOP;
        }

        public Builder openAiApi(OpenAiApi openAiApi) {
            this.openAiApi = openAiApi;
            return this;
        }

        public Builder defaultOptions(OpenAiChatOptions defaultOptions) {
            this.defaultOptions = defaultOptions;
            return this;
        }

        public Builder toolCallingManager(ToolCallingManager toolCallingManager) {
            this.toolCallingManager = toolCallingManager;
            return this;
        }

        public Builder toolExecutionEligibilityPredicate(ToolExecutionEligibilityPredicate toolExecutionEligibilityPredicate) {
            this.toolExecutionEligibilityPredicate = toolExecutionEligibilityPredicate;
            return this;
        }

        public Builder retryTemplate(RetryTemplate retryTemplate) {
            this.retryTemplate = retryTemplate;
            return this;
        }

        public Builder observationRegistry(ObservationRegistry observationRegistry) {
            this.observationRegistry = observationRegistry;
            return this;
        }

        public AlibabaOpenAiChatModel build() {
            return this.toolCallingManager != null ? new AlibabaOpenAiChatModel(this.openAiApi, this.defaultOptions, this.toolCallingManager, this.retryTemplate, this.observationRegistry, this.toolExecutionEligibilityPredicate) : new AlibabaOpenAiChatModel(this.openAiApi, this.defaultOptions, AlibabaOpenAiChatModel.DEFAULT_TOOL_CALLING_MANAGER, this.retryTemplate, this.observationRegistry, this.toolExecutionEligibilityPredicate);
        }
    }
}
