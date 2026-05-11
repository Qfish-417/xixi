package com.xixi.manjuagent.exception;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 全局异常处理器 - 统一处理所有异常，防止错误信息泄露
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理参数校验异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        FieldError::getDefaultMessage
                ));
        
        log.warn("参数校验失败: {}", errors);
        
        return ResponseEntity.badRequest().body(buildErrorResponse(
                "参数校验失败",
                errors,
                HttpStatus.BAD_REQUEST.value()
        ));
    }

    /**
     * 处理Token过期异常
     */
    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<Map<String, Object>> handleExpiredJwtException(ExpiredJwtException ex) {
        log.warn("Token已过期: {}", ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(buildErrorResponse(
                "Token已过期，请重新登录",
                null,
                HttpStatus.UNAUTHORIZED.value()
        ));
    }

    /**
     * 处理Token签名异常
     */
    @ExceptionHandler(SignatureException.class)
    public ResponseEntity<Map<String, Object>> handleSignatureException(SignatureException ex) {
        log.warn("Token签名验证失败: {}", ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(buildErrorResponse(
                "Token签名无效",
                null,
                HttpStatus.UNAUTHORIZED.value()
        ));
    }

    /**
     * 处理Token格式异常
     */
    @ExceptionHandler(MalformedJwtException.class)
    public ResponseEntity<Map<String, Object>> handleMalformedJwtException(MalformedJwtException ex) {
        log.warn("Token格式错误: {}", ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(buildErrorResponse(
                "Token格式错误",
                null,
                HttpStatus.UNAUTHORIZED.value()
        ));
    }

    /**
     * 处理不支持的Token格式异常
     */
    @ExceptionHandler(UnsupportedJwtException.class)
    public ResponseEntity<Map<String, Object>> handleUnsupportedJwtException(UnsupportedJwtException ex) {
        log.warn("不支持的Token格式: {}", ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(buildErrorResponse(
                "不支持的Token格式",
                null,
                HttpStatus.UNAUTHORIZED.value()
        ));
    }

    /**
     * 处理认证异常
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAuthenticationException(AuthenticationException ex) {
        log.warn("认证失败: {}", ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(buildErrorResponse(
                "认证失败，请检查用户名或密码",
                null,
                HttpStatus.UNAUTHORIZED.value()
        ));
    }

    /**
     * 处理凭证错误异常（用户名/密码错误）
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentialsException(BadCredentialsException ex) {
        log.warn("凭证错误: {}", ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(buildErrorResponse(
                "用户名或密码错误",
                null,
                HttpStatus.UNAUTHORIZED.value()
        ));
    }

    /**
     * 处理业务异常（自定义异常）
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, Object>> handleBusinessException(BusinessException ex) {
        log.warn("业务异常: {}", ex.getMessage());
        
        return ResponseEntity.status(ex.getStatus()).body(buildErrorResponse(
                ex.getMessage(),
                ex.getDetails(),
                ex.getStatus().value()
        ));
    }

    /**
     * 处理非法参数异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("非法参数: {}", ex.getMessage());
        
        return ResponseEntity.badRequest().body(buildErrorResponse(
                ex.getMessage(),
                null,
                HttpStatus.BAD_REQUEST.value()
        ));
    }

    /**
     * 处理其他运行时异常（非业务异常）
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
        // 记录详细日志
        log.error("运行时异常: {}", ex.getMessage(), ex);
        
        // 生产环境不返回详细错误信息
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(buildErrorResponse(
                "服务器内部错误，请稍后重试",
                null,
                HttpStatus.INTERNAL_SERVER_ERROR.value()
        ));
    }

    /**
     * 处理其他未知异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        log.error("未知异常: {}", ex.getMessage(), ex);
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(buildErrorResponse(
                "服务器内部错误，请稍后重试",
                null,
                HttpStatus.INTERNAL_SERVER_ERROR.value()
        ));
    }

    /**
     * 构建统一的错误响应格式
     */
    private Map<String, Object> buildErrorResponse(String message, Object details, int status) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", true);
        response.put("message", message);
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("status", status);
        
        if (details != null) {
            response.put("details", details);
        }
        
        return response;
    }
}