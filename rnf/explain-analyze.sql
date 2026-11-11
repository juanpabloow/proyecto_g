-- =====================================================================
--  Tiempos de las consultas SQL principales (Módulo 7 — RNF)
--
--  Mide con EXPLAIN (ANALYZE, BUFFERS) el SQL equivalente al que genera
--  Hibernate para las dos consultas de lectura de DAC. ANALYZE ejecuta la
--  consulta de verdad, pero ambas son SELECT: no modifican nada.
--
--  Preparar los datos (5.000 contratos ficticios, ver rnf/k6-cargar-datos.js):
--    set -a; source .env; set +a
--    k6 run -e CONTRATOS=5000 rnf/k6-cargar-datos.js
--
--  Correr:
--    docker exec -i dac_db psql -U dac_user -d dac < rnf/explain-analyze.sql
--
--  Cada consulta se ejecuta 3 veces: la 1.ª calienta la caché de PostgreSQL
--  y las otras dos muestran el tiempo estable.
-- =====================================================================

\pset pager off
SELECT count(*) AS contratos_en_bd FROM contrato;

-- ---------------------------------------------------------------------
-- 1. ContratoRepo.findAllConFetch()
--    JPQL: SELECT c FROM ContratoJpa c JOIN FETCH c.entidad
--          JOIN FETCH c.contratista JOIN FETCH c.funcionario ORDER BY c.id
--    Se usa al empezar cada carga (índice en memoria) y en GET /api/alertas.
-- ---------------------------------------------------------------------
\echo '=== 1. findAllConFetch (corrida 1 de 3) ==='
EXPLAIN (ANALYZE, BUFFERS)
SELECT c.id_contrato, c.numero_contrato, c.monto, c.fecha,
       e.id_entidad, e.nombre, ct.id_contratista, ct.nombre, f.id_funcionario, f.nombre
FROM contrato c
JOIN entidad e      ON e.id_entidad = c.id_entidad
JOIN contratista ct ON ct.id_contratista = c.id_contratista
JOIN funcionario f  ON f.id_funcionario = c.id_funcionario
ORDER BY c.id_contrato;

\echo '=== 1. findAllConFetch (corrida 2 de 3) ==='
EXPLAIN (ANALYZE, BUFFERS)
SELECT c.id_contrato, c.numero_contrato, c.monto, c.fecha,
       e.id_entidad, e.nombre, ct.id_contratista, ct.nombre, f.id_funcionario, f.nombre
FROM contrato c
JOIN entidad e      ON e.id_entidad = c.id_entidad
JOIN contratista ct ON ct.id_contratista = c.id_contratista
JOIN funcionario f  ON f.id_funcionario = c.id_funcionario
ORDER BY c.id_contrato;

\echo '=== 1. findAllConFetch (corrida 3 de 3) ==='
EXPLAIN (ANALYZE, BUFFERS)
SELECT c.id_contrato, c.numero_contrato, c.monto, c.fecha,
       e.id_entidad, e.nombre, ct.id_contratista, ct.nombre, f.id_funcionario, f.nombre
FROM contrato c
JOIN entidad e      ON e.id_entidad = c.id_entidad
JOIN contratista ct ON ct.id_contratista = c.id_contratista
JOIN funcionario f  ON f.id_funcionario = c.id_funcionario
ORDER BY c.id_contrato;

-- ---------------------------------------------------------------------
-- 2. ContratoRepo.findByNumeroContratoAndEntidad(numero, entidad)
--    Búsqueda por clave natural (R-2), con parámetros como los envía JPA.
--    Debe usar el índice único uq_contrato_clave_natural.
-- ---------------------------------------------------------------------
SELECT numero_contrato AS numero, id_entidad AS entidad
FROM contrato WHERE numero_contrato = 'K6-02500' \gset

PREPARE por_clave_natural(varchar, bigint) AS
SELECT c.id_contrato, c.numero_contrato, c.id_entidad, c.id_contratista,
       c.id_funcionario, c.monto, c.fecha
FROM contrato c
WHERE c.numero_contrato = $1 AND c.id_entidad = $2;

\echo '=== 2. clave natural (corrida 1 de 3) ==='
EXPLAIN (ANALYZE, BUFFERS) EXECUTE por_clave_natural(:'numero', :entidad);
\echo '=== 2. clave natural (corrida 2 de 3) ==='
EXPLAIN (ANALYZE, BUFFERS) EXECUTE por_clave_natural(:'numero', :entidad);
\echo '=== 2. clave natural (corrida 3 de 3) ==='
EXPLAIN (ANALYZE, BUFFERS) EXECUTE por_clave_natural(:'numero', :entidad);

DEALLOCATE por_clave_natural;
