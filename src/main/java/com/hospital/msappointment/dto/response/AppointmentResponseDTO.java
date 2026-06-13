package com.hospital.msappointment.dto.response;

import com.hospital.msappointment.entity.enums.AppointmentStatus;
import com.hospital.msappointment.entity.enums.Specialty;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class AppointmentResponseDTO {
    private UUID id;
    private UUID patientId;
    private UUID doctorId;
    private LocalDateTime scheduledAt;
    private Integer durationMinutes;
    private Specialty specialty;
    private AppointmentStatus status;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
