package com.company.employeemanagement.ai.agent.tool.impl;

import com.company.employeemanagement.ai.agent.tool.AgentToolContext;
import com.company.employeemanagement.ai.agent.tool.AiAgentTool;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;

/**
 * Abstract base class for all {@link AiAgentTool} implementations.
 *
 * <p>Provides shared JSON argument parsing and error handling so that concrete
 * tools only need to implement their domain logic.
 *
 * @author Employee Management Portal Team
 */
public abstract class AbstractAgentTool implements AiAgentTool {

    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Parses the raw JSON arguments string into a {@code Map<String, String>}.
     * Returns an empty map on parse failure.
     *
     * @param argumentsJson the JSON string from the LLM tool call
     * @return parsed arguments, or empty map if parsing fails
     */
    protected Map<String, String> parseArgs(final String argumentsJson) {
        if (argumentsJson == null || argumentsJson.isBlank() || "{}".equals(argumentsJson.trim())) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(argumentsJson, new TypeReference<Map<String, String>>() {});
        } catch (Exception e) {
            log.warn("Tool '{}': failed to parse arguments JSON '{}': {}", getName(), argumentsJson, e.getMessage());
            return Collections.emptyMap();
        }
    }

    /**
     * Returns a named string argument, or the default if absent or blank.
     *
     * @param args         the parsed argument map
     * @param key          the argument name
     * @param defaultValue the fallback value
     * @return the argument value or default
     */
    protected String arg(final Map<String, String> args, final String key, final String defaultValue) {
        String v = args.get(key);
        return (v == null || v.isBlank()) ? defaultValue : v.trim();
    }

    /**
     * Returns a named string argument, or {@code null} if absent.
     *
     * @param args the parsed argument map
     * @param key  the argument name
     * @return the argument value, or {@code null}
     */
    protected String arg(final Map<String, String> args, final String key) {
        return arg(args, key, null);
    }

    /**
     * Template execute method: delegates to {@link #doExecute} and wraps
     * unexpected exceptions in a safe error string.
     */
    @Override
    public final String execute(final String argumentsJson, final AgentToolContext context) {
        log.info("TOOL CALL tool={} user={} role={}", getName(), context.username(), context.primaryRole());
        try {
            Map<String, String> args = parseArgs(argumentsJson);
            String result = doExecute(args, context);
            log.info("TOOL RESULT tool={} resultLength={}", getName(), result.length());
            return result;
        } catch (com.company.employeemanagement.exception.AccessDeniedException e) {
            log.warn("Tool '{}': access denied for user={}: {}", getName(), context.username(), e.getMessage());
            return "Access denied: " + e.getMessage();
        } catch (com.company.employeemanagement.exception.ResourceNotFoundException e) {
            log.warn("Tool '{}': resource not found for user={}: {}", getName(), context.username(), e.getMessage());
            return "Not found: " + e.getMessage();
        } catch (Exception e) {
            log.error("Tool '{}': unexpected error for user={}: {}", getName(), context.username(), e.getMessage(), e);
            return "Error executing tool " + getName() + ": " + e.getMessage();
        }
    }

    /**
     * Implement the actual tool logic here.
     *
     * @param args    parsed argument map
     * @param context the execution context
     * @return a string result (plain text or short JSON)
     */
    protected abstract String doExecute(Map<String, String> args, AgentToolContext context);
}
