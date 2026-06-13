package com.hospital.msappointment.dto.request;

import com.hospital.msappointment.entity.enums.AppointmentStatus;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UpdateAppointmentRequestDTO {

    @Future(message = "La nueva fecha debe ser en el futuro")
    private LocalDateTime scheduledAt;

    @Min(value = 10, message = "La duración mínima es 10 minutos")
    @Max(value = 180, message = "La duración máxima es 180 minutos")
    private Integer durationMinutes;

    private AppointmentStatus status;

    @Size(max = 1000, message = "Las notas no pueden superar los 1000 caracteres")
    private String notes;
}
