package com.example.rbcs.web.controller.form;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AccountForm {
    @NotBlank(message = "Account number is required")
    private String accountNo;
}
