package com.deepakravi.bankapp.service;

import com.deepakravi.bankapp.dto.AccountDtos.DepositRequest;
import com.deepakravi.bankapp.dto.AccountDtos.TransferRequest;
import com.deepakravi.bankapp.exception.ApiExceptions;
import com.deepakravi.bankapp.model.Account;
import com.deepakravi.bankapp.model.Transaction;
import com.deepakravi.bankapp.model.Transaction.TransactionType;
import com.deepakravi.bankapp.repository.AccountRepository;
import com.deepakravi.bankapp.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    /**
     * Deposits are single-account writes, so a straightforward @Transactional
     * with row lock is enough - no ordering concerns like a transfer has.
     */
    @Transactional
    public Transaction deposit(String accountNumber, DepositRequest request) {
        checkIdempotency(request.idempotencyKey());

        Account account = accountRepository.findByAccountNumberForUpdate(accountNumber)
                .orElseThrow(() -> new ApiExceptions.ResourceNotFoundException("Account not found: " + accountNumber));

        assertActive(account);

        account.setBalance(account.getBalance().add(request.amount()));
        accountRepository.save(account);

        Transaction tx = Transaction.builder()
                .idempotencyKey(request.idempotencyKey())
                .toAccount(account)
                .amount(request.amount())
                .type(TransactionType.DEPOSIT)
                .build();

        return transactionRepository.save(tx);
    }

    /**
     * Transfer between two accounts. Two things make this safe under concurrent
     * requests, which is the part of this project actually worth discussing
     * in an interview:
     *
     * 1. Lock ordering: we always lock accounts in a fixed order (lower account
     *    number first) regardless of which is "from" and which is "to". Locking
     *    in a fixed global order is the standard way to prevent deadlocks when
     *    two transfers run in opposite directions between the same two accounts
     *    at the same time (A->B and B->A concurrently).
     * 2. Idempotency key: the client supplies a unique key per transfer attempt.
     *    If the same request is retried (e.g. client-side timeout + retry, or a
     *    double-tap on a submit button), we detect the existing transaction and
     *    return it rather than moving money twice.
     */
    @Transactional
    public Transaction transfer(String fromAccountNumber, TransferRequest request) {
        checkIdempotency(request.idempotencyKey());

        if (fromAccountNumber.equals(request.toAccountNumber())) {
            throw new IllegalArgumentException("Cannot transfer to the same account");
        }

        String first = fromAccountNumber.compareTo(request.toAccountNumber()) < 0
                ? fromAccountNumber : request.toAccountNumber();
        String second = fromAccountNumber.compareTo(request.toAccountNumber()) < 0
                ? request.toAccountNumber() : fromAccountNumber;

        Account firstLocked = accountRepository.findByAccountNumberForUpdate(first)
                .orElseThrow(() -> new ApiExceptions.ResourceNotFoundException("Account not found: " + first));
        Account secondLocked = accountRepository.findByAccountNumberForUpdate(second)
                .orElseThrow(() -> new ApiExceptions.ResourceNotFoundException("Account not found: " + second));

        Account from = firstLocked.getAccountNumber().equals(fromAccountNumber) ? firstLocked : secondLocked;
        Account to = firstLocked.getAccountNumber().equals(request.toAccountNumber()) ? firstLocked : secondLocked;

        assertActive(from);
        assertActive(to);

        if (from.getBalance().compareTo(request.amount()) < 0) {
            throw new ApiExceptions.InsufficientFundsException(
                    "Insufficient balance in account " + fromAccountNumber);
        }

        from.setBalance(from.getBalance().subtract(request.amount()));
        to.setBalance(to.getBalance().add(request.amount()));

        accountRepository.save(from);
        accountRepository.save(to);

        Transaction tx = Transaction.builder()
                .idempotencyKey(request.idempotencyKey())
                .fromAccount(from)
                .toAccount(to)
                .amount(request.amount())
                .type(TransactionType.TRANSFER)
                .remarks(request.remarks())
                .build();

        return transactionRepository.save(tx);
    }

    public List<Transaction> history(String accountNumber, String requesterEmail) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ApiExceptions.ResourceNotFoundException("Account not found: " + accountNumber));

        if (!account.getOwner().getEmail().equals(requesterEmail)) {
            throw new ApiExceptions.ResourceNotFoundException("Account not found: " + accountNumber);
        }

        return transactionRepository.findByFromAccountIdOrToAccountIdOrderByCreatedAtDesc(account.getId(), account.getId());
    }

    private void checkIdempotency(String idempotencyKey) {
        transactionRepository.findByIdempotencyKey(idempotencyKey).ifPresent(existing -> {
            throw new ApiExceptions.DuplicateRequestException(
                    "Request with idempotency key " + idempotencyKey + " was already processed");
        });
    }

    private void assertActive(Account account) {
        if (account.getStatus() != Account.AccountStatus.ACTIVE) {
            throw new ApiExceptions.AccountNotActiveException(
                    "Account " + account.getAccountNumber() + " is not active (status=" + account.getStatus() + ")");
        }
    }
}
