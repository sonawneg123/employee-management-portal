package com.company.employeemanagement.ai.rag.controller;

import com.company.employeemanagement.ai.rag.dto.IngestDocumentRequest;
import com.company.employeemanagement.ai.rag.dto.KnowledgeDocumentResponse;
import com.company.employeemanagement.ai.rag.dto.KnowledgeSearchRequest;
import com.company.employeemanagement.ai.rag.dto.KnowledgeSearchResult;
import com.company.employeemanagement.ai.rag.entity.enums.KnowledgeDocumentStatus;
import com.company.employeemanagement.ai.rag.entity.enums.KnowledgeSourceType;
import com.company.employeemanagement.ai.rag.exception.RagException;
import com.company.employeemanagement.ai.rag.service.KnowledgeIngestionService;
import com.company.employeemanagement.ai.rag.service.KnowledgeRetrievalService;
import com.company.employeemanagement.exception.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for {@link KnowledgeController} using standalone MockMvc.
 *
 * <p>Security (JWT/role checks) is enforced at the URL-rule level by
 * {@link com.company.employeemanagement.config.SecurityConfig}; those rules are
 * tested separately via integration tests. This class focuses on:
 * <ul>
 *   <li>Happy-path CRUD and search responses</li>
 *   <li>Validation failures (400)</li>
 *   <li>Not-found propagation (400 from {@link RagException})</li>
 * </ul>
 *
 * @author Employee Management Portal Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("KnowledgeController")
class KnowledgeControllerTest {

    @Mock
    private KnowledgeIngestionService ingestionService;

    @Mock
    private KnowledgeRetrievalService retrievalService;

    @InjectMocks
    private KnowledgeController controller;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /ai/rag/documents — ingest
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /ai/rag/documents — ingest document")
    class IngestDocument {

        @Test
        @DisplayName("valid request returns 201 with document response")
        void validRequestReturns201() throws Exception {
            IngestDocumentRequest request = new IngestDocumentRequest(
                    "Remote Work Policy", "Desc", KnowledgeSourceType.POLICY, null,
                    "Full policy content here"
            );
            KnowledgeDocumentResponse response = buildDocResponse("Remote Work Policy");
            when(ingestionService.ingestDocument(any())).thenReturn(response);

            mockMvc.perform(post("/ai/rag/documents")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.title", is("Remote Work Policy")))
                    .andExpect(jsonPath("$.status", is("ACTIVE")));
        }

        @Test
        @DisplayName("missing title returns 400")
        void missingTitleReturns400() throws Exception {
            String body = """
                    {"description":"desc","sourceType":"POLICY","content":"content here"}
                    """;
            mockMvc.perform(post("/ai/rag/documents")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("missing content returns 400")
        void missingContentReturns400() throws Exception {
            String body = """
                    {"title":"Title","sourceType":"POLICY"}
                    """;
            mockMvc.perform(post("/ai/rag/documents")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("missing sourceType returns 400")
        void missingSourceTypeReturns400() throws Exception {
            String body = """
                    {"title":"Title","content":"content here"}
                    """;
            mockMvc.perform(post("/ai/rag/documents")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("service throws RagException → returns 400")
        void serviceRagExceptionReturns400() throws Exception {
            // Use valid content that passes @NotBlank so the service is actually called
            IngestDocumentRequest request = new IngestDocumentRequest(
                    "Title", null, KnowledgeSourceType.OTHER, null, "non-blank content"
            );
            when(ingestionService.ingestDocument(any()))
                    .thenThrow(new RagException("content blank after trimming"));

            mockMvc.perform(post("/ai/rag/documents")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /ai/rag/documents — list
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /ai/rag/documents — list all")
    class ListDocuments {

        @Test
        @DisplayName("returns 200 with list of documents")
        void returnsDocumentList() throws Exception {
            when(ingestionService.listAll()).thenReturn(List.of(
                    buildDocResponse("Policy A"),
                    buildDocResponse("Handbook B")
            ));

            mockMvc.perform(get("/ai/rag/documents"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].title", is("Policy A")))
                    .andExpect(jsonPath("$[1].title", is("Handbook B")));
        }

        @Test
        @DisplayName("returns 200 with empty list when no documents exist")
        void returnsEmptyList() throws Exception {
            when(ingestionService.listAll()).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/ai/rag/documents"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /ai/rag/documents/{id} — get one
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /ai/rag/documents/{id} — get document by ID")
    class GetDocument {

        @Test
        @DisplayName("returns 200 with document for valid ID")
        void returnsDocumentForValidId() throws Exception {
            UUID id = UUID.randomUUID();
            when(ingestionService.getDocument(id)).thenReturn(buildDocResponse("Policy"));

            mockMvc.perform(get("/ai/rag/documents/{id}", id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title", is("Policy")));
        }

        @Test
        @DisplayName("returns 400 when document not found")
        void returns400WhenNotFound() throws Exception {
            UUID id = UUID.randomUUID();
            when(ingestionService.getDocument(id))
                    .thenThrow(new RagException("not found: " + id));

            mockMvc.perform(get("/ai/rag/documents/{id}", id))
                    .andExpect(status().isBadRequest());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DELETE /ai/rag/documents/{id}
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("DELETE /ai/rag/documents/{id}")
    class DeleteDocument {

        @Test
        @DisplayName("returns 204 on successful delete")
        void returns204OnSuccess() throws Exception {
            UUID id = UUID.randomUUID();
            doNothing().when(ingestionService).deleteDocument(id);

            mockMvc.perform(delete("/ai/rag/documents/{id}", id))
                    .andExpect(status().isNoContent());

            verify(ingestionService).deleteDocument(id);
        }

        @Test
        @DisplayName("returns 400 when document to delete not found")
        void returns400WhenNotFound() throws Exception {
            UUID id = UUID.randomUUID();
            doThrow(new RagException("not found: " + id))
                    .when(ingestionService).deleteDocument(id);

            mockMvc.perform(delete("/ai/rag/documents/{id}", id))
                    .andExpect(status().isBadRequest());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /ai/rag/search
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /ai/rag/search")
    class Search {

        @Test
        @DisplayName("valid search request returns 200 with results")
        void validSearchReturnsResults() throws Exception {
            KnowledgeSearchResult result = new KnowledgeSearchResult(
                    UUID.randomUUID(), "Policy A",
                    UUID.randomUUID(), 0,
                    "Relevant chunk content", "TITLE"
            );
            when(retrievalService.search(any())).thenReturn(List.of(result));

            KnowledgeSearchRequest request = new KnowledgeSearchRequest("leave policy", 5);
            mockMvc.perform(post("/ai/rag/search")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].matchType", is("TITLE")))
                    .andExpect(jsonPath("$[0].documentTitle", is("Policy A")));
        }

        @Test
        @DisplayName("blank query returns 400")
        void blankQueryReturns400() throws Exception {
            String body = """
                    {"query":"","maxResults":5}
                    """;
            mockMvc.perform(post("/ai/rag/search")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("empty results returns 200 with empty array")
        void emptyResultsReturns200() throws Exception {
            when(retrievalService.search(any())).thenReturn(Collections.emptyList());

            KnowledgeSearchRequest request = new KnowledgeSearchRequest("nothing here", 10);
            mockMvc.perform(post("/ai/rag/search")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @DisplayName("topK field alias is accepted and search proceeds normally")
        void topKAliasIsAccepted() throws Exception {
            KnowledgeSearchResult result = new KnowledgeSearchResult(
                    UUID.randomUUID(), "Employee Leave Policy",
                    UUID.randomUUID(), 0,
                    "Employees are entitled to annual leave according to company policy.",
                    "CONTENT"
            );
            when(retrievalService.search(any())).thenReturn(List.of(result));

            // Send topK (the alias) instead of maxResults
            String body = """
                    {"query":"Employees are entitled to annual leave","topK":5}
                    """;
            mockMvc.perform(post("/ai/rag/search")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].documentTitle", is("Employee Leave Policy")))
                    .andExpect(jsonPath("$[0].matchType", is("CONTENT")))
                    .andExpect(jsonPath("$[0].chunkContent",
                            is("Employees are entitled to annual leave according to company policy.")));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /ai/rag/documents/reindex — bulk re-index all ACTIVE documents
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /ai/rag/documents/reindex — bulk re-index all ACTIVE documents")
    class ReindexAll {

        @Test
        @DisplayName("returns 200 with list of successfully re-indexed documents")
        void returns200WithReindexedDocuments() throws Exception {
            List<KnowledgeDocumentResponse> results = List.of(
                    buildDocResponse("Policy A"),
                    buildDocResponse("Policy B")
            );
            when(ingestionService.reindexAllActiveDocuments()).thenReturn(results);

            mockMvc.perform(post("/ai/rag/documents/reindex")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].title", is("Policy A")))
                    .andExpect(jsonPath("$[1].title", is("Policy B")));

            verify(ingestionService).reindexAllActiveDocuments();
        }

        @Test
        @DisplayName("returns 200 with empty list when no ACTIVE documents exist")
        void returns200WithEmptyListWhenNoDocuments() throws Exception {
            when(ingestionService.reindexAllActiveDocuments()).thenReturn(Collections.emptyList());

            mockMvc.perform(post("/ai/rag/documents/reindex")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @DisplayName("service RagException during reindex → returns 400")
        void serviceExceptionReturns400() throws Exception {
            when(ingestionService.reindexAllActiveDocuments())
                    .thenThrow(new RagException("embedding provider unavailable"));

            mockMvc.perform(post("/ai/rag/documents/reindex")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /ai/rag/documents/{id}/reindex — re-index one document
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /ai/rag/documents/{id}/reindex — re-index single document")
    class ReindexOne {

        @Test
        @DisplayName("returns 200 with re-indexed document response")
        void returns200WithReindexedDocument() throws Exception {
            UUID id = UUID.randomUUID();
            KnowledgeDocumentResponse response = buildDocResponse("Attendance Policy");
            when(ingestionService.reindexDocument(id)).thenReturn(response);

            mockMvc.perform(post("/ai/rag/documents/{id}/reindex", id)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title", is("Attendance Policy")));

            verify(ingestionService).reindexDocument(id);
        }

        @Test
        @DisplayName("returns 400 when document not found")
        void returns400WhenDocumentNotFound() throws Exception {
            UUID id = UUID.randomUUID();
            when(ingestionService.reindexDocument(id))
                    .thenThrow(new RagException("not found: " + id));

            mockMvc.perform(post("/ai/rag/documents/{id}/reindex", id)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("returns 400 when embedding fails during reindex")
        void returns400WhenEmbeddingFails() throws Exception {
            UUID id = UUID.randomUUID();
            when(ingestionService.reindexDocument(id))
                    .thenThrow(new RagException("Re-index embedding generation failed"));

            mockMvc.perform(post("/ai/rag/documents/{id}/reindex", id)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private KnowledgeDocumentResponse buildDocResponse(String title) {
        return new KnowledgeDocumentResponse(
                UUID.randomUUID(),
                title,
                "description",
                KnowledgeSourceType.POLICY,
                null,
                KnowledgeDocumentStatus.ACTIVE,
                LocalDateTime.now(),
                LocalDateTime.now(),
                "test-user"
        );
    }
}
