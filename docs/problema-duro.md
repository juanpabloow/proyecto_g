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

En `src/contratos.py`, `cargar_contratos` mantiene un conjunto
`claves_vistas`. Antes de agregar una fila, calcula su clave natural; si
ya está en el conjunto, la fila se ignora y no llega a la detección de
alertas.

**Regla de conflicto (gana el primero).** Si dos filas comparten la clave
pero traen datos distintos —por ejemplo un monto corregido— se conserva
**la primera** y la segunda se descarta sin avisar. Verificado: dos filas
con clave `001 + E1` y montos `100` y `999` dejan un solo contrato con
monto `100`.

Es una decisión consciente y también una limitación: si la segunda fila
era la corrección, se pierde. Registrar esos conflictos en lugar de
descartarlos en silencio queda como trabajo futuro
([HU-06](historias-usuario.md)).

## 5. Qué queda cubierto y qué no

| Escenario | ¿Cubierto? | Detalle |
|---|---|---|
| Fila duplicada dentro del mismo archivo | ✅ Sí | Se ignora la repetición |
| Correr el programa dos veces con el mismo archivo | ✅ Sí | No hay estado acumulado: el resultado es idéntico |
| Duplicado con datos distintos (adenda, corrección) | ⚠️ Parcial | Gana el primero, sin registro del conflicto (§4) |
| Dos archivos distintos, en una misma ejecución, que comparten contratos | ❌ No | Cada llamada a `cargar_contratos` usa su propio conjunto de claves; unir las dos listas duplicaría |
| Idempotencia entre ejecuciones (persistencia) | ❌ No | No hay base de datos todavía. Es el siguiente paso del roadmap |
| Dos personas cargando archivos al mismo tiempo (concurrencia) | ❌ No | Riesgo conocido y aceptado para este MVP |

**El alcance real de esta entrega es la idempotencia *dentro de una
carga*.** Está dicho así, y no como "idempotencia total", a propósito:
prometer el caso persistente sin base de datos sería falso.

## 6. Criterios de aceptación

- [x] Un CSV con la misma clave repetida N veces produce **un** contrato.
- [x] Ese CSV **no** genera una alerta de reincidencia por el duplicado.
- [x] La deduplicación ocurre **antes** de la detección de alertas.
- [x] Existe una prueba automatizada que lo demuestra.

## 7. Pruebas que lo respaldan

`tests/test_contratos.py`:

| Prueba | Qué demuestra |
|---|---|
| `test_ignora_contratos_duplicados` | La clave repetida se carga una sola vez |
| `test_duplicado_no_genera_alerta_falsa` | El duplicado no se convierte en reincidencia (§2, punto 3) |
| `test_gana_el_primero_ante_datos_distintos` | La regla de conflicto de §4 |
| `test_mismo_numero_en_entidades_distintas_no_se_deduplica` | El mismo `numero_contrato` en dos entidades **no** se deduplica |
