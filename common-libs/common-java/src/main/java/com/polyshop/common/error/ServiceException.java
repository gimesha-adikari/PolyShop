package com.polyshop.common.error;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import java.util.Map;

@Getter
public class ServiceException extends RuntimeException {

    private final HttpStatus status;
    private final String code;
    private final String path;
    private final Map<String, Object> details;

    public ServiceException(
            HttpStatus status,
            String code,
            String message,
            String path,
            Map<String, Object> details
    ) {
        super(message);
        this.status = status;
        this.code = code;
        this.path = path;
        this.details = details;
    }

}
