package com.hospital.msappointment.exception;

import java.util.UUID;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(UUID id) {
        super("Usuario no encontrado en MS-USER con ID: " + id);
    }
}
