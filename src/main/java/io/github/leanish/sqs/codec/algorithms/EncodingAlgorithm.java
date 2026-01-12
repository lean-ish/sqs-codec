/*
 * Copyright (c) 2026 Leandro Aguiar
 * Licensed under the MIT License.
 * See LICENSE file in the project root for full license information.
 */
package io.github.leanish.sqs.codec.algorithms;

import io.github.leanish.sqs.codec.algorithms.encoding.Base64Encoder;
import io.github.leanish.sqs.codec.algorithms.encoding.Encoder;
import io.github.leanish.sqs.codec.algorithms.encoding.StandardBase64Encoder;
import io.github.leanish.sqs.codec.algorithms.encoding.UnencodedEncoder;

public enum EncodingAlgorithm {
    /** URL-safe Base64 for attribute values that might travel through URL contexts. */
    BASE64("base64"),
    /** Standard Base64 for systems that require "+" "/" and "=" padding. */
    BASE64_STD("base64-std"),
    /** No encoding; payload is treated as UTF-8 bytes. */
    NONE("none");

    private static final Encoder BASE64_ENCODER = new Base64Encoder();
    private static final Encoder BASE64_STD_ENCODER = new StandardBase64Encoder();
    private static final Encoder UNENCODED = new UnencodedEncoder();

    private final String id;

    EncodingAlgorithm(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public Encoder encoder() {
        return switch (this) {
            case BASE64 -> BASE64_ENCODER;
            case BASE64_STD -> BASE64_STD_ENCODER;
            case NONE -> UNENCODED;
        };
    }

    public static EncodingAlgorithm effectiveFor(
            CompressionAlgorithm compressionAlgorithm,
            EncodingAlgorithm encodingAlgorithm) {
        if (encodingAlgorithm == EncodingAlgorithm.NONE
                && compressionAlgorithm != CompressionAlgorithm.NONE) {
            return EncodingAlgorithm.BASE64;
        }
        return encodingAlgorithm;
    }
}
