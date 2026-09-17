package com.example.iso8583;

public final class TransactionKey {

    private TransactionKey() {
    }

    public static String from(IsoMessage request) {
        String stan = request.getField(11);
        String terminalId = request.getField(41);

        return stan + "-" + terminalId;
    }

    public class TransactionTimeoutConfig {

        private final long timeoutMillis;
        private final int maxAttempts;

        public TransactionTimeoutConfig(
                long timeoutMillis,
                int maxAttempts
        ) {
            this.timeoutMillis = timeoutMillis;
            this.maxAttempts = maxAttempts;
        }

        public long getTimeoutMillis() {
            return timeoutMillis;
        }

        public int getMaxAttempts() {
            return maxAttempts;
        }
    }
}