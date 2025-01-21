package com.example.rbcs.web.controller;

import com.example.rbcs.domain.entity.Account;
import com.example.rbcs.domain.repository.AccountRepository;
import com.example.rbcs.web.controller.form.AccountForm;
import com.example.rbcs.web.controller.response.AccountResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AccountControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private AccountRepository accountRepository;

    private long setupAccountId;

    @AfterEach
    public void teardown() {
        // reset data
        accountRepository.deleteAllInBatch();
    }

    @BeforeEach
    public void setup() {
        AccountForm form = new AccountForm();
        form.setAccountNo("1234567");

        // Act
        ResponseEntity<AccountResponse> response = restTemplate.postForEntity(
                "/api/v1/accounts", form, AccountResponse.class);
        setupAccountId = response.getBody().getId();
    }

    @Test
    void getAccount_success() {
        // Arrange
        Long accountId = setupAccountId;

        // Act
        ResponseEntity<AccountResponse> response = restTemplate.getForEntity(
                "/api/v1/accounts/{id}", AccountResponse.class, accountId);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        AccountResponse accountResponse = response.getBody();
        assertThat(accountResponse.getId()).isEqualTo(accountId);
        assertThat(accountResponse.getAccountNumber()).isEqualTo("1234567");
        assertThat(accountResponse.getStatus()).isEqualTo("INITIAL");
        assertThat(accountResponse.getBalance()).isZero();
    }

    @Test
    void getAccountByNo_success() {
        // Arrange
        Long accountId = setupAccountId;

        // Act
        ResponseEntity<AccountResponse> response = restTemplate.getForEntity(
                "/api/v1/accounts?accountNo={accountNo}", AccountResponse.class, "1234567");

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        AccountResponse accountResponse = response.getBody();
        assertThat(accountResponse.getId()).isEqualTo(accountId);
        assertThat(accountResponse.getAccountNumber()).isEqualTo("1234567");
        assertThat(accountResponse.getStatus()).isEqualTo("INITIAL");
        assertThat(accountResponse.getBalance()).isZero();
    }

    @Test
    void getAccount_NotExist_shouldReturn404() {
        // Arrange
        Long accountId = 999L; // Assuming this ID does not exist

        // Act & Assert
        ResponseEntity<String> response = restTemplate.getForEntity("/api/v1/accounts/{id}", String.class, accountId);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getAccountByNo_NotExist_shouldReturn404() {

        // Act & Assert
        ResponseEntity<String> response = restTemplate.getForEntity("/api/v1/accounts?accountNo={accountNo}", String.class, "accountNotExist");

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void createAccountRepeat_shouldReturn400() {
        // Arrange
        AccountForm form = new AccountForm();
        form.setAccountNo("1234567");

        // Act
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/accounts", form, String.class);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).contains("resource exist");
    }

    @Test
    void activateAccount_success() {
        // Arrange
        Long accountId = setupAccountId;

        // Act
        restTemplate.put("/api/v1/accounts/{id}/status/activate", null, accountId);

        // Assert
        Account account = accountRepository.findById(accountId).orElseThrow();
        assertThat(account.getStatus()).isEqualTo(Account.Status.ACTIVATED);
    }

    @Test
    void freezeAccount_success() {
        // Arrange
        Long accountId = setupAccountId;
        restTemplate.put("/api/v1/accounts/{id}/status/activate", null, accountId);

        // Act
        restTemplate.put("/api/v1/accounts/{id}/status/freeze", null, accountId);

        // Assert
        Account account = accountRepository.findById(accountId).orElseThrow();
        assertThat(account.getStatus()).isEqualTo(Account.Status.FROZEN);
    }

    @Test
    void unfreezeAccount_success() {
        // Arrange
        Long accountId = setupAccountId;
        restTemplate.put("/api/v1/accounts/{id}/status/activate", null, accountId);
        restTemplate.put("/api/v1/accounts/{id}/status/freeze", null, accountId);

        // Act
        restTemplate.put("/api/v1/accounts/{id}/status/unfreeze", null, accountId);

        // Assert
        Account account = accountRepository.findById(accountId).orElseThrow();
        assertThat(account.getStatus()).isEqualTo(Account.Status.ACTIVATED);
    }

    @Test
    void activateAccount_notFound() {
        // Arrange
        Long invalidAccountId = 999L;

        // Act
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/accounts/{id}/status/activate",
                HttpMethod.PUT,
                null,
                String.class,
                invalidAccountId
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).contains("Account not found with id: 999");
    }

    @Test
    void activateAccount_whenFrozen_shouldReturn400() {
        // Arrange
        Long accountId = setupAccountId;
        restTemplate.put("/api/v1/accounts/{id}/status/activate", null, accountId);
        restTemplate.put("/api/v1/accounts/{id}/status/freeze", null, accountId);

        // Act
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/accounts/{id}/status/activate",
                HttpMethod.PUT,
                null,
                String.class,
                accountId
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("not in initial status");
    }

    @Test
    void activateAccount_whenActivated_shouldReturn400() {
        // Arrange
        Long accountId = setupAccountId;
        restTemplate.put("/api/v1/accounts/{id}/status/activate", null, accountId);

        // Act
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/accounts/{id}/status/activate",
                HttpMethod.PUT,
                null,
                String.class,
                accountId
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("not in initial status");
    }

    @Test
    void concurrentAccountOperations() throws InterruptedException {
        // Arrange
        Long accountId = setupAccountId;
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        restTemplate.put("/api/v1/accounts/{id}/status/activate", null, accountId);

        // Act
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    restTemplate.put("/api/v1/accounts/{id}/status/freeze", null, accountId);
                    restTemplate.put("/api/v1/accounts/{id}/status/unfreeze", null, accountId);
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        executor.shutdown();

        // Assert
        Account account = accountRepository.findById(accountId).orElseThrow();
        assertThat(account.getStatus()).isIn(Account.Status.ACTIVATED, Account.Status.FROZEN);
    }
}