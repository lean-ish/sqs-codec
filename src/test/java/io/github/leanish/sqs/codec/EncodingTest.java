/*
 * Copyright (c) 2026 Leandro Aguiar
 * Licensed under the MIT License.
 * See LICENSE file in the project root for full license information.
 */
package io.github.leanish.sqs.codec;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import io.github.leanish.sqs.codec.algorithms.EncodingAlgorithm;
import io.github.leanish.sqs.codec.algorithms.encoding.Base64Encoder;
import io.github.leanish.sqs.codec.algorithms.encoding.StandardBase64Encoder;
import io.github.leanish.sqs.codec.algorithms.encoding.UnencodedEncoder;

class EncodingTest {

    private final Base64Encoder urlEncoder = new Base64Encoder();
    private final StandardBase64Encoder standardEncoder = new StandardBase64Encoder();
    private final UnencodedEncoder unencodedEncoder = new UnencodedEncoder();

    @Test
    void base64EncodersExposeAlgorithms() {
        assertThat(urlEncoder.algorithm()).isEqualTo(EncodingAlgorithm.BASE64);
        assertThat(standardEncoder.algorithm()).isEqualTo(EncodingAlgorithm.BASE64_STD);
        assertThat(unencodedEncoder.algorithm()).isEqualTo(EncodingAlgorithm.NONE);
    }

    @Test
    void base64VariantsEncodeDifferently() {
        byte[] payload = new byte[] {(byte) 0xfb, (byte) 0xef, (byte) 0xff};

        assertThat(new String(urlEncoder.encode(payload), StandardCharsets.UTF_8)).isEqualTo("--__");
        assertThat(new String(standardEncoder.encode(payload), StandardCharsets.UTF_8)).isEqualTo("++//");
    }

    @Test
    void unencodedEncoderPassesThroughBytes() {
        String payload = "payload-42";

        byte[] encoded = unencodedEncoder.encode(payload.getBytes(StandardCharsets.UTF_8));
        byte[] decoded = unencodedEncoder.decode(encoded);

        assertThat(new String(encoded, StandardCharsets.UTF_8)).isEqualTo(payload);
        assertThat(new String(decoded, StandardCharsets.UTF_8)).isEqualTo(payload);
    }

    @Test
    void urlSafeBase64RejectsInvalidPayload() {
        assertThatThrownBy(() -> urlEncoder.decode("!@#".getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(PayloadCodecException.class)
                .hasMessage("Invalid base64 payload")
                .hasCauseInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void standardBase64RejectsInvalidPayload() {
        assertThatThrownBy(() -> standardEncoder.decode("[]".getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(PayloadCodecException.class)
                .hasMessage("Invalid base64 payload")
                .hasCauseInstanceOf(IllegalArgumentException.class);
    }
}
