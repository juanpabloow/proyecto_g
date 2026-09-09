# Módulo Java de DAC

Implementa **[HU-04 — No generar alertas a partir de filas
incompletas](../docs/historias-usuario.md)**. Es la única historia que
este módulo entrega.

Trae también la carga (HU-01) y la detección (HU-02) porque **HU-04 no se
puede probar sin ellas**: su criterio 1 dice *"…entonces no se genera
ninguna alerta"*, y para comprobar que una alerta no aparece hace falta
un detector; para tener filas que descartar, un cargador. Ese código
existe en el módulo, así que tiene sus propias pruebas — el
[DoD del proyecto](../docs/historias-usuario.md) lo exige.

La duplicación con `src/` es deliberada y está registrada en la
[Decisión 13](../docs/decisiones-tecnicas.md).

## Cómo correrlo

No hace falta Maven ni Gradle, solo un JDK 21 o superior:

```bash
java/ejecutar.sh           # las pruebas y luego el flujo completo
java/ejecutar.sh pruebas   # solo las pruebas
java/ejecutar.sh main      # solo el flujo completo
java/ejecutar.sh main data/privado/mi_archivo.csv   # con otro CSV
```

Salida esperada de `main`: **5 contratos cargados, 0 filas descartadas,
1 alerta** — `ACME SAS` + `Juan Pérez` con 3 contratos de evidencia. Es
el mismo resultado que `python3 main.py`.

## Cómo demostrar HU-04

El CSV de siempre no sirve para esto: no tiene filas incompletas, así que
la carga reporta 0 descartes. Para la demostración está
`data/contratos_incompletos_ejemplo.csv`, con dos filas sin contratista ni
funcionario, una sin funcionario, un duplicado y una reincidencia real.

```bash
# 1. El archivo limpio: 5 contratos, 0 descartes, 1 alerta
java/ejecutar.sh main

# 2. El archivo con filas incompletas: se descartan, se reporta cuáles
#    y por qué, y la reincidencia real sigue apareciendo
java/ejecutar.sh main data/contratos_incompletos_ejemplo.csv

# 3. Las 7 pruebas de HU-04, nombradas como sus criterios de aceptación
java/ejecutar.sh pruebas
```

## Qué hay adentro

```
java/
├── ejecutar.sh                        Compilar y correr (sin gestor de dependencias)
├── src/dac/
│   ├── Contrato.java                  Los 6 campos + clave natural (R-1, R-2, R-8)
│   ├── Alerta.java                    Par señalado + evidencia; exige 2+ contratos (HU-02)
│   ├── FilaDescartada.java            Una fila que no se cargó, y el motivo (HU-04)
│   ├── ResultadoCarga.java            Lo cargado y lo descartado: las dos salidas importan
│   ├── LectorCSV.java                 CSV con comillas y comas dentro de campos
│   ├── CargadorDeContratos.java       Validación + idempotencia + filas incompletas
│   ├── DetectorDeReincidencias.java   La señal del MVP (R-3)
│   └── Main.java                      Flujo de punta a punta
└── test/dac/pruebas/
    ├── PruebasFilasIncompletas.java   HU-04: la historia entregada (7 pruebas)
    ├── PruebasCarga.java              Base | HU-01 + problema duro (11 pruebas)
    ├── PruebasReincidencia.java       Base | HU-02, un caso por criterio (8 pruebas)
    ├── Pruebas.java                   Arnés mínimo de pruebas (sin JUnit)
    └── EjecutorDePruebas.java         Corre todo; sale con 1 si algo falla
```

**26 pruebas, todas pasando.** La salida las agrupa en ese orden: primero
HU-04, después la base marcada como tal, para que se vea qué entrega este
módulo y qué está solo para sostenerlo.

## Tres diferencias con la versión en Python

| | `src/` (Python) | `java/` |
|---|---|---|
| Filas incompletas | Generan una alerta del par `("", "")` ([R-6](../docs/reglas-de-negocio.md)) | Se descartan y se reportan (HU-04) |
| Idempotencia | Dentro de una llamada a `cargar_contratos` | Dentro de un `CargadorDeContratos`, aunque sean varios archivos |
| CSV con comas entre comillas | Lo maneja el módulo `csv` | Lo maneja `LectorCSV` |

Ninguna de las tres cambia la señal: las dos versiones detectan las
mismas reincidencias sobre datos limpios.

## Por qué no hay JUnit

El equipo no tiene Maven ni Gradle instalados, y bajar JUnit a mano para
26 pruebas cuesta más que escribir el arnés de `Pruebas.java`, que son 100
líneas. Cuando el proyecto adopte un gestor de dependencias, cada
`caso("...", () -> {...})` se convierte en un `@Test` sin tocar las
aserciones.

## Lo que este módulo tampoco hace

Las mismas limitaciones que la versión en Python, salvo HU-04:

- No reconoce nombres escritos distinto ([R-5](../docs/reglas-de-negocio.md), HU-05).
- No recuerda nada al cerrar el programa ([HU-07](../docs/historias-usuario.md)).
- Un duplicado con datos distintos se pierde en silencio ([HU-06](../docs/historias-usuario.md)).
- `monto` y `fecha` son texto ([R-8](../docs/reglas-de-negocio.md)).
