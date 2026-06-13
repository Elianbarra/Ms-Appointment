package com.hospital.msappointment.dto.request;

import com.hospital.msappointment.entity.enums.Specialty;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class CreateAppointmentRequestDTO {

    @NotNull(message = "El ID del paciente es requerido")
    private UUID patientId;

    @NotNull(message = "El ID del doctor es requerido")
    private UUID doctorId;

    @NotNull(message = "La fecha y hora de la cita es requerida")
    @Future(message = "La cita debe ser en una fecha futura")
    private LocalDateTime scheduledAt;

    @Min(value = 10, message = "La duración mínima es 10 minutos")
    @Max(value = 180, message = "La duración máxima es 180 minutos")
    private Integer durationMinutes = 30;

    @NotNull(message = "La especialidad es requerida")
    private Specialty specialty;

    @Size(max = 1000, message = "Las notas no pueden superar los 1000 caracteres")
    private String notes;
}
