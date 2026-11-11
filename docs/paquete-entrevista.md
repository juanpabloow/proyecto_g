# Paquete para la entrevista

Resumen para preparar la entrevista final: qué problema resolvimos, por
qué es difícil, qué medimos y qué decidimos. Cada afirmación remite a la
evidencia en el repositorio. Las cifras son las medidas, con sus
condiciones; no hay estimaciones.

## 1. El problema que resolvimos

**Qué:** DAC (Detector de Alertas de Contratación) cruza los contratos
públicos de un archivo CSV y marca dos señales de posible soborno o
extorsión: **reincidencia** (el mismo contratista con el mismo funcionario
en 2 o más contratos) y **fraccionamiento** (contratos pequeños del mismo
contratista en la misma entidad que, en 30 días, alcanzan el tope de
cuantía). Cada alerta trae los contratos exactos que la originaron.

**Para quién y por qué vale la pena:** el analista de investigación
(S-1) hoy hace ese cruce a mano en hojas de cálculo. Es lento y se le
escapan coincidencias. DAC no acusa a nadie: la alerta es una hipótesis
que una persona verifica, y por eso **sin evidencia no hay alerta**
(HU-02, criterio 5; [vision-producto.md](vision-producto.md)).

**Cómo se ve:** interfaz web con inicio de sesión; se arrastra el CSV y se
ven las alertas con su evidencia, las filas descartadas y los conflictos,
y se exporta el reporte a Excel. Las 8 historias están terminadas
([backlog.md](backlog.md)).

## 2. El problema duro: idempotencia

**Enunciado:** cargar el mismo contrato dos veces debe dar el mismo
resultado que cargarlo una vez ([problema-duro.md](problema-duro.md)).

**Por qué es difícil de verdad:** un duplicado no produce un error,
produce una **reincidencia falsa**. El mismo contrato contado dos veces se
ve exactamente igual que un contratista que repite con un funcionario: el
sistema inventaría la señal que promete detectar, contra una persona real.

**Mecanismo:**
1. **Clave natural** `numero_contrato` + `entidad` (no el número solo, ni la fila completa).
2. Dentro de una carga, se conserva la primera versión de cada clave.
3. Entre cargas, lo impide **la base**: `uq_contrato_clave_natural`.
4. Si el duplicado traía datos distintos, gana el primero y el conflicto se reporta (HU-06).

**Evidencia:**
- Pruebas: `CargadorDeContratosTest#deduplica_por_clave_natural`,
  `CargadorDeContratosTest#el_duplicado_no_genera_alerta_falsa`,
  `ContratoE2ETest#e2e_carga_persiste_y_es_idempotente`,
  `RequisitosNoFuncionalesTest#rnf2_integridad_ante_recargas`.
- Demostración en vivo: [demo-entrega-final.md](demo-entrega-final.md),
  demostración 4 (la misma carga dos veces da 0 nuevos).
- Registro de auditoría: cada carga deja una línea con los contratos
  nuevos y los que ya estaban guardados (ver problema-duro.md).

## 3. Métricas reales y sus condiciones

Protocolo del curso: tres corridas, se descarta la primera y se informan
la 2 y la 3. Todas las mediciones son del 11/11/2026, en un Apple M4 Pro
(12 núcleos, Java 26, PostgreSQL 16 en Docker), sobre el código del commit
`778634a`.

| Qué | Corridas 2 y 3 | Objetivo | Datos y carga |
|---|---|---|---|
| RNF-1: cargar 5.000 contratos, con las dos señales calculadas | **1.509 ms** y **1.402 ms** | ≤ 5.000 ms | 5.000 contratos ficticios (semilla 2026), 1 usuario, base vacía |
| RNF-2: 10 recargas del mismo archivo | **0 nuevos, 10 de 10 sin cambios** en las dos | 0 duplicados | 1.000 contratos ficticios (semilla 7) |
| RNF-3: 200 usuarios a la vez en `GET /api/alertas`, con sesión | p95 **567 ms** y **761 ms**; **0 %** de errores | p95 ≤ 1.500 ms, < 1 % de errores | k6, 1.200 contratos (semilla 2026) |
| RNF-3 con HTTP Basic en cada petición (una corrida, informativa) | p95 **2,72 s**, 0 % de errores: **no cumple** | Ídem | Causa: BCrypt en cada petición ([DT-17](deuda-tecnica.md)) |
| SQL con 5.000 contratos (EXPLAIN ANALYZE) | `findAllConFetch` **2,46** y **2,34 ms**; clave natural **0,004** y **0,009 ms** (Index Scan) | — | PostgreSQL 16 en Docker |
| Suite automática | Ver `./mvnw verify` en el README | 0 fallos | Local y GitHub Actions |

El detalle de cada medición (operación, carga, datos, equipo y corridas)
está en [seguridad-y-rnf.md](seguridad-y-rnf.md).

## 4. Decisiones técnicas, con lo que descartamos

| Decisión | Alternativas descartadas | Por qué |
|---|---|---|
| **La identidad de un contrato es `numero_contrato` + `entidad`** (Decisión 5) | Solo el número; la fila completa; un id generado | Dos entidades repiten el consecutivo "001"; con la fila completa, un monto corregido pasaría como contrato nuevo; un id generado no reconoce un duplicado que llega de afuera |
| **Una fila incompleta se descarta y se reporta; el archivo sigue siendo válido** (Decisión 12) | Aceptarla; invalidar el archivo completo | Aceptarla produce una alerta "sobre nadie"; invalidar el archivo deja al analista sin nada por una fila mala entre miles. Es provisional, pendiente de S-1 |
| **Las pruebas de punta a punta entran por HTTP real, contra PostgreSQL real, sin mocks** (Decisión 16) | `MockMvc`; llamar directo al servicio | Con un socket real, la prueba falla si se rompe cualquier frontera (ruta, JSON, transacción, restricción de la base). Los mocks quedan para las pruebas unitarias de la capa de aplicación |
| **Normalizar nombres de forma conservadora** (Decisión 19) | Comparación exacta; quitar también sufijos societarios y alias | La exacta escondía reincidencias reales; quitar sufijos uniría "ACME SAS" con "ACME LTDA" y señalaría a quien no es (S-6) |

Más en [decisiones-tecnicas.md](decisiones-tecnicas.md), incluidos los
patrones de diseño aplicados y rechazados.

## 5. Lo que sabemos que falta

La deuda está priorizada en [deuda-tecnica.md](deuda-tecnica.md). Las
más importantes:
- Reglas sin validar con un analista real (DT-02).
- El fraccionamiento no se guarda (DT-01).
- Seguridad para desplegar: superusuario de la base, sin límite de intentos, sin HTTPS ni cifrado (DT-19, DT-20, DT-14, DT-21).

## 6. Reflexión del equipo

[COMPLETAR: qué aprendimos, qué haríamos distinto, qué fue lo más difícil
y cómo usamos la IA sin dejar de entender lo que entregamos. Escribirlo
con nuestras palabras; no lo debe redactar la IA.]
