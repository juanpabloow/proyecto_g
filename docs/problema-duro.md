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

Dentro de una carga, `CargadorDeContratos` guarda la primera versión de
cada clave natural; una fila con una clave ya vista se ignora y no llega a
la detección de alertas.

Entre cargas y entre ejecuciones lo impide la base de datos: la
restricción `uq_contrato_clave_natural` ([db/schema.sql](../db/schema.sql))
hace imposible guardar dos contratos con el mismo número en la misma
entidad, y `ContratoServicio` solo inserta los contratos cuya clave no está
ya guardada (HU-07).

**Regla de conflicto (gana el primero).** Si dos filas comparten la clave
pero traen datos distintos, por ejemplo un monto corregido, se conserva
**la primera**, y la diferencia **se reporta** como conflicto con su número
de fila (HU-06), tanto dentro del archivo como contra lo ya guardado.

## 5. Qué queda cubierto y qué no

| Escenario | ¿Cubierto? | Detalle |
|---|---|---|
| Fila duplicada dentro del mismo archivo | ✅ Sí | Se ignora la repetición |
| Volver a cargar el mismo archivo | ✅ Sí | 0 contratos nuevos; la base lo impide con `uq_contrato_clave_natural` |
| Dos archivos distintos que comparten contratos | ✅ Sí | Cada contrato existe una sola vez |
| Idempotencia entre ejecuciones (persistencia) | ✅ Sí | Los contratos están en PostgreSQL y sobreviven al reinicio del servidor |
| Duplicado con datos distintos (adenda, corrección) | ✅ Sí, con límite | Gana el primero y el conflicto se reporta, pero no se guarda ([DT-08](deuda-tecnica.md)) |
| Dos personas cargando el mismo contrato al mismo tiempo | ⚠️ Parcial | La base impide el duplicado y la segunda carga recibe 409; falta la prueba automática ([DT-09](deuda-tecnica.md)) |

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
| *el mismo número en entidades distintas son dos contratos* | El mismo `numero_contrato` en dos entidades **no** se deduplica |
| `ContratoE2ETest` → `e2e_carga_persiste_y_es_idempotente` | La misma carga dos veces no duplica nada en la base (HU-07) |
