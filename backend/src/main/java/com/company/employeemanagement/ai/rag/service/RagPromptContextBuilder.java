package com.company.employeemanagement.ai.rag.service;

import com.company.employeemanagement.ai.rag.dto.KnowledgeSearchResult;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Transforms a list of {@link KnowledgeSearchResult} chunks retrieved from the
 * Phase 2A knowledge base into a clearly delimited context block that can be
 * injected into the Groq system prompt.
 *
 * <h2>Output format (when chunks are present)</h2>
 * <pre>
 * KNOWLEDGE BASE CONTEXT
 *
 * [Document: Employee Leave Policy]
 * Employees are entitled to annual leave according to company policy.
 * ...
 *
 * [Document: Remote Work Policy]
 * Employees may work remotely up to three days per week.
 * ...
 *
 * END KNOWLEDGE BASE CONTEXT
 * </pre>
 *
 * <h2>Output format (when no chunks are present)</h2>
 * <pre>
 * No relevant company knowledge was found for this question.
 * Do not claim that general knowledge is an official company policy.
 * </pre>
 *
 * <p>Database IDs (document UUID, chunk UUID) are intentionally omitted from the
 * context block — they add no semantic value for the language model and would
 * merely consume context-window tokens.
 *
 * <p>This component is stateless and thread-safe. It is designed to be injected
 * into {@link com.company.employeemanagement.ai.service.AiChatService} without
 * coupling that service directly to any storage implementation.
 *
 * @author Employee Management Portal Team
 */
@Component
public class RagPromptContextBuilder {

    private static final String SECTION_HEADER = "KNOWLEDGE BASE CONTEXT";
    private static final String SECTION_FOOTER = "END KNOWLEDGE BASE CONTEXT";

    private static final String NO_CONTEXT_NOTICE =
            "No relevant company knowledge was found for this question.\n"
            + "Do not claim that general knowledge is an official company policy.";

    /**
     * Builds the RAG context section to be appended to the system prompt.
     *
     * <p>When {@code chunks} is empty, a no-context notice is returned that
     * instructs the model not to fabricate company-specific policy.
     *
     * @param chunks the ordered list of retrieved knowledge chunks; must not be {@code null}
     * @return a non-null, non-empty context string suitable for inclusion in a system prompt
     */
    public String buildContextSection(final List<KnowledgeSearchResult> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return NO_CONTEXT_NOTICE;
        }

        final StringBuilder sb = new StringBuilder();
        sb.append(SECTION_HEADER).append("\n");

        String lastTitle = null;
        for (final KnowledgeSearchResult chunk : chunks) {
            final String title = chunk.documentTitle();

            // Emit a document heading whenever the document changes
            if (!title.equals(lastTitle)) {
                sb.append("\n[Document: ").append(title).append("]\n");
                lastTitle = title;
            }

            sb.append(chunk.chunkContent().trim()).append("\n");
        }

        sb.append("\n").append(SECTION_FOOTER);
        return sb.toString();
    }
}
