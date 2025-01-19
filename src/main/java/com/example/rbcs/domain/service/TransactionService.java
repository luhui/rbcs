package com.example.rbcs.domain.service;

import com.example.rbcs.domain.entity.Account;
import com.example.rbcs.domain.entity.Transaction;

public interface TransactionService {
    /**
     * 创建交易
     * @param source 操作账户，存取款转账的主体账户
     * @param target 收款账户，当且仅当type为TRANSFER时，target不为空
     * @param type 交易类型
     * @param amount 转账金额
     */
    Transaction createTransaction(Account source, Account target, Transaction.Type type, long amount);

    /**
     * 执行转账
     * @param transactionId 交易ID
     */
    void executeTransaction(long transactionId);
}
