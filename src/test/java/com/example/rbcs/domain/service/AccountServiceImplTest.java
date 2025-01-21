package com.example.rbcs.domain.service;

import com.example.rbcs.domain.entity.Account;
import com.example.rbcs.domain.entity.Transaction;
import com.example.rbcs.domain.exception.AccountNotFoundException;
import com.example.rbcs.domain.exception.AccountStatusInvalid;
import com.example.rbcs.domain.repository.AccountRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

@SpringBootTest
@ActiveProfiles("test")
class AccountServiceImplTest {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AccountServiceImpl accountService;

    private Account activeAccount;
    private Account frozenAccount;

    @AfterEach
    public void tearDown() {
        accountRepository.deleteAllInBatch();
    }

    @BeforeEach
    void setUp() {
        activeAccount = new Account();
        activeAccount.setAccountNumber("123456");
        activeAccount.setStatus(Account.Status.ACTIVATED);
        accountRepository.save(activeAccount);

        frozenAccount = new Account();
        frozenAccount.setAccountNumber("654321");
        frozenAccount.setStatus(Account.Status.FROZEN);
        accountRepository.save(frozenAccount);
    }

    @Test
    void validateAccount_shouldPass_whenAccountExistsAndActive() {
        accountService.validateAccount(activeAccount.getAccountNumber());
    }

    @Test
    void validateAccount_shouldThrowException_whenAccountNotFound() {
        assertThatThrownBy(() -> accountService.validateAccount("98173821789"))
            .isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    void validateAccount_shouldThrowException_whenAccountNotActive() {
        assertThatThrownBy(() -> accountService.validateAccount(frozenAccount.getAccountNumber()))
            .isInstanceOf(AccountStatusInvalid.class);
    }

    @Test
    void getValidAccounts_shouldThrowException_whenOneInvalid() {
        assertThatThrownBy(() -> accountService.getValidAccounts(Arrays.asList(activeAccount.getAccountNumber(), frozenAccount.getAccountNumber())))
            .isInstanceOf(AccountStatusInvalid.class);
    }

    @Test
    void getValidAccounts_shouldThrowException_whenSomeAccountsNotFound() {
        assertThatThrownBy(() -> accountService.getValidAccounts(Arrays.asList(activeAccount.getAccountNumber(), "999999")))
            .isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    void getValidAccount_shouldReturnAccount_whenExistsAndActive() {
        Account result = accountService.getValidAccount(activeAccount.getAccountNumber());

        assertThat(result.getId()).isEqualTo(activeAccount.getId());
    }

    @Test
    void createAccount_shouldSaveNewAccount() {
        Account result = accountService.createAccount("99987123");

        assertThat(result.getAccountNumber()).isEqualTo("99987123");
    }

    @Test
    void activateAccount_shouldActivateAccount() {
        var initialAccount = new Account();
        initialAccount.setAccountNumber("123456789");
        accountRepository.save(initialAccount);

        accountService.activateAccount(initialAccount.getId());

        final var acc = accountRepository.findById(initialAccount.getId());
        assertThat(acc.isPresent()).isTrue();
        assertThat(acc.get().getStatus()).isEqualTo(Account.Status.ACTIVATED);
    }

    @Test
    void activateAccount_shouldThrowException_whenAccountNotFound() {
        assertThatThrownBy(() -> accountService.activateAccount(9999L))
            .isInstanceOf(AccountNotFoundException.class)
            .hasMessageContaining("Account not found with id: 9999");
    }

    @Test
    void freezeAccount_shouldFreezeAccount() {
        accountService.freezeAccount(activeAccount.getId());

        final var acc = accountRepository.findById(activeAccount.getId());
        assertThat(acc.isPresent()).isTrue();
        assertThat(acc.get().getStatus()).isEqualTo(Account.Status.FROZEN);
    }

    @Test
    void unfreezeAccount_shouldUnfreezeAccount() {
        accountService.defreezeAccount(frozenAccount.getId());

        final var acc = accountRepository.findById(frozenAccount.getId());
        assertThat(acc.isPresent()).isTrue();
        assertThat(acc.get().getStatus()).isEqualTo(Account.Status.ACTIVATED);
    }

    /**
     * 并发测试冻结账户：
     * activate是幂等的，因此并发执行若干次，在中间插入一个冻结操作，最终账户应该被冻结。
     * 如果没有加锁，有概率会覆盖掉冻结的结果
     * 因为从冻结->激活需要解冻的操作，通过激活接口状态不可逆
     */
    @Test
    @RepeatedTest(5)
    void concurrentTest_freezeAccount_shouldBeFreezeAccount() throws InterruptedException {
        // Arrange

        var initialAccount = new Account();
        initialAccount.setAccountNumber("123456789");
        accountRepository.save(initialAccount);

        int threadCount = 100;
        CountDownLatch latch = new CountDownLatch(threadCount);

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

        // Act
        for (int i = 0; i < threadCount-1; i++) {
            executorService.submit(() -> {
                try {
                    accountService.activateAccount(initialAccount.getId());
                } catch (Exception e) {
                    System.err.println("Error: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }
        executorService.submit(() -> {
            try {
                accountService.freezeAccount(initialAccount.getId());
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            } finally {
                latch.countDown();
            }
        });

        latch.await(); // 等待所有线程执行完毕
        executorService.shutdown();

        // Assert
        final var acc = accountRepository.findById(initialAccount.getId());
        assertThat(acc.isPresent()).isTrue();
        assertThat(acc.get().getStatus()).isEqualTo(Account.Status.FROZEN);
    }
}