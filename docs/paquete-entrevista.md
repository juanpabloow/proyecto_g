# Paquete de entrevista — DAC (Detector de Alertas de Contratación)

Resumen de la evidencia más importante del proyecto, pensado para
presentarlo en 5 minutos y defenderlo bajo preguntas. Cada afirmación
apunta al archivo, la prueba o la medición que la respalda.

**Equipo:** Juan Pablo Cardozo Rivera (backend) · Yerson Andrés Pérez
Cadena (QA y pruebas) · Gerson Geovanni Rojo Rodríguez (requisitos y
análisis de negocio). Aportes individuales en [Equipo/](Equipo/README.md).

**Demo grabada (3 min):** enlazada desde el [README](../README.md#demo-grabada).

---

## 1. El problema resuelto y por qué importa

Los analistas de investigación e inteligencia revisan a mano, en hojas de
cálculo, miles de contratos públicos buscando señales de soborno o
extorsión. Es lento y depende de que alguien "vea" el patrón.

DAC recibe un CSV de contratos y detecta automáticamente dos señales:

| Señal | Regla | Historia |
|---|---|---|
| **Reincidencia** | Mismo contratista con el mismo funcionario en 2 o más contratos, aunque sean de entidades distintas | HU-02 |
| **Fraccionamiento** | Contratos pequeños del mismo contratista en la misma entidad que en 30 días suman el tope de cuantía | HU-03 |

Cada alerta trae **los contratos exactos que la originaron**, para que el
analista la verifique en segundos. DAC no acusa a nadie: una alerta es
una hipótesis que una persona verifica ([visión](vision-producto.md),
[stakeholders](stakeholders.md)).

**Alcance entregado:** 8 de 8 historias con criterios de aceptación y
pruebas ([historias de usuario](historias-usuario.md)), interfaz web,
API REST, persistencia en PostgreSQL 16 e inicio de sesión del analista.

**Cómo correrlo:** `docker compose up -d` · `./mvnw spring-boot:run` ·
`./mvnw verify` (detalle en el [README](../README.md)).

## 2. El problema duro: idempotencia

> Cargar el mismo contrato dos veces debe producir el mismo resultado
> que cargarlo una vez.

**Por qué es difícil de verdad:** un duplicado no produce un error,
produce una *alerta que parece válida*. El mismo contrato contado dos
veces se ve exactamente igual que una reincidencia real: el sistema
inventaría la señal que promete detectar ([problema duro](problema-duro.md)).

**Mecanismo implementado:**

1. **Clave natural** `numero_contrato + entidad` (Decisión 5).
2. **Dentro de una carga:** `CargadorDeContratos` ignora la clave repetida
   antes de detectar alertas.
3. **Entre cargas y entre ejecuciones:** la restricción
   `uq_contrato_clave_natural` de la base lo hace imposible, aunque el
   código fallara (HU-07).
4. **Conflictos:** si el duplicado trae datos distintos, no se pierde en
   silencio: se reporta como conflicto (HU-06).
5. **Cargas simultáneas:** la base sigue impidiendo el duplicado y la API
   responde 409 con un mensaje claro (S-09).
6. **Registro auditable:** cada carga queda en `dac.auditoria` con fecha y
   cantidades, sin nombres de personas (S-07).

**Evidencia:**

| Evidencia | Qué demuestra |
|---|---|
| `CargadorDeContratosTest` (grupo *Problema duro*) | La clave repetida se carga una vez y no genera alerta falsa |
| `ContratoE2ETest` → `e2e_carga_persiste_y_es_idempotente` | Por HTTP real contra PostgreSQL: la misma carga dos veces no duplica nada |
| `ConflictosDeDuplicadosTest` | El duplicado con datos distintos se reporta |
| `RequisitosNoFuncionalesTest` (RNF-2) | 1.000 contratos cargados 10 veces: 0 duplicados en 10 de 10 |
| Demo en vivo | Momento 2 del README: `contratosNuevos: 0` en la segunda carga |

**Límites que siguen existiendo:** la carga simultánea no tiene prueba
automática (DT-09) y la regla para elegir entre dos versiones de un
contrato en conflicto sigue pendiente de validar con un analista (S-1).

## 3. Métricas reales y condiciones de medición

Protocolo del curso: tres corridas medidas, la primera de calentamiento
no cuenta. Detalle completo en [seguridad y RNF](seguridad-y-rnf.md).

| RNF | Objetivo | Resultado | Condiciones |
|---|---|---|---|
| **RNF-1 Rendimiento** — cargar 5.000 contratos | ≤ 5 s | ✅ Peor de 3 corridas: **2.156 ms** (43 % del límite) | CI GitHub Actions, `ubuntu-24.04`, 4 núcleos, Java 21, PostgreSQL 16, datos ficticios de semilla fija |
| **RNF-2 Integridad** — mismo archivo de 1.000 contratos, 10 recargas | 0 duplicados | ✅ **0 contratos nuevos en 10 de 10** | Igual que RNF-1, se mide en cada PR |
| **RNF-3 Concurrencia** — 200 usuarios leyendo `/api/alertas` (k6) | < 1 % errores y p95 ≤ 1.500 ms | ✅ Con sesión: **0 % errores, p95 777 ms** · ❌ Con HTTP Basic en cada petición: p95 2,72 s | Local, macOS Apple Silicon, 1.200 contratos, pool de 10 conexiones |

**Antes y después de una optimización (Decisión 25):** la carga
consultaba la base por cada fila y el tiempo crecía más rápido que el
archivo. Se cambió a leer lo existente una sola vez y trabajar con
índices en memoria; con eso RNF-1 cumple. `EXPLAIN ANALYZE` confirma que
el SQL no es el cuello de botella (búsqueda por clave natural < 0,02 ms).

**Lo que no cumple, dicho claro:** con HTTP Basic en cada petición, BCrypt
(lento a propósito) hace que RNF-3 no cumpla. Queda como deuda D-7.

**Pruebas:** 88 automatizadas (59 de dominio y 29 de punta a punta por
HTTP real contra PostgreSQL), en verde en cada push y PR.

## 4. Decisiones técnicas importantes y alternativas descartadas

Registro completo en [decisiones técnicas](decisiones-tecnicas.md).

| Decisión | Alternativa descartada | Por qué |
|---|---|---|
| **El esquema lo gobierna `db/schema.sql`**, no Hibernate (D-15) | `ddl-auto=update` | Las restricciones que sostienen el problema duro no pueden quedar a merced del mapeo. El esquema real coincide con el documentado |
| **Un solo lenguaje: Java 21 + Spring Boot** (D-18) | Mantener Python y Java en paralelo | Los dos programas no se comportaban igual: ante una fila sin contratista, Java la descartaba y Python generaba una alerta "sobre nadie". Se portaron todas las pruebas a JUnit |
| **Una fila incompleta se descarta y se reporta; el archivo sigue siendo válido** (D-12) | Aceptarla, o invalidar todo el archivo | Aceptarla crea alertas sobre nadie; invalidar deja al analista sin nada por una fila mala entre 5.000 |
| **Interfaz sin frameworks**, servida por el mismo Spring Boot (D-21) | React/Angular en un proyecto aparte | Un solo comando levanta todo y la interfaz no tiene lógica de negocio propia: usa la misma API que las pruebas |

## 5. Seguridad en una frase por control

Una sola puerta de entrada (el CSV) y todo lo que trae se trata como no
confiable: login con contraseña en hash BCrypt, validación en la frontera
(solo `.csv`, 10 MB, longitudes, montos y fechas), consultas
parametrizadas con ataque SQLi probado, XSS frenado con `textContent` y
CSP, fórmulas de Excel neutralizadas, secretos en `.env`, errores sin
información interna y auditoría sin datos personales. Lo que falta
(HTTPS, roles, límite de intentos, CSRF) está declarado como deuda y es
aceptable solo mientras DAC corra en `localhost` con datos ficticios.

## 6. Qué haríamos distinto

1. **Empezar con una sola tecnología desde la semana 1.** Mantuvimos
   Python y Java en paralelo y terminaron comportándose distinto. La
   consolidación (D-18) costó una semana que pudo evitarse.
2. **Tener la base de datos desde el esqueleto andante.** La idempotencia
   empezó limitada "a una carga" porque no había persistencia; con la base
   desde el inicio, el problema duro se habría resuelto completo antes.
3. **Validar las reglas de negocio con un analista real.** El umbral de
   fraccionamiento, la normalización de nombres y el manejo de filas
   incompletas son provisionales (D-12, D-19, D-20) porque nunca tuvimos
   ese interlocutor.
4. **Medir desde el principio.** Los RNF se midieron en el Módulo 7; antes
   no sabíamos que la carga se volvía lenta con archivos grandes.
5. **Respetar siempre la revisión cruzada.** Algunos PR los fusionó su
   propio autor; la regla de revisar antes de fusionar debió aplicarse en
   todos.

## 7. Uso de IA

Declaración completa en [uso de IA](uso-ia.md): qué herramienta, para qué,
qué se aceptó, qué se modificó, qué se rechazó y cómo se verificó. Regla
del equipo: nada generado por IA entra a `main` sin pruebas en verde y sin
que alguien del equipo pueda explicarlo.
