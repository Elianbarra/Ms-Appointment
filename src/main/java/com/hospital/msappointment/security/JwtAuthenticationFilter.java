package com.hospital.msappointment.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospital.msappointment.client.auth.AuthRestClient;
import com.hospital.msappointment.client.auth.dto.TokenValidationResponseDTO;
import com.hospital.msappointment.exception.AuthServiceUnavailableException;
import com.hospital.msappointment.exception.InvalidTokenException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final AuthRestClient authRestClient;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            writeError(response, HttpStatus.UNAUTHORIZED, "Token de autenticación requerido");
            return;
        }

        String rawToken = authHeader.substring(7); // quita "Bearer "

        try {
            TokenValidationResponseDTO claims = authRestClient.validate(rawToken);

            if (!claims.isValid()) {
                writeError(response, HttpStatus.UNAUTHORIZED, "Token inválido");
                return;
            }

            // Adjunta el usuario autenticado al request para uso posterior
            AuthenticatedUser user = AuthenticatedUser.builder()
                    .userId(claims.getUserId())
                    .email(claims.getEmail())
                    .role(claims.getRole())
                    .build();
            request.setAttribute(AuthenticatedUser.REQUEST_ATTRIBUTE, user);

            filterChain.doFilter(request, response);

        } catch (InvalidTokenException e) {
            writeError(response, HttpStatus.UNAUTHORIZED, e.getMessage());
        } catch (AuthServiceUnavailableException e) {
            writeError(response, HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
        }
    }

    private void writeError(HttpServletResponse response,
                             HttpStatus status,
                             String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(
                objectMapper.writeValueAsString(Map.of(
                        "timestamp", LocalDateTime.now().toString(),
                        "status", status.value(),
                        "message", message
                ))
        );
    }
}
