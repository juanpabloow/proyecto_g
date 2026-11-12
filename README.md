# Detector de Alertas de Contratación (DAC)

Sistema de apoyo para analistas de investigación e inteligencia que
detecta señales de posible soborno o extorsión en la contratación
pública, cruzando automáticamente datos que hoy se revisan a mano en
hojas de cálculo.

Cada alerta viene con **los contratos exactos que la originaron**, para
que el analista pueda verificarla en segundos. DAC **no acusa a nadie**:
una alerta es una hipótesis a verificar por una persona
([por qué](docs/vision-producto.md)).

## Video de la entrega

**[COMPLETAR: enlace al video]** (3 minutos; guion en
[docs/demo-entrega-final.md](docs/demo-entrega-final.md#guion-del-video-3-minutos)).

## Integrantes

| Integrante | Rol | Ficha |
|---|---|---|
| Juan Pablo Cardozo Rivera | Ingeniero de backend | [ficha](docs/Equipo/juan-pablo-cardozo-rivera.md) |
| Yerson Andrés Pérez Cadena | Ingeniero de QA y pruebas | [ficha](docs/Equipo/yerson_cadena.md) |
| Gerson Geovanni Rojo Rodríguez | Ingeniero de requisitos y analista de negocio | [ficha](docs/Equipo/gerson_rojo.md) |

## Estado actual — backlog completo (8 de 8 historias) e interfaz web

[![CI](https://github.com/juanpabloow/proyecto_g/actions/workflows/ci.yml/badge.svg)](https://github.com/juanpabloow/proyecto_g/actions/workflows/ci.yml)

- **Las 8 historias del backlog están hechas**, cada una con sus pruebas
  ([historias de usuario](docs/backlog.md)).
- **Dos señales de alerta**, cada una con los contratos que la originaron:
  - **Reincidencia** (HU-02): el mismo contratista con el mismo
    funcionario en 2 o más contratos, aunque sean de entidades distintas.
  - **Fraccionamiento** (HU-03): contratos pequeños del mismo contratista
    en la misma entidad que, en 30 días, suman el tope de cuantía.
- **Nombres escritos distinto** ("ACME SAS", "acme s.a.s.") se reconocen
  como el mismo (HU-05).
- **Problema duro resuelto:** cargar el mismo archivo dos veces no duplica
  nada, ni dentro de una carga ni entre ejecuciones (HU-07,
  [detalle](docs/problema-duro.md)). Si el duplicado traía datos
  distintos, se **reporta el conflicto** en vez de perderlo (HU-06).
- Las filas incompletas se descartan y se reportan (HU-04).
- **Interfaz web** en http://localhost:8080 para cargar el CSV, ver las
  alertas con su evidencia y **exportar el reporte** a Excel (HU-08).
- **93 pruebas automatizadas:** 63 de dominio y 30 de punta a punta por
  HTTP real contra PostgreSQL, incluidas pruebas de ataques simulados y la
  medición de los dos requisitos no funcionales. Corren solas en cada Pull
  Request (GitHub Actions).
- **Seguridad básica y RNF (Módulo 7):** checklist con hallazgos
  corregidos, secretos fuera del repositorio y RNF medidos en
  [docs/seguridad-y-rnf.md](docs/seguridad-y-rnf.md).
- Deuda técnica declarada y priorizada en
  [docs/deuda-tecnica.md](docs/deuda-tecnica.md).

Todo el sistema está en **un solo lenguaje**: Java 21 con Spring Boot. La
implementación paralela en Python se consolidó aquí
([Decisión 18](docs/decisiones-tecnicas.md)).

## Cómo correrlo

Requiere **Docker** y **JDK 21 o superior**. Maven **no** hace falta: el
Maven Wrapper (`./mvnw`, o `mvnw.cmd` en Windows) descarga la versión
correcta la primera vez. Todos los comandos se corren desde la **raíz del
proyecto**.

```bash
cp .env.example .env        # 1. secretos locales: edítalo y cambia las claves
docker compose up -d        # 2. PostgreSQL 16 y el visor web de la base
./mvnw spring-boot:run      # 3. DAC en http://localhost:8080 (deja esta terminal abierta)
./mvnw verify               # 4. en otra terminal: compila y corre las 93 pruebas
```

### 1. El archivo `.env`

`.env` no se sube al repositorio (está en `.gitignore`). Lo leen Docker y la
aplicación: `./mvnw spring-boot:run` lo carga solo, no hay que exportar nada
([Decisión 24](docs/decisiones-tecnicas.md)). Variables:

| Variable | Para qué | ¿Obligatoria? |
|---|---|---|
| `DB_USER`, `DB_PASSWORD`, `DB_NAME` | Usuario y clave de PostgreSQL | Sí |
| `DAC_AUTH_USER`, `DAC_AUTH_PASSWORD` | Usuario y clave para **entrar a DAC** (pantalla de login y API) | Sí. Si faltan, la aplicación no arranca y dice qué definir |
| `GRAFANA_ADMIN_PASSWORD` | Clave del usuario `admin` de Grafana | Solo para la observabilidad opcional |

No hay credencial por defecto. Solo el perfil de desarrollo trae una
credencial demo para probar en tu computador:
`./mvnw spring-boot:run -Dspring-boot.run.profiles=dev` entra con
`analista` / `dac-demo-2026`. Nunca lo uses con datos reales.

### 2. La base de datos

`docker compose up -d` levanta PostgreSQL 16 en `localhost:5433` y un visor
web en `localhost:8081`, publicados **solo en 127.0.0.1**. La primera vez
crea las 6 tablas desde [db/schema.sql](db/schema.sql) y siembra 5 contratos
de ejemplo desde [db/datos.sql](db/datos.sql). Para mirar lo que hay
adentro, ve a [Ver los datos](#ver-los-datos).

> Si ya tenías la base creada con otra clave, pon esa misma clave en `.env`
> o borra el volumen con `docker compose down -v`.

### 3. El servidor y cómo entrar

`./mvnw spring-boot:run` deja la terminal ocupada; `Ctrl+C` lo detiene.
Abre **http://localhost:8080**: te lleva a la pantalla de login. Entra con
el `DAC_AUTH_USER` y el `DAC_AUTH_PASSWORD` de tu `.env`. Para otro puerto:
`SERVER_PORT=8090 ./mvnw spring-boot:run`.

Para los ejemplos con `curl`, carga tus variables en esa otra terminal:

```bash
set -a; source .env; set +a
```

Los ejemplos canalizan la respuesta a [`jq`](https://jqlang.github.io/jq/)
solo para leerla formateada: si no lo tienes, quita el `| jq`.

### Pruebas

`./mvnw verify` necesita la base corriendo. Esperado:
`Tests run: 93, Failures: 0, Errors: 0, Skipped: 0` y `BUILD SUCCESS`. Las
63 de dominio no usan la base. Las 30 de punta a punta sí, y **vacían sus
tablas** (`TRUNCATE`): no las corras sobre datos que quieras conservar.

`java/ejecutar.sh` sigue existiendo como atajo (`run`, `dev`, `build`) sobre
los mismos comandos del wrapper.

### Usarlo

**Desde el navegador:** abre **http://localhost:8080**, entra con tu
usuario y clave, arrastra un CSV
(por ejemplo `data/contratos_ejemplo.csv`) y pulsa **Analizar archivo**.
Las pestañas muestran las alertas de reincidencia y de fraccionamiento
con su evidencia, las filas descartadas y los conflictos. El botón
**Exportar reporte (CSV)** descarga las alertas para abrirlas en Excel.

Para ver cada señal hay un archivo de ejemplo:

| Archivo | Qué muestra |
|---|---|
| `data/contratos_ejemplo.csv` | 1 reincidencia (ACME SAS + Juan Pérez) y 1 fraccionamiento en la Gobernación |
| `data/contratos_incompletos_ejemplo.csv` | Filas descartadas y un duplicado (HU-04) |
| `data/contratos_nombres_variantes_ejemplo.csv` | El mismo par escrito de tres formas (HU-05) |
| `data/contratos_fraccionamiento_ejemplo.csv` | 3 contratos que fraccionan 105 millones en 18 días (HU-03) |
| `data/contratos_conflicto_ejemplo.csv` | Un contrato ya guardado que llega con otro monto y un duplicado con datos distintos (HU-06) |
| `data/contratos_ataques_ejemplo.csv` | Nombres con inyección SQL y XSS que se guardan y muestran como texto (S-03, S-04) |

**Desde la terminal:**

```bash
curl -u "$DAC_AUTH_USER:$DAC_AUTH_PASSWORD" \
  -F archivo=@data/contratos_ejemplo.csv http://localhost:8080/api/contratos/cargar
```

Sin `-u` la API responde `401`.

Responde `contratosNuevos`, `contratosYaGuardados`, `totalEnBd`, `duplicadosIgnorados`,
`filasDescartadas`, `conflictos`, `alertas` y `alertasFraccionamiento`
con su evidencia. Otros endpoints:

```bash
curl -u "$DAC_AUTH_USER:$DAC_AUTH_PASSWORD" http://localhost:8080/api/alertas            # estado actual
curl -u "$DAC_AUTH_USER:$DAC_AUTH_PASSWORD" -OJ http://localhost:8080/api/alertas/exportar  # descarga alertas_dac.csv
curl http://localhost:8080/actuator/health                                              # público: {"status":"UP",...}
```

> Los datos de `data/contratos_ejemplo.csv` son **ficticios**. Los
> archivos con datos reales van en `data/privado/`, que no se sube al
> repositorio.

## Cómo demostrarlo

> El guion completo de la entrega final, con una demostración por historia,
> por control de seguridad y por RNF, está en
> [docs/demo-entrega-final.md](docs/demo-entrega-final.md). Los momentos de
> abajo son la versión corta.

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
curl -s -u "$DAC_AUTH_USER:$DAC_AUTH_PASSWORD" -F archivo=@data/contratos_ejemplo.csv \
  http://localhost:8080/api/contratos/cargar | jq
```

→ `contratosNuevos: 5`, `totalEnBd: 5`, **1 alerta**: `ACME SAS` +
`Juan Pérez` con **3 contratos de evidencia**. Dos de esos contratos son
de entidades distintas: la señal cruza instituciones (R-3).

### Momento 2 — el problema duro

Repite **exactamente el mismo comando**:

```bash
curl -s -u "$DAC_AUTH_USER:$DAC_AUTH_PASSWORD" -F archivo=@data/contratos_ejemplo.csv \
  http://localhost:8080/api/contratos/cargar | jq
```

→ `contratosNuevos: 0`, `totalEnBd: 5`. El archivo se cargó dos veces y
no se duplicó nada, ni los contratos ni la alerta. No depende de que el
programa se acuerde: lo impide la restricción `uq_contrato_clave_natural`
de la base de datos ([HU-07](docs/backlog.md)).

### Momento 3 — filas incompletas (HU-04)

```bash
curl -s -u "$DAC_AUTH_USER:$DAC_AUTH_PASSWORD" -F archivo=@data/contratos_incompletos_ejemplo.csv \
  http://localhost:8080/api/contratos/cargar | jq
```

→ `200 OK` con 4 contratos cargados, **3 filas descartadas con su número
de fila y el motivo**, 1 duplicado ignorado, y la reincidencia real
intacta. Nadie recibe una alerta "sobre nadie", y el archivo no se
invalida por tres filas malas ([Decisión 12](docs/decisiones-tecnicas.md)).

### Momento 4 — la ruta de error

```bash
printf 'numero_contrato,entidad\n001,X\n' > /tmp/malo.csv
curl -s -o /dev/null -w "HTTP %{http_code}\n" -u "$DAC_AUTH_USER:$DAC_AUTH_PASSWORD" -F archivo=@/tmp/malo.csv \
  http://localhost:8080/api/contratos/cargar
```

→ `HTTP 400`, y la base queda intacta: una carga rechazada no escribe
nada ([R-1](docs/reglas-de-negocio.md)).

### Mostrarlo en pantalla, no en números

Con el [visor web](#en-el-navegador) abierto en la tabla `contrato`,
ejecuta el Momento 2 y refresca: **siguen 5 filas**. La idempotencia se
ve, no solo se cuenta.

Y para señalar el problema duro convertido en restricción: clic en
`contrato` → *Mostrar estructura* → `uq_contrato_clave_natural`.

## Ver los datos

### En el navegador

**http://localhost:8081** — servidor `db`, base `dac`, y el usuario y la
clave que pusiste en tu archivo `.env`. Clic en `contrato` → *Select data*. Las llaves
foráneas son enlaces navegables: clic en un `id_contratista` y saltas al
contratista.

En la pantalla principal, el enlace **"Esquema de base de datos"** dibuja
las 6 tablas con sus relaciones, generado desde la base real.

### En la terminal, como tabla

Todas las consultas están listas en [db/consultas.sql](db/consultas.sql):

```bash
docker exec -i dac_db psql -U dac_user -d dac < db/consultas.sql
```

**Los contratos con nombres en vez de identificadores:**

```bash
docker exec dac_db psql -U dac_user -d dac -c "
SELECT c.numero_contrato AS num, e.nombre AS entidad, ct.nombre AS contratista,
       f.nombre AS funcionario, c.monto, c.fecha
FROM contrato c
JOIN entidad e      USING (id_entidad)
JOIN contratista ct USING (id_contratista)
JOIN funcionario f  USING (id_funcionario)
ORDER BY e.nombre, c.numero_contrato;"
```

```
 num |       entidad       |    contratista    | funcionario  |    monto    |   fecha
-----+---------------------+-------------------+--------------+-------------+------------
 001 | Alcaldía de Ejemplo | ACME SAS          | Juan Pérez   | 50000000.00 | 2026-01-15
 002 | Alcaldía de Ejemplo | Otra Empresa Ltda | Ana Ruiz     | 30000000.00 | 2026-02-10
 004 | Alcaldía de Ejemplo | Constructora XYZ  | Carlos Gómez | 80000000.00 | 2026-03-20
 003 | Gobernación Ejemplo | ACME SAS          | Juan Pérez   | 45000000.00 | 2026-03-05
 005 | Gobernación Ejemplo | ACME SAS          | Juan Pérez   | 60000000.00 | 2026-04-01
(5 rows)
```

**Las alertas con su evidencia** — cada fila es un contrato que sustenta
la alerta:

```bash
docker exec dac_db psql -U dac_user -d dac -c "
SELECT a.tipo, ct.nombre AS contratista, f.nombre AS funcionario,
       c.numero_contrato AS evidencia, e.nombre AS entidad
FROM alerta a
JOIN contratista ct     USING (id_contratista)
JOIN funcionario f      USING (id_funcionario)
JOIN alerta_contrato ac USING (id_alerta)
JOIN contrato c         USING (id_contrato)
JOIN entidad e          USING (id_entidad)
ORDER BY c.numero_contrato;"
```

```
     tipo     | contratista | funcionario | evidencia |       entidad
--------------+-------------+-------------+-----------+---------------------
 REINCIDENCIA | ACME SAS    | Juan Pérez  | 001       | Alcaldía de Ejemplo
 REINCIDENCIA | ACME SAS    | Juan Pérez  | 003       | Gobernación Ejemplo
 REINCIDENCIA | ACME SAS    | Juan Pérez  | 005       | Gobernación Ejemplo
(3 rows)
```

Las tres filas son la respuesta a *"¿por qué me estás señalando a esta
pareja?"*. Dos de esos contratos son de la gobernación y uno de la
alcaldía: la señal cruza instituciones (R-3).

**Un conteo rápido de todo:**

```bash
docker exec dac_db psql -U dac_user -d dac -c "
SELECT (SELECT count(*) FROM contrato)        AS contratos,
       (SELECT count(*) FROM alerta)          AS alertas,
       (SELECT count(*) FROM alerta_contrato) AS evidencia;"
```

**Sesión interactiva**, para explorar a mano:

```bash
docker exec -it dac_db psql -U dac_user -d dac
```

Dentro: `\dt` lista las tablas, `\d contrato` muestra su estructura con
las restricciones —ahí se ve `uq_contrato_clave_natural`, el problema
duro— y `\q` sale.

## Si algo falla

| Síntoma | Causa | Solución |
|---|---|---|
| `Port 8080 was already in use` | Quedó un servidor anterior vivo | `lsof -ti:8080 \| xargs kill` |
| El servidor no arranca | La base está apagada | `docker compose up -d` |
| `relation ... does not exist` | El volumen quedó a medias | `docker compose down -v && docker compose up -d` |
| `contratosNuevos: 0` en la primera carga | El contenedor sembró `db/datos.sql` | Reinicia a estado limpio (arriba) |
| Números distintos a los esperados | `./mvnw verify` dejó otro estado en la base | Reinicia a estado limpio (arriba) |
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
| [Backlog](docs/backlog.md) | Las 8 historias con su estado, cada criterio con su prueba Java, y la trazabilidad historia → código → prueba → commit → demostración |
| [Reglas de negocio](docs/reglas-de-negocio.md) | Qué cuenta como alerta, glosario del dominio y limitaciones conocidas |
| [Modelo de dominio](docs/modelo-dominio.md) | Las clases Java reales (campos, métodos y relaciones) en Mermaid |
| [Esquema de base de datos](docs/esquema-bd.md) | Modelo entidad–relación: qué se guarda y cómo se relaciona |
| [Modelo físico](db/schema.sql) | El esquema anterior traducido a PostgreSQL, ejecutable |
| [Módulo Java — HU-04](docs/modulo-java-hu04.md) | Qué entrega el módulo Java, cómo se relaciona con las demás historias y cómo demostrarlo |
| [Decisiones técnicas](docs/decisiones-tecnicas.md) | Por qué se decidió cada cosa y qué se descartó |
| [Equipo](docs/Equipo/) | Rol y aporte de cada integrante |
| [Seguridad y RNF](docs/seguridad-y-rnf.md) | Checklist de seguridad, superficie de entrada, RNF medidos, OWASP Top 10:2025 |
| [Evidencias por semana](docs/seguridad/) | Semana 13 (seguridad) y semana 14 (RNF con gráficas y release 0.3) |
| [Deuda técnica](docs/deuda-tecnica.md) | Con qué deuda nos quedamos, cuánto cuesta no pagarla y cómo se paga |
| [Uso de IA](docs/uso-ia.md) | Registro por actividad del uso de la IA y postura del equipo |
| [Paquete para la entrevista](docs/paquete-entrevista.md) | Problema, problema duro, métricas reales y decisiones, para preparar la entrevista |
| [Guion de demostración](docs/demo-entrega-final.md) | Una demostración por historia, control de seguridad y RNF, verificada en vivo |
