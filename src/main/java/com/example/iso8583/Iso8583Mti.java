package com.example.iso8583;

public final class Iso8583Mti {

    private final int version;
    private final int messageClass;
    private final int messageFunction;
    private final int messageOrigin;

    private Iso8583Mti(
            int version,
            int messageClass,
            int messageFunction,
            int messageOrigin
    ) {
        this.version = version;
        this.messageClass = messageClass;
        this.messageFunction = messageFunction;
        this.messageOrigin = messageOrigin;
    }

    public static Iso8583Mti parse(String mti) {
        if (mti == null) {
            throw new Iso8583ParseException(
                    Iso8583ErrorCode.INVALID_MTI,
                    "MTI tidak boleh null",
                    null,
                    null,
                    null
            );
        }

        if (mti.length() != 4) {
            throw new Iso8583ParseException(
                    Iso8583ErrorCode.INVALID_MTI,
                    "MTI harus memiliki 4 digit. Actual=" + mti.length(),
                    null,
                    null,
                    mti
            );
        }

        if (!mti.matches("\\d{4}")) {
            throw new Iso8583ParseException(
                    Iso8583ErrorCode.INVALID_MTI,
                    "MTI hanya boleh mengandung angka",
                    null,
                    null,
                    mti
            );
        }

        return new Iso8583Mti(
                Character.digit(mti.charAt(0), 10),
                Character.digit(mti.charAt(1), 10),
                Character.digit(mti.charAt(2), 10),
                Character.digit(mti.charAt(3), 10)
        );
    }

    public int getVersion() {
        return version;
    }

    public int getMessageClass() {
        return messageClass;
    }

    public int getMessageFunction() {
        return messageFunction;
    }

    public int getMessageOrigin() {
        return messageOrigin;
    }

    public boolean isRequest() {
        return messageFunction == 0;
    }

    public boolean isResponse() {
        return messageFunction == 1;
    }

    @Override
    public String toString() {
        return String.valueOf(version)
                + messageClass
                + messageFunction
                + messageOrigin;
    }
}