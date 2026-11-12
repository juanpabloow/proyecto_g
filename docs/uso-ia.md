# Uso de IA en el proyecto

## Herramientas usadas

| Herramienta | Qué es | Para qué la usamos |
|---|---|---|
| **Claude Code** (Anthropic) | Asistente que trabaja sobre el repositorio desde la terminal: lee el código, ejecuta comandos y pruebas, y propone o hace cambios | Código, pruebas, auditorías del repositorio, mediciones y documentación técnica |
| **Claude (chat)** (Anthropic) | Conversación en el navegador, sin acceso al repositorio | Planificación, preparación de la presentación, lectura de las guías del curso y redacción de los prompts que luego le dimos a Claude Code. [COMPLETAR: confirmar y detallar] |

Las secciones "Declaración…" de más abajo se escribieron al cerrar cada
entrega. El registro por actividad las resume con los mismos campos para
todas.

## Registro por actividad

Cada actividad dice: herramienta, para qué, qué se aceptó, qué se
modificó, qué se rechazó y por qué, cómo se verificó, la decisión humana
final y el riesgo que permanece. Lo marcado **[COMPLETAR]** solo lo puede
escribir el equipo.

### A1. Visión, stakeholders y documentos de proceso (agosto de 2026)
- **Herramienta:** Claude [COMPLETAR: chat o Claude Code].
- **Para qué:** primer borrador de la visión del producto, los stakeholders y los documentos de proceso.
- **Aceptado:** la estructura y los borradores, después de revisarlos.
- **Modificado:** [COMPLETAR: qué partes reescribió el equipo].
- **Rechazado y por qué:** [COMPLETAR].
- **Cómo se verificó:** revisión del equipo contra el enunciado del curso. [COMPLETAR: quién revisó].
- **Decisión humana final:** el alcance y el problema duro los eligió el equipo (Decisiones 1 y 2).
- **Riesgo que permanece:** texto que el equipo no sepa defender. Se mitiga con la Definition of Done: cada integrante debe poder explicarlo sin la IA.

### A2. Explicación de Git y GitHub
- **Herramienta:** Claude [COMPLETAR: chat o Claude Code].
- **Para qué:** explicar ramas, Pull Requests y fusión a integrantes sin experiencia previa.
- **Aceptado / modificado / rechazado:** [COMPLETAR].
- **Cómo se verificó:** el historial muestra el trabajo por ramas y Pull Requests (PR #1 a #10).
- **Decisión humana final:** trabajar siempre con rama y Pull Request, sin fusionar con el CI en rojo.
- **Riesgo que permanece:** ninguno relevante.

### A3. Esqueleto inicial de código: carga del CSV y reincidencias (agosto de 2026)
- **Herramienta:** Claude [COMPLETAR: chat o Claude Code].
- **Para qué:** generar el primer código (en Python) y sus pruebas unitarias.
- **Aceptado:** el código, después de que se explicara línea por línea al equipo.
- **Modificado:** se reimplementó en Java y se consolidó en un solo lenguaje (Decisiones 14 y 18).
- **Rechazado y por qué:** [COMPLETAR].
- **Cómo se verificó:** pruebas automáticas, portadas a JUnit al consolidar.
- **Decisión humana final:** consolidar en Java + Spring Boot (Decisión 18).
- **Riesgo que permanece:** ninguno: ese código ya no existe en el repositorio.

### A4. Revisión de consistencia entre documentos y código
- **Herramienta:** Claude [COMPLETAR: chat o Claude Code].
- **Para qué:** encontrar comportamientos que estaban en el código y no escritos en ninguna parte.
- **Aceptado:** documentar las reglas en [reglas-de-negocio.md](reglas-de-negocio.md) (Decisión 8).
- **Modificado / rechazado:** [COMPLETAR].
- **Cómo se verificó:** cada regla cita su clase y su prueba.
- **Decisión humana final:** las decisiones de negocio pendientes se documentan, no se inventan (Decisión 11).
- **Riesgo que permanece:** que documento y código vuelvan a separarse. Se revisó otra vez en la entrega final.

### A5. Backlog completo, interfaz web y CI (PR #5, octubre de 2026)
- **Herramienta:** Claude [COMPLETAR: chat o Claude Code].
- **Para qué:** código y pruebas de HU-03, HU-05, HU-06 y HU-08, interfaz web, flujo de CI y primer borrador de su documentación.
- **Aceptado:** el código y las pruebas, revisados en el Pull Request.
- **Modificado:** los valores de fraccionamiento y de normalización que propuso la IA quedaron como **provisionales y configurables** (Decisiones 19 y 20).
- **Rechazado y por qué:** [COMPLETAR].
- **Cómo se verificó:** la suite completa (63 pruebas en ese momento) en verde en GitHub Actions contra PostgreSQL.
- **Decisión humana final:** el equipo definió las 8 historias y su prioridad; la IA trabajó sobre ese backlog.
- **Riesgo que permanece:** reglas que nadie del negocio validó ([DT-02](deuda-tecnica.md)).

### A6. Módulo 7: seguridad y RNF (PR #6, octubre de 2026)
- **Herramienta:** Claude [COMPLETAR: chat o Claude Code].
- **Para qué:** checklist de seguridad, ataques simulados, secretos fuera del repositorio, RNF medidos.
- **Aceptado, modificado y rechazado:** ver la tabla *Registro de sugerencias de la IA* en la declaración del Módulo 7, más abajo. Por ejemplo, se **modificó** la etiqueta `v0.4.0-rc1` a `v0.3.0-rc1`, y se **rechazó** desplegar sin HTTPS.
- **Cómo se verificó:** CI en verde, con 82 pruebas en ese momento.
- **Decisión humana final:** las de esa tabla; la prueba de cargas simultáneas se le asignó a QA.
- **Riesgo que permanece:** la deuda de seguridad declarada ([deuda-tecnica.md](deuda-tecnica.md)).

### A7. Observabilidad, k6 y OWASP Top 10:2025 (noviembre de 2026)
- **Herramienta:** Claude [COMPLETAR: chat o Claude Code].
- **Para qué:** Actuator y Micrometer, el stack de Prometheus y Grafana, la prueba de carga con k6 y el mapeo OWASP.
- **Aceptado:** todo lo anterior.
- **Modificado:** el mapeo OWASP se volvió estricto en la entrega final (A8 abajo).
- **Rechazado y por qué:** la revisión de dependencias en el CI, porque alarga mucho cada ejecución (DT-15).
- **Cómo se verificó:** la medición de k6 del 4/11. Al auditar se encontró que el script dejó de funcionar al entrar el login; se corrigió en la entrega final.
- **Decisión humana final:** agregar el inicio de sesión en la misma entrega.
- **Riesgo que permanece:** ver DT-17 y DT-24.

### A8. Inicio de sesión (PR #9, noviembre de 2026)
- **Herramienta:** Claude [COMPLETAR: chat o Claude Code].
- **Para qué:** Spring Security con un usuario, clave en hash BCrypt, pantalla de login.
- **Aceptado:** la implementación y la prueba de 401.
- **Modificado:** [COMPLETAR].
- **Rechazado y por qué:** los roles, que se dejaron como deuda DT-03.
- **Cómo se verificó:** CI en verde.
- **Decisión humana final:** hacerlo en una rama aparte con Pull Request.
- **Riesgo que permanece:** CSRF desactivado y sin límite de intentos (DT-16, DT-20).

### A9. Auditorías de solo lectura y entrega final (PR #10, 11 de noviembre de 2026)
- **Herramienta:** Claude Code.
- **Para qué:** auditar el repositorio contra el checklist de clase y la guía de entrega, y luego cerrar los huecos: k6 con autenticación, EXPLAIN ANALYZE, Actuator y cabeceras, credenciales y puertos, Maven Wrapper, documentos de la guía, trazabilidad, Mockito, protocolo de medición y guion de demostración.
- **Aceptado:** los cambios del PR #10, un commit por tarea.
- **Modificado:** al medir, k6 con HTTP Basic no cumplió RNF-3. En vez de cambiar el objetivo, se agregó un modo con sesión (el uso real) y se reportaron las dos cifras. [COMPLETAR: confirmar que el equipo está de acuerdo con este criterio].
- **Rechazado y por qué:** quedaron fuera de alcance, por decisión del equipo, los roles, el usuario de base de datos sin superusuario, CSRF, el límite de intentos, HTTPS y el cifrado en reposo. Se documentaron como deuda.
- **Cómo se verificó:** la suite completa con `./mvnw verify`, el CI del PR #10 y el guion [demo-entrega-final.md](demo-entrega-final.md) ejecutado desde una base limpia (la interfaz, en Chrome conducido por un script).
- **Decisión humana final:** el alcance, la regla "nada queda como cumple si no se demuestra en vivo" y la revisión del PR por otro integrante antes de fusionar. [COMPLETAR: quién revisó y fusionó].
- **Riesgo que permanece:** el volumen de documentación generada; cada integrante debe poder explicar su parte. Las secciones de reflexión y el video son del equipo.

### A10. Claude (chat): planificación, presentación, guías y prompts
- **Herramienta:** Claude (chat).
- **Para qué:** [COMPLETAR: planificación de las entregas, preparación de la presentación y del video, lectura de las guías del curso y redacción de los prompts para Claude Code].
- **Aceptado:** [COMPLETAR].
- **Modificado:** [COMPLETAR].
- **Rechazado y por qué:** [COMPLETAR].
- **Cómo se verificó:** [COMPLETAR].
- **Decisión humana final:** [COMPLETAR].
- **Riesgo que permanece:** [COMPLETAR].

## En qué se usó
- Redacción inicial de la visión del producto y de los documentos de
  proceso, revisada y ajustada por el equipo.
- Explicación de conceptos de Git/GitHub para integrantes sin experiencia
  previa (ramas, Pull Requests, fusión).
- Generación del esqueleto de código inicial —la carga del CSV y la
  detección de reincidencias— y sus pruebas unitarias, explicado línea por
  línea al equipo antes de aceptarlo.
- Revisión de consistencia entre documentos y código: sacar a la luz
  comportamientos que estaban implícitos en el código y no escritos en
  ninguna parte (ver [reglas-de-negocio.md](reglas-de-negocio.md)).

## Declaración de esta entrega: 8 historias, interfaz y deuda técnica

Usamos **Claude (Anthropic)** como asistente, desde nuestros computadores
y con nuestras propias cuentas. El trabajo se subió al repositorio desde
la cuenta de GitHub de cada integrante, mediante una rama y un Pull
Request.

**En qué nos ayudó la IA:**

- Generar el código de las historias que faltaban (HU-03, HU-05, HU-06 y
  HU-08) y sus pruebas, a partir del backlog, las reglas de negocio y las
  decisiones que ya teníamos en el repositorio.
- Proponer los valores iniciales del fraccionamiento (tope de
  $100.000.000 y ventana de 30 días) y qué diferencias de escritura
  ignorar al comparar nombres.
- Construir la interfaz web (HTML, CSS y JavaScript) y el flujo de
  integración continua.
- Redactar el primer borrador de la documentación de esta entrega:
  historias desarrolladas, reglas R-5, R-9 y R-10, Decisiones 19 a 23 e
  informe de deuda técnica.

**Lo que hicimos nosotros:**

- Definimos las 8 historias, su prioridad y el alcance desde las semanas
  2 y 3; la IA trabajó sobre ese backlog.
- Decidimos dejar las reglas de HU-03 y HU-05 como **provisionales** y
  configurables, porque no las ha validado un analista real (DT-02 en
  [deuda-tecnica.md](deuda-tecnica.md)).
- Revisamos el código, las pruebas y los documentos en el Pull Request
  antes de fusionarlo, y corregimos lo que no correspondía.
- Verificamos que la suite completa (63 pruebas) pase en GitHub Actions
  contra PostgreSQL real. Nada se fusiona a `main` con el CI en rojo.

## Declaración del Módulo 7: seguridad y requisitos no funcionales

Usamos el mismo asistente (Claude), desde nuestros computadores. El
trabajo se subió desde la cuenta de GitHub de Gerson Rojo en la rama
`seg/modulo-7` y el Pull Request #6.

**Instrucciones que le dimos:**

1. Le pasamos lo que pide el Módulo 7 (la ruta de las semanas 13 y 14 y
   la evidencia que se entrega) y le preguntamos qué se había hecho y qué
   faltaba en el proyecto.
2. Le pedimos completar lo que faltaba: checklist de seguridad con
   hallazgos, pruebas de ataques simulados, secretos fuera del
   repositorio, los dos RNF medidos con método y la documentación,
   respetando las reglas del curso (rama, Pull Request, pruebas en CI).
3. Le pedimos que todo quedara acorde al proyecto y que la declaración de
   IA dijera las instrucciones que le dimos y qué se aceptó o no.
4. Le pasamos las infografías de las semanas 13 y 14, le pedimos revisar
   el trabajo contra ellas y completar lo que faltara (evidencias por
   semana, gráficas de los RNF y la versión del release).

**Registro de sugerencias de la IA:**

| Sugerencia | Qué decidimos |
|---|---|
| Declarar los dos RNF: rendimiento (5.000 contratos en ≤ 5 s) e integridad ante recargas (10 recargas, 0 duplicados) | **Aceptada.** No encontramos en el repositorio los RNF candidatos de la semana 3, así que quedan declarados en este módulo |
| Medir los RNF en GitHub Actions con datos ficticios de semilla fija | **Aceptada** (Decisión 26) |
| Leer la clave de la base de un archivo `.env` | **Aceptada** (Decisión 24) |
| Cabeceras de seguridad, validación en la frontera, errores sin detalles y registro de auditoría sin nombres | **Aceptadas** |
| Cambiar la carga para no consultar la base fila por fila | **Aceptada** (Decisión 25) |
| Etiquetar el release candidate como `v0.4.0-rc1` | **Modificada:** el curso llama a esta entrega *Release 0.3*, así que la etiqueta es `v0.3.0-rc1` (Decisión 28) |
| Escribir la prueba automática de cargas simultáneas (DT-09) | **Modificada:** la escribe Yerson Pérez (QA) en su propio Pull Request, con una guía paso a paso, para que la prueba sea suya |
| Mover toda la evidencia de seguridad a `docs/seguridad/` | **Modificada:** el documento principal sigue en `docs/seguridad-y-rnf.md` (lo pide la ruta del módulo) y en `docs/seguridad/` quedan los resúmenes por semana que piden las infografías |
| Primer borrador de esta declaración | **Modificada:** la reescribimos para decir con nuestras palabras desde dónde se hizo el trabajo, qué instrucciones dimos y qué decidimos nosotros |
| Agregar inicio de sesión y roles | **No se aplica en este alcance**: queda como deuda DT-03, condición para usar datos reales |
| Agregar la revisión automática de dependencias (OWASP Dependency-Check) al CI | **No se aplica**: alarga mucho cada ejecución del CI; queda como deuda DT-15 |
| Desplegar el release candidate en un servicio gratuito (nivel avanzado, opcional) | **No se aplica**: sin inicio de sesión ni HTTPS no es seguro publicarlo |

## Declaración de esta entrega: observabilidad, carga (k6) y OWASP 2025

Usamos **Claude (Anthropic)** como asistente, desde nuestros computadores.
Esta entrega responde al checklist de clase del 28 de octubre de 2026.

**En qué nos ayudó la IA:**

- Revisar todo el proyecto contra el checklist y decirnos, con evidencia del
  código, qué ya cumplía y qué faltaba.
- Agregar observabilidad: Spring Boot Actuator + Micrometer
  (`/actuator/health` y `/actuator/prometheus`) y el stack opcional de
  Prometheus + Grafana en `observabilidad/`.
- Escribir la prueba de carga con k6 (`rnf/k6-concurrencia.js`) y ejecutarla:
  200 usuarios concurrentes, 0 % de errores, p95 de 771 ms (RNF-3).
- Mapear los controles de seguridad al OWASP Top 10:2025 (sección 6 de
  [seguridad-y-rnf.md](seguridad-y-rnf.md)), con la lista verificada contra el
  sitio oficial.
- Arreglar `docker-compose.yml` (unas comillas que rompían el YAML en Docker
  Compose v2+) para que `docker compose up -d` funcione.

**Lo que decidimos nosotros:**

- Al principio **no** agregamos inicio de sesión (la IA lo marcó como el único
  ítem que contradecía una decisión ya documentada, DT-03); tras revisarlo
  **decidimos agregarlo** en esta misma entrega (ver la declaración siguiente).
  **DT-15** (escaneo automático de dependencias) sí se queda como deuda por el
  tiempo de CI.
- Pedimos expresamente subir estos cambios a `main`.

## Declaración de esta entrega: inicio de sesión (login básico)

Usamos **Claude (Anthropic)**. Al ver que el checklist pedía "Login y
contraseñas", pedimos agregar un inicio de sesión básico.

**En qué nos ayudó la IA:**

- Spring Security con un solo usuario tomado del entorno y la clave guardada
  como **hash BCrypt**; formulario de login para el navegador y HTTP Basic
  para clientes y pruebas.
- Pantalla de login y botón de salir, respetando la CSP (sin scripts ni
  estilos en línea).
- Actualizar las pruebas E2E para que se autentiquen y una prueba nueva de que
  sin credenciales la API responde 401.
- Documentar la nueva deuda **DT-16** (CSRF desactivado en este alcance) y
  bajar DT-03 a "faltan roles".

**Lo que decidimos nosotros:**

- Hacerlo en una rama aparte con Pull Request (no directo a `main`) por el
  tamaño del cambio, y mantener los **roles** como deuda DT-03.

**Verificación:** 56 pruebas de dominio en verde, arranque de la app con los
endpoints de Actuator comprobados, k6 con 0 % de errores, y el CI de GitHub
Actions corriendo la suite completa sobre PostgreSQL real.

## Declaración de la entrega final: cierre de seguridad y RNF (11/11/2026)

Usamos **Claude (Anthropic)**, desde nuestros computadores. Primero le
pedimos una auditoría de solo lectura contra el checklist de clase del 28 de
octubre y luego le pedimos cerrar los huecos con cambios pequeños, en una
rama con un commit por tarea y un Pull Request que revisamos antes de
fusionar.

**En qué nos ayudó la IA:**

- Auditar el proyecto y encontrar, con evidencia, lo que el documento de
  seguridad afirmaba y ya no era cierto: el script de k6 sin autenticación,
  `/actuator/prometheus` abierto, el 401 sin cabeceras, el superusuario de la
  base y el texto de A04.
- Arreglar el script de k6 para que se autentique, escribir el generador de
  datos con semilla fija y ejecutar la medición con 200 usuarios. Encontró
  que con HTTP Basic en cada petición el p95 no cumple, por el costo de
  BCrypt, y lo reportamos así en lugar de esconderlo.
- Medir las consultas SQL con `EXPLAIN ANALYZE` sobre 5.000 contratos.
- Cerrar Actuator salvo `health`, mover las cabeceras a Spring Security para
  que también salgan en los 401, dejar la credencial demo solo en el perfil
  de desarrollo y publicar los puertos de Docker solo en `127.0.0.1`, con sus
  pruebas.
- Reescribir `seguridad-y-rnf.md` (superficie de entrada, datos sensibles,
  RNF de los ocho atributos, OWASP estricto y deuda al cierre) y escribir y
  **ejecutar** el guion [demo-entrega-final.md](demo-entrega-final.md).

**Lo que decidimos nosotros:**

- El alcance: dejar fuera los roles, el usuario de base de datos sin
  superusuario, CSRF, el límite de intentos de login, HTTPS y el cifrado en
  reposo, y documentarlos como deuda.
- La regla de que nada quede como "cumple" si no se puede demostrar en vivo.
- Revisar el Pull Request antes de fusionarlo.

**Verificación:** suite completa de 93 pruebas en verde contra PostgreSQL
real, y cada demostración del guion ejecutada desde una base limpia.

## Qué no se hizo con IA
- Las decisiones de alcance (qué señal cubrir primero, qué queda fuera
  del MVP) fueron discutidas y decididas por el equipo.
- Las decisiones de negocio pendientes (umbral de reincidencia, qué hacer
  con las filas incompletas) **no** se resolvieron con IA a propósito:
  corresponden al usuario del sistema, y contestarlas por nuestra cuenta
  produciría reglas que nadie validó
  ([Decisión 11](decisiones-tecnicas.md)).

## Postura del equipo frente al uso de IA
Usamos la IA como apoyo para acelerar tareas mecánicas y para aprender
conceptos nuevos, pero cada integrante es responsable de entender y poder
explicar el código y los documentos que entrega, sin depender de la IA
para sustentar el proyecto. Es también un criterio de la *Definition of
Done*: una historia no está terminada si nadie del equipo puede
explicarla sin ayuda ([backlog.md](backlog.md)).
