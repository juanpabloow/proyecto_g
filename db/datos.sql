-- Datos de ejemplo basados en data/contratos_ejemplo.csv

-- 1. Entidades
INSERT INTO entidad (nombre) VALUES ('Alcaldía de Ejemplo');
INSERT INTO entidad (nombre) VALUES ('Gobernación Ejemplo');

-- 2. Contratistas
INSERT INTO contratista (nombre) VALUES ('ACME SAS');
INSERT INTO contratista (nombre) VALUES ('Otra Empresa Ltda');
INSERT INTO contratista (nombre) VALUES ('Constructora XYZ');

-- 3. Funcionarios
INSERT INTO funcionario (nombre) VALUES ('Juan Pérez');
INSERT INTO funcionario (nombre) VALUES ('Ana Ruiz');
INSERT INTO funcionario (nombre) VALUES ('Carlos Gómez');

-- 4. Contratos
INSERT INTO contrato (numero_contrato, id_entidad, id_contratista, id_funcionario, monto, fecha)
VALUES ('001', 1, 1, 1, 50000000, '2026-01-15');

INSERT INTO contrato (numero_contrato, id_entidad, id_contratista, id_funcionario, monto, fecha)
VALUES ('002', 1, 2, 2, 30000000, '2026-02-10');

INSERT INTO contrato (numero_contrato, id_entidad, id_contratista, id_funcionario, monto, fecha)
VALUES ('003', 2, 1, 1, 45000000, '2026-03-05');

INSERT INTO contrato (numero_contrato, id_entidad, id_contratista, id_funcionario, monto, fecha)
VALUES ('004', 1, 3, 3, 80000000, '2026-03-20');

INSERT INTO contrato (numero_contrato, id_entidad, id_contratista, id_funcionario, monto, fecha)
VALUES ('005', 2, 1, 1, 60000000, '2026-04-01');

-- 5. Alerta: ACME SAS + Juan Pérez → REINCIDENCIA
INSERT INTO alerta (tipo, id_contratista, id_funcionario)
VALUES ('REINCIDENCIA', 1, 1);

-- 6. Evidencia de la alerta
INSERT INTO alerta_contrato (id_alerta, id_contrato) VALUES (1, 1);
INSERT INTO alerta_contrato (id_alerta, id_contrato) VALUES (1, 3);
INSERT INTO alerta_contrato (id_alerta, id_contrato) VALUES (1, 5);
