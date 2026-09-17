# Mapa del proyecto — dónde está cada cosa

Guía de navegación del repositorio. Si buscas algo específico, empieza
por la tabla de la sección 1.

## 1. "Estoy buscando…"

| Si buscas… | Está en… |
|---|---|
| Qué problema resuelve el proyecto y para quién | [docs/vision-producto.md](vision-producto.md) |
| Quiénes son las partes interesadas y qué exige cada una | [docs/stakeholders.md](stakeholders.md) |
| Cuál es el problema duro y cómo se resolvió | [docs/problema-duro.md](problema-duro.md) |
| Las historias de usuario y el backlog priorizado | [docs/historias-usuario.md](historias-usuario.md) |
| **La historia central, desarrollada a fondo** | [docs/historias-usuario.md → HU-02](historias-usuario.md#hu-02--detectar-reincidencia-contratista-funcionario-historia-central) |
| Qué cuenta como alerta / qué hace el sistema con un dato raro | [docs/reglas-de-negocio.md](reglas-de-negocio.md) |
| El glosario del dominio (reincidencia, clave natural, evidencia…) | [docs/reglas-de-negocio.md § Glosario](reglas-de-negocio.md) |
| Las clases del sistema y cómo se relacionan | [docs/diagrama-dominio.md](diagrama-dominio.md) |
| Cómo se guardarán los datos (modelo entidad–relación) | [docs/esquema-bd.md](esquema-bd.md) |
| El modelo físico en PostgreSQL | [db/schema.sql](../db/schema.sql) |
| **La primera rebanada ejecutable y su trazabilidad** | [esqueleto/rebanada.md](../esqueleto/rebanada.md) |
| Cómo levantar la base de datos | [docker-compose.yml](../docker-compose.yml) y §5 de este documento |
| Ver la base de datos en el navegador | Adminer en http://localhost:8081 — ver §5 |
| El endpoint HTTP y cómo probarlo | [esqueleto/rebanada.md §7](../esqueleto/rebanada.md) |
| **Qué hace el módulo Java y cómo demostrarlo** | [docs/modulo-java-hu04.md](modulo-java-hu04.md) |
| Cómo correr el módulo Java (referencia corta) | [java/README.md](../java/README.md) |
| Por qué se tomó cada decisión | [docs/decisiones-tecnicas.md](decisiones-tecnicas.md) |
| Quién hizo qué en el equipo | [docs/Equipo/](Equipo/) |
| Cómo se usó IA y cuál es la postura del equipo | [docs/uso-ia.md](uso-ia.md) |
| Cómo ejecutar el proyecto y las pruebas | [README.md](../README.md) y §5 de este documento |
| Lo que falta y en qué orden | §6 de este documento |

## 2. Estructura del repositorio

```
proyecto_g/
├── README.md                    Punto de entrada: qué es, cómo correrlo, índice
├── .gitignore                   Datos reales fuera del repo (R-7)
├── docker-compose.yml           PostgreSQL 16 en el 5433 + Adminer (visor web) en el 8081
│
├── db/
│   ├── schema.sql               Modelo físico en PostgreSQL (6 tablas)
│   ├── datos.sql                Datos de ejemplo que siembra el contenedor
│   └── consultas.sql            Consultas de verificación
│
├── esqueleto/
│   └── rebanada.md              La primera rebanada: secuencia, clases, prueba y trazabilidad
│
├── java/                        Toda la aplicación: Java 21 + Spring Boot (Decisiones 14 y 18)
│   ├── pom.xml                  Maven: Spring Boot 3.3, JPA, PostgreSQL, JUnit 5
│   ├── ejecutar.sh              run | build | package
│   ├── src/main/java/dac/
│   │   ├── api/                 ContratoControlador — POST /api/contratos/cargar
│   │   ├── aplicacion/          ContratoServicio — orquesta y delimita la transacción
│   │   ├── dominio/             Contrato, Alerta, CargadorDeContratos, DetectorDeReincidencias…
│   │   └── persistencia/        5 repositorios Spring Data + 5 entidades JPA
│   └── src/test/java/dac/
│       ├── CargadorDeContratosTest.java    HU-01 + problema duro (10 pruebas)
│       ├── DetectorDeReincidenciasTest.java  HU-02, un caso por criterio (8 pruebas)
│       ├── FilasIncompletasTest.java       HU-04 / R-6 (7 pruebas)
│       └── ContratoE2ETest.java            De punta a punta por HTTP real (3 pruebas)
│
├── data/
│   ├── contratos_ejemplo.csv    5 contratos ficticios: el flujo feliz
│   ├── contratos_incompletos_ejemplo.csv   Filas incompletas: demostración de HU-04
│   └── privado/                 Datos reales — ignorado por Git
│
└── docs/
    ├── vision-producto.md       Visión, alcance del MVP, métricas, roadmap
    ├── stakeholders.md          12 stakeholders, matriz influencia/interés
    ├── problema-duro.md         Idempotencia: enunciado, decisión, alcance real
    ├── historias-usuario.md     Backlog priorizado (MoSCoW) + HU-02 a fondo
    ├── reglas-de-negocio.md     Lógica de negocio explícita + glosario
    ├── diagrama-dominio.md      Diagrama de clases (Mermaid) + decisiones de modelado
    ├── esquema-bd.md            Modelo entidad–relación (Mermaid) + restricciones
    ├── decisiones-tecnicas.md   18 decisiones con su alternativa descartada
    ├── modulo-java-hu04.md      Qué entrega HU-04, cómo ejecutarlo y demostrarlo
    ├── mapa-del-proyecto.md     Este documento
    ├── uso-ia.md                Uso de IA y postura del equipo
    └── Equipo/                  Un archivo por integrante
```

## 3. Cómo funciona, de punta a punta

```
POST /api/contratos/cargar (multipart: el CSV)
          │
          ▼
ContratoControlador ─────────── frontera HTTP; excepción del dominio → 400
          │
          ▼
ContratoServicio ────────────── @Transactional: todo o nada
          │
          ├─► CargadorDeContratos ── valida las 6 columnas (R-1)
          │                        ── descarta filas incompletas (R-6, HU-04)
          │                        ── ignora claves repetidas (R-2)
          │
          ├─► guarda solo los contratos nuevos ── uq_contrato_clave_natural (HU-07)
          │
          ├─► relee el histórico completo desde la base
          │
          ├─► DetectorDeReincidencias ── par con 2+ contratos → 1 alerta (R-3)
          │
          └─► guarda cada alerta con su evidencia ── uq_alerta_par
          │
          ▼
200 OK · contratosNuevos, totalEnBd, duplicadosIgnorados,
         filasDescartadas y alertas con su evidencia
```

La limpieza ocurre **antes** de la detección. Ése es el punto central del
problema duro: si un duplicado o una fila vacía llegaran al detector, se
verían exactamente iguales a una reincidencia real.

Y la detección corre sobre **todo el histórico en base de datos**, no solo
sobre el archivo que acaba de llegar: por eso una reincidencia que se
reparte entre dos cargas distintas igual aparece (HU-07).

El detalle de esta rebanada —diagrama de secuencia, diagrama de clases,
contrato de la prueba única y trazabilidad a los insumos— está en
[esqueleto/rebanada.md](../esqueleto/rebanada.md).

## 4. Trazabilidad: historia → regla → código → prueba

| Historia | Regla | Código | Pruebas |
|---|---|---|---|
| [HU-01](historias-usuario.md#hu-01--cargar-contratos-desde-un-archivo-csv) Cargar contratos | R-1, R-2, R-8 | `dominio/CargadorDeContratos.java` | `CargadorDeContratosTest` (5) |
| [HU-02](historias-usuario.md#hu-02--detectar-reincidencia-contratista-funcionario-historia-central) Reincidencia | R-3, R-4 | `dominio/DetectorDeReincidencias.java` | `DetectorDeReincidenciasTest` (8) |
| [Problema duro](problema-duro.md) Idempotencia | R-2, R-6 | `dominio/CargadorDeContratos.java`, `db/schema.sql` | `CargadorDeContratosTest` (5) + `e2e_carga_persiste_y_es_idempotente` |
| [HU-04](historias-usuario.md#hu-04--no-generar-alertas-a-partir-de-filas-incompletas) Filas incompletas | R-6 | `dominio/CargadorDeContratos.java` | `FilasIncompletasTest` (7) + `carga_filas_incompletas_sin_invalidar_el_archivo` |
| [HU-07](historias-usuario.md#hu-07--conservar-los-contratos-entre-ejecuciones) Idempotencia entre ejecuciones | R-2 | `aplicacion/ContratoServicio.java`, `uq_contrato_clave_natural` | `e2e_carga_persiste_y_es_idempotente` |
| HU-03, HU-05, HU-06, HU-08 | R-5, R-8 | — (pendientes) | — |

**28 pruebas, todas pasando:** 25 de dominio (rápidas, sin base de datos)
y 3 de punta a punta por HTTP real contra PostgreSQL. Cada criterio de
aceptación automatizable de HU-01, HU-02, HU-04 y HU-07 tiene su prueba;
ninguna historia se declaró "hecha" sin ella.

Los nombres de las pruebas **son** los criterios de aceptación: al correr
`mvn test` se lee la trazabilidad historia → prueba en la propia salida.

## 5. Cómo verificarlo

```bash
docker compose up -d             # PostgreSQL 16 en localhost:5433 + Adminer en localhost:8081
cd java && mvn test              # las 28 pruebas
java/ejecutar.sh                 # levanta el endpoint en localhost:8080
```

Con el servidor arriba:

```bash
curl -F archivo=@data/contratos_ejemplo.csv http://localhost:8080/api/contratos/cargar
```

Salida esperada del `curl` con la base recién creada: **`contratosNuevos: 0`**
—el contenedor ya sembró esos 5 contratos desde `db/datos.sql`—,
**`totalEnBd: 5`** y **1 alerta**: `ACME SAS` + `Juan Pérez`, con 3
contratos como evidencia (uno en la alcaldía y dos en la gobernación: la
señal cruza entidades, R-3). Repetir el mismo `curl` vuelve a dar
`contratosNuevos: 0`: eso es HU-07.

Salida esperada de `mvn test`: `Tests run: 28, Failures: 0, Errors: 0`.
Las 3 de punta a punta requieren el contenedor arriba; las otras 25 no
tocan la base.

## 6. Estado y pendientes

**Terminado en esta entrega:** carga con validación, idempotencia dentro
de la carga y también entre ejecuciones (HU-07), señal de reincidencia con
evidencia, la primera rebanada del esqueleto andante cruzando las cinco
fronteras técnicas con su prueba de punta a punta, y la documentación de
visión, stakeholders, problema duro, reglas de negocio, backlog,
decisiones y trazabilidad de la rebanada.

**Pendiente, en orden de prioridad:**

| # | Pendiente | Tipo | Referencia |
|---|---|---|---|
| 1 | No hay canalización de CI/CD: la rebanada solo se verifica a mano | Alcance | [rebanada §4](../esqueleto/rebanada.md) |
| 2 | Confirmar con S-1 la política de filas incompletas (hoy: descartar y reportar, decidido por el equipo) | Decisión provisional | [Decisión 12](decisiones-tecnicas.md), [HU-04](historias-usuario.md) |
| 3 | Validar con un analista real el umbral de reincidencia (¿2 veces? ¿ventana de tiempo?) | Supuesto sin validar | [R-3](reglas-de-negocio.md) |
| 4 | Nombres escritos distinto no se reconocen → falsos negativos | Limitación | [R-5](reglas-de-negocio.md), [HU-05](historias-usuario.md) |
| 5 | Ninguna prueba corre contra el JAR empaquetado | Alcance | [rebanada §4](../esqueleto/rebanada.md) |
| 6 | Carga concurrente del mismo archivo: no está ejercitada | Limitación | [rebanada §4](../esqueleto/rebanada.md) |
| 7 | Un duplicado con datos distintos se pierde en silencio | Limitación | [HU-06](historias-usuario.md) |
| 8 | `monto` y `fecha` se manejan como texto | Deuda técnica | [R-8](reglas-de-negocio.md) |
| 9 | Fraccionamiento, tablero visual, SECOP, concurrencia | Trabajo futuro | [Visión §6](vision-producto.md) |

Los pendientes 3 y 4 son los que más afectan la confianza del analista:
el primero puede producir alertas falsas y el segundo puede ocultar
reincidencias reales.

## 7. Quién mantiene qué

| Área | Responsable | Archivos |
|---|---|---|
| Requisitos, backlog y reglas de negocio | Gerson Rojo — [ficha](Equipo/gerson_rojo.md) | `docs/historias-usuario.md`, `docs/reglas-de-negocio.md`, `docs/vision-producto.md`, `docs/stakeholders.md` |
| Carga de datos y detección de alertas | Juan Pablo Cardozo — [ficha](Equipo/juan-pablo-cardozo-rivera.md) | `java/src/main/` |
| Plan de pruebas y verificación de criterios | Yerson Pérez — [ficha](Equipo/yerson_cadena.md) | `java/src/test/` |
| Decisiones y documentación de proceso | Equipo | `docs/decisiones-tecnicas.md`, `docs/uso-ia.md` |

Los pendientes 2 y 3 de la sección 6 son **decisiones de negocio**, no
técnicas: le corresponde al rol de requisitos llevarlas a un analista
real ([Decisión 11](decisiones-tecnicas.md)). El pendiente 2 ya tiene una
respuesta provisional del equipo para no dejar HU-04 bloqueada, pero
sigue necesitando la confirmación de S-1
([Decisión 12](decisiones-tecnicas.md)).
