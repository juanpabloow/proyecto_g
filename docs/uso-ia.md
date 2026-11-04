# Uso de IA en el proyecto

## Herramienta usada
Claude (Anthropic), usado desde nuestros computadores como apoyo para
diseño, documentación, explicación de código y, desde la entrega de las
8 historias, generación de código y pruebas (ver la declaración de esa
entrega más abajo).

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
explicarla sin ayuda ([historias-usuario.md](historias-usuario.md)).
