# Uso de IA en el proyecto

## Herramienta usada
Claude (Anthropic), como apoyo para diseño, documentación y explicación
de código.

## En qué se usó
- Redacción inicial de la visión del producto y de los documentos de
  proceso, revisada y ajustada por el equipo.
- Explicación de conceptos de Git/GitHub para integrantes sin experiencia
  previa (ramas, Pull Requests, fusión).
- Generación del esqueleto de código inicial (`src/contratos.py`,
  `src/alertas.py`) y sus pruebas unitarias, explicado línea por línea al
  equipo antes de aceptarlo.
- Revisión de consistencia entre documentos y código: sacar a la luz
  comportamientos que estaban implícitos en `src/` y no escritos en
  ninguna parte (ver [reglas-de-negocio.md](reglas-de-negocio.md)).

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
