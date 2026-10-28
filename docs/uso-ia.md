# Uso de IA en el proyecto

## Herramienta usada
Claude (Anthropic), como apoyo para diseño, documentación, explicación
de código y, desde la entrega de las 8 historias, implementación de
código y pruebas (ver la declaración de esa entrega más abajo).

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

Herramienta: **Claude (Anthropic)**, usada por Gerson Rojo.

| Entregable | Qué hizo la IA | Qué hizo el equipo |
|---|---|---|
| HU-03, HU-05, HU-06 y HU-08 (código y pruebas) | Propuso e implementó el código de dominio, el servicio, los controladores y las pruebas (unitarias y de punta a punta) a partir de las historias y las reglas del repositorio | Definió las historias, sus criterios y el alcance; revisa el Pull Request y debe poder explicar cada prueba antes de fusionarlo |
| Reglas provisionales de HU-03 y HU-05 | **Propuso** el tope de $100.000.000, la ventana de 30 días y qué diferencias de escritura ignorar | Las acepta como **provisionales** y las deja como parámetros; quedan pendientes de un analista real (DT-02 en [deuda-tecnica.md](deuda-tecnica.md)) |
| Interfaz web | Diseñó y escribió el HTML, CSS y JavaScript | Revisa que muestre lo que el analista necesita (evidencia, aviso de que DAC no acusa a nadie) |
| Integración continua | Escribió el flujo de GitHub Actions | Revisa que el PR quede en verde antes de fusionar |
| Documentación | Redactó el borrador de las historias desarrolladas, reglas R-5/R-9/R-10, Decisiones 19 a 23 y el informe de deuda técnica | Revisa y corrige; la priorización de la deuda se discute en equipo |

**Cómo se verificó lo que hizo la IA:** las pruebas de dominio se
ejecutaron antes de subir el código, y el Pull Request corre la suite
completa contra PostgreSQL real en GitHub Actions. Nada se fusiona a
`main` con la integración continua en rojo.

**Sobre los commits:** se suben desde la cuenta de GitHub de Gerson Rojo,
que es el responsable de la entrega. Que el código lo haya generado la
IA queda declarado aquí, no en cada commit.

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
