/*
 * Copyright (c) 2026 Leandro Aguiar
 * Licensed under the MIT License.
 * See LICENSE file in the project root for full license information.
 */
package io.github.leanish.sqs.codec;

public enum CompressionAlgorithm {
    /** Zstandard compression for high ratio with good performance. */
    ZSTD("zstd"),
    /** Gzip compression for interoperability with common tooling. */
    GZIP("gzip"),
    /** No compression; payload bytes are left as-is. */
    NONE("none");

    private final String attributeValue;

    CompressionAlgorithm(String attributeValue) {
        this.attributeValue = attributeValue;
    }

    public String attributeValue() {
        return attributeValue;
    }
}
