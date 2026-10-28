# Deuda técnica

Con qué deuda nos quedamos al cerrar las 8 historias del backlog y poner
la interfaz web. Cada ítem dice **qué es**, **por qué se aceptó**, **qué
cuesta no pagarlo** y **cómo se paga**. Se prioriza por impacto en la
confianza del analista (S-1) y por riesgo de seguridad (S-7, S-8).

Prioridad: **Alta** = puede producir una alerta falsa, esconder una real o
exponer datos. **Media** = limita el uso real o encarece el
mantenimiento. **Baja** = incomodidad o costo futuro.

## Resumen

| ID | Deuda | Tipo | Prioridad | Esfuerzo para pagarla |
|---|---|---|---|---|
| DT-01 | Las alertas de fraccionamiento no se guardan en la base | Diseño | Alta | Medio |
| DT-02 | Reglas de HU-03 y HU-05 sin validar con un analista real | Negocio | Alta | Bajo (reunión) + configuración |
| DT-03 | La API y la pantalla no tienen inicio de sesión | Seguridad | Alta | Medio |
| DT-04 | Credenciales de la base escritas en el repositorio | Seguridad | Alta | Bajo |
| DT-05 | Alertas guardadas que ya no se recalculan (alertas "huérfanas") | Diseño | Media | Medio |
| DT-06 | Cada carga relee **todo** el histórico en memoria | Rendimiento | Media | Alto |
| DT-07 | `monto` y `fecha` siguen siendo texto en el dominio | Diseño | Media | Medio |
| DT-08 | Los conflictos (HU-06) se informan pero no se guardan | Diseño | Media | Bajo |
| DT-09 | Dos cargas simultáneas del mismo archivo pueden chocar | Concurrencia | Media | Medio |
| DT-10 | El esquema se crea borrando tablas; no hay migraciones | Operación | Media | Medio |
| DT-11 | Consultas repetidas por fila al guardar (N+1) | Rendimiento | Baja | Bajo |
| DT-12 | La interfaz web no tiene pruebas automáticas propias | Pruebas | Baja | Medio |
| DT-13 | Un solo tope de cuantía para todas las entidades | Negocio | Baja | Medio |

## Detalle

### DT-01 — Las alertas de fraccionamiento no se guardan · Alta

**Qué es.** Las de reincidencia se guardan en `alerta` y `alerta_contrato`;
las de fraccionamiento se calculan cada vez que se carga o se consulta.

**Por qué se aceptó.** La tabla `alerta` exige un `id_funcionario` y su
`CHECK` solo admite `REINCIDENCIA`. El fraccionamiento se agrupa por
contratista y **entidad**, y cada contrato puede tener un funcionario
distinto. Cambiarlo exigía rediseñar el modelo físico, que es una
evidencia ya revisada ([Decisión 15](decisiones-tecnicas.md)).

**Costo de no pagarla.** No hay fecha de "generada en" ni historial de
estas alertas: si cambia el tope (DT-02), las alertas anteriores
desaparecen sin rastro. Rompe la idea de "sin evidencia guardada no hay
alerta".

**Cómo se paga.** Hacer `id_funcionario` opcional, agregar `id_entidad` a
`alerta`, ampliar el `CHECK` con `FRACCIONAMIENTO` y ajustar `uq_alerta_par`.

### DT-02 — Reglas provisionales sin validar · Alta

**Qué es.** El tope ($100.000.000), la ventana (30 días) y qué diferencias
de escritura se ignoran los decidió el equipo ([Decisiones 19 y 20](decisiones-tecnicas.md)),
igual que la política de filas incompletas ([Decisión 12](decisiones-tecnicas.md)).

**Costo de no pagarla.** Un tope demasiado alto esconde fraccionamientos
reales; uno demasiado bajo llena la pantalla de alertas falsas y el
analista deja de confiar. Es el riesgo que el proyecto declaró como el más
grave desde la visión.

**Cómo se paga.** Una sesión con un analista (S-1) para confirmar los
valores. Cambiarlos es editar `application.properties` y, para HU-05, la
clase `NormalizadorDeNombres`. Responsable: rol de requisitos.

### DT-03 — Sin inicio de sesión · Alta (deuda de seguridad)

**Qué es.** Cualquiera que llegue al puerto 8080 puede cargar archivos,
ver las alertas y exportarlas.

**Por qué se aceptó.** El sistema corre en el computador del analista,
con datos ficticios, y no está desplegado.

**Costo de no pagarla.** Con datos reales, los nombres de funcionarios y
contratistas señalados son información sensible (S-7, S-8). Una alerta
filtrada puede dañar la reputación de alguien inocente (R-4, S-6).
**No se debe desplegar en red sin pagar esta deuda.**

**Cómo se paga.** Spring Security con usuarios por rol (analista,
coordinador) y HTTPS.

### DT-04 — Credenciales en el repositorio · Alta (deuda de seguridad)

**Qué es.** `dac_user` / `dac_pass` están en `application.properties`,
`docker-compose.yml` y el flujo de CI.

**Por qué se aceptó.** Son de la base local de desarrollo y de pruebas,
sin datos reales.

**Costo de no pagarla.** Si alguien reutiliza esa configuración en un
servidor real, la clave ya es pública en GitHub.

**Cómo se paga.** Leerlas de variables de entorno
(`${DB_PASSWORD}`) y, en GitHub Actions, de *secrets*; dejar en el
repositorio solo un `.env.example`.

### DT-05 — Alertas guardadas que ya no se recalculan · Media

**Qué es.** Una alerta de reincidencia guardada no se borra si la regla
cambia. Ejemplo: antes de HU-05, "ACME SAS" y "acme sas" eran pares
distintos; si alguna base vieja tenía una alerta por cada variante, ahí
siguen.

**Costo de no pagarla.** La tabla `alerta` y la pantalla (que recalcula)
pueden no coincidir.

**Cómo se paga.** Al cambiar una regla, recalcular y reemplazar las
alertas guardadas en la misma transacción, o versionar la regla en la alerta.

### DT-06 — Cada carga relee todo el histórico · Media

**Qué es.** `ContratoServicio` trae **todos** los contratos a memoria para
detectar las dos señales, en cada carga y en cada consulta.

**Costo de no pagarla.** Con los volúmenes de SECOP (millones de
contratos) la carga se vuelve lenta y puede agotar la memoria.

**Cómo se paga.** Detectar con consultas SQL (`GROUP BY` por par y
ventanas por fecha) o solo sobre los pares afectados por la carga nueva.

### DT-07 — `monto` y `fecha` como texto en el dominio · Media

**Qué es.** El `Contrato` del dominio los guarda como `String` (R-8). El
fraccionamiento los convierte cada vez que evalúa y la persistencia otra vez.

**Costo de no pagarla.** Conversiones repetidas y validaciones en dos
sitios: un formato nuevo de fecha (por ejemplo `15/01/2026`) hay que
arreglarlo en dos lugares.

**Cómo se paga.** Convertir y validar una sola vez al cargar
(`BigDecimal`, `LocalDate`), reportando la fila si el valor no sirve.

### DT-08 — Conflictos que no se guardan · Media

**Qué es.** HU-06 informa el conflicto en la respuesta y en la pantalla,
pero no queda registrado.

**Costo de no pagarla.** Si el analista cierra la pantalla, pierde el
aviso; no hay forma de decidir después cuál versión era la correcta.

**Cómo se paga.** Tabla `conflicto_carga` con la versión rechazada y el
estado de la revisión.

### DT-09 — Cargas simultáneas · Media

**Qué es.** Si dos personas cargan el mismo archivo al mismo tiempo, ambas
pueden ver que el contrato "no existe" e intentar insertarlo. La base lo
impide (`uq_contrato_clave_natural`), pero la segunda carga termina en un
error 500 en vez de un mensaje claro.

**Cómo se paga.** Capturar la violación de unicidad y reintentar, o usar
`INSERT … ON CONFLICT DO NOTHING`. Ya estaba registrado como pendiente en
[rebanada §4](../esqueleto/rebanada.md).

### DT-10 — Sin migraciones · Media

**Qué es.** `db/schema.sql` empieza con `DROP TABLE`: sirve para crear la
base, no para cambiarla.

**Costo de no pagarla.** Pagar DT-01 o DT-08 en una base con datos
reales obligaría a borrarlos.

**Cómo se paga.** Flyway con scripts numerados (`V1__esquema.sql`,
`V2__alerta_fraccionamiento.sql`).

### DT-11 — Consultas N+1 al guardar · Baja

**Qué es.** Por cada fila se busca la entidad, el contratista, el
funcionario y el contrato por separado.

**Costo.** Archivos de miles de filas tardan más de lo necesario.

**Cómo se paga.** Cargar en lote los nombres ya existentes antes del ciclo.

### DT-12 — Interfaz sin pruebas propias · Baja

**Qué es.** Hay una prueba que verifica que la página se sirve y la API
que usa está cubierta, pero el JavaScript (pestañas, arrastrar archivo,
mensajes de error) se probó a mano.

**Cómo se paga.** Pruebas con Playwright en el CI.

### DT-13 — Un solo tope para todas las entidades · Baja

**Qué es.** En la contratación pública colombiana el tope de menor cuantía
depende del presupuesto de cada entidad; DAC usa uno solo.

**Cómo se paga.** Una tabla de topes por entidad y año, cargada por el analista.

## Lo que sí se pagó en esta entrega

| Deuda anterior | Cómo se pagó |
|---|---|
| Nombres escritos distinto no se reconocían (pendiente 4 del mapa) | HU-05 |
| Un duplicado con datos distintos se perdía en silencio (pendiente 7) | HU-06 |
| No había CI: las pruebas se corrían a mano (pendiente 1) | GitHub Actions, [Decisión 22](decisiones-tecnicas.md) |
| La demostración solo se podía hacer con `curl` | Interfaz web, [Decisión 21](decisiones-tecnicas.md) |
| El error 400 no decía qué estaba mal | Respuesta con mensaje, [Decisión 23](decisiones-tecnicas.md) |
