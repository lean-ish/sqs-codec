/*
 * Copyright (c) 2026 Leandro Aguiar
 * Licensed under the MIT License.
 * See LICENSE file in the project root for full license information.
 */
package io.github.leanish.sqs.codec.algorithms.encoding;

import io.github.leanish.sqs.codec.algorithms.EncodingAlgorithm;

public interface Encoder {
    EncodingAlgorithm algorithm();
    byte[] encode(byte[] payload);
    byte[] decode(byte[] encoded);
}
