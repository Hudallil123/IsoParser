package com.example.iso8583;

public interface DownstreamClient {
    DownstreamResult process(IsoMessage request);
}