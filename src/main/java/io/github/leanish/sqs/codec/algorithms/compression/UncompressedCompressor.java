/*
 * Copyright (c) 2026 Leandro Aguiar
 * Licensed under the MIT License.
 * See LICENSE file in the project root for full license information.
 */
package io.github.leanish.sqs.codec.algorithms.compression;

import io.github.leanish.sqs.codec.algorithms.CompressionAlgorithm;

public final class UncompressedCompressor implements Compressor {

    @Override
    public CompressionAlgorithm algorithm() {
        return CompressionAlgorithm.NONE;
    }

    @Override
    public byte[] compress(byte[] payload) {
        return payload;
    }

    @Override
    public byte[] decompress(byte[] payload) {
        return payload;
    }
}
