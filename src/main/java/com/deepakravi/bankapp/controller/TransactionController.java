package com.deepakravi.bankapp.controller;

import com.deepakravi.bankapp.dto.AccountDtos.DepositRequest;
import com.deepakravi.bankapp.dto.AccountDtos.TransferRequest;
import com.deepakravi.bankapp.model.Transaction;
import com.deepakravi.bankapp.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts/{accountNumber}")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping("/deposit")
    public ResponseEntity<Transaction> deposit(
            @PathVariable String accountNumber,
            @Valid @RequestBody DepositRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(transactionService.deposit(accountNumber, request));
    }

    @PostMapping("/transfer")
    public ResponseEntity<Transaction> transfer(
            @PathVariable String accountNumber,
            @Valid @RequestBody TransferRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(transactionService.transfer(accountNumber, request));
    }

    @GetMapping("/transactions")
    public ResponseEntity<List<Transaction>> history(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable String accountNumber) {
        return ResponseEntity.ok(transactionService.history(accountNumber, principal.getUsername()));
    }
}
