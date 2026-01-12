/*
 * Copyright (c) 2026 Leandro Aguiar
 * Licensed under the MIT License.
 * See LICENSE file in the project root for full license information.
 */
package io.github.leanish.sqs.codec.algorithms.checksum;

import io.github.leanish.sqs.codec.PayloadCodecException;
import io.github.leanish.sqs.codec.algorithms.ChecksumAlgorithm;

public final class UndigestedDigestor implements Digestor {

    @Override
    public ChecksumAlgorithm algorithm() {
        return ChecksumAlgorithm.NONE;
    }

    @Override
    public String checksum(byte[] payload) {
        throw new PayloadCodecException("Digestor algorithm is none");
    }
}
