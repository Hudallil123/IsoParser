package com.example.iso8583;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class IsoMessage {

    private String mti;

    private String bitmap;

    private final Map<Integer, String> fields = new LinkedHashMap<>();

    public String getMti() {
        return mti;
    }

    public void setMti(String mti) {
        this.mti = mti;
    }

    public String getBitmap() {
        return bitmap;
    }

    public void setBitmap(String bitmap) {
        this.bitmap = bitmap;
    }

    public Map<Integer, String> getFields() {
        return fields;
    }

    public void setField(int fieldNumber, String value) {
        fields.put(fieldNumber, value);
    }

    public String getField(int fieldNumber) {
        return fields.get(fieldNumber);
    }

    @Override
    public String toString() {
        return "IsoMessage{" +
                "mti='" + mti + '\'' +
                ", bitmap='" + bitmap + '\'' +
                ", fields=" + fields +
                '}';
    }
}