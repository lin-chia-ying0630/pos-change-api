package com.alin.lin.controller;

import com.alin.lin.dto.ResponseBodyDto;
import com.alin.lin.exception.ChangeCaseConflictException;
import com.alin.lin.util.ResponseUtil;
import jakarta.validation.ConstraintViolationException;
import org.mybatis.spring.MyBatisSystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ResponseBodyDto<Void>> handleBadRequest(IllegalArgumentException exception) {
        return ResponseUtil.badRequest(exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ResponseBodyDto<Void>> handleRequestValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getDefaultMessage())
                .collect(Collectors.joining("、"));
        return ResponseUtil.badRequest(message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ResponseBodyDto<Void>> handleConstraintViolation(ConstraintViolationException exception) {
        String message = exception.getConstraintViolations().stream()
                .map(violation -> violation.getMessage())
                .collect(Collectors.joining("、"));
        return ResponseUtil.badRequest(message);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ResponseBodyDto<Void>> handleMethodValidation(HandlerMethodValidationException exception) {
        String message = exception.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream())
                .map(error -> error.getDefaultMessage())
                .collect(Collectors.joining("、"));
        return ResponseUtil.badRequest(message);
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ResponseBodyDto<Void>> handleNotFound(NoSuchElementException exception) {
        return ResponseUtil.notFound(exception.getMessage());
    }

    @ExceptionHandler(ChangeCaseConflictException.class)
    public ResponseEntity<ResponseBodyDto<Void>> handleConflict(ChangeCaseConflictException exception) {
        return ResponseUtil.conflict(exception.getMessage());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ResponseBodyDto<Void>> handleNoResource(NoResourceFoundException exception) {
        return ResponseUtil.notFound("找不到 API 路徑");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ResponseBodyDto<Void>> handleMethodNotAllowed(HttpRequestMethodNotSupportedException exception) {
        return ResponseUtil.builder(org.springframework.http.HttpStatus.METHOD_NOT_ALLOWED, false, null, "", "", "HTTP 方法不允許");
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ResponseBodyDto<Void>> handleIllegalState(IllegalStateException exception) {
        log.error("Unexpected application error", exception);
        return ResponseUtil.serverError(exception.getMessage());
    }

    @ExceptionHandler(CannotGetJdbcConnectionException.class)
    public ResponseEntity<ResponseBodyDto<Void>> handleDatabaseConnection(CannotGetJdbcConnectionException exception) {
        log.error("Database connection failed", exception);
        return ResponseUtil.serviceUnavailable("資料庫連線失敗，請確認 DB_URL、DB_USERNAME、DB_PASSWORD 與 main 資料庫是否已建立");
    }

    @ExceptionHandler(CannotCreateTransactionException.class)
    public ResponseEntity<ResponseBodyDto<Void>> handleTransactionConnection(CannotCreateTransactionException exception) {
        log.error("Database transaction failed", exception);
        return ResponseUtil.serviceUnavailable("資料庫連線失敗，請確認 DB_URL、DB_USERNAME、DB_PASSWORD 與 main 資料庫是否已建立");
    }

    @ExceptionHandler(MyBatisSystemException.class)
    public ResponseEntity<ResponseBodyDto<Void>> handleMyBatis(MyBatisSystemException exception) {
        log.error("Database operation failed", exception);
        return ResponseUtil.serviceUnavailable("資料庫作業失敗，請確認 MySQL 連線、schema.sql 是否已匯入，以及資料表是否存在");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResponseBodyDto<Void>> handleUnexpected(Exception exception) {
        log.error("Unexpected server error", exception);
        return ResponseUtil.serverError("系統發生未預期錯誤");
    }
}
