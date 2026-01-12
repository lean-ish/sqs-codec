/*
 * Copyright (c) 2026 Leandro Aguiar
 * Licensed under the MIT License.
 * See LICENSE file in the project root for full license information.
 */
package io.github.leanish.sqs.codec.algorithms.encoding;

import java.util.Base64;

import io.github.leanish.sqs.codec.PayloadCodecException;
import io.github.leanish.sqs.codec.algorithms.EncodingAlgorithm;

public final class Base64Encoder implements Encoder {

    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

    @Override
    public EncodingAlgorithm algorithm() {
        return EncodingAlgorithm.BASE64;
    }

    @Override
    public byte[] encode(byte[] payload) {
        return ENCODER.encode(payload);
    }

    @Override
    public byte[] decode(byte[] encoded) {
        try {
            return DECODER.decode(encoded);
        } catch (IllegalArgumentException e) {
            throw new PayloadCodecException("Invalid base64 payload", e);
        }
    }
}
