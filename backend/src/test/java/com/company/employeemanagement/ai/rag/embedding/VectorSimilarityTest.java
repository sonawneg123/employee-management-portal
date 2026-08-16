package com.company.employeemanagement.ai.rag.embedding;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * Unit tests for {@link VectorSimilarity}.
 *
 * @author Employee Management Portal Team
 */
@DisplayName("VectorSimilarity")
class VectorSimilarityTest {

    // ── Cosine similarity ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("cosineSimilarity")
    class CosineSimilarityTests {

        @Test
        @DisplayName("identical vectors produce similarity 1.0")
        void identicalVectors() {
            float[] v = {1.0f, 0.0f, 0.0f};
            assertThat(VectorSimilarity.cosineSimilarity(v, v)).isCloseTo(1.0, within(1e-6));
        }

        @Test
        @DisplayName("orthogonal vectors produce similarity 0.0")
        void orthogonalVectors() {
            float[] a = {1.0f, 0.0f};
            float[] b = {0.0f, 1.0f};
            assertThat(VectorSimilarity.cosineSimilarity(a, b)).isCloseTo(0.0, within(1e-6));
        }

        @Test
        @DisplayName("opposite vectors produce similarity -1.0")
        void oppositeVectors() {
            float[] a = { 1.0f, 0.0f};
            float[] b = {-1.0f, 0.0f};
            assertThat(VectorSimilarity.cosineSimilarity(a, b)).isCloseTo(-1.0, within(1e-6));
        }

        @Test
        @DisplayName("scale-invariant: [1,1] vs [2,2] is 1.0")
        void scaleInvariant() {
            float[] a = {1.0f, 1.0f};
            float[] b = {2.0f, 2.0f};
            assertThat(VectorSimilarity.cosineSimilarity(a, b)).isCloseTo(1.0, within(1e-6));
        }

        @Test
        @DisplayName("null first argument throws IllegalArgumentException")
        void nullFirstThrows() {
            assertThatThrownBy(() -> VectorSimilarity.cosineSimilarity(null, new float[]{1.0f}))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("different length vectors throw IllegalArgumentException")
        void differentLengthThrows() {
            assertThatThrownBy(() ->
                    VectorSimilarity.cosineSimilarity(new float[]{1f, 2f}, new float[]{1f}))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("zero vector produces similarity 0.0 (not NaN)")
        void zeroVectorProducesZero() {
            float[] zero    = {0.0f, 0.0f};
            float[] nonZero = {1.0f, 1.0f};
            assertThat(VectorSimilarity.cosineSimilarity(zero, nonZero)).isCloseTo(0.0, within(1e-6));
        }
    }

    // ── Serialisation round-trip ──────────────────────────────────────────────

    @Nested
    @DisplayName("toBytes / fromBytes round-trip")
    class SerializationTests {

        @Test
        @DisplayName("round-trip preserves exact float values")
        void roundTripExact() {
            float[] original = {0.1f, -0.5f, 3.14f, Float.MAX_VALUE, Float.MIN_VALUE};
            byte[] bytes = VectorSimilarity.toBytes(original);
            float[] recovered = VectorSimilarity.fromBytes(bytes);
            assertThat(recovered).containsExactly(original);
        }

        @Test
        @DisplayName("toBytes produces length = 4 × vector.length")
        void byteLengthCorrect() {
            float[] v = new float[768];
            assertThat(VectorSimilarity.toBytes(v)).hasSize(768 * 4);
        }

        @Test
        @DisplayName("fromBytes with non-multiple-of-4 throws IllegalArgumentException")
        void invalidByteLengthThrows() {
            assertThatThrownBy(() -> VectorSimilarity.fromBytes(new byte[]{1, 2, 3}))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("fromBytes(null) throws IllegalArgumentException")
        void nullBytesThrows() {
            assertThatThrownBy(() -> VectorSimilarity.fromBytes(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
