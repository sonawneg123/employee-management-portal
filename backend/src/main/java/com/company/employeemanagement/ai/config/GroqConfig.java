package com.company.employeemanagement.ai.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

/**
 * Spring configuration for the Groq AI integration.
 *
 * <p>Registers {@link GroqProperties} as a configuration-properties bean and
 * provides a pre-configured {@link RestClient} that the {@link com.company.employeemanagement.ai.client.GroqClient}
 * uses to call the Groq API.
 *
 * <p>The {@code RestClient} bean is scoped to the AI module only; it does not
 * affect any other HTTP client in the application.
 *
 * @author Employee Management Portal Team
 */
@Configuration
@EnableConfigurationProperties(GroqProperties.class)
public class GroqConfig {

    private final GroqProperties groqProperties;

    /**
     * Constructs the configuration with the bound Groq properties.
     *
     * @param groqProperties the bound Groq configuration properties
     */
    public GroqConfig(final GroqProperties groqProperties) {
        this.groqProperties = groqProperties;
    }

    /**
     * Creates a {@link RestClient} pre-configured for the Groq API.
     *
     * <p>The {@code Authorization} header is set at build time using the
     * configured API key so that individual requests do not need to set it
     * manually. The key is never logged.
     *
     * @return the configured {@link RestClient}
     */
    @Bean("groqRestClient")
    public RestClient groqRestClient() {
        return RestClient.builder()
                .baseUrl(groqProperties.getBaseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + groqProperties.getApiKey())
                .build();
    }
}
