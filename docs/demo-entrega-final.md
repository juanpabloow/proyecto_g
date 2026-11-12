# Guion de demostración — entrega final

Una demostración por cada historia de usuario, por cada control de
seguridad y por cada RNF que decimos cumplir. Cada una dice **qué
demuestra**, **qué hacer** (clics en la interfaz o comando exacto) y **qué
debe salir**.

> **Verificado el 11 de noviembre de 2026**, en orden y desde una base
> limpia, con exactamente estos comandos. Los pasos de la interfaz se
> ejecutaron en Google Chrome, conducido por un script (sin ventana); los
> comandos, en la terminal. Los números de k6 y de `EXPLAIN ANALYZE`
> dependen de la máquina: se indica el valor obtenido y el criterio que debe
> cumplirse.

## Antes de empezar

Requiere Docker, JDK 21+, `jq` y, para la demostración 15, k6
(`brew install k6`). Maven no hace falta: se usa el Maven Wrapper (`./mvnw`).
Abre **dos terminales** en la raíz del proyecto.

En la **terminal 2** (la de los comandos), una sola vez:

```bash
set -a; source .env; set +a                   # DB_*, DAC_AUTH_USER, DAC_AUTH_PASSWORD, GRAFANA_ADMIN_PASSWORD
export BASE=http://localhost:8080 BASE_URL=http://localhost:8080
export AUTH="${DAC_AUTH_USER}:${DAC_AUTH_PASSWORD}"
cargar() { curl -s -u "$AUTH" -F archivo=@"$1" "$BASE/api/contratos/cargar"; }
estado() { curl -s -u "$AUTH" "$BASE/api/alertas" | jq -c '{totalEnBd, reincidencias: (.alertas|length), fraccionamientos: (.alertasFraccionamiento|length)}'; }
```

`cargar` hace por terminal lo mismo que el botón **Analizar archivo**, y
`estado` muestra lo que hay en la base. Para las cargas usa la interfaz
**o** `cargar`, no las dos: cargar dos veces el mismo archivo da 0 nuevos
(que es justamente la demostración 4).

---

## 0. Reiniciar a estado limpio y arrancar todo desde cero

**Demuestra:** RNF-7 (despliegue): DAC se levanta desde cero con los
comandos de la guía de entrega, los mismos del README.

1. Si es la primera vez: `cp .env.example .env` y cambia las tres claves
   (`DB_PASSWORD`, `DAC_AUTH_PASSWORD`, `GRAFANA_ADMIN_PASSWORD`).
2. Terminal 2, base desde cero (borra el volumen de la base de demostración):

   ```bash
   docker compose down -v
   docker compose up -d
   until docker exec dac_db pg_isready -q -h 127.0.0.1 -U dac_user -d dac; do sleep 1; done
   docker exec dac_db psql -U dac_user -d dac -c "TRUNCATE alerta_contrato, alerta, contrato, funcionario, contratista, entidad RESTART IDENTITY CASCADE"
   ```

   El `TRUNCATE` vacía los 5 contratos que siembra `db/datos.sql`, para que
   la primera carga muestre el contraste. Para repetir la demostración
   después, basta con esa última línea.
3. Terminal 1: `./mvnw spring-boot:run` (lee la credencial de `.env`).
4. Terminal 2: `curl -s $BASE/actuator/health`

**Resultado esperado:** `TRUNCATE TABLE` y luego
`{"status":"UP","groups":["liveness","readiness"]}`.

---

## 1. Inicio de sesión

**Demuestra:** S-10 (autenticación), OWASP A01/A07.

**En la interfaz:**

1. Abre `http://localhost:8080` → te lleva a **/login.html** (sin sesión no
   se ve nada).
2. Entra con tu usuario y una clave equivocada → **"Usuario o contraseña
   incorrectos."**
3. Entra con la clave de `.env` → pantalla principal con **0** contratos en
   la base.

**Por terminal:**

```bash
curl -s -o /dev/null -w "%{http_code}\n" $BASE/api/alertas                                 # sin credenciales
curl -s -o /dev/null -w "%{http_code}\n" -u "${DAC_AUTH_USER}:clave-mala" $BASE/api/alertas  # clave equivocada
curl -s -o /dev/null -w "%{http_code}\n" -u "$AUTH" $BASE/api/alertas                      # credencial de .env
curl -s -o /dev/null -w "%{http_code} %{redirect_url}\n" -H "Accept: text/html" $BASE/   # la página, como la pide un navegador
```

**Resultado esperado:** `401`, `401`, `200` y `302 http://localhost:8080/login.html`.
Un navegador va al formulario de login. Un cliente que no pide HTML (como
`curl` sin la cabecera `Accept`) recibe un `401` directo.

---

## 2. HU-01 — Cargar contratos desde un CSV

**Demuestra:** HU-01, R-1, R-2.

**En la interfaz:** arrastra `data/contratos_ejemplo.csv` a la zona de
carga y pulsa **Analizar archivo**.

**Resultado esperado:** mensaje **"Listo: 5 contrato(s) nuevo(s)."**;
**5** contratos en la base y **5** nuevos en esta carga.

**Comprobación:** `estado` → `{"totalEnBd":5,"reincidencias":1,"fraccionamientos":1}`

---

## 3. HU-02 — Reincidencia contratista–funcionario

**Demuestra:** HU-02 (historia central), R-3, R-4.

**En la interfaz:** pestaña **Reincidencia (1)**.

**Resultado esperado:** **ACME SAS + Juan Pérez — 3 contratos en 2
entidades · suma $ 155.000.000**, con la tabla de evidencia: 001 (Alcaldía
de Ejemplo, 15/01/2026, $ 50.000.000), 003 (Gobernación Ejemplo,
05/03/2026, $ 45.000.000) y 005 (Gobernación Ejemplo, 01/04/2026,
$ 60.000.000). La alerta trae su evidencia y no trae puntajes.

**Comprobación:**

```bash
curl -s -u "$AUTH" $BASE/api/alertas | jq -c '.alertas[] | {contratista, funcionario, contratos: [.evidencia[].numeroContrato]}'
```

→ `{"contratista":"ACME SAS","funcionario":"Juan Pérez","contratos":["001","003","005"]}`

---

## 4. El problema duro — la misma carga dos veces

**Demuestra:** idempotencia (problema duro), HU-07, RNF-2,
`uq_contrato_clave_natural`.

**En la interfaz:** vuelve a cargar `data/contratos_ejemplo.csv`.

**Resultado esperado:** **"Listo: 0 contrato(s) nuevo(s)."**; siguen **5**
contratos en la base y **1** reincidencia.

**Comprobación:**

```bash
docker exec dac_db psql -U dac_user -d dac -tAc "select count(*) from contrato"
```

→ `5`

**Registro auditable** (en la terminal 1, la del servidor): cada carga deja
una línea, y la recarga se reconoce porque dice `nuevos=0` y todos sus
contratos en `yaGuardados`:

```
AUDITORIA carga aceptada archivo="contratos_ejemplo.csv" bytes=406 nuevos=5 yaGuardados=0 duplicadosEnArchivo=0 totalEnBd=5 descartadas=0 conflictos=0 reincidencias=1 fraccionamientos=1
AUDITORIA carga aceptada archivo="contratos_ejemplo.csv" bytes=406 nuevos=0 yaGuardados=5 duplicadosEnArchivo=0 totalEnBd=5 descartadas=0 conflictos=0 reincidencias=1 fraccionamientos=1
```

La línea no lleva nombres de personas, solo cantidades (S-07).

---

## 5. HU-03 — Fraccionamiento de contratos

**Demuestra:** HU-03, R-9 (tope $100.000.000, ventana 30 días).

**En la interfaz:** carga `data/contratos_fraccionamiento_ejemplo.csv` y
abre la pestaña **Fraccionamiento (2)**.

**Resultado esperado:** **"Listo: 5 contrato(s) nuevo(s)."** y dos alertas:

- **Suministros del Sur en Alcaldía de Ejemplo**: 3 contratos (401, 402 y
  403) en menos de 30 días suman **$ 105.000.000** (tope: $ 100.000.000).
  El 404 (otra entidad) y el 405 (septiembre, fuera de la ventana) no entran.
- **ACME SAS en Gobernación Ejemplo**: 2 contratos (003 y 005) suman
  **$ 105.000.000**.

**Comprobación:**

```bash
curl -s -u "$AUTH" $BASE/api/alertas | jq -c '.alertasFraccionamiento[] | {contratista, entidad, contratos: [.evidencia[].numeroContrato], montoTotal}'
```

→ dos líneas: ACME SAS / Gobernación Ejemplo con `["003","005"]` y Suministros del Sur / Alcaldía de Ejemplo con `["401","402","403"]`, ambas con `"montoTotal":105000000.00`.

---

## 6. HU-04 — Filas incompletas

**Demuestra:** HU-04, R-6, Decisión 12 (la fila se descarta y se reporta;
el archivo sigue siendo válido).

**En la interfaz:** carga `data/contratos_incompletos_ejemplo.csv` y abre
**Filas descartadas (3)**.

**Resultado esperado:** **"Listo: 4 contrato(s) nuevo(s) · 1 repetido(s)
ignorado(s) · 3 fila(s) descartada(s)."**, y en la pestaña:

- Fila 5: faltan los campos contratista, funcionario
- Fila 6: faltan los campos contratista, funcionario
- Fila 7: falta el campo funcionario

**Por terminal (en lugar de la interfaz):**

```bash
cargar data/contratos_incompletos_ejemplo.csv | jq -c '{contratosNuevos, duplicadosIgnorados, filasDescartadas}'
```

→ `{"contratosNuevos":4,"duplicadosIgnorados":1,"filasDescartadas":[{"fila":5,"motivo":"faltan los campos contratista, funcionario"},{"fila":6,"motivo":"faltan los campos contratista, funcionario"},{"fila":7,"motivo":"falta el campo funcionario"}]}`

---

## 7. HU-05 — El mismo nombre escrito distinto

**Demuestra:** HU-05, R-5 (se ignoran mayúsculas, espacios, tildes, puntos
y comas; no los sufijos societarios).

**En la interfaz:** carga `data/contratos_nombres_variantes_ejemplo.csv` y
abre **Reincidencia**.

**Resultado esperado:** **"Listo: 4 contrato(s) nuevo(s)."** y una alerta
**Constructora Andina S.A.S. + María Gómez — 3 contratos en 2 entidades**
(301, 302 y 303, escritos de tres formas). El 304 (**LTDA**) no se une. Se
muestran los nombres **originales**.

**Comprobación:**

```bash
curl -s -u "$AUTH" $BASE/api/alertas | jq -c '.alertas[] | select(.contratista | startswith("Constructora")) | {contratista, funcionario, contratos: [.evidencia[].numeroContrato]}'
```

→ `{"contratista":"Constructora Andina S.A.S.","funcionario":"María Gómez","contratos":["301","302","303"]}`

---

## 8. HU-06 — Duplicados con datos distintos

**Demuestra:** HU-06, Decisión 6 (gana la primera versión, pero la
diferencia se reporta).

**En la interfaz:** carga `data/contratos_conflicto_ejemplo.csv` y abre
**Conflictos (2)**.

**Resultado esperado:** **"Listo: 1 contrato(s) nuevo(s) · 1 repetido(s)
ignorado(s) · 2 conflicto(s) para revisar."**, y en la pestaña:

- Contrato **601** (Alcaldía de Ejemplo), fila 4: choca con **fila 3** en
  **monto, fecha** (conflicto dentro del mismo archivo).
- Contrato **001** (Alcaldía de Ejemplo), fila 2: choca con **contrato ya
  guardado en la base** en **monto** (conflicto contra lo cargado antes).

**Comprobación** (el 001 conserva su monto original):

```bash
docker exec dac_db psql -U dac_user -d dac -tAc "select monto from contrato where numero_contrato='001'"
```

→ `50000000.00`

---

## 9. HU-07 — Los contratos se conservan entre ejecuciones

**Demuestra:** HU-07 (persistencia en PostgreSQL).

1. Terminal 1: `Ctrl+C` para detener el servidor y `./mvnw spring-boot:run`
   para arrancarlo otra vez.
2. En el navegador, recarga la página (la sesión anterior se perdió con el
   reinicio: vuelve a entrar).

**Resultado esperado:** siguen **19** contratos en la base y las mismas
alertas: nada se perdió con el reinicio.

**Comprobación:** `estado` → `{"totalEnBd":19,"reincidencias":4,"fraccionamientos":3}`

---

## 10. HU-08 — Exportar el reporte de alertas

**Demuestra:** HU-08, R-10 (CSV para Excel con BOM y `;`, fórmulas
neutralizadas).

**En la interfaz:** pulsa **Exportar reporte (CSV)** → se descarga
`alertas_dac.csv`.

**Por terminal:**

```bash
curl -s -u "$AUTH" -o /tmp/alertas_dac.csv $BASE/api/alertas/exportar
head -c 3 /tmp/alertas_dac.csv | xxd -p      # BOM UTF-8 para que Excel lea las tildes
sed -n 1p /tmp/alertas_dac.csv               # encabezado
wc -l < /tmp/alertas_dac.csv                 # encabezado + 1 fila por contrato de evidencia
```

**Resultado esperado:** `efbbbf`;
`tipo;alerta;contratista;funcionario;entidad;numero_contrato;monto;fecha;detalle`
(precedido del BOM) y `22`: el encabezado más 21 filas, 13 de las 4
reincidencias y 8 de los 3 fraccionamientos. Una alerta viaja con todos
sus contratos. El archivo que baja la interfaz es idéntico.

**Cerrar sesión:** pulsa **Salir** → vuelves a /login.html con
**"Cerraste sesión."**, y la sesión ya no sirve. Entra de nuevo para
seguir.

---

## 11. Inyección SQL y XSS: el ataque se guarda como texto

**Demuestra:** S-03, S-04, OWASP A05.

**En la interfaz:** carga `data/contratos_ataques_ejemplo.csv` (nombres
como `x'); DROP TABLE contrato; --`, `' OR '1'='1`,
`<script>alert(1)</script>` y `<img src=x onerror=alert(2)>`) y abre
**Reincidencia**.

**Resultado esperado:** **"Listo: 4 contrato(s) nuevo(s)."** Aparecen dos
alertas nuevas cuyos nombres se ven **como texto**, por ejemplo
`<script>alert(1)</script> + <img src=x onerror=alert(2)>`. **No se abre
ninguna ventana de alerta** y la tabla `contrato` sigue existiendo.

**Comprobación:**

```bash
docker exec dac_db psql -U dac_user -d dac -tAc "select count(*) from contrato"
docker exec dac_db psql -U dac_user -d dac -tAc "select nombre from contratista where nombre like 'x%' or nombre like '<%' order by 1"
```

→ `23`, y los dos nombres tal cual: `<script>alert(1)</script>` y
`x'); DROP TABLE contrato; --`.

---

## 12. Validaciones en la frontera

**Demuestra:** S-02, R-8, OWASP A10 (errores con mensaje y sin dejar nada a
medias).

```bash
printf 'hola\n' > /tmp/no-es-csv.txt
curl -s -w "  HTTP %{http_code}\n" -u "$AUTH" -F archivo=@/tmp/no-es-csv.txt $BASE/api/contratos/cargar

head -c 11000000 /dev/zero | tr '\0' 'x' > /tmp/grande.csv
curl -s -w "  HTTP %{http_code}\n" -u "$AUTH" -F archivo=@/tmp/grande.csv $BASE/api/contratos/cargar

printf 'numero_contrato,entidad,contratista,funcionario,monto,fecha\n701,Entidad X,Empresa A,Persona B,1000,2026-01-01\n702,Entidad X,Empresa A,Persona B,-5,2026-01-02\n' > /tmp/negativo.csv
curl -s -w "  HTTP %{http_code}\n" -u "$AUTH" -F archivo=@/tmp/negativo.csv $BASE/api/contratos/cargar

printf 'numero_contrato,entidad,contratista,funcionario,monto,fecha\n801,Entidad X,Empresa A,Persona B,1000,15/01/2026\n' > /tmp/fecha.csv
curl -s -w "  HTTP %{http_code}\n" -u "$AUTH" -F archivo=@/tmp/fecha.csv $BASE/api/contratos/cargar

estado
```

**Resultado esperado:**

```
{"error":"Solo se aceptan archivos .csv"}  HTTP 400
{"error":"El archivo supera el máximo de 10 MB"}  HTTP 413
{"error":"Fila 3: el monto no puede ser negativo (R-8)"}  HTTP 400
{"error":"Fila 2: fecha no válida \"15/01/2026\" (se espera AAAA-MM-DD)"}  HTTP 400
{"totalEnBd":23,...}
```

El total no cambió: ninguna carga rechazada guardó nada (ni siquiera la
fila 701, que era válida).

---

## 13. Las cabeceras de seguridad también salen en un 401

**Demuestra:** S-01, OWASP A02.

```bash
curl -s -D - -o /dev/null $BASE/api/alertas | grep -iE '^HTTP|content-security|x-frame|x-content-type'
```

**Resultado esperado:**

```
HTTP/1.1 401
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
Content-Security-Policy: default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; connect-src 'self'; object-src 'none'; base-uri 'none'; form-action 'self'; frame-ancestors 'none'
```

---

## 14. Actuator: health público, métricas con autenticación

**Demuestra:** S-13, RNF-4 (parte que cumple), observabilidad.

```bash
curl -s -w "  HTTP %{http_code}\n" $BASE/actuator/health
curl -s -o /dev/null -w "prometheus sin credenciales: %{http_code}\n" $BASE/actuator/prometheus
curl -s -o /dev/null -w "info sin credenciales: %{http_code}\n" $BASE/actuator/info
curl -s -u "$AUTH" $BASE/actuator/prometheus | grep -m1 '^http_server_requests_seconds_count'
```

**Resultado esperado:** `{"status":"UP","groups":["liveness","readiness"]}  HTTP 200`,
`prometheus sin credenciales: 401`, `info sin credenciales: 401` y una línea
`http_server_requests_seconds_count{...} <número>`.

---

## 15. k6: 200 usuarios concurrentes

**Demuestra:** RNF-3 (escalabilidad): menos del 1 % de errores y p95 de
`/api/alertas` ≤ 1.500 ms, con el protocolo del curso (tres corridas, se
descarta la primera).

```bash
k6 run -e CONTRATOS=1200 rnf/k6-cargar-datos.js          # datos con semilla fija
for n in 1 2 3; do                                       # modo sesión, como el navegador (≈1 min cada una)
  k6 run -e AUTH_MODO=sesion --summary-export /tmp/k6-sesion-$n.json rnf/k6-concurrencia.js > /tmp/k6-sesion-$n.txt
done
for n in 2 3; do jq -c "{corrida: $n, p95_ms: .metrics[\"http_req_duration{endpoint:alertas}\"][\"p(95)\"], errores: .metrics.http_req_failed.value}" /tmp/k6-sesion-$n.json; done
```

**Dónde ver el resultado:** al final de cada corrida, en el bloque
**THRESHOLDS** de `/tmp/k6-sesion-<n>.txt`:
`http_req_duration{endpoint:alertas}` (p95) y `http_req_failed` (% de
errores). El `jq` resume las corridas 2 y 3.

**Resultado esperado:**

- Carga de datos: `nuevos=1200 totalEnBd=1223` (los 1.200 de k6 más los 23
  de las demostraciones anteriores).
- **Corridas 2 y 3: cumplen.** Los tres umbrales con ✓, `errores: 0` y p95
  por debajo de 1.500 ms (685 y 917 ms en esta verificación; 567 y
  761 ms en la medición registrada en `docs/seguridad/`).

**Variante informativa, Basic en cada petición** (no cumple, deuda
[DT-17](deuda-tecnica.md)): `k6 run rnf/k6-concurrencia.js` termina con
`thresholds on metrics 'http_req_duration{endpoint:alertas}' have been
crossed` (p95 de 2,32 s en esta verificación) y 0 % de errores. La causa
es BCrypt en cada petición.

---

## 16. EXPLAIN ANALYZE de las consultas principales

**Demuestra:** tiempos de SQL medidos, con el índice de la clave natural en uso.

```bash
k6 run -e CONTRATOS=5000 rnf/k6-cargar-datos.js
docker exec -i dac_db psql -U dac_user -d dac < rnf/explain-analyze.sql | grep -E '^ +[0-9]+$|===|Index Scan|Execution Time'
```

**Resultado esperado:** unos 5.000 contratos en la base (**5023**);
`findAllConFetch` con **Execution Time** de pocos milisegundos
(**3,47 · 3,11 · 2,62 ms** en esta verificación); la búsqueda por clave natural con **Index Scan using
uq_contrato_clave_natural** y **Execution Time** de centésimas de
milisegundo (**0,011 · 0,009 · 0,010 ms**). Medición registrada:
`docs/seguridad/explain-analyze.txt`.

---

## 17. Sin credencial por defecto y puertos solo en 127.0.0.1

**Demuestra:** S-12, S-14, OWASP A02/A07.

```bash
DAC_AUTH_PASSWORD= SERVER_PORT=8091 ./mvnw spring-boot:run 2>&1 | grep -m1 -o "Falta la credencial del analista[^.]*"
docker port dac_db
docker port dac_adminer
```

**Resultado esperado:**
`Falta la credencial del analista: define DAC_AUTH_USER y DAC_AUTH_PASSWORD en el archivo `
(la aplicación no arranca sin clave; con
`./mvnw spring-boot:run -Dspring-boot.run.profiles=dev` sí arranca, con la
credencial demo), `5432/tcp -> 127.0.0.1:5433` y
`8080/tcp -> 127.0.0.1:8081`.

---

## 18. Observabilidad: Prometheus raspa con autenticación (opcional)

**Demuestra:** observabilidad; S-13 (Prometheus necesita la credencial).

```bash
docker compose --env-file .env -f observabilidad/docker-compose.observabilidad.yml up -d
sleep 15
curl -s localhost:9090/api/v1/targets | jq -r '.data.activeTargets[] | "\(.scrapeUrl) \(.health)"'
curl -s -o /dev/null -w "grafana admin/admin: %{http_code}\n" -u admin:admin localhost:3000/api/org
curl -s -o /dev/null -w "grafana con la clave de .env: %{http_code}\n" -u "admin:${GRAFANA_ADMIN_PASSWORD}" localhost:3000/api/org
docker compose --env-file .env -f observabilidad/docker-compose.observabilidad.yml down
```

Si DAC no corre en el 8080, agrega antes `export DAC_TARGET=host.docker.internal:<puerto>`.

**Resultado esperado:** `http://host.docker.internal:8080/actuator/prometheus up`,
`grafana admin/admin: 401` y `grafana con la clave de .env: 200`. En el
navegador: http://localhost:9090 → *Status → Targets*, y Grafana en
http://localhost:3000.

---

## 19. Pruebas automáticas

**Demuestra:** RNF-5 (mantenibilidad), RNF-1 y RNF-2 medidos con el
protocolo del curso, y todos los ataques simulados.

> ⚠️ Las pruebas de punta a punta **vacían las tablas** (`TRUNCATE`). Déjalo
> para el final de la parte con datos.

```bash
./mvnw verify
cat java/target/rnf/resultados.md
```

**Resultado esperado:** `Tests run: 93, Failures: 0, Errors: 0, Skipped: 0`
y `BUILD SUCCESS`. `resultados.md` trae la fecha, el commit, el equipo y las
corridas 1 (descartada), 2 y 3 de RNF-1 y RNF-2. Al terminar, la base
conserva los contratos de la última prueba: para volver a demostrar,
ejecuta el `TRUNCATE` de la demostración 0.

---

## 20. Cadena de trazabilidad completa de HU-02

**Demuestra:** que una historia se puede seguir de punta a punta: backlog →
código → prueba → commit → demostración.

```bash
grep -n "HU-02 Reincidencia" docs/backlog.md                                   # 1. backlog: fila de trazabilidad
grep -n "public List<Alerta> detectar" java/src/main/java/dac/dominio/DetectorDeReincidencias.java   # 2. código
./mvnw -q -pl java test -Dtest=DetectorDeReincidenciasTest 2>&1 | grep "Tests run" | tail -1        # 3. prueba
git show --stat --format='%h %ad %s' --date=short b027f43 | grep -E "^b027f43|DetectorDeReincidencias"  # 4. commit
```

5. Demostración: la 3 de este guion (la alerta de ACME SAS + Juan Pérez con
   sus 3 contratos).

**Resultado esperado:**

```
232:| HU-02 Reincidencia | `dominio/DetectorDeReincidencias`, …
20:    public List<Alerta> detectar(List<Contrato> contratos) {
Tests run: 8, Failures: 0, Errors: 0, Skipped: 0
b027f43 2026-09-09 feat: migrar stack backend a Spring Boot + PostgreSQL (HU-07)
 .../java/dac/dominio/DetectorDeReincidencias.java  |  31 ++++
```

---

## 21. La integración continua en verde

**Demuestra:** que cada Pull Request corre la suite completa contra
PostgreSQL real (`.github/workflows/ci.yml`, con `./mvnw -B verify`).

- **En el navegador:** https://github.com/juanpabloow/proyecto_g/actions →
  flujo **CI** → la ejecución más reciente de la rama o del Pull Request →
  marca verde. En **Summary** está la medición de RNF-1 y RNF-2 del runner;
  en **Artifacts**, `reporte-pruebas`.
- **En el Pull Request:** pestaña **Checks**, o al pie de la conversación
  ("All checks have passed").
- **Por terminal** (con la CLI de GitHub):

  ```bash
  gh pr checks 10
  ```

**Resultado esperado:** `pruebas	pass	…` con el enlace a la ejecución.

---

## Guion del video (3 minutos)

Un tramo cada 30 segundos. Antes de grabar: demostración 0 hecha, servidor
corriendo, navegador en la pantalla de login y una terminal con la
preparación de "Antes de empezar".

| Tiempo | Qué se muestra | Qué se dice |
|---|---|---|
| 0:00–0:30 | Pantalla de login → entrar con la credencial de `.env` | "DAC detecta señales de posible soborno en contratos públicos. Cada alerta trae los contratos que la originaron: no acusa, da evidencia para verificar. Sin sesión no se ve nada." |
| 0:30–1:00 | Cargar `data/contratos_ejemplo.csv` → pestaña **Reincidencia** | "Cinco contratos. ACME SAS aparece con el mismo funcionario en tres contratos, en dos entidades: una alerta, con número, entidad, fecha y monto de cada contrato." |
| 1:00–1:30 | Pestaña **Fraccionamiento**; luego cargar `data/contratos_nombres_variantes_ejemplo.csv` | "Segunda señal: dos contratos de ACME en la Gobernación que suman el tope en menos de 30 días. Y nombres escritos distinto, con tildes o puntos, se reconocen como el mismo contratista." |
| 1:30–2:00 | **Problema duro:** volver a cargar `data/contratos_ejemplo.csv` → "Listo: 0 contrato(s) nuevo(s)." | "El problema duro es la idempotencia. Un contrato contado dos veces parece una reincidencia: el sistema inventaría la señal. Cargo el mismo archivo otra vez: cero nuevos." |
| 2:00–2:30 | Terminal: la línea `AUDITORIA … nuevos=0 yaGuardados=5` y `select count(*) from contrato` → 5 | "Queda auditado: cero nuevos, cinco ya guardados, sin nombres de personas. Y no depende del código: la base lo impide con una restricción única sobre número y entidad." |
| 2:30–3:00 | `./mvnw verify` ya ejecutado (BUILD SUCCESS) y la pestaña **Checks** del PR en verde | "La suite de punta a punta contra PostgreSQL real y las mediciones de rendimiento corren en cada Pull Request. Lo que falta está priorizado en la deuda técnica." |

---

## Registro de la verificación (11 de noviembre de 2026)

Ejecutado en orden, desde `docker compose down -v`, con
`./mvnw spring-boot:run` en **http://localhost:8080**, en un Apple M4 Pro
con macOS y zsh. Los pasos de la interfaz se hicieron en Chrome, conducido
por un script.

| # | Demostración | Resultado real |
|---|---|---|
| 0 | Arranque desde cero | ✅ `TRUNCATE TABLE`; health `UP` |
| 1 | Login | ✅ 401 / 401 / 200 / 302 a `/login.html`; clave mala → "Usuario o contraseña incorrectos." |
| 2–3 | HU-01, HU-02 | ✅ 5 nuevos; ACME SAS + Juan Pérez con 001, 003 y 005 |
| 4 | Problema duro | ✅ 0 nuevos, 5 en la base, y las dos líneas de auditoría idénticas a las de arriba |
| 5–8 | HU-03 a HU-06 | ✅ 2 fraccionamientos; filas 5, 6 y 7 descartadas; 301, 302 y 303 unidos; 2 conflictos y el 001 en 50000000.00 |
| 9 | HU-07 | ✅ tras reiniciar con `./mvnw spring-boot:run`: 19 contratos, 4 reincidencias, 3 fraccionamientos |
| 10 | HU-08 y salir | ✅ `efbbbf`, 22 líneas, descarga idéntica; "Cerraste sesión." y luego 401 |
| 11 | SQLi y XSS | ✅ texto, 0 ventanas de alerta, 23 contratos, nombres guardados tal cual |
| 12 | Validaciones | ✅ 400 / 413 / 400 "Fila 3" / 400 "Fila 2"; total sin cambios (23) |
| 13–14 | Cabeceras y Actuator | ✅ cabeceras en el 401; health 200, prometheus e info 401 |
| 15 | k6 | ✅ corridas 2 y 3: p95 685 y 917 ms, 0 % de errores; Basic: 2,32 s (no cumple, como se declara) |
| 16–18 | EXPLAIN, credencial y puertos, observabilidad | Verificadas más temprano el mismo día con estos comandos (en esa corrida, con `java/ejecutar.sh` y el puerto 8090); no se repitieron en esta corrida |
| 19 | Pruebas | ✅ `Tests run: 93, Failures: 0, Errors: 0, Skipped: 0`, BUILD SUCCESS |
| 20 | Trazabilidad de HU-02 | ✅ (salida de arriba) |
| 21 | CI | ✅ `gh pr checks 10` → `pruebas pass` |
