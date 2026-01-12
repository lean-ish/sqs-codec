/*
 * Copyright (c) 2026 Leandro Aguiar
 * Licensed under the MIT License.
 * See LICENSE file in the project root for full license information.
 */
package io.github.leanish.sqs.codec;

import io.github.leanish.sqs.codec.algorithms.ChecksumAlgorithm;
import io.github.leanish.sqs.codec.algorithms.CompressionAlgorithm;
import io.github.leanish.sqs.codec.algorithms.EncodingAlgorithm;

public record PayloadCodecConfiguration(
        CompressionAlgorithm compressionAlgorithm,
        EncodingAlgorithm encodingAlgorithm,
        ChecksumAlgorithm checksumAlgorithm) {
}
