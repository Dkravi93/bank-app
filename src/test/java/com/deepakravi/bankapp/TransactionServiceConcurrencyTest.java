package com.deepakravi.bankapp;

import com.deepakravi.bankapp.dto.AccountDtos.CreateAccountRequest;
import com.deepakravi.bankapp.dto.AccountDtos.TransferRequest;
import com.deepakravi.bankapp.model.Account;
import com.deepakravi.bankapp.model.User;
import com.deepakravi.bankapp.repository.AccountRepository;
import com.deepakravi.bankapp.repository.UserRepository;
import com.deepakravi.bankapp.service.AccountService;
import com.deepakravi.bankapp.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Proves the core claim behind this project: firing many concurrent transfers
 * out of the same account never lets the balance go negative or "lose" money,
 * because of the pessimistic row lock + fixed lock ordering in
 * TransactionService.transfer(). This is the test I'd actually walk an
 * interviewer through.
 */
@SpringBootTest
@ActiveProfiles("test")
class TransactionServiceConcurrencyTest {

    @Autowired private UserRepository userRepository;
    @Autowired private AccountRepository accountRepository;
    @Autowired private AccountService accountService;
    @Autowired private TransactionService transactionService;
    @Autowired private PasswordEncoder passwordEncoder;

    private String fromAccountNumber;
    private String toAccountNumber;

    @BeforeEach
    void setUp() {
        User owner = userRepository.save(User.builder()
                .fullName("Test Owner")
                .email("owner-" + UUID.randomUUID() + "@test.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .role(User.Role.CUSTOMER)
                .build());

        Account from = Account.builder()
                .owner(owner)
                .balance(new BigDecimal("1000.00"))
                .accountType(Account.AccountType.SAVINGS)
                .status(Account.AccountStatus.ACTIVE)
                .build();
        Account to = Account.builder()
                .owner(owner)
                .balance(BigDecimal.ZERO)
                .accountType(Account.AccountType.SAVINGS)
                .status(Account.AccountStatus.ACTIVE)
                .build();

        fromAccountNumber = accountRepository.save(from).getAccountNumber();
        toAccountNumber = accountRepository.save(to).getAccountNumber();
    }

    @Test
    void concurrentTransfers_neverOverdraftTheSourceAccount() throws InterruptedException {
        int threadCount = 20;
        BigDecimal transferAmount = new BigDecimal("100.00"); // 20 x 100 = 2000, but balance is only 1000

        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            String idempotencyKey = UUID.randomUUID().toString();
            pool.submit(() -> {
                try {
                    startGate.await();
                    transactionService.transfer(fromAccountNumber,
                            new TransferRequest(toAccountNumber, transferAmount, idempotencyKey, "concurrency test"));
                } catch (Exception ignored) {
                    // Expected: some of these will fail with InsufficientFundsException
                    // once the balance runs out. That's the correct behavior.
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startGate.countDown(); // release all threads at once
        doneLatch.await(30, TimeUnit.SECONDS);
        pool.shutdown();

        Account from = accountRepository.findByAccountNumber(fromAccountNumber).orElseThrow();
        Account to = accountRepository.findByAccountNumber(toAccountNumber).orElseThrow();

        // Exactly 10 of the 20 transfers should have succeeded (10 x 100 = 1000),
        // leaving the source account at exactly zero - never negative.
        assertEquals(0, new BigDecimal("0.00").compareTo(from.getBalance()),
                "Source account must never go negative under concurrent transfers");
        assertEquals(0, new BigDecimal("1000.00").compareTo(to.getBalance()),
                "Destination account must reflect exactly the successful transfers, no double-credits");
    }
}
