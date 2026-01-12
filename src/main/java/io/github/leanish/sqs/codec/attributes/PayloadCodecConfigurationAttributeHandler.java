/*
 * Copyright (c) 2026 Leandro Aguiar
 * Licensed under the MIT License.
 * See LICENSE file in the project root for full license information.
 */
package io.github.leanish.sqs.codec.attributes;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

import io.github.leanish.sqs.codec.PayloadCodecConfiguration;
import io.github.leanish.sqs.codec.PayloadCodecException;
import io.github.leanish.sqs.codec.algorithms.ChecksumAlgorithm;
import io.github.leanish.sqs.codec.algorithms.CompressionAlgorithm;
import io.github.leanish.sqs.codec.algorithms.EncodingAlgorithm;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;

public final class PayloadCodecConfigurationAttributeHandler {

    private final PayloadCodecConfiguration configuration;
    private final int version;
    // Preserve the raw attribute value to report exact unsupported algorithms.
    private final @Nullable String checksumAlgorithmValue;

    private PayloadCodecConfigurationAttributeHandler(
            PayloadCodecConfiguration configuration,
            int version,
            @Nullable String checksumAlgorithmValue) {
        this.configuration = configuration;
        this.version = version;
        this.checksumAlgorithmValue = checksumAlgorithmValue;
    }

    public static boolean hasCodecAttributes(Map<String, MessageAttributeValue> attributes) {
        return hasNonBlankAttribute(attributes, PayloadCodecAttributes.COMPRESSION_ALG)
                || hasNonBlankAttribute(attributes, PayloadCodecAttributes.ENCODING_ALG);
    }

    public static PayloadCodecConfigurationAttributeHandler forOutbound(PayloadCodecConfiguration configuration) {
        EncodingAlgorithm effectiveEncoding = EncodingAlgorithm.effectiveFor(
                configuration.compressionAlgorithm(),
                configuration.encodingAlgorithm());
        PayloadCodecConfiguration effectiveConfiguration = new PayloadCodecConfiguration(
                configuration.compressionAlgorithm(),
                effectiveEncoding,
                configuration.checksumAlgorithm());
        String checksumAlgorithmId = "";
        if (configuration.checksumAlgorithm() != ChecksumAlgorithm.NONE) {
            checksumAlgorithmId = configuration.checksumAlgorithm().id();
        }
        return new PayloadCodecConfigurationAttributeHandler(
                effectiveConfiguration,
                PayloadCodecAttributes.VERSION_VALUE,
                checksumAlgorithmId);
    }

    public static PayloadCodecConfigurationAttributeHandler fromAttributes(Map<String, MessageAttributeValue> attributes) {
        String compressionValue = MessageAttributeUtils.attributeValue(attributes, PayloadCodecAttributes.COMPRESSION_ALG);
        String encodingValue = MessageAttributeUtils.attributeValue(attributes, PayloadCodecAttributes.ENCODING_ALG);
        CompressionAlgorithm compressionAlgorithm = CompressionAlgorithm.NONE;
        if (StringUtils.isNotBlank(compressionValue)) {
            compressionAlgorithm = compressionFromAttributeValue(compressionValue);
        }
        EncodingAlgorithm encodingAlgorithm = EncodingAlgorithm.NONE;
        if (StringUtils.isNotBlank(encodingValue)) {
            encodingAlgorithm = encodingFromAttributeValue(encodingValue);
        }
        if (compressionAlgorithm != CompressionAlgorithm.NONE
                && encodingAlgorithm == EncodingAlgorithm.NONE
                && StringUtils.isNotBlank(encodingValue)) {
            throw new PayloadCodecException("Unsupported payload encoding: " + encodingValue);
        }
        String versionValue = MessageAttributeUtils.attributeValue(attributes, PayloadCodecAttributes.VERSION);
        int version = PayloadCodecAttributes.VERSION_VALUE;
        if (StringUtils.isNotBlank(versionValue)) {
            try {
                version = Integer.parseInt(versionValue);
            } catch (NumberFormatException e) {
                throw new PayloadCodecException("Unsupported codec version: " + versionValue);
            }
            if (version != PayloadCodecAttributes.VERSION_VALUE) {
                throw new PayloadCodecException("Unsupported codec version: " + versionValue);
            }
        }
        String checksumAlgorithmValue = MessageAttributeUtils.attributeValue(attributes, PayloadCodecAttributes.CHECKSUM_ALG);
        ChecksumAlgorithm checksumAlgorithm = ChecksumAlgorithm.NONE;
        if (StringUtils.isNotBlank(checksumAlgorithmValue)) {
            checksumAlgorithm = ChecksumAlgorithm.fromAttributeValue(checksumAlgorithmValue);
        }
        PayloadCodecConfiguration configuration = new PayloadCodecConfiguration(
                compressionAlgorithm,
                encodingAlgorithm,
                checksumAlgorithm);
        return new PayloadCodecConfigurationAttributeHandler(
                configuration,
                version,
                checksumAlgorithmValue);
    }

    public PayloadCodecConfiguration configuration() {
        return configuration;
    }

    public void applyTo(Map<String, MessageAttributeValue> attributes) {
        attributes.put(PayloadCodecAttributes.COMPRESSION_ALG,
                MessageAttributeUtils.stringAttribute(configuration.compressionAlgorithm().id()));
        attributes.put(PayloadCodecAttributes.ENCODING_ALG,
                MessageAttributeUtils.stringAttribute(configuration.encodingAlgorithm().id()));
        attributes.put(PayloadCodecAttributes.VERSION, MessageAttributeUtils.numberAttribute(version));
        if (StringUtils.isNotBlank(checksumAlgorithmValue)) {
            attributes.put(PayloadCodecAttributes.CHECKSUM_ALG,
                    MessageAttributeUtils.stringAttribute(checksumAlgorithmValue));
        }
    }

    public static CompressionAlgorithm compressionFromAttributeValue(String value) {
        if (StringUtils.isBlank(value)) {
            throw new PayloadCodecException("Unsupported payload compression: " + value);
        }
        for (CompressionAlgorithm compression : CompressionAlgorithm.values()) {
            if (compression.id().equalsIgnoreCase(value)) {
                return compression;
            }
        }

        throw new PayloadCodecException("Unsupported payload compression: " + value);
    }

    public static EncodingAlgorithm encodingFromAttributeValue(String value) {
        if (StringUtils.isBlank(value)) {
            throw new PayloadCodecException("Unsupported payload encoding: " + value);
        }
        for (EncodingAlgorithm encoding : EncodingAlgorithm.values()) {
            if (encoding.id().equalsIgnoreCase(value)) {
                return encoding;
            }
        }
        throw new PayloadCodecException("Unsupported payload encoding: " + value);
    }

    private static boolean hasNonBlankAttribute(Map<String, MessageAttributeValue> attributes, String name) {
        String value = MessageAttributeUtils.attributeValue(attributes, name);
        return StringUtils.isNotBlank(value);
    }
}
