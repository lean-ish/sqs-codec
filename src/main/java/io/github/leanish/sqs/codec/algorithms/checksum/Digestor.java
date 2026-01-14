/*
 * Copyright (c) 2026 Leandro Aguiar
 * Licensed under the MIT License.
 * See LICENSE file in the project root for full license information.
 */
package io.github.leanish.sqs.codec.algorithms.checksum;

import com.google.errorprone.annotations.Immutable;

/**
 * Strategy interface for computing payload checksums.
 */
@Immutable
public interface Digestor {
    String checksum(byte[] payload);
}
