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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.github.leanish.sqs.codec.algorithms.CompressionAlgorithm;
import io.github.leanish.sqs.codec.algorithms.EncodingAlgorithm;

class PayloadCodecTest {

    @ParameterizedTest
    @MethodSource("roundTripCases")
    void roundTripPreservesPayload(CompressionAlgorithm compressionAlgorithm, EncodingAlgorithm encoding) {
        PayloadCodec codec = new PayloadCodec(compressionAlgorithm, encoding);
        String payload = "{\"value\":42}";
        byte[] encoded = codec.encode(payload.getBytes(StandardCharsets.UTF_8));

        String decoded = new String(codec.decode(encoded), StandardCharsets.UTF_8);

        assertThat(decoded).isEqualTo(payload);
    }

    @Test
    void defaultCodecUsesPlaintextEncoding() {
        PayloadCodec codec = new PayloadCodec();
        String payload = "payload-42";

        byte[] encoded = codec.encode(payload.getBytes(StandardCharsets.UTF_8));

        assertThat(new String(encoded, StandardCharsets.UTF_8)).isEqualTo(payload);
        assertThat(new String(codec.decode(encoded), StandardCharsets.UTF_8)).isEqualTo(payload);
    }

    @Test
    void effectiveEncodingAlgorithmUsesBase64WhenCompressed() {
        assertThat(EncodingAlgorithm.effectiveFor(
                CompressionAlgorithm.ZSTD,
                EncodingAlgorithm.NONE))
                .isEqualTo(EncodingAlgorithm.BASE64);
        assertThat(EncodingAlgorithm.effectiveFor(
                CompressionAlgorithm.GZIP,
                EncodingAlgorithm.BASE64_STD))
                .isEqualTo(EncodingAlgorithm.BASE64_STD);
        assertThat(EncodingAlgorithm.effectiveFor(
                CompressionAlgorithm.NONE,
                EncodingAlgorithm.NONE))
                .isEqualTo(EncodingAlgorithm.NONE);
    }

    @Test
    void decodeWrapsInvalidBase64() {
        PayloadCodec codec = new PayloadCodec(CompressionAlgorithm.NONE, EncodingAlgorithm.BASE64);

        assertThatThrownBy(() -> codec.decode("!!!".getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(PayloadCodecException.class)
                .hasMessage("Invalid base64 payload")
                .hasCauseInstanceOf(IllegalArgumentException.class);
    }

    private static Stream<Arguments> roundTripCases() {
        return Stream.of(
                Arguments.of(CompressionAlgorithm.NONE, EncodingAlgorithm.NONE),
                Arguments.of(CompressionAlgorithm.NONE, EncodingAlgorithm.BASE64),
                Arguments.of(CompressionAlgorithm.NONE, EncodingAlgorithm.BASE64_STD),
                Arguments.of(CompressionAlgorithm.ZSTD, EncodingAlgorithm.BASE64),
                Arguments.of(CompressionAlgorithm.ZSTD, EncodingAlgorithm.BASE64_STD),
                Arguments.of(CompressionAlgorithm.ZSTD, EncodingAlgorithm.NONE),
                Arguments.of(CompressionAlgorithm.GZIP, EncodingAlgorithm.BASE64),
                Arguments.of(CompressionAlgorithm.GZIP, EncodingAlgorithm.BASE64_STD),
                Arguments.of(CompressionAlgorithm.GZIP, EncodingAlgorithm.NONE));
    }
}
