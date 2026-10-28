# Historias de usuario

## Cómo escribimos las historias

Cada historia tiene: **rol** (quién), **necesidad** (qué) y **beneficio**
(para qué) — nunca una solución técnica disfrazada de historia. Los
criterios de aceptación se escriben en formato **Dado / Cuando /
Entonces**, para que cada criterio se pueda convertir directamente en una
prueba automatizada.

Prioridad en **MoSCoW** (*Must / Should / Could / Won't*) y estimación en
puntos relativos (1 = trivial, 8 = requiere investigación).

## Backlog

| ID | Historia | Prioridad | Puntos | Estado | Pruebas |
|---|---|---|---|---|---|
| [HU-01](#hu-01--cargar-contratos-desde-un-archivo-csv) | Cargar contratos desde un archivo CSV | **Must** | 3 | ✅ Hecha | `CargadorDeContratosTest` |
| [HU-02](#hu-02--detectar-reincidencia-contratista-funcionario-historia-central) | Detectar reincidencia contratista–funcionario | **Must** | 5 | ✅ Hecha | `DetectorDeReincidenciasTest` |
| [HU-04](#hu-04--no-generar-alertas-a-partir-de-filas-incompletas) | No generar alertas a partir de filas incompletas | Should | 2 | ✅ Hecha | `FilasIncompletasTest` |
| [HU-05](#hu-05--reconocer-el-mismo-nombre-escrito-de-formas-distintas) | Reconocer el mismo nombre escrito distinto | Should | 5 | ✅ Hecha *(regla provisional)* | `NombresVariantesTest` |
| [HU-06](#hu-06--saber-cuándo-un-duplicado-traía-datos-distintos) | Saber cuándo un duplicado traía datos distintos | Could | 3 | ✅ Hecha | `ConflictosDeDuplicadosTest` |
| [HU-07](#hu-07--conservar-los-contratos-entre-ejecuciones) | Conservar los contratos entre ejecuciones | Should | 8 | ✅ Hecha | `ContratoE2ETest` |
| [HU-03](#hu-03--detectar-fraccionamiento-de-contratos) | Detectar fraccionamiento de contratos | Could | 8 | ✅ Hecha *(regla provisional)* | `DetectorDeFraccionamientoTest` |
| [HU-08](#hu-08--exportar-el-reporte-de-alertas) | Exportar el reporte de alertas | Could | 3 | ✅ Hecha | `ExportadorDeAlertasTest` |

**8 de 8 historias hechas.** Cada una tiene sus pruebas de dominio y,
además, una prueba de punta a punta por HTTP real en `ContratoE2ETest` o
`AlertasE2ETest`. Todas corren en la integración continua
(`.github/workflows/ci.yml`) en cada Pull Request.

Dos historias quedan con una **regla de negocio provisional**: HU-05
(qué diferencias de escritura se ignoran, [Decisión 19](decisiones-tecnicas.md))
y HU-03 (tope y ventana de días, [Decisión 20](decisiones-tecnicas.md)).
Funcionan y están probadas, pero sus parámetros los propuso el equipo y
falta confirmarlos con un analista real (S-1). Se registra como deuda en
[deuda-tecnica.md](deuda-tecnica.md).

Los roles citados (S-1, S-2, S-3…) son los de
[stakeholders.md](stakeholders.md).

---

## HU-01 — Cargar contratos desde un archivo CSV

> **Como** analista de investigación (S-1),
> **quiero** cargar un archivo CSV con los contratos (entidad,
> contratista, funcionario, monto y fecha),
> **para** tener los datos listos para analizar sin cruzarlos a mano.

**Criterios de aceptación**

1. **Dado** un CSV con las seis columnas requeridas, **cuando** lo cargo,
   **entonces** obtengo un contrato por cada fila.
   → `test_carga_contratos_validos`
2. **Dado** un CSV al que le falta una columna requerida, **cuando** lo
   cargo, **entonces** falla con un error que **nombra** la columna
   faltante (no un error genérico).
   → `test_falla_si_faltan_columnas`
3. **Dado** un archivo vacío o sin encabezado, **cuando** lo cargo,
   **entonces** falla con un mensaje claro (no con un error interno).
   → `test_falla_si_el_archivo_esta_vacio`
4. **Dado** un CSV con la misma clave de contrato repetida, **cuando** lo
   cargo, **entonces** el contrato aparece una sola vez.
   → `test_ignora_contratos_duplicados`

**Reglas de negocio aplicadas:** [R-1](reglas-de-negocio.md),
[R-2](reglas-de-negocio.md), [R-8](reglas-de-negocio.md).
**Código:** `java/src/main/java/dac/dominio/CargadorDeContratos.java` ·
**Pruebas:** `CargadorDeContratosTest` (5 casos de carga)

---

## HU-02 — Detectar reincidencia contratista–funcionario *(historia central)*

> **Como** analista de investigación e inteligencia (S-1),
> **quiero** que el sistema me marque cuando un mismo contratista
> aparece con un mismo funcionario en más de un contrato, **y me muestre
> exactamente cuáles contratos lo originaron**,
> **para** decidir en segundos si vale la pena abrir una investigación,
> en vez de reconstruir el cruce a mano en Excel.

### Contexto de negocio

Es la señal más citada como punto de partida en una investigación de
contratación: el mismo funcionario adjudicando repetidamente al mismo
contratista. **No prueba nada por sí sola** — puede ser un proveedor
legítimo especializado. Por eso la historia no termina en "avisar": la
alerta solo sirve si trae la evidencia, porque lo que el analista hace a
continuación es verificarla una por una
([visión §4](vision-producto.md)).

### Criterios de aceptación

1. **Dado** un conjunto de contratos donde el par (contratista,
   funcionario) aparece en 2 o más contratos,
   **cuando** ejecuto la detección,
   **entonces** se genera **una** alerta para ese par.
   → `test_detecta_reincidencia_simple`

2. **Dado** un conjunto donde ningún par se repite,
   **cuando** ejecuto la detección,
   **entonces** no se genera ninguna alerta.
   → `test_no_genera_alerta_si_no_hay_repeticion`

3. **Dado** un par que aparece en 3 contratos,
   **cuando** ejecuto la detección,
   **entonces** obtengo **una sola** alerta con los **3** contratos como
   evidencia (una alerta por par, no una por contrato).
   → `test_agrupa_todos_los_contratos_del_par_en_una_sola_alerta`

4. **Dado** un mismo contratista con **dos funcionarios distintos**,
   **cuando** ejecuto la detección,
   **entonces** no se genera alerta: la señal es el **par**, no el
   contratista por sí solo.
   → `test_mismo_contratista_con_funcionarios_distintos_no_alerta`

5. **Dado** cualquier resultado de la detección,
   **cuando** reviso las alertas,
   **entonces** **toda** alerta trae al menos 2 contratos de evidencia.
   *Una alerta sin evidencia es un defecto, no una alerta.*
   → `test_toda_alerta_trae_su_evidencia`

6. **Dado** un contrato que viene duplicado en el archivo,
   **cuando** cargo y ejecuto la detección,
   **entonces** **no** se genera alerta: un contrato contado dos veces no
   es una reincidencia (éste es el
   [problema duro](problema-duro.md)).
   → `test_duplicado_no_genera_alerta_falsa`

7. **Dado** un análisis terminado, **cuando** veo la salida, **entonces**
   cada alerta muestra, por contrato, su número, entidad, fecha y monto
   —lo mínimo para poder buscar el expediente físico— (S-2, S-3).
   → `DetectorDeReincidenciasTest` → *la evidencia trae número, entidad, fecha y monto*

### Fuera del alcance de esta historia

- Umbral configurable y ventana de tiempo ([R-3](reglas-de-negocio.md)).
- Nombres escritos de forma distinta ([HU-05](#hu-05--reconocer-el-mismo-nombre-escrito-de-formas-distintas)).
- Priorizar u ordenar alertas por monto o por gravedad.

### Definition of Done (cumplida)

- [x] Los 6 criterios automatizables tienen prueba y pasan.
- [x] La regla quedó escrita en [R-3](reglas-de-negocio.md), con su
      supuesto sin validar señalado.
- [x] Funciona de punta a punta: `POST /api/contratos/cargar` devuelve la
      alerta con su evidencia, y queda guardada en la base.
- [x] Cualquier integrante del equipo puede explicar cómo se genera una
      alerta.

**Código:** `java/src/main/java/dac/dominio/DetectorDeReincidencias.java` ·
**Pruebas:** `DetectorDeReincidenciasTest` (8 casos, uno por criterio)

---

## HU-04 — No generar alertas a partir de filas incompletas

> **Como** analista (S-1), **quiero** que las filas sin contratista o sin
> funcionario no produzcan alertas, **para** no perder tiempo revisando
> una alerta "sobre nadie".

**Criterios de aceptación**

1. **Dado** un CSV con dos filas cuyo contratista y funcionario están
   vacíos, **cuando** ejecuto el análisis, **entonces** no se genera
   ninguna alerta.
   → `criterio 1: dos filas sin contratista ni funcionario no generan alerta`
2. **Dado** un CSV con filas incompletas, **cuando** lo cargo,
   **entonces** el sistema me informa cuántas filas se descartaron y por
   qué.
   → `criterio 2: informa cuántas filas se descartaron y por qué`

### La decisión que la desbloqueó

La pregunta abierta era: ¿la fila incompleta **se descarta**, **se
reporta** o **invalida el archivo completo**? Se resolvió así
([Decisión 12](decisiones-tecnicas.md)):

> **La fila se descarta y se reporta. El archivo sigue siendo válido.**

- **Aceptarla** era el defecto: produce una alerta del par `("", "")`,
  una alerta sobre nadie ([R-6](reglas-de-negocio.md)).
- **Invalidar el archivo** deja al analista sin nada por una fila mala
  entre cinco mil. El costo cae sobre quien no cometió el error.
- **Descartar en silencio** esconde un problema de calidad de datos — por
  eso el criterio 2 ya pedía informar cuántas y por qué. La propia
  historia venía respondiendo la pregunta a medias.

**Es provisional.** Sigue siendo una decisión de S-1: el equipo la tomó
para no dejar la historia bloqueada, es una excepción explícita a la
[Decisión 11](decisiones-tecnicas.md), y revertirla es cambiar una lista
(`CAMPOS_OBLIGATORIOS`), no reescribir la carga.

### Qué se descarta exactamente

| Campo vacío | ¿Descarta la fila? | Por qué |
|---|---|---|
| `contratista`, `funcionario` | Sí | Son el par sobre el que se alerta: vacíos generan una alerta sobre nadie |
| `numero_contrato`, `entidad` | Sí | Son la clave natural: sin ella no se puede saber si el contrato ya estaba cargado ([R-2](reglas-de-negocio.md)) |
| `monto`, `fecha` | No | La señal no los usa ([R-8](reglas-de-negocio.md)); descartar por ellos perdería reincidencias reales |

Una celda con solo espacios cuenta como vacía. Los valores que sí se
cargan **no se normalizan**: decidir que `" Juan Pérez"` y `"Juan Pérez"`
son la misma persona es [HU-05](#hu-05--reconocer-el-mismo-nombre-escrito-de-formas-distintas)
y necesita validación de S-1 ([R-5](reglas-de-negocio.md)).

### Definition of Done

- [x] Los 2 criterios tienen prueba y pasan.
- [x] La regla quedó escrita en [R-6](reglas-de-negocio.md), con la
      decisión de negocio marcada como provisional.
- [x] Funciona de punta a punta: `POST /api/contratos/cargar` con un CSV
      que traiga filas incompletas.
- [ ] **Confirmar la Decisión 12 con un analista real (S-1).**

**Código:** `java/src/main/java/dac/dominio/CargadorDeContratos.java` ·
**Pruebas:** `FilasIncompletasTest` (7 casos) y, de punta a punta,
`ContratoE2ETest` → `carga_filas_incompletas_sin_invalidar_el_archivo`

---

## HU-05 — Reconocer el mismo nombre escrito de formas distintas

> **Como** analista (S-1), **quiero** que "ACME SAS", "acme sas" y
> "ACME S.A.S." se reconozcan como el mismo contratista, **para** no
> perder reincidencias reales por una diferencia de escritura.

**Criterios de aceptación**

1. **Dado** dos contratos del mismo par escritos con distinta
   capitalización, espacios sobrantes, tildes o puntos, **cuando** ejecuto
   la detección, **entonces** se genera la alerta.
   → `NombresVariantesTest` → *capitalización o espacios sobrantes generan la alerta*,
   *tildes y puntos de la razón social no separan al contratista*
2. **Dado** una normalización aplicada, **cuando** veo la alerta,
   **entonces** se muestran los nombres **originales**, no los
   normalizados (el analista debe reconocer el dato de la fuente).
   → *la alerta muestra los nombres originales, no los normalizados*
3. **Dado** dos empresas con distinto sufijo societario ("ACME SAS" y
   "ACME LTDA") o apellidos que solo difieren en la ñ ("Peña" y "Pena"),
   **cuando** ejecuto la detección, **entonces** **no** se unen.
   → *sufijos societarios distintos siguen siendo empresas distintas*,
   *la ñ se conserva*

**Regla aplicada ([Decisión 19](decisiones-tecnicas.md), provisional):** se
ignoran mayúsculas, espacios sobrantes, tildes, puntos y comas. **No** se
quitan sufijos societarios ni se usan alias, porque unir de más señala a
quien no es (S-6). La normalización solo se usa para comparar; en la base
y en pantalla queda el nombre original.

**Código:** `java/src/main/java/dac/dominio/NormalizadorDeNombres.java` ·
**De punta a punta:** `AlertasE2ETest` → *nombres escritos distinto forman una sola alerta*
con `data/contratos_nombres_variantes_ejemplo.csv`.

---

## HU-06 — Saber cuándo un duplicado traía datos distintos

> **Como** analista (S-1), **quiero** enterarme cuando dos filas con el
> mismo contrato traen montos, fechas o personas distintas, **para**
> decidir cuál es la versión correcta en vez de perder la corrección en
> silencio.

**Criterios de aceptación**

1. **Dado** dos filas con la misma clave y montos distintos, **cuando**
   cargo el archivo, **entonces** el contrato se carga una sola vez
   **y** el conflicto queda reportado con su número de fila.
   → `ConflictosDeDuplicadosTest` → *misma clave con montos distintos se carga una vez y reporta el conflicto*
2. **Dado** un duplicado idéntico, **cuando** lo cargo, **entonces** se
   ignora sin reportar conflicto.
   → *un duplicado idéntico no es un conflicto*
3. **Dado** un duplicado que difiere en varios campos, **cuando** lo
   cargo, **entonces** el conflicto nombra **todos** los campos distintos.
   → *el conflicto nombra todos los campos que difieren*
4. **Dado** un contrato que ya está en la base, **cuando** llega en un
   archivo nuevo con otro monto, **entonces** no se duplica y el conflicto
   se reporta contra el "contrato ya guardado en la base".
   → `AlertasE2ETest` → *un contrato ya guardado que llega con otro monto se reporta como conflicto*

Sigue ganando la primera versión ([Decisión 6](decisiones-tecnicas.md));
lo nuevo es que la diferencia ya no se pierde. La pantalla los muestra en
la pestaña **Conflictos**.

**Código:** `CargadorDeContratos.camposDistintos` y `ContratoServicio.cargarYDetectar`.

---

## HU-07 — Conservar los contratos entre ejecuciones

> **Como** analista (S-1), **quiero** que el sistema recuerde los
> contratos ya cargados de una sesión a otra, **para** poder cargar
> archivos nuevos sin que se repita lo que ya analicé.

**Criterios de aceptación**

1. **Dado** un contrato cargado en una ejecución anterior, **cuando**
   vuelvo a cargarlo, **entonces** no se duplica ni genera alerta nueva.
   → `ContratoE2ETest` → `e2e_carga_persiste_y_es_idempotente`
2. **Dado** dos archivos que comparten contratos, **cuando** cargo ambos,
   **entonces** cada contrato existe una sola vez.
   → `AlertasE2ETest` → *nombres escritos distinto forman una sola alerta, persistida una vez*

Es el paso que convierte la idempotencia "dentro de una carga" en
idempotencia real ([problema duro §5](problema-duro.md)). La clave natural
es una restricción de la base (`uq_contrato_clave_natural` en
[db/schema.sql](../db/schema.sql)), y `uq_alerta_par` impide que las
alertas se dupliquen.

---

## HU-03 — Detectar fraccionamiento de contratos

> **Como** analista (S-1), **quiero** que el sistema detecte contratos
> pequeños divididos a propósito para evitar un proceso de mayor cuantía,
> **para** identificar otra forma común de evadir controles.

**Respuestas a las preguntas abiertas ([Decisión 20](decisiones-tecnicas.md), provisional):**

| Pregunta | Respuesta del equipo | Dónde se cambia |
|---|---|---|
| ¿Por qué se agrupa? | Mismo **contratista** (normalizado, HU-05) en la misma **entidad** | `Contrato.contratistaEnEntidad()` |
| ¿Qué ventana de tiempo? | **30 días** entre el primer y el último contrato | `dac.fraccionamiento.ventana-dias` |
| ¿Qué umbral de monto? | Cada contrato **por debajo** del tope y la suma **alcanza o supera** el tope de **$100.000.000** | `dac.fraccionamiento.tope` |

**Criterios de aceptación**

1. **Dado** 2 o más contratos del mismo contratista y entidad, cada uno
   bajo el tope, dentro de la ventana, que suman el tope, **cuando**
   ejecuto la detección, **entonces** se genera **una** alerta con todos
   ellos como evidencia y la suma.
   → `DetectorDeFraccionamientoTest` → *contratos pequeños … generan alerta*
2. **Dado** contratos que no suman el tope, **entonces** no hay alerta.
   → *si no suman el tope, no hay alerta*
3. **Dado** contratos separados por más días que la ventana, **entonces**
   no se suman. → *contratos fuera de la ventana de días no se suman*
4. **Dado** contratos en entidades distintas, **entonces** no hay alerta.
   → *en entidades distintas no es fraccionamiento*
5. **Dado** un contrato que ya supera el tope, **entonces** no cuenta como
   fracción. → *un contrato que ya supera el tope no cuenta como fracción*
6. **Dado** un contrato sin monto o sin fecha, **entonces** no participa
   (R-8). → *contratos sin monto o sin fecha no participan*

**Código:** `DetectorDeFraccionamiento`, `AlertaFraccionamiento` ·
**De punta a punta:** `AlertasE2ETest` con `data/contratos_fraccionamiento_ejemplo.csv`.
La pantalla los muestra en la pestaña **Fraccionamiento**.

**Limitación declarada:** estas alertas se calculan en cada consulta y
**no se guardan** en la tabla `alerta` (ver [deuda-tecnica.md](deuda-tecnica.md), DT-01).

---

## HU-08 — Exportar el reporte de alertas

> **Como** coordinador del área (S-2), **quiero** exportar las alertas a
> un archivo, **para** compartirlas con el equipo de investigación
> (S-3) sin tener que copiar la salida de la consola.

**Criterios de aceptación**

1. **Dado** alertas en la base, **cuando** exporto, **entonces** obtengo
   un CSV con **una fila por cada contrato de evidencia** (tipo, número de
   alerta, contratista, funcionario, entidad, número, monto, fecha y
   detalle). → `ExportadorDeAlertasTest` → *una fila por contrato de evidencia*
2. **Dado** alertas de fraccionamiento, **entonces** también se exportan,
   con la suma y el tope. → *incluye también las alertas de fraccionamiento*
3. **Dado** que no hay alertas, **entonces** el archivo trae solo el
   encabezado. → *sin alertas, el archivo trae solo el encabezado*
4. **Dado** que el archivo se abre en Excel, **entonces** las tildes se
   ven bien (UTF-8 con BOM, separador `;`). → *empieza con BOM*
5. **Dado** un nombre que empieza por `=`, `+`, `-` o `@`, **entonces** se
   exporta neutralizado para que Excel no lo ejecute como fórmula
   (seguridad). → *una celda que parece fórmula se neutraliza*

**Endpoint:** `GET /api/alertas/exportar` (descarga `alertas_dac.csv`) y
botón **Exportar reporte (CSV)** en la pantalla. ·
**De punta a punta:** `AlertasE2ETest` → *exporta un CSV descargable*.

---

## Definition of Ready / Definition of Done

Una historia está **lista para trabajarse** cuando: tiene rol, necesidad
y beneficio; sus criterios están en Dado/Cuando/Entonces; no depende de
una decisión de negocio sin resolver; y se puede terminar dentro de una
iteración.

Una historia está **terminada** cuando: cada criterio automatizable tiene
una prueba que pasa; la regla de negocio quedó escrita en
[reglas-de-negocio.md](reglas-de-negocio.md); funciona de punta a punta;
y cualquier integrante puede explicarla sin ayuda de la IA
([uso-ia.md](uso-ia.md)).
