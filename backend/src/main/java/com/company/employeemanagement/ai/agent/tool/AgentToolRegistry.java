package com.company.employeemanagement.ai.agent.tool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Central registry of all {@link AiAgentTool} instances available to the agent.
 *
 * <p>Spring collects all {@code @Component} / {@code @Service} beans that implement
 * {@link AiAgentTool} and injects them as a list. This registry indexes them by
 * {@link AiAgentTool#getName()} for O(1) lookup during agent execution.
 *
 * @author Employee Management Portal Team
 */
@Component
public class AgentToolRegistry {

    private static final Logger log = LoggerFactory.getLogger(AgentToolRegistry.class);

    /** Ordered map from tool name → tool, preserving registration order for prompt generation. */
    private final Map<String, AiAgentTool> toolsByName;

    /**
     * Constructs the registry by indexing the injected tool list.
     *
     * @param tools all {@link AiAgentTool} beans detected by Spring
     */
    public AgentToolRegistry(final List<AiAgentTool> tools) {
        Map<String, AiAgentTool> map = new LinkedHashMap<>();
        for (AiAgentTool tool : tools) {
            if (map.containsKey(tool.getName())) {
                log.warn("Duplicate tool name '{}' — second registration ignored. Fix tool configuration.",
                        tool.getName());
            } else {
                map.put(tool.getName(), tool);
                log.debug("Registered agent tool: {} (readOnly={}, requiresConfirmation={})",
                        tool.getName(), tool.isReadOnly(), tool.isRequiresConfirmation());
            }
        }
        this.toolsByName = Collections.unmodifiableMap(map);
        log.info("AgentToolRegistry initialised — {} tool(s) registered", toolsByName.size());
    }

    /**
     * Looks up a tool by its name.
     *
     * @param name the tool name (lower_snake_case)
     * @return an {@link Optional} containing the tool, or empty if not found
     */
    public Optional<AiAgentTool> findByName(final String name) {
        return Optional.ofNullable(toolsByName.get(name));
    }

    /**
     * Returns all registered tools as an unmodifiable collection.
     *
     * @return all tools
     */
    public Collection<AiAgentTool> allTools() {
        return toolsByName.values();
    }

    /**
     * Returns tools accessible to at least one of the given roles.
     *
     * @param roles the Spring Security role strings held by the current user
     * @return tools the user is authorised to use
     */
    public List<AiAgentTool> toolsForRoles(final Collection<String> roles) {
        return toolsByName.values().stream()
                .filter(t -> t.getAllowedRoles().stream().anyMatch(roles::contains))
                .toList();
    }
}
