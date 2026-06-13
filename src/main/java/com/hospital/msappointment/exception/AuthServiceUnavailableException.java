package com.hospital.msappointment.exception;

public class AuthServiceUnavailableException extends RuntimeException {
    public AuthServiceUnavailableException() {
        super("MS-AUTH no disponible");
    }
}
