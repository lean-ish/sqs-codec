/*
 * Copyright (c) 2026 Leandro Aguiar
 * Licensed under the MIT License.
 * See LICENSE file in the project root for full license information.
 */
package io.github.leanish.sqs.codec;

import org.apache.commons.lang3.StringUtils;

public enum ChecksumAlgorithm {
    /** MD5 checksum for lightweight integrity checks. */
    MD5("md5"),
    /** SHA-256 checksum for stronger integrity guarantees. */
    SHA256("sha256"),
    /** No checksum; integrity attributes are omitted. */
    NONE("none");

    private final String attributeValue;

    ChecksumAlgorithm(String attributeValue) {
        this.attributeValue = attributeValue;
    }

    public String attributeValue() {
        return attributeValue;
    }

    public static ChecksumAlgorithm fromAttributeValue(String value) {
        if (StringUtils.isBlank(value)) {
            throw new PayloadCodecException("Unsupported checksum algorithm: " + value);
        }
        for (ChecksumAlgorithm algorithm : values()) {
            if (algorithm.attributeValue.equalsIgnoreCase(value)) {
                return algorithm;
            }
        }
        throw new PayloadCodecException("Unsupported checksum algorithm: " + value);
    }
}
