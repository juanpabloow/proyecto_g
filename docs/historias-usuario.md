# Historias de usuario

## HU-01 — Cargar contratos
**Como** analista, **quiero** cargar un archivo de contratos (CSV) con
entidad, contratista, funcionario, monto y fecha, **para** tener los
datos listos para análisis sin cruzarlos a mano.

**Criterios de aceptación:**
- El sistema lee un archivo CSV con las columnas requeridas.
- Si el archivo tiene columnas faltantes, muestra un error claro.
- Los contratos ya cargados (misma clave) no se vuelven a duplicar.

## HU-02 — Detectar reincidencia contratista-funcionario
**Como** analista, **quiero** que el sistema marque cuando un contratista
se repite con el mismo funcionario en distintos contratos, **para**
detectar posibles redes de forma automática.

**Criterios de aceptación:**
- El sistema agrupa los contratos por par (contratista, funcionario).
- Si un par aparece en más de un contrato, se genera una alerta.
- La alerta muestra los contratos exactos que la originaron (número,
  fecha, monto).

## HU-03 — Detectar fraccionamiento (futuro)
**Como** analista, **quiero** que el sistema detecte contratos pequeños
divididos a propósito para evitar procesos de mayor cuantía, **para**
identificar otra forma común de evadir controles.

**Estado:** fuera del alcance de esta entrega, queda como trabajo futuro.
