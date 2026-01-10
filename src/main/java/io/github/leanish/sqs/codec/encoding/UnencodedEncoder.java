/*
 * Copyright (c) 2026 Leandro Aguiar
 * Licensed under the MIT License.
 * See LICENSE file in the project root for full license information.
 */
package io.github.leanish.sqs.codec.encoding;

import java.nio.charset.StandardCharsets;

import io.github.leanish.sqs.codec.EncodingAlgorithm;

public final class UnencodedEncoder implements Encoder {

    @Override
    public EncodingAlgorithm algorithm() {
        return EncodingAlgorithm.NONE;
    }

    @Override
    public String encode(byte[] payload) {
        return new String(payload, StandardCharsets.UTF_8);
    }

    @Override
    public byte[] decode(String encoded) {
        return encoded.getBytes(StandardCharsets.UTF_8);
    }
}
