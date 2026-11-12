# Problema duro: idempotencia en la carga de contratos

## 1. Enunciado

> **Cargar el mismo contrato dos veces debe producir el mismo resultado
> que cargarlo una vez.**

Si una fila viene repetida en el archivo, o si alguien reenvía el mismo
CSV, el sistema **no debe crear una alerta nueva ni contar el contrato
dos veces**.

## 2. Por qué éste es el problema duro de esta entrega

No es difícil por la técnica —es difícil porque **es el punto donde el
sistema se gana o se pierde la confianza del usuario**.

1. **Es el error más frecuente del flujo real.** Archivos reenviados,
   copias mal hechas en Excel, filas pegadas dos veces. No es un caso
   borde: es el día a día.
2. **Falla en silencio.** Un duplicado no produce un error; produce una
   *alerta que parece válida*. Nadie se da cuenta.
3. **Ataca el problema que justifica el proyecto.** Un duplicado
   convierte un contrato en una "reincidencia" falsa: el mismo contrato
   contado dos veces se ve exactamente igual que un contratista
   repitiendo con un funcionario. El sistema inventaría precisamente la
   señal que promete detectar.
4. **Un falso positivo cuesta más que un falso negativo.** Del otro lado
   de la alerta hay personas reales (stakeholder S-6). Y para el
   analista, dos alertas falsas bastan para volver a Excel.

## 3. Decisión: la clave natural

Cada contrato se identifica con **`numero_contrato` + `entidad`**.

| Alternativa | Por qué se descartó |
|---|---|
| Solo `numero_contrato` | Dos entidades distintas pueden usar el mismo consecutivo ("001"); se descartarían contratos legítimos |
| La fila completa (los seis campos) | Un monto corregido o una fecha reformateada crearían un "contrato nuevo": el duplicado pasaría igual |
| Un id generado por el sistema | No sirve: el duplicado llega desde afuera, hay que reconocerlo por lo que *ya trae* |

Regla asociada: [R-2](reglas-de-negocio.md).

## 4. Cómo se resuelve hoy

Dentro de una carga, `CargadorDeContratos` mantiene un conjunto de claves
vistas: antes de agregar una fila calcula su clave natural, y si ya está,
la fila se ignora y no llega a la detección de alertas.

Entre cargas y entre ejecuciones lo impide la base de datos: la
restricción `uq_contrato_clave_natural` ([db/schema.sql](../db/schema.sql))
hace imposible guardar dos contratos con el mismo número en la misma
entidad, y `ContratoServicio` consulta por clave natural antes de
insertar (HU-07).

**Regla de conflicto (gana el primero, y se reporta).** Si dos filas
comparten la clave pero traen datos distintos —por ejemplo un monto
corregido— se conserva **la primera** y la segunda **no se pierde en
silencio**: la respuesta de la carga la informa en `conflictos`, con los
datos de las dos versiones, para que el analista decida
([HU-06](historias-usuario.md), `ConflictosDeDuplicadosTest`).

**Cargas simultáneas.** Si dos cargas intentan guardar el mismo contrato
al mismo tiempo, la restricción de la base impide el duplicado y la API
responde **409** con un mensaje que explica que no se guardó nada y se
puede reintentar (S-09 en [seguridad y RNF](seguridad-y-rnf.md)).

**Registro auditable.** Cada carga, aceptada o rechazada, queda en el
registro `dac.auditoria` con fecha y cantidades (nuevos, duplicados
ignorados, descartados), sin nombres de personas (S-07).

## 5. Qué queda cubierto y qué no

| Escenario | ¿Cubierto? | Detalle |
|---|---|---|
| Fila duplicada dentro del mismo archivo | ✅ Sí | Se ignora la repetición antes de detectar alertas |
| El mismo archivo cargado dos o más veces | ✅ Sí | La base ya tiene el contrato: `contratosNuevos: 0`. Medido en RNF-2: 10 recargas, 0 duplicados en 10 de 10 |
| Dos archivos distintos que comparten contratos | ✅ Sí | La segunda carga reconoce los contratos que ya están en la base |
| Idempotencia entre ejecuciones (persistencia) | ✅ Sí | Restricción `uq_contrato_clave_natural` en PostgreSQL (HU-07) |
| Duplicado con datos distintos (adenda, corrección) | ✅ Reportado | Gana el primero y el conflicto se informa (HU-06). Qué versión debe ganar sigue pendiente de validar con un analista (S-1) |
| Dos personas cargando al mismo tiempo (concurrencia) | ⚠️ Parcial | La base impide el duplicado y la API responde 409; falta una prueba automática que provoque la carrera (DT-09) |

**Historia del alcance.** En la primera entrega el alcance era solo la
idempotencia *dentro de una carga*, porque no había base de datos y
prometer más habría sido falso (Decisión 7). Con la persistencia
(Decisión 14) y HU-06/HU-07, hoy cubre también recargas, ejecuciones
distintas y conflictos.

## 6. Criterios de aceptación

- [x] Un CSV con la misma clave repetida N veces produce **un** contrato.
- [x] Ese CSV **no** genera una alerta de reincidencia por el duplicado.
- [x] La deduplicación ocurre **antes** de la detección de alertas.
- [x] Existe una prueba automatizada que lo demuestra.

## 7. Pruebas que lo respaldan

`CargadorDeContratosTest`, grupo *Problema duro — Idempotencia*:

| Prueba | Qué demuestra |
|---|---|
| *la misma clave natural repetida se carga una sola vez* | La clave repetida se carga una sola vez |
| *un contrato duplicado no genera una alerta falsa* | El duplicado no se convierte en reincidencia (§2, punto 3) |
| *ante datos distintos con la misma clave, gana el primero* | La regla de conflicto de §4 |
| `ConflictosDeDuplicadosTest` | El duplicado con datos distintos se reporta como conflicto (HU-06) |
| `RequisitosNoFuncionalesTest` (RNF-2) | 1.000 contratos cargados 10 veces: 0 duplicados en 10 de 10 |
| `AuditoriaTest` | Cada carga queda registrada y el registro no acepta líneas inyectadas |
| *el mismo número en entidades distintas son dos contratos* | El mismo `numero_contrato` en dos entidades **no** se deduplica |
| `ContratoE2ETest` → `e2e_carga_persiste_y_es_idempotente` | La misma carga dos veces no duplica nada en la base (HU-07) |
