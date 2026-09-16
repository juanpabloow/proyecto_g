-- =====================================================================
--  DAC - Detector de Alertas de Contratacion
--  Modelo fisico - PostgreSQL 12+
--
--  Traduccion directa del modelo entidad-relacion de docs/esquema-bd.md:
--  las mismas seis tablas, sin columnas adicionales.
--
--  Uso:  createdb dac
--        psql -d dac -f db/schema.sql
-- =====================================================================

BEGIN;

-- Permite volver a ejecutar el script sobre una base ya creada.
DROP TABLE IF EXISTS alerta_contrato CASCADE;
DROP TABLE IF EXISTS alerta          CASCADE;
DROP TABLE IF EXISTS contrato        CASCADE;
DROP TABLE IF EXISTS entidad         CASCADE;
DROP TABLE IF EXISTS contratista     CASCADE;
DROP TABLE IF EXISTS funcionario     CASCADE;


-- ---------------------------------------------------------------------
--  1. Sujetos del dominio
-- ---------------------------------------------------------------------

-- Organismo publico que contrata (alcaldia, gobernacion, ministerio).
CREATE TABLE entidad (
    id_entidad BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre     VARCHAR(200) NOT NULL,
    nit        VARCHAR(20),

    CONSTRAINT uq_entidad_nombre UNIQUE (nombre),
    -- El NIT queda opcional a proposito: el CSV de contratos no lo trae.
    -- Mientras tanto, la entidad se identifica por su nombre.
    CONSTRAINT uq_entidad_nit UNIQUE (nit)
);

-- Persona o empresa que recibe el contrato.
CREATE TABLE contratista (
    id_contratista BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre         VARCHAR(200) NOT NULL,

    -- La comparacion de nombres es exacta (R-5): "ACME SAS" y "acme sas"
    -- son dos contratistas distintos para el sistema. Cuando se resuelva
    -- HU-05, esta restriccion pasara al nombre normalizado.
    CONSTRAINT uq_contratista_nombre UNIQUE (nombre)
);

-- Servidor publico asociado a la adjudicacion.
CREATE TABLE funcionario (
    id_funcionario BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre         VARCHAR(200) NOT NULL,

    CONSTRAINT uq_funcionario_nombre UNIQUE (nombre)
);


-- ---------------------------------------------------------------------
--  2. Contrato
-- ---------------------------------------------------------------------

CREATE TABLE contrato (
    id_contrato     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    numero_contrato VARCHAR(50)   NOT NULL,
    id_entidad      BIGINT        NOT NULL,
    id_contratista  BIGINT        NOT NULL,
    id_funcionario  BIGINT        NOT NULL,
    -- monto y fecha admiten NULL a proposito: R-6 dice que una fila con
    -- monto o fecha vacios NO se descarta, porque la senal de reincidencia
    -- no los usa (R-3, R-8) y descartarla perderia reincidencias reales.
    -- El modelo logico (docs/esquema-bd.md) tampoco los marca obligatorios.
    monto           NUMERIC(15,2),
    fecha           DATE,

    -- >>> El problema duro, convertido en restriccion <<<
    -- La identidad de un contrato es numero_contrato + entidad (R-2).
    -- Cargar dos veces el mismo contrato ya no depende de que el programa
    -- se acuerde de revisar: lo rechaza la base de datos.
    -- El orden (id_entidad, numero_contrato) no es casual: el indice que
    -- crea esta restriccion sirve tambien para la llave foranea a entidad.
    CONSTRAINT uq_contrato_clave_natural UNIQUE (id_entidad, numero_contrato),

    CONSTRAINT fk_contrato_entidad FOREIGN KEY (id_entidad)
        REFERENCES entidad (id_entidad) ON DELETE RESTRICT,
    CONSTRAINT fk_contrato_contratista FOREIGN KEY (id_contratista)
        REFERENCES contratista (id_contratista) ON DELETE RESTRICT,
    CONSTRAINT fk_contrato_funcionario FOREIGN KEY (id_funcionario)
        REFERENCES funcionario (id_funcionario) ON DELETE RESTRICT,

    -- No hay una regla de negocio validada sobre el monto; solo se
    -- rechaza un valor negativo (R-8).
    CONSTRAINT ck_contrato_monto CHECK (monto >= 0)
);

-- El par (contratista, funcionario) es exactamente el agrupamiento de la
-- senal de reincidencia (R-3). Este indice lo cubre y, de paso, cubre la
-- llave foranea a contratista.
CREATE INDEX idx_contrato_par ON contrato (id_contratista, id_funcionario);
CREATE INDEX idx_contrato_funcionario ON contrato (id_funcionario);


-- ---------------------------------------------------------------------
--  3. Alertas y su evidencia
-- ---------------------------------------------------------------------

-- Una alerta senala un par contratista-funcionario. No expresa
-- culpabilidad ni puntaje: es una hipotesis a verificar (R-4).
CREATE TABLE alerta (
    id_alerta      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tipo           VARCHAR(30) NOT NULL,
    id_contratista BIGINT      NOT NULL,
    id_funcionario BIGINT      NOT NULL,
    generada_en    TIMESTAMPTZ NOT NULL DEFAULT now(),

    -- Una sola alerta por par y tipo: volver a correr la deteccion sobre
    -- los mismos datos no duplica alertas. Es la misma idea del problema
    -- duro, aplicada al resultado.
    CONSTRAINT uq_alerta_par UNIQUE (id_contratista, id_funcionario, tipo),

    -- Hoy existe una sola senal. Agregar fraccionamiento (HU-03) es
    -- agregar su valor a esta lista.
    CONSTRAINT ck_alerta_tipo CHECK (tipo IN ('REINCIDENCIA')),

    CONSTRAINT fk_alerta_contratista FOREIGN KEY (id_contratista)
        REFERENCES contratista (id_contratista) ON DELETE RESTRICT,
    CONSTRAINT fk_alerta_funcionario FOREIGN KEY (id_funcionario)
        REFERENCES funcionario (id_funcionario) ON DELETE RESTRICT
);

CREATE INDEX idx_alerta_funcionario ON alerta (id_funcionario);

-- Evidencia: que contratos originaron cada alerta. Resuelve la relacion
-- N:M entre alerta y contrato. Sin filas aqui, la alerta no vale nada.
CREATE TABLE alerta_contrato (
    id_alerta   BIGINT NOT NULL,
    id_contrato BIGINT NOT NULL,

    CONSTRAINT pk_alerta_contrato PRIMARY KEY (id_alerta, id_contrato),

    -- Si se borra la alerta, su evidencia se va con ella.
    CONSTRAINT fk_alerta_contrato_alerta FOREIGN KEY (id_alerta)
        REFERENCES alerta (id_alerta) ON DELETE CASCADE,
    -- Un contrato que sustenta una alerta no se puede borrar.
    CONSTRAINT fk_alerta_contrato_contrato FOREIGN KEY (id_contrato)
        REFERENCES contrato (id_contrato) ON DELETE RESTRICT
);

CREATE INDEX idx_alerta_contrato_contrato ON alerta_contrato (id_contrato);

-- Nota: la regla "toda alerta trae 2 o mas contratos" (HU-02) no se puede
-- escribir como restriccion de columna, porque depende de contar filas de
-- otra tabla. Hoy la garantiza la aplicacion; en la base requeriria un
-- trigger.

COMMIT;
