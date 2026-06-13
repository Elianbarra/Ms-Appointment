package com.hospital.msappointment.security;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

/**
 * Representa al usuario autenticado extraído del JWT validado por MS-AUTH.
 * Se almacena en los atributos del request para que el resto de la cadena lo consuma.
 */
@Getter
@Builder
public class AuthenticatedUser {
    private UUID userId;
    private String email;
    private String role;

    public static final String REQUEST_ATTRIBUTE = "authenticatedUser";
}
