package com.hospital.msappointment.client.auth;

import com.hospital.msappointment.client.auth.dto.TokenValidationRequestDTO;
import com.hospital.msappointment.client.auth.dto.TokenValidationResponseDTO;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class AuthRestClient {

    private final RestClient restClient;

    public AuthRestClient(@Qualifier("authRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    /**
     * Llama a MS-AUTH para validar el JWT con la clave pública (.pem).
     * MS-AUTH firma y verifica — MS-APPOINTMENT no toca las claves.
     *
     * @param rawToken token sin el prefijo "Bearer "
     * @return claims del token si es válido
     */
    public TokenValidationResponseDTO validate(String rawToken) {
        return restClient.post()
                .uri("/api/auth/validate")
                .body(new TokenValidationRequestDTO(rawToken))
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                    throw new com.hospital.msappointment.exception.InvalidTokenException();
                })
                .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                    throw new com.hospital.msappointment.exception.AuthServiceUnavailableException();
                })
                .body(TokenValidationResponseDTO.class);
    }
}
