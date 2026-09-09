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
├── main.py                      Flujo end-to-end: cargar → detectar → mostrar
│
├── src/
│   ├── contratos.py             Carga del CSV + idempotencia (problema duro)
│   └── alertas.py               Señal de reincidencia contratista–funcionario
│
├── tests/
│   ├── test_contratos.py        Carga, validación e idempotencia (7 pruebas)
│   └── test_alertas.py          Detección de reincidencia (5 pruebas)
│
├── db/
│   └── schema.sql               Modelo físico en PostgreSQL (6 tablas)
│
├── java/                        Módulo Java: lo mismo que src/ + HU-04 (Decisión 13)
│   ├── ejecutar.sh              Compilar y correr, sin Maven ni Gradle
│   ├── src/dac/                 Contrato, Alerta, CargadorDeContratos, DetectorDeReincidencias…
│   └── test/dac/pruebas/        26 pruebas con un arnés propio
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
    ├── decisiones-tecnicas.md   13 decisiones con su alternativa descartada
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

El [módulo Java](../java/README.md) sigue el mismo flujo con un paso más
en la carga: antes de deduplicar, descarta las filas incompletas y las
reporta ([HU-04](historias-usuario.md), [Decisión 12](decisiones-tecnicas.md)).

## 4. Trazabilidad: historia → regla → código → prueba

| Historia | Regla | Código | Pruebas |
|---|---|---|---|
| [HU-01](historias-usuario.md#hu-01--cargar-contratos-desde-un-archivo-csv) Cargar contratos | R-1, R-2, R-8 | `src/contratos.py` | `test_carga_contratos_validos`, `test_falla_si_faltan_columnas`, `test_falla_si_el_archivo_esta_vacio` |
| [HU-02](historias-usuario.md#hu-02--detectar-reincidencia-contratista-funcionario-historia-central) Reincidencia | R-3, R-4 | `src/alertas.py` | `test_detecta_reincidencia_simple`, `test_no_genera_alerta_si_no_hay_repeticion`, `test_agrupa_todos_los_contratos_del_par_en_una_sola_alerta`, `test_mismo_contratista_con_funcionarios_distintos_no_alerta`, `test_toda_alerta_trae_su_evidencia` |
| [Problema duro](problema-duro.md) Idempotencia | R-2, R-6 | `src/contratos.py` | `test_ignora_contratos_duplicados`, `test_duplicado_no_genera_alerta_falsa`, `test_gana_el_primero_ante_datos_distintos`, `test_mismo_numero_en_entidades_distintas_no_se_deduplica` |
| [HU-04](historias-usuario.md#hu-04--no-generar-alertas-a-partir-de-filas-incompletas) Filas incompletas | R-6 | `java/src/dac/CargadorDeContratos.java` | `java/test/dac/pruebas/PruebasFilasIncompletas.java` (7 pruebas) |
| HU-03, HU-05 a HU-08 | R-5, R-8 | — (pendientes) | — |

**12 pruebas en Python y 26 en Java, todas pasando.** Cada criterio de
aceptación automatizable de HU-01, HU-02 y HU-04 tiene su prueba; ninguna
historia se declaró "hecha" sin ella. HU-04 está hecha **solo en Java**
([Decisión 13](decisiones-tecnicas.md)).

## 5. Cómo verificarlo

```bash
pip install pytest
python3 main.py          # flujo completo con los datos de ejemplo
python3 -m pytest -v     # las 12 pruebas de Python

java/ejecutar.sh         # las 26 pruebas de Java y el mismo flujo completo
```

Salida esperada de `main.py`: **5 contratos cargados, 1 alerta** —
`ACME SAS` + `Juan Pérez`, con 3 contratos como evidencia (uno en la
alcaldía y dos en la gobernación: la señal cruza entidades, R-3).

## 6. Estado y pendientes

**Terminado en esta entrega:** carga con validación, idempotencia dentro
de la carga, señal de reincidencia con evidencia, 12 pruebas, y la
documentación de visión, stakeholders, problema duro, reglas de negocio,
backlog y decisiones.

**Pendiente, en orden de prioridad:**

| # | Pendiente | Tipo | Referencia |
|---|---|---|---|
| 1 | Confirmar con S-1 la política de filas incompletas (hoy: descartar y reportar, decidido por el equipo) | Decisión provisional | [Decisión 12](decisiones-tecnicas.md), [HU-04](historias-usuario.md) |
| 1b | Consolidar Python y Java en un solo lenguaje: HU-04 solo está en Java | Duplicación deliberada | [Decisión 13](decisiones-tecnicas.md) |
| 2 | Validar con un analista real el umbral de reincidencia (¿2 veces? ¿ventana de tiempo?) | Supuesto sin validar | [R-3](reglas-de-negocio.md) |
| 3 | Nombres escritos distinto no se reconocen → falsos negativos | Limitación | [R-5](reglas-de-negocio.md), [HU-05](historias-usuario.md) |
| 4 | Idempotencia entre ejecuciones (requiere persistencia) | Alcance | [HU-07](historias-usuario.md) |
| 5 | Un duplicado con datos distintos se pierde en silencio | Limitación | [HU-06](historias-usuario.md) |
| 6 | `monto` y `fecha` se manejan como texto | Deuda técnica | [R-8](reglas-de-negocio.md) |
| 7 | Fraccionamiento, tablero visual, SECOP, concurrencia | Trabajo futuro | [Visión §6](vision-producto.md) |

Los pendientes 2 y 3 son los que más afectan la confianza del analista:
el primero puede producir alertas falsas y el segundo puede ocultar
reincidencias reales.

## 7. Quién mantiene qué

| Área | Responsable | Archivos |
|---|---|---|
| Requisitos, backlog y reglas de negocio | Gerson Rojo — [ficha](Equipo/gerson_rojo.md) | `docs/historias-usuario.md`, `docs/reglas-de-negocio.md`, `docs/vision-producto.md`, `docs/stakeholders.md` |
| Carga de datos y detección de alertas | Juan Pablo Cardozo — [ficha](Equipo/juan-pablo-cardozo-rivera.md) | `main.py`, `src/`, `java/src/` |
| Plan de pruebas y verificación de criterios | Yerson Pérez — [ficha](Equipo/yerson_cadena.md) | `tests/`, `java/test/` |
| Decisiones y documentación de proceso | Equipo | `docs/decisiones-tecnicas.md`, `docs/uso-ia.md` |

Los pendientes 1 y 2 de la sección 6 son **decisiones de negocio**, no
técnicas: le corresponde al rol de requisitos llevarlas a un analista
real ([Decisión 11](decisiones-tecnicas.md)). El pendiente 1 ya tiene una
respuesta provisional del equipo para no dejar HU-04 bloqueada, pero
sigue necesitando la confirmación de S-1
([Decisión 12](decisiones-tecnicas.md)).
