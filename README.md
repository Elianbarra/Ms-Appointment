# MS-APPOINTMENT — Microservicio de Gestión de Citas

Microservicio desarrollado con **Spring Boot 3.5.3** encargado de gestionar las citas médicas del sistema hospitalario. Se conecta a **NeonDB (PostgreSQL)** para persistir los datos y valida la existencia de pacientes y doctores consultando **MS-USER** mediante OpenFeign. Se despliega en **Kubernetes (K8s)**.

---

## Tecnologías y Versiones

| Tecnología | Versión | Uso |
|---|---|---|
| Java | 25 (LTS) | Lenguaje principal |
| Spring Boot | 3.5.3 | Framework principal |
| Spring Cloud | 2025.0.0 | Gestión de dependencias cloud |
| Gradle (Kotlin DSL) | 8.14 | Herramienta de build |
| Spring Data JPA | gestionado por Spring Boot 3.5.3 | Acceso a datos |
| Hibernate | 7.x (incluido en Spring Boot 3.5.3) | ORM |
| PostgreSQL Driver | 42.7.x (gestionado por Spring Boot 3.5.3) | Conector JDBC |
| PostgreSQL | 17 | Motor de base de datos (NeonDB) |
| Flyway | 11.x (gestionado por Spring Boot 3.5.3) | Migraciones de base de datos |
| Spring Cloud OpenFeign | gestionado por Spring Cloud 2025.0.0 | Comunicación con MS-USER |
| Spring Validation | gestionado por Spring Boot 3.5.3 | Validación de DTOs |
| Lombok | gestionado por Spring Boot 3.5.3 | Reducción de boilerplate |
| spring-dotenv | 4.0.0 | Carga automática del `.env` en local |
| eclipse-temurin | 25-jre | Imagen base JRE para Docker/K8s |
| Kubernetes | 1.32+ | Orquestación de contenedores |

---

## Por qué Java 25 y no Java 21 (a diferencia de MS-USER)

MS-USER documenta incompatibilidades concretas entre Java 25 y el stack de Spring Boot 3.4.x. En MS-APPOINTMENT esas incompatibilidades están resueltas:

1. **Spring Cloud OpenFeign — URL validation**: Spring Cloud 2025.0.0 (OpenFeign 4.3.x) corrige el comportamiento de validación de `java.net.URL` que causaba `MalformedURLException` con placeholders sin resolver. La validación ahora se difiere hasta que el `Environment` de Spring ha cargado todas las propiedades.

2. **Flyway 11.x — classloading conflict**: Flyway 11.x resuelve el conflicto de `NullFlywayTelemetryManager` al reestructurar el sistema de plugins internos para ser compatible con el module system de Java 25.

3. **Soporte oficial**: Spring Boot 3.5.x incluye Java 25 en su matriz de versiones validadas.

---

## Por qué Kubernetes y no Docker Compose

MS-USER usa Docker Compose para orquestar el stack localmente, lo cual es suficiente para desarrollo. MS-APPOINTMENT da el siguiente paso hacia producción real usando Kubernetes:

| Capacidad | Docker Compose | Kubernetes |
|---|---|---|
| Escalado automático | No | HPA (HorizontalPodAutoscaler) |
| Self-healing (reinicio de pods caídos) | Limitado | Nativo |
| Rolling deployments sin downtime | No | Nativo |
| Gestión de secretos | Variables de entorno | `Secret` resources |
| Health checks declarativos | Básico | Liveness / Readiness probes |
| Multi-nodo | No | Sí |

En un sistema hospitalario donde la disponibilidad es crítica, K8s permite actualizar el servicio sin cortar el tráfico, escalar ante picos de demanda y recuperarse automáticamente de fallos.

---

## Patrones de Diseño

### 1. Repository Pattern

**Archivo:** `repository/AppointmentRepository.java`

Misma motivación que en MS-USER. La lógica de negocio no sabe ni le importa si la base de datos es NeonDB, PostgreSQL local u otra. `AppointmentRepository` declara métodos como `findByPatientIdAndActiveTrue` o `existsByDoctorIdAndScheduledAtBetween...` sin escribir SQL.

```
Controller → AppointmentClient → AppointmentRepository → NeonDB
```

### 2. Facade Pattern

**Archivo:** `client/AppointmentClient.java`

El Controller delega toda la orquestación al Facade. Para crear una cita, el Facade:
1. Valida que el paciente exista en MS-USER (vía Feign)
2. Valida que el doctor exista en MS-USER (vía Feign)
3. Verifica que no haya solapamiento de horario para el doctor
4. Persiste la entidad `Appointment`
5. Retorna el DTO de respuesta

```java
// El Controller solo ve esto:
return ResponseEntity.status(201).body(appointmentClient.createAppointment(dto));
```

---

## Estructura del Proyecto

```
Ms-appointment/
├── Dockerfile
├── .env.example
├── build.gradle.kts
├── settings.gradle.kts
├── k8s/
│   ├── namespace.yaml          ← namespace "hospital"
│   ├── configmap.yaml          ← variables no sensibles
│   ├── secret.yaml             ← credenciales DB y URLs internas
│   ├── deployment.yaml         ← pods, probes, recursos
│   ├── service.yaml            ← ClusterIP interno
│   └── hpa.yaml                ← autoescalado por CPU/memoria
└── src/main/
    ├── resources/
    │   ├── application.yml
    │   └── db/migration/
    │       └── V1__create_appointments.sql
    └── java/com/hospital/msappointment/
        ├── MsAppointmentApplication.java
        ├── controller/
        │   └── AppointmentController.java     ← recibe HTTP, delega al Facade
        ├── dto/
        │   ├── request/
        │   │   ├── CreateAppointmentRequestDTO.java
        │   │   └── UpdateAppointmentRequestDTO.java
        │   └── response/
        │       └── AppointmentResponseDTO.java
        ├── entity/
        │   ├── Appointment.java
        │   └── enums/
        │       ├── AppointmentStatus.java     (PENDING, CONFIRMED, CANCELLED, COMPLETED, NO_SHOW)
        │       └── Specialty.java             (GENERAL, CARDIOLOGY, NEUROLOGY, ...)
        ├── repository/
        │   └── AppointmentRepository.java     ← Patrón Repository
        ├── client/
        │   ├── AppointmentClient.java         ← Patrón Facade (capa de negocio)
        │   └── user/
        │       ├── UserFeignClient.java        ← cliente HTTP hacia MS-USER
        │       └── dto/
        │           └── UserResponseDTO.java
        └── exception/
            ├── GlobalExceptionHandler.java
            ├── AppointmentNotFoundException.java
            └── AppointmentConflictException.java
```

---

## Endpoints

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/api/appointments` | Crea una nueva cita |
| `GET` | `/api/appointments` | Lista todas las citas activas |
| `GET` | `/api/appointments/{id}` | Obtiene una cita por ID |
| `GET` | `/api/appointments/patient/{patientId}` | Citas de un paciente |
| `GET` | `/api/appointments/doctor/{doctorId}` | Citas de un doctor |
| `PUT` | `/api/appointments/{id}` | Actualiza datos de la cita |
| `PUT` | `/api/appointments/{id}/cancel` | Cancela la cita |
| `DELETE` | `/api/appointments/{id}` | Desactiva la cita (soft delete) |

---

## Flujo de Creación de Cita

```
POST /api/appointments
        ↓
  AppointmentController
        ↓
  AppointmentClient (Facade)
     ├── GET /api/users/{patientId}  →  MS-USER (valida que existe)
     ├── GET /api/users/{doctorId}   →  MS-USER (valida que existe)
     ├── Verifica solapamiento de horario del doctor
     └── Guarda Appointment en NeonDB  →  tabla: appointments
```

---

## Configuración

### Opción A — Local con NeonDB

Crear `.env` en la raíz copiando `.env.example`:

```env
DB_URL=jdbc:postgresql://HostNeonDB/msappointment_db?sslmode=require&channel_binding=require
DB_USER=neondb_owner
DB_PASS=*********
MS_USER_URL=http://localhost:8081
```

```bash
./gradlew bootRun
```

El servicio inicia en el puerto **8082**.

### Opción B — Kubernetes

**Pre-requisito:** el namespace `hospital` y los secrets de MS-USER ya deben existir.

```bash
# 1. Crear namespace (solo la primera vez)
kubectl apply -f k8s/namespace.yaml

# 2. Editar k8s/secret.yaml con los valores reales y aplicar
kubectl apply -f k8s/secret.yaml

# 3. Aplicar ConfigMap
kubectl apply -f k8s/configmap.yaml

# 4. Desplegar
kubectl apply -f k8s/deployment.yaml
kubectl apply -f k8s/service.yaml
kubectl apply -f k8s/hpa.yaml

# 5. Verificar pods
kubectl get pods -n hospital
```

El servicio queda accesible dentro del cluster en `http://ms-appointment-service:8082`.

---

## Arquitectura en K8s

```
┌─────────────────────────── Namespace: hospital ─────────────────────────────┐
│                                                                              │
│  ┌───────────────────────────┐        ┌─────────────────────────────────┐   │
│  │  ms-appointment           │──────▶ │  ms-user-service                │   │
│  │  Deployment (2–6 pods)    │        │  ClusterIP :8081                │   │
│  │  ClusterIP :8082          │        └─────────────────────────────────┘   │
│  │  HPA: CPU 70% / Mem 80%   │                                              │
│  └───────────────────────────┘                                              │
│                │                                                             │
│                ▼                                                             │
│         NeonDB (PostgreSQL 17 — externo)                                    │
│         tabla: appointments                                                  │
└──────────────────────────────────────────────────────────────────────────────┘
```

---

## Base de Datos

- **Proveedor (producción):** NeonDB (PostgreSQL serverless) — `sa-east-1`
- **Migraciones:** Flyway 11.x — scripts en `src/main/resources/db/migration/`
- **Tabla principal:** `appointments`
- **`ddl-auto`:** `none` — Hibernate no toca el esquema, Flyway es el único responsable
