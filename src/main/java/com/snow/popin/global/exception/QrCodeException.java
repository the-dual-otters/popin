package com.snow.popin.global.exception;


public class QrCodeException extends RuntimeException {
    public QrCodeException(String message) {
        super(message);
    }

    public QrCodeException(Throwable cause) {
        super(cause);
    }

    public QrCodeException(String message, Throwable cause) {
        super(message, cause);
    }
}
