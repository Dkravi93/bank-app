package com.deepakravi.bankapp.dto;

import com.deepakravi.bankapp.model.Account;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;

public class AccountDtos {

    public record CreateAccountRequest(
            Account.AccountType accountType
    ) {}

    public record AccountResponse(
            Long id,
            String accountNumber,
            BigDecimal balance,
            Account.AccountType accountType,
            Account.AccountStatus status,
            Instant createdAt
    ) {
        public static AccountResponse from(Account a) {
            return new AccountResponse(a.getId(), a.getAccountNumber(), a.getBalance(),
                    a.getAccountType(), a.getStatus(), a.getCreatedAt());
        }
    }

    public record DepositRequest(
            @NotNull @DecimalMin(value = "0.01", message = "Amount must be positive") BigDecimal amount,
            @NotBlank String idempotencyKey
    ) {}

    public record TransferRequest(
            @NotBlank String toAccountNumber,
            @NotNull @DecimalMin(value = "0.01", message = "Amount must be positive") BigDecimal amount,
            @NotBlank String idempotencyKey,
            String remarks
    ) {}
}
