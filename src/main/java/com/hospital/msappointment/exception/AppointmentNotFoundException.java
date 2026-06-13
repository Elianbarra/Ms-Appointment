package com.hospital.msappointment.exception;

import java.util.UUID;

public class AppointmentNotFoundException extends RuntimeException {
    public AppointmentNotFoundException(UUID id) {
        super("Cita no encontrada con ID: " + id);
    }
}
