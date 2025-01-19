package com.example.rbcs.web.controller;

import com.example.rbcs.domain.entity.Account;
import com.example.rbcs.domain.entity.Transaction;
import com.example.rbcs.domain.repository.AccountRepository;
import com.example.rbcs.domain.repository.TransactionRepository;
import com.example.rbcs.web.controller.form.AccountForm;
import com.example.rbcs.web.controller.form.TransactionForm;
import com.example.rbcs.web.controller.response.AccountResponse;
import com.example.rbcs.web.controller.response.TransactionResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = "classpath:application-test.properties")
class TransactionControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AccountRepository accountRepository;

    private AccountResponse sourceAccount;
    private AccountResponse destinationAccount;

    @AfterEach
    public void teardown() {
        transactionRepository.deleteAllInBatch();
        accountRepository.deleteAllInBatch();
    }

    @BeforeEach
    public void setup() {
        sourceAccount = createAccount("1234567890");
        destinationAccount = createAccount("1122334455");
    }

    private AccountResponse createAccount(String accountNo) {
        var form = new AccountForm();
        form.setAccountNo(accountNo);
        var res = restTemplate.postForEntity("/api/v1/accounts", form, AccountResponse.class).getBody();
        restTemplate.put("/api/v1/accounts/{id}/status/activate", null, res.getId());
        return getAccount(res.getId());
    }

    private AccountResponse getAccount(long id) {
        return restTemplate.getForEntity("/api/v1/accounts/{id}", AccountResponse.class, id).getBody();
    }

    private TransactionResponse createTransaction(Transaction.Type type, long amount) throws InterruptedException {
        var form = new TransactionForm();
        form.setSourceAccountNo(sourceAccount.getAccountNumber());
        form.setDestinationAccountNo(destinationAccount.getAccountNumber());
        form.setType(type);
        form.setAmount(amount);
        var res = restTemplate.postForEntity("/api/v1/transactions", form, TransactionResponse.class);
        System.out.println(res);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(res.getBody().getSourceAccountId()).isEqualTo(sourceAccount.getId().toString());
        if (type == Transaction.Type.TRANSFER) {
            assertThat(res.getBody().getTargetAccountId()).isEqualTo(destinationAccount.getId().toString());
        } else {
            assertThat(res.getBody().getTargetAccountId()).isNull();
        }

        Thread.sleep(100);
        return getTransaction(Long.parseLong(res.getBody().getId()));
    }

    private TransactionResponse getTransaction(long id) {
        var res = restTemplate.getForEntity("/api/v1/transactions/{id}", TransactionResponse.class, id);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        return res.getBody();
    }

    @Test
    void createWithdrawalTransaction_success() throws Exception {
        // Arrange
        createTransaction(Transaction.Type.DEPOSIT, 1000L);

        // Act
        var transactionResponse = createTransaction(Transaction.Type.WITHDRAWAL, 1000L);

        // Assert
        assertThat(transactionResponse).isNotNull();
        assertThat(transactionResponse.getSourceAccountId()).isEqualTo(sourceAccount.getId().toString());
        assertThat(transactionResponse.getAmount()).isEqualTo(1000L);

        var account = getAccount(sourceAccount.getId());
        assertThat(account.getBalance()).isEqualTo(0);
    }

    @Test
    void createDepositTransaction_success() throws Exception {

        // Act
        var transactionResponse = createTransaction(Transaction.Type.DEPOSIT, 500L);

        // Assert
        assertThat(transactionResponse).isNotNull();
        assertThat(transactionResponse.getSourceAccountId()).isEqualTo(sourceAccount.getId().toString());
        assertThat(transactionResponse.getAmount()).isEqualTo(500L);

        var account = getAccount(sourceAccount.getId());
        assertThat(account.getBalance()).isEqualTo(500L);
    }

    @Test
    void createTransferTransaction_success() throws Exception {
        // Arrange
        createTransaction(Transaction.Type.DEPOSIT, 1000L);

        // Act
        var transactionResponse = createTransaction(Transaction.Type.TRANSFER, 200L);

        // Assert
        assertThat(transactionResponse).isNotNull();
        assertThat(transactionResponse.getAmount()).isEqualTo(200L);

        var sourceAccountResponse = getAccount(sourceAccount.getId());
        var destinationAccountResponse = getAccount(destinationAccount.getId());

        assertThat(sourceAccountResponse.getBalance()).isEqualTo(800L);
        assertThat(destinationAccountResponse.getBalance()).isEqualTo(200L);
    }


    @Test
    void getTransaction_notFound() {
        // Act
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/v1/transactions/999", String.class);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).contains("TransactionNotFoundException");
    }

    @Test
    void createTransferTransaction_insufficientBalance() throws Exception {
        // Arrange
        createTransaction(Transaction.Type.DEPOSIT, 100L);

        // Act
        var transactionResponse = createTransaction(Transaction.Type.TRANSFER, 1000L);

        // Assert
        assertThat(transactionResponse).isNotNull();
        assertThat(transactionResponse.getStatus()).isEqualTo("FAILED");
        assertThat(transactionResponse.getFailureReason()).contains("Insufficient balance");
    }

    @Test
    void createTransferTransaction_destinationAccountNotFound() throws Exception {
        // Arrange
        createTransaction(Transaction.Type.DEPOSIT, 1000L);

        // Act
        var form = new TransactionForm();
        form.setSourceAccountNo(sourceAccount.getAccountNumber());
        form.setDestinationAccountNo("9999999999"); // 不存在的账户
        form.setType(Transaction.Type.TRANSFER);
        form.setAmount(200L);
        ResponseEntity<String> response = restTemplate.postForEntity("/api/v1/transactions", form, String.class);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).contains("AccountNotFoundException");
    }

    @Test
    void createTransferTransaction_missingDestinationAccount() throws Exception {
        // Arrange
        createTransaction(Transaction.Type.DEPOSIT, 1000L);

        // Act
        var form = new TransactionForm();
        form.setSourceAccountNo(sourceAccount.getAccountNumber());
        form.setType(Transaction.Type.TRANSFER);
        form.setAmount(200L);
        ResponseEntity<String> response = restTemplate.postForEntity("/api/v1/transactions", form, String.class);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("Destination account number is required for transfer");
    }
}