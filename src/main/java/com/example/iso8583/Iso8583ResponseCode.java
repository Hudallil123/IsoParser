package com.example.iso8583;

public final class Iso8583ResponseCode {

    public static final String APPROVED = "00";
    public static final String DO_NOT_HONOR = "05";
    public static final String INVALID_TRANSACTION = "12";
    public static final String INVALID_AMOUNT = "13";
    public static final String INVALID_ACCOUNT = "14";
    public static final String INSUFFICIENT_FUNDS = "51";
    public static final String SYSTEM_MALFUNCTION = "96";

    private Iso8583ResponseCode() {
    }
}