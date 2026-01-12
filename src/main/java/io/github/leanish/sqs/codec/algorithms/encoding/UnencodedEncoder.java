/*
 * Copyright (c) 2026 Leandro Aguiar
 * Licensed under the MIT License.
 * See LICENSE file in the project root for full license information.
 */
package io.github.leanish.sqs.codec.algorithms.encoding;

import io.github.leanish.sqs.codec.algorithms.EncodingAlgorithm;

public final class UnencodedEncoder implements Encoder {

    @Override
    public EncodingAlgorithm algorithm() {
        return EncodingAlgorithm.NONE;
    }

    @Override
    public byte[] encode(byte[] payload) {
        return payload;
    }

    @Override
    public byte[] decode(byte[] encoded) {
        return encoded;
    }
}
