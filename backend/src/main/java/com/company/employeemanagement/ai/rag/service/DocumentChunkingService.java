package com.company.employeemanagement.ai.rag.service;

import com.company.employeemanagement.ai.rag.config.RagProperties;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Splits document text into overlapping, word-boundary-aligned chunks.
 *
 * <h2>Algorithm</h2>
 * <ol>
 *   <li>If the text is blank or shorter than {@code chunkSize}, it is returned
 *       as a single chunk (or an empty list for blank/null input).</li>
 *   <li>Otherwise, a sliding window of size {@code chunkSize} characters
 *       advances by {@code (chunkSize - chunkOverlap)} characters on each step.</li>
 *   <li>Each window is snapped to the nearest word boundary to avoid splitting
 *       mid-word: the end boundary walks left to the last space within the
 *       window; the start boundary (for chunks after the first) snaps to the
 *       first space after the raw start position.</li>
 *   <li>Chunks are deduplicated: if snapping produces the same text as the
 *       previous chunk it is skipped.</li>
 * </ol>
 *
 * <p>The implementation is deterministic — the same text + same properties
 * always produces the same list of chunks.
 *
 * @author Employee Management Portal Team
 */
@Service
public class DocumentChunkingService {

    private final RagProperties ragProperties;

    public DocumentChunkingService(final RagProperties ragProperties) {
        this.ragProperties = ragProperties;
    }

    /**
     * Splits {@code text} into chunks according to the configured
     * {@code chunkSize} and {@code chunkOverlap}.
     *
     * @param text the document text to split; may be {@code null}
     * @return an ordered, immutable list of chunk strings; never {@code null}
     */
    public List<String> chunk(final String text) {
        if (text == null || text.isBlank()) {
            return Collections.emptyList();
        }

        final String trimmed = text.trim();
        final int chunkSize = ragProperties.getChunkSize();
        final int chunkOverlap = ragProperties.getChunkOverlap();
        final int step = Math.max(1, chunkSize - chunkOverlap);

        if (trimmed.length() <= chunkSize) {
            return List.of(trimmed);
        }

        final List<String> chunks = new ArrayList<>();
        int start = 0;

        while (start < trimmed.length()) {
            int rawEnd = Math.min(start + chunkSize, trimmed.length());

            // Snap end boundary left to the last space (word boundary) unless
            // we are already at the very end of the string.
            int end = rawEnd;
            if (rawEnd < trimmed.length()) {
                int lastSpace = trimmed.lastIndexOf(' ', rawEnd);
                if (lastSpace > start) {
                    end = lastSpace;
                }
            }

            String chunk = trimmed.substring(start, end).trim();
            if (!chunk.isEmpty()) {
                // Deduplicate: skip if identical to the previous chunk
                if (chunks.isEmpty() || !chunk.equals(chunks.get(chunks.size() - 1))) {
                    chunks.add(chunk);
                }
            }

            // Advance by step; ensure forward progress to avoid an infinite loop
            int nextStart = start + step;

            // Snap nextStart forward to the first space after the raw advance
            // to avoid starting mid-word (only when not already at a boundary).
            if (nextStart < trimmed.length() && trimmed.charAt(nextStart) != ' ') {
                int nextSpace = trimmed.indexOf(' ', nextStart);
                if (nextSpace != -1 && nextSpace < nextStart + chunkOverlap) {
                    nextStart = nextSpace + 1;
                }
            } else if (nextStart < trimmed.length()) {
                nextStart++; // skip the space itself
            }

            if (nextStart <= start) {
                nextStart = start + 1; // guaranteed forward progress
            }
            start = nextStart;
        }

        return Collections.unmodifiableList(chunks);
    }

    /**
     * Estimates the number of tokens in {@code text} by counting whitespace-
     * delimited words. This is a fast approximation suitable for chunk sizing.
     *
     * @param text the text to estimate; may be {@code null}
     * @return estimated token count, or {@code 0} for blank/null input
     */
    public int estimateTokenCount(final String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        return text.trim().split("\\s+").length;
    }
}
