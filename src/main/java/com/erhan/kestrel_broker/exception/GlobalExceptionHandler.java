package com.erhan.kestrel_broker.exception;

import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String,Object>> handleBusiness(BusinessException ex) {
        return ResponseEntity.unprocessableEntity().body(Map.of(
                "title", "Business Rule Violated",
                "status", 422,
                "detail", ex.getMessage()
        ));
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(NotFoundException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("title", "Not Found");
        response.put("detail", ex.getMessage());
        response.put("status", 404);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String,Object>> handleValidation(MethodArgumentNotValidException ex) {
        var body = new HashMap<String,Object>();
        body.put("title", "Validation Failed");
        body.put("status", 400);
        var fe = ex.getBindingResult().getFieldError();
        body.put("detail", fe != null ? fe.getDefaultMessage() : "Validation error");
        return ResponseEntity.badRequest().body(body);
    }

}
