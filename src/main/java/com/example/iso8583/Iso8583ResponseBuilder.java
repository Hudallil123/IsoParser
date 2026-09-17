package com.example.iso8583;

import org.springframework.stereotype.Component;

@Component
public class Iso8583ResponseBuilder {

    public IsoMessage build(IsoMessage request, TransactionResult result) {
        IsoMessage response = new IsoMessage();

        response.setMti("0210");
        response.setField(11, request.getField(11));
        response.setField(39, result.getResponseCode());
        response.setField(41, request.getField(41));

        return response;
    }
}