# Visión del producto

## Problema
Los analistas de investigación e inteligencia detectan redes de soborno o
extorsión en la contratación pública de forma manual y demorada, cruzando
datos en hojas de cálculo sin trazabilidad.

## Solución propuesta
Un sistema que recibe los datos de los contratos (entidad, contratista,
funcionario, monto, fecha) y avisa automáticamente cuando un contratista
se repite con el mismo funcionario en distintos contratos, mostrando con
qué contratos exactos se detectó esa repetición.

## Alcance de esta entrega (MVP)
Una sola señal de alerta: **reincidencia contratista-funcionario**.

Quedan fuera de esta entrega, documentados como trabajo futuro:
- Detección de fraccionamiento de contratos.
- Tablero visual de presentación.
- Integración con el sistema electrónico de contratación pública (SECOP).

## Usuarios objetivo
Analistas de investigación e inteligencia que hoy hacen este cruce a mano.

## Valor
Reduce el tiempo de detección, deja un registro ordenado y trazable de
los hallazgos, y elimina el cruce manual en Excel.
