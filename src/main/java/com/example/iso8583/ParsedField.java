package com.example.iso8583;

public record ParsedField(
        String value,
        int startPosition,
        int nextPosition
) {
}