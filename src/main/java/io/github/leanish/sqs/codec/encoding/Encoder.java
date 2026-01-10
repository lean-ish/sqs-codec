/*
 * Copyright (c) 2026 Leandro Aguiar
 * Licensed under the MIT License.
 * See LICENSE file in the project root for full license information.
 */
package io.github.leanish.sqs.codec.encoding;

import io.github.leanish.sqs.codec.EncodingAlgorithm;

public interface Encoder {
    EncodingAlgorithm algorithm();
    String encode(byte[] payload);
    byte[] decode(String encoded);
}
