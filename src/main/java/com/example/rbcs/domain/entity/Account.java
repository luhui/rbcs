package com.example.rbcs.domain.entity;

import com.example.rbcs.domain.exception.AmountInvalidException;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import com.example.rbcs.domain.exception.InsufficientBalanceException;
import com.example.rbcs.domain.exception.AccountStatusInvalid;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.util.Date;

/**
 * 账户实体，负责账户的原子操作
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String accountNumber;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Status status = Status.INITIAL;

    @Builder.Default
    private Long balance = 0L;

    @Version
    private Integer version;

    @CreationTimestamp
    private Date createdAt;
    @UpdateTimestamp
    private Date updatedAt;

    @PostLoad
    public void onLoad() {
        if (status == null) {
            status = Status.INITIAL;
        }

        if (balance == null) {
            balance = 0L;
        }
    }

    public void deposit(long amount) {
        assertAmount(amount);
        assertActivateStatus();
        balance += amount;
        log.info("[aid:{}]deposit amount: {}, current balance: {}", id, amount, balance);
    }

    public void withdraw(long amount) {
        if (!canWithdraw(amount)) {
            throw new InsufficientBalanceException("Insufficient balance");
        }
        balance -= amount;
        log.info("[aid:{}]withdraw amount: {}, current balance: {}", id, amount, balance);
    }

    public boolean canWithdraw(long amount) {
        assertAmount(amount);
        assertActivateStatus();
        return balance >= amount;
    }

    public void freeze() {
        if (status == Status.INITIAL) {
            throw new AccountStatusInvalid("account not activated");
        }

        if (status != Status.FROZEN) {
            log.info("[aid:{}]freeze", id);
            status = Status.FROZEN;
        }
    }

    public void deFreeze() {
        if (status == Status.ACTIVATED) return;

        if (status == Status.FROZEN) {
            log.info("[aid:{}]deFreeze", id);
            status = Status.ACTIVATED;
        } else {
            throw new AccountStatusInvalid("not in frozen status");
        }
    }

    public void activate() {
        if (status == Status.INITIAL) {
            log.info("[aid:{}]activate", id);
            status = Status.ACTIVATED;
        } else {
            throw new AccountStatusInvalid("not in initial status");
        }
    }

    public void assertActivateStatus() {
        if (status != Status.ACTIVATED) {
            throw new AccountStatusInvalid("not in activate status: " + status);
        }
    }

    private void assertAmount(long amount) {
        if (amount <= 0) {
            throw new AmountInvalidException("amount must be positive");
        }
    }

    public enum Status {
        INITIAL,
        ACTIVATED,
        FROZEN
    }
}