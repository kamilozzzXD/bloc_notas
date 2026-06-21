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

## 📋 Próximos pasos sugeridos

- [ ] Implementar CRUD de notas (`Nota` entity, `NotaRepository`, `NotaController`)
- [ ] Agregar roles de usuario (ADMIN, USER) con `@PreAuthorize`
- [ ] Implementar refresh tokens para renovar el JWT sin reloguear
- [ ] Configurar HTTPS en producción
- [ ] Dockerizar el proyecto con `docker-compose.yml`
- [ ] Agregar paginación a las notas

---

## 🛠️ Tecnologías utilizadas

| Capa | Tecnología |
|---|---|
| Backend framework | Spring Boot 4.1.0 |
| Seguridad | Spring Security 6 |
| ORM | Hibernate / Spring Data JPA |
| Base de datos | MySQL 8+ |
| Pool de conexiones | HikariCP |
| JWT | JJWT 0.12.6 |
| Frontend framework | React 19 + TypeScript |
| Bundler | Vite 8 |
| Enrutado | React Router DOM 7 |
| HTTP client | Axios 1.x |
| Estilos | CSS Modules (Vanilla CSS) |
| Fuente tipográfica | Inter (Google Fonts) |
