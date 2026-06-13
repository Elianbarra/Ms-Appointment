package com.hospital.msappointment.repository;

import com.hospital.msappointment.entity.Appointment;
import com.hospital.msappointment.entity.enums.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

    List<Appointment> findByPatientIdAndActiveTrue(UUID patientId);

    List<Appointment> findByDoctorIdAndActiveTrue(UUID doctorId);

    List<Appointment> findByActiveTrue();

    Optional<Appointment> findByIdAndActiveTrue(UUID id);

    List<Appointment> findByStatusAndActiveTrue(AppointmentStatus status);

    // Detecta solapamiento de citas para un doctor
    boolean existsByDoctorIdAndScheduledAtBetweenAndActiveTrueAndStatusNot(
            UUID doctorId,
            LocalDateTime start,
            LocalDateTime end,
            AppointmentStatus excludedStatus
    );
}
