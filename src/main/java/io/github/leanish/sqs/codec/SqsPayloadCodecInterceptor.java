/*
 * Copyright (c) 2026 Leandro Aguiar
 * Licensed under the MIT License.
 * See LICENSE file in the project root for full license information.
 */
package io.github.leanish.sqs.codec;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import io.github.leanish.sqs.codec.checksum.Digestor;
import io.github.leanish.sqs.codec.checksum.Md5Digestor;
import io.github.leanish.sqs.codec.checksum.Sha256Digestor;
import io.github.leanish.sqs.codec.checksum.UndigestedDigestor;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SqsPayloadCodecInterceptor implements ExecutionInterceptor {

    private static final Digestor NONE_DIGESTOR = new UndigestedDigestor();
    private static final Digestor SHA256_DIGESTOR = new Sha256Digestor();
    private static final Digestor MD5_DIGESTOR = new Md5Digestor();

    @Builder.Default
    private CompressionAlgorithm compressionAlgorithm = CompressionAlgorithm.NONE;

    @Builder.Default
    private EncodingAlgorithm encodingAlgorithm = EncodingAlgorithm.NONE;

    @Builder.Default
    private ChecksumAlgorithm checksumAlgorithm = ChecksumAlgorithm.MD5;

    @Override
    public SdkRequest modifyRequest(Context.ModifyRequest context, ExecutionAttributes executionAttributes) {
        SdkRequest request = context.request();
        if (request instanceof SendMessageRequest sendMessageRequest) {
            return encodeSendMessage(sendMessageRequest);
        }
        if (request instanceof SendMessageBatchRequest sendMessageBatchRequest) {
            return encodeSendMessageBatch(sendMessageBatchRequest);
        }
        return request;
    }

    @Override
    public SdkResponse modifyResponse(Context.ModifyResponse context, ExecutionAttributes executionAttributes) {
        SdkResponse response = context.response();
        if (response instanceof ReceiveMessageResponse receiveMessageResponse) {
            return decodeReceiveMessageResponse(receiveMessageResponse);
        }
        return response;
    }

    private SendMessageRequest encodeSendMessage(SendMessageRequest request) {
        if (shouldSkipEncoding(request.messageAttributes())) {
            return request;
        }
        PayloadCodec codec = outboundCodec();
        byte[] payloadBytes = request.messageBody().getBytes(StandardCharsets.UTF_8);
        Map<String, MessageAttributeValue> attributes = new HashMap<>(request.messageAttributes());
        attributes.put(PayloadCodecAttributes.COMPRESSION_ALG, compressionAlgorithmAttribute());
        attributes.put(PayloadCodecAttributes.ENCODING_ALG, encodingAlgorithmAttribute());
        if (checksumEnabled()) {
            attributes.put(PayloadCodecAttributes.CHECKSUM_ALG, checksumAlgorithmAttribute());
            attributes.put(PayloadCodecAttributes.CHECKSUM, checksumAttribute(payloadBytes));
        }
        attributes.put(PayloadCodecAttributes.VERSION, versionAttribute());
        attributes.put(PayloadCodecAttributes.RAW_LENGTH, rawLengthAttribute(payloadBytes.length));
        String encodedBody = codec.encode(payloadBytes);
        return request.toBuilder()
                .messageBody(encodedBody)
                .messageAttributes(attributes)
                .build();
    }

    private SendMessageBatchRequest encodeSendMessageBatch(SendMessageBatchRequest request) {
        List<SendMessageBatchRequestEntry> entries = request.entries();
        List<SendMessageBatchRequestEntry> encodedEntries = entries.stream()
                .map(this::encodeSendMessageEntry)
                .toList();
        return request.toBuilder()
                .entries(encodedEntries)
                .build();
    }

    private SendMessageBatchRequestEntry encodeSendMessageEntry(SendMessageBatchRequestEntry entry) {
        if (shouldSkipEncoding(entry.messageAttributes())) {
            return entry;
        }
        PayloadCodec codec = outboundCodec();
        byte[] payloadBytes = entry.messageBody().getBytes(StandardCharsets.UTF_8);
        Map<String, MessageAttributeValue> attributes = new HashMap<>(entry.messageAttributes());
        attributes.put(PayloadCodecAttributes.COMPRESSION_ALG, compressionAlgorithmAttribute());
        attributes.put(PayloadCodecAttributes.ENCODING_ALG, encodingAlgorithmAttribute());
        if (checksumEnabled()) {
            attributes.put(PayloadCodecAttributes.CHECKSUM_ALG, checksumAlgorithmAttribute());
            attributes.put(PayloadCodecAttributes.CHECKSUM, checksumAttribute(payloadBytes));
        }
        attributes.put(PayloadCodecAttributes.VERSION, versionAttribute());
        attributes.put(PayloadCodecAttributes.RAW_LENGTH, rawLengthAttribute(payloadBytes.length));
        String encodedBody = codec.encode(payloadBytes);
        return entry.toBuilder()
                .messageBody(encodedBody)
                .messageAttributes(attributes)
                .build();
    }

    private boolean shouldSkipEncoding(Map<String, MessageAttributeValue> messageAttributes) {
        return hasAttribute(messageAttributes, PayloadCodecAttributes.COMPRESSION_ALG)
                || hasAttribute(messageAttributes, PayloadCodecAttributes.ENCODING_ALG);
    }

    private boolean hasAttribute(Map<String, MessageAttributeValue> attributes, String name) {
        MessageAttributeValue attributeValue = attributes.get(name);
        if (attributeValue == null) {
            return false;
        }
        return StringUtils.isNotBlank(attributeValue.stringValue());
    }

    private MessageAttributeValue compressionAlgorithmAttribute() {
        return MessageAttributeValue.builder()
                .dataType("String")
                .stringValue(compressionAlgorithm.attributeValue())
                .build();
    }

    private MessageAttributeValue encodingAlgorithmAttribute() {
        return MessageAttributeValue.builder()
                .dataType("String")
                .stringValue(effectiveEncodingAlgorithm().attributeValue())
                .build();
    }

    private MessageAttributeValue checksumAlgorithmAttribute() {
        return MessageAttributeValue.builder()
                .dataType("String")
                .stringValue(checksumAlgorithm.attributeValue())
                .build();
    }

    private MessageAttributeValue checksumAttribute(byte[] payloadBytes) {
        String checksum = outboundChecksum().checksum(payloadBytes);
        return MessageAttributeValue.builder()
                .dataType("String")
                .stringValue(checksum)
                .build();
    }

    private MessageAttributeValue versionAttribute() {
        return MessageAttributeValue.builder()
                .dataType("String")
                .stringValue(PayloadCodecAttributes.VERSION_VALUE)
                .build();
    }

    private MessageAttributeValue rawLengthAttribute(int length) {
        return MessageAttributeValue.builder()
                .dataType("Number")
                .stringValue(Integer.toString(length))
                .build();
    }

    private ReceiveMessageResponse decodeReceiveMessageResponse(ReceiveMessageResponse response) {
        List<Message> messages = response.messages();
        if (messages.isEmpty()) {
            return response;
        }
        List<Message> decoded = new ArrayList<>(messages.size());
        for (Message message : messages) {
            decoded.add(decodeMessageIfNeeded(message));
        }
        return response.toBuilder()
                .messages(decoded)
                .build();
    }

    private Message decodeMessageIfNeeded(Message message) {
        Map<String, MessageAttributeValue> attributes = message.messageAttributes();
        MessageAttributeValue compressionAttribute = attributes.get(PayloadCodecAttributes.COMPRESSION_ALG);
        MessageAttributeValue encodingAttribute = attributes.get(PayloadCodecAttributes.ENCODING_ALG);
        if (compressionAttribute == null && encodingAttribute == null) {
            return message;
        }
        if (compressionAttribute == null || StringUtils.isBlank(compressionAttribute.stringValue())) {
            throw new PayloadCodecException("Missing required SQS attribute: " + PayloadCodecAttributes.COMPRESSION_ALG);
        }
        if (encodingAttribute == null || StringUtils.isBlank(encodingAttribute.stringValue())) {
            throw new PayloadCodecException("Missing required SQS attribute: " + PayloadCodecAttributes.ENCODING_ALG);
        }
        CompressionAlgorithm compressionAlgorithm = compressionFromAttributeValue(
                compressionAttribute.stringValue());
        EncodingAlgorithm encodingAlgorithm = encodingFromAttributeValue(
                encodingAttribute.stringValue());
        if (compressionAlgorithm != CompressionAlgorithm.NONE && encodingAlgorithm == EncodingAlgorithm.NONE) {
            throw new PayloadCodecException("Unsupported payload encoding: " + encodingAttribute.stringValue());
        }
        String version = requiredAttribute(attributes, PayloadCodecAttributes.VERSION);
        if (!PayloadCodecAttributes.VERSION_VALUE.equals(version)) {
            throw new PayloadCodecException("Unsupported codec version: " + version);
        }
        PayloadCodec codec = new PayloadCodec(
                compressionAlgorithm,
                encodingAlgorithm);
        byte[] decodedBytes = codec.decode(message.body());
        if (checksumEnabled()) {
            String checksumAlgorithmValue = requiredAttribute(attributes, PayloadCodecAttributes.CHECKSUM_ALG);
            assertChecksumAlgorithm(checksumAlgorithmValue);
            String expectedChecksum = requiredAttribute(attributes, PayloadCodecAttributes.CHECKSUM);
            String actualChecksum = outboundChecksum().checksum(decodedBytes);
            if (!actualChecksum.equals(expectedChecksum)) {
                throw new PayloadCodecException("Payload checksum mismatch");
            }
        }
        String decodedBody = new String(decodedBytes, StandardCharsets.UTF_8);
        return message.toBuilder()
                .body(decodedBody)
                .build();
    }

    private static String requiredAttribute(Map<String, MessageAttributeValue> attributes, String name) {
        MessageAttributeValue attributeValue = attributes.get(name);
        if (attributeValue == null) {
            throw new PayloadCodecException("Missing required SQS attribute: " + name);
        }
        String value = attributeValue.stringValue();
        if (StringUtils.isBlank(value)) {
            throw new PayloadCodecException("Missing required SQS attribute: " + name);
        }
        return value;
    }

    private PayloadCodec outboundCodec() {
        return new PayloadCodec(compressionAlgorithm, encodingAlgorithm);
    }

    private EncodingAlgorithm effectiveEncodingAlgorithm() {
        if (compressionAlgorithm != CompressionAlgorithm.NONE
                && encodingAlgorithm == EncodingAlgorithm.NONE) {
            return EncodingAlgorithm.BASE64;
        }
        return encodingAlgorithm;
    }

    private static CompressionAlgorithm compressionFromAttributeValue(String value) {
        if (StringUtils.isBlank(value)) {
            throw new PayloadCodecException("Unsupported payload compression: " + value);
        }
        for (CompressionAlgorithm compression : CompressionAlgorithm.values()) {
            if (compression.attributeValue().equalsIgnoreCase(value)) {
                return compression;
            }
        }
        throw new PayloadCodecException("Unsupported payload compression: " + value);
    }

    private static EncodingAlgorithm encodingFromAttributeValue(String value) {
        if (StringUtils.isBlank(value)) {
            throw new PayloadCodecException("Unsupported payload encoding: " + value);
        }
        for (EncodingAlgorithm encoding : EncodingAlgorithm.values()) {
            if (encoding.attributeValue().equalsIgnoreCase(value)) {
                return encoding;
            }
        }
        throw new PayloadCodecException("Unsupported payload encoding: " + value);
    }

    private void assertChecksumAlgorithm(String checksumAlgorithmValue) {
        ChecksumAlgorithm actual = ChecksumAlgorithm.fromAttributeValue(checksumAlgorithmValue);
        if (actual != checksumAlgorithm) {
            throw new PayloadCodecException("Unsupported checksum algorithm: " + checksumAlgorithmValue);
        }
    }

    private Digestor outboundChecksum() {
        return switch (checksumAlgorithm) {
            case MD5 -> MD5_DIGESTOR;
            case SHA256 -> SHA256_DIGESTOR;
            case NONE -> NONE_DIGESTOR;
        };
    }

    private boolean checksumEnabled() {
        return checksumAlgorithm != ChecksumAlgorithm.NONE;
    }
}
