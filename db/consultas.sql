-- =====================================================================
--  DAC - Detector de Alertas de Contratacion
--  Consultas de ejemplo
--
--  Uso: psql -U dac_user -d dac -f db/consultas.sql
--       o desde el contenedor:
--       docker exec -it dac_db psql -U dac_user -d dac -f db/consultas.sql
-- =====================================================================

-- Ver entidades
SELECT * FROM entidad;

-- Ver contratistas
SELECT * FROM contratista;

-- Ver funcionarios
SELECT * FROM funcionario;

-- Ver contratos con nombres
SELECT c.numero_contrato, e.nombre AS entidad, ct.nombre AS contratista,
       f.nombre AS funcionario, c.monto, c.fecha
FROM contrato c
JOIN entidad e      ON e.id_entidad      = c.id_entidad
JOIN contratista ct ON ct.id_contratista = c.id_contratista
JOIN funcionario f  ON f.id_funcionario  = c.id_funcionario;

-- Ver alertas con evidencia
SELECT a.tipo, ct.nombre AS contratista, f.nombre AS funcionario,
       c.numero_contrato, c.monto, c.fecha
FROM alerta a
JOIN contratista ct     ON ct.id_contratista = a.id_contratista
JOIN funcionario f      ON f.id_funcionario  = a.id_funcionario
JOIN alerta_contrato ac ON ac.id_alerta      = a.id_alerta
JOIN contrato c         ON c.id_contrato     = ac.id_contrato;
