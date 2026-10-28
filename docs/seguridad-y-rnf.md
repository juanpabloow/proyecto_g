# Seguridad y requisitos no funcionales (Módulo 7)

Evidencia del Módulo 7 (semanas 13–14): checklist de seguridad aplicado
con sus hallazgos y correcciones, los dos requisitos no funcionales (RNF)
medidos con método reproducible y la deuda de seguridad que decidimos no
atender. Todo lo que aquí se afirma tiene una prueba automática que corre
en cada Pull Request (GitHub Actions).

**La pregunta del módulo:** *¿por dónde entraría un dato malicioso a
nuestro sistema, y qué lo detiene?* En DAC hay una sola puerta de entrada:
el CSV que sube el analista (`POST /api/contratos/cargar`). Todo dato que
viene en ese archivo se trata como no confiable: nombres de contratistas,
de funcionarios y de entidades, y también el nombre del propio archivo.

## 1. Checklist de seguridad aplicado

Cada fila es un hallazgo real sobre DAC, no una casilla marcada. **Estado:**
✅ corregido y probado · ⚠️ aceptado como deuda (sección 3) · — no aplica.

| # | Control | Hallazgo antes del Módulo 7 | Corrección | Evidencia (prueba) | Estado |
|---|---|---|---|---|---|
| S-01 | Cabeceras de seguridad | Ninguna: la página podía meterse en un iframe ajeno y el navegador podía "adivinar" tipos | Filtro `CabecerasDeSeguridad`: Content-Security-Policy (solo scripts y estilos propios), `X-Frame-Options: DENY`, `nosniff`, `Referrer-Policy: no-referrer`, `Cache-Control: no-store` en la API | `SeguridadE2ETest` → *cabeceras de seguridad en la página y en la API* | ✅ |
| S-02 | Validación de entradas en la frontera | Un nombre de más de 200 caracteres o un monto negativo llegaban a PostgreSQL y la carga terminaba en **error 500**; se aceptaba cualquier tipo de archivo | Se valida antes de escribir: solo `.csv`, máximo 10 MB, longitud de cada campo igual a su columna, monto no negativo y fecha AAAA-MM-DD, con mensaje que nombra la fila | `SeguridadE2ETest` (4 casos) y `ValidacionEnLaFronteraTest` (3) | ✅ |
| S-03 | Inyección SQL | Sin hallazgo en el código: Spring Data JPA siempre envía los valores como parámetros. Faltaba **demostrarlo** | Prueba de ataque: un contratista `x'); DROP TABLE contrato; --` y un funcionario `' OR '1'='1` se guardan como texto | `SeguridadE2ETest` → *inyección SQL* | ✅ |
| S-04 | XSS (scripts en los datos) | Sin hallazgo: la interfaz usa `textContent`. Faltaba **demostrarlo** y tener una segunda barrera | Prueba de que el JS no usa `innerHTML`, `eval` ni similares; la CSP impide ejecutar scripts que no sean del servidor | `SeguridadE2ETest` → *XSS* (2 casos) | ✅ |
| S-05 | Divulgación de información en errores | El 400 no decía qué fallaba, y un error inesperado podía mostrar detalles de Spring | Errores con mensaje útil para el analista y sin trazas, clases ni SQL (`server.error.include-*=never`) | `SeguridadE2ETest` → *errores sin detalles internos* | ✅ |
| S-06 | Secretos fuera del repositorio | La clave de la base estaba escrita en `application.properties` y `docker-compose.yml` (deuda DT-04) | Se lee de `.env` (ignorado por Git, plantilla `.env.example`). El CI falla si alguien vuelve a escribir una clave en el código | `SeguridadE2ETest` → *la clave no está en el código* + paso del CI | ✅ |
| S-07 | Registro de eventos críticos | No quedaba rastro de quién cargó o exportó qué ni de los rechazos | Registro `dac.auditoria`: cada carga aceptada o rechazada y cada exportación, con fecha y cantidades, **sin nombres de personas** (S-8). Los saltos de línea se limpian para que nadie invente líneas en el registro | `SeguridadE2ETest` → *auditoría*, `AuditoriaTest` (3) | ✅ |
| S-08 | Inyección de fórmulas al exportar | Ya corregido en HU-08 | Una celda que empieza por `= + - @` se exporta como texto | `ExportadorDeAlertasTest` | ✅ |
| S-09 | Cargas simultáneas (DT-09) | Dos cargas al mismo tiempo del mismo contrato terminaban en un 500 sin explicación | La base sigue impidiendo el duplicado; ahora la respuesta es **409** con un mensaje que explica que no se guardó nada y se puede reintentar | Revisado en código (`ManejadorDeErrores`); la prueba concurrente queda en la deuda | ✅ |
| S-10 | Autenticación y autorización | No hay inicio de sesión: cualquiera con acceso al puerto 8080 usa el sistema | No se implementa en este alcance | — | ⚠️ DT-03 |
| S-11 | Hash de contraseñas | DAC no guarda contraseñas de usuarios (no hay cuentas) | — | — | — (aplicará con S-10) |
| S-12 | Fuerza bruta de inicio de sesión | No hay inicio de sesión | — | — | — (aplicará con S-10) |
| S-13 | Acceso directo a objetos (IDOR) | La API no expone identificadores ni datos por usuario; todo analista ve todo | Sin cambio | — | ⚠️ ligado a DT-03 |
| S-14 | Cifrado en tránsito (HTTPS) | El servidor corre en `http://localhost` | No aplica mientras no se despliegue | — | ⚠️ DT-14 |
| S-15 | Dependencias vulnerables | No hay revisión automática de las librerías | No se agrega en este alcance | — | ⚠️ DT-15 |

### Modelo básico de amenazas

| Activo | Amenaza | Cómo entraría | Qué lo detiene hoy | Impacto si pasa |
|---|---|---|---|---|
| Base de contratos y alertas | Borrado o lectura indebida | Inyección SQL en un nombre del CSV | Consultas parametrizadas (S-03) | Alto |
| Navegador del analista | Ejecución de código ajeno | Un `<script>` en un nombre del CSV | `textContent` + CSP (S-04) | Alto |
| Excel de quien recibe el reporte | Ejecución de fórmulas | Un nombre que empieza por `=` | Neutralización al exportar (S-08) | Alto |
| Datos sensibles (S-8) | Filtración | Acceso al puerto 8080 sin sesión | **Nada todavía** (DT-03) | Alto |
| Disponibilidad | Saturar el servidor | Archivos enormes | Límite de 10 MB (S-02) | Medio |
| Clave de la base | Uso indebido | Leerla en GitHub | `.env` fuera del repositorio (S-06) | Alto |
| Registro de auditoría | Falsificarlo | Saltos de línea en un dato | Limpieza al registrar (S-07) | Medio |

## 2. Requisitos no funcionales verificados

Declaramos dos RNF con criterio medible. La medición la hace
`RequisitosNoFuncionalesTest` en **cada** ejecución del CI, siempre con el
mismo método, los mismos datos y el mismo tipo de máquina, así que
cualquiera puede repetirla.

**Condiciones de la medición:**

- **Máquina:** runner `ubuntu-latest` de GitHub Actions; la prueba anota
  núcleos, memoria de la JVM y versión de Java en cada corrida.
- **Base:** PostgreSQL 16 en contenedor, con el esquema real de `db/schema.sql`.
- **Datos:** contratos **ficticios** generados con semilla fija (la misma
  semilla da siempre el mismo archivo): 20 entidades, 600 contratistas,
  250 funcionarios, montos de 1 a 150 millones y fechas de 2025 y 2026.
- **Cómo se mide:** por HTTP real (`POST /api/contratos/cargar`), desde que
  se envía el archivo hasta que llega la respuesta completa con las alertas.
- **Medición registrada aquí:** ejecución del CI del 28 de octubre de 2026
  en el PR #6, en un runner de 4 núcleos con 3.996 MB para la JVM y Java
  21.0.13. Cada nueva ejecución vuelve a medir.
- **Dónde ver el resultado:** pestaña *Actions* del repositorio → la
  ejecución → **Summary**, y el artefacto `reporte-pruebas` (archivo
  `rnf/resultados.md`).

### RNF-1 — Rendimiento de la carga

| | |
|---|---|
| **Escenario** | El analista carga un archivo de **5.000 contratos** |
| **Objetivo** | La respuesta, con las dos señales calculadas, llega en **5 segundos o menos** |
| **Método** | 1 carga de calentamiento (no cuenta) y 3 cargas medidas, con la base vacía antes de cada una; se toma la **peor** de las 3 |
| **Resultado** | ✅ **Cumple.** Tiempos de las 3 corridas: 2.156, 1.578 y 1.481 ms. **Peor: 2.156 ms** (43 % del límite). El archivo de 346 KB produjo 103 alertas |

**Hallazgo y corrección:** el servicio hacía varias consultas a la base
por cada fila del archivo, y cada consulta obligaba a Hibernate a revisar
todo lo cargado hasta ese momento: el tiempo crecía más que la cantidad de
filas. Se cambió para leer lo existente **una sola vez** al empezar y
trabajar con índices en memoria ([Decisión 25](decisiones-tecnicas.md)).

### RNF-2 — Integridad ante recargas (idempotencia)

| | |
|---|---|
| **Escenario** | El mismo archivo de **1.000 contratos** se carga **10 veces seguidas** (archivos reenviados, el caso real del problema duro) |
| **Objetivo** | Después de cada recarga hay exactamente los mismos contratos, alertas y filas de evidencia que tras la primera carga: **0 duplicados en 10 de 10** |
| **Método** | Se cuentan las filas de `contrato`, `alerta` y `alerta_contrato` después de cada recarga y se suman los contratos nuevos que informa cada respuesta |
| **Resultado** | ✅ **Cumple.** Tras la 1.ª carga: 1.000 contratos, 4 alertas y 8 filas de evidencia. En las 10 recargas: **0 contratos nuevos** y las tres cantidades iguales en **10 de 10** |

## 3. Deuda de seguridad declarada

Riesgos que **decidimos no atender** en este alcance, por qué es
aceptable hoy y qué tendría que pasar para que deje de serlo. El detalle
de cada deuda está en [deuda-tecnica.md](deuda-tecnica.md).

| Deuda | Por qué es aceptable hoy | Deja de ser aceptable cuando… |
|---|---|---|
| **DT-03** Sin inicio de sesión ni roles (S-10 a S-13) | DAC corre en el computador del analista, en `localhost`, solo con datos ficticios | Se despliegue en una red o se carguen datos reales. **Es la condición para usar datos reales** |
| **DT-14** Sin HTTPS | Sin despliegue no hay tráfico por la red | Se despliegue en cualquier servidor |
| **DT-15** Sin revisión automática de dependencias | Las librerías son las de Spring Boot 3.3, que el equipo mantiene actualizadas a mano | Pase más de un semestre sin actualizar, o se despliegue |
| **DT-09** La carga simultánea no tiene prueba automática | La base impide el duplicado y la respuesta ya es un 409 claro | Varios analistas usen el sistema al mismo tiempo |

## 4. Estabilización

- **Defectos corregidos en este módulo:** error 500 con nombres largos,
  error 500 con montos negativos, error 500 con cargas simultáneas,
  mensajes de error sin explicación y lentitud de la carga con archivos
  grandes.
- **Alcance congelado:** no se agregan historias nuevas. Lo que queda es la
  deuda declarada arriba y en [deuda-tecnica.md](deuda-tecnica.md).
- **Release candidate:** se etiqueta `v0.4.0-rc1` en `main` cuando se
  fusionen el PR #5 y este PR con el CI en verde.
