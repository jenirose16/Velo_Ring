package com.veloring.generator.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionEvent {
    private String transactionId;
    private String senderAccountId;
    private String receiverAccountId;
    private BigDecimal amount;
    private String currency;
    private Instant eventTime;
    private Instant processedAt;
    private Instant detectedAt;
    private String channel;
}
