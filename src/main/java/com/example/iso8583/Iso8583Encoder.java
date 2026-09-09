package com.example.iso8583;

import java.nio.charset.StandardCharsets;

public final class Iso8583Encoder {

    private Iso8583Encoder() {}

    public static byte[] encodeAscii(String value) {
        if (value == null) {
            throw new Iso8583ParseException(
                    Iso8583ErrorCode.INVALID_MESSAGE,
                    "Value tidak boleh null",
                    null,
                    null,
                    null
            );
        }

        return value.getBytes(StandardCharsets.US_ASCII);
    }

    public static String decodeAscii(byte[] data) {
        if (data == null) {
            throw new Iso8583ParseException(
                    Iso8583ErrorCode.INVALID_MESSAGE,
                    "Data tidak boleh null",
                    null,
                    null,
                    null
            );
        }

        return new String(
                data,
                StandardCharsets.US_ASCII
        );
    }

    public static byte[] encodeBcd(String value) {
        if (value == null) {
            throw new Iso8583ParseException(
                    Iso8583ErrorCode.INVALID_MESSAGE,
                    "Value tidak boleh null",
                    null,
                    null,
                    null
            );
        }

        if (!value.matches("\\d+")) {
            throw new Iso8583ParseException(
                    Iso8583ErrorCode.FIELD_DATA_INVALID,
                    "BCD hanya boleh berisi digit: "
                            + value,
                    null,
                    null,
                    value
            );
        }

        String normalized = value;

        if (normalized.length() % 2 != 0) {
            normalized = "0" + normalized;
        }

        byte[] result = new byte[normalized.length() / 2];

        for (int i = 0; i < normalized.length(); i += 2) {

            int high =
                    Character.digit(
                            normalized.charAt(i),
                            10
                    );

            int low =
                    Character.digit(
                            normalized.charAt(i + 1),
                            10
                    );

            result[i / 2] =
                    (byte) (
                            (high << 4) | low
                    );
        }

        return result;
    }

    // =========================================================
    // BCD DECODER
    // =========================================================

    public static String decodeBcd(byte[] data) {

        if (data == null) {
            throw new Iso8583ParseException(
                    Iso8583ErrorCode.INVALID_MESSAGE,
                    "Data tidak boleh null",
                    null,
                    null,
                    null
            );
        }

        StringBuilder result = new StringBuilder(data.length * 2);

        for (byte b : data) {

            int high = (b >> 4) & 0x0F;

            int low = b & 0x0F;

            if (high > 9 || low > 9) {

                throw new Iso8583ParseException(
                        Iso8583ErrorCode.FIELD_DATA_INVALID,
                        "Invalid BCD byte",
                        null,
                        null,
                        String.format(
                                "%02X",
                                b
                        )
                );
            }

            result.append(high);
            result.append(low);
        }

        return result.toString();
    }

    public static String bytesToHex( byte[] data ) {

        if (data == null) {
            throw new Iso8583ParseException(
                    Iso8583ErrorCode.INVALID_MESSAGE,
                    "Data tidak boleh null",
                    null,
                    null,
                    null
            );
        }

        StringBuilder result = new StringBuilder(data.length * 3);

        for (byte b : data) {
            result.append(String.format("%02X ", b));
        }

        return result.toString().trim();
    }
}