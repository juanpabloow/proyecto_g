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
