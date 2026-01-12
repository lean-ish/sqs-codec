/*
 * Copyright (c) 2026 Leandro Aguiar
 * Licensed under the MIT License.
 * See LICENSE file in the project root for full license information.
 */
package io.github.leanish.sqs.codec.algorithms.compression;

import io.github.leanish.sqs.codec.algorithms.CompressionAlgorithm;

public interface Compressor {
    CompressionAlgorithm algorithm();
    byte[] compress(byte[] payload);
    byte[] decompress(byte[] payload);
}
