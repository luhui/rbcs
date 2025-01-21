package com.example.rbcs.domain.entity;

import com.example.rbcs.domain.exception.AmountInvalidException;
import com.example.rbcs.domain.exception.TransactionAccountInvalid;
import com.example.rbcs.domain.exception.TransactionStatusInvalid;
import com.example.rbcs.domain.exception.TransactionTypeInvalid;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.domain.AbstractAggregateRoot;

import java.util.Date;
import java.util.Objects;

/**
 * 交易实体，负责交易过程的原子操作，包括账户的调度、状态管理
 */
@Data
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Slf4j
@org.hibernate.annotations.Cache(region = "RBCS", usage = CacheConcurrencyStrategy.READ_WRITE)
@Table(indexes = {
        @Index(name = "idx_transaction_status", columnList = "status")
})
public class Transaction extends AbstractAggregateRoot<Transaction> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @ManyToOne
    @JoinColumn(name = "source_account_id")
    private Account sourceAccount;
    @ManyToOne
    @JoinColumn(name = "target_account_id")
    private Account targetAccount;
    private long amount;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Status status = Status.PENDING;

    @Enumerated(EnumType.STRING)
    private Type type;

    private String failureReason;

    @CreationTimestamp
    private Date createdAt;
    @UpdateTimestamp
    private Date updatedAt;

    @Version
    private Integer version;

    @PrePersist
    public void prePersist() {
        if (type == null) {
            throw new TransactionTypeInvalid("Transaction type must be provided");
        }
        if (amount <= 0) {
            throw new AmountInvalidException("Amount must be positive");
        }

        switch (type) {
            case WITHDRAWAL:
            case DEPOSIT:
                if (sourceAccount == null) {
                    throw new TransactionAccountInvalid("Source account must be provided for " + type);
                }
                break;
            case TRANSFER:
                if (sourceAccount == null || targetAccount == null) {
                    throw new TransactionAccountInvalid("Both source and target accounts must be provided for transfer");
                }
                break;
            default:
                throw new TransactionAccountInvalid("Unknown transaction type: " + type);
        }
    }

    @PostLoad
    public void postLoad() {
        if (status == null) {
            status = Status.PENDING;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transaction that = (Transaction) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, sourceAccount, targetAccount, amount, status, createdAt, updatedAt);
    }

    public void cancel() {
        if (status == Status.CANCELLED) return;
        if (status != Status.PENDING) {
            throw new TransactionStatusInvalid("Transaction is not in pending status");
        }
        status = Status.CANCELLED;
    }

    public void rollback() {
        if (status == Status.ROLLBACK) return;
        if (status != Status.COMPLETED) {
            throw new TransactionStatusInvalid("Transaction is not in completed status");
        }

        switch (type) {
            case WITHDRAWAL:
                sourceAccount.deposit(amount);
                break;
            case DEPOSIT:
                sourceAccount.withdraw(amount);
                break;
            case TRANSFER:
                targetAccount.withdraw(amount);
                sourceAccount.deposit(amount);
                break;
            default:
                throw new IllegalArgumentException("Unknown transaction type: " + type);
        }
        
        status = Status.ROLLBACK;
    }

    /**
     * 执行交易，根据类型进行相应的账户操作
     * 非线程安全操作，调用方必须保证线程安全
     */
    public void execute() {
        log.info("[{}]Executing transaction: [{}][{}]", id, type, amount);
        // 保障幂等
        if (status == Status.COMPLETED) return;
        if (status != Status.PENDING) {
            throw new TransactionStatusInvalid("Transaction is not in pending state");
        }

        switch (type) {
            case WITHDRAWAL:
                sourceAccount.withdraw(amount);
                break;
            case DEPOSIT:
                sourceAccount.deposit(amount);
                break;
            case TRANSFER:
                sourceAccount.withdraw(amount);
                targetAccount.deposit(amount);
                break;
            default:
                throw new IllegalArgumentException("Unknown transaction type: " + type);
        }

        status = Status.COMPLETED;
    }

    public void fail(String reason) {
        if (status == Status.FAILED) return;
        if (status != Status.PENDING) {
            throw new TransactionStatusInvalid("Transaction is not in pending state");
        }
        status = Status.FAILED;
        this.failureReason = reason;
    }

    public enum Status {
        PENDING,
        COMPLETED,
        ROLLBACK,
        CANCELLED,
        FAILED
    }

    public enum Type {
        WITHDRAWAL,  // 取款
        DEPOSIT,     // 存款
        TRANSFER     // 转账
    }
}