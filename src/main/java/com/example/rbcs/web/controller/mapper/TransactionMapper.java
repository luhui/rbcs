package com.example.rbcs.web.controller.mapper;

import com.example.rbcs.domain.entity.Transaction;
import com.example.rbcs.web.controller.response.TransactionResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface TransactionMapper {
    @Mapping(source = "sourceAccount.id", target = "sourceAccountId")
    @Mapping(source = "targetAccount.id", target = "targetAccountId")
    @Mapping(source = "status", target = "status", qualifiedByName = "transactionStatusToString")
    TransactionResponse toResponse(Transaction transaction);

     @Named("transactionStatusToString")
     default String transactionStatusToString(Transaction.Status status) {
         return status != null ? status.name() : null;
     }
}