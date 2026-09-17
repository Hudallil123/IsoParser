package com.example.iso8583;

public class IsoFieldDefinition {

    private final int fieldNumber;
    private final FieldType fieldType;
    private final int maxLength;
    private final FieldDataType dataType;
    private final FieldEncoding encoding;
    private final FieldLengthUnit lengthUnit;
    private final boolean required;

    public IsoFieldDefinition(
            int fieldNumber,
            FieldType fieldType,
            int maxLength,
            FieldDataType dataType,
            boolean required
    ) {
        this(
                fieldNumber,
                fieldType,
                maxLength,
                dataType,
                FieldEncoding.ASCII,
                FieldLengthUnit.CHARACTERS,
                required
        );
    }

    public IsoFieldDefinition(
            int fieldNumber,
            FieldType fieldType,
            int maxLength,
            FieldDataType dataType,
            FieldEncoding encoding,
            boolean required
    ) {
        this(
                fieldNumber,
                fieldType,
                maxLength,
                dataType,
                encoding,
                defaultLengthUnit(encoding),
                required
        );
    }

    public IsoFieldDefinition(
            int fieldNumber,
            FieldType fieldType,
            int maxLength,
            FieldDataType dataType,
            FieldEncoding encoding,
            FieldLengthUnit lengthUnit,
            boolean required
    ) {
        this.fieldNumber = fieldNumber;
        this.fieldType = fieldType;
        this.maxLength = maxLength;
        this.dataType = dataType;
        this.encoding = encoding;
        this.lengthUnit = lengthUnit;
        this.required = required;
    }

    private static FieldLengthUnit defaultLengthUnit(
            FieldEncoding encoding
    ) {
        return switch (encoding) {
            case ASCII -> FieldLengthUnit.CHARACTERS;
            case BCD -> FieldLengthUnit.DIGITS;
            case BINARY -> FieldLengthUnit.BYTES;
        };
    }

    public int getFieldNumber() {
        return fieldNumber;
    }

    public FieldType getFieldType() {
        return fieldType;
    }

    public int getMaxLength() {
        return maxLength;
    }

    public FieldDataType getDataType() {
        return dataType;
    }

    public FieldEncoding getEncoding() {
        return encoding;
    }

    public FieldLengthUnit getLengthUnit() {
        return lengthUnit;
    }

    public boolean isRequired() {
        return required;
    }
}