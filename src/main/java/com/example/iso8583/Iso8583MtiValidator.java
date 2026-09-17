package com.example.iso8583;

public final class Iso8583MtiValidator {

    private Iso8583MtiValidator() {
    }

    public static void validateRequest(Iso8583Mti mti) {
        if (mti == null) {
            throw new Iso8583ParseException(
                    Iso8583ErrorCode.INVALID_MTI,
                    "MTI tidak boleh null",
                    null,
                    null,
                    null
            );
        }

        if (mti.getVersion() != 0) {
            throw new Iso8583ParseException(
                    Iso8583ErrorCode.INVALID_MTI,
                    "Version MTI tidak didukung: " + mti.getVersion(),
                    null,
                    null,
                    mti.toString()
            );
        }

        if (mti.getMessageClass() != 2) {
            throw new Iso8583ParseException(
                    Iso8583ErrorCode.INVALID_MTI,
                    "Message class harus Financial (2)",
                    null,
                    null,
                    mti.toString()
            );
        }

        if (!mti.isRequest()) {
            throw new Iso8583ParseException(
                    Iso8583ErrorCode.INVALID_MTI,
                    "MTI harus berupa request",
                    null,
                    null,
                    mti.toString()
            );
        }
    }

    public static void validateResponse(Iso8583Mti mti) {
        if (mti == null) {
            throw new Iso8583ParseException(
                    Iso8583ErrorCode.INVALID_MTI,
                    "MTI tidak boleh null",
                    null,
                    null,
                    null
            );
        }

        if (mti.getVersion() != 0) {
            throw new Iso8583ParseException(
                    Iso8583ErrorCode.INVALID_MTI,
                    "Version MTI tidak didukung: " + mti.getVersion(),
                    null,
                    null,
                    mti.toString()
            );
        }

        if (mti.getMessageClass() != 2) {
            throw new Iso8583ParseException(
                    Iso8583ErrorCode.INVALID_MTI,
                    "Message class harus Financial (2)",
                    null,
                    null,
                    mti.toString()
            );
        }

        if (!mti.isResponse()) {
            throw new Iso8583ParseException(
                    Iso8583ErrorCode.INVALID_MTI,
                    "MTI harus berupa response",
                    null,
                    null,
                    mti.toString()
            );
        }
    }
}