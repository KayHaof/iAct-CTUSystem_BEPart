package com.example.initialProject.exception;

import com.example.initialProject.util.RestResponse;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalException {

    // 401 Unauthorized - Sai username/password
    @ExceptionHandler(value = { UsernameNotFoundException.class, BadCredentialsException.class })
    public ResponseEntity<RestResponse<Object>> handleAuthException(Exception ex) {
        RestResponse<Object> res = new RestResponse<>();
        res.setStatusCode(HttpStatus.UNAUTHORIZED.value());
        res.setError(ex.getMessage());
        res.setMessage("Authentication failed!");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(res);
    }

    // 404 Not Found - Id không tồn tại
    @ExceptionHandler(IdInvalidException.class)
    public ResponseEntity<RestResponse<Object>> handleIdInvalidException(IdInvalidException ex) {
        RestResponse<Object> res = new RestResponse<>();
        String message = ex.getMessage().toLowerCase();

        if (message.contains("đăng nhập") || message.contains("token") || message.contains("jwt")) {
            res.setStatusCode(HttpStatus.UNAUTHORIZED.value());
            res.setError("Authentication Required");
            res.setMessage(ex.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(res);
        }

        if (message.contains("quyền") || message.contains("access denied")) {
            res.setStatusCode(HttpStatus.FORBIDDEN.value());
            res.setError(ex.getMessage());
            res.setMessage("Access denied!");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(res);
        }

        res.setStatusCode(HttpStatus.NOT_FOUND.value());
        res.setError(ex.getMessage());
        res.setMessage("Resource not found!");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(res);
    }

    // 400 Bad Request - Validation error
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<RestResponse<Object>> validationError(
        MethodArgumentNotValidException ex
    ) {
        BindingResult result = ex.getBindingResult();
        final List<FieldError> fieldErrors = result.getFieldErrors();

        RestResponse<Object> res = new RestResponse<>();
        res.setStatusCode(HttpStatus.BAD_REQUEST.value());
        res.setError("Validation error");

        List<String> errors = fieldErrors
            .stream()
            .map(FieldError::getDefaultMessage)
            .collect(Collectors.toList());

        res.setMessage(errors.size() > 1 ? errors : errors.get(0));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(res);
    }

    // 404 Not Found - URL không tồn tại
    @ExceptionHandler({ NoResourceFoundException.class, NoHandlerFoundException.class })
    public ResponseEntity<RestResponse<Object>> handleNotFoundException(
        NoResourceFoundException ex
    ) {
        RestResponse<Object> res = new RestResponse<>();
        res.setStatusCode(HttpStatus.NOT_FOUND.value());
        res.setError(ex.getMessage());
        res.setMessage("404 Not Found. URL may not exist!");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(res);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<RestResponse<Object>> handleMethodNotAllowed(
        HttpRequestMethodNotSupportedException ex
    ) {
        RestResponse<Object> res = new RestResponse<>();
        res.setStatusCode(HttpStatus.NOT_FOUND.value());
        res.setError("Method Not Allowed");
        res.setMessage("Đường dẫn không hợp lệ hoặc thiếu tham số trong URL!");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(res);
    }
}
