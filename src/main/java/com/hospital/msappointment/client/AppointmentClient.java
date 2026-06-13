package com.hospital.msappointment.client;

import com.hospital.msappointment.client.user.UserRestClient;
import com.hospital.msappointment.dto.request.CreateAppointmentRequestDTO;
import com.hospital.msappointment.dto.request.UpdateAppointmentRequestDTO;
import com.hospital.msappointment.dto.response.AppointmentResponseDTO;
import com.hospital.msappointment.entity.Appointment;
import com.hospital.msappointment.entity.enums.AppointmentStatus;
import com.hospital.msappointment.exception.AppointmentConflictException;
import com.hospital.msappointment.exception.AppointmentNotFoundException;
import com.hospital.msappointment.repository.AppointmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Facade Pattern — coordina la lógica de negocio de citas.
 * El Controller solo interactúa con esta clase.
 */
@Component
@RequiredArgsConstructor
public class AppointmentClient {

    private final AppointmentRepository appointmentRepository;
    private final UserRestClient userRestClient;

    // ─── Create ──────────────────────────────────────────────────────────────

    public AppointmentResponseDTO createAppointment(CreateAppointmentRequestDTO dto) {
        // Valida que el paciente y el doctor existan en MS-USER
        userRestClient.getUserById(dto.getPatientId());
        userRestClient.getUserById(dto.getDoctorId());

        // Verifica que no haya solapamiento de horario para el doctor
        LocalDateTime end = dto.getScheduledAt().plusMinutes(dto.getDurationMinutes());
        boolean overlap = appointmentRepository
                .existsByDoctorIdAndScheduledAtBetweenAndActiveTrueAndStatusNot(
                        dto.getDoctorId(),
                        dto.getScheduledAt(),
                        end,
                        AppointmentStatus.CANCELLED
                );
        if (overlap) {
            throw new AppointmentConflictException("El doctor ya tiene una cita en ese horario");
        }

        Appointment appointment = Appointment.builder()
                .patientId(dto.getPatientId())
                .doctorId(dto.getDoctorId())
                .scheduledAt(dto.getScheduledAt())
                .durationMinutes(dto.getDurationMinutes())
                .specialty(dto.getSpecialty())
                .notes(dto.getNotes())
                .build();

        return toResponse(appointmentRepository.save(appointment));
    }

    // ─── Read ─────────────────────────────────────────────────────────────────

    public List<AppointmentResponseDTO> getAllAppointments() {
        return appointmentRepository.findByActiveTrue()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public AppointmentResponseDTO getAppointmentById(UUID id) {
        return appointmentRepository.findByIdAndActiveTrue(id)
                .map(this::toResponse)
                .orElseThrow(() -> new AppointmentNotFoundException(id));
    }

    public List<AppointmentResponseDTO> getByPatient(UUID patientId) {
        return appointmentRepository.findByPatientIdAndActiveTrue(patientId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<AppointmentResponseDTO> getByDoctor(UUID doctorId) {
        return appointmentRepository.findByDoctorIdAndActiveTrue(doctorId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ─── Update ───────────────────────────────────────────────────────────────

    public AppointmentResponseDTO updateAppointment(UUID id, UpdateAppointmentRequestDTO dto) {
        Appointment appointment = appointmentRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new AppointmentNotFoundException(id));

        if (dto.getScheduledAt() != null)     appointment.setScheduledAt(dto.getScheduledAt());
        if (dto.getDurationMinutes() != null)  appointment.setDurationMinutes(dto.getDurationMinutes());
        if (dto.getStatus() != null)           appointment.setStatus(dto.getStatus());
        if (dto.getNotes() != null)            appointment.setNotes(dto.getNotes());

        return toResponse(appointmentRepository.save(appointment));
    }

    // ─── Cancel / Delete ──────────────────────────────────────────────────────

    public AppointmentResponseDTO cancelAppointment(UUID id) {
        Appointment appointment = appointmentRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new AppointmentNotFoundException(id));
        appointment.setStatus(AppointmentStatus.CANCELLED);
        return toResponse(appointmentRepository.save(appointment));
    }

    public void deleteAppointment(UUID id) {
        Appointment appointment = appointmentRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new AppointmentNotFoundException(id));
        appointment.setActive(false);
        appointmentRepository.save(appointment);
    }

    // ─── Mapper ───────────────────────────────────────────────────────────────

    private AppointmentResponseDTO toResponse(Appointment a) {
        return AppointmentResponseDTO.builder()
                .id(a.getId())
                .patientId(a.getPatientId())
                .doctorId(a.getDoctorId())
                .scheduledAt(a.getScheduledAt())
                .durationMinutes(a.getDurationMinutes())
                .specialty(a.getSpecialty())
                .status(a.getStatus())
                .notes(a.getNotes())
                .createdAt(a.getCreatedAt())
                .updatedAt(a.getUpdatedAt())
                .build();
    }
}
