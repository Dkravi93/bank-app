package com.deepakravi.bankapp.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Immutable ledger entry. We never update or delete a Transaction row —
 * corrections happen by writing a new offsetting transaction. This keeps
 * the ledger auditable, which is the standard pattern for financial systems.
 */
@Entity
@Table(name = "transaction_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Client-supplied key so retried requests don't double-process a transfer.
    @Column(nullable = false, unique = true)
    private String idempotencyKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_account_id")
    private Account fromAccount; // null for deposits

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_account_id")
    private Account toAccount; // null for withdrawals

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TransactionStatus status = TransactionStatus.COMPLETED;

    private String remarks;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public enum TransactionType {
        DEPOSIT, WITHDRAWAL, TRANSFER
    }

    public enum TransactionStatus {
        COMPLETED, FAILED, REVERSED
    }
}
