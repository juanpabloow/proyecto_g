# Problema duro: idempotencia en la carga de contratos

## 1. Enunciado

> **Cargar el mismo contrato dos veces debe producir el mismo resultado
> que cargarlo una vez**, y la repetición debe quedar **registrada**.

Si una fila viene repetida en el archivo, si alguien reenvía el mismo CSV
o si dos archivos distintos comparten contratos, el sistema **no debe
contar el contrato dos veces ni crear una alerta nueva**, y el registro de
auditoría debe mostrar que la recarga ocurrió y no agregó nada.

## 2. La dificultad real

No es difícil por la técnica: es difícil porque **es el punto donde el
sistema se gana o se pierde la confianza del usuario**.

1. **Es el error más frecuente del flujo real.** Archivos reenviados,
   copias mal hechas en Excel, filas pegadas dos veces. No es un caso
   borde: es el día a día.
2. **Falla en silencio.** Un duplicado no produce un error; produce una
   *alerta que parece válida*.
3. **Inventa justo la señal que el sistema promete detectar.** El mismo
   contrato contado dos veces se ve **exactamente igual** que un
   contratista que repite con un funcionario: una reincidencia falsa.
4. **Un falso positivo cuesta más que un falso negativo.** Del otro lado de
   la alerta hay personas reales (S-6). Al analista le bastan dos alertas
   falsas para volver a Excel.

## 3. Qué podía fallar

| Falla posible | Consecuencia | Qué la impide hoy |
|---|---|---|
| Identificar el contrato por el número solo | Dos entidades con el consecutivo "001" se fusionan y se pierde un contrato legítimo | La clave natural incluye la entidad (§4) |
| Identificar el contrato por la fila completa | Un monto corregido pasa como contrato nuevo y crea una reincidencia falsa | Ídem |
| Una fila repetida dentro del mismo archivo | Contrato contado dos veces | Deduplicación al cargar (§5, capa 1) |
| Reenviar el mismo archivo otro día | Contratos duplicados en la base y alertas falsas | Índice de lo ya guardado (capa 2) y restricción única de la base (capa 3) |
| Que el programa "se olvide" de revisar (un error futuro en el código) | Duplicados en la base | La restricción `uq_contrato_clave_natural` lo impide igual (capa 3) |
| Dos cargas del mismo contrato al mismo tiempo | Las dos creen que el contrato no existe | La base rechaza la segunda; la transacción se deshace y responde 409 |
| Un archivo con un valor malo a mitad de camino | Una carga a medias: unos contratos guardados y otros no | Se valida todo **antes** de escribir y la carga es una sola transacción |
| La alerta se guarda dos veces al recargar | Alertas repetidas | `uq_alerta_par` (una alerta por par y tipo) |
| Una recarga que nadie puede comprobar después | No se puede auditar si hubo duplicación | Registro de auditoría con `nuevos` y `yaGuardados` (§5) |

## 4. Decisión: la clave natural

Cada contrato se identifica con **`numero_contrato` + `entidad`**
([Decisión 5](decisiones-tecnicas.md), [R-2](reglas-de-negocio.md)).

| Alternativa | Por qué se descartó |
|---|---|
| Solo `numero_contrato` | Dos entidades distintas pueden usar el mismo consecutivo ("001") |
| La fila completa (los seis campos) | Un monto corregido o una fecha reformateada crearían un "contrato nuevo" |
| Un id generado por el sistema | El duplicado llega desde afuera: hay que reconocerlo por lo que *ya trae* |

## 5. El mecanismo

Tres capas, de la más rápida a la más segura, y un registro:

1. **Dentro de la carga (dominio).** `CargadorDeContratos.cargarDesde`
   guarda la primera versión de cada `Contrato.claveNatural()`. Una fila con
   una clave ya vista se cuenta en `duplicadosIgnorados` y no llega a la
   detección de alertas. Si traía datos distintos, gana la primera
   ([Decisión 6](decisiones-tecnicas.md)) y la diferencia se reporta como
   conflicto (HU-06).
2. **Contra lo ya guardado (aplicación).** `ContratoServicio.cargarYDetectar`
   lee una sola vez lo que hay en la base (`ContratoRepo.findAllConFetch`) y
   arma un índice por clave natural. Solo inserta los contratos que no están
   en el índice y cuenta los demás en `contratosYaGuardados`. Todo ocurre en
   una transacción (`@Transactional`): montos y fechas se validan antes de
   escribir, y un valor malo rechaza el archivo sin dejar nada a medias.
3. **La base de datos.** `uq_contrato_clave_natural UNIQUE (id_entidad,
   numero_contrato)` en [db/schema.sql](../db/schema.sql) hace **imposible**
   guardar dos veces el mismo contrato, aunque el código se equivocara o dos
   cargas llegaran a la vez (la segunda recibe 409). `uq_alerta_par` hace lo
   mismo con las alertas.

**Registro auditable.** Cada carga aceptada deja una línea en el registro
`dac.auditoria` (`dac.seguridad.Auditoria.cargaAceptada`), con cantidades y
**sin nombres de personas**:

```
AUDITORIA carga aceptada archivo="contratos_ejemplo.csv" bytes=406 nuevos=5 yaGuardados=0 duplicadosEnArchivo=0 totalEnBd=5 descartadas=0 conflictos=0 reincidencias=1 fraccionamientos=1
AUDITORIA carga aceptada archivo="contratos_ejemplo.csv" bytes=406 nuevos=0 yaGuardados=5 duplicadosEnArchivo=0 totalEnBd=5 descartadas=0 conflictos=0 reincidencias=1 fraccionamientos=1
```

La primera línea es la carga; la segunda, la recarga del mismo archivo:
`nuevos=0`, todos sus contratos en `yaGuardados`, y `totalEnBd` no cambia.
Son líneas reales del 11/11/2026. La línea real
obtenida al recargar está en [demo-entrega-final.md](demo-entrega-final.md),
demostración 4.

## 6. Cómo se verificó

| Qué demuestra | Prueba Java (`java/src/test/java/dac/`) |
|---|---|
| La misma clave repetida en un archivo se carga una sola vez | `CargadorDeContratosTest#deduplica_por_clave_natural` |
| El duplicado no se convierte en una reincidencia falsa | `CargadorDeContratosTest#el_duplicado_no_genera_alerta_falsa` |
| Ante datos distintos con la misma clave, gana el primero | `CargadorDeContratosTest#gana_el_primero` |
| El mismo número en entidades distintas son dos contratos | `CargadorDeContratosTest#el_numero_solo_no_identifica` |
| Recargar el mismo archivo por HTTP no duplica contratos, entidades, alertas ni evidencia | `ContratoE2ETest#e2e_carga_persiste_y_es_idempotente` |
| Diez recargas seguidas: 0 duplicados en 10 de 10 | `RequisitosNoFuncionalesTest#rnf2_integridad_ante_recargas` (RNF-2) |
| Un segundo archivo distinto que repite un contrato no lo duplica y reporta el conflicto | `AlertasE2ETest#conflicto_contra_la_base` |
| La recarga queda auditada (`nuevos=0 yaGuardados=2`), sin nombres de personas | `SeguridadE2ETest#la_recarga_queda_auditada` |
| Un archivo con un valor malo no deja nada a medias | `SeguridadE2ETest#monto_negativo` |
| La base rechaza por sí misma un valor inválido | `RestriccionesDeBaseDeDatosTest#rechaza_monto_negativo` |

Además se demuestra en vivo: [demo-entrega-final.md](demo-entrega-final.md),
demostraciones 4 (la misma carga dos veces) y 9 (reinicio del servidor).

## 7. Qué queda cubierto y qué no

| Escenario | ¿Cubierto? | Detalle |
|---|---|---|
| Fila duplicada dentro del mismo archivo | ✅ Sí | Se ignora y se cuenta en `duplicadosEnArchivo` |
| Volver a cargar el mismo archivo | ✅ Sí | 0 contratos nuevos, auditado |
| Dos archivos distintos que comparten contratos | ✅ Sí | Cada contrato existe una sola vez |
| Idempotencia entre ejecuciones (persistencia) | ✅ Sí | Los contratos están en PostgreSQL y sobreviven al reinicio del servidor |
| Duplicado con datos distintos (adenda, corrección) | ✅ Sí, con límite | Gana el primero y el conflicto se reporta, pero no se guarda ([DT-08](deuda-tecnica.md)) y la comparación tiene una inconsistencia conocida ([DT-23](deuda-tecnica.md)) |
| Dos personas cargando el mismo contrato al mismo tiempo | ⚠️ Parcial | La base impide el duplicado y responde 409; falta la prueba automática ([DT-09](deuda-tecnica.md)) |

**Limitaciones que siguen:**

- Se reconoce el duplicado **por la clave natural**: si la fuente cambia el
  número de un contrato (por ejemplo, "001" y "0001"), son dos contratos.
  Normalizar números de contrato es una regla de negocio sin validar
  (DT-02).
- El registro de auditoría sale por la consola del servidor y no se guarda
  en archivo ([DT-22](deuda-tecnica.md)).
- Las alertas de fraccionamiento se recalculan en cada consulta: son
  idempotentes por construcción, pero no quedan guardadas
  ([DT-01](deuda-tecnica.md)).

**Historia del alcance.** En la primera entrega el alcance era solo la
idempotencia *dentro de una carga*, porque no había base de datos y
prometer más habría sido falso (Decisión 7). Con la persistencia
(Decisión 14) y HU-06/HU-07, hoy cubre también recargas, ejecuciones
distintas y conflictos.

## 8. Criterios de aceptación

- [x] Un CSV con la misma clave repetida N veces produce **un** contrato.
- [x] Ese CSV **no** genera una alerta de reincidencia por el duplicado.
- [x] La deduplicación ocurre **antes** de la detección de alertas.
- [x] Volver a cargar el mismo archivo no duplica nada en la base.
- [x] La recarga queda registrada en la auditoría, sin nombres de personas.
- [x] Existen pruebas automáticas que lo demuestran (§6).
