package com.hospital.msappointment.client.auth.dto;

import lombok.Data;

import java.util.UUID;

/**
 * Claims retornados por MS-AUTH al validar un JWT.
 * El campo "role" permite al filtro autorizar por rol si es necesario.
 */
@Data
public class TokenValidationResponseDTO {
    private boolean valid;
    private UUID userId;
    private String email;
    private String role;        // PATIENT, DOCTOR, NURSE, ADMIN, RECEPTIONIST
}
