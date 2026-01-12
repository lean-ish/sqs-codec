/*
 * Copyright (c) 2026 Leandro Aguiar
 * Licensed under the MIT License.
 * See LICENSE file in the project root for full license information.
 */
package io.github.leanish.sqs.codec.algorithms.checksum;

import io.github.leanish.sqs.codec.algorithms.ChecksumAlgorithm;

public interface Digestor {
    ChecksumAlgorithm algorithm();
    String checksum(byte[] payload);
}
