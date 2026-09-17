package com.example.iso8583;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class Iso8583Builder {

    private final Map<Integer, IsoFieldDefinition> definitions;

    public Iso8583Builder(Map<Integer, IsoFieldDefinition> definitions) {
        this.definitions = definitions;
    }

    public String build(IsoMessage message) {

        validateMessage(message);

        String primaryBitmap = buildPrimaryBitmap(message);
        boolean hasSecondaryBitmap = hasSecondaryFields(message);

        String secondaryBitmap = hasSecondaryBitmap
                ? buildSecondaryBitmap(message)
                : null;

        StringBuilder result = new StringBuilder();

        result.append(message.getMti());
        result.append(primaryBitmap);

        if (hasSecondaryBitmap) {
            result.append(secondaryBitmap);
        }

        appendFields(result, message, 2, 64);

        if (hasSecondaryBitmap) {
            appendFields(result, message, 65, 128);
        }

        return result.toString();
    }

    public byte[] buildBytes(IsoMessage message) {

        validateMessage(message);

        String primaryBitmap = buildPrimaryBitmap(message);
        boolean hasSecondaryBitmap = hasSecondaryFields(message);

        String secondaryBitmap = hasSecondaryBitmap
                ? buildSecondaryBitmap(message)
                : null;

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        writeAscii(output, message.getMti());
        writeAscii(output, primaryBitmap);

        if (hasSecondaryBitmap) {
            writeAscii(output, secondaryBitmap);
        }

        appendFieldsBytes(output, message, 2, 64);

        if (hasSecondaryBitmap) {
            appendFieldsBytes(output, message, 65, 128);
        }

        return output.toByteArray();
    }

    private void appendFields(
            StringBuilder result,
            IsoMessage message,
            int startField,
            int endField
    ) {

        for (int field = startField; field <= endField; field++) {

            if (!BitmapUtil.isFieldPresent(
                    buildFullBinaryBitmap(message),
                    field
            )) {
                continue;
            }

            String value = message.getField(field);

            IsoFieldDefinition definition =
                    definitions.get(field);

            if (definition == null) {
                throw new Iso8583ParseException(
                        Iso8583ErrorCode.FIELD_DEFINITION_NOT_FOUND,
                        "Definition DE " + field + " tidak ditemukan",
                        field,
                        null,
                        value
                );
            }

            appendFieldValue(
                    result,
                    field,
                    value,
                    definition
            );
        }
    }

    private void appendFieldsBytes(
            ByteArrayOutputStream output,
            IsoMessage message,
            int startField,
            int endField
    ) {

        for (int field = startField; field <= endField; field++) {

            if (!isFieldPresent(message, field)) {
                continue;
            }

            String value = message.getField(field);

            IsoFieldDefinition definition =
                    definitions.get(field);

            if (definition == null) {
                throw new Iso8583ParseException(
                        Iso8583ErrorCode.FIELD_DEFINITION_NOT_FOUND,
                        "Definition DE " + field + " tidak ditemukan",
                        field,
                        null,
                        value
                );
            }

            appendFieldBytes(
                    output,
                    field,
                    value,
                    definition
            );
        }
    }

    private void appendFieldValue(
            StringBuilder result,
            int field,
            String value,
            IsoFieldDefinition definition
    ) {

        validateFieldValue(
                field,
                value,
                definition
        );

        switch (definition.getFieldType()) {

            case FIXED -> {

                validateFixedLength(
                        field,
                        value,
                        definition
                );

                result.append(value);
            }

            case LLVAR -> {

                validateVariableLength(
                        field,
                        value,
                        definition,
                        2
                );

                result.append(
                        String.format(
                                "%02d",
                                value.length()
                        )
                );

                result.append(value);
            }

            case LLLVAR -> {

                validateVariableLength(
                        field,
                        value,
                        definition,
                        3
                );

                result.append(
                        String.format(
                                "%03d",
                                value.length()
                        )
                );

                result.append(value);
            }
        }
    }

    private void appendFieldBytes(
            ByteArrayOutputStream output,
            int field,
            String value,
            IsoFieldDefinition definition
    ) {

        validateFieldValue(
                field,
                value,
                definition
        );

        byte[] encodedValue;

        switch (definition.getFieldType()) {

            case FIXED -> {

                validateFixedLength(
                        field,
                        value,
                        definition
                );

                encodedValue =
                        Iso8583Encoder.encodeField(
                                value,
                                definition
                        );

                output.writeBytes(encodedValue);
            }

            case LLVAR -> {

                validateVariableLength(
                        field,
                        value,
                        definition,
                        2
                );

                writeVariableLength(
                        output,
                        value,
                        definition,
                        2
                );

                encodedValue =
                        Iso8583Encoder.encodeField(
                                value,
                                definition
                        );

                output.writeBytes(encodedValue);
            }

            case LLLVAR -> {

                validateVariableLength(
                        field,
                        value,
                        definition,
                        3
                );

                writeVariableLength(
                        output,
                        value,
                        definition,
                        3
                );

                encodedValue =
                        Iso8583Encoder.encodeField(
                                value,
                                definition
                        );

                output.writeBytes(encodedValue);
            }
        }
    }

    private void writeVariableLength(
            ByteArrayOutputStream output,
            String value,
            IsoFieldDefinition definition,
            int lengthDigits
    ) {

        int logicalLength = value.length();

        String lengthValue;

        if (lengthDigits == 2) {
            lengthValue =
                    String.format(
                            "%02d",
                            logicalLength
                    );
        } else {
            lengthValue =
                    String.format(
                            "%03d",
                            logicalLength
                    );
        }

        writeAscii(
                output,
                lengthValue
        );
    }

    private void writeAscii(
            ByteArrayOutputStream output,
            String value
    ) {

        byte[] bytes =
                value.getBytes(
                        StandardCharsets.US_ASCII
                );

        output.writeBytes(bytes);
    }

    private boolean isFieldPresent(
            IsoMessage message,
            int field
    ) {

        String binaryBitmap =
                buildFullBinaryBitmap(message);

        return BitmapUtil.isFieldPresent(
                binaryBitmap,
                field
        );
    }

    private String buildFullBinaryBitmap(
            IsoMessage message
    ) {

        String primaryBitmap =
                buildPrimaryBitmap(message);

        String primaryBinary =
                BitmapUtil.hexToBinary(
                        primaryBitmap
                );

        if (!hasSecondaryFields(message)) {
            return primaryBinary;
        }

        String secondaryBitmap =
                buildSecondaryBitmap(message);

        String secondaryBinary =
                BitmapUtil.hexToBinary(
                        secondaryBitmap
                );

        return primaryBinary + secondaryBinary;
    }

    private String buildPrimaryBitmap(
            IsoMessage message
    ) {

        StringBuilder binary =
                new StringBuilder(
                        "0000000000000000000000000000000000000000000000000000000000000000"
                );

        for (int field = 2; field <= 64; field++) {

            if (message.getField(field) != null) {

                binary.setCharAt(
                        field - 1,
                        '1'
                );
            }
        }

        if (hasSecondaryFields(message)) {

            binary.setCharAt(
                    0,
                    '1'
            );
        }

        return binaryToHex(
                binary.toString()
        );
    }

    private String buildSecondaryBitmap(
            IsoMessage message
    ) {

        StringBuilder binary =
                new StringBuilder(
                        "0000000000000000000000000000000000000000000000000000000000000000"
                );

        for (int field = 65; field <= 128; field++) {

            if (message.getField(field) != null) {

                binary.setCharAt(
                        field - 65,
                        '1'
                );
            }
        }

        return binaryToHex(
                binary.toString()
        );
    }

    private boolean hasSecondaryFields(
            IsoMessage message
    ) {

        for (int field = 65; field <= 128; field++) {

            if (message.getField(field) != null) {
                return true;
            }
        }

        return false;
    }

    private String binaryToHex(
            String binary
    ) {

        StringBuilder hex =
                new StringBuilder(
                        binary.length() / 4
                );

        for (int i = 0; i < binary.length(); i += 4) {

            String chunk =
                    binary.substring(i, i + 4);

            int value =
                    Integer.parseInt(
                            chunk,
                            2
                    );

            hex.append(
                    Integer.toHexString(
                            value
                    ).toUpperCase()
            );
        }

        return hex.toString();
    }

    private void validateMessage(
            IsoMessage message
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

        if (message.getMti() == null ||
                message.getMti().length() != 4) {

            throw new Iso8583ParseException(
                    Iso8583ErrorCode.INVALID_MTI,
                    "MTI harus 4 karakter",
                    null,
                    null,
                    message.getMti()
            );
        }
    }

    private void validateFieldValue(
            int field,
            String value,
            IsoFieldDefinition definition
    ) {

        if (value == null) {

            if (definition.isRequired()) {

                throw new Iso8583ParseException(
                        Iso8583ErrorCode.FIELD_REQUIRED_MISSING,
                        "DE " + field + " wajib diisi",
                        field,
                        null,
                        null
                );
            }

            throw new Iso8583ParseException(
                    Iso8583ErrorCode.INVALID_MESSAGE,
                    "DE " + field + " value null",
                    field,
                    null,
                    null
            );
        }
    }

    private void validateFixedLength(
            int field,
            String value,
            IsoFieldDefinition definition
    ) {

        if (value.length() != definition.getMaxLength()) {

            throw new Iso8583ParseException(
                    Iso8583ErrorCode.FIELD_LENGTH_INVALID,
                    "DE " + field +
                            " harus memiliki length " +
                            definition.getMaxLength() +
                            ", actual=" +
                            value.length(),
                    field,
                    null,
                    value
            );
        }
    }

    private void validateVariableLength(
            int field,
            String value,
            IsoFieldDefinition definition,
            int lengthDigits
    ) {

        if (value.length() > definition.getMaxLength()) {

            throw new Iso8583ParseException(
                    Iso8583ErrorCode.FIELD_LENGTH_EXCEEDED,
                    "DE " + field +
                            " melebihi maximum length " +
                            definition.getMaxLength(),
                    field,
                    null,
                    value
            );
        }

        int maxIndicator =
                lengthDigits == 2
                        ? 99
                        : 999;

        if (value.length() > maxIndicator) {

            throw new Iso8583ParseException(
                    Iso8583ErrorCode.FIELD_LENGTH_EXCEEDED,
                    "DE " + field +
                            " tidak dapat direpresentasikan oleh length indicator",
                    field,
                    null,
                    value
            );
        }
    }
}