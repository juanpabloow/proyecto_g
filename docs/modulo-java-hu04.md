# Módulo Java — HU-04

Documento para el equipo. Explica qué entrega el módulo `java/`, por qué
está hecho así, cómo se relaciona con el resto del backlog y cómo
demostrar que funciona.

---

## 1. Qué entrega

**Una sola historia: [HU-04 — No generar alertas a partir de filas
incompletas](historias-usuario.md#hu-04--no-generar-alertas-a-partir-de-filas-incompletas).**

> **Como** analista (S-1), **quiero** que las filas sin contratista o sin
> funcionario no produzcan alertas, **para** no perder tiempo revisando
> una alerta "sobre nadie".

Era la historia de mayor prioridad entre las pendientes (*Should*, 2
puntos) y la que estaba marcada como siguiente en el backlog.

## 2. El problema que resuelve

Antes de HU-04, un CSV con dos filas sin contratista ni funcionario
producía esto:

```
Alertas encontradas: 2

⚠️  ACME SAS + Juan Pérez          ← alerta real
   - Contrato 101 (...)
   - Contrato 103 (...)

⚠️   +                              ← alerta sobre nadie
   - Contrato 104 (...)
   - Contrato 105 (...)
```

El sistema veía el par `("", "")` repetido dos veces y lo trataba como
una reincidencia. El analista abre la alerta y no hay a quién
investigar: pierde el tiempo, y peor, empieza a desconfiar de las demás
alertas. Está documentado en [R-6](reglas-de-negocio.md).

## 3. La decisión que lo desbloqueó

La historia llevaba semanas bloqueada por una pregunta de negocio, no
técnica: **¿la fila incompleta se descarta, se reporta, o invalida el
archivo completo?**

Se resolvió así ([Decisión 12](decisiones-tecnicas.md)):

> **La fila se descarta y se reporta. El archivo sigue siendo válido.**

| Alternativa | Por qué no |
|---|---|
| **Aceptar la fila** | Es el defecto mismo: produce la alerta sobre nadie |
| **Invalidar el archivo completo** | Una fila mala entre 5.000 dejaría al analista sin nada. El costo cae sobre quien no cometió el error |
| **Descartar en silencio** | Esconde un problema de calidad de datos. Además, el criterio de aceptación 2 ya pedía *"informar cuántas filas se descartaron y por qué"* |

**Importante para la sustentación:** esta decisión es de S-1, no del
equipo técnico. La tomamos de forma **provisional** para no dejar la
historia bloqueada, y quedó registrada como una **excepción explícita a
la [Decisión 11](decisiones-tecnicas.md)** (que dice justamente que las
decisiones de negocio no se resuelven por cuenta técnica). Sigue
pendiente de confirmar con un analista real, y la casilla está sin marcar
en el DoD de HU-04.

Revertirla cuesta una línea: la lista `CAMPOS_OBLIGATORIOS` en
`java/src/dac/CargadorDeContratos.java:47`.

## 4. Qué se descarta exactamente

| Campo vacío | ¿Descarta la fila? | Por qué |
|---|---|---|
| `contratista`, `funcionario` | **Sí** | Son el par sobre el que se alerta: vacíos generan una alerta sobre nadie |
| `numero_contrato`, `entidad` | **Sí** | Son la clave natural: sin ella no se puede saber si el contrato ya estaba cargado ([R-2](reglas-de-negocio.md)) |
| `monto`, `fecha` | **No** | La señal no los usa ([R-8](reglas-de-negocio.md)); descartar por ellos perdería reincidencias reales |

Dos precisiones que suelen preguntarse:

- **Una celda con solo espacios cuenta como vacía.** `"   "` no es un
  contratista.
- **Los valores que sí se cargan no se normalizan.** Decidir que
  `" Juan Pérez"` y `"Juan Pérez"` son la misma persona es
  [HU-05](historias-usuario.md), necesita validación de S-1 y está
  explícitamente descartado por ahora
  ([R-5](reglas-de-negocio.md), [Decisión 9](decisiones-tecnicas.md)).

## 5. Cómo se relaciona con las otras historias

El módulo entrega **una** historia. Pero HU-04 no se puede probar sola:
su criterio 1 dice *"…entonces **no se genera ninguna alerta**"*. Para
comprobar que una alerta **no** aparece hace falta algo capaz de
generarla, y para tener filas incompletas que descartar hace falta algo
que lea el CSV.

| Historia | Papel en este módulo | Estado |
|---|---|---|
| **HU-04** Filas incompletas | **La historia entregada** | ✅ Hecha en Java |
| HU-01 Cargar contratos desde CSV | **Base**: sin cargador no hay filas que descartar | Reimplementada como soporte |
| HU-02 Reincidencia contratista–funcionario | **Base**: sin detector no se puede verificar que la alerta no aparece | Reimplementada como soporte |
| Problema duro (idempotencia) | Vive en la misma clase que HU-04: ambas filtran antes de detectar | Conservado |
| HU-05 Nombres escritos distinto | **No tocada.** Se documenta como limitación en las pruebas | ⬜ Pendiente |
| HU-06 Duplicado con datos distintos | **No tocada.** Sigue ganando la primera fila ([Decisión 6](decisiones-tecnicas.md)) | ⬜ Pendiente |
| HU-07 Persistencia entre ejecuciones | **No tocada**, pero hay un avance parcial: ver §6 | ⬜ Pendiente |
| HU-08 Exportar el reporte | **No tocada** | ⬜ Pendiente |

Por eso las pruebas están agrupadas así, y en ese orden:

```
HU-04 - No generar alertas a partir de filas incompletas  <-- la historia   (7)
Base | HU-01 - Cargar contratos desde un archivo CSV                        (5)
Base | Problema duro - Idempotencia                                         (6)
Base | HU-02 - Detectar reincidencia contratista-funcionario                (8)
```

**7 pruebas de la historia, 19 de la base que la sostiene.** El código de
la base existe en Java, así que no puede quedarse sin pruebas: el DoD del
proyecto pide que cada criterio automatizable tenga la suya.

## 6. Cómo funciona por dentro

```
CSV
 │
 ▼
LectorCSV                   parte el texto en filas, respetando comillas
 │
 ▼
CargadorDeContratos
 ├─ validarColumnas()       ¿están las 6 columnas? si no, falla nombrando cuáles (R-1)
 ├─ camposVacios()          ← AQUÍ VIVE HU-04: fila incompleta → FilaDescartada
 └─ clavesVistas            duplicado por numero_contrato + entidad → se ignora (R-2)
 │
 ▼
ResultadoCarga              contratos + filas descartadas + duplicados ignorados
 │
 ▼
DetectorDeReincidencias     agrupa por par (contratista, funcionario) (R-3)
 │
 ▼
Alerta                      par señalado + evidencia (mínimo 2 contratos)
```

El orden importa: **HU-04 filtra antes de deduplicar, y las dos filtran
antes de detectar.** Si una fila incompleta llegara al detector, se vería
igual que una reincidencia real — que es exactamente el mismo argumento
del [problema duro](problema-duro.md).

### Las clases

| Archivo | Qué hace |
|---|---|
| `Contrato.java` | Los 6 campos + `claveNatural()` y `par()`. Conserva las columnas extra (R-1) |
| `Alerta.java` | Par señalado + evidencia. **Rechaza construirse con menos de 2 contratos** |
| `FilaDescartada.java` | Una fila que no se cargó, con su número de fila y el motivo |
| `ResultadoCarga.java` | Las dos salidas de la carga: lo cargado y lo descartado |
| `LectorCSV.java` | CSV con comillas y comas dentro de campos |
| `CargadorDeContratos.java` | Validación + **HU-04** + idempotencia |
| `DetectorDeReincidencias.java` | La señal del MVP (R-3) |
| `Main.java` | El flujo de punta a punta |

### Dos detalles que conviene conocer

1. **`clavesVistas` es un campo de instancia**, tal como está en el
   [diagrama de dominio](diagrama-dominio.md). Consecuencia: el mismo
   `CargadorDeContratos` reconoce un contrato ya cargado aunque venga en
   otro archivo. Sigue sin sobrevivir al cierre del programa — eso es
   [HU-07](historias-usuario.md) y necesita base de datos.
2. **No hay JUnit.** No tenemos Maven ni Gradle instalados, y bajar JUnit
   a mano para 26 pruebas cuesta más que el arnés de `Pruebas.java`, que
   son 100 líneas. Cuando adoptemos un gestor de dependencias, cada
   `caso("...", () -> {...})` se vuelve un `@Test` sin tocar las
   aserciones.

## 7. Cómo ejecutarlo

Solo hace falta un **JDK 21 o superior**. Nada de Maven ni Gradle.

```bash
java/ejecutar.sh            # las 26 pruebas y después el flujo completo
java/ejecutar.sh pruebas    # solo las pruebas
java/ejecutar.sh main       # solo el análisis, con el CSV de ejemplo
```

Con otro archivo:

```bash
java/ejecutar.sh main data/privado/contratos_reales.csv
```

Tres cosas que importan:

1. El CSV necesita las seis columnas con estos nombres exactos:
   `numero_contrato,entidad,contratista,funcionario,monto,fecha`. Si
   falta alguna, el error dice cuál. Columnas de más se aceptan y se
   conservan.
2. Los datos reales van en `data/privado/`, que está en `.gitignore`
   ([R-7](reglas-de-negocio.md)). Nunca al repositorio.
3. Las rutas relativas se resuelven desde la raíz del repo, porque el
   script se para ahí. Desde otra carpeta, usar ruta absoluta.

El script compila en `java/build/`, que está ignorado por Git.

## 8. Cómo demostrar que funciona

El CSV de siempre **no sirve** para esto: no tiene filas incompletas, así
que reporta 0 descartes. Para la demostración está
`data/contratos_incompletos_ejemplo.csv` (ficticio y versionado).

### Paso 1 — mostrar el archivo de entrada

```
1  numero_contrato,entidad,contratista,funcionario,monto,fecha
2  101,Alcaldía de Ejemplo,ACME SAS,Juan Pérez,50000000,2026-01-15
3  102,Alcaldía de Ejemplo,Constructora XYZ,Ana Ruiz,30000000,2026-02-10
4  103,Gobernación Ejemplo,ACME SAS,Juan Pérez,45000000,2026-03-05
5  104,Alcaldía de Ejemplo,,,80000000,2026-03-20                      ← sin contratista ni funcionario
6  105,Gobernación Ejemplo,,,12000000,2026-04-01                      ← sin contratista ni funcionario
7  106,Alcaldía de Ejemplo,Suministros del Sur,   ,25000000,...       ← funcionario en blanco
8  107,Gobernación Ejemplo,Suministros del Sur,Carlos Gómez,,         ← monto y fecha vacíos
9  101,Alcaldía de Ejemplo,ACME SAS,Juan Pérez,50000000,...           ← duplicado exacto
```

Las líneas **5 y 6 son la clave**: son dos filas con el mismo par vacío.
Sin HU-04, el sistema las agrupa y genera la alerta sobre nadie. Con una
sola fila no se vería el defecto.

### Paso 2 — correr el análisis

```bash
java/ejecutar.sh main data/contratos_incompletos_ejemplo.csv
```

```
Contratos cargados: 4
Duplicados ignorados: 1 (mismo número de contrato en la misma entidad — R-2)
Filas descartadas: 3 (el resto del archivo se analiza igual — Decisión 12)
   - fila 5: faltan los campos contratista, funcionario
   - fila 6: faltan los campos contratista, funcionario
   - fila 7: falta el campo funcionario
Alertas encontradas: 1

[!] ACME SAS + Juan Pérez
   - Contrato 101 (Alcaldía de Ejemplo, 2026-01-15, $50000000)
   - Contrato 103 (Gobernación Ejemplo, 2026-03-05, $45000000)
```

**Tres cosas para señalar en voz alta:**

1. **No hay ninguna alerta sobre nadie.** Las filas 5 y 6 ya no se
   agrupan en una reincidencia falsa.
2. **El sistema dice qué descartó y por qué**, con el número de fila. Es
   el criterio de aceptación 2, literal.
3. **El archivo no se invalidó.** Tres filas malas, y la reincidencia
   real sigue detectada con su evidencia. Esa es la Decisión 12.

Nótese también que la fila 8 (`107`) **sí se cargó** aunque tiene monto y
fecha vacíos: la señal no los usa, y descartarla habría perdido un
contrato válido.

### Paso 3 — mostrar las pruebas

```bash
java/ejecutar.sh pruebas
```

Los casos están nombrados como los criterios de aceptación, así que la
pantalla misma muestra la trazabilidad **historia → prueba**:

```
HU-04 - No generar alertas a partir de filas incompletas  <-- la historia
  [ok]  criterio 1: dos filas sin contratista ni funcionario no generan alerta
  [ok]  criterio 2: informa cuántas filas se descartaron y por qué
  [ok]  una fila incompleta no invalida el archivo (Decisión 12)
  [ok]  una celda con solo espacios cuenta como vacía
  [ok]  una fila sin clave natural se descarta: no se puede deduplicar (R-2)
  [ok]  monto y fecha vacíos NO descartan la fila (R-8)
  [ok]  una línea en blanco no se reporta como fila descartada
```

## 9. Qué NO hace este módulo

Las mismas limitaciones del MVP, salvo HU-04:

- **No reconoce nombres escritos distinto.** `"ACME SAS"` y `"acme sas"`
  son dos contratistas ([R-5](reglas-de-negocio.md), HU-05). Hay una
  prueba que lo deja documentado como limitación conocida, no como
  descuido.
- **No recuerda nada al cerrar el programa**
  ([HU-07](historias-usuario.md)).
- **Un duplicado con datos distintos se pierde en silencio**
  ([HU-06](historias-usuario.md)).
- **`monto` y `fecha` son texto**: no se suman ni se comparan
  ([R-8](reglas-de-negocio.md)).
- **No hay interfaz gráfica.** Es una herramienta de consola. El tablero
  visual está en [visión §6](vision-producto.md) como trabajo futuro; lo
  más cercano en el backlog es HU-08.

## 10. Qué queda pendiente

| # | Pendiente | De quién es |
|---|---|---|
| 1 | **Confirmar la Decisión 12 con un analista real (S-1).** Si dice que una fila incompleta debe invalidar el archivo, la decisión cambia | Rol de requisitos |
| 2 | **Consolidar Python y Java en un solo lenguaje.** Hoy HU-04 está solo en Java y `src/` conserva el comportamiento de R-6. La duplicación es deliberada y está en la [Decisión 13](decisiones-tecnicas.md) | Equipo |

## 11. Qué mirar según tu rol

| Rol | Empieza por |
|---|---|
| **Requisitos y negocio** (Gerson) | §3 (la decisión), §4 (qué se descarta) y §10. La Decisión 12 necesita tu gestión con S-1 |
| **QA y pruebas** (Yerson) | §5 (por qué hay 26 pruebas y no 7) y §8 paso 3. Los nombres de los casos son los criterios de aceptación |
| **Backend** (Juan Pablo) | §6 (cómo funciona por dentro) y `java/src/dac/CargadorDeContratos.java` |
| **Cualquiera que solo quiera verlo correr** | §7 y §8 |

---

**Referencias:** [HU-04](historias-usuario.md#hu-04--no-generar-alertas-a-partir-de-filas-incompletas) ·
[R-6](reglas-de-negocio.md) ·
[Decisión 12 y 13](decisiones-tecnicas.md) ·
[README del módulo](../java/README.md)
