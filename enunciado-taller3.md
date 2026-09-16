Taller 3-Calificable (2026.09.09) - Diseño trazable del esqueleto andante

Requisitos de finalización.



Apertura: miércoles, 9 de septiembre de 2026, 19:00

Cierre: miércoles, 9 de septiembre de 2026, 21:10



**Del modelo a la primera rebanada: diseño trazable del esqueleto andante.**



En el desarrollo de software, un "esqueleto funcional" **(walking skeleton)** es una implementación mínima pero operativa de un sistema que conecta todos los componentes arquitectónicos principales de extremo a extremo. 



Este concepto, acuñado por el pionero de las metodologías ágiles Alistair Cockburn, demuestra que las vías de comunicación fundamentales funcionan correctamente antes de incorporar las funcionalidades completas.



Al tomar una porción de este esqueleto, se selecciona una única franja vertical de funcionalidad para desarrollarla primero y así demostrar que la arquitectura funciona de extremo a extremo. 



**Concepto clave:** La "rebanada vertical" (**vertical slice**). En lugar de construir el software horizontalmente (capa por capa: primero toda la base de datos, luego todo el **backend** y finalmente todo el **frontend**), una porción del esqueleto funcional atraviesa verticalmente todas las capas.



**Características clave:** 



**Extremo a extremo:** Abarca todas las capas, como la interfaz de usuario, el backend, la base de datos y la canalización de despliegue. 



**Funcionalidades mínimas:** Contiene una cantidad muy reducida de lógica de negocio o funcionalidad para el usuario. 



**Real y ejecutable:** Ejecuta código real y desplegable en lugar de utilizar objetos simulados (**mocks**) o esquemas estáticos.



**¿Por qué utilizar un esqueleto funcional?** 



**Reducción temprana de riesgos:** Permite probar integraciones, bases de datos y servicios de terceros que conllevan riesgos desde el primer día, en lugar de hacerlo al llegar a la fecha límite del proyecto. 



**Configuración temprana de CI/CD:** Facilita que los equipos de DevOps establezcan de inmediato canalizaciones automatizadas de compilación, pruebas y despliegue. 



**Base sólida:** A diferencia del Producto Mínimo Viable (MVP), centrado en el negocio, este enfoque asegura primero la arquitectura técnica. Los debates en DevOps Stack Exchange destacan cómo esta configuración inicial evita tener que  trabajar bajo presión más adelante.



En éste taller vamos de la etapa de modelado a la construcción. A partir de las evidencias que su equipo ya consolidó —visión, backlog, problema duro, modelo de dominio y modelo de datos— usted define **la primera rebanada ejecutable** de su sistema: el flujo más delgado que atraviesa todas las fronteras técnicas de punta a punta.



Pueden apoyarse en el Tutor de Ingeniería de Software o en asistentes de IA para producir el borrador. Lo que se evalúa no es el borrador, sino **su trazabilidad:** cada elemento de sus diagramas debe apuntar a una línea concreta de sus propias evidencias, y todo lo que no tenga respaldo debe quedar declarado como vacío. Un entregable impecable sin trazabilidad verificable se califica por debajo de uno modesto y honesto. Cómo estudiante de Ingeniería de Software DEBE poder defender su trabajo.



**Tutor de Ingeniería de Software**



Actúa como Ingeniero de Software Senior guiando la construcción de un Esqueleto Andante.



**INSUMOS (adjuntos; son la ÚNICA fuente de verdad):**



* vision-producto.md
* backlog.md
* problema-duro.md
* domain-model.md
* data-model.md



**REGLA DE FIDELIDAD (obligatoria):**



No inventes entidades, atributos, operaciones, tablas, relaciones, reglas, endpoints, restricciones que no aparezcan en los insumos. Si algo necesario para el flujo no está definido, NO lo completes: detente y lista lo que falta bajo el encabezado "VACÍOS DETECTADOS", indicando en qué archivo debería estar. Un vacío declarado vale más que un supuesto plausible.



**TAREA 1 — Selección de rebanada.**



Propón 2 candidatas a rebanada del esqueleto y elige una. Criterio de decisión: máximo número de fronteras técnicas cruzadas (HTTP/UI, aplicación, dominio, persistencia real, migración) con el mínimo de reglas de negocio. Justifica en 3 líneas por qué la descartada tiene más dominio o menos integración.

Nombra explícitamente las fronteras que cruza la elegida.



**TAREA 2 — Diagrama de secuencia (artefacto principal).**



Genera en Mermaid el diagrama de secuencia SOLO de la rebanada elegida,

desde el actor externo hasta la base de datos y de vuelta. Requisitos:



\- un participante por frontera técnica, no por clase

\- muestra el retorno, no solo la ida

\- marca con nota dónde ocurre la transacción y dónde el mapeo a persistencia

\- incluye la ruta de error más simple (entrada inválida o recurso inexistente)

\- prohibido añadir participantes que no sean necesarios para este flujo



Comprueba:



* que representa solamente la rebanada elegida;
* que empieza fuera del proceso;
* que llega hasta persistencia real;
* que muestra el retorno;
* que existe un participante por frontera técnica y no una proliferación de clases;
* que señala dónde comienza/termina la transacción;
* que identifica dónde ocurre el mapeo dominio ↔ persistencia;
* que contiene solamente la ruta de error mínima requerida;
* que ningún participante existe por “arquitectura habitual” sin evidencia.



**TAREA 3 — Diagrama de clases (derivado).**



Genera en Mermaid un diagrama de clases que contenga EXCLUSIVAMENTE las clases que aparecen como participantes o mensajes en la Tarea 2. Para cada una indica su capa. Marca cuáles son interfaces (puertos) y cuáles implementaciones.



Si una clase del domain-model.md no aparece en la Tarea 2, NO la incluyas.



Comprueba que aparezcan EXCLUSIVAMENTE elementos requeridos por la secuencia.



**TAREA 4 — Contrato de la prueba única**



A partir del diagrama de secuencia, escribe en prosa la especificación de UNA prueba end-to-end: punto de entrada, precondición de datos, acción, aserción observable, y estado esperado en la base de datos real. La aserción debe poder fallar si cualquiera de las fronteras está rota.



**Comprueba si la prueba podría fallar cuando se rompe cualquiera de estas fronteras:**



* entrada externa;
* aplicación;
* mapeo;
* driver;
* migración;
* base de datos;
* respuesta.



Identifica cualquier frontera que pueda romperse sin que la prueba lo detecte.



**TAREA 5 — Trazabilidad**



Tabla de 3 columnas: elemento del diagrama | archivo de origen | línea o sección que lo respalda. Toda fila sin origen verificable va a VACÍOS DETECTADOS.



**SALIDA:** en el orden indicado, sin preámbulo. Diagramas en bloques ```mermaid.



