/*
 * Copyright (c) 2026 Leandro Aguiar
 * Licensed under the MIT License.
 * See LICENSE file in the project root for full license information.
 */
package io.github.leanish.sqs.codec.algorithms;

import org.apache.commons.lang3.StringUtils;

import io.github.leanish.sqs.codec.PayloadCodecException;
import io.github.leanish.sqs.codec.algorithms.checksum.Digestor;
import io.github.leanish.sqs.codec.algorithms.checksum.Md5Digestor;
import io.github.leanish.sqs.codec.algorithms.checksum.Sha256Digestor;
import io.github.leanish.sqs.codec.algorithms.checksum.UndigestedDigestor;

public enum ChecksumAlgorithm {
    /** MD5 checksum for lightweight integrity checks. */
    MD5("md5"),
    /** SHA-256 checksum for stronger integrity guarantees. */
    SHA256("sha256"),
    /** No checksum; integrity attributes are omitted. */
    NONE("none");

    private static final Digestor MD5_DIGESTOR = new Md5Digestor();
    private static final Digestor SHA256_DIGESTOR = new Sha256Digestor();
    private static final Digestor UNDIGESTED = new UndigestedDigestor();

    private final String id;

    ChecksumAlgorithm(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static ChecksumAlgorithm fromAttributeValue(String value) {
        if (StringUtils.isBlank(value)) {
            throw new PayloadCodecException("Unsupported checksum algorithm: " + value);
        }
        for (ChecksumAlgorithm algorithm : values()) {
            if (algorithm.id.equalsIgnoreCase(value)) {
                return algorithm;
            }
        }
        throw new PayloadCodecException("Unsupported checksum algorithm: " + value);
    }

    public Digestor digestor() {
        return switch (this) {
            case MD5 -> MD5_DIGESTOR;
            case SHA256 -> SHA256_DIGESTOR;
            case NONE -> UNDIGESTED;
        };
    }
}
