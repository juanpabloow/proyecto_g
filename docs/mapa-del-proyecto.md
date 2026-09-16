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
├── docker-compose.yml           PostgreSQL 16 en el puerto 5433, con el esquema aplicado
├── requirements.txt             Dependencias de Python (psycopg2, pytest)
├── main.py                      Flujo Python: cargar → persistir → detectar → mostrar
│
├── src/
│   ├── contratos.py             Carga del CSV + idempotencia (problema duro)
│   ├── alertas.py               Señal de reincidencia contratista–funcionario
│   └── repositorio.py           Persistencia en PostgreSQL (HU-07)
│
├── tests/
│   ├── test_contratos.py        Carga, validación e idempotencia (7 pruebas)
│   └── test_alertas.py          Detección de reincidencia (5 pruebas)
│
├── db/
│   ├── schema.sql               Modelo físico en PostgreSQL (6 tablas)
│   ├── datos.sql                Datos de ejemplo que siembra el contenedor
│   └── consultas.sql            Consultas de verificación
│
├── esqueleto/
│   └── rebanada.md              La primera rebanada: secuencia, clases, prueba y trazabilidad
│
├── java/                        Backend de la rebanada: Spring Boot (Decisión 14)
│   ├── pom.xml                  Maven: Spring Boot 3.3, JPA, PostgreSQL, JUnit
│   ├── ejecutar.sh              run | build | package
│   ├── src/main/java/dac/
│   │   ├── api/                 ContratoControlador — POST /api/contratos/cargar
│   │   ├── aplicacion/          ContratoServicio — orquesta y delimita la transacción
│   │   ├── dominio/             Contrato, Alerta, CargadorDeContratos, DetectorDeReincidencias…
│   │   └── persistencia/        4 repositorios Spring Data + 4 entidades JPA
│   ├── src/test/java/dac/       ContratoE2ETest — la prueba única, por HTTP real
│   └── src/dac/ · test/dac/     Módulo Java plano: huérfano, ya no se compila (ver §6)
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
    ├── decisiones-tecnicas.md   16 decisiones con su alternativa descartada
    ├── modulo-java-hu04.md      Qué entrega el módulo Java, cómo ejecutarlo y demostrarlo
    ├── mapa-del-proyecto.md     Este documento
    ├── uso-ia.md                Uso de IA y postura del equipo
    └── Equipo/                  Un archivo por integrante
```

## 3. Cómo funciona, de punta a punta

```
data/contratos_ejemplo.csv
          │
          ▼
cargar_contratos()  ── valida columnas requeridas (R-1)
  src/contratos.py   ── descarta claves repetidas: numero_contrato + entidad (R-2)
          │
          ▼   lista de contratos únicos
detectar_reincidencias()  ── agrupa por par (contratista, funcionario) (R-3)
  src/alertas.py           ── par con 2+ contratos → 1 alerta con su evidencia
          │
          ▼   lista de alertas
     main.py  ── imprime cada alerta con número, entidad, fecha y monto
```

La deduplicación ocurre **antes** de la detección. Ése es el punto
central del problema duro: si el duplicado llegara a la detección, se
vería exactamente igual que una reincidencia real.

El [módulo Java](../java/README.md) sigue el mismo flujo con dos pasos
más. En la carga, antes de deduplicar, descarta las filas incompletas y
las reporta ([HU-04](historias-usuario.md),
[Decisión 12](decisiones-tecnicas.md)). Y todo el flujo cuelga de un
endpoint HTTP que persiste en PostgreSQL:

```
POST /api/contratos/cargar (multipart: archivo)
          │
          ▼
ContratoControlador ── frontera HTTP
          │
          ▼
ContratoServicio ── @Transactional: guarda solo lo nuevo (HU-07)
          │        └── detecta sobre el histórico completo en BD
          ▼
Spring Data JPA → PostgreSQL (uq_contrato_clave_natural: R-2)
          │
          ▼
200 OK con contratosNuevos, totalEnBd, filasDescartadas y alertas
```

El detalle de esta rebanada —diagrama de secuencia, diagrama de clases,
contrato de la prueba única y trazabilidad a los insumos— está en
[esqueleto/rebanada.md](../esqueleto/rebanada.md).

## 4. Trazabilidad: historia → regla → código → prueba

| Historia | Regla | Código | Pruebas |
|---|---|---|---|
| [HU-01](historias-usuario.md#hu-01--cargar-contratos-desde-un-archivo-csv) Cargar contratos | R-1, R-2, R-8 | `src/contratos.py` | `test_carga_contratos_validos`, `test_falla_si_faltan_columnas`, `test_falla_si_el_archivo_esta_vacio` |
| [HU-02](historias-usuario.md#hu-02--detectar-reincidencia-contratista-funcionario-historia-central) Reincidencia | R-3, R-4 | `src/alertas.py` | `test_detecta_reincidencia_simple`, `test_no_genera_alerta_si_no_hay_repeticion`, `test_agrupa_todos_los_contratos_del_par_en_una_sola_alerta`, `test_mismo_contratista_con_funcionarios_distintos_no_alerta`, `test_toda_alerta_trae_su_evidencia` |
| [Problema duro](problema-duro.md) Idempotencia | R-2, R-6 | `src/contratos.py` | `test_ignora_contratos_duplicados`, `test_duplicado_no_genera_alerta_falsa`, `test_gana_el_primero_ante_datos_distintos`, `test_mismo_numero_en_entidades_distintas_no_se_deduplica` |
| [HU-04](historias-usuario.md#hu-04--no-generar-alertas-a-partir-de-filas-incompletas) Filas incompletas | R-6 | `java/src/main/java/dac/dominio/CargadorDeContratos.java` | `java/src/test/java/dac/ContratoE2ETest.java` (`rechaza_csv_sin_columnas_requeridas`) |
| [HU-07](historias-usuario.md#hu-07--conservar-los-contratos-entre-ejecuciones) Idempotencia entre ejecuciones | R-2 | `java/src/main/java/dac/aplicacion/ContratoServicio.java`, `src/repositorio.py` | `e2e_carga_persiste_y_es_idempotente` |
| HU-03, HU-05, HU-06, HU-08 | R-5, R-8 | — (pendientes) | — |

**12 pruebas en Python y 2 de punta a punta en Java, todas pasando.**
Cada criterio de aceptación automatizable de HU-01, HU-02, HU-04 y HU-07
tiene su prueba; ninguna historia se declaró "hecha" sin ella. HU-04 está
hecha **solo en Java** ([Decisión 13](decisiones-tecnicas.md)).

Las 26 pruebas del módulo Java plano (`java/test/dac/pruebas/`) **ya no se
ejecutan**: Maven solo compila `src/main/java` y `src/test/java`
([Decisión 14](decisiones-tecnicas.md)). Es el pendiente 1 de §6.

## 5. Cómo verificarlo

```bash
docker compose up -d db          # PostgreSQL 16 en localhost:5433, esquema aplicado

pip install -r requirements.txt
python3 main.py                  # flujo Python de punta a punta, contra la BD real
python3 -m pytest -v             # las 12 pruebas de Python (en memoria, sin BD)

cd java && mvn test              # la prueba única de la rebanada, por HTTP real
java/ejecutar.sh                 # levanta el endpoint en localhost:8080
```

Salida esperada de `main.py` con la BD recién creada: **5 contratos en el
CSV, 0 nuevos guardados** (el contenedor ya los sembró desde
`db/datos.sql`), **5 totales en BD y 1 alerta** — `ACME SAS` +
`Juan Pérez`, con 3 contratos como evidencia (uno en la alcaldía y dos en
la gobernación: la señal cruza entidades, R-3). Con la BD apagada,
`main.py` avisa y sigue en memoria.

Salida esperada de `mvn test`: `Tests run: 2, Failures: 0, Errors: 0`.
Requiere el contenedor arriba: la prueba usa la base de datos real, no una
en memoria.

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
| 1 | Resolver el módulo Java huérfano: `java/src/dac/` y sus 26 pruebas ya no se compilan | Deuda estructural | [Decisión 14](decisiones-tecnicas.md), [rebanada §6.C](../esqueleto/rebanada.md) |
| 1b | Consolidar Python y Java en un solo lenguaje: HU-04 solo está en Java | Duplicación deliberada | [Decisión 13](decisiones-tecnicas.md) |
| 2 | Confirmar con S-1 la política de filas incompletas (hoy: descartar y reportar, decidido por el equipo) | Decisión provisional | [Decisión 12](decisiones-tecnicas.md), [HU-04](historias-usuario.md) |
| 3 | Validar con un analista real el umbral de reincidencia (¿2 veces? ¿ventana de tiempo?) | Supuesto sin validar | [R-3](reglas-de-negocio.md) |
| 4 | Nombres escritos distinto no se reconocen → falsos negativos | Limitación | [R-5](reglas-de-negocio.md), [HU-05](historias-usuario.md) |
| 5 | El módulo Java detecta alertas pero no las guarda en `alerta`/`alerta_contrato` | Alcance | [rebanada §6.C](../esqueleto/rebanada.md) |
| 6 | No hay canalización de CI/CD: la rebanada solo se verifica a mano | Alcance | [rebanada §4](../esqueleto/rebanada.md) |
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
| Carga de datos y detección de alertas | Juan Pablo Cardozo — [ficha](Equipo/juan-pablo-cardozo-rivera.md) | `main.py`, `src/`, `java/src/main/` |
| Plan de pruebas y verificación de criterios | Yerson Pérez — [ficha](Equipo/yerson_cadena.md) | `tests/`, `java/src/test/` |
| Decisiones y documentación de proceso | Equipo | `docs/decisiones-tecnicas.md`, `docs/uso-ia.md` |

Los pendientes 2 y 3 de la sección 6 son **decisiones de negocio**, no
técnicas: le corresponde al rol de requisitos llevarlas a un analista
real ([Decisión 11](decisiones-tecnicas.md)). El pendiente 2 ya tiene una
respuesta provisional del equipo para no dejar HU-04 bloqueada, pero
sigue necesitando la confirmación de S-1
([Decisión 12](decisiones-tecnicas.md)).
