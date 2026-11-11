# Backlog

## Cómo escribimos las historias

Cada historia tiene: **rol** (quién), **necesidad** (qué) y **beneficio**
(para qué), nunca una solución técnica disfrazada de historia. Los
criterios de aceptación se escriben en formato **Dado / Cuando /
Entonces**, y cada uno apunta a la **prueba Java que lo verifica**, con la
forma `Clase#método` (en `java/src/test/java/dac/`). La salida de
`./mvnw verify` muestra el `@DisplayName` de cada prueba.

Prioridad en **MoSCoW** (*Must / Should / Could / Won't*) y estimación en
puntos relativos (1 = trivial, 8 = requiere investigación). Los roles (S-1,
S-2, S-3…) son los de [stakeholders.md](stakeholders.md).

**Estados:**

- **Comprometida:** el equipo se comprometió a entregarla. Las 8 están
  comprometidas desde el backlog inicial; la columna dice cuándo se entregó,
  según el historial de Git (ver la trazabilidad).
- **Terminada:** cumple la Definition of Done (al final del documento).
- **Pendiente:** lo que todavía falta, aunque la historia esté terminada
  (por ejemplo, validar una regla con un analista real).

## Resumen

| ID | Historia | Prioridad | Puntos | Entregada (según el historial) | Estado | Pendiente |
|---|---|---|---|---|---|---|
| [HU-01](#hu-01--cargar-contratos-desde-un-archivo-csv) | Cargar contratos desde un archivo CSV | **Must** | 3 | 18/08/2026 (Python); 09/09/2026 (Java) | ✅ Terminada | — |
| [HU-02](#hu-02--detectar-reincidencia-contratistafuncionario-historia-central) | Detectar reincidencia contratista–funcionario | **Must** | 5 | 18/08/2026 (Python); 09/09/2026 (Java) | ✅ Terminada | Confirmar con S-1 el umbral de 2 y la ausencia de ventana de tiempo ([R-3](reglas-de-negocio.md)) |
| [HU-04](#hu-04--no-generar-alertas-a-partir-de-filas-incompletas) | No generar alertas a partir de filas incompletas | Should | 2 | 09/09/2026 | ✅ Terminada | Confirmar la Decisión 12 con S-1 |
| [HU-07](#hu-07--conservar-los-contratos-entre-ejecuciones) | Conservar los contratos entre ejecuciones | Should | 8 | 09/09/2026 – 16/09/2026 (taller 3) | ✅ Terminada | Prueba automática de cargas simultáneas ([DT-09](deuda-tecnica.md)) |
| [HU-05](#hu-05--reconocer-el-mismo-nombre-escrito-de-formas-distintas) | Reconocer el mismo nombre escrito distinto | Should | 5 | 28/10/2026 (PR #5) | ✅ Terminada (regla provisional) | Validar las reglas de normalización con S-1 ([DT-02](deuda-tecnica.md)) |
| [HU-06](#hu-06--saber-cuándo-un-duplicado-traía-datos-distintos) | Saber cuándo un duplicado traía datos distintos | Could | 3 | 28/10/2026 (PR #5) | ✅ Terminada | Guardar los conflictos en la base ([DT-08](deuda-tecnica.md)) |
| [HU-03](#hu-03--detectar-fraccionamiento-de-contratos) | Detectar fraccionamiento de contratos | Could | 8 | 28/10/2026 (PR #5) | ✅ Terminada (regla provisional) | Validar tope y ventana con S-1 ([DT-02](deuda-tecnica.md)); guardar las alertas ([DT-01](deuda-tecnica.md)) |
| [HU-08](#hu-08--exportar-el-reporte-de-alertas) | Exportar el reporte de alertas | Could | 3 | 28/10/2026 (PR #5) | ✅ Terminada | — |

**8 de 8 historias terminadas.** Todas tienen pruebas de dominio y una
prueba de punta a punta por HTTP real contra PostgreSQL, y todas corren en
la integración continua (`.github/workflows/ci.yml`) en cada Pull Request.
Lo pendiente son decisiones de negocio que solo puede tomar un analista
real (S-1) y deuda declarada en [deuda-tecnica.md](deuda-tecnica.md).

---

## HU-01 — Cargar contratos desde un archivo CSV

> **Como** analista de investigación (S-1),
> **quiero** cargar un archivo CSV con los contratos (entidad,
> contratista, funcionario, monto y fecha),
> **para** tener los datos listos para analizar sin cruzarlos a mano.

| # | Criterio de aceptación | Prueba Java |
|---|---|---|
| 1 | **Dado** un CSV con las seis columnas requeridas, **cuando** lo cargo, **entonces** obtengo un contrato por cada fila | `CargadorDeContratosTest#carga_una_fila_por_contrato` |
| 2 | **Dado** un CSV al que le falta una columna requerida, **cuando** lo cargo, **entonces** falla con un error que **nombra** la columna faltante | `CargadorDeContratosTest#falla_nombrando_la_columna_faltante`; por HTTP: `ContratoE2ETest#rechaza_csv_sin_columnas_requeridas` |
| 3 | **Dado** un archivo vacío o sin encabezado, **cuando** lo cargo, **entonces** falla con un mensaje claro | `CargadorDeContratosTest#falla_si_el_archivo_esta_vacio`, `CargadorDeContratosTest#falla_si_no_hay_encabezado` |
| 4 | **Dado** un CSV con la misma clave de contrato repetida, **cuando** lo cargo, **entonces** el contrato aparece una sola vez | `CargadorDeContratosTest#deduplica_por_clave_natural` |

**Reglas:** [R-1](reglas-de-negocio.md), [R-2](reglas-de-negocio.md), [R-8](reglas-de-negocio.md).
**Código:** `dac.dominio.CargadorDeContratos`, `dac.dominio.LectorCSV`, `dac.api.ContratoControlador`.

---

## HU-02 — Detectar reincidencia contratista–funcionario *(historia central)*

> **Como** analista de investigación e inteligencia (S-1),
> **quiero** que el sistema me marque cuando un mismo contratista
> aparece con un mismo funcionario en más de un contrato, **y me muestre
> exactamente cuáles contratos lo originaron**,
> **para** decidir en segundos si vale la pena abrir una investigación,
> en vez de reconstruir el cruce a mano en Excel.

**Contexto de negocio.** Es la señal más citada como punto de partida en
una investigación de contratación. **No prueba nada por sí sola**: puede ser
un proveedor legítimo especializado. Por eso la alerta solo sirve si trae
la evidencia ([visión §4](vision-producto.md)).

| # | Criterio de aceptación | Prueba Java |
|---|---|---|
| 1 | **Dado** un par (contratista, funcionario) en 2 o más contratos, **cuando** ejecuto la detección, **entonces** se genera **una** alerta para ese par | `DetectorDeReincidenciasTest#un_par_repetido_genera_alerta` |
| 2 | **Dado** un conjunto donde ningún par se repite, **entonces** no se genera ninguna alerta | `DetectorDeReincidenciasTest#sin_repeticiones_no_hay_alertas` |
| 3 | **Dado** un par en 3 contratos, **entonces** hay **una sola** alerta con los **3** contratos como evidencia | `DetectorDeReincidenciasTest#tres_contratos_una_sola_alerta` |
| 4 | **Dado** un contratista con **dos funcionarios distintos**, **entonces** no hay alerta: la señal es el **par** | `DetectorDeReincidenciasTest#la_senal_es_el_par_no_el_contratista` |
| 5 | **Dado** cualquier resultado, **entonces** **toda** alerta trae al menos 2 contratos de evidencia | `DetectorDeReincidenciasTest#toda_alerta_trae_evidencia`, `DetectorDeReincidenciasTest#una_alerta_con_un_contrato_es_un_defecto` |
| 6 | **Dado** un contrato duplicado en el archivo, **cuando** cargo y detecto, **entonces** **no** hay alerta ([problema duro](problema-duro.md)) | `CargadorDeContratosTest#el_duplicado_no_genera_alerta_falsa` |
| 7 | **Dado** un análisis terminado, **entonces** cada contrato de la alerta muestra número, entidad, fecha y monto (S-2, S-3) | `DetectorDeReincidenciasTest#la_evidencia_trae_los_datos_del_contrato` |

**De punta a punta:** la alerta y su evidencia quedan guardadas en
`alerta` y `alerta_contrato`: `ContratoE2ETest#e2e_carga_persiste_y_es_idempotente`.
**Fuera del alcance:** umbral configurable y ventana de tiempo
([R-3](reglas-de-negocio.md)); ordenar alertas por gravedad (R-4).
**Código:** `dac.dominio.DetectorDeReincidencias`, `dac.dominio.Alerta`,
`dac.aplicacion.ContratoServicio` (guarda la alerta).

---

## HU-04 — No generar alertas a partir de filas incompletas

> **Como** analista (S-1), **quiero** que las filas sin contratista o sin
> funcionario no produzcan alertas, **para** no perder tiempo revisando
> una alerta "sobre nadie".

| # | Criterio de aceptación | Prueba Java |
|---|---|---|
| 1 | **Dado** un CSV con dos filas cuyo contratista y funcionario están vacíos, **cuando** ejecuto el análisis, **entonces** no se genera ninguna alerta | `FilasIncompletasTest#dos_filas_vacias_no_generan_alerta` |
| 2 | **Dado** un CSV con filas incompletas, **cuando** lo cargo, **entonces** el sistema me informa cuántas filas se descartaron y por qué | `FilasIncompletasTest#informa_cuantas_y_por_que`; por HTTP: `ContratoE2ETest#carga_filas_incompletas_sin_invalidar_el_archivo` |

**Decisión que la desbloqueó ([Decisión 12](decisiones-tecnicas.md), provisional):**
la fila se descarta y se reporta, y el archivo sigue siendo válido
(`FilasIncompletasTest#una_fila_mala_no_invalida_el_archivo`). Se descartan
las filas sin `contratista`, `funcionario`, `numero_contrato` o `entidad`;
`monto` y `fecha` vacíos **no** descartan la fila
(`FilasIncompletasTest#monto_y_fecha_vacios_no_descartan`).
**Regla:** [R-6](reglas-de-negocio.md). **Código:** `CargadorDeContratos.CAMPOS_OBLIGATORIOS`, `FilaDescartada`.

---

## HU-05 — Reconocer el mismo nombre escrito de formas distintas

> **Como** analista (S-1), **quiero** que "ACME SAS", "acme sas" y
> "ACME S.A.S." se reconozcan como el mismo contratista, **para** no
> perder reincidencias reales por una diferencia de escritura.

| # | Criterio de aceptación | Prueba Java |
|---|---|---|
| 1 | **Dado** dos contratos del mismo par escritos con distinta capitalización, espacios, tildes o puntos, **entonces** se genera la alerta | `NombresVariantesTest#capitalizacion_y_espacios_no_esconden_la_reincidencia`, `NombresVariantesTest#tildes_y_puntos_no_separan` |
| 2 | **Dado** una normalización aplicada, **cuando** veo la alerta, **entonces** se muestran los nombres **originales** | `NombresVariantesTest#la_evidencia_conserva_los_nombres_originales` |
| 3 | **Dado** "ACME SAS" y "ACME LTDA", o "Peña" y "Pena", **entonces** **no** se unen | `NombresVariantesTest#sufijos_distintos_no_se_unen`, `NombresVariantesTest#la_enie_se_conserva` |

**De punta a punta:** `AlertasE2ETest#nombres_variantes_por_http`.
**Regla ([Decisión 19](decisiones-tecnicas.md), provisional):** [R-5](reglas-de-negocio.md).
**Código:** `dac.dominio.NormalizadorDeNombres`, `Contrato.par()`.

---

## HU-06 — Saber cuándo un duplicado traía datos distintos

> **Como** analista (S-1), **quiero** enterarme cuando dos filas con el
> mismo contrato traen montos, fechas o personas distintas, **para**
> decidir cuál es la versión correcta en vez de perder la corrección en
> silencio.

| # | Criterio de aceptación | Prueba Java |
|---|---|---|
| 1 | **Dado** dos filas con la misma clave y montos distintos, **entonces** el contrato se carga una vez **y** el conflicto se reporta con su número de fila | `ConflictosDeDuplicadosTest#monto_distinto_se_reporta` |
| 2 | **Dado** un duplicado idéntico, **entonces** se ignora sin reportar conflicto | `ConflictosDeDuplicadosTest#duplicado_identico_no_es_conflicto` |
| 3 | **Dado** un duplicado que difiere en varios campos, **entonces** el conflicto nombra **todos** los campos distintos | `ConflictosDeDuplicadosTest#nombra_todos_los_campos` |
| 4 | **Dado** un contrato que ya está en la base, **cuando** llega en un archivo nuevo con otro monto, **entonces** no se duplica y el conflicto se reporta contra el "contrato ya guardado en la base" | `AlertasE2ETest#conflicto_contra_la_base` |

Sigue ganando la primera versión ([Decisión 6](decisiones-tecnicas.md)).
**Código:** `CargadorDeContratos.camposDistintos`, `ContratoServicio.cargarYDetectar`, `ConflictoDeDuplicado`.

---

## HU-07 — Conservar los contratos entre ejecuciones

> **Como** analista (S-1), **quiero** que el sistema recuerde los
> contratos ya cargados de una sesión a otra, **para** poder cargar
> archivos nuevos sin que se repita lo que ya analicé.

| # | Criterio de aceptación | Prueba Java |
|---|---|---|
| 1 | **Dado** un contrato cargado en una ejecución anterior, **cuando** vuelvo a cargarlo, **entonces** no se duplica ni genera alerta nueva | `ContratoE2ETest#e2e_carga_persiste_y_es_idempotente`; 10 recargas: `RequisitosNoFuncionalesTest#rnf2_integridad_ante_recargas` |
| 2 | **Dado** dos archivos distintos que comparten contratos, **cuando** cargo ambos, **entonces** cada contrato existe una sola vez | `AlertasE2ETest#conflicto_contra_la_base` (el segundo archivo repite el contrato 001: `contratosNuevos` = 0) |

La clave natural es una restricción de la base (`uq_contrato_clave_natural`
en [db/schema.sql](../db/schema.sql)) y `uq_alerta_par` impide duplicar
alertas. Detalle en [problema-duro.md](problema-duro.md).
**Código:** `dac.aplicacion.ContratoServicio`, `dac.persistencia.*`.

---

## HU-03 — Detectar fraccionamiento de contratos

> **Como** analista (S-1), **quiero** que el sistema detecte contratos
> pequeños divididos a propósito para evitar un proceso de mayor cuantía,
> **para** identificar otra forma común de evadir controles.

Regla ([Decisión 20](decisiones-tecnicas.md), provisional): mismo
contratista (normalizado) y entidad, 2 o más contratos cada uno **por
debajo** del tope, dentro de **30 días**, que juntos **alcanzan** el tope de
**$100.000.000** (`dac.fraccionamiento.*` en `application.properties`).

| # | Criterio de aceptación | Prueba Java |
|---|---|---|
| 1 | **Dado** 2+ contratos del mismo contratista y entidad, bajo el tope, en la ventana, que suman el tope, **entonces** hay **una** alerta con todos ellos y la suma | `DetectorDeFraccionamientoTest#contratos_pequenos_que_suman_el_tope_alertan` |
| 2 | **Dado** contratos que no suman el tope, **entonces** no hay alerta | `DetectorDeFraccionamientoTest#sin_alcanzar_el_tope_no_alerta` |
| 3 | **Dado** contratos separados por más días que la ventana, **entonces** no se suman | `DetectorDeFraccionamientoTest#fuera_de_la_ventana_no_se_suman` |
| 4 | **Dado** contratos en entidades distintas, **entonces** no hay alerta | `DetectorDeFraccionamientoTest#entidades_distintas_no_alertan` |
| 5 | **Dado** un contrato que ya supera el tope, **entonces** no cuenta como fracción | `DetectorDeFraccionamientoTest#contrato_grande_no_es_fraccion` |
| 6 | **Dado** un contrato sin monto o sin fecha, **entonces** no participa (R-8) | `DetectorDeFraccionamientoTest#sin_monto_o_fecha_no_participan` |

**De punta a punta:** `AlertasE2ETest#detecta_fraccionamiento_por_http`,
`AlertasE2ETest#fraccionamiento_respeta_ventana_y_entidad`.
**Limitación:** estas alertas se calculan en cada consulta y no se guardan
([DT-01](deuda-tecnica.md)).
**Código:** `dac.dominio.DetectorDeFraccionamiento`, `AlertaFraccionamiento`, `dac.aplicacion.ConfiguracionDeSenales`.

---

## HU-08 — Exportar el reporte de alertas

> **Como** coordinador del área (S-2), **quiero** exportar las alertas a
> un archivo, **para** compartirlas con el equipo de investigación (S-3)
> sin copiar la salida de la consola.

| # | Criterio de aceptación | Prueba Java |
|---|---|---|
| 1 | **Dado** alertas en la base, **cuando** exporto, **entonces** obtengo un CSV con **una fila por contrato de evidencia** | `ExportadorDeAlertasTest#una_fila_por_evidencia` |
| 2 | **Dado** alertas de fraccionamiento, **entonces** también se exportan, con la suma y el tope | `ExportadorDeAlertasTest#incluye_fraccionamiento` |
| 3 | **Dado** que no hay alertas, **entonces** el archivo trae solo el encabezado | `ExportadorDeAlertasTest#sin_alertas_solo_encabezado` |
| 4 | **Dado** que el archivo se abre en Excel, **entonces** las tildes se ven bien (UTF-8 con BOM, separador `;`) | `ExportadorDeAlertasTest#trae_bom` |
| 5 | **Dado** un nombre que empieza por `=`, `+`, `-` o `@`, **entonces** se exporta neutralizado (seguridad, R-10) | `ExportadorDeAlertasTest#neutraliza_formulas` |

**De punta a punta:** `AlertasE2ETest#exporta_el_reporte` (`GET /api/alertas/exportar`, descarga `alertas_dac.csv`).
**Código:** `dac.dominio.ExportadorDeAlertas`, `dac.api.AlertaControlador`.

---

## Trazabilidad: historia → código → prueba → commit → demostración

Cada historia, con las clases que la implementan, sus pruebas, el commit
donde se implementó (hash corto, comprobado con `git log`) y la
demostración que la muestra en vivo en
[demo-entrega-final.md](demo-entrega-final.md). Para ver un commit:
`git show --stat <hash>`.

| Historia | Clases (`java/src/main/java/dac/`) | Pruebas | Commit de implementación | Commit de las pruebas | Demostración |
|---|---|---|---|---|---|
| HU-01 Cargar CSV | `dominio/CargadorDeContratos`, `dominio/LectorCSV`, `api/ContratoControlador` | `CargadorDeContratosTest` | `b027f43` (09/09/2026, Java + Spring Boot); primera versión en Python: `6f20ee0` (18/08/2026) | `339fc23` (16/09/2026) | 2 |
| HU-02 Reincidencia | `dominio/DetectorDeReincidencias`, `dominio/Alerta`, `aplicacion/ContratoServicio` | `DetectorDeReincidenciasTest`, `ContratoE2ETest` | `b027f43` (09/09/2026); la alerta se guarda con su evidencia desde `383aab6` (16/09/2026); primera versión en Python: `6f20ee0` | `339fc23` (16/09/2026) | 3 |
| HU-03 Fraccionamiento | `dominio/DetectorDeFraccionamiento`, `dominio/AlertaFraccionamiento`, `aplicacion/ConfiguracionDeSenales` | `DetectorDeFraccionamientoTest`, `AlertasE2ETest` | `2c7f35d` (dominio) y `d1f4151` (aplicación), 28/10/2026 | `03f33fd` (28/10/2026) | 5 |
| HU-04 Filas incompletas | `dominio/CargadorDeContratos` (`CAMPOS_OBLIGATORIOS`), `dominio/FilaDescartada` | `FilasIncompletasTest`, `ContratoE2ETest` | `6f96cae` (09/09/2026, primera versión Java); en Spring Boot desde `b027f43` | `339fc23` (16/09/2026) | 6 |
| HU-05 Nombres variantes | `dominio/NormalizadorDeNombres`, `dominio/Contrato` (`par()`) | `NombresVariantesTest`, `AlertasE2ETest` | `2c7f35d` (28/10/2026) | `03f33fd` (28/10/2026) | 7 |
| HU-06 Conflictos | `dominio/ConflictoDeDuplicado`, `dominio/CargadorDeContratos` (`camposDistintos`), `aplicacion/ContratoServicio` | `ConflictosDeDuplicadosTest`, `AlertasE2ETest` | `2c7f35d` (dominio) y `d1f4151` (aplicación), 28/10/2026 | `03f33fd` (28/10/2026) | 8 |
| HU-07 Persistencia | `aplicacion/ContratoServicio`, `persistencia/ContratoJpa` y repositorios, `uq_contrato_clave_natural` | `ContratoE2ETest`, `RequisitosNoFuncionalesTest` | `b027f43` (09/09/2026) | `d6f39ad` (16/09/2026, como `ContratoServicioE2ETest`; renombrada a `ContratoE2ETest` en `9eb5b38`) | 4 y 9 |
| HU-08 Exportar | `dominio/ExportadorDeAlertas`, `api/AlertaControlador` | `ExportadorDeAlertasTest`, `AlertasE2ETest` | `2c7f35d` (dominio) y `e48a13f` (API), 28/10/2026 | `03f33fd` (28/10/2026) | 10 |

El código de cada historia siguió cambiando después (por ejemplo, el
rendimiento de la carga en el Módulo 7). El commit de la tabla es donde la
historia **empezó a funcionar**; `git log --follow <archivo>` muestra el
resto.

## Definition of Ready / Definition of Done

Una historia está **lista para trabajarse** cuando tiene rol, necesidad y
beneficio; sus criterios están en Dado/Cuando/Entonces; no depende de una
decisión de negocio sin resolver; y se puede terminar dentro de una
iteración.

Una historia está **terminada** cuando cada criterio tiene una prueba Java
que pasa en el CI; la regla de negocio quedó escrita en
[reglas-de-negocio.md](reglas-de-negocio.md); funciona de punta a punta
(HTTP real + PostgreSQL); y cualquier integrante puede explicarla sin ayuda
de la IA ([uso-ia.md](uso-ia.md)).
