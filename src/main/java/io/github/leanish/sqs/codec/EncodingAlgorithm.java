/*
 * Copyright (c) 2026 Leandro Aguiar
 * Licensed under the MIT License.
 * See LICENSE file in the project root for full license information.
 */
package io.github.leanish.sqs.codec;

public enum EncodingAlgorithm {
    /** URL-safe Base64 for attribute values that might travel through URL contexts. */
    BASE64("base64"),
    /** Standard Base64 for systems that require "+" "/" and "=" padding. */
    BASE64_STD("base64-std"),
    /** No encoding; payload is treated as UTF-8 bytes. */
    NONE("none");

    private final String attributeValue;

    EncodingAlgorithm(String attributeValue) {
        this.attributeValue = attributeValue;
    }

    public String attributeValue() {
        return attributeValue;
    }
}
