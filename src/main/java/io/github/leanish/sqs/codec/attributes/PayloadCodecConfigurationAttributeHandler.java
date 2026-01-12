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
    private final String version;
    // Preserve the raw attribute value to report exact unsupported algorithms.
    private final @Nullable String checksumAlgorithmValue;

    private PayloadCodecConfigurationAttributeHandler(
            PayloadCodecConfiguration configuration,
            String version,
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
        String compressionValue = PayloadCodecAttributes.attributeValue(attributes, PayloadCodecAttributes.COMPRESSION_ALG);
        String encodingValue = PayloadCodecAttributes.attributeValue(attributes, PayloadCodecAttributes.ENCODING_ALG);
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
        String version = PayloadCodecAttributes.attributeValue(attributes, PayloadCodecAttributes.VERSION);
        if (StringUtils.isNotBlank(version) && !PayloadCodecAttributes.VERSION_VALUE.equals(version)) {
            throw new PayloadCodecException("Unsupported codec version: " + version);
        }
        if (StringUtils.isBlank(version)) {
            version = PayloadCodecAttributes.VERSION_VALUE;
        }
        String checksumAlgorithmValue = PayloadCodecAttributes.attributeValue(attributes, PayloadCodecAttributes.CHECKSUM_ALG);
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
                PayloadCodecAttributes.stringAttribute(configuration.compressionAlgorithm().id()));
        attributes.put(PayloadCodecAttributes.ENCODING_ALG,
                PayloadCodecAttributes.stringAttribute(configuration.encodingAlgorithm().id()));
        attributes.put(PayloadCodecAttributes.VERSION, PayloadCodecAttributes.stringAttribute(version));
        if (StringUtils.isNotBlank(checksumAlgorithmValue)) {
            attributes.put(PayloadCodecAttributes.CHECKSUM_ALG,
                    PayloadCodecAttributes.stringAttribute(checksumAlgorithmValue));
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
        String value = PayloadCodecAttributes.attributeValue(attributes, name);
        return StringUtils.isNotBlank(value);
    }
}
