package com.company.employeemanagement.ai.rag.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration class that enables {@link RagProperties} as a
 * {@code @ConfigurationProperties} bean.
 *
 * @author Employee Management Portal Team
 */
@Configuration
@EnableConfigurationProperties(RagProperties.class)
public class RagConfig {
}
