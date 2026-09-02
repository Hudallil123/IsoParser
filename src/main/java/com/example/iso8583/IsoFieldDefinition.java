package com.example.iso8583;

public record IsoFieldDefinition(
        int number,
        FieldType type,
        int maxLength,
        FieldDataType dataType,
        boolean required
) {
}