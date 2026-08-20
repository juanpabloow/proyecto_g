# Reglas de negocio y glosario

Este documento existe porque la lógica de negocio estaba **implícita en
el código**: había que leer `src/` para saber qué cuenta como alerta y
qué pasa con un dato raro. Aquí queda explícita, con lo que el sistema
hace hoy y lo que todavía no hace.

Cada regla dice **[Implementada]**, **[Parcial]** o **[Pendiente]**.

## Glosario

| Término | Significado en DAC |
|---|---|
| **Contrato** | Una fila del CSV: un acuerdo entre una entidad y un contratista, gestionado por un funcionario |
| **Entidad** | Organismo público que contrata (alcaldía, gobernación, ministerio) |
| **Contratista** | Persona o empresa que recibe el contrato |
| **Funcionario** | Servidor público que aparece asociado a la adjudicación del contrato |
| **Clave natural** | El par de campos que identifica un contrato de forma única: `numero_contrato` + `entidad` |
| **Señal de alerta** | Regla que marca un patrón sospechoso. El MVP tiene una: reincidencia |
| **Reincidencia** | El mismo contratista aparece con el mismo funcionario en más de un contrato |
| **Alerta** | Resultado de una señal: el par señalado **más** los contratos que la originaron |
| **Evidencia** | La lista de contratos dentro de una alerta. Sin evidencia no hay alerta |
| **Idempotencia** | Cargar el mismo contrato dos veces produce el mismo resultado que cargarlo una vez |
| **Fraccionamiento** | Partir un contrato grande en varios pequeños para evitar un proceso de mayor cuantía (señal futura) |

## R-1 — Un contrato válido tiene seis campos **[Implementada]**

`numero_contrato`, `entidad`, `contratista`, `funcionario`, `monto`,
`fecha`. Si al CSV le falta cualquiera de esas columnas, la carga falla
con un error que nombra las columnas faltantes. Columnas *adicionales*
se aceptan y se conservan.

- Código: `src/contratos.py` → `COLUMNAS_REQUERIDAS`
- Prueba: `tests/test_contratos.py::test_falla_si_faltan_columnas`

## R-2 — La identidad de un contrato es `numero_contrato` + `entidad` **[Implementada]**

Ni `numero_contrato` solo (dos entidades pueden usar el mismo
consecutivo "001") ni la fila completa (un cambio de monto no lo
convierte en otro contrato).

- Código: `src/contratos.py` → `clave = (fila["numero_contrato"], fila["entidad"])`
- Detalle en [problema-duro.md](problema-duro.md)

## R-3 — Reincidencia: el mismo par en más de un contrato **[Implementada]**

Se agrupan los contratos por el par (`contratista`, `funcionario`). Si un
par aparece en **2 o más** contratos, se genera **una** alerta para ese
par, con todos sus contratos como evidencia.

Precisiones que antes no estaban escritas:

- **El umbral es 2.** No hay parámetro configurable.
- **No hay ventana de tiempo.** Dos contratos separados por cinco años
  cuentan igual que dos del mismo mes.
- **Cruza entidades.** Si el par se repite en una alcaldía y en una
  gobernación, sigue siendo una sola alerta (así se comporta el dato de
  ejemplo: `ACME SAS` + `Juan Pérez` en dos entidades distintas).
- **El monto no influye.** Dos contratos de 1.000 pesos generan la misma
  alerta que dos de mil millones.

> ⚠️ **Supuesto sin validar.** Nadie del negocio (S-1) ha confirmado que
> el umbral correcto sea 2 y sin ventana de tiempo. Es la primera
> pregunta a llevar a un analista real: *¿"más de una vez" o "más de N
> veces en un periodo"?* Cambiar esto cambia la regla, no el código
> alrededor.

- Código: `src/alertas.py` → `detectar_reincidencias`
- Prueba: `tests/test_alertas.py`

## R-4 — Una alerta describe coincidencias, no culpabilidad **[Implementada por diseño]**

El sistema no calcula puntajes, rankings ni probabilidades sobre
personas. Solo dice *"este par se repite, aquí están los contratos"*. La
interpretación es del analista. Ver [visión §4](vision-producto.md) y
el stakeholder S-6 en [stakeholders.md](stakeholders.md).

## R-5 — La comparación de nombres es **exacta** **[Parcial — limitación conocida]**

Hoy `"ACME SAS"`, `"acme sas"` y `"ACME S.A.S."` son tres contratistas
distintos para el sistema, y `" Juan Pérez"` con espacio inicial es otro
funcionario. Comportamiento verificado: un CSV con las mismas dos
personas escritas en minúscula **no** genera alerta.

- **Riesgo:** falsos negativos. Es la limitación más grande del MVP.
- **Por qué no se resolvió:** normalizar (mayúsculas, tildes, sufijos
  societarios, alias) son reglas de negocio que necesitan validación con
  un analista; inventarlas sería peor que dejar la limitación visible.
- **Trabajo futuro:** [HU-05](historias-usuario.md).

## R-6 — Filas incompletas: hoy se aceptan **[Pendiente — genera falsos positivos]**

Verificado: dos contratos con `contratista` y `funcionario` **vacíos**
producen una alerta del par `("", "")`. Es decir, una alerta sobre
nadie.

- **Decisión que falta:** ¿la fila incompleta se descarta, se reporta
  como error de calidad de datos, o invalida el archivo completo? Es una
  decisión del negocio (S-1), no técnica, y por eso no se resolvió por
  cuenta propia.
- **Trabajo futuro:** [HU-04](historias-usuario.md) — prioridad más
  alta del backlog pendiente.

## R-7 — Los datos reales no entran al repositorio **[Implementada]**

`data/contratos_ejemplo.csv` es un archivo **ficticio** y sí se versiona:
sin él, `python3 main.py` no funciona en un clon nuevo. Cualquier archivo
con datos reales va en `data/privado/`, que está en `.gitignore`.

Motivado por los stakeholders S-7 (TI/seguridad) y S-8 (protección de
datos).

## R-8 — `monto` y `fecha` se tratan como texto **[Parcial]**

En el MVP se leen del CSV como cadenas y solo se muestran; no se suman,
no se comparan ni se validan. Alcanza para la señal de reincidencia
(R-3, que no usa monto ni fecha), pero **no** alcanzará para
fraccionamiento ([HU-03](historias-usuario.md)), que necesita sumar
montos y comparar fechas dentro de una ventana.

- **Trabajo futuro:** convertir y validar tipos al cargar.
