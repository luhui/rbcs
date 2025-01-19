package com.example.rbcs.web.controller;

import com.example.rbcs.applications.TransactionApplicationService;
import com.example.rbcs.domain.exception.TransactionNotFoundException;
import com.example.rbcs.domain.repository.TransactionRepository;
import com.example.rbcs.web.controller.form.TransactionForm;
import com.example.rbcs.web.controller.mapper.TransactionMapper;
import com.example.rbcs.web.controller.response.TransactionResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Slf4j
public class TransactionController {

    private final TransactionApplicationService transactionService;
    private final TransactionMapper transactionMapper;
    private final TransactionRepository transactionRepository;

    /**
     * 创建交易
     * @param transactionForm 交易参数
     * @return TransactionResponse
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse createTransaction(@Valid @RequestBody TransactionForm transactionForm) {
        log.info("Creating transaction: {}", transactionForm);
        return switch (transactionForm.getType()) {
            case WITHDRAWAL -> transactionMapper.toResponse(
                    transactionService.createWithdrawTransaction(transactionForm.getSourceAccountNo(), transactionForm.getAmount()));
            case DEPOSIT -> transactionMapper.toResponse(
                    transactionService.createDepositTransaction(transactionForm.getSourceAccountNo(), transactionForm.getAmount()));
            case TRANSFER -> {
                if (!StringUtils.hasText(transactionForm.getDestinationAccountNo())) {
                    throw new IllegalArgumentException("Destination account number is required for transfer");
                }
                if (Objects.equals(transactionForm.getSourceAccountNo(), transactionForm.getDestinationAccountNo())) {
                    throw new IllegalArgumentException("Source and destination account numbers cannot be the same");
                }
                yield transactionMapper.toResponse(
                        transactionService.createTransferTransaction(transactionForm.getSourceAccountNo(), transactionForm.getDestinationAccountNo(), transactionForm.getAmount()));
            }
        };
    }

    @GetMapping("/{id}")
    public TransactionResponse getTransaction(@PathVariable Long id) {
        final var t = transactionRepository.findById(id).orElseThrow(TransactionNotFoundException::new);
        return transactionMapper.toResponse(t);
    }
}