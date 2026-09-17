package com.example.iso8583;

import org.springframework.stereotype.Service;

@Service
public class TransactionServiceImpl implements TransactionService {

    private final DownstreamClient downstreamClient;

    public TransactionServiceImpl(DownstreamClient downstreamClient) {
        this.downstreamClient = downstreamClient;
    }

    @Override
    public TransactionResult process(IsoMessage request) {

        DownstreamResult result = downstreamClient.process(request);

        if (result.isSuccess()) {
            return new TransactionResult(
                    Iso8583ResponseCode.APPROVED
            );
        }

        return new TransactionResult(
                result.getResponseCode()
        );
    }
}