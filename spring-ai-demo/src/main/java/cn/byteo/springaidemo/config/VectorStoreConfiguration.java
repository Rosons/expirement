package cn.byteo.springaidemo.config;

import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * <p>这里描述类的主要功能</p>
 *
 * @author Roson
 * @file VectoreStoreConfiguration
 * @since 2026/4/16
 */
@Configuration
public class VectorStoreConfiguration {

    @Bean
    VectorStore simpleVectorStore(OpenAiEmbeddingModel embeddingModel) {
        // SpringAI提供的本地化模拟向量库
        return SimpleVectorStore
                .builder(embeddingModel)
                .build();
    }
}
