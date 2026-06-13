# DEPLOY — ms-appointment

Guía de despliegue local con Minikube o Docker Desktop + Kubernetes.

---

## Arquitectura de red en el cluster

```
EXTERNO
  │
  ├── frontend        → NodePort / Ingress (expuesto)
  └── bff             → NodePort / Ingress (expuesto, orquesta hacia ms-*)
        │
        │  (red interna ClusterIP — nunca accesible desde fuera)
        ├── ms-user-service:8081
        ├── ms-auth-service:8080
        ├── ms-appointment-service:8082  ← este servicio
        └── ms-waitlist-service:8083
              │
              └── ms-appointment-postgres-svc:5432  (BD exclusiva)
```

---

## Pre-requisitos

| Herramienta | Versión mínima | Verificar con |
|---|---|---|
| Docker | 27+ | `docker --version` |
| kubectl | 1.29+ | `kubectl version --client` |
| Minikube **o** Docker Desktop con K8s | — | `minikube version` / Docker Desktop → Settings → Kubernetes |
| Java 25 JDK (para builds locales) | 25 | `java -version` |

---

## Paso 1 — Construir la imagen Docker

> El contexto de build es la carpeta `ms-appointment/`.
> Ejecuta desde **dentro** de la carpeta del servicio.

```bash
# Desde la raíz multi-proyecto (carpeta padre de ms-appointment):
docker build -t ms-appointment:latest ./ms-appointment/

# O desde dentro de ms-appointment/:
cd ms-appointment/
docker build -t ms-appointment:latest .
```

### Verificar que la imagen existe

```bash
docker images ms-appointment
```

---

## Paso 2 — Cargar la imagen en el cluster local

### Opción A — Minikube

```bash
# Carga la imagen del daemon local de Docker al daemon interno de Minikube
minikube image load ms-appointment:latest

# Verifica que llegó
minikube image ls | grep ms-appointment
```

> Alternativa: evaluar el entorno de Docker de Minikube antes de buildear
> (así el `docker build` escribe directamente en Minikube):
> ```bash
> eval $(minikube docker-env)
> docker build -t ms-appointment:latest .
> ```

### Opción B — Docker Desktop

No se requiere ningún paso adicional.
Docker Desktop comparte el daemon local con su cluster K8s.
La imagen construida en el Paso 1 ya está disponible.

---

## Paso 3 — Preparar los Secrets

Edita `k8s/secret.yaml` y reemplaza los valores de ejemplo con los reales
**antes** de aplicarlos. Este archivo **no se debe subir a Git** con valores reales.

```yaml
# k8s/secret.yaml — sección ms-appointment-db-secret
stringData:
  POSTGRES_USER:     "msappointment_user"      # ← cambia si quieres
  POSTGRES_PASSWORD: "CHANGE_ME_DB_PASSWORD"   # ← pon una contraseña real
  DB_URL:            "jdbc:postgresql://ms-appointment-postgres-svc:5432/msappointment_db"
```

---

## Paso 4 — Aplicar los manifiestos en orden

```bash
# 4.1 Namespace (solo la primera vez)
kubectl apply -f k8s/namespace.yaml

# 4.2 Secrets y ConfigMap
kubectl apply -f k8s/secret.yaml
kubectl apply -f k8s/configmap.yaml

# 4.3 Almacenamiento y base de datos
kubectl apply -f k8s/postgres-pvc.yaml
kubectl apply -f k8s/postgres-deployment.yaml
kubectl apply -f k8s/postgres-service.yaml

# 4.4 Espera a que PostgreSQL esté listo
kubectl rollout status deployment/ms-appointment-postgres -n hospital

# 4.5 Aplicación
kubectl apply -f k8s/deployment.yaml
kubectl apply -f k8s/service.yaml
kubectl apply -f k8s/hpa.yaml
```

O aplica todo de una vez (K8s resolverá el orden):

```bash
kubectl apply -f k8s/
```

---

## Paso 5 — Verificar el despliegue

```bash
# Ver todos los recursos del namespace
kubectl get all -n hospital

# Estado de los pods
kubectl get pods -n hospital -l app=ms-appointment
kubectl get pods -n hospital -l app=ms-appointment-postgres

# Espera a que ms-appointment esté Running
kubectl rollout status deployment/ms-appointment -n hospital

# Logs del pod de la aplicación
kubectl logs -n hospital -l app=ms-appointment --tail=100 -f

# Logs de PostgreSQL
kubectl logs -n hospital -l app=ms-appointment-postgres --tail=50
```

---

## Paso 6 — Probar el servicio localmente

`ms-appointment-service` es ClusterIP (no expuesto fuera del cluster).
Para pruebas locales usa `kubectl port-forward`:

```bash
# Abre el puerto 8082 del pod en tu máquina local
kubectl port-forward -n hospital service/ms-appointment-service 8082:8082
```

Luego en otra terminal:

```bash
# Listar citas (requiere token JWT válido de MS-AUTH)
curl -s http://localhost:8082/api/appointments \
     -H "Authorization: Bearer <TOKEN>" | jq .

# Health check rápido (TCP — no requiere auth)
curl -v --max-time 3 http://localhost:8082/api/appointments 2>&1 | head -5
```

---

## Paso 7 — Eliminar el despliegue (limpieza)

```bash
# Elimina todo excepto el namespace
kubectl delete -f k8s/ --ignore-not-found

# O elimina el namespace completo (elimina TODO dentro de él)
kubectl delete namespace hospital
```

---

## Flujo de trabajo con Docker (resumen rápido)

```bash
# Rebuild y redeploy tras cambios en el código:
docker build -t ms-appointment:latest .
minikube image load ms-appointment:latest     # solo Minikube
kubectl rollout restart deployment/ms-appointment -n hospital
kubectl rollout status deployment/ms-appointment -n hospital
```

---

## Git — Commit inicial y push a GitHub

```bash
# Desde la carpeta ms-appointment/
cd Ms-appointment

# 1. Inicializar el repositorio local
git init

# 2. Agregar todos los archivos (el .gitignore excluirá binarios y secretos)
git add .

# 3. Verificar qué se va a subir (importante antes del primer commit)
git status

# 4. Commit inicial
git commit -m "feat: scaffold inicial de ms-appointment — Java 25, Spring Boot 3.5.3, K8s"

# 5. Renombrar la rama a main
git branch -M main

# 6. Conectar con el repositorio remoto
git remote add origin https://github.com/Elianbarra/Ms-Appointment.git

# 7. Subir
git push -u origin main
```

### Commits posteriores (flujo normal)

```bash
git add .
git commit -m "tipo: descripción corta del cambio"
git push
```

Tipos de commit recomendados: `feat`, `fix`, `refactor`, `docs`, `chore`, `ci`.

---

## Notas de seguridad

- `k8s/secret.yaml` contiene valores de ejemplo. **Nunca subas credenciales reales a Git.**
- Para producción usa un gestor de secretos (HashiCorp Vault, AWS Secrets Manager, Sealed Secrets).
- Las claves PEM / JWKS van en un `Secret` de K8s montado como volumen, no en variables de entorno.
