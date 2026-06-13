package com.hospital.msappointment.controller;

import com.hospital.msappointment.dto.request.CreateAppointmentRequestDTO;
import com.hospital.msappointment.dto.request.UpdateAppointmentRequestDTO;
import com.hospital.msappointment.dto.response.AppointmentResponseDTO;
import com.hospital.msappointment.service.AppointmentService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
@Tag(name = "Appointments", description = "Gestión de citas médicas")
@SecurityRequirement(name = "bearerAuth")
public class AppointmentController {

    private final AppointmentService appointmentService;

    @PostMapping
    public ResponseEntity<AppointmentResponseDTO> create(
            @Valid @RequestBody CreateAppointmentRequestDTO dto) {
        return ResponseEntity.status(201).body(appointmentService.createAppointment(dto));
    }

    @GetMapping
    public ResponseEntity<List<AppointmentResponseDTO>> getAll() {
        return ResponseEntity.ok(appointmentService.getAllAppointments());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AppointmentResponseDTO> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(appointmentService.getAppointmentById(id));
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<AppointmentResponseDTO>> getByPatient(@PathVariable UUID patientId) {
        return ResponseEntity.ok(appointmentService.getByPatient(patientId));
    }

    @GetMapping("/doctor/{doctorId}")
    public ResponseEntity<List<AppointmentResponseDTO>> getByDoctor(@PathVariable UUID doctorId) {
        return ResponseEntity.ok(appointmentService.getByDoctor(doctorId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AppointmentResponseDTO> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateAppointmentRequestDTO dto) {
        return ResponseEntity.ok(appointmentService.updateAppointment(id, dto));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<AppointmentResponseDTO> cancel(@PathVariable UUID id) {
        return ResponseEntity.ok(appointmentService.cancelAppointment(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        appointmentService.deleteAppointment(id);
        return ResponseEntity.noContent().build();
    }
}
