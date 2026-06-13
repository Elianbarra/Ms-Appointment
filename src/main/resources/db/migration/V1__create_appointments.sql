-- V1: Tabla principal de citas médicas
CREATE TABLE appointments (
    id              UUID            NOT NULL DEFAULT gen_random_uuid(),
    patient_id      UUID            NOT NULL,
    doctor_id       UUID            NOT NULL,
    scheduled_at    TIMESTAMP       NOT NULL,
    duration_minutes INT            NOT NULL DEFAULT 30,
    specialty       VARCHAR(50)     NOT NULL,
    status          VARCHAR(30)     NOT NULL DEFAULT 'PENDING',
    notes           TEXT,
    active          BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_appointments PRIMARY KEY (id),
    CONSTRAINT chk_status CHECK (status IN ('PENDING','CONFIRMED','CANCELLED','COMPLETED','NO_SHOW')),
    CONSTRAINT chk_duration CHECK (duration_minutes > 0)
);

-- Índices para las consultas más frecuentes
CREATE INDEX idx_appointments_patient   ON appointments (patient_id);
CREATE INDEX idx_appointments_doctor    ON appointments (doctor_id);
CREATE INDEX idx_appointments_status    ON appointments (status);
CREATE INDEX idx_appointments_scheduled ON appointments (scheduled_at);
