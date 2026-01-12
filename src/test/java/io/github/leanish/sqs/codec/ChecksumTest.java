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

import io.github.leanish.sqs.codec.algorithms.ChecksumAlgorithm;
import io.github.leanish.sqs.codec.algorithms.checksum.Md5Digestor;
import io.github.leanish.sqs.codec.algorithms.checksum.Sha256Digestor;
import io.github.leanish.sqs.codec.algorithms.checksum.UndigestedDigestor;

class ChecksumTest {

    @Test
    void digestorsComputeExpectedChecksums() {
        byte[] payload = "payload-42".getBytes(StandardCharsets.UTF_8);

        Md5Digestor md5 = new Md5Digestor();
        Sha256Digestor sha256 = new Sha256Digestor();

        assertThat(md5.checksum(payload)).isEqualTo("t2tngCwK9b7C9eqVQunqfg==");
        assertThat(sha256.checksum(payload)).isEqualTo("eQTFzG7BGaPUgIUuq8rJBeIyQhPVNOfDTHjJAxb8udg=");
    }

    @Test
    void undigestedDigestorThrows() {
        UndigestedDigestor digestor = new UndigestedDigestor();

        assertThat(digestor.algorithm()).isEqualTo(ChecksumAlgorithm.NONE);
        assertThatThrownBy(() -> digestor.checksum("payload".getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(PayloadCodecException.class)
                .hasMessage("Digestor algorithm is none");
    }

    @Test
    void checksumAlgorithmFromAttributeValueIsCaseInsensitive() {
        assertThat(ChecksumAlgorithm.fromAttributeValue("MD5")).isEqualTo(ChecksumAlgorithm.MD5);
        assertThat(ChecksumAlgorithm.fromAttributeValue("sha256")).isEqualTo(ChecksumAlgorithm.SHA256);
        assertThat(ChecksumAlgorithm.fromAttributeValue("None")).isEqualTo(ChecksumAlgorithm.NONE);
    }

    @Test
    void checksumAlgorithmFromAttributeValueRejectsUnknown() {
        assertThatThrownBy(() -> ChecksumAlgorithm.fromAttributeValue("sha1"))
                .isInstanceOf(PayloadCodecException.class)
                .hasMessage("Unsupported checksum algorithm: sha1");
    }

    @Test
    void checksumAlgorithmFromAttributeValueRejectsBlank() {
        assertThatThrownBy(() -> ChecksumAlgorithm.fromAttributeValue("  "))
                .isInstanceOf(PayloadCodecException.class)
                .hasMessage("Unsupported checksum algorithm:   ");
    }
}
