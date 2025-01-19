package com.example.rbcs.web.controller.mapper;

import com.example.rbcs.domain.entity.Account;
import com.example.rbcs.web.controller.response.AccountResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface AccountMapper {
    @Mapping(source = "status", target = "status", qualifiedByName = "transactionStatusToString")
    AccountResponse toResponse(Account account);

     @Named("transactionStatusToString")
     default String transactionStatusToString(Account.Status status) {
         return status != null ? status.name() : null;
     }
}