// RNF-3 — Escalabilidad / concurrencia (Módulo 7).
//
// Simula 200 usuarios concurrentes leyendo el estado del sistema, que es la
// operación real que varios analistas harían a la vez. Es de SOLO LECTURA
// (GET): no modifica la base, así que se puede repetir sin ensuciar datos.
//
//   GET /api/alertas  -> recalcula las dos señales sobre todo el histórico
//   GET /login.html   -> página estática pública (línea base del servidor web)
//
// Desde el inicio de sesión (S-10) la API exige credenciales, tomadas de las
// variables de entorno DAC_AUTH_USER y DAC_AUTH_PASSWORD (las del .env).
// Dos modos (variable AUTH_MODO):
//   basic  (por defecto) -> cada petición lleva HTTP Basic, como un cliente
//                           programático. Spring Security verifica el hash
//                           BCrypt en CADA petición.
//   sesion               -> cada usuario virtual inicia sesión una vez por el
//                           formulario (POST /login) y luego usa la cookie de
//                           sesión, como el analista en el navegador.
//
// Cómo reproducir la medición (servidor DAC en http://localhost:8080):
//   1. Base limpia (ver README → "Reiniciar a estado limpio").
//   2. set -a; source .env; set +a
//   3. k6 run -e CONTRATOS=1200 rnf/k6-cargar-datos.js      # datos con semilla fija
//   4. Protocolo del curso: tres corridas, se descarta la primera:
//      for n in 1 2 3; do
//        k6 run -e AUTH_MODO=sesion --summary-export docs/seguridad/k6-sesion-corrida-$n.json rnf/k6-concurrencia.js
//      done
//   (Modo basic, informativo: k6 run --summary-export docs/seguridad/k6-basic-informativa.json rnf/k6-concurrencia.js)
//
// Objetivo (umbral que hace fallar la prueba si no se cumple):
//   - menos del 1 % de peticiones con error
//   - p95 del endpoint dinámico /api/alertas por debajo de 1500 ms
//
// Requiere k6 (https://k6.io):  brew install k6

import http from "k6/http";
import encoding from "k6/encoding";
import { check } from "k6";

const BASE = __ENV.BASE_URL || "http://localhost:8080";
const USUARIO = __ENV.DAC_AUTH_USER;
const CLAVE = __ENV.DAC_AUTH_PASSWORD;
const MODO = __ENV.AUTH_MODO || "basic";

if (!USUARIO || !CLAVE) {
  throw new Error("Faltan DAC_AUTH_USER y DAC_AUTH_PASSWORD (cárgalas con: set -a; source .env; set +a)");
}

if (MODO !== "basic" && MODO !== "sesion") {
  throw new Error(`AUTH_MODO debe ser "basic" o "sesion", no "${MODO}"`);
}

const AUTENTICADO = MODO === "basic"
  ? { headers: { Authorization: `Basic ${encoding.b64encode(`${USUARIO}:${CLAVE}`)}` }, tags: { endpoint: "alertas" } }
  : { tags: { endpoint: "alertas" } };

export const options = {
  // k6 borra las cookies al empezar cada iteración; en modo sesión hay que
  // conservarlas para que el usuario virtual siga con la sesión iniciada.
  noCookiesReset: true,
  scenarios: {
    doscientos_usuarios: {
      executor: "ramping-vus",
      startVUs: 0,
      stages: [
        { duration: "15s", target: 200 }, // sube hasta 200 usuarios
        { duration: "30s", target: 200 }, // sostiene 200 usuarios
        { duration: "10s", target: 0 },   // baja
      ],
      gracefulStop: "10s",
    },
  },
  thresholds: {
    http_req_failed: ["rate<0.01"],
    "http_req_duration{endpoint:alertas}": ["p(95)<1500"],
    "http_req_duration{endpoint:pagina}": ["p(95)<500"],
  },
};

export default function () {
  // Modo sesión: el primer paso de cada usuario virtual es iniciar sesión;
  // k6 guarda la cookie JSESSIONID para las peticiones siguientes.
  if (MODO === "sesion" && __ITER === 0) {
    const login = http.post(`${BASE}/login`, { username: USUARIO, password: CLAVE },
      { tags: { endpoint: "login" } });
    check(login, { "login -> 200": (r) => r.status === 200 && !r.url.includes("error") });
  }

  // Lectura del estado: el camino caro (recalcula reincidencia y fraccionamiento).
  const alertas = http.get(`${BASE}/api/alertas`, AUTENTICADO);
  check(alertas, {
    "alertas -> 200": (r) => r.status === 200,
    "alertas -> JSON": (r) => (r.headers["Content-Type"] || "").includes("application/json"),
    "alertas -> no-store": (r) => (r.headers["Cache-Control"] || "").includes("no-store"),
  });

  // Línea base: servir una página estática pública bajo la misma carga.
  const pagina = http.get(`${BASE}/login.html`, { tags: { endpoint: "pagina" } });
  check(pagina, { "pagina -> 200": (r) => r.status === 200 });
}
