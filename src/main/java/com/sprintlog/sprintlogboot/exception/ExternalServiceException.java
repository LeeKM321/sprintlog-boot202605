package com.sprintlog.sprintlogboot.exception;

public class ExternalServiceException extends BusinessException {
    public ExternalServiceException(ErrorCode errorCode, String detail, Throwable cause) {
        super(errorCode, detail, cause);
    }
}
