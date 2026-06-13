package com.hospital.msappointment.client.user.dto;

import lombok.Data;

import java.util.UUID;

/**
 * DTO que refleja la respuesta de MS-USER.
 * Solo incluye los campos que MS-APPOINTMENT necesita.
 */
@Data
public class UserResponseDTO {
    private UUID id;
    private String firstName;
    private String lastName;
    private String email;
    private String role;   // PATIENT, DOCTOR, NURSE, ADMIN, RECEPTIONIST
}
