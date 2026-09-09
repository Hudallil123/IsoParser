package com.example.iso8583;

import java.nio.charset.StandardCharsets;

public final class Iso8583Framer {

    private static final int HEADER_LENGTH = 4;

    private Iso8583Framer() {
    }

    public static byte[] addLengthHeader(
            byte[] message
    ) {

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

        if (messageLength > 9999) {
            throw new Iso8583ParseException(
                    Iso8583ErrorCode.FIELD_LENGTH_EXCEEDED,
                    "Message terlalu panjang. Length="
                            + messageLength,
                    null,
                    null,
                    String.valueOf(messageLength)
            );
        }

        String lengthHeader = String.format("%04d",messageLength);

        byte[] header = lengthHeader.getBytes(StandardCharsets.US_ASCII);

        byte[] result = new byte[HEADER_LENGTH + messageLength];

        System.arraycopy(
                header,
                0,
                result,
                0,
                HEADER_LENGTH
        );

        System.arraycopy(
                message,
                0,
                result,
                HEADER_LENGTH,
                messageLength
        );

        return result;
    }

    public static int readLengthHeader(
            byte[] data
    ) {

        if (data == null) {
            throw new Iso8583ParseException(
                    Iso8583ErrorCode.INVALID_MESSAGE,
                    "Data tidak boleh null",
                    null,
                    null,
                    null
            );
        }

        if (data.length < HEADER_LENGTH) {
            throw new Iso8583ParseException(
                    Iso8583ErrorCode.FIELD_DATA_INCOMPLETE,
                    "Data belum memiliki length header lengkap",
                    null,
                    null,
                    null
            );
        }

        String header =
                new String(
                        data,
                        0,
                        HEADER_LENGTH,
                        StandardCharsets.US_ASCII
                );

        if (!header.matches("\\d{4}")) {
            throw new Iso8583ParseException(
                    Iso8583ErrorCode.INVALID_MESSAGE,
                    "Length header tidak valid: "
                            + header,
                    null,
                    0,
                    header
            );
        }

        return Integer.parseInt(header);
    }

    public static byte[] extractMessage(
            byte[] framedData
    ) {

        int messageLength = readLengthHeader(framedData);

        int totalLength = HEADER_LENGTH + messageLength;

        if (framedData.length < totalLength) {
            throw new Iso8583ParseException(
                    Iso8583ErrorCode.FIELD_DATA_INCOMPLETE,
                    "Message belum lengkap. Expected="
                            + messageLength
                            + ", Actual="
                            + Math.max(
                            0,
                            framedData.length - HEADER_LENGTH
                    ),
                    null,
                    null,
                    null
            );
        }

        byte[] message =
                new byte[messageLength];

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