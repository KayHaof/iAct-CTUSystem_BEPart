package com.example.exception;

import com.example.dto.ApiResponse;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.autoconfigure.web.reactive.error.AbstractErrorWebExceptionHandler;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
@Order(-2)
public class GlobalErrorWebExceptionHandler extends AbstractErrorWebExceptionHandler {

    public GlobalErrorWebExceptionHandler(ErrorAttributes errorAttributes,
                                          WebProperties webProperties,
                                          ApplicationContext applicationContext,
                                          ServerCodecConfigurer serverCodecConfigurer) {
        super(errorAttributes, webProperties.getResources(), applicationContext);
        this.setMessageWriters(serverCodecConfigurer.getWriters());
    }

    @Override
    protected RouterFunction<ServerResponse> getRoutingFunction(ErrorAttributes errorAttributes) {
        return RouterFunctions.route(RequestPredicates.all(), this::renderErrorResponse);
    }

    private Mono<ServerResponse> renderErrorResponse(ServerRequest request) {
        Map<String, Object> errorProperties = getErrorAttributes(request, ErrorAttributeOptions.defaults());
        Throwable throwable = getError(request);

        HttpStatus httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        String message = "Lỗi hệ thống tại Gateway";
        int appCode = ErrorCode.UNCATEGORIZED_EXCEPTION.getCode();

        if (throwable instanceof ResponseStatusException) {
            ResponseStatusException e = (ResponseStatusException) throwable;
            httpStatus = (HttpStatus) e.getStatusCode();
            message = e.getReason() != null ? e.getReason() : httpStatus.getReasonPhrase();

            if (httpStatus == HttpStatus.NOT_FOUND) {
                appCode = ErrorCode.UNCATEGORIZED_EXCEPTION.getCode();
                message = "Đường dẫn không tồn tại";
            }
        }

        ApiResponse<Object> apiResponse = new ApiResponse<>();
        apiResponse.setCode(appCode);
        apiResponse.setMessage(message);
        // apiResponse.setResult(errorProperties);

        return ServerResponse.status(httpStatus)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(apiResponse));
    }
}