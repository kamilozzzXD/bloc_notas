# 📒 Bloc de Notas — Monorepo Full-Stack

Aplicación de bloc de notas con autenticación JWT, construida como monorepo con:

- **`/backend`** → Spring Boot 4.x + Spring Security + MySQL + JWT
- **`/frontend`** → React 19 + TypeScript + Vite + Axios

---

## 📁 Estructura del Proyecto

```
bloc_notas/
├── backend/
│   ├── src/main/java/com/blocnotas/backend/
│   │   ├── BackendApplication.java          ← Punto de entrada Spring Boot
│   │   ├── controller/
│   │   │   └── AuthController.java          ← POST /api/auth/login y /register
│   │   ├── dto/
│   │   │   ├── AuthRequest.java             ← DTO de entrada { username, password }
│   │   │   ├── AuthResponse.java            ← DTO de salida { token, tipo, username }
│   │   │   └── MensajeResponse.java         ← DTO para mensajes simples
│   │   ├── entity/
│   │   │   └── Usuario.java                 ← Entidad JPA → tabla MySQL "usuarios"
│   │   ├── repository/
│   │   │   └── UsuarioRepository.java       ← Spring Data JPA Repository
│   │   └── security/
│   │       ├── JwtUtils.java                ← Generación y validación de JWT
│   │       ├── JwtAuthFilter.java           ← Filtro que extrae el Bearer token
│   │       ├── JwtAuthEntryPoint.java       ← Handler para respuestas 401 en JSON
│   │       ├── SecurityConfig.java          ← Configuración central de Spring Security
│   │       └── UserDetailsServiceImpl.java  ← Carga usuarios desde MySQL
│   ├── src/main/resources/
│   │   └── application.properties           ← Config MySQL + HikariCP + JWT vía env vars
│   ├── .env.example                         ← Variables de entorno requeridas (copiar como .env)
│   ├── .dockerignore                        ← Excluye target/, .env y archivos innecesarios
│   ├── Dockerfile                           ← Build multi-stage: Maven → JRE Alpine
│   └── pom.xml                              ← Dependencias Maven (JJWT 0.12.6 incluido)
│
├── frontend/
│   ├── src/
│   │   ├── api/
│   │   │   ├── apiClient.ts                 ← Instancia Axios con interceptores JWT
│   │   │   └── authApi.ts                   ← Funciones login() y register()
│   │   ├── components/
│   │   │   └── ProtectedRoute.tsx           ← Guard para rutas privadas
│   │   ├── pages/
│   │   │   ├── LoginPage.tsx                ← Formulario de inicio de sesión
│   │   │   ├── RegisterPage.tsx             ← Formulario de registro
│   │   │   ├── HomePage.tsx                 ← Vista protegida post-login
│   │   │   ├── Auth.module.css              ← Estilos dark glassmorphism Login/Register
│   │   │   └── HomePage.module.css          ← Estilos de la página principal
│   │   ├── App.tsx                          ← Router con rutas públicas y protegidas
│   │   ├── main.tsx                         ← Punto de entrada React
│   │   └── index.css                        ← Reset global + fuente Inter
│   ├── .env.example                         ← Variables de entorno frontend
│   └── package.json
│
└── README.md                                ← Este archivo
```

---

## ⚙️ Backend — Spring Boot

### Tecnologías

| Librería | Versión | Uso |
|---|---|---|
| Spring Boot | 4.1.0 | Framework principal |
| Spring Security | 6.x (incluido en Boot 4) | Autenticación y autorización |
| Spring Data JPA | 3.x | ORM con Hibernate |
| MySQL Connector/J | runtime | Driver de base de datos |
| JJWT | 0.12.6 | Generación y validación de JWT |
| HikariCP | incluido | Pool de conexiones |
| Jakarta Validation | incluido | Validación de DTOs |

### Cómo funciona el sistema de seguridad

```
Petición HTTP
     │
     ▼
JwtAuthFilter                 ← Lee header "Authorization: Bearer <token>"
     │ Token válido?
     ├─ Sí → Carga UserDetails → setAuthentication(SecurityContext)
     └─ No → Continúa sin autenticación
     │
     ▼
SecurityFilterChain
     ├─ /api/auth/**  → permitAll() (sin JWT)
     └─ /**           → authenticated() (JWT requerido)
     │
     ▼ Si no autenticado
JwtAuthEntryPoint             ← Devuelve JSON 401 (no HTML)
```

### Flujo de Registro

```
POST /api/auth/register
Body: { "username": "camil", "password": "mipass123" }

1. Validar campos (Bean Validation)
2. Verificar que el username no exista (HTTP 409 si duplicado)
3. Encriptar password con BCryptPasswordEncoder
4. Persistir Usuario en MySQL
5. Responder HTTP 201 { "mensaje": "Usuario registrado correctamente..." }
```

### Flujo de Login

```
POST /api/auth/login
Body: { "username": "camil", "password": "mipass123" }

1. AuthenticationManager.authenticate() → carga usuario + verifica BCrypt
2. Si inválido → Spring lanza BadCredentialsException → HTTP 401 automático
3. Si válido → JwtUtils.generateToken(username) → JWT firmado con HMAC-SHA256
4. Responder HTTP 200 { "token": "eyJhbG...", "tipo": "Bearer", "username": "camil" }
```

### Configuración `application.properties`

Toda la configuración sensible se lee desde **variables de entorno**:

```properties
# Conexión MySQL
spring.datasource.url=jdbc:mysql://${DB_HOST}:${DB_PORT}/${DB_NAME}?...
spring.datasource.username=${DB_USER}
spring.datasource.password=${DB_PASS}

# HikariCP (plan gratuito = máximo 3 conexiones)
spring.datasource.hikari.maximum-pool-size=3

# JWT
jwt.secret=${JWT_SECRET}
jwt.expiration-ms=${JWT_EXPIRATION_MS}
```

### Variables de entorno requeridas

Copia `backend/.env.example` como referencia:

| Variable | Descripción | Ejemplo |
|---|---|---|
| `DB_HOST` | Host de MySQL | `localhost` |
| `DB_PORT` | Puerto de MySQL | `3306` |
| `DB_NAME` | Nombre de la base de datos | `bloc_notas` |
| `DB_USER` | Usuario MySQL | `root` |
| `DB_PASS` | Contraseña MySQL | `secreto` |
| `JWT_SECRET` | Clave secreta Base64 (64+ chars) | ver `.env.example` |
| `JWT_EXPIRATION_MS` | Duración del token en ms | `86400000` (24h) |

> 💡 **Genera tu JWT_SECRET** con: `openssl rand -base64 64`

### Cómo ejecutar el backend

**Prerequisitos:** Java 21+, Maven 3.9+, MySQL corriendo.

```bash
# 1. Crear la base de datos en MySQL
mysql -u root -p -e "CREATE DATABASE bloc_notas CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# 2. Configurar variables de entorno (Windows PowerShell)
$env:DB_HOST="localhost"
$env:DB_PORT="3306"
$env:DB_NAME="bloc_notas"
$env:DB_USER="root"
$env:DB_PASS="tu_contraseña"
$env:JWT_SECRET="dGhpcy1pcy1hLXZlcnktbG9uZy1zZWNyZXQta2V5LWZvci1ibG9jLW5vdGFzLWFwcA=="
$env:JWT_EXPIRATION_MS="86400000"

# 3. Ejecutar
cd backend
./mvnw spring-boot:run
# ó en Windows: mvnw.cmd spring-boot:run
```

El servidor queda disponible en `http://localhost:8080`.

---

## 🎨 Frontend — React + TypeScript + Vite

### Tecnologías

| Librería | Versión | Uso |
|---|---|---|
| React | 19.x | UI |
| TypeScript | 6.x | Tipado estático |
| Vite | 8.x | Dev server y bundler |
| React Router DOM | 7.x | Enrutado SPA |
| Axios | 1.x | Cliente HTTP |

### Cómo funciona la autenticación en el frontend

```
Usuario llena formulario Login
          │
          ▼
authApi.login()  →  POST /api/auth/login
          │
          │  ¿Respuesta 200?
          ├─ Sí → sessionStorage.setItem('token', jwt)
          │        sessionStorage.setItem('username', nombre)
          │        navigate('/')  → HomePage (protegida)
          └─ No → Mostrar banner de error con el mensaje del backend

En cada petición subsiguiente:
apiClient interceptor → añade header "Authorization: Bearer <token>"

Si el backend responde 401:
apiClient interceptor → sessionStorage.clear() → redirige a /login
```

### Almacenamiento del JWT

El token se guarda en **`sessionStorage`** (no `localStorage`):
- Se borra automáticamente al cerrar la pestaña/navegador.
- Más seguro en entornos compartidos.
- Para persistencia entre sesiones, cambia a `localStorage` en `LoginPage.tsx`.

### Estructura de rutas

| Ruta | Componente | Acceso |
|---|---|---|
| `/` | `HomePage` | 🔒 Privada (requiere JWT) |
| `/login` | `LoginPage` | 🌐 Pública |
| `/register` | `RegisterPage` | 🌐 Pública |
| `/*` | Redirect → `/login` | — |

### Cómo ejecutar el frontend

**Prerequisitos:** Node.js 18+, pnpm.

```bash
# 1. Entrar a la carpeta frontend
cd frontend

# 2. Copiar variables de entorno (opcional si backend está en localhost:8080)
copy .env.example .env.local
# Editar VITE_API_URL si el backend está en otro host

# 3. Instalar dependencias (ya instaladas)
pnpm install

# 4. Iniciar dev server
pnpm dev
```

El frontend queda en `http://localhost:5173`.

---

## 🔌 Endpoints de la API

### Autenticación (públicos)

#### `POST /api/auth/register`

**Body:**
```json
{
  "username": "camil",
  "password": "mipass123"
}
```

**Respuestas:**
- `201 Created` → `{ "mensaje": "Usuario registrado correctamente..." }`
- `400 Bad Request` → Error de validación (username muy corto, password muy corta)
- `409 Conflict` → `{ "mensaje": "El nombre de usuario 'camil' ya está en uso." }`

---

#### `POST /api/auth/login`

**Body:**
```json
{
  "username": "camil",
  "password": "mipass123"
}
```

**Respuestas:**
- `200 OK` → `{ "token": "eyJhbGc...", "tipo": "Bearer", "username": "camil" }`
- `401 Unauthorized` → Credenciales incorrectas

---

### Rutas protegidas (requieren `Authorization: Bearer <token>`)

> Las rutas de notas se agregarán en la próxima fase del proyecto.

---

## 🔒 Seguridad — Buenas Prácticas Implementadas

- ✅ Contraseñas encriptadas con **BCrypt** (factor de coste por defecto: 10)
- ✅ API stateless sin sesiones HTTP (SessionCreationPolicy.STATELESS)
- ✅ CSRF deshabilitado (correcto para APIs REST con JWT)
- ✅ CORS configurado explícitamente (solo orígenes conocidos)
- ✅ Credenciales de DB y JWT secret en **variables de entorno** (nunca hardcodeadas)
- ✅ Errores 401 devueltos en JSON (no páginas HTML de Spring por defecto)
- ✅ Validación de entrada con Bean Validation en DTOs
- ✅ HikariCP limitado a 3 conexiones (plan gratuito de MySQL)

---

## 🐳 Docker — Backend Multi-Stage

### ¿Qué es un build multi-stage y por qué usarlo?

Un **Dockerfile multi-stage** divide el proceso en dos (o más) etapas independientes dentro del mismo archivo:

```
┌─────────────────────────────────────────────┐
│  ETAPA 1: build (maven:3.9-eclipse-temurin-21) │
│                                             │
│  • Tiene Maven + JDK completo               │
│  • Descarga dependencias                    │
│  • Compila el código fuente                 │
│  • Genera backend-0.0.1-SNAPSHOT.jar        │
│                                             │
│  ❌ Esta imagen NO va a producción          │
└─────────────────────┬───────────────────────┘
                      │  COPY --from=build *.jar
                      ▼
┌─────────────────────────────────────────────┐
│  ETAPA 2: runtime (eclipse-temurin:21-jre-alpine) │
│                                             │
│  • Solo tiene el JRE (sin Maven, sin JDK)   │
│  • Pesa ~85 MB (vs ~500 MB con JDK)         │
│  • Usuario no-root (seguridad)              │
│  • Variables de entorno en runtime          │
│                                             │
│  ✅ Esta imagen SÍ se despliega             │
└─────────────────────────────────────────────┘
```

**Ventajas clave:**
- La imagen final **no contiene** el código fuente ni Maven → menos superficie de ataque.
- Es hasta **6x más pequeña** que un Dockerfile de una sola etapa.
- Las credenciales (`.env`) **nunca entran** a la imagen gracias al `.dockerignore`.

### Comandos para construir y probar localmente (VS Code Terminal)

> Ejecuta todos estos comandos desde la carpeta raíz del monorepo (`bloc_notas/`).

#### 1. Construir la imagen

```powershell
# El contexto de build es la carpeta /backend
docker build -t bloc-notas-backend:local ./backend
```

Puedes ver las dos etapas ejecutándose en la terminal. La primera vez tarda ~2-3 min descargando dependencias; los rebuilds son mucho más rápidos gracias al caché de capas.

#### 2. Verificar que la imagen se creó y su tamaño

```powershell
docker images bloc-notas-backend
# Deberías ver algo como:
# REPOSITORY            TAG     SIZE
# bloc-notas-backend    local   ~180 MB
```

#### 3. Ejecutar el contenedor con las variables de entorno

```powershell
# Pasa las mismas variables que usas en tu .env local
docker run --rm -p 8080:8080 `
  -e DB_HOST="bszmqngodks6lmrzi146-mysql.services.clever-cloud.com" `
  -e DB_PORT="3306" `
  -e DB_NAME="tu_nombre_db" `
  -e DB_USER="tu_usuario" `
  -e DB_PASS="tu_contraseña" `
  -e JWT_SECRET="tu_jwt_secret_base64" `
  -e JWT_EXPIRATION_MS="86400000" `
  bloc-notas-backend:local
```

Alternativamente, puedes pasar tu archivo `.env` directamente:

```powershell
docker run --rm -p 8080:8080 --env-file ./backend/.env bloc-notas-backend:local
```

#### 4. Probar que el backend responde

```powershell
# En otra terminal (o usa Postman / Thunder Client)
curl -X POST http://localhost:8080/api/auth/login `
  -H "Content-Type: application/json" `
  -d '{"username":"test","password":"test123"}'

# Respuesta esperada: 401 (si el usuario no existe) o 200 con JWT
```

#### 5. Detener el contenedor

```powershell
# Si lo ejecutaste sin --rm, usa:
docker ps                          # ver el ID del contenedor
docker stop <container_id>         # detenerlo
```

### Estructura de archivos Docker

| Archivo | Ubicación | Propósito |
|---|---|---|
| [Dockerfile](file:///c:/Users/camil/Downloads/bloc_notas/backend/Dockerfile) | `backend/Dockerfile` | Define las 2 etapas de build |
| [.dockerignore](file:///c:/Users/camil/Downloads/bloc_notas/backend/.dockerignore) | `backend/.dockerignore` | Excluye `target/`, `.env`, `.idea/`, etc. |

### Despliegue en Render / Koyeb

Cuando conectes tu repositorio a Render o Koyeb:
1. Apunta el **Root Directory** a `backend/`.
2. Docker detectará automáticamente el `Dockerfile`.
3. Configura las **variables de entorno** en el panel de la plataforma (nunca en el código).
4. El puerto `8080` ya está expuesto en el `Dockerfile` (`EXPOSE 8080`).

---

## 📋 Próximos pasos sugeridos

- [ ] Implementar CRUD de notas (`Nota` entity, `NotaRepository`, `NotaController`)
- [ ] Agregar roles de usuario (ADMIN, USER) con `@PreAuthorize`
- [ ] Implementar refresh tokens para renovar el JWT sin reloguear
- [ ] Configurar HTTPS en producción
- [x] ~~Dockerizar el proyecto con `docker-compose.yml`~~ ✅ Hecho (Dockerfile multi-stage)
- [ ] Agregar `docker-compose.yml` para desarrollo local con MySQL en contenedor
- [ ] Agregar paginación a las notas

---

## 🛠️ Tecnologías utilizadas

| Capa | Tecnología |
|---|---|
| Backend framework | Spring Boot 4.1.0 |
| Seguridad | Spring Security 6 |
| ORM | Hibernate / Spring Data JPA |
| Base de datos | MySQL 8+ (Clever Cloud) |
| Pool de conexiones | HikariCP |
| JWT | JJWT 0.12.6 |
| Frontend framework | React 19 + TypeScript |
| Bundler | Vite 8 |
| Enrutado | React Router DOM 7 |
| HTTP client | Axios 1.x |
| Estilos | CSS Modules (Vanilla CSS) |
| Fuente tipográfica | Inter (Google Fonts) |
| Contenedorización | Docker (multi-stage build) |
| Base imagen runtime | Eclipse Temurin 21 JRE Alpine |
| Despliegue backend | Render / Koyeb (Docker) |
| Despliegue frontend | Vercel |
