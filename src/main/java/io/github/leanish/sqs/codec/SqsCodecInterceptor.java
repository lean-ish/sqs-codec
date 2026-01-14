/*
 * Copyright (c) 2026 Leandro Aguiar
 * Licensed under the MIT License.
 * See LICENSE file in the project root for full license information.
 */
package io.github.leanish.sqs.codec;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.github.leanish.sqs.codec.algorithms.ChecksumAlgorithm;
import io.github.leanish.sqs.codec.algorithms.CompressionAlgorithm;
import io.github.leanish.sqs.codec.algorithms.EncodingAlgorithm;
import io.github.leanish.sqs.codec.attributes.CodecAttributes;
import io.github.leanish.sqs.codec.attributes.CodecConfigurationAttributeHandler;
import io.github.leanish.sqs.codec.attributes.MessageAttributeUtils;
import io.github.leanish.sqs.codec.attributes.PayloadChecksumAttributeHandler;
import io.github.leanish.sqs.codec.attributes.PayloadRawLengthAttributeHandler;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.With;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

/**
 * AWS SDK v2 execution interceptor that encodes/decodes SQS message bodies and manages codec attributes.
 */
@With
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SqsCodecInterceptor implements ExecutionInterceptor {

    private static final SqsCodecInterceptor DEFAULT = new SqsCodecInterceptor(
            CompressionAlgorithm.NONE,
            EncodingAlgorithm.NONE,
            ChecksumAlgorithm.MD5);
    private static final List<String> CODEC_ATTRIBUTE_NAMES = List.of(
            CodecAttributes.CONF,
            CodecAttributes.CHECKSUM,
            CodecAttributes.RAW_LENGTH);

    private final CompressionAlgorithm compressionAlgorithm;
    private final EncodingAlgorithm encodingAlgorithm;
    private final ChecksumAlgorithm checksumAlgorithm;

    @Override
    public SdkRequest modifyRequest(Context.ModifyRequest context, ExecutionAttributes executionAttributes) {
        SdkRequest request = context.request();
        if (request instanceof SendMessageRequest sendMessageRequest) {
            return encodeSendMessage(sendMessageRequest);
        }
        if (request instanceof SendMessageBatchRequest sendMessageBatchRequest) {
            return encodeSendMessageBatch(sendMessageBatchRequest);
        }
        if (request instanceof ReceiveMessageRequest receiveMessageRequest) {
            return ensureCodecAttributesRequested(receiveMessageRequest);
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

    @SuppressWarnings("DuplicatedCode") // known but sadly SendMessageRequest and SendMessageBatchRequestEntry are not polymorphic
    private SendMessageRequest encodeSendMessage(SendMessageRequest request) {
        if (CodecConfigurationAttributeHandler.hasCodecAttributes(request.messageAttributes())) {
            // Already encoded upstream; avoid double-encoding or overwriting attributes (if valid)
            CodecConfigurationAttributeHandler.fromAttributes(request.messageAttributes());
            return request;
        }

        Codec codec = outboundCodec();
        byte[] payloadBytes = request.messageBody().getBytes(StandardCharsets.UTF_8);
        CodecConfiguration configuration = configuration();

        Map<String, MessageAttributeValue> attributes = new HashMap<>(request.messageAttributes());
        CodecConfigurationAttributeHandler.forOutbound(configuration)
                .applyTo(attributes);
        PayloadRawLengthAttributeHandler.forOutbound(payloadBytes.length)
                .applyTo(attributes);
        PayloadChecksumAttributeHandler.forOutbound(configuration.checksumAlgorithm(), payloadBytes)
                .applyTo(attributes);
        String encodedBody = new String(codec.encode(payloadBytes), StandardCharsets.UTF_8);

        return request.toBuilder()
                .messageBody(encodedBody)
                .messageAttributes(attributes)
                .build();
    }

    private SendMessageBatchRequest encodeSendMessageBatch(SendMessageBatchRequest request) {
        List<SendMessageBatchRequestEntry> encodedEntries = request.entries()
                .stream()
                .map(this::encodeSendMessageEntry)
                .toList();

        return request.toBuilder()
                .entries(encodedEntries)
                .build();
    }

    @SuppressWarnings("DuplicatedCode") // known but sadly SendMessageRequest and SendMessageBatchRequestEntry are not polymorphic
    private SendMessageBatchRequestEntry encodeSendMessageEntry(SendMessageBatchRequestEntry entry) {
        if (CodecConfigurationAttributeHandler.hasCodecAttributes(entry.messageAttributes())) {
            // Already encoded upstream; avoid double-encoding or overwriting attributes (if valid)
            CodecConfigurationAttributeHandler.fromAttributes(entry.messageAttributes());
            return entry;
        }

        Codec codec = outboundCodec();
        byte[] payloadBytes = entry.messageBody().getBytes(StandardCharsets.UTF_8);
        CodecConfiguration configuration = configuration();

        Map<String, MessageAttributeValue> attributes = new HashMap<>(entry.messageAttributes());
        CodecConfigurationAttributeHandler.forOutbound(configuration)
                .applyTo(attributes);
        PayloadRawLengthAttributeHandler.forOutbound(payloadBytes.length)
                .applyTo(attributes);
        PayloadChecksumAttributeHandler.forOutbound(configuration.checksumAlgorithm(), payloadBytes)
                .applyTo(attributes);
        String encodedBody = new String(codec.encode(payloadBytes), StandardCharsets.UTF_8);

        return entry.toBuilder()
                .messageBody(encodedBody)
                .messageAttributes(attributes)
                .build();
    }

    private ReceiveMessageRequest ensureCodecAttributesRequested(ReceiveMessageRequest request) {
        Set<String> attributeNames = new HashSet<>(request.messageAttributeNames());
        if (attributeNames.contains("All") || attributeNames.containsAll(CODEC_ATTRIBUTE_NAMES)) {
            return request;
        }

        Set<String> neededAttributeNames = Stream.concat(attributeNames.stream(), CODEC_ATTRIBUTE_NAMES.stream())
                .collect(Collectors.toUnmodifiableSet());
        return request.toBuilder()
                .messageAttributeNames(neededAttributeNames)
                .build();
    }

    private ReceiveMessageResponse decodeReceiveMessageResponse(ReceiveMessageResponse response) {
        List<Message> messages = response.messages();
        if (messages.isEmpty()) {
            return response;
        }

        List<Message> decoded = messages.stream()
                .map(this::decodeMessageIfNeeded)
                .toList();

        return response.toBuilder()
                .messages(decoded)
                .build();
    }

    private Message decodeMessageIfNeeded(Message message) {
        Map<String, MessageAttributeValue> attributes = message.messageAttributes();
        if (!CodecConfigurationAttributeHandler.hasAnyAttributes(attributes)) {
            // allowing messages queued before this codec was added
            return message;
        }

        CodecConfiguration configuration = CodecConfigurationAttributeHandler.fromAttributes(attributes)
                .configuration();
        boolean shouldDecode = configuration.compressionAlgorithm() != CompressionAlgorithm.NONE
                || configuration.encodingAlgorithm() != EncodingAlgorithm.NONE;
        String checksumValue = MessageAttributeUtils.attributeValue(attributes, CodecAttributes.CHECKSUM);
        boolean shouldValidateChecksum = PayloadChecksumAttributeHandler.needsValidation(checksumValue, configuration.checksumAlgorithm());
        if (!shouldDecode && !shouldValidateChecksum) {
            return message;
        }

        byte[] payloadBytes;
        if (shouldDecode) {
            Codec codec = new Codec(configuration.compressionAlgorithm(), configuration.encodingAlgorithm());
            payloadBytes = codec.decode(message.body().getBytes(StandardCharsets.UTF_8));
        } else {
            payloadBytes = message.body().getBytes(StandardCharsets.UTF_8);
        }
        PayloadChecksumAttributeHandler.validate(configuration.checksumAlgorithm(), checksumValue, payloadBytes);
        if (!shouldDecode) {
            return message;
        }

        return message.toBuilder()
                .body(new String(payloadBytes, StandardCharsets.UTF_8))
                .build();
    }

    private Codec outboundCodec() {
        return new Codec(compressionAlgorithm, encodingAlgorithm);
    }

    private CodecConfiguration configuration() {
        return new CodecConfiguration(
                CodecAttributes.VERSION_VALUE,
                compressionAlgorithm,
                encodingAlgorithm,
                checksumAlgorithm);
    }

    public static SqsCodecInterceptor defaultInterceptor() {
        return DEFAULT;
    }
}
