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

## R-6 — Filas incompletas: se descartan y se reportan **[Implementada en Java — pendiente en Python]**

Una fila sin `contratista`, sin `funcionario`, sin `numero_contrato` o
sin `entidad` no se convierte en contrato: se descarta, y la carga
informa **cuántas** filas se descartaron y **por qué**, indicando el
número de fila. El resto del archivo se analiza igual.

- **Qué cuenta como vacío:** vacío o solo espacios. Ojo: eso **no** es
  normalizar nombres — los valores se guardan tal como vienen en el
  archivo (R-5, [Decisión 9](decisiones-tecnicas.md)).
- **Qué no descarta la fila:** `monto` y `fecha` vacíos. La señal no los
  usa (R-8) y descartar por ellos perdería reincidencias reales.
- **Por qué se descartan también las filas sin clave natural:** sin
  `numero_contrato` + `entidad` no se puede saber si el contrato ya
  estaba cargado (R-2), que es el problema duro de la entrega.
- **Decisión de negocio:** [Decisión 12](decisiones-tecnicas.md), tomada
  por el equipo de forma **provisional** y pendiente de confirmación con
  S-1.

> ⚠️ **Solo en el módulo Java.** En `src/contratos.py` sigue el
> comportamiento anterior, verificado: dos contratos con `contratista` y
> `funcionario` vacíos producen una alerta del par `("", "")` — una
> alerta sobre nadie. La duplicación es deliberada y está registrada en
> la [Decisión 13](decisiones-tecnicas.md).

- Código: `java/src/dac/CargadorDeContratos.java` → `CAMPOS_OBLIGATORIOS`
- Pruebas: `java/test/dac/pruebas/PruebasFilasIncompletas.java`
- Historia: [HU-04](historias-usuario.md)

## R-7 — Los datos reales no entran al repositorio **[Implementada]**

Los dos CSV de `data/` son **ficticios** y sí se versionan: sin ellos, ni
`python3 main.py` ni la demostración de HU-04 funcionan en un clon nuevo.

| Archivo | Para qué |
|---|---|
| `contratos_ejemplo.csv` | El flujo feliz: 5 contratos limpios, 1 reincidencia |
| `contratos_incompletos_ejemplo.csv` | Filas incompletas y un duplicado: sirve para mostrar HU-04 y el defecto que corrige |

Cualquier archivo con datos reales va en `data/privado/`, que está en
`.gitignore`.

Motivado por los stakeholders S-7 (TI/seguridad) y S-8 (protección de
datos).

## R-8 — `monto` y `fecha` se tratan como texto **[Parcial]**

En el MVP se leen del CSV como cadenas y solo se muestran; no se suman,
no se comparan ni se validan. Alcanza para la señal de reincidencia
(R-3, que no usa monto ni fecha), pero **no** alcanzará para
fraccionamiento ([HU-03](historias-usuario.md)), que necesita sumar
montos y comparar fechas dentro de una ventana.

- **Trabajo futuro:** convertir y validar tipos al cargar.
