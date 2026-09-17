package com.example.iso8583;

import java.util.Map;

public class Iso8583Logger {

    public void logRequest(IsoMessage message, String connectionId) {
        System.out.println(
                "ISO8583 REQUEST | connectionId=" + connectionId +
                        " | MTI=" + message.getMti() +
                        " | STAN=" + maskField(11, message.getField(11)) +
                        " | PAN=" + maskField(2, message.getField(2)) +
                        " | Terminal=" + message.getField(41)
        );
    }

    public void logResponse(IsoMessage message, String connectionId, long durationMs) {
        System.out.println(
                "ISO8583 RESPONSE | connectionId=" + connectionId +
                        " | MTI=" + message.getMti() +
                        " | STAN=" + maskField(11, message.getField(11)) +
                        " | ResponseCode=" + message.getField(39) +
                        " | DurationMs=" + durationMs
        );
    }

    private String maskField(int field, String value) {
        if (value == null) {
            return null;
        }

        if (field == 2) {
            return maskPan(value);
        }

        return value;
    }

    private String maskPan(String pan) {
        if (pan == null || pan.isBlank()) {
            return pan;
        }

        if (pan.length() <= 10) {
            return "*".repeat(pan.length());
        }

        int visiblePrefix = 6;
        int visibleSuffix = 4;
        int maskedLength = pan.length() - visiblePrefix - visibleSuffix;

        return pan.substring(0, visiblePrefix)
                + "*".repeat(maskedLength)
                + pan.substring(pan.length() - visibleSuffix);
    }

}