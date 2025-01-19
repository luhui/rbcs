package com.example.rbcs.web.controller.form;

import com.example.rbcs.domain.entity.Transaction;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TransactionForm {
    /**
     * 交易账号主体
     */
    @NotBlank(message = "Source account number is required")
    private String sourceAccountNo;
    /**
     * 收款账号，当且仅当交易类型为转账时有效，且不能为空
     */
    private String destinationAccountNo;
    /**
     * 交易类型，不能为空
     */
    @NotNull(message = "Transaction type cannot be null")
    private Transaction.Type type;
    /**
     * 交易金额，不能为空且大于0
     */
    @Min(value = 1, message = "Amount cannot be less than 1")
    @Max(value = Long.MAX_VALUE, message = "Amount cannot be greater than " + Long.MAX_VALUE)
    private long amount;
}
