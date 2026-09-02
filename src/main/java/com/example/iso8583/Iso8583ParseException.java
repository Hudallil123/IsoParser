package com.example.iso8583;

public class Iso8583ParseException
        extends RuntimeException {

    private final Iso8583ErrorCode errorCode;

    private final Integer fieldNumber;

    private final Integer position;

    private final String value;

    public Iso8583ParseException(
            Iso8583ErrorCode errorCode,
            String message,
            Integer fieldNumber,
            Integer position,
            String value
    ) {

        super(message);

        this.errorCode = errorCode;
        this.fieldNumber = fieldNumber;
        this.position = position;
        this.value = value;
    }

    public Iso8583ErrorCode getErrorCode() {
        return errorCode;
    }

    public Integer getFieldNumber() {
        return fieldNumber;
    }

    public Integer getPosition() {
        return position;
    }

    public String getValue() {
        return value;
    }
}