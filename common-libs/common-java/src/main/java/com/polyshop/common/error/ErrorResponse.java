package com.polyshop.common.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
public class ErrorResponse {

    private Instant timestamp;
    private int status;
    private String error;
    private String code;
    private String message;
    private String path;
    private String requestId;
    private Map<String, Object> details;

    public ErrorResponse() {
        this.timestamp = Instant.now();
    }

    public ErrorResponse(int status,
                         String error,
                         String code,
                         String message,
                         String path,
                         String requestId,
                         Map<String, Object> details) {
        this.timestamp = Instant.now();
        this.status = status;
        this.error = error;
        this.code = code;
        this.message = message;
        this.path = path;
        this.requestId = requestId;
        this.details = details;
    }

    public static ErrorResponse of(int status,
                                   String error,
                                   String code,
                                   String message) {
        return new ErrorResponse(status, error, code, message, null, null, null);
    }
}
