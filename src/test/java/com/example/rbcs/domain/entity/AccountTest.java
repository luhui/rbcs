package com.example.rbcs.domain.entity;

import com.example.rbcs.domain.exception.AccountStatusInvalid;
import com.example.rbcs.domain.exception.AmountInvalidException;
import com.example.rbcs.domain.exception.InsufficientBalanceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class AccountTest {

    private Account account;

    @BeforeEach
    void setUp() {
        account = new Account();
        account.setAccountNumber("123456");
        account.setBalance(1000L);
        account.setStatus(Account.Status.ACTIVATED);
    }

    @Test
    void deposit_shouldIncreaseBalance_whenAccountIsActive() {
        account.deposit(500L);
        assertThat(account.getBalance()).isEqualTo(1500L);
    }

    @Test
    void deposit_shouldThrowException_whenAccountIsNotActive() {
        account.setStatus(Account.Status.FROZEN);
        assertThatThrownBy(() -> account.deposit(500L))
            .isInstanceOf(AccountStatusInvalid.class);
    }

    @Test
    void withdraw_shouldDecreaseBalance_whenAccountIsActiveAndHasSufficientBalance() {
        account.withdraw(400L);
        assertThat(account.getBalance()).isEqualTo(600L);
    }

    @Test
    void withdraw_shouldThrowException_whenAccountIsNotActive() {
        account.setStatus(Account.Status.FROZEN);
        assertThatThrownBy(() -> account.withdraw(500L))
            .isInstanceOf(AccountStatusInvalid.class);
    }

    @Test
    void withdraw_shouldThrowException_whenInsufficientBalance() {
        assertThatThrownBy(() -> account.withdraw(1500L))
            .isInstanceOf(InsufficientBalanceException.class);
    }

    @Test
    void freeze_shouldChangeStatusToFrozen_whenAccountIsActive() {
        account.freeze();
        assertThat(account.getStatus()).isEqualTo(Account.Status.FROZEN);
    }

    @Test
    void freeze_shouldNotChangeStatus_whenAccountIsAlreadyFrozen() {
        account.setStatus(Account.Status.FROZEN);
        account.freeze();
        assertThat(account.getStatus()).isEqualTo(Account.Status.FROZEN);
    }

    @Test
    void deFreeze_shouldChangeStatusToActivated_whenAccountIsFrozen() {
        account.setStatus(Account.Status.FROZEN);
        account.deFreeze();
        assertThat(account.getStatus()).isEqualTo(Account.Status.ACTIVATED);
    }

    @Test
    void deFreeze_shouldDoNothing_whenAccountIsActivated() {
        assertThatCode(() -> account.deFreeze()).doesNotThrowAnyException();
        assertThat(account.getStatus()).isEqualTo(Account.Status.ACTIVATED);
    }

    @Test
    void activate_shouldChangeStatusToActivated_whenAccountIsInitial() {
        account.setStatus(Account.Status.INITIAL);
        account.activate();
        assertThat(account.getStatus()).isEqualTo(Account.Status.ACTIVATED);
    }

    @Test
    void activate_shouldThrowException_whenAccountIsNotInitial() {
        Executable executable = () -> account.activate();
        assertThatThrownBy(executable::execute)
            .isInstanceOf(AccountStatusInvalid.class);
    }

    @Test
    void canWithdraw_shouldReturnTrue_whenBalanceIsSufficient() {
        assertThat(account.canWithdraw(500L)).isTrue();
    }

    @Test
    void canWithdraw_shouldReturnFalse_whenBalanceIsInsufficient() {
        assertThat(account.canWithdraw(1500L)).isFalse();
    }

    @Test
    void canWithdraw_shouldThrowException_whenAmountIsInvalid() {
        Executable executable = () -> account.canWithdraw(-100L);
        assertThatThrownBy(executable::execute)
            .isInstanceOf(AmountInvalidException.class);
    }

    @Test
    void deposit_shouldThrowException_whenAmountIsZero() {
        Executable executable = () -> account.deposit(0L);
        assertThatThrownBy(executable::execute)
            .isInstanceOf(AmountInvalidException.class);
    }

    @Test
    void deposit_shouldThrowException_whenAmountIsNegative() {
        Executable executable = () -> account.deposit(-100L);
        assertThatThrownBy(executable::execute)
            .isInstanceOf(AmountInvalidException.class);
    }

    @Test
    void deposit_shouldHandleMaxLongValue() {
        account.setBalance(Long.MAX_VALUE - 500L);
        account.deposit(500L);
        assertThat(account.getBalance()).isEqualTo(Long.MAX_VALUE);
    }

    @Test
    void withdraw_shouldHandleMinBalance() {
        account.setBalance(500L);
        account.withdraw(500L);
        assertThat(account.getBalance()).isEqualTo(0L);
    }
}