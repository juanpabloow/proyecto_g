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

## Decisiones sobre el problema duro

| # | Decisión | Alternativa descartada | Por qué |
|---|---|---|---|
| 5 | La clave de un contrato es **`numero_contrato` + `entidad`** | Solo `numero_contrato`; o la fila completa | Dos entidades pueden repetir el consecutivo "001"; y usar la fila completa dejaría pasar el duplicado con un monto corregido. Detalle en [problema duro §3](problema-duro.md) |
| 6 | Ante dos filas con la misma clave y datos distintos, **gana la primera** | Gana la última; o registrar el conflicto | Es lo más simple y predecible para el MVP. La limitación queda visible y registrada como [HU-06](historias-usuario.md) |
| 7 | La idempotencia se limita a **una carga**, sin persistencia | Guardar en base de datos ya en esta entrega | Sin base de datos, prometer idempotencia entre ejecuciones sería falso. Se documentó el alcance real en vez de exagerarlo ([HU-07](historias-usuario.md)) |

## Decisiones de proceso y datos

| # | Decisión | Alternativa descartada | Por qué |
|---|---|---|---|
| 8 | La lógica de negocio se documenta en [reglas-de-negocio.md](reglas-de-negocio.md), aparte del código | Dejarla implícita en `src/` | Estaba solo en el código: había que leer Python para saber qué cuenta como alerta. Ahora las reglas —incluidas las limitaciones— son revisables por alguien que no programa (S-1, S-12) |
| 9 | **No** normalizamos nombres de contratistas/funcionarios en este MVP | Aplicar mayúsculas/tildes/sufijos por nuestra cuenta | Normalizar de más une empresas distintas de nombre parecido y señala a alguien por error (S-6). Requiere validación con un analista ([R-5](reglas-de-negocio.md)) |
| 10 | El CSV de ejemplo **se versiona**; los datos reales van en `data/privado/` (ignorado) | Ignorar toda la carpeta `data/` | Ignorar `data/` completa dejaba el repositorio sin el archivo que `main.py` necesita: un clon nuevo no podía ejecutar el proyecto ([R-7](reglas-de-negocio.md)) |
| 11 | Las decisiones de negocio sin resolver **se documentan como pendientes**, no se resuelven por cuenta técnica | Elegir nosotros el umbral, el manejo de filas incompletas, etc. | Son decisiones del usuario (S-1). Inventarlas produciría un sistema que "funciona" con reglas que nadie validó ([R-3](reglas-de-negocio.md), [HU-04](historias-usuario.md)) |

Nuevas decisiones se van agregando aquí a medida que el equipo avanza.
