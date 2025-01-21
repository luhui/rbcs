package com.example.rbcs.web.error;

import com.example.rbcs.domain.exception.AccountNotFoundException;
import com.example.rbcs.domain.exception.DomainException;
import com.example.rbcs.domain.exception.TransactionNotFoundException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

/**
 * 统一错误转换
 * TODO: 实际业务中这里应该要把具体的异常转换成错误码，方便端侧枚举错误码进行处理
 */
@Slf4j
@ControllerAdvice
public class GlobalErrorHandler {
    @ExceptionHandler(value = {DomainException.class, IllegalArgumentException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<CommonErrorResponse> handleDomainException(Exception ex, WebRequest request) {
        log.error("[{}]Exception occurred: {}", request.getDescription(false), getExceptionAllInformation(ex));
        var status = HttpStatus.BAD_REQUEST;
        if (ex instanceof AccountNotFoundException || ex instanceof TransactionNotFoundException) {
            status = HttpStatus.NOT_FOUND;
        }
        return new ResponseEntity<>(
                new CommonErrorResponse(ex.getClass().getSimpleName(), ex.getMessage()),
                status);
    }

    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public ResponseEntity<CommonErrorResponse> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException ex, WebRequest request) {
        BindingResult bindingResult = ex.getBindingResult();

        // 构建自定义的验证错误消息
        StringBuilder errorMessageBuilder = new StringBuilder();
        for (FieldError fieldError : bindingResult.getFieldErrors()) {
            errorMessageBuilder.append(fieldError.getField())
                    .append(": ")
                    .append(fieldError.getDefaultMessage())
                    .append("; ");
        }
        String errorMessage = errorMessageBuilder.toString();

         log.error("[{}] Validation exception occurred: {}", request.getDescription(false), errorMessage);

        CommonErrorResponse errorResponse = new CommonErrorResponse("ValidationException", errorMessage);
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(value = DataIntegrityViolationException.class)
    public ResponseEntity<CommonErrorResponse> handleConstraintViolationException(
            ConstraintViolationException ex, WebRequest request) {
        log.error("[{}] Constraint violation exception occurred: {}", request.getDescription(false), getExceptionAllInformation(ex));
        return new ResponseEntity<>(new CommonErrorResponse("ConstraintViolationException", "Unexpected attribute or resource exist"), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(value = RuntimeException.class)
    public ResponseEntity<CommonErrorResponse> handleRuntimeException(Exception ex, WebRequest request) {
        log.error("[{}]Unexpected exception occurred: {}", request.getDescription(false), getExceptionAllInformation(ex));
        return new ResponseEntity<>(new CommonErrorResponse(ex.getClass().getSimpleName(), ex.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Data
    @AllArgsConstructor
    public static class CommonErrorResponse {
        private String exception;
        private String message;
    }

    public static String getExceptionAllInformation(Exception exception) {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            exception.printStackTrace(new PrintStream(byteArrayOutputStream));
            return byteArrayOutputStream.toString();
        } catch (Exception var2) {
            return exception != null ? exception.getMessage() : null;
        }
    }
}
