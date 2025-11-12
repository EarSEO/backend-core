package com.earseo.core.common.exception;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class BaseException extends RuntimeException {
    private final ErrorCodeInterface errorCodeInterface;

    public ErrorCode getErrorCode() {
        return this.errorCodeInterface.getErrorCode();
    }
}
