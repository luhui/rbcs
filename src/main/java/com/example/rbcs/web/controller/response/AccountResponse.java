package com.example.rbcs.web.controller.response;

import lombok.Data;

import java.util.Date;

@Data
public class AccountResponse {
    private Long id;
    private String accountNumber;
    private String status;
    private Long balance;
    private Date createdAt;
    private Date updatedAt;
}