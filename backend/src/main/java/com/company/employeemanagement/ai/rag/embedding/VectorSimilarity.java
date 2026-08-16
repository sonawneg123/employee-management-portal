package com.company.employeemanagement.ai.rag.embedding;

/**
 * Utility class for vector similarity calculations.
 *
 * <p>All operations are stateless and thread-safe.
 *
 * @author Employee Management Portal Team
 */
public final class VectorSimilarity {

    private VectorSimilarity() { }

    /**
     * Computes the cosine similarity between two float vectors.
     *
     * <p>Both vectors must have the same length. Returns a value in the range
     * {@code [-1.0, 1.0]} where {@code 1.0} means identical direction.
     *
     * @param a the first vector; must not be {@code null} or empty
     * @param b the second vector; must have the same length as {@code a}
     * @return cosine similarity in range {@code [-1.0, 1.0]}
     * @throws IllegalArgumentException if vectors are null, empty, or different lengths
     */
    public static double cosineSimilarity(final float[] a, final float[] b) {
        if (a == null || b == null || a.length == 0 || a.length != b.length) {
            throw new IllegalArgumentException(
                    "Both vectors must be non-null, non-empty, and equal length.");
        }

        double dot = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < a.length; i++) {
            dot   += (double) a[i] * b[i];
            normA += (double) a[i] * a[i];
            normB += (double) b[i] * b[i];
        }

        final double denom = Math.sqrt(normA) * Math.sqrt(normB);
        if (denom == 0.0) {
            return 0.0;
        }
        return dot / denom;
    }

    /**
     * Serialises a float array to a byte array (big-endian IEEE 754).
     * Each {@code float} occupies 4 bytes.
     *
     * @param vector the vector to serialise; must not be {@code null}
     * @return a byte array of length {@code vector.length * 4}
     */
    public static byte[] toBytes(final float[] vector) {
        final byte[] bytes = new byte[vector.length * 4];
        for (int i = 0; i < vector.length; i++) {
            final int bits = Float.floatToIntBits(vector[i]);
            final int off  = i * 4;
            bytes[off]     = (byte) (bits >> 24);
            bytes[off + 1] = (byte) (bits >> 16);
            bytes[off + 2] = (byte) (bits >> 8);
            bytes[off + 3] = (byte)  bits;
        }
        return bytes;
    }

    /**
     * Deserialises a byte array produced by {@link #toBytes(float[])} back to a float array.
     *
     * @param bytes the serialised vector bytes; length must be a multiple of 4
     * @return the reconstructed float array
     * @throws IllegalArgumentException if the byte array length is not a multiple of 4
     */
    public static float[] fromBytes(final byte[] bytes) {
        if (bytes == null || bytes.length % 4 != 0) {
            throw new IllegalArgumentException("Byte array length must be a multiple of 4.");
        }
        final float[] vector = new float[bytes.length / 4];
        for (int i = 0; i < vector.length; i++) {
            final int off  = i * 4;
            final int bits = ((bytes[off] & 0xFF) << 24)
                           | ((bytes[off + 1] & 0xFF) << 16)
                           | ((bytes[off + 2] & 0xFF) << 8)
                           |  (bytes[off + 3] & 0xFF);
            vector[i] = Float.intBitsToFloat(bits);
        }
        return vector;
    }
}
