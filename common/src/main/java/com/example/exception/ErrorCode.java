package com.example.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public enum ErrorCode {
    UNCATEGORIZED_EXCEPTION(9999, "Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_KEY(1001, "Invalid message key", HttpStatus.BAD_REQUEST),
    USER_EXISTED(1002, "User already exists", HttpStatus.BAD_REQUEST),
    USERNAME_INVALID(1003, "Username must be at least 3 characters", HttpStatus.BAD_REQUEST),
    PASSWORD_INVALID(1004, "Password must be at least 8 characters", HttpStatus.BAD_REQUEST),
    USER_NOT_EXISTED(1005, "User not found", HttpStatus.NOT_FOUND),
    UNAUTHENTICATED(1006, "Unauthenticated", HttpStatus.UNAUTHORIZED),
    FORBIDDEN(1007, "You do not have permission to execute this action", HttpStatus.FORBIDDEN),
    RESOURCE_NOT_EXISTED(1008, "This resource not found", HttpStatus.NOT_FOUND),
    ACCOUNT_LOCKED(1009,"This account is blocked or inactive. Please contact admin to describe problem and resolve", HttpStatus.LOCKED ),
    VALUE_EXISTED(1010, "This value already exists", HttpStatus.BAD_REQUEST),
    INCORRECT_VALUE(1011, "This value is incorrect or unapproval", HttpStatus.BAD_REQUEST),
    INVALID_ACTION(1012, "This action is not allowed", HttpStatus.FORBIDDEN)
    ;

    private final int code;
    private final String message;
    private final HttpStatusCode statusCode;

    ErrorCode(int code, String message, HttpStatusCode statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }
}