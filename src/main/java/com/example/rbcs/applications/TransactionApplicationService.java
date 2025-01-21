package com.example.rbcs.applications;

import com.example.rbcs.domain.entity.Transaction;

public interface TransactionApplicationService {
    /**
     * 转账
     * @param fromAccountId 转账账号
     * @param toAccountId 接收账号
     * @param amount 金额
     */
    Transaction createTransferTransaction(Long fromAccountId, Long toAccountId, Long amount);

    /**
     * 提现
     * @param accountId 账号id
     * @param amount 金额
     * @return Transaction
     */
    Transaction createWithdrawTransaction(Long accountId, Long amount);

    /**
     * 存款
     * @param accountId 账号id
     * @param amount 金额
     * @return Transaction
     */
    Transaction createDepositTransaction(Long accountId, Long amount);
}
