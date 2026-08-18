package com.company.employeemanagement.ai.agent.tool.impl;

import com.company.employeemanagement.ai.agent.tool.AgentToolContext;
import com.company.employeemanagement.dto.response.EmployeeResponse;
import com.company.employeemanagement.dto.response.PageResponse;
import com.company.employeemanagement.service.EmployeeService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Tool: search_employees
 * Searches for employees by keyword/name. ADMIN, HR, MANAGER only.
 * Excludes salary field from results.
 */
@Component
public class SearchEmployeesTool extends AbstractAgentTool {

    private final EmployeeService employeeService;

    public SearchEmployeesTool(final EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @Override public String getName() { return "search_employees"; }

    @Override public String getDescription() {
        return "Use this tool whenever the user asks about employees in the organisation — "
               + "to find employees by name, search by department, get someone's employee ID, "
               + "or list employees in a specific team. "
               + "Trigger phrases: 'who works in [department]', 'find employee [name]', "
               + "'search for [name]', 'who is in the Engineering team', "
               + "'list all employees', 'employees in HR'. "
               + "Parameters: keyword (optional — name, department, or search term). "
               + "Never answer employee lookup questions from general knowledge — always call this tool.";
    }

    @Override public String getParameterSchema() {
        return """
               {"type":"object","properties":{"keyword":{"type":"string","description":"Name or search term"}},
               "required":[]}""";
    }

    @Override public Set<String> getAllowedRoles() {
        return Set.of("ROLE_ADMIN", "ROLE_HR", "ROLE_MANAGER");
    }

    @Override public boolean isRequiresConfirmation() { return false; }
    @Override public boolean isReadOnly() { return true; }

    @Override
    protected String doExecute(final Map<String, String> args, final AgentToolContext context) {
        String keyword = arg(args, "keyword", "");
        PageResponse<EmployeeResponse> page = employeeService.findAll(
                keyword.isBlank() ? null : keyword, null, null, PageRequest.of(0, 20));

        if (page.content().isEmpty()) {
            return "No employees found" + (keyword.isBlank() ? "" : " matching '" + keyword + "'") + ".";
        }

        return "Found " + page.totalElements() + " employee(s):\n" +
               page.content().stream()
                   .map(GetCurrentEmployeeTool::formatEmployee)
                   .collect(Collectors.joining("\n"));
    }
}
