/*
 * Copyright (c) 2026 Leandro Aguiar
 * Licensed under the MIT License.
 * See LICENSE file in the project root for full license information.
 */
package io.github.leanish.sqs.codec.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;

import io.github.leanish.sqs.codec.SqsPayloadCodecInterceptor;
import io.github.leanish.sqs.codec.algorithms.ChecksumAlgorithm;
import io.github.leanish.sqs.codec.algorithms.CompressionAlgorithm;
import io.github.leanish.sqs.codec.algorithms.EncodingAlgorithm;
import io.github.leanish.sqs.codec.attributes.MessageAttributeUtils;
import io.github.leanish.sqs.codec.attributes.PayloadCodecAttributes;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

@Tag("integration")
@Testcontainers
class SqsPayloadCodecInterceptorIntegrationTest {

    @Container
    private static final LocalStackContainer LOCALSTACK = new LocalStackContainer(
            DockerImageName.parse("localstack/localstack:3.0.2"))
            .withServices("sqs");

    @Test
    void interceptorRoundTripPreservesBodyAndAttributes() {
        String payload = "{\"value\":42}";
        byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);

        try (SqsClient client = sqsClient(
                CompressionAlgorithm.ZSTD,
                EncodingAlgorithm.NONE,
                ChecksumAlgorithm.MD5)) {
            String queueUrl = createQueue(client);

            client.sendMessage(SendMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .messageBody(payload)
                    .messageAttributes(Map.of("shopId", MessageAttributeUtils.stringAttribute("shop-1")))
                    .build());

            Message message = receiveSingleMessage(client, queueUrl);

            assertThat(message.body()).isEqualTo(payload);
            assertThat(message.messageAttributes())
                    .containsKeys(
                            PayloadCodecAttributes.COMPRESSION_ALG,
                            PayloadCodecAttributes.ENCODING_ALG,
                            PayloadCodecAttributes.VERSION,
                            PayloadCodecAttributes.RAW_LENGTH,
                            PayloadCodecAttributes.CHECKSUM_ALG,
                            PayloadCodecAttributes.CHECKSUM,
                            "shopId");

            Map<String, MessageAttributeValue> attributes = message.messageAttributes();
            assertThat(attributes.get(PayloadCodecAttributes.COMPRESSION_ALG).stringValue())
                    .isEqualTo(CompressionAlgorithm.ZSTD.id());
            assertThat(attributes.get(PayloadCodecAttributes.ENCODING_ALG).stringValue())
                    .isEqualTo(EncodingAlgorithm.BASE64.id());
            assertThat(attributes.get(PayloadCodecAttributes.VERSION).dataType())
                    .isEqualTo("Number");
            assertThat(attributes.get(PayloadCodecAttributes.VERSION).stringValue())
                    .isEqualTo(Integer.toString(PayloadCodecAttributes.VERSION_VALUE));
            assertThat(attributes.get(PayloadCodecAttributes.RAW_LENGTH).stringValue())
                    .isEqualTo(Integer.toString(payloadBytes.length));
            assertThat(attributes.get(PayloadCodecAttributes.CHECKSUM_ALG).stringValue())
                    .isEqualTo(ChecksumAlgorithm.MD5.id());
            assertThat(attributes.get(PayloadCodecAttributes.CHECKSUM).stringValue())
                    .isEqualTo(ChecksumAlgorithm.MD5.digestor().checksum(payloadBytes));
        }
    }

    @Test
    void interceptorSkipsChecksumWhenDisabled() {
        String payload = "payload-no-checksum";

        try (SqsClient client = sqsClient(
                CompressionAlgorithm.NONE,
                EncodingAlgorithm.NONE,
                ChecksumAlgorithm.NONE)) {
            String queueUrl = createQueue(client);

            client.sendMessage(SendMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .messageBody(payload)
                    .build());

            Message message = receiveSingleMessage(client, queueUrl);

            assertThat(message.body()).isEqualTo(payload);
            assertThat(message.messageAttributes())
                    .doesNotContainKeys(
                            PayloadCodecAttributes.CHECKSUM_ALG,
                            PayloadCodecAttributes.CHECKSUM);
        }
    }

    private static Message receiveSingleMessage(SqsClient client, String queueUrl) {
        long deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();
        while (System.nanoTime() < deadline) {
            ReceiveMessageResponse response = client.receiveMessage(ReceiveMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .maxNumberOfMessages(1)
                    .messageAttributeNames("All")
                    .waitTimeSeconds(1)
                    .build());
            if (!response.messages().isEmpty()) {
                return response.messages().getFirst();
            }
        }
        throw new AssertionError("No messages received from queue " + queueUrl);
    }

    private static SqsClient sqsClient(
            CompressionAlgorithm compressionAlgorithm,
            EncodingAlgorithm encodingAlgorithm,
            ChecksumAlgorithm checksumAlgorithm) {
        return SqsClient.builder()
                .endpointOverride(LOCALSTACK.getEndpoint())
                .region(Region.of(LOCALSTACK.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(LOCALSTACK.getAccessKey(), LOCALSTACK.getSecretKey())))
                .overrideConfiguration(config -> config.addExecutionInterceptor(
                        new SqsPayloadCodecInterceptor()
                                .withCompressionAlgorithm(compressionAlgorithm)
                                .withEncodingAlgorithm(encodingAlgorithm)
                                .withChecksumAlgorithm(checksumAlgorithm)))
                .checksumValidationEnabled(false)
                .build();
    }

    private static String createQueue(SqsClient client) {
        String queueName = "codec-it-" + UUID.randomUUID();
        return client.createQueue(CreateQueueRequest.builder()
                .queueName(queueName)
                .build())
                .queueUrl();
    }

}
