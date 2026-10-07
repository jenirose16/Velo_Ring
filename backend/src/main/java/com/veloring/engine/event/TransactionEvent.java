package com.veloring.engine.event;

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
    
    // Temporal fields specifically separated as per architecture constraints
    private Instant eventTime;     // When the transaction occurred on the payment rail
    private Instant processedAt;   // When the engine ingested it
    private Instant detectedAt;    // When a ring was detected (to be used later)
    
    private String channel;
}
