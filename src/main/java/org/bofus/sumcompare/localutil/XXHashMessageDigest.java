package org.bofus.sumcompare.localutil;

import java.security.MessageDigest;
import net.jpountz.xxhash.StreamingXXHash32;
import net.jpountz.xxhash.StreamingXXHash64;
import net.jpountz.xxhash.XXHashFactory;

/**
 * Custom MessageDigest implementation for XXHash algorithms.
 * This wrapper allows XXHash to be used with the existing MessageDigest-based
 * infrastructure.
 */
public class XXHashMessageDigest extends MessageDigest {
    private final boolean is64Bit;
    private final XXHashFactory factory;
    private StreamingXXHash32 xxhash32;
    private StreamingXXHash64 xxhash64;
    private byte[] buffer;
    private int bufferPos;
    private static final int SEED = 0; // Default seed value
    private static final int INITIAL_BUFFER_SIZE = 8192;

    /**
     * Create a new XXHash MessageDigest.
     * 
     * @param algorithm Either "XXHASH32" or "XXHASH64"
     */
    public XXHashMessageDigest(String algorithm) {
        super(algorithm);
        this.factory = XXHashFactory.fastestInstance();
        this.is64Bit = algorithm.equals("XXHASH64");
        this.buffer = new byte[INITIAL_BUFFER_SIZE];
        this.bufferPos = 0;

        if (is64Bit) {
            xxhash64 = factory.newStreamingHash64(SEED);
        } else {
            xxhash32 = factory.newStreamingHash32(SEED);
        }
    }

    @Override
    protected void engineUpdate(byte input) {
        buffer[bufferPos++] = input;
        if (bufferPos >= buffer.length) {
            flushBuffer();
        }
    }

    @Override
    protected void engineUpdate(byte[] input, int offset, int len) {
        int remaining = len;
        int currentOffset = offset;

        while (remaining > 0) {
            int spaceInBuffer = buffer.length - bufferPos;
            int toCopy = Math.min(remaining, spaceInBuffer);

            System.arraycopy(input, currentOffset, buffer, bufferPos, toCopy);
            bufferPos += toCopy;
            currentOffset += toCopy;
            remaining -= toCopy;

            if (bufferPos >= buffer.length) {
                flushBuffer();
            }
        }
    }

    private void flushBuffer() {
        if (bufferPos > 0) {
            if (is64Bit) {
                xxhash64.update(buffer, 0, bufferPos);
            } else {
                xxhash32.update(buffer, 0, bufferPos);
            }
            bufferPos = 0;
        }
    }

    @Override
    protected byte[] engineDigest() {
        flushBuffer();

        if (is64Bit) {
            long hash = xxhash64.getValue();
            byte[] result = new byte[8];
            for (int i = 0; i < 8; i++) {
                result[7 - i] = (byte) (hash & 0xFF);
                hash >>= 8;
            }
            engineReset();
            return result;
        } else {
            int hash = xxhash32.getValue();
            byte[] result = new byte[4];
            for (int i = 0; i < 4; i++) {
                result[3 - i] = (byte) (hash & 0xFF);
                hash >>= 8;
            }
            engineReset();
            return result;
        }
    }

    @Override
    protected void engineReset() {
        bufferPos = 0;
        if (is64Bit) {
            xxhash64 = factory.newStreamingHash64(SEED);
        } else {
            xxhash32 = factory.newStreamingHash32(SEED);
        }
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return new XXHashMessageDigest(getAlgorithm());
    }
}
