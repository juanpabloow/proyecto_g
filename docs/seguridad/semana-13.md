# Semana 13 — Seguridad básica del producto (evidencias)

Registro de evidencias de la semana 13 del Módulo 7. El detalle completo
(checklist S-01 a S-15, modelo de amenazas y deuda de seguridad) está en
[docs/seguridad-y-rnf.md](../seguridad-y-rnf.md); aquí queda el resumen
de lo que se ejecutó, con el commit o la prueba que lo demuestra.

**Pregunta de la semana:** ¿por dónde entraría un dato malicioso a
nuestro sistema, y qué lo detiene? En DAC hay una sola puerta: el CSV que
sube el analista (`POST /api/contratos/cargar`).

## 1. Checklist de controles mínimos aplicado

| Control del curso | En DAC | Evidencia |
|---|---|---|
| Autenticación implementada | ⚠️ No en este alcance: corre en `localhost` con datos ficticios | Deuda DT-03 |
| Autorización por roles | ⚠️ Ligada a la anterior | Deuda DT-03 |
| Validación de todas las entradas | ✅ Tipo `.csv`, máx. 10 MB, longitud por columna, monto ≥ 0, fecha AAAA-MM-DD | `ValidacionEnLaFronteraTest`, `SeguridadE2ETest` · commits `d6ad93c`, `4a69cb2`, `f58a196` |
| Consultas parametrizadas | ✅ Spring Data JPA; demostrado con ataque | `SeguridadE2ETest` → inyección SQL |
| Gestión segura de sesiones | — No hay sesiones | Aplica con DT-03 |
| Protección de datos sensibles | ✅ Clave de la base fuera del repositorio; el registro no guarda nombres | Decisiones 24 y 27 · commits `62b76e2`, `4f850ce` |
| Mensajes de error sin información sensible | ✅ Sin trazas, clases ni SQL | `SeguridadE2ETest` → errores sin detalles internos |
| Cabeceras de seguridad básicas | ✅ CSP, `X-Frame-Options`, `nosniff`, `no-referrer`, `no-store` | `SeguridadE2ETest` · commit `3623a0e` |
| Registro de eventos críticos | ✅ Cargas aceptadas o rechazadas y exportaciones | `AuditoriaTest` · commits `3623a0e`, `add1ec1` |

## 2. Modelo básico de amenazas

Está en la sección *Modelo básico de amenazas* de
[seguridad-y-rnf.md](../seguridad-y-rnf.md#modelo-básico-de-amenazas):
siete activos, cada uno con su amenaza, el vector de entrada, el control
que lo detiene hoy y su impacto.

## 3. Pruebas de seguridad (ataques simulados)

Todas corren contra el sistema real (HTTP + PostgreSQL) en cada Pull
Request.

| Ataque del curso | Ejemplo usado | Resultado esperado | Resultado |
|---|---|---|---|
| Inyección SQL | `' OR '1'='1` y `x'); DROP TABLE contrato; --` | Bloqueado | ✅ Se guarda como texto; la tabla sigue intacta |
| XSS | `<script>alert(1)</script>` en un nombre | Neutralizado | ✅ Se muestra como texto (`textContent`) y la CSP bloquea scripts ajenos |
| IDOR | — | Autorizado | — No aplica: la API no expone datos por usuario (DT-03) |
| Divulgación de información | Error provocado a propósito | Sin datos internos | ✅ Mensaje útil sin trazas ni SQL |
| Fuerza bruta de login | — | Limitar intentos | — No aplica: no hay inicio de sesión (DT-03) |
| Fórmula en Excel *(propio de DAC)* | `=HYPERLINK(...)` en un nombre | Neutralizada | ✅ Se exporta como texto |

## 4. Correcciones aplicadas

| Hallazgo | Corrección | Commit |
|---|---|---|
| Nombre de más de 200 caracteres → error 500 | Se rechaza la fila con mensaje claro | `d6ad93c` |
| Monto negativo → error 500 | Validación antes de escribir | `4a69cb2` |
| Se aceptaba cualquier tipo de archivo | Solo `.csv`, máximo 10 MB | `f58a196` |
| Clave de la base publicada en GitHub (DT-04) | Archivo `.env` y revisión de secretos en el CI | `62b76e2`, `0b87b33` |
| Errores con detalles de Spring | `server.error.include-*=never` | `4f850ce` |
| Sin registro de eventos críticos | Registro `dac.auditoria` | `3623a0e` |

Todo entró a `main` con el Pull Request #6 (CI en verde, 82 pruebas).

## 5. Decisiones técnicas registradas

[Decisiones 24 a 27](../decisiones-tecnicas.md): clave en `.env`, lectura
única en la carga, RNF medidos en el CI y registro de auditoría sin
nombres. Lo que se decidió **no** hacer (inicio de sesión, HTTPS, revisión
de dependencias) está en la deuda DT-03, DT-14 y DT-15.
