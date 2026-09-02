package com.example.iso8583;


public final class FieldValidator {

    private FieldValidator() {
    }

    public static void validate(
            int field,
            String value,
            FieldDataType dataType,
            int position
    ) {

        if (value == null) {
            throw new Iso8583ParseException(
                    Iso8583ErrorCode.FIELD_DATA_INVALID,
                    "DE " + field +
                            " tidak boleh null",
                    field,
                    position,
                    null
            );
        }

        if (dataType == null) {

            throw new Iso8583ParseException(
                    Iso8583ErrorCode.FIELD_DATA_INVALID,
                    "Data type DE " + field +
                            " tidak boleh null",
                    field,
                    position,
                    value
            );
        }

        switch (dataType) {

            case NUMERIC -> validateNumeric(
                    field,
                    value,
                    position
            );

            case ALPHA -> validateAlpha(
                    field,
                    value,
                    position
            );

            case ALPHANUMERIC -> validateAlphaNumeric(
                    field,
                    value,
                    position
            );

            case BINARY -> validateBinary(
                    field,
                    value,
                    position
            );
        }
    }

    private static void validateNumeric(
            int field,
            String value,
            int position
    ) {

        if (!value.matches("\\d+")) {

            throw new Iso8583ParseException(
                    Iso8583ErrorCode.FIELD_DATA_INVALID,
                    "DE " + field +
                            " harus NUMERIC. " +
                            "Value=" + value,
                    field,
                    position,
                    value
            );
        }
    }

    private static void validateAlpha(
            int field,
            String value,
            int position
    ) {

        if (!value.matches("[A-Za-z]+")) {

            throw new Iso8583ParseException(
                    Iso8583ErrorCode.FIELD_DATA_INVALID,
                    "DE " + field +
                            " harus ALPHA. " +
                            "Value=" + value,
                    field,
                    position,
                    value
            );
        }
    }

    private static void validateAlphaNumeric(
            int field,
            String value,
            int position
    ) {

        if (!value.matches("[A-Za-z0-9]+")) {

            throw new Iso8583ParseException(
                    Iso8583ErrorCode.FIELD_DATA_INVALID,
                    "DE " + field +
                            " harus ALPHANUMERIC. " +
                            "Value=" + value,
                    field,
                    position,
                    value
            );
        }
    }

    private static void validateBinary(
            int field,
            String value,
            int position
    ) {

        if (!value.matches("[0-9A-Fa-f]+")) {

            throw new Iso8583ParseException(
                    Iso8583ErrorCode.FIELD_DATA_INVALID,
                    "DE " + field +
                            " harus BINARY/HEX. " +
                            "Value=" + value,
                    field,
                    position,
                    value
            );
        }

        if (value.length() % 2 != 0) {

            throw new Iso8583ParseException(
                    Iso8583ErrorCode.FIELD_DATA_INVALID,
                    "DE " + field +
                            " binary/hex harus memiliki " +
                            "jumlah karakter genap",
                    field,
                    position,
                    value
            );
        }
    }
}