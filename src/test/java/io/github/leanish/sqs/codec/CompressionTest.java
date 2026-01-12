/*
 * Copyright (c) 2026 Leandro Aguiar
 * Licensed under the MIT License.
 * See LICENSE file in the project root for full license information.
 */
package io.github.leanish.sqs.codec;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;
import java.util.zip.ZipException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.github.leanish.sqs.codec.algorithms.CompressionAlgorithm;
import io.github.leanish.sqs.codec.algorithms.compression.Compressor;
import io.github.leanish.sqs.codec.algorithms.compression.GzipCompressor;
import io.github.leanish.sqs.codec.algorithms.compression.UncompressedCompressor;
import io.github.leanish.sqs.codec.algorithms.compression.ZstdCompressor;

class CompressionTest {

    @ParameterizedTest
    @MethodSource("compressorCases")
    void compressionRoundTripPreservesPayload(Compressor compressor, CompressionAlgorithm expectedAlgorithm) {
        byte[] payload = "payload-42".getBytes(StandardCharsets.UTF_8);

        byte[] compressed = compressor.compress(payload);
        byte[] decoded = compressor.decompress(compressed);

        assertThat(compressor.algorithm()).isEqualTo(expectedAlgorithm);
        assertThat(decoded).isEqualTo(payload);
    }

    @Test
    void uncompressedCompressorReturnsSameInstance() {
        UncompressedCompressor compressor = new UncompressedCompressor();
        byte[] payload = "payload-42".getBytes(StandardCharsets.UTF_8);

        assertThat(compressor.compress(payload)).isSameAs(payload);
        assertThat(compressor.decompress(payload)).isSameAs(payload);
    }

    @Test
    void gzipDecompressRejectsInvalidPayload() {
        GzipCompressor compressor = new GzipCompressor();
        byte[] payload = "not-gzip".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> compressor.decompress(payload))
                .isInstanceOf(java.io.UncheckedIOException.class)
                .hasCauseInstanceOf(ZipException.class);
    }

    @Test
    void zstdDecompressRejectsInvalidPayload() {
        ZstdCompressor compressor = new ZstdCompressor();
        byte[] payload = "not-zstd".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> compressor.decompress(payload))
                .isInstanceOf(java.io.UncheckedIOException.class);
    }

    private static Stream<Arguments> compressorCases() {
        return Stream.of(
                Arguments.of(new UncompressedCompressor(), CompressionAlgorithm.NONE),
                Arguments.of(new GzipCompressor(), CompressionAlgorithm.GZIP),
                Arguments.of(new ZstdCompressor(), CompressionAlgorithm.ZSTD));
    }
}
