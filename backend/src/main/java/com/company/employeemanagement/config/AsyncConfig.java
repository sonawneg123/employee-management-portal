package com.company.employeemanagement.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Spring {@link EnableAsync} configuration for the AI review background pipeline.
 *
 * <p>A dedicated {@link ThreadPoolTaskExecutor} named {@code aiReviewExecutor} is
 * created so that AI review threads are isolated from the default Spring async
 * executor and can be tuned independently via
 * {@link AiReviewAsyncProperties}.
 *
 * <p>HTTP request threads are never blocked waiting for Groq — the executor runs
 * tasks on separate threads after the submission transaction has committed.
 *
 * @author Employee Management Portal Team
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    private final AiReviewAsyncProperties asyncProperties;

    /**
     * Constructs the configuration with the bound async properties.
     *
     * @param asyncProperties the AI review executor configuration
     */
    public AsyncConfig(final AiReviewAsyncProperties asyncProperties) {
        this.asyncProperties = asyncProperties;
    }

    /**
     * Creates the dedicated task executor for AI review background processing.
     *
     * <p>Configuration:
     * <ul>
     *   <li>Core pool — minimum threads kept alive.</li>
     *   <li>Max pool — maximum threads under peak load.</li>
     *   <li>Queue — task buffer before thread creation kicks in.</li>
     *   <li>ThreadNamePrefix — identifies threads in log output.</li>
     *   <li>WaitForTasksToCompleteOnShutdown — allows in-flight reviews to
     *       complete gracefully during application shutdown.</li>
     * </ul>
     *
     * @return the configured executor
     */
    @Bean(name = "aiReviewExecutor")
    public Executor aiReviewExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(asyncProperties.getCorePoolSize());
        executor.setMaxPoolSize(asyncProperties.getMaxPoolSize());
        executor.setQueueCapacity(asyncProperties.getQueueCapacity());
        executor.setThreadNamePrefix(asyncProperties.getThreadNamePrefix());
        executor.setKeepAliveSeconds(asyncProperties.getKeepAliveSeconds());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
