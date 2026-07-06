package com.deepakravi.bankapp.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "account")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Human-facing account number, separate from internal PK
    @Column(nullable = false, unique = true, updatable = false)
    private String accountNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal balance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AccountType accountType = AccountType.SAVINGS;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AccountStatus status = AccountStatus.ACTIVE;

    // Optimistic locking: prevents two concurrent transfers from
    // corrupting the balance via lost-update. JPA checks this
    // on every UPDATE and throws OptimisticLockException on conflict.
    @Version
    private Long version;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
        if (this.accountNumber == null) {
            this.accountNumber = "AC" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        }
    }

    public enum AccountType {
        SAVINGS, CURRENT
    }

    public enum AccountStatus {
        ACTIVE, FROZEN, CLOSED
    }
}
