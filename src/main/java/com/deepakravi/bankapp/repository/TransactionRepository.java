package com.deepakravi.bankapp.repository;

import com.deepakravi.bankapp.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    List<Transaction> findByFromAccountIdOrToAccountIdOrderByCreatedAtDesc(Long fromAccountId, Long toAccountId);
}
