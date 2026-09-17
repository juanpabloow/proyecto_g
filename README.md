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

**La base de datos** (requiere Docker):

```bash
docker compose up -d         # PostgreSQL 16 en localhost:5433 + visor web en localhost:8081
```

Para ver la base en el navegador: **http://localhost:8081** — servidor
`db`, usuario `dac_user`, clave `dac_pass`, base `dac`. Sirve para mostrar
las tablas, los datos y el diagrama de relaciones sin escribir SQL.

**La aplicación** (requiere JDK 21+ y Maven):

```bash
cd java && mvn test          # las 28 pruebas
java/ejecutar.sh             # levanta el endpoint en localhost:8080
```

Con el servidor arriba, la rebanada completa en un comando:

```bash
curl -F archivo=@data/contratos_ejemplo.csv http://localhost:8080/api/contratos/cargar
```

Responde `contratosNuevos`, `totalEnBd`, `filasDescartadas` y `alertas`.
Llamarlo dos veces devuelve `contratosNuevos: 0` la segunda: eso es HU-07.

Para ver HU-04 hace falta un archivo con filas incompletas — el de
ejemplo no las tiene:

```bash
curl -F archivo=@data/contratos_incompletos_ejemplo.csv http://localhost:8080/api/contratos/cargar
```

En `filasDescartadas` viene cada fila que no se cargó y por qué.

> Los datos de `data/contratos_ejemplo.csv` son **ficticios**. Los
> archivos con datos reales van en `data/privado/`, que no se sube al
> repositorio.

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
