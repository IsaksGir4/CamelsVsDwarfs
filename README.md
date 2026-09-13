# Camel vs Dwarf Racing System

Sistema de gestión de carreras de camellos y enanos para la EIA Camel vs. Dwarf
Racing League — proyecto académico de la asignatura de Implementación e Integración.

## Equipo

- Julio José Jaller Cordero
- Isaac Giraldo

## Arquitectura

- **Backend**: API REST en Spring Boot, arquitectura por capas (`controller` →
  `service` → `repository` → `entity`/`dto`)
- **Autenticación/Autorización**: Keycloak como proveedor de identidad OAuth2/OIDC
  (resource server) — el backend nunca almacena contraseñas
- **Base de datos**: PostgreSQL
- **Frontend**: (pendiente — ver sección de estado por módulo)

## Tecnologías

| Componente | Tecnología |
|---|---|
| Lenguaje / Framework | Java 21, Spring Boot 4.1 |
| Seguridad | Keycloak 26.4 (OAuth2 Resource Server) |
| Base de datos | PostgreSQL 15 |
| ORM | Hibernate / Spring Data JPA |
| Contenedores | Docker, Docker Compose |
| Testing | JUnit 5, Mockito, AssertJ |
| CI | GitHub Actions |

## Modelo de base de datos

Ver diagrama ER: `DBi&I2026-Page-1.drawio.png` (raíz del repo).

## Estrategia de seguridad

Toda la autenticación pasa por **Keycloak**, no por código propio del backend:

- El backend actúa como **resource server**: valida los JWT que emite Keycloak
  (firma, emisor, expiración) usando `spring-boot-starter-oauth2-resource-server`
- Los roles vienen en el claim `realm_access.roles` del token, y se mapean a
  autoridades de Spring Security (`ROLE_ADMIN`, `ROLE_ORGANIZER`, `ROLE_VIEWER`)
  en `SecurityConfig.java`
- No existe endpoint de login/registro propio — el login ocurre directamente
  contra Keycloak (redirect a su pantalla, o `grant_type=password` para pruebas)
- La entidad local `User` (`entity/User.java`) **no almacena contraseñas** — se
  sincroniza automáticamente la primera vez que un usuario autenticado hace
  cualquier request (`UserSyncService.findOrCreateUser`), usando el claim `sub`
  del token como identificador estable
- Respuestas estructuradas: `401` (sin token o inválido) y `403` (rol
  insuficiente) devuelven JSON con `timestamp`, `status`, `error`, `message`, `path`

### Roles y permisos

| Rol | Permisos |
|---|---|
| `admin` | Gestión total: usuarios, competidores, equipos, carreras, registros, resultados, audit log |
| `organizer` | Gestiona carreras, registros y resultados; solo lectura de competidores/equipos |
| `viewer` | Solo lectura: información pública, calendarios, resultados, standings |

Los roles se asignan **desde la consola de administración de Keycloak**
(`http://localhost:8180/admin/master/console/` → realm `camelsvsdwarfs` →
Users → seleccionar usuario → Role mapping), no desde un endpoint propio del
backend — decisión de alcance dado el tiempo disponible (ver "Known limitations").

## Instalación y ejecución

### Requisitos
- Docker Desktop
- Java 21 (solo si se quiere correr el backend fuera de Docker)

### Pasos

1. Clonar el repositorio
2. Crear `.env` en la raíz, con base en `.env.example`:
3. Levantar todo:
```powershell
   docker compose up -d --build
```
4. Esperar a que los 3 contenedores (`app`, `db`, `keycloak`) estén `healthy`:
```powershell
   docker compose ps
```
5. **Paso manual único, necesario después de cada `docker compose down -v`**
   (ver "Known limitations" — es una limitación de Keycloak, no un paso opcional):
    - Entrar a `http://localhost:8180/admin/master/console/` (usuario/contraseña:
      los que pusiste en `KEYCLOAK_ADMIN_USER`/`KEYCLOAK_ADMIN_PASSWORD`)
    - Cambiar al realm `camelsvsdwarfs`
    - **Realm settings → Default roles → Assign role → `viewer`**
    - Esto hace que todo usuario nuevo (incluidos los que se auto-registren)
      reciba automáticamente acceso de lectura

### URLs y puertos

| Servicio | URL |
|---|---|
| Backend API | http://localhost:8080 |
| Keycloak (login/token) | http://localhost:8180 |
| Keycloak Admin Console | http://localhost:8180/admin/master/console/ |
| PostgreSQL | localhost:5433 |

## Usuarios de prueba

| Username | Password | Rol |
|---|---|---|
| `admin` | `admin123` | admin |
| `organizer` | `organizer123` | organizer |
| `viewer` | `viewer123` | viewer |

## Ejemplos de uso de la API

### Obtener un token (Keycloak)


## Testing

```powershell
cd backend
.\gradlew clean test
```

## Known Limitations / Future Improvements

- **Rol por defecto para usuarios nuevos**: Keycloak no aplica correctamente el
  rol `default-roles-<realm>` cuando el realm se importa junto con usuarios
  explícitos — confirmado con múltiples intentos y respaldado por una discusión
  oficial del propio equipo de Keycloak
  (github.com/keycloak/keycloak/discussions/44782). El código de importación de
  Keycloak explícitamente omite la asignación de roles por defecto a usuarios
  que vienen en el archivo de importación. Por eso, después de cada
  `docker compose down -v`, es necesario el paso manual descrito arriba
  (Realm settings → Default roles → Assign role → `viewer`).
- **Gestión de roles vía API propia**: la asignación/remoción de roles a
  usuarios (ej. `PATCH /api/users/{id}/role`) y la integración con un Service
  Account de Keycloak para automatizar esto desde el backend quedaron fuera de
  alcance por tiempo. El PDF no lo exige explícitamente; los roles se gestionan
  desde la consola de administración de Keycloak.

