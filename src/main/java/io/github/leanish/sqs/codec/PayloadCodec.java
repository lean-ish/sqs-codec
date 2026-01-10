/*
 * Copyright (c) 2026 Leandro Aguiar
 * Licensed under the MIT License.
 * See LICENSE file in the project root for full license information.
 */
package io.github.leanish.sqs.codec;

import org.jetbrains.annotations.VisibleForTesting;

import io.github.leanish.sqs.codec.compression.Compressor;
import io.github.leanish.sqs.codec.compression.GzipCompressor;
import io.github.leanish.sqs.codec.compression.UncompressedCompressor;
import io.github.leanish.sqs.codec.compression.ZstdCompressor;
import io.github.leanish.sqs.codec.encoding.Base64Encoder;
import io.github.leanish.sqs.codec.encoding.Encoder;
import io.github.leanish.sqs.codec.encoding.StandardBase64Encoder;
import io.github.leanish.sqs.codec.encoding.UnencodedEncoder;

final class PayloadCodec {

    // COMPRESSORS
    private static final Compressor UNCOMPRESSED = new UncompressedCompressor();
    private static final Compressor ZSTD_COMPRESSION = new ZstdCompressor();
    private static final Compressor GZIP_COMPRESSION = new GzipCompressor();

    // ENCODERS
    private static final Encoder UNENCODED = new UnencodedEncoder();
    private static final Encoder BASE64_ENCODER = new Base64Encoder();
    private static final Encoder BASE64_STD_ENCODER = new StandardBase64Encoder();

    private final Compressor compressor;
    private final Encoder encoder;

    PayloadCodec() {
        this(CompressionAlgorithm.NONE, EncodingAlgorithm.NONE);
    }

    PayloadCodec(
            CompressionAlgorithm compressionAlgorithm,
            EncodingAlgorithm encoding) {
        EncodingAlgorithm effectiveEncoding = effectiveEncodingAlgorithm(compressionAlgorithm, encoding);
        this.compressor = compressorFor(compressionAlgorithm);
        this.encoder = encoderFor(effectiveEncoding);
    }

    public String encode(byte[] payload) {
        return encoder.encode(compressor.compress(payload));
    }

    public byte[] decode(String encoded) {
        return compressor.decompress(encoder.decode(encoded));
    }

    private static Compressor compressorFor(CompressionAlgorithm algorithm) {
        return switch (algorithm) {
            case ZSTD -> ZSTD_COMPRESSION;
            case GZIP -> GZIP_COMPRESSION;
            case NONE -> UNCOMPRESSED;
        };
    }

    private static Encoder encoderFor(EncodingAlgorithm encoding) {
        return switch (encoding) {
            case BASE64 -> BASE64_ENCODER;
            case BASE64_STD -> BASE64_STD_ENCODER;
            case NONE -> UNENCODED;
        };
    }

    @VisibleForTesting
    static EncodingAlgorithm effectiveEncodingAlgorithm(
            CompressionAlgorithm compressionAlgorithm,
            EncodingAlgorithm encodingAlgorithm) {
        if (encodingAlgorithm == EncodingAlgorithm.NONE
                && compressionAlgorithm != CompressionAlgorithm.NONE) {
            return EncodingAlgorithm.BASE64;
        }
        return encodingAlgorithm;
    }
}
