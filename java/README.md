# Módulo Java de DAC — backend de la rebanada

Backend del [esqueleto andante](../esqueleto/rebanada.md): Java 21 +
Spring Boot 3.3 + PostgreSQL 16. Expone un endpoint que recibe el CSV,
persiste los contratos nuevos y devuelve las alertas detectadas sobre el
histórico completo.

```
POST /api/contratos/cargar    Content-Type: multipart/form-data
                              Param: archivo (el CSV)
```

Cruza las cinco fronteras técnicas de la rebanada: HTTP → aplicación →
dominio → persistencia real → migración. El diagrama de secuencia, el de
clases, el contrato de la prueba única y la trazabilidad a las evidencias
del proyecto están en [esqueleto/rebanada.md](../esqueleto/rebanada.md).

## Historias que entrega

| Historia | Qué aporta este módulo |
|---|---|
| [HU-01](../docs/historias-usuario.md) | Carga del CSV con validación de las 6 columnas requeridas (R-1) |
| [HU-02](../docs/historias-usuario.md) | Detección de reincidencia contratista–funcionario (R-3), con su evidencia |
| [HU-04](../docs/historias-usuario.md) | Las filas incompletas se descartan y se reportan ([Decisión 12](../docs/decisiones-tecnicas.md)). Detalle en [modulo-java-hu04.md](../docs/modulo-java-hu04.md) |
| [HU-07](../docs/historias-usuario.md) | Idempotencia **entre ejecuciones**: recargar el mismo CSV no duplica nada |

## Cómo correrlo

Requiere **JDK 21+**, **Maven** y **Docker**.

```bash
docker compose up -d db      # PostgreSQL en localhost:5433, esquema y datos aplicados
cd java && mvn test          # las pruebas de punta a punta contra la BD real
java/ejecutar.sh             # levanta el endpoint en localhost:8080
```

`ejecutar.sh` acepta tres acciones:

```bash
java/ejecutar.sh             # run — inicia el servidor
java/ejecutar.sh build       # compila y ejecuta las pruebas
java/ejecutar.sh package     # genera target/dac-0.0.1-SNAPSHOT.jar
```

Con el servidor arriba:

```bash
curl -F archivo=@../data/contratos_ejemplo.csv http://localhost:8080/api/contratos/cargar
```

Responde `contratosNuevos`, `totalEnBd`, `duplicadosIgnorados`,
`filasDescartadas` y `alertas`. Con la base recién creada la primera
llamada devuelve `contratosNuevos: 0` —el contenedor ya sembró esos 5
contratos desde `db/datos.sql`— y `totalEnBd: 5` con 1 alerta.

Si la base de datos no está arriba, la aplicación no arranca: la rebanada
usa persistencia real, no una base en memoria.

## Qué hay adentro

```
java/
├── pom.xml                              Maven: Spring Boot web + JPA + PostgreSQL + JUnit
├── ejecutar.sh                          run | build | package
├── src/main/resources/
│   └── application.properties           Conexión a localhost:5433; ddl-auto=none (Decisión 15)
├── src/main/java/dac/
│   ├── DacApplication.java              Arranque de Spring Boot
│   ├── api/
│   │   └── ContratoControlador.java     POST /api/contratos/cargar; excepción del dominio → 400
│   ├── aplicacion/
│   │   ├── ContratoServicio.java        Orquesta la rebanada; @Transactional
│   │   └── ResultadoServicio.java       Lo que sale por HTTP
│   ├── dominio/
│   │   ├── Contrato.java                Los 6 campos + claveNatural() y par()
│   │   ├── Alerta.java                  Par señalado + evidencia; exige 2+ contratos
│   │   ├── FilaDescartada.java          Una fila que no se cargó, y el motivo (HU-04)
│   │   ├── ResultadoCarga.java          Lo cargado, lo descartado y los duplicados
│   │   ├── LectorCSV.java               CSV con comillas y comas dentro de campos
│   │   ├── CargadorDeContratos.java     Validación + filas incompletas + deduplicación
│   │   └── DetectorDeReincidencias.java La señal del MVP (R-3)
│   └── persistencia/
│       ├── *Repo.java                   4 interfaces Spring Data (los puertos)
│       └── *Jpa.java                    4 entidades: el mapeo a db/schema.sql
└── src/test/java/dac/
    └── ContratoE2ETest.java             La prueba única + la ruta de error + HU-04
```

## Las pruebas

```bash
cd java && mvn test     # Tests run: 3, Failures: 0, Errors: 0
```

Las tres entran por HTTP real (`WebEnvironment.RANDOM_PORT`: Tomcat
embebido en un socket, sin mocks) y usan PostgreSQL real
([Decisión 16](../docs/decisiones-tecnicas.md)):

| Prueba | Qué fija |
|---|---|
| `e2e_carga_persiste_y_es_idempotente` | La prueba única del esqueleto. Carga el CSV dos veces: la segunda no debe agregar nada (HU-07). Falla si cualquiera de las cinco fronteras se rompe |
| `rechaza_csv_sin_columnas_requeridas` | La ruta de error del diagrama: sin las 6 columnas (R-1) responde `400` y no escribe nada |
| `carga_filas_incompletas_sin_invalidar_el_archivo` | R-6 y HU-04: 3 filas descartadas con su motivo, 1 duplicado ignorado, la reincidencia real intacta y el archivo válido |

Necesitan el contenedor `dac_db` arriba. Truncan las tablas antes de cada
caso, porque el contenedor siembra datos de ejemplo al crearse.

## El módulo Java plano (`src/dac/`, `test/dac/`) está huérfano

En el árbol siguen `src/dac/*.java` y `test/dac/pruebas/*.java`: la
versión anterior de este módulo, sin Maven, con un arnés de pruebas propio
y 26 casos. **Ya no se compila ni se ejecuta**, porque Maven solo mira
`src/main/java` y `src/test/java`
([Decisión 14](../docs/decisiones-tecnicas.md)).

No se borró todavía porque esas 26 pruebas cubren HU-01, HU-02 y HU-04 con
mucha más granularidad que la única prueba e2e de hoy, y portarlas a JUnit
es trabajo pendiente —no trabajo perdido—. Es el pendiente 1 del
[mapa del proyecto](../docs/mapa-del-proyecto.md).

## Diferencias con la versión en Python

| | `src/` (Python) | `java/` |
|---|---|---|
| Punto de entrada | `python3 main.py` (CLI) | `POST /api/contratos/cargar` |
| Filas incompletas | Generan una alerta del par `("", "")` ([R-6](../docs/reglas-de-negocio.md)) | Se descartan y se reportan (HU-04) |
| Idempotencia en el archivo | Dentro de una llamada a `cargar_contratos` | Dentro de una llamada a `cargarDesde` |
| Idempotencia entre ejecuciones | Sí, vía `ON CONFLICT` (HU-07) | Sí, vía consulta por clave natural + `uq_contrato_clave_natural` (HU-07) |
| Persistencia de alertas | Sí, en `alerta` y `alerta_contrato` | **No**: las detecta y las devuelve, pero no las guarda |

La duplicación es deliberada y está registrada en la
[Decisión 13](../docs/decisiones-tecnicas.md). Consolidar en un solo
lenguaje es el pendiente 1b del mapa del proyecto.

## Lo que este módulo no hace

- No guarda las alertas que detecta ([rebanada §6.C](../esqueleto/rebanada.md)).
- No reconoce nombres escritos distinto ([R-5](../docs/reglas-de-negocio.md), HU-05).
- Un duplicado con datos distintos se pierde en silencio ([HU-06](../docs/historias-usuario.md)).
- `monto` y `fecha` viajan como texto por el dominio ([R-8](../docs/reglas-de-negocio.md)); solo se convierten en el mapeo a persistencia.
- No hay canalización de CI/CD: la rebanada se verifica a mano.
