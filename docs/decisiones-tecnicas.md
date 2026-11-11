# Decisiones técnicas

Registro de las decisiones que tomamos, con la alternativa que
descartamos y el motivo. Sirve para que cualquiera —incluido el equipo
dentro de dos meses— entienda por qué el proyecto está como está.

## Decisiones de alcance y tecnología

| # | Decisión | Alternativa descartada | Por qué |
|---|---|---|---|
| 1 | Elegimos resolver **idempotencia** como problema duro de esta entrega | Concurrencia | Es más frecuente en el flujo manual real (archivos reenviados o duplicados) y es más simple de resolver bien en el tiempo disponible |
| 2 | El MVP cubre **una sola señal de alerta**: reincidencia contratista-funcionario | Incluir también fraccionamiento desde ya | Preferimos una funcionalidad bien sustentada que cuatro a medias; el fraccionamiento queda documentado como trabajo futuro ([HU-03](backlog.md)) |
| 3 | Los datos se cargan desde un archivo **CSV** | Conectar directo a una base de datos o a SECOP | No hay tiempo ni necesidad todavía; un CSV es suficiente para demostrar el flujo completo end-to-end |
| 4 | Lenguaje: **Python** | Java / JavaScript | Es el más simple de leer y explicar para el equipo, y tiene librerías simples para manejar datos tabulares. **Superada por las Decisiones 14 y 18:** el proyecto es hoy Java + Spring Boot |
| 14 | El backend de la rebanada se construye en **Java 21 + Spring Boot 3.3 + PostgreSQL 16**, con el CSV entrando por `POST /api/contratos/cargar` | Seguir con el módulo Java plano (`java/src/dac/`) y conectar JDBC a mano; o hacer la rebanada en Python | El taller del esqueleto andante exige cruzar la frontera HTTP/UI y llegar a persistencia real. Spring Data resuelve la conexión y el mapeo, que era justo el trabajo que el módulo plano no había hecho. Revisa la Decisión 4: el backend ya no es Python. Consecuencia declarada: `java/src/dac/` y sus 26 pruebas quedan huérfanos (ver [rebanada §6.C](../esqueleto/rebanada.md)) |
| 15 | El esquema lo gobierna **`db/schema.sql`**, no Hibernate (`spring.jpa.hibernate.ddl-auto=none`) | Dejar que Hibernate genere las tablas con `ddl-auto=update` | El modelo físico es una evidencia del proyecto y está revisado ([esquema-bd.md](esquema-bd.md)); si Hibernate lo generara, el esquema real dejaría de coincidir con el documentado y las restricciones que sostienen el problema duro quedarían a merced del mapeo |
| 18 | **Un solo lenguaje: Java + Spring Boot.** Se elimina la implementación en Python (`main.py`, `src/`, `tests/`) y el módulo Java plano (`java/src/dac/`, `java/test/`) | Mantener las dos implementaciones; o consolidar en Python | Eran dos programas que hacían casi lo mismo y **no se comportaban igual**: ante una fila sin contratista, Java la descartaba (HU-04) y Python generaba la alerta sobre nadie. Dos verdades sobre la misma regla es peor que una sola incompleta. Se consolidó en Java porque es donde está la rebanada del esqueleto andante, que cruza las cinco fronteras. **No se perdió nada:** las 12 pruebas de Python y las 26 del módulo plano se portaron a JUnit (28 pruebas hoy), y la persistencia de alertas, que solo tenía Python, se implementó en Java (`AlertaJpa`, `AlertaRepo`) |
| 17 | `monto` y `fecha` **admiten NULL** en el modelo físico | Dejarlos `NOT NULL` y descartar la fila; o rellenarlos con 0 y una fecha centinela | [R-6](reglas-de-negocio.md) dice que monto o fecha vacíos **no** descartan la fila, y el modelo lógico ([esquema-bd.md](esquema-bd.md)) no los marca obligatorios: el `NOT NULL` del `schema.sql` no tenía respaldo. Con él, una fila sin monto devolvía `400` y anulaba el archivo completo, justo lo que la Decisión 12 rechaza. Rellenar con valores centinela habría inventado datos. Hallazgo de la rebanada: ver [rebanada §6.E](../esqueleto/rebanada.md) |
| 16 | La prueba única entra por **HTTP real** (`WebEnvironment.RANDOM_PORT` + `TestRestTemplate`) | `MockMvc`; o llamar directo a `ContratoServicio` | El enunciado exige código "real y ejecutable, no objetos simulados". Llamar al servicio dejaba la frontera HTTP sin cubrir: una ruta mal mapeada pasaba inadvertida. Con Tomcat embebido en un socket real, la prueba falla si cualquiera de las cinco fronteras se rompe |

## Decisiones sobre el problema duro

| # | Decisión | Alternativa descartada | Por qué |
|---|---|---|---|
| 5 | La clave de un contrato es **`numero_contrato` + `entidad`** | Solo `numero_contrato`; o la fila completa | Dos entidades pueden repetir el consecutivo "001"; y usar la fila completa dejaría pasar el duplicado con un monto corregido. Detalle en [problema duro §3](problema-duro.md) |
| 6 | Ante dos filas con la misma clave y datos distintos, **gana la primera** | Gana la última; o registrar el conflicto | Es lo más simple y predecible para el MVP. La limitación queda visible y registrada como [HU-06](backlog.md) |
| 7 | La idempotencia se limita a **una carga**, sin persistencia | Guardar en base de datos ya en esta entrega | Sin base de datos, prometer idempotencia entre ejecuciones sería falso. Se documentó el alcance real en vez de exagerarlo ([HU-07](backlog.md)). **Superada por la Decisión 14:** ya hay persistencia y HU-07 está hecha |

## Decisiones de proceso y datos

| # | Decisión | Alternativa descartada | Por qué |
|---|---|---|---|
| 8 | La lógica de negocio se documenta en [reglas-de-negocio.md](reglas-de-negocio.md), aparte del código | Dejarla implícita en el código | Estaba solo en el código: había que leerlo para saber qué cuenta como alerta. Ahora las reglas —incluidas las limitaciones— son revisables por alguien que no programa (S-1, S-12) |
| 9 | **No** normalizamos nombres de contratistas/funcionarios en este MVP | Aplicar mayúsculas/tildes/sufijos por nuestra cuenta | Normalizar de más une empresas distintas de nombre parecido y señala a alguien por error (S-6). Requiere validación con un analista ([R-5](reglas-de-negocio.md)) |
| 10 | El CSV de ejemplo **se versiona**; los datos reales van en `data/privado/` (ignorado) | Ignorar toda la carpeta `data/` | Ignorar `data/` completa dejaba el repositorio sin el archivo que las pruebas necesitan: un clon nuevo no podía ejecutar el proyecto ([R-7](reglas-de-negocio.md)) |
| 11 | Las decisiones de negocio sin resolver **se documentan como pendientes**, no se resuelven por cuenta técnica | Elegir nosotros el umbral, el manejo de filas incompletas, etc. | Son decisiones del usuario (S-1). Inventarlas produciría un sistema que "funciona" con reglas que nadie validó ([R-3](reglas-de-negocio.md), [HU-04](backlog.md)) |
| 12 | Una fila incompleta **se descarta y se reporta**; el archivo sigue siendo válido | Aceptarla (lo que se hacía, [R-6](reglas-de-negocio.md)); o invalidar el archivo completo | Aceptarla produce una alerta del par `("", "")`: una alerta sobre nadie. Invalidar el archivo deja al analista sin nada por una fila mala entre 5.000. Y descartar en silencio esconde un problema de calidad de datos, por eso se informa cuántas y por qué — que es lo que el criterio 2 de [HU-04](backlog.md) ya pedía |
| 13 | *(Resuelta por la Decisión 18)* El módulo [`java/`](../java/) reimplementa HU-01, HU-02 y HU-04; el código Python queda intacto | Migrar todo a Java de una vez; o hacer HU-04 solo en Python | HU-04 en Java necesitaba la base (carga + detección) reimplementada. La duplicación fue deliberada y temporal; duró una semana y se cerró con la Decisión 18 |

## Sobre la Decisión 12: una excepción consciente a la Decisión 11

La Decisión 11 dice que las decisiones de negocio sin resolver se
documentan como pendientes en vez de resolverlas por cuenta técnica. La
Decisión 12 es una **excepción explícita**, y conviene que se note:

- La historia llevaba semanas bloqueada por una pregunta que su propio
  criterio de aceptación 2 ya respondía a medias (*"me informa cuántas
  filas se descartaron y por qué"* solo tiene sentido si la fila se
  descarta y el archivo sobrevive).
- Es **provisional y reversible**: cambiar la política es cambiar la
  lista `CAMPOS_OBLIGATORIOS` en `CargadorDeContratos`, no reescribir la
  carga.
- **Sigue pendiente de confirmación con S-1.** Si el analista dice que
  una fila incompleta debe invalidar el archivo, la decisión cambia y el
  código detrás también.

## Decisiones de la segunda mitad del backlog

| # | Decisión | Alternativa descartada | Por qué |
|---|---|---|---|
| 19 | **HU-05:** los nombres se comparan normalizados ignorando mayúsculas, espacios sobrantes, tildes, puntos y comas; se conserva la ñ y **no** se quitan sufijos ni se usan alias. Es **provisional** (excepción a la Decisión 11, como la 12) | Mantener la comparación exacta (Decisión 9); o normalizar también sufijos societarios y alias | La comparación exacta era la limitación más grande del MVP: escondía reincidencias reales. Se eligió el conjunto más conservador: solo diferencias que no cambian la identidad. Quitar sufijos uniría "ACME SAS" con "ACME LTDA", que pueden ser empresas distintas (S-6). Revierte parcialmente la Decisión 9. Pendiente de S-1 |
| 20 | **HU-03:** fraccionamiento = mismo contratista y entidad, 2+ contratos bajo el tope, en una ventana de **30 días**, que suman el tope de **$100.000.000**. Tope y ventana son parámetros en `application.properties`. Es **provisional** | Esperar a S-1 para implementarla; o fijar los valores en el código | La historia estaba bloqueada por las mismas preguntas que la HU-04. Con parámetros, validarla con un analista es cambiar dos líneas de configuración, no el código. Las alertas de fraccionamiento se calculan al consultar y **no se guardan** (ver deuda DT-01), para no cambiar el modelo físico revisado en esta entrega |
| 21 | **Interfaz web** en HTML, CSS y JavaScript sin librerías, servida por el mismo Spring Boot (`src/main/resources/static`) | React/Angular en un proyecto aparte; o seguir solo con `curl` | Un solo comando levanta todo, no hay servidor de Node ni compilación de frontend, y el equipo puede leer el código completo. Usa la misma API que las pruebas, así que la interfaz no tiene lógica de negocio propia |
| 22 | **Integración continua** con GitHub Actions: cada push y cada Pull Request levanta PostgreSQL 16 con `db/schema.sql` y corre la suite (hoy `./mvnw -B verify`, Decisión 29) | Seguir corriendo las pruebas a mano (pendiente 1 del mapa) | La regla del curso es "rama principal siempre verde". Sin CI dependía de que alguien se acordara de correr las pruebas antes de fusionar |
| 23 | El error de carga responde **400 con `{"error": "..."}`** en vez de un 400 sin cuerpo | Mantener el 400 vacío | La pantalla necesita decirle al analista *qué* columna falta; antes solo podía decir "error" |

## Decisiones del Módulo 7 (seguridad y RNF)

| # | Decisión | Alternativa descartada | Por qué |
|---|---|---|---|
| 24 | La clave de la base se lee de **`.env`** (ignorado por Git, plantilla `.env.example`) o de variables de entorno; sin ella la aplicación no arranca. En el CI se usa una base desechable | Dejar la clave en `application.properties`; o usar *secrets* de GitHub | La clave estaba publicada en el repositorio (DT-04). Los *secrets* exigen permisos de administrador del repositorio; la base del CI se destruye al terminar y no guarda datos reales |
| 25 | La carga **lee lo existente una sola vez** y trabaja con índices en memoria | Seguir consultando por cada fila; o escribir la detección en SQL | Con 5.000 filas, las consultas por fila hacían que Hibernate revisara todo lo cargado en cada consulta. La lectura única cumple el RNF-1 sin cambiar el modelo; la detección en SQL queda para cuando el volumen lo exija (DT-06) |
| 26 | Los **RNF se miden en el CI** con datos ficticios de semilla fija | Medir una vez a mano en un computador del equipo | Una cifra sin método no sirve: en el CI el método, los datos y la máquina son siempre los mismos y la medición se repite en cada cambio |
| 27 | El registro de auditoría guarda **cantidades, no nombres** | Registrar el contenido de cada carga | El log sería otra copia de datos sensibles (S-8) |
| 28 | El release candidate se etiqueta **`v0.3.0-rc1`** | `v0.4.0-rc1` | El curso llama a la entrega de la semana 14 *Release 0.3* (MVP funcional completo); usar el mismo número evita confusión al revisar |

## Decisiones de la entrega final

| # | Decisión | Alternativa descartada | Por qué |
|---|---|---|---|
| 29 | **Maven Wrapper en la raíz** con un POM agregador mínimo (`pom.xml` que solo declara el módulo `java/`): `./mvnw spring-boot:run` y `./mvnw verify` funcionan desde la raíz | Dejar el wrapper en `java/` y pedir `cd java`; o mover `java/pom.xml` y `src/` a la raíz | La guía de entrega pide esos comandos desde un clon limpio. El agregador no mueve código ni cambia `java/pom.xml`: si algo fallara, se borra un archivo y se vuelve a `cd java`. Mover el proyecto habría cambiado todas las rutas del CI y de la documentación. El agregador salta `spring-boot:run` (`<skip>true</skip>`) para que solo arranque el módulo |
| 30 | La **credencial demo** del analista vive solo en el perfil `dev` (`application-dev.properties`); fuera de él, sin `DAC_AUTH_USER` y `DAC_AUTH_PASSWORD` la aplicación no arranca | Mantener una credencial por defecto en `application.properties` | Una clave conocida que funciona sin configurar nada es un fallo de autenticación (OWASP A07). Las pruebas activan el perfil `dev` desde la configuración de surefire, así que no necesitan variables |
| 31 | Las **cabeceras de seguridad** las escribe Spring Security en todas las respuestas, y se elimina el filtro propio `CabecerasDeSeguridad` | Mantener el filtro propio | El filtro corría **después** de Spring Security: un 401 salía sin CSP ni `nosniff`. Dejar los dos habría duplicado las cabeceras |

## Patrones de diseño

Patrones que el código aplica, y uno que **decidimos no aplicar**, con su
argumento. Se citan las clases reales.

### Repository — aplicado

**Dónde:** `dac.persistencia.EntidadRepo`, `ContratistaRepo`,
`FuncionarioRepo`, `ContratoRepo` y `AlertaRepo`, que extienden
`JpaRepository` (Spring Data).

**Por qué:** `ContratoServicio` pide "los contratos con sus relaciones"
(`ContratoRepo.findAllConFetch()`), "todas las entidades"
(`EntidadRepo.findAll()`) o "guarda este contrato" (`ContratoRepo.save`)
sin escribir SQL. Spring Data genera consultas
**siempre parametrizadas**, y esa es la defensa contra la inyección SQL
(S-03, demostrada en `SeguridadE2ETest#inyeccion_sql_se_guarda_como_texto`).
Además, el único SQL escrito a mano (`findAllConFetch`) es JPQL con
`JOIN FETCH`, para traer las relaciones en una sola consulta.

**Costo aceptado:** las entidades JPA (`ContratoJpa`) son clases distintas
de las del dominio (`Contrato`) y hay que mapear entre ellas
(`ContratoServicio.aDominio`). Lo preferimos a anotar el dominio con JPA,
porque así el dominio se prueba sin base de datos (las 59 pruebas de
dominio no levantan Spring).

### Capa de servicio (fachada de aplicación) — aplicado

**Dónde:** `dac.aplicacion.ContratoServicio`. Los controladores
(`ContratoControlador`, `AlertaControlador`) solo traducen HTTP y delegan
en él.

**Por qué:** una carga es una sola transacción (`@Transactional`) que
cruza cuatro pasos: leer el CSV (`CargadorDeContratos`), validar montos y
fechas **antes** de escribir, guardar solo lo nuevo y detectar las dos
señales. Con la fachada, "una carga rechazada no deja nada a medias" se
cumple en un solo lugar, y lo prueba
`SeguridadE2ETest#monto_negativo` (0 contratos tras el rechazo). Si cada
controlador orquestara los repositorios, la regla se repetiría.

### Objeto de valor inmutable — aplicado

**Dónde:** `Contrato`, `Alerta`, `AlertaFraccionamiento`, `ResultadoCarga`,
`ConflictoDeDuplicado` y `FilaDescartada` son *records* de Java, y copian sus
listas con `List.copyOf`.

**Por qué:** la identidad de un contrato es su valor (`claveNatural()`),
no una referencia en memoria. Esa igualdad por valor es lo que hace
funcionar la deduplicación. La inmutabilidad, además, garantiza que una
alerta no pierda evidencia después de creada: el constructor compacto de
`Alerta` rechaza menos de 2 contratos, y nadie puede vaciarla después.

### Strategy para las señales — rechazado por ahora

**Qué sería:** una interfaz `Senal { List<?> detectar(List<Contrato>) }`
implementada por `DetectorDeReincidencias` y `DetectorDeFraccionamiento`,
y que `ContratoServicio` recorra una lista de señales.

**Por qué no:** las dos señales **no producen lo mismo**: `Alerta` señala un
par contratista–funcionario y se guarda en la base; `AlertaFraccionamiento`
señala un contratista en una entidad, lleva suma y tope, y no se guarda
(DT-01). Una interfaz común obligaría a un tipo de resultado genérico y a
`instanceof` al guardar y al exportar. Con dos señales, invocarlas por su
nombre en `ContratoServicio` es más claro.

**Cuándo cambiaría:** al agregar una tercera señal, o al persistir el
fraccionamiento con la misma tabla `alerta` (DT-01), que es cuando las
señales pasarían a compartir forma. Está anotado en RNF-8
([seguridad-y-rnf.md](seguridad-y-rnf.md)).

Nuevas decisiones se van agregando aquí a medida que el equipo avanza.
