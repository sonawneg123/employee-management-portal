package com.company.employeemanagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


/**
 * Entry point for the Employee Management Portal application.
 *
 * <p>Bootstraps the Spring Boot context, enables JPA auditing for
 * automatic population of {@code createdAt}, {@code updatedAt},
 * {@code createdBy}, and {@code updatedBy} fields on all entities
 * that extend {@link com.company.employeemanagement.entity.BaseEntity}.
 *
 * @author Employee Management Portal Team
 * @version 1.0.0
 */
@SpringBootApplication

public class EmployeeManagementApplication {

    /**
     * Application main method.
     *
     * @param args command-line arguments passed to the JVM
     */
    public static void main(final String[] args) {
        SpringApplication.run(EmployeeManagementApplication.class, args);
    }
}
