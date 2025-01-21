package com.example.rbcs.web.controller;

import com.example.rbcs.domain.exception.AccountNotFoundException;
import com.example.rbcs.domain.repository.AccountRepository;
import com.example.rbcs.domain.service.AccountService;
import com.example.rbcs.web.controller.form.AccountForm;
import com.example.rbcs.web.controller.mapper.AccountMapper;
import com.example.rbcs.web.controller.response.AccountResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final AccountService accountService;
    private final AccountMapper accountMapper;
    private final AccountRepository accountRepository;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse createAccount(@Valid @RequestBody AccountForm accountForm) {
        return accountMapper.toResponse(accountService.createAccount(accountForm.getAccountNo()));
    }

    @GetMapping("/{id}")
    public AccountResponse getAccount(@PathVariable Long id) {
        return accountMapper.toResponse(accountRepository.findById(id).orElseThrow(AccountNotFoundException::new));
    }

    @PutMapping("/{id}/status/activate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void activateAccount(@PathVariable Long id) {
        accountService.activateAccount(id);
    }

    @PutMapping("/{id}/status/freeze")
    public void freezeAccount(@PathVariable Long id) {
        accountService.freezeAccount(id);
    }

    @PutMapping("/{id}/status/unfreeze")
    public void unfreezeAccount(@PathVariable Long id) {
        accountService.defreezeAccount(id);
    }
}