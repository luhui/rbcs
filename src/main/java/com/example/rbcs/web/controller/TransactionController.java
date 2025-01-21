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
                    transactionService.createWithdrawTransaction(transactionForm.getSourceAccountId(), transactionForm.getAmount()));
            case DEPOSIT -> transactionMapper.toResponse(
                    transactionService.createDepositTransaction(transactionForm.getSourceAccountId(), transactionForm.getAmount()));
            case TRANSFER -> {
                if (transactionForm.getDestinationAccountId() == null) {
                    throw new IllegalArgumentException("Destination account id is required for transfer");
                }
                if (Objects.equals(transactionForm.getSourceAccountId(), transactionForm.getDestinationAccountId())) {
                    throw new IllegalArgumentException("Source and destination account id cannot be the same");
                }
                yield transactionMapper.toResponse(
                        transactionService.createTransferTransaction(transactionForm.getSourceAccountId(), transactionForm.getDestinationAccountId(), transactionForm.getAmount()));
            }
        };
    }

    @GetMapping("/{id}")
    public TransactionResponse getTransaction(@PathVariable Long id) {
        final var t = transactionRepository.findById(id).orElseThrow(TransactionNotFoundException::new);
        return transactionMapper.toResponse(t);
    }
}