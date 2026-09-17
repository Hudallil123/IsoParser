package com.example.iso8583;

public class TransactionResult {

    private final String responseCode;

    public TransactionResult(String responseCode) {
        this.responseCode = responseCode;
    }

    public String getResponseCode() {
        return responseCode;
    }

    public boolean isApproved() {
        return Iso8583ResponseCode.APPROVED.equals(responseCode);
    }
}