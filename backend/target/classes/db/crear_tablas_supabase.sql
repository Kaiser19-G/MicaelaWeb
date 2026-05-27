-- ==============================================================================
--  SCRIPT DE CREACIÓN DE TABLAS – I.E. Micaela Bastidas Puyocahua
--  Ejecutar en: Supabase Dashboard → SQL Editor → New Query → Run
--  Compatible con PostgreSQL 15+ (Supabase)
-- ==============================================================================

-- ── 1. USUARIOS (tabla base de autenticación) ─────────────────────────────────
CREATE TABLE IF NOT EXISTS usuarios (
    id               BIGSERIAL PRIMARY KEY,
    username         VARCHAR(50)  NOT NULL UNIQUE,
    password         VARCHAR(255) NOT NULL,
    email            VARCHAR(100) UNIQUE,
    rol              VARCHAR(20)  NOT NULL CHECK (rol IN ('DIRECTOR','DOCENTE','ALUMNO','ADMIN')),
    activo           BOOLEAN      NOT NULL DEFAULT TRUE,
    primer_login     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- ── 2. DOCENTES ───────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS docentes (
    id               BIGSERIAL PRIMARY KEY,
    codigo_docente   VARCHAR(12)  NOT NULL UNIQUE,
    dni              VARCHAR(8)   NOT NULL UNIQUE,
    apellido_paterno VARCHAR(60)  NOT NULL,
    apellido_materno VARCHAR(60)  NOT NULL,
    nombres          VARCHAR(100) NOT NULL,
    especialidad     VARCHAR(100),
    condicion        VARCHAR(20)  NOT NULL DEFAULT 'NOMBRADO' CHECK (condicion IN ('NOMBRADO','CONTRATADO')),
    email_institucional VARCHAR(100),
    celular          VARCHAR(15),
    usuario_id       BIGINT REFERENCES usuarios(id),
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- ── 3. AULAS ──────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS aulas (
    id                   BIGSERIAL PRIMARY KEY,
    grado                VARCHAR(10)  NOT NULL,
    seccion              VARCHAR(5)   NOT NULL,
    nivel                VARCHAR(15)  NOT NULL CHECK (nivel IN ('PRIMARIA','SECUNDARIA')),
    anio_academico       INTEGER      NOT NULL,
    aula_referencia      VARCHAR(20),
    docente_principal_id BIGINT REFERENCES docentes(id),
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- ── 4. ALUMNOS ────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS alumnos (
    id                      BIGSERIAL PRIMARY KEY,
    codigo_estudiante        VARCHAR(12)  NOT NULL UNIQUE,
    dni                     VARCHAR(8)   NOT NULL UNIQUE,
    apellido_paterno         VARCHAR(60)  NOT NULL,
    apellido_materno         VARCHAR(60)  NOT NULL,
    nombres                 VARCHAR(100) NOT NULL,
    fecha_nacimiento         DATE,
    sexo                    VARCHAR(10)  CHECK (sexo IN ('MASCULINO','FEMENINO')),
    aula_id                 BIGINT REFERENCES aulas(id),
    tutor_id                BIGINT REFERENCES docentes(id),
    anio_academico          INTEGER,
    estado_matricula        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVO'
                            CHECK (estado_matricula IN ('ACTIVO','RETIRADO','TRASLADADO','EGRESADO')),
    -- Permiso de Academia Pre-Universitaria
    tiene_permiso_academia  BOOLEAN      NOT NULL DEFAULT FALSE,
    hora_entrada_academia   VARCHAR(10),  -- '13:30' o '14:30'
    -- Apoderado
    nombre_apoderado        VARCHAR(150),
    celular_apoderado       VARCHAR(15),
    email_apoderado         VARCHAR(100),
    created_at              TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_alumno_dni     ON alumnos(dni);
CREATE INDEX IF NOT EXISTS idx_alumno_codigo  ON alumnos(codigo_estudiante);

-- ── 5. ASISTENCIAS ────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS asistencias (
    id                          BIGSERIAL PRIMARY KEY,
    alumno_id                   BIGINT NOT NULL REFERENCES alumnos(id),
    aula_id                     BIGINT REFERENCES aulas(id),
    docente_id                  BIGINT REFERENCES docentes(id),
    fecha                       DATE   NOT NULL,
    estado                      VARCHAR(20) NOT NULL DEFAULT 'ASISTIO'
                                CHECK (estado IN ('ASISTIO','FALTA','TARDANZA','LICENCIA','PERMISO_ACADEMIA','JUSTIFICADO')),
    hora_llegada                TIME,
    hora_entrada_turno          TIME,
    minutos_tardanza            INTEGER,
    -- Permiso de academia
    aplicado_permiso_academia   BOOLEAN NOT NULL DEFAULT FALSE,
    hora_permiso_academia       VARCHAR(10),
    -- Justificación
    justificacion               VARCHAR(500),
    tiene_justificacion         BOOLEAN NOT NULL DEFAULT FALSE,
    url_evidencia_justificacion TEXT,
    -- Sync offline
    sincronizado_offline        BOOLEAN NOT NULL DEFAULT FALSE,
    timestamp_registro_local    TIMESTAMPTZ,
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    -- Restricción: un alumno solo puede tener un registro por día
    UNIQUE (alumno_id, fecha)
);

CREATE INDEX IF NOT EXISTS idx_asistencia_alumno_fecha ON asistencias(alumno_id, fecha);
CREATE INDEX IF NOT EXISTS idx_asistencia_fecha        ON asistencias(fecha);
CREATE INDEX IF NOT EXISTS idx_asistencia_docente      ON asistencias(docente_id);

-- ── 6. NOTAS ──────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS notas (
    id                   BIGSERIAL PRIMARY KEY,
    alumno_id            BIGINT      NOT NULL REFERENCES alumnos(id),
    docente_id           BIGINT      NOT NULL REFERENCES docentes(id),
    aula_id              BIGINT      REFERENCES aulas(id),
    area_curricular      VARCHAR(100) NOT NULL,
    competencia          VARCHAR(200),
    periodo_academico    VARCHAR(10)  NOT NULL, -- 'B1','B2','B3','B4'
    anio_academico       INTEGER     NOT NULL,
    calificacion_numerica NUMERIC(4,1),
    calificacion_literal VARCHAR(5)  CHECK (calificacion_literal IN ('AD','A','B','C')),
    observaciones        VARCHAR(500),
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    -- Un alumno solo tiene una nota por área/período/año
    UNIQUE (alumno_id, area_curricular, periodo_academico, anio_academico)
);

CREATE INDEX IF NOT EXISTS idx_nota_alumno   ON notas(alumno_id);
CREATE INDEX IF NOT EXISTS idx_nota_periodo  ON notas(alumno_id, periodo_academico, anio_academico);

-- ── 7. EVIDENCIAS ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS evidencias (
    id               BIGSERIAL PRIMARY KEY,
    nota_id          BIGINT REFERENCES notas(id) ON DELETE CASCADE,
    alumno_id        BIGINT NOT NULL REFERENCES alumnos(id),
    docente_id       BIGINT NOT NULL REFERENCES docentes(id),
    nombre_archivo   VARCHAR(255) NOT NULL,
    nombre_original  VARCHAR(255),
    ruta_storage     TEXT        NOT NULL,
    url_publica      TEXT,
    tipo_contenido   VARCHAR(100),
    tamano_bytes     BIGINT,
    tipo_evidencia   VARCHAR(30)  NOT NULL DEFAULT 'EXAMEN'
                     CHECK (tipo_evidencia IN ('EXAMEN','PRACTICA','TRABAJO_GRUPAL','EXPOSICION','PROYECTO','TAREA','OTRO')),
    descripcion      VARCHAR(300),
    fecha_evaluacion DATE,
    periodo_academico VARCHAR(10),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_evidencia_nota    ON evidencias(nota_id);
CREATE INDEX IF NOT EXISTS idx_evidencia_alumno  ON evidencias(alumno_id);
CREATE INDEX IF NOT EXISTS idx_evidencia_docente ON evidencias(docente_id);

-- ── 8. EXPEDIENTE DE DOCUMENTOS (Matrícula Digital) ──────────────────────────
CREATE TABLE IF NOT EXISTS expediente_documentos (
    id                          BIGSERIAL PRIMARY KEY,
    alumno_id                   BIGINT NOT NULL REFERENCES alumnos(id),
    tipo_documento              VARCHAR(40) NOT NULL
                                CHECK (tipo_documento IN ('DNI','PARTIDA_NACIMIENTO','CERTIFICADO_ESTUDIOS',
                                'LIBRETA_NOTAS_ANTERIOR','FOTO_CARNET','CONSTANCIA_SALUD','FICHA_MATRICULA','OTRO')),
    nombre_archivo              VARCHAR(255) NOT NULL,
    ruta_storage                TEXT        NOT NULL,
    url_publica                 TEXT,
    tipo_contenido              VARCHAR(100),
    tamano_bytes                BIGINT,
    estado_verificacion         VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE'
                                CHECK (estado_verificacion IN ('PENDIENTE','VERIFICADO','RECHAZADO','REQUIERE_RESUBIDA')),
    observaciones_verificacion  VARCHAR(300),
    anio_matricula              INTEGER,
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ── 9. USUARIO ADMIN INICIAL (para poder hacer login) ────────────────────────
-- Contraseña: admin123  (BCrypt hash — cámbiala en producción)
INSERT INTO usuarios (username, password, email, rol, activo, primer_login)
VALUES (
    'admin',
    '$2a$12$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lVWy',
    'admin@bastidas.edu.pe',
    'ADMIN',
    TRUE,
    TRUE
)
ON CONFLICT (username) DO NOTHING;

-- ── Verificación final ────────────────────────────────────────────────────────
SELECT
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS tamaño
FROM pg_tables
WHERE schemaname = 'public'
ORDER BY tablename;
