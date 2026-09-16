# Decisiones técnicas

Registro de las decisiones que tomamos, con la alternativa que
descartamos y el motivo. Sirve para que cualquiera —incluido el equipo
dentro de dos meses— entienda por qué el proyecto está como está.

## Decisiones de alcance y tecnología

| # | Decisión | Alternativa descartada | Por qué |
|---|---|---|---|
| 1 | Elegimos resolver **idempotencia** como problema duro de esta entrega | Concurrencia | Es más frecuente en el flujo manual real (archivos reenviados o duplicados) y es más simple de resolver bien en el tiempo disponible |
| 2 | El MVP cubre **una sola señal de alerta**: reincidencia contratista-funcionario | Incluir también fraccionamiento desde ya | Preferimos una funcionalidad bien sustentada que cuatro a medias; el fraccionamiento queda documentado como trabajo futuro ([HU-03](historias-usuario.md)) |
| 3 | Los datos se cargan desde un archivo **CSV** | Conectar directo a una base de datos o a SECOP | No hay tiempo ni necesidad todavía; un CSV es suficiente para demostrar el flujo completo end-to-end |
| 4 | Lenguaje: **Python** | Java / JavaScript | Es el más simple de leer y explicar para el equipo, y tiene librerías simples para manejar datos tabulares |
| 14 | El backend de la rebanada se construye en **Java 21 + Spring Boot 3.3 + PostgreSQL 16**, con el CSV entrando por `POST /api/contratos/cargar` | Seguir con el módulo Java plano (`java/src/dac/`) y conectar JDBC a mano; o hacer la rebanada en Python | El taller del esqueleto andante exige cruzar la frontera HTTP/UI y llegar a persistencia real. Spring Data resuelve la conexión y el mapeo, que era justo el trabajo que el módulo plano no había hecho. Revisa la Decisión 4: el backend ya no es Python. Consecuencia declarada: `java/src/dac/` y sus 26 pruebas quedan huérfanos (ver [rebanada §6.C](../esqueleto/rebanada.md)) |
| 15 | El esquema lo gobierna **`db/schema.sql`**, no Hibernate (`spring.jpa.hibernate.ddl-auto=none`) | Dejar que Hibernate genere las tablas con `ddl-auto=update` | El modelo físico es una evidencia del proyecto y está revisado ([esquema-bd.md](esquema-bd.md)); si Hibernate lo generara, el esquema real dejaría de coincidir con el documentado y las restricciones que sostienen el problema duro quedarían a merced del mapeo |
| 17 | `monto` y `fecha` **admiten NULL** en el modelo físico | Dejarlos `NOT NULL` y descartar la fila; o rellenarlos con 0 y una fecha centinela | [R-6](reglas-de-negocio.md) dice que monto o fecha vacíos **no** descartan la fila, y el modelo lógico ([esquema-bd.md](esquema-bd.md)) no los marca obligatorios: el `NOT NULL` del `schema.sql` no tenía respaldo. Con él, una fila sin monto devolvía `400` y anulaba el archivo completo, justo lo que la Decisión 12 rechaza. Rellenar con valores centinela habría inventado datos. Hallazgo de la rebanada: ver [rebanada §6.E](../esqueleto/rebanada.md) |
| 16 | La prueba única entra por **HTTP real** (`WebEnvironment.RANDOM_PORT` + `TestRestTemplate`) | `MockMvc`; o llamar directo a `ContratoServicio` | El enunciado exige código "real y ejecutable, no objetos simulados". Llamar al servicio dejaba la frontera HTTP sin cubrir: una ruta mal mapeada pasaba inadvertida. Con Tomcat embebido en un socket real, la prueba falla si cualquiera de las cinco fronteras se rompe |

## Decisiones sobre el problema duro

| # | Decisión | Alternativa descartada | Por qué |
|---|---|---|---|
| 5 | La clave de un contrato es **`numero_contrato` + `entidad`** | Solo `numero_contrato`; o la fila completa | Dos entidades pueden repetir el consecutivo "001"; y usar la fila completa dejaría pasar el duplicado con un monto corregido. Detalle en [problema duro §3](problema-duro.md) |
| 6 | Ante dos filas con la misma clave y datos distintos, **gana la primera** | Gana la última; o registrar el conflicto | Es lo más simple y predecible para el MVP. La limitación queda visible y registrada como [HU-06](historias-usuario.md) |
| 7 | La idempotencia se limita a **una carga**, sin persistencia | Guardar en base de datos ya en esta entrega | Sin base de datos, prometer idempotencia entre ejecuciones sería falso. Se documentó el alcance real en vez de exagerarlo ([HU-07](historias-usuario.md)). **Superada por la Decisión 14:** ya hay persistencia y HU-07 está hecha |

## Decisiones de proceso y datos

| # | Decisión | Alternativa descartada | Por qué |
|---|---|---|---|
| 8 | La lógica de negocio se documenta en [reglas-de-negocio.md](reglas-de-negocio.md), aparte del código | Dejarla implícita en `src/` | Estaba solo en el código: había que leer Python para saber qué cuenta como alerta. Ahora las reglas —incluidas las limitaciones— son revisables por alguien que no programa (S-1, S-12) |
| 9 | **No** normalizamos nombres de contratistas/funcionarios en este MVP | Aplicar mayúsculas/tildes/sufijos por nuestra cuenta | Normalizar de más une empresas distintas de nombre parecido y señala a alguien por error (S-6). Requiere validación con un analista ([R-5](reglas-de-negocio.md)) |
| 10 | El CSV de ejemplo **se versiona**; los datos reales van en `data/privado/` (ignorado) | Ignorar toda la carpeta `data/` | Ignorar `data/` completa dejaba el repositorio sin el archivo que `main.py` necesita: un clon nuevo no podía ejecutar el proyecto ([R-7](reglas-de-negocio.md)) |
| 11 | Las decisiones de negocio sin resolver **se documentan como pendientes**, no se resuelven por cuenta técnica | Elegir nosotros el umbral, el manejo de filas incompletas, etc. | Son decisiones del usuario (S-1). Inventarlas produciría un sistema que "funciona" con reglas que nadie validó ([R-3](reglas-de-negocio.md), [HU-04](historias-usuario.md)) |
| 12 | Una fila incompleta **se descarta y se reporta**; el archivo sigue siendo válido | Aceptarla (lo que se hacía, [R-6](reglas-de-negocio.md)); o invalidar el archivo completo | Aceptarla produce una alerta del par `("", "")`: una alerta sobre nadie. Invalidar el archivo deja al analista sin nada por una fila mala entre 5.000. Y descartar en silencio esconde un problema de calidad de datos, por eso se informa cuántas y por qué — que es lo que el criterio 2 de [HU-04](historias-usuario.md) ya pedía |
| 13 | El módulo [`java/`](../java/) reimplementa HU-01, HU-02 y HU-04; el código Python queda intacto | Migrar todo a Java de una vez; o hacer HU-04 solo en Python | HU-04 en Java necesitaba la base (carga + detección) reimplementada. Revisa la Decisión 4 en parte: **hay duplicación deliberada** — HU-04 está hecha en Java y sigue pendiente en Python. Consolidar en un solo lenguaje es lo primero de la próxima entrega |

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

Nuevas decisiones se van agregando aquí a medida que el equipo avanza.
