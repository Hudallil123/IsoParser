package com.example.iso8583;

import java.util.Map;

public class Iso8583Builder {

    private final Map<Integer, IsoFieldDefinition> definitions;

    public Iso8583Builder(
            Map<Integer, IsoFieldDefinition> definitions
    ) {
        this.definitions = definitions;
    }

    public String build(IsoMessage message) {

        validateMessage(message);

        String mti = message.getMti();

        String primaryBitmap = buildPrimaryBitmap(message);

        StringBuilder result = new StringBuilder();
        result.append(mti);
        result.append(primaryBitmap);
        appendFields(
                result,
                message,
                2,
                64
        );

        return result.toString();
    }

    private void validateMessage(IsoMessage message) {

        if (message == null) {
            throw new Iso8583ParseException(
                    Iso8583ErrorCode.INVALID_MESSAGE,
                    "IsoMessage tidak boleh null",
                    null,
                    null,
                    null
            );
        }

        if (message.getMti() == null || message.getMti().isBlank()) {
            throw new Iso8583ParseException(
                    Iso8583ErrorCode.INVALID_MTI,
                    "MTI tidak boleh kosong",
                    null,
                    null,
                    message.getMti()
            );
        }

        if (message.getMti().length() != 4) {
            throw new Iso8583ParseException(
                    Iso8583ErrorCode.INVALID_MTI,
                    "MTI harus memiliki 4 karakter",
                    null,
                    null,
                    message.getMti()
            );
        }
    }

    private String buildPrimaryBitmap(IsoMessage message) {

        StringBuilder binary = new StringBuilder("0".repeat(64));
        for (int field : message.getFields().keySet()) {
            if (field < 2 || field > 64) {
                continue;
            }

            binary.setCharAt(field - 1, '1');
        }

        return binaryToHex(binary.toString());
    }

    private void appendFields(
            StringBuilder result,
            IsoMessage message,
            int startField,
            int endField
    ) {

        for (int field = startField;
             field <= endField;
             field++) {

            String value = message.getField(field);

            if (value == null) {
                continue;
            }

            IsoFieldDefinition definition = definitions.get(field);

            if (definition == null) {
                throw new Iso8583ParseException(
                        Iso8583ErrorCode.FIELD_DEFINITION_NOT_FOUND,
                        "Definition DE " +
                                field +
                                " tidak ditemukan",
                        field,
                        result.length(),
                        value
                );
            }

            FieldValidator.validate(
                    field,
                    value,
                    definition.dataType(),
                    result.length()
            );

            appendField(
                    result,
                    field,
                    value,
                    definition
            );
        }
    }

    private void appendField(
            StringBuilder result,
            int field,
            String value,
            IsoFieldDefinition definition
    ) {

        switch (definition.type()) {

            case FIXED -> appendFixedField(
                    result,
                    field,
                    value,
                    definition
            );

            case LLVAR -> appendLlvarField(
                    result,
                    field,
                    value,
                    definition
            );

            case LLLVAR -> appendLllvarField(
                    result,
                    field,
                    value,
                    definition
            );
        }
    }

    private void appendFixedField(
            StringBuilder result,
            int field,
            String value,
            IsoFieldDefinition definition
    ) {

        if (value.length() != definition.maxLength()) {

            throw new Iso8583ParseException(
                    Iso8583ErrorCode.FIELD_LENGTH_INVALID,
                    "DE " + field +
                            " harus memiliki panjang " +
                            definition.maxLength() +
                            ". Actual=" +
                            value.length(),
                    field,
                    result.length(),
                    value
            );
        }

        result.append(value);
    }

    private void appendLlvarField(
            StringBuilder result,
            int field,
            String value,
            IsoFieldDefinition definition
    ) {

        if (value.length() > definition.maxLength()) {

            throw new Iso8583ParseException(
                    Iso8583ErrorCode.FIELD_LENGTH_EXCEEDED,
                    "DE " + field +
                            " melebihi maximum length. " +
                            "Actual=" +
                            value.length() +
                            ", Max=" +
                            definition.maxLength(),
                    field,
                    result.length(),
                    value
            );
        }

        result.append(
                String.format(
                        "%02d",
                        value.length()
                )
        );

        result.append(value);
    }

    private void appendLllvarField(
            StringBuilder result,
            int field,
            String value,
            IsoFieldDefinition definition
    ) {

        if (value.length() > definition.maxLength()) {

            throw new Iso8583ParseException(
                    Iso8583ErrorCode.FIELD_LENGTH_EXCEEDED,
                    "DE " + field +
                            " melebihi maximum length. " +
                            "Actual=" +
                            value.length() +
                            ", Max=" +
                            definition.maxLength(),
                    field,
                    result.length(),
                    value
            );
        }

        result.append(
                String.format(
                        "%03d",
                        value.length()
                )
        );

        result.append(value);
    }

    private String binaryToHex( String binary ) {

        StringBuilder hex = new StringBuilder();

        for (int i = 0;
             i < binary.length();
             i += 4) {

            String group =
                    binary.substring(
                            i,
                            i + 4
                    );

            int value =
                    Integer.parseInt(
                            group,
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
}