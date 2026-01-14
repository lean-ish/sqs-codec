/*
 * Copyright (c) 2026 Leandro Aguiar
 * Licensed under the MIT License.
 * See LICENSE file in the project root for full license information.
 */
package io.github.leanish.sqs.codec.algorithms.compression;

import com.google.errorprone.annotations.Immutable;

/**
 * Strategy interface for compressing and decompressing payload bytes.
 */
@Immutable
public interface Compressor {
    byte[] compress(byte[] payload);
    byte[] decompress(byte[] payload);
}
