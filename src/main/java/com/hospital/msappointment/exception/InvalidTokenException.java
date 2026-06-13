package com.hospital.msappointment.exception;

public class InvalidTokenException extends RuntimeException {
    public InvalidTokenException() {
        super("Token JWT inválido o expirado");
    }
}
