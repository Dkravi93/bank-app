package com.deepakravi.bankapp.controller;

import com.deepakravi.bankapp.dto.AccountDtos.*;
import com.deepakravi.bankapp.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody CreateAccountRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(accountService.createAccount(principal.getUsername(), request));
    }

    @GetMapping
    public ResponseEntity<List<AccountResponse>> listMyAccounts(@AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(accountService.listMyAccounts(principal.getUsername()));
    }

    @GetMapping("/{accountNumber}")
    public ResponseEntity<AccountResponse> getAccount(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable String accountNumber) {
        return ResponseEntity.ok(accountService.getAccount(accountNumber, principal.getUsername()));
    }
}
