package com.example.iso8583;

public class DownstreamResult {

    private final boolean success;
    private final String responseCode;

    public DownstreamResult(
            boolean success,
            String responseCode
    ) {
        this.success = success;
        this.responseCode = responseCode;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getResponseCode() {
        return responseCode;
    }
}