package com.hospital.msappointment.client.user;

import com.hospital.msappointment.client.user.dto.UserResponseDTO;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Component
public class UserRestClient {

    private final RestClient restClient;

    public UserRestClient(@Qualifier("userRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public UserResponseDTO getUserById(UUID id) {
        return restClient.get()
                .uri("/api/users/{id}", id)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                    throw new com.hospital.msappointment.exception.UserNotFoundException(id);
                })
                .body(UserResponseDTO.class);
    }
}
