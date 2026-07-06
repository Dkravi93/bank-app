package com.deepakravi.bankapp.service;

import com.deepakravi.bankapp.dto.AccountDtos.*;
import com.deepakravi.bankapp.exception.ApiExceptions;
import com.deepakravi.bankapp.model.Account;
import com.deepakravi.bankapp.model.User;
import com.deepakravi.bankapp.repository.AccountRepository;
import com.deepakravi.bankapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    @Transactional
    public AccountResponse createAccount(String ownerEmail, CreateAccountRequest request) {
        User owner = userRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new ApiExceptions.ResourceNotFoundException("User not found"));

        Account account = Account.builder()
                .owner(owner)
                .balance(BigDecimal.ZERO)
                .accountType(request.accountType() != null ? request.accountType() : Account.AccountType.SAVINGS)
                .status(Account.AccountStatus.ACTIVE)
                .build();

        accountRepository.save(account);
        return AccountResponse.from(account);
    }

    public List<AccountResponse> listMyAccounts(String ownerEmail) {
        User owner = userRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new ApiExceptions.ResourceNotFoundException("User not found"));

        return accountRepository.findByOwnerId(owner.getId()).stream()
                .map(AccountResponse::from)
                .toList();
    }

    public AccountResponse getAccount(String accountNumber, String requesterEmail) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ApiExceptions.ResourceNotFoundException("Account not found: " + accountNumber));

        if (!account.getOwner().getEmail().equals(requesterEmail)) {
            throw new ApiExceptions.ResourceNotFoundException("Account not found: " + accountNumber);
        }

        return AccountResponse.from(account);
    }
}
