package com.company.employeemanagement.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for the AI review async task executor.
 *
 * <p>All values have safe defaults that can be overridden via environment variables
 * or {@code application.properties}.
 *
 * @author Employee Management Portal Team
 */
@Component
@ConfigurationProperties(prefix = "app.ai-review.async")
public class AiReviewAsyncProperties {

    /** Core number of threads kept alive even when idle. Default: 2. */
    private int corePoolSize = 2;

    /** Maximum number of threads created under high load. Default: 5. */
    private int maxPoolSize = 5;

    /** Queue capacity before new tasks are rejected. Default: 50. */
    private int queueCapacity = 50;

    /** Prefix used for thread names (useful in logs). */
    private String threadNamePrefix = "ai-review-";

    /** Keep-alive time in seconds for threads above corePoolSize. Default: 60. */
    private int keepAliveSeconds = 60;

    public int getCorePoolSize()          { return corePoolSize; }
    public void setCorePoolSize(int v)    { this.corePoolSize = v; }

    public int getMaxPoolSize()           { return maxPoolSize; }
    public void setMaxPoolSize(int v)     { this.maxPoolSize = v; }

    public int getQueueCapacity()         { return queueCapacity; }
    public void setQueueCapacity(int v)   { this.queueCapacity = v; }

    public String getThreadNamePrefix()           { return threadNamePrefix; }
    public void setThreadNamePrefix(String v)     { this.threadNamePrefix = v; }

    public int getKeepAliveSeconds()              { return keepAliveSeconds; }
    public void setKeepAliveSeconds(int v)        { this.keepAliveSeconds = v; }
}
