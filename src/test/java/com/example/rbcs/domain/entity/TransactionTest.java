package com.example.rbcs.domain.entity;

import com.example.rbcs.domain.exception.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class TransactionTest {

    private Transaction transaction;
    private Account sourceAccount;
    private Account targetAccount;

    @BeforeEach
    void setUp() {
        transaction = new Transaction();
        transaction.setId(1L);
        transaction.setAmount(1000L);
        transaction.setType(Transaction.Type.TRANSFER);
        
        sourceAccount = Mockito.mock(Account.class);
        targetAccount = Mockito.mock(Account.class);
        
        transaction.setSourceAccount(sourceAccount);
        transaction.setTargetAccount(targetAccount);
    }

    @Test
    void testPrePersist_ZeroAmount() {
        transaction.setAmount(0L);
        assertThatThrownBy(() -> transaction.prePersist())
            .isInstanceOf(AmountInvalidException.class)
            .hasMessage("Amount must be positive");
    }

    @Test
    void testPrePersist_NegativeAmount() {
        transaction.setAmount(-1000L);
        assertThatThrownBy(() -> transaction.prePersist())
            .isInstanceOf(AmountInvalidException.class)
            .hasMessage("Amount must be positive");
    }

    @Test
    void testExecute_InsufficientBalance() {
        transaction.setType(Transaction.Type.WITHDRAWAL);
        doThrow(new InsufficientBalanceException("Insufficient balance"))
            .when(sourceAccount).withdraw(1000L);
        
        assertThatThrownBy(() -> transaction.execute())
            .isInstanceOf(InsufficientBalanceException.class)
            .hasMessage("Insufficient balance");
        
        assertThat(transaction.getStatus()).isEqualTo(Transaction.Status.PENDING);
    }

    @Test
    void testCancel_AlreadyCompleted() {
        transaction.execute();
        assertThatThrownBy(() -> transaction.cancel())
            .isInstanceOf(TransactionStatusInvalid.class)
            .hasMessage("Transaction is not in pending status");
    }

    @Test
    void testNormalTransfer() {
        // Given
        transaction.setType(Transaction.Type.TRANSFER);
        transaction.setAmount(500L);
        
        // When
        transaction.execute();
        
        // Then
        verify(sourceAccount, times(1)).withdraw(500L);
        verify(targetAccount, times(1)).deposit(500L);
        assertThat(transaction.getStatus()).isEqualTo(Transaction.Status.COMPLETED);
    }

    @Test
    void testNormalWithdrawal() {
        // Given
        transaction.setType(Transaction.Type.WITHDRAWAL);
        transaction.setAmount(300L);
        transaction.setTargetAccount(null);
        
        // When
        transaction.execute();
        
        // Then
        verify(sourceAccount, times(1)).withdraw(300L);
        verify(targetAccount, never()).deposit(anyLong());
        assertThat(transaction.getStatus()).isEqualTo(Transaction.Status.COMPLETED);
    }

    @Test
    void testNormalDeposit() {
        // Given
        transaction.setType(Transaction.Type.DEPOSIT);
        transaction.setAmount(200L);
        transaction.setTargetAccount(null);
        
        // When
        transaction.execute();
        
        // Then
        verify(sourceAccount, times(1)).deposit(200L);
        verify(targetAccount, never()).withdraw(anyLong());
        assertThat(transaction.getStatus()).isEqualTo(Transaction.Status.COMPLETED);
    }

    @Test
    void testExecute_TransactionAlreadyCompleted() {
        // Given
        transaction.execute();

        // When & Then
        assertThatCode(() -> transaction.execute()).doesNotThrowAnyException();
        assertThat(transaction.getStatus()).isEqualTo(Transaction.Status.COMPLETED);
        verify(sourceAccount, times(1)).withdraw(1000L);
        verify(targetAccount, times(1)).deposit(1000L);
    }

    @Test
    void testCancel_TransactionAlreadyCancelled() {
        transaction.cancel();
        assertThatCode(() -> transaction.cancel()).doesNotThrowAnyException();
        assertThat(transaction.getStatus()).isEqualTo(Transaction.Status.CANCELLED);
        verify(sourceAccount, never()).withdraw(anyLong());
        verify(targetAccount, never()).deposit(anyLong());
    }

    @Test
    void testRollback_TransactionAlreadyRollback() {
        transaction.execute();
        transaction.rollback();
        assertThatCode(() -> transaction.rollback()).doesNotThrowAnyException();
        assertThat(transaction.getStatus()).isEqualTo(Transaction.Status.ROLLBACK);
        verify(sourceAccount, times(1)).withdraw(1000L);
        verify(targetAccount, times(1)).deposit(1000L);
    }

    @Test
    void testFail_TransactionAlreadyFailed() {
        transaction.fail("Test reason");
        transaction.fail("Test reason");

        assertThat(transaction.getStatus()).isEqualTo(Transaction.Status.FAILED);
        assertThat(transaction.getFailureReason()).isEqualTo("Test reason");
    }

    @Test
    void testFail_TransactionNotPending() {
        transaction.setStatus(Transaction.Status.COMPLETED);

        assertThatThrownBy(() -> transaction.fail("Test reason"))
                .isInstanceOf(TransactionStatusInvalid.class)
                .hasMessage("Transaction is not in pending state");
    }

    @Test
    void testFail_TransactionPending() {
        transaction.fail("Test reason");

        assertThat(transaction.getStatus()).isEqualTo(Transaction.Status.FAILED);
        assertThat(transaction.getFailureReason()).isEqualTo("Test reason");
    }
}