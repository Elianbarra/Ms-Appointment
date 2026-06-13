package com.hospital.msappointment.client.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TokenValidationRequestDTO {
    private String token;
}
