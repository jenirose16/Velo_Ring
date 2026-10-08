package com.veloring.engine.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionEvent {
    @NotBlank
    private String transactionId;

    @NotBlank
    private String senderAccountId;

    @NotBlank
    private String receiverAccountId;

    @NotNull
    @Positive
    private BigDecimal amount;

    @NotBlank
    private String currency;
    
    // Temporal fields specifically separated as per architecture constraints
    @NotNull
    private Instant eventTime;     // When the transaction occurred on the payment rail

    private Instant processedAt;   // When the engine ingested it
    private Instant detectedAt;    // When a ring was detected (to be used later)

    @NotBlank
    private String channel;
}
