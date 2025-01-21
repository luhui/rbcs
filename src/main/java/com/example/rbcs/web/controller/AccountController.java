package com.example.rbcs.web.controller;

import com.example.rbcs.domain.exception.AccountNotFoundException;
import com.example.rbcs.domain.repository.AccountRepository;
import com.example.rbcs.domain.service.AccountService;
import com.example.rbcs.web.controller.form.AccountForm;
import com.example.rbcs.web.controller.mapper.AccountMapper;
import com.example.rbcs.web.controller.response.AccountResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * 账户管理
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final AccountService accountService;
    private final AccountMapper accountMapper;
    private final AccountRepository accountRepository;

    /**
     * 创建账户
     * @param accountForm 账户表单
     * @return AccountResponse
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse createAccount(@Valid @RequestBody AccountForm accountForm) {
        return accountMapper.toResponse(accountService.createAccount(accountForm.getAccountNo()));
    }

    /**
     * 获取账户详情
     * @param id 账户ID
     * @return AccountResponse
     */
    @GetMapping("/{id}")
    public AccountResponse getAccount(@PathVariable Long id) {
        return accountMapper.toResponse(accountRepository.findById(id).orElseThrow(AccountNotFoundException::new));
    }

    /**
     * 获取账户详情
     * @param accountNo 账户号
     * @return AccountResponse
     */
    @GetMapping()
    public AccountResponse getAccount(@Valid @NotBlank(message = "accountNo must not blank") @RequestParam("accountNo") String accountNo) {
        return accountMapper.toResponse(accountRepository.findByAccountNumber(accountNo).orElseThrow(AccountNotFoundException::new));
    }

    /**
     * 激活账户
     * @param id 账户ID
     */
    @PutMapping("/{id}/status/activate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void activateAccount(@PathVariable Long id) {
        accountService.activateAccount(id);
    }

    /**
     * 冻结账户
     * @param id 账户ID
     */
    @PutMapping("/{id}/status/freeze")
    public void freezeAccount(@PathVariable Long id) {
        accountService.freezeAccount(id);
    }

    /**
     * 解冻账户
     * @param id 账户ID
     */
    @PutMapping("/{id}/status/unfreeze")
    public void unfreezeAccount(@PathVariable Long id) {
        accountService.defreezeAccount(id);
    }
}