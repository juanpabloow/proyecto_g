# Detector de Alertas de Contratación (DAC)

Sistema de apoyo para analistas de investigación e inteligencia que
detecta señales de posible soborno o extorsión en la contratación
pública, cruzando automáticamente datos que hoy se revisan a mano en
hojas de cálculo.

Cada alerta viene con **los contratos exactos que la originaron**, para
que el analista pueda verificarla en segundos. DAC **no acusa a nadie**:
una alerta es una hipótesis a verificar por una persona
([por qué](docs/vision-producto.md)).

## Integrantes

| Integrante | Rol | Ficha |
|---|---|---|
| Juan Pablo Cardozo Rivera | Ingeniero de backend | [ficha](docs/Equipo/juan-pablo-cardozo-rivera.md) |
| Yerson Andrés Pérez Cadena | Ingeniero de QA y pruebas | [ficha](docs/Equipo/yerson_cadena.md) |
| Gerson Geovanni Rojo Rodríguez | Ingeniero de requisitos y analista de negocio | [ficha](docs/Equipo/gerson_rojo.md) |

## Estado actual (Semana 4 — esqueleto andante)

La primera rebanada vertical funciona de punta a punta y cruza las cinco
fronteras técnicas: HTTP → aplicación → dominio → persistencia real →
migración. El detalle, con su trazabilidad a las evidencias del proyecto,
está en [esqueleto/rebanada.md](esqueleto/rebanada.md).

- `POST /api/contratos/cargar` recibe el CSV y responde con los contratos
  nuevos, el total en base de datos, las filas descartadas y las alertas.
- Persiste en **PostgreSQL** real ([db/schema.sql](db/schema.sql), 6 tablas).
- **Problema duro resuelto:** un contrato duplicado no genera una alerta
  falsa ([detalle](docs/problema-duro.md)). Y ahora la clave natural es una
  restricción de la base, así que la idempotencia también vale **entre
  ejecuciones** ([HU-07](docs/historias-usuario.md)).
- Detecta la señal del MVP: reincidencia contratista–funcionario, sobre el
  histórico completo en base de datos.
- **[HU-04](docs/historias-usuario.md):** una fila sin contratista o sin
  funcionario no produce una alerta sobre nadie — se descarta y se reporta
  ([Decisión 12](docs/decisiones-tecnicas.md)).
- Las alertas y su evidencia quedan guardadas en la base, no solo
  devueltas en la respuesta.
- **28 pruebas automatizadas**, todas pasando: 25 de dominio y 3 de punta
  a punta por HTTP real contra la base de datos real.

Todo el sistema está en **un solo lenguaje**: Java 21 con Spring Boot. La
implementación paralela en Python se consolidó aquí
([Decisión 18](docs/decisiones-tecnicas.md)).

## Cómo correrlo

Requiere **Docker**, **JDK 21+** y **Maven**.

### 1. Levantar la base de datos

```bash
docker compose up -d
```

PostgreSQL 16 en `localhost:5433` y un visor web en `localhost:8081`.
La primera vez crea las 6 tablas desde [db/schema.sql](db/schema.sql) y
siembra datos de ejemplo desde [db/datos.sql](db/datos.sql).

Para ver la base en el navegador: **http://localhost:8081** — servidor
`db`, usuario `dac_user`, clave `dac_pass`, base `dac`. Muestra las
tablas, los datos y el diagrama de relaciones sin escribir SQL.

### 2. Correr las pruebas

```bash
cd java && mvn test
```

Esperado: `Tests run: 28, Failures: 0, Errors: 0`. Las 25 de dominio no
necesitan la base; las 3 de punta a punta sí.

### 3. Levantar el servidor

```bash
java/ejecutar.sh
```

Queda ocupada la terminal. `Ctrl+C` para pararlo. Abre **otra terminal**
para los comandos siguientes.

### 4. Usarlo

```bash
curl -F archivo=@data/contratos_ejemplo.csv http://localhost:8080/api/contratos/cargar
```

Responde `contratosNuevos`, `totalEnBd`, `duplicadosIgnorados`,
`filasDescartadas` y `alertas` con su evidencia.

> Los datos de `data/contratos_ejemplo.csv` son **ficticios**. Los
> archivos con datos reales van en `data/privado/`, que no se sube al
> repositorio.

## Cómo demostrarlo

> ⚠️ **Antes de empezar, reinicia a estado limpio.** El contenedor siembra
> los mismos 5 contratos que trae el CSV de ejemplo, así que sobre una
> base recién creada la primera carga responde `contratosNuevos: 0`. Es
> correcto —es la idempotencia funcionando contra los datos sembrados—
> pero arruina la demostración, porque no se ve el contraste.

### Reiniciar a estado limpio

```bash
docker exec dac_db psql -U dac_user -d dac -c "TRUNCATE alerta_contrato, alerta, contrato, funcionario, contratista, entidad RESTART IDENTITY CASCADE"
```

Vacía las tablas sin borrar el esquema. Es lo mismo que hacen las pruebas
antes de cada caso. Ejecútalo cada vez que quieras repetir la
demostración desde cero.

### Momento 1 — funciona de punta a punta

```bash
curl -s -F archivo=@data/contratos_ejemplo.csv \
  http://localhost:8080/api/contratos/cargar | python3 -m json.tool
```

→ `contratosNuevos: 5`, `totalEnBd: 5`, **1 alerta**: `ACME SAS` +
`Juan Pérez` con **3 contratos de evidencia**. Dos de esos contratos son
de entidades distintas: la señal cruza instituciones (R-3).

### Momento 2 — el problema duro

Repite **exactamente el mismo comando**:

```bash
curl -s -F archivo=@data/contratos_ejemplo.csv \
  http://localhost:8080/api/contratos/cargar | python3 -m json.tool
```

→ `contratosNuevos: 0`, `totalEnBd: 5`. El archivo se cargó dos veces y
no se duplicó nada, ni los contratos ni la alerta. No depende de que el
programa se acuerde: lo impide la restricción `uq_contrato_clave_natural`
de la base de datos ([HU-07](docs/historias-usuario.md)).

### Momento 3 — filas incompletas (HU-04)

```bash
curl -s -F archivo=@data/contratos_incompletos_ejemplo.csv \
  http://localhost:8080/api/contratos/cargar | python3 -m json.tool
```

→ `200 OK` con 4 contratos cargados, **3 filas descartadas con su número
de fila y el motivo**, 1 duplicado ignorado, y la reincidencia real
intacta. Nadie recibe una alerta "sobre nadie", y el archivo no se
invalida por tres filas malas ([Decisión 12](docs/decisiones-tecnicas.md)).

### Momento 4 — la ruta de error

```bash
printf 'numero_contrato,entidad\n001,X\n' > /tmp/malo.csv
curl -s -o /dev/null -w "HTTP %{http_code}\n" -F archivo=@/tmp/malo.csv \
  http://localhost:8080/api/contratos/cargar
```

→ `HTTP 400`, y la base queda intacta: una carga rechazada no escribe
nada ([R-1](docs/reglas-de-negocio.md)).

### Mostrarlo en el navegador

Con **http://localhost:8081** abierto en la tabla `contrato`, ejecuta el
Momento 2 y refresca: **siguen 5 filas**. La idempotencia se ve, no solo
se cuenta.

Para mostrar el problema duro convertido en restricción: clic en
`contrato` → *Mostrar estructura* → `uq_contrato_clave_natural`.

## Si algo falla

| Síntoma | Causa | Solución |
|---|---|---|
| `Port 8080 was already in use` | Quedó un servidor anterior vivo | `lsof -ti:8080 \| xargs kill` |
| El servidor no arranca | La base está apagada | `docker compose up -d` |
| `relation ... does not exist` | El volumen quedó a medias | `docker compose down -v && docker compose up -d` |
| `contratosNuevos: 0` en la primera carga | El contenedor sembró `db/datos.sql` | Reinicia a estado limpio (arriba) |
| Números distintos a los esperados | `mvn test` dejó otro estado en la base | Reinicia a estado limpio (arriba) |
| `mvn: command not found` | Terminal abierta antes de instalar Maven | Abre una terminal nueva |

Para borrar **todo**, incluidos los datos, y volver al punto de partida:

```bash
docker compose down -v && docker compose up -d
```

## Documentación

Empieza por el [mapa del proyecto](docs/mapa-del-proyecto.md) si no sabes
dónde buscar algo.

| Documento | Qué responde |
|---|---|
| [Mapa del proyecto](docs/mapa-del-proyecto.md) | Dónde está cada cosa, cómo funciona de punta a punta, qué falta |
| [Rebanada del esqueleto andante](esqueleto/rebanada.md) | La primera rebanada ejecutable: secuencia, clases, la prueba única, trazabilidad y vacíos declarados |
| [Visión del producto](docs/vision-producto.md) | Qué problema resuelve, para quién, alcance y métricas de éxito |
| [Stakeholders](docs/stakeholders.md) | Quiénes están involucrados y qué exige cada uno del diseño |
| [Problema duro](docs/problema-duro.md) | Idempotencia: por qué es el problema duro y hasta dónde está resuelto |
| [Historias de usuario](docs/historias-usuario.md) | Backlog priorizado y la historia central desarrollada a fondo |
| [Reglas de negocio](docs/reglas-de-negocio.md) | Qué cuenta como alerta, glosario del dominio y limitaciones conocidas |
| [Diagrama de dominio](docs/diagrama-dominio.md) | Las clases del sistema y por qué están modeladas así |
| [Esquema de base de datos](docs/esquema-bd.md) | Modelo entidad–relación: qué se guarda y cómo se relaciona |
| [Modelo físico](db/schema.sql) | El esquema anterior traducido a PostgreSQL, ejecutable |
| [Módulo Java — HU-04](docs/modulo-java-hu04.md) | Qué entrega el módulo Java, cómo se relaciona con las demás historias y cómo demostrarlo |
| [Decisiones técnicas](docs/decisiones-tecnicas.md) | Por qué se decidió cada cosa y qué se descartó |
| [Equipo](docs/Equipo/) | Rol y aporte de cada integrante |
| [Uso de IA](docs/uso-ia.md) | Cómo se usó la IA y la postura del equipo |
