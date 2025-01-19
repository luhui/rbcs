package com.example.rbcs.applications;

import com.example.rbcs.domain.entity.Transaction;

public interface TransactionApplicationService {
    /**
     * 转账
     * @param fromAccountNo 转账账号
     * @param toAccountNo 接收账号
     * @param amount 金额
     */
    Transaction createTransferTransaction(String fromAccountNo, String toAccountNo, Long amount);

    /**
     * 提现
     * @param accountNo 账号
     * @param amount 金额
     * @return Transaction
     */
    Transaction createWithdrawTransaction(String accountNo, Long amount);

    /**
     * 存款
     * @param accountNo 账号
     * @param amount 金额
     * @return Transaction
     */
    Transaction createDepositTransaction(String accountNo, Long amount);
}
