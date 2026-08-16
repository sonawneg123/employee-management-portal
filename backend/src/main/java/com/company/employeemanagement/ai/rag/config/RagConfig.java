package com.company.employeemanagement.ai.rag.config;

import com.company.employeemanagement.ai.rag.service.DatabaseKnowledgeRetrievalService;
import com.company.employeemanagement.ai.rag.service.KnowledgeRetrievalService;
import com.company.employeemanagement.ai.rag.service.VectorKnowledgeRetrievalService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Spring configuration class that:
 * <ol>
 *   <li>Enables {@link RagProperties} as a {@code @ConfigurationProperties} bean.</li>
 *   <li>Selects the active {@link KnowledgeRetrievalService} implementation based
 *       on the {@code ai.rag.retrieval-strategy} property:
 *       <ul>
 *         <li>{@code vector}   → {@link VectorKnowledgeRetrievalService} (default)</li>
 *         <li>{@code database} → {@link DatabaseKnowledgeRetrievalService}</li>
 *       </ul>
 *   </li>
 * </ol>
 *
 * <p>Both implementations remain in the application context. The {@code @Primary}
 * bean is selected automatically by {@code AiChatService} which depends only on
 * the {@link KnowledgeRetrievalService} interface.
 *
 * @author Employee Management Portal Team
 */
@Configuration
@EnableConfigurationProperties(RagProperties.class)
public class RagConfig {

    private static final Logger log = LoggerFactory.getLogger(RagConfig.class);

    /**
     * Exposes the active {@link KnowledgeRetrievalService} as the {@code @Primary} bean.
     *
     * <p>When {@code ai.rag.retrieval-strategy=vector} (default) the vector service is
     * returned. When set to {@code database} the keyword-based service is returned.
     *
     * @param vectorService   the semantic vector retrieval implementation
     * @param databaseService the keyword database retrieval implementation
     * @param ragProperties   RAG configuration (contains the strategy choice)
     * @return the active retrieval service
     */
    @Bean
    @Primary
    public KnowledgeRetrievalService activeKnowledgeRetrievalService(
            final VectorKnowledgeRetrievalService   vectorService,
            final DatabaseKnowledgeRetrievalService databaseService,
            final RagProperties                     ragProperties) {

        final String strategy = ragProperties.getRetrievalStrategy();
        if ("database".equalsIgnoreCase(strategy)) {
            log.info("RAG retrieval strategy: DATABASE (keyword LIKE search)");
            return databaseService;
        }
        // Default: vector
        log.info("RAG retrieval strategy: VECTOR (semantic embedding similarity)");
        return vectorService;
    }
}
