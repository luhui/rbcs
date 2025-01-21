package com.example.rbcs.domain.service;

import com.example.rbcs.domain.entity.Account;
import com.example.rbcs.domain.entity.Transaction;
import com.example.rbcs.domain.exception.AmountInvalidException;
import com.example.rbcs.domain.exception.TransactionAccountInvalid;
import com.example.rbcs.domain.exception.TransactionNotFoundException;
import com.example.rbcs.domain.repository.AccountRepository;
import com.example.rbcs.domain.repository.TransactionRepository;
import com.example.rbcs.infrastructure.defer.Defers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class TransactionServiceImplTest {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private TransactionServiceImpl transactionService;

    @Autowired
    private AccountRepository accountRepository;

    @MockitoBean
    private ApplicationEventPublisher eventPublisher;

    @MockitoBean
    private Defers defers;

    private Account sourceAccount;
    private Account targetAccount;

    @AfterEach
    void tearDown() {
        // 清理测试数据
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
    }

    @BeforeEach
    void setUp() {
        // 初始化测试数据
        System.out.println("setUp" + accountRepository.findAll());
        sourceAccount = new Account();
        sourceAccount.activate();
        sourceAccount.setBalance(1000L);
        sourceAccount.setAccountNumber("1234567890");
        accountRepository.save(sourceAccount);

        targetAccount = new Account();
        targetAccount.activate();
        targetAccount.setBalance(500L);
        targetAccount.setAccountNumber("0987654321");
        accountRepository.save(targetAccount);
    }

    @Test
    void createTransaction_shouldCreateTransactionSuccessfully() {
        // Arrange
        // Act
        Transaction transaction = transactionService.createTransaction(
                sourceAccount, targetAccount, Transaction.Type.TRANSFER, 100L);

        // Assert
        assertThat(transaction).isNotNull();
        assertThat(transaction.getSourceAccount()).isEqualTo(sourceAccount);
        assertThat(transaction.getTargetAccount()).isEqualTo(targetAccount);
        assertThat(transaction.getType()).isEqualTo(Transaction.Type.TRANSFER);
        assertThat(transaction.getAmount()).isEqualTo(100L);

        Optional<Transaction> savedTransaction = transactionRepository.findById(transaction.getId());
        assertThat(savedTransaction).isPresent();
        assertThat(savedTransaction.get().getStatus()).isEqualTo(Transaction.Status.PENDING);
        assertThat(savedTransaction.get().getSourceAccount().getId()).isEqualTo(sourceAccount.getId());
        assertThat(savedTransaction.get().getTargetAccount().getId()).isEqualTo(targetAccount.getId());
        assertThat(savedTransaction.get().getType()).isEqualTo(Transaction.Type.TRANSFER);
        assertThat(savedTransaction.get().getAmount()).isEqualTo(100L);
    }

    @Test
    void createTransaction_shouldThrowExceptionWhenSourceAccountIsNull() {
        // Act & Assert
        assertThatThrownBy(() -> transactionService.createTransaction(null, targetAccount, Transaction.Type.TRANSFER, 100L))
            .isInstanceOf(TransactionAccountInvalid.class)
            .hasMessage("Source accounts cannot be null");
    }

    @Test
    void createTransaction_shouldThrowExceptionWhenTypeIsNull() {
        // Act & Assert
        assertThatThrownBy(() -> transactionService.createTransaction(sourceAccount, targetAccount, null, 100L))
            .isInstanceOf(TransactionAccountInvalid.class)
            .hasMessage("Transaction type cannot be null");
    }

    @Test
    void createTransaction_shouldThrowExceptionWhenTargetAccountIsNullForTransfer() {
        // Act & Assert
        assertThatThrownBy(() -> transactionService.createTransaction(sourceAccount, null, Transaction.Type.TRANSFER, 100L))
            .isInstanceOf(TransactionAccountInvalid.class)
            .hasMessage("Target accounts cannot be null");
    }

    @Test
    void createTransaction_shouldThrowExceptionWhenSourceAndTargetAreSame() {
        // Arrange

        // Act & Assert
        assertThatThrownBy(() -> transactionService.createTransaction(sourceAccount, sourceAccount, Transaction.Type.TRANSFER, 100L))
            .isInstanceOf(TransactionAccountInvalid.class)
            .hasMessage("Source and target accounts cannot be the same");
    }

    @Test
    void createTransaction_shouldThrowExceptionWhenAmountIsInvalid() {
        // Act & Assert
        assertThatThrownBy(() -> transactionService.createTransaction(sourceAccount, targetAccount, Transaction.Type.TRANSFER, 0L))
            .isInstanceOf(AmountInvalidException.class)
            .hasMessage("Amount must be greater than zero");
    }

    @Test
    void executeTransaction_shouldExecuteTransactionSuccessfully() {
        // Arrange
        var transaction = transactionService.createTransaction(sourceAccount, null, Transaction.Type.DEPOSIT, 100L);

        // Act
        transactionService.executeTransaction(transaction.getId());

        // Assert
        Optional<Transaction> executedTransaction = transactionRepository.findById(transaction.getId());
        assertThat(executedTransaction).isPresent();
        assertThat(executedTransaction.get().getStatus()).isEqualTo(Transaction.Status.COMPLETED);
        var sourceAccount = accountRepository.findById(this.sourceAccount.getId());
        assertThat(sourceAccount.isPresent()).isTrue();
        assertThat(sourceAccount.get().getBalance()).isEqualTo(1100);
    }

    @Test
    void executeTransaction_shouldThrowExceptionWhenTransactionNotFound() {
        // Act & Assert
        assertThatThrownBy(() -> transactionService.executeTransaction(999L))
            .isInstanceOf(TransactionNotFoundException.class);
    }

    @Test
    void executeTransaction_concurrencyDeposit() throws InterruptedException {
        // Arrange
        final var transaction = transactionService.createTransaction(sourceAccount, null, Transaction.Type.DEPOSIT, 100L);

        int threadCount = 2000;
        CountDownLatch latch = new CountDownLatch(threadCount);

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

        // Act
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    transactionService.executeTransaction(transaction.getId());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(); // 等待所有线程执行完毕
        executorService.shutdown();

        // Assert
        Optional<Transaction> executedTransaction = transactionRepository.findById(transaction.getId());
        assertThat(executedTransaction).isPresent();
        assertThat(executedTransaction.get().getStatus()).isEqualTo(Transaction.Status.COMPLETED);
        var sourceAccount = accountRepository.findById(this.sourceAccount.getId());
        assertThat(sourceAccount.isPresent()).isTrue();
        assertThat(sourceAccount.get().getBalance()).isEqualTo(1100);
    }

    @Test
    void executeTransaction_concurrencyWithdrawal() throws InterruptedException {
        // Arrange
        final var transaction = transactionService.createTransaction(sourceAccount, null, Transaction.Type.WITHDRAWAL, 100L);

        int threadCount = 2000;
        CountDownLatch latch = new CountDownLatch(threadCount);

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

        // Act
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    transactionService.executeTransaction(transaction.getId());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(); // 等待所有线程执行完毕
        executorService.shutdown();
        var sourceAccount = accountRepository.findById(this.sourceAccount.getId());

        // Assert
        Optional<Transaction> executedTransaction = transactionRepository.findById(transaction.getId());
        assertThat(executedTransaction).isPresent();
        assertThat(executedTransaction.get().getStatus()).isEqualTo(Transaction.Status.COMPLETED);
        assertThat(sourceAccount.isPresent()).isTrue();
        assertThat(sourceAccount.get().getBalance()).isEqualTo(900);
    }

    @Test
    void executeTransaction_concurrencyTransfer() throws InterruptedException {
        // Arrange
        final var transaction = transactionService.createTransaction(sourceAccount, targetAccount, Transaction.Type.TRANSFER, 100L);

        int threadCount = 2000;
        CountDownLatch latch = new CountDownLatch(threadCount);

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

        // Act
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    transactionService.executeTransaction(transaction.getId());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(); // 等待所有线程执行完毕
        executorService.shutdown();
        var sourceAccount = accountRepository.findById(this.sourceAccount.getId());
        var targetAccount = accountRepository.findById(this.targetAccount.getId());

        // Assert
        Optional<Transaction> executedTransaction = transactionRepository.findById(transaction.getId());
        assertThat(executedTransaction).isPresent();
        assertThat(executedTransaction.get().getStatus()).isEqualTo(Transaction.Status.COMPLETED);
        assertThat(sourceAccount.isPresent()).isTrue();
        assertThat(sourceAccount.get().getBalance()).isEqualTo(900);
        assertThat(targetAccount.isPresent()).isTrue();
        assertThat(targetAccount.get().getBalance()).isEqualTo(600);
    }

    @Test
    void executeTransaction_concurrencyCreateTransferTransaction() throws InterruptedException {
        // Arrange
        int threadCount = 100;
        CountDownLatch latch = new CountDownLatch(threadCount);

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

        // Act
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    final var transaction = transactionService.createTransaction(sourceAccount, targetAccount, Transaction.Type.TRANSFER, 1L);
                    transactionService.executeTransaction(transaction.getId());
                } catch (Exception e) {
                    System.err.println("Error: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(); // 等待所有线程执行完毕
        executorService.shutdown();
        var sourceAccount = accountRepository.findById(this.sourceAccount.getId());
        var targetAccount = accountRepository.findById(this.targetAccount.getId());

        // Assert
        assertThat(sourceAccount.isPresent()).isTrue();
        assertThat(sourceAccount.get().getBalance()).isEqualTo(900);
        assertThat(targetAccount.isPresent()).isTrue();
        assertThat(targetAccount.get().getBalance()).isEqualTo(600);
    }
}