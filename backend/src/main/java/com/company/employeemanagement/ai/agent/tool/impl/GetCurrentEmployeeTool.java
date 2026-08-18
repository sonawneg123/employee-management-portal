package com.company.employeemanagement.ai.agent.tool.impl;

import com.company.employeemanagement.ai.agent.tool.AgentToolContext;
import com.company.employeemanagement.dto.response.EmployeeResponse;
import com.company.employeemanagement.security.SecurityUtils;
import com.company.employeemanagement.service.EmployeeService;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/**
 * Tool: get_current_employee
 * Returns the profile of the currently authenticated user's linked employee record.
 * Available to all authenticated roles.
 */
@Component
public class GetCurrentEmployeeTool extends AbstractAgentTool {

    private final SecurityUtils securityUtils;
    private final EmployeeService employeeService;

    public GetCurrentEmployeeTool(final SecurityUtils securityUtils,
                                   final EmployeeService employeeService) {
        this.securityUtils = securityUtils;
        this.employeeService = employeeService;
    }

    @Override public String getName() { return "get_current_employee"; }

    @Override public String getDescription() {
        return "Use this tool whenever the user asks about their own profile, personal information, "
               + "job title, department, employee code, email, joining date, or employment status. "
               + "Trigger phrases: 'my profile', 'who am I', 'my information', 'my employee record', "
               + "'what department am I in', 'what is my job title'. "
               + "Never answer these questions from general knowledge — always call this tool.";
    }

    @Override public String getParameterSchema() { return "{}"; }

    @Override public Set<String> getAllowedRoles() {
        return Set.of("ROLE_ADMIN", "ROLE_HR", "ROLE_MANAGER", "ROLE_EMPLOYEE");
    }

    @Override public boolean isRequiresConfirmation() { return false; }
    @Override public boolean isReadOnly() { return true; }

    @Override
    protected String doExecute(final Map<String, String> args, final AgentToolContext context) {
        if (context.currentEmployee() == null) {
            return "No employee record is linked to the current user account.";
        }
        EmployeeResponse emp = employeeService.findById(context.currentEmployee().getId());
        // Exclude salary — sensitive field
        return formatEmployee(emp);
    }

    public static String formatEmployee(final EmployeeResponse emp) {
        return String.format(
                "Employee: %s %s | Code: %s | Job: %s | Dept: %s | Status: %s | Joined: %s | Email: %s",
                emp.firstName(), emp.lastName(), emp.employeeCode(),
                emp.jobTitle(), emp.departmentName(), emp.status(),
                emp.dateOfJoining(), emp.email()
        );
    }
}
