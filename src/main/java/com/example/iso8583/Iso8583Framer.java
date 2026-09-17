package com.example.iso8583;

import java.nio.charset.StandardCharsets;

public final class Iso8583Framer {

    private static final int HEADER_LENGTH = 4;
    private static final int MAX_MESSAGE_LENGTH = 9999;

    private Iso8583Framer() {
    }

    public static byte[] addLengthHeader(byte[] message) {

        if (message == null) {
            throw new Iso8583ParseException(
                    Iso8583ErrorCode.INVALID_MESSAGE,
                    "Message tidak boleh null",
                    null,
                    null,
                    null
            );
        }

        int messageLength = message.length;

        if (messageLength > MAX_MESSAGE_LENGTH) {
            throw new Iso8583ParseException(
                    Iso8583ErrorCode.FIELD_LENGTH_EXCEEDED,
                    "Message terlalu panjang. Length=" + messageLength,
                    null,
                    null,
                    String.valueOf(messageLength)
            );
        }

        String lengthHeader = String.format("%04d", messageLength);

        byte[] header = lengthHeader.getBytes(StandardCharsets.US_ASCII);

        byte[] result = new byte[HEADER_LENGTH + messageLength];

        System.arraycopy(header, 0, result, 0, HEADER_LENGTH);
        System.arraycopy(message, 0, result, HEADER_LENGTH, messageLength);

        return result;
    }

    public static int parseLengthHeader(byte[] header) {

        if (header == null) {
            throw new Iso8583ParseException(
                    Iso8583ErrorCode.INVALID_MESSAGE,
                    "Header tidak boleh null",
                    null,
                    null,
                    null
            );
        }

        if (header.length != HEADER_LENGTH) {
            throw new Iso8583ParseException(
                    Iso8583ErrorCode.FIELD_DATA_INCOMPLETE,
                    "Length header harus 4 byte",
                    null,
                    null,
                    null
            );
        }

        String value = new String(header, StandardCharsets.US_ASCII);

        if (!value.matches("\\d{4}")) {
            throw new Iso8583ParseException(
                    Iso8583ErrorCode.INVALID_MESSAGE,
                    "Length header tidak valid: " + value,
                    null,
                    null,
                    value
            );
        }

        int length = Integer.parseInt(value);

        if (length <= 0) {
            throw new Iso8583ParseException(
                    Iso8583ErrorCode.INVALID_MESSAGE,
                    "Message length harus lebih besar dari 0",
                    null,
                    null,
                    value
            );
        }

        return length;
    }

    public static byte[] extractMessage(byte[] framedData) {

        if (framedData == null) {
            throw new Iso8583ParseException(
                    Iso8583ErrorCode.INVALID_MESSAGE,
                    "Framed data tidak boleh null",
                    null,
                    null,
                    null
            );
        }

        int messageLength = parseLengthHeader(
                java.util.Arrays.copyOfRange(
                        framedData,
                        0,
                        Math.min(HEADER_LENGTH, framedData.length)
                )
        );

        int totalLength = HEADER_LENGTH + messageLength;

        if (framedData.length < totalLength) {
            throw new Iso8583ParseException(
                    Iso8583ErrorCode.FIELD_DATA_INCOMPLETE,
                    "Message belum lengkap. Expected="
                            + messageLength
                            + ", Actual="
                            + Math.max(0, framedData.length - HEADER_LENGTH),
                    null,
                    null,
                    null
            );
        }

        byte[] message = new byte[messageLength];

        System.arraycopy(
                framedData,
                HEADER_LENGTH,
                message,
                0,
                messageLength
        );

        return message;
    }
}