package com.example.rbcs.web.controller.response;

import lombok.Data;

import java.util.Date;

@Data
public class TransactionResponse {
    private String id;
    private String sourceAccountId;
    private String targetAccountId;
    private long amount;
    private String status;
    private String failureReason;;
    private String type;
    private Date createdAt;
    private Date updatedAt;
}