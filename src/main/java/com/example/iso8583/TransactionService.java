package com.example.iso8583;

public interface TransactionService {
    TransactionResult process(IsoMessage request);
}