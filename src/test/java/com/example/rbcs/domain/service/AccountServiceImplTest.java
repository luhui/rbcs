package com.example.rbcs.domain.service;

import com.example.rbcs.domain.entity.Account;
import com.example.rbcs.domain.exception.AccountNotFoundException;
import com.example.rbcs.domain.exception.AccountStatusInvalid;
import com.example.rbcs.domain.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceImplTest {

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private AccountServiceImpl accountService;

    private Account activeAccount;
    private Account frozenAccount;

    @BeforeEach
    void setUp() {
        activeAccount = new Account();
        activeAccount.setId(1L);
        activeAccount.setAccountNumber("123456");
        activeAccount.setStatus(Account.Status.ACTIVATED);

        frozenAccount = new Account();
        frozenAccount.setId(2L);
        frozenAccount.setAccountNumber("654321");
        frozenAccount.setStatus(Account.Status.FROZEN);
    }

    @Test
    void validateAccount_shouldPass_whenAccountExistsAndActive() {
        when(accountRepository.findByAccountNumber("123456"))
            .thenReturn(Optional.of(activeAccount));

        accountService.validateAccount("123456");

        verify(accountRepository).findByAccountNumber("123456");
    }

    @Test
    void validateAccount_shouldThrowException_whenAccountNotFound() {
        when(accountRepository.findByAccountNumber("123456"))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.validateAccount("123456"))
            .isInstanceOf(AccountNotFoundException.class);

        verify(accountRepository).findByAccountNumber("123456");
    }

    @Test
    void validateAccount_shouldThrowException_whenAccountNotActive() {
        when(accountRepository.findByAccountNumber("654321"))
            .thenReturn(Optional.of(frozenAccount));

        assertThatThrownBy(() -> accountService.validateAccount("654321"))
            .isInstanceOf(AccountStatusInvalid.class);

        verify(accountRepository).findByAccountNumber("654321");
    }

    @Test
    void getValidAccounts_shouldReturnAccounts_whenAllExistAndActive() {
        when(accountRepository.findByAccountNumberIn(Arrays.asList("123456", "654321")))
            .thenReturn(Arrays.asList(activeAccount, frozenAccount));

        assertThatThrownBy(() -> accountService.getValidAccounts(Arrays.asList("123456", "654321")))
            .isInstanceOf(AccountStatusInvalid.class);

        verify(accountRepository).findByAccountNumberIn(Arrays.asList("123456", "654321"));
    }

    @Test
    void getValidAccounts_shouldThrowException_whenSomeAccountsNotFound() {
        when(accountRepository.findByAccountNumberIn(Arrays.asList("123456", "999999")))
            .thenReturn(Collections.singletonList(activeAccount));

        assertThatThrownBy(() -> accountService.getValidAccounts(Arrays.asList("123456", "999999")))
            .isInstanceOf(AccountNotFoundException.class);

        verify(accountRepository).findByAccountNumberIn(Arrays.asList("123456", "999999"));
    }

    @Test
    void getValidAccount_shouldReturnAccount_whenExistsAndActive() {
        when(accountRepository.findByAccountNumber("123456"))
            .thenReturn(Optional.of(activeAccount));

        Account result = accountService.getValidAccount("123456");

        assertThat(result).isEqualTo(activeAccount);
        verify(accountRepository).findByAccountNumber("123456");
    }

    @Test
    void createAccount_shouldSaveNewAccount() {
        when(accountRepository.save(any(Account.class))).thenReturn(activeAccount);

        Account result = accountService.createAccount("123456");

        assertThat(result.getAccountNumber()).isEqualTo("123456");
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    void activateAccount_shouldActivateAccount() {
        Account initialAccount = new Account();
        initialAccount.setId(1L);
        initialAccount.setStatus(Account.Status.INITIAL);

        when(accountRepository.findById(1L)).thenReturn(Optional.of(initialAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(initialAccount);

        accountService.activateAccount(1L);

        assertThat(initialAccount.getStatus()).isEqualTo(Account.Status.ACTIVATED);
        verify(accountRepository).findById(1L);
        verify(accountRepository).save(initialAccount);
    }

    @Test
    void activateAccount_shouldThrowException_whenAccountNotFound() {
        when(accountRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.activateAccount(1L))
            .isInstanceOf(AccountNotFoundException.class)
            .hasMessageContaining("Account not found with id: 1");

        verify(accountRepository).findById(1L);
        verify(accountRepository, never()).save(any());
    }

    @Test
    void freezeAccount_shouldFreezeAccount() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(activeAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(activeAccount);

        accountService.freezeAccount(1L);

        assertThat(activeAccount.getStatus()).isEqualTo(Account.Status.FROZEN);
        verify(accountRepository).findById(1L);
        verify(accountRepository).save(activeAccount);
    }

    @Test
    void unfreezeAccount_shouldUnfreezeAccount() {
        when(accountRepository.findById(2L)).thenReturn(Optional.of(frozenAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(frozenAccount);

        accountService.unfreezeAccount(2L);

        assertThat(frozenAccount.getStatus()).isEqualTo(Account.Status.ACTIVATED);
        verify(accountRepository).findById(2L);
        verify(accountRepository).save(frozenAccount);
    }
}