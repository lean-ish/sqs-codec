/*
 * Copyright (c) 2026 Leandro Aguiar
 * Licensed under the MIT License.
 * See LICENSE file in the project root for full license information.
 */
package io.github.leanish.sqs.codec.algorithms;

import io.github.leanish.sqs.codec.algorithms.compression.Compressor;
import io.github.leanish.sqs.codec.algorithms.compression.GzipCompressor;
import io.github.leanish.sqs.codec.algorithms.compression.SnappyCompressor;
import io.github.leanish.sqs.codec.algorithms.compression.UncompressedCompressor;
import io.github.leanish.sqs.codec.algorithms.compression.ZstdCompressor;

public enum CompressionAlgorithm {
    /** Zstandard compression for high ratio with good performance. */
    ZSTD("zstd"),
    /** Snappy compression for low-latency payloads. */
    SNAPPY("snappy"),
    /** Gzip compression for interoperability with common tooling. */
    GZIP("gzip"),
    /** No compression; payload bytes are left as-is. */
    NONE("none");

    private static final Compressor ZSTD_COMPRESSOR = new ZstdCompressor();
    private static final Compressor SNAPPY_COMPRESSOR = new SnappyCompressor();
    private static final Compressor GZIP_COMPRESSOR = new GzipCompressor();
    private static final Compressor UNCOMPRESSED = new UncompressedCompressor();

    private final String id;

    CompressionAlgorithm(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public Compressor compressor() {
        return switch (this) {
            case ZSTD -> ZSTD_COMPRESSOR;
            case SNAPPY -> SNAPPY_COMPRESSOR;
            case GZIP -> GZIP_COMPRESSOR;
            case NONE -> UNCOMPRESSED;
        };
    }
}
