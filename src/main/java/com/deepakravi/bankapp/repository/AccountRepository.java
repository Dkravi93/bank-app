package com.deepakravi.bankapp.repository;

import com.deepakravi.bankapp.model.Account;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByAccountNumber(String accountNumber);

    List<Account> findByOwnerId(Long ownerId);

    /**
     * Row-level pessimistic lock used inside the transfer transaction so two
     * concurrent transfers touching the same account can't both read a stale
     * balance and overwrite each other. Combined with @Version for defense in
     * depth: this stops the race at read time, @Version catches anything
     * that slips through at write time.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.accountNumber = :accountNumber")
    Optional<Account> findByAccountNumberForUpdate(@Param("accountNumber") String accountNumber);
}
