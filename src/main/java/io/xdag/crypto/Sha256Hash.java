/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2030 The XdagJ Developers
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package io.xdag.crypto;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

public class Sha256Hash {
    /** bytes 消息长度为32字节 */
    public static final int LENGTH = 32;
    public static final Sha256Hash ZERO_HASH = wrap(new byte[LENGTH]);
    private final byte[] bytes;

    public Sha256Hash(byte[] rawHashBytes) {
        // Assert.isTrue(rawHashBytes.length == LENGTH, "rawHashBytes length !=" +
        // LENGTH);
        this.bytes = rawHashBytes;
    }

    public static Sha256Hash wrap(String hexString) throws DecoderException {
        return wrap(Hex.decodeHex(hexString));
    }

    public static Sha256Hash wrap(byte[] rawHashBytes) {
        return new Sha256Hash(rawHashBytes);
    }

    public static byte[] hashTwice(byte[] input) {
        return hashTwice(input, 0, input.length);
    }

    public static byte[] hashTwice(byte[] input, int offset, int length) {
        MessageDigest digest = newDigest();
        digest.update(input, offset, length);
        return digest.digest(digest.digest());
    }

    /** MessageDigest not thread safe */
    public static MessageDigest newDigest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            // Can't happen.
            throw new RuntimeException(e);
        }
    }

    public static byte[] bytesToSHA256(byte[] bytes) {
        byte[] encrypt = new byte[256];
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            messageDigest.update(bytes);
            encrypt = messageDigest.digest();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return encrypt;
    }

    public static byte[] bytesToSHA256(byte[] data1, byte[] data2) {
        byte[] bytes = new byte[data1.length + data1.length];
        System.arraycopy(data1, 0, bytes, 0, data1.length);
        System.arraycopy(data2, 0, bytes, data1.length, data2.length);

        return bytesToSHA256(bytes);
    }

    public byte[] getBytes() {
        return bytes;
    }
}
