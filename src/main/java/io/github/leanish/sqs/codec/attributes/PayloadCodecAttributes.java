/*
 * Copyright (c) 2026 Leandro Aguiar
 * Licensed under the MIT License.
 * See LICENSE file in the project root for full license information.
 */
package io.github.leanish.sqs.codec.attributes;

import java.util.Map;

import org.jspecify.annotations.Nullable;

import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;

public final class PayloadCodecAttributes {

    public static final String COMPRESSION_ALG = "x-codec-compression-alg";
    public static final String ENCODING_ALG = "x-codec-encoding-alg";
    public static final String CHECKSUM_ALG = "x-codec-checksum-alg";
    public static final String CHECKSUM = "x-codec-checksum";
    public static final String VERSION = "x-codec-version";
    public static final String RAW_LENGTH = "x-codec-raw-length";

    public static final String VERSION_VALUE = "1.0.0";

    private PayloadCodecAttributes() {
    }

    @Nullable
    public static String attributeValue(Map<String, MessageAttributeValue> attributes, String name) {
        MessageAttributeValue attributeValue = attributes.get(name);
        if (attributeValue == null) {
            return null;
        }
        return attributeValue.stringValue();
    }

    public static MessageAttributeValue stringAttribute(String value) {
        return MessageAttributeValue.builder()
                .dataType("String")
                .stringValue(value)
                .build();
    }
}
