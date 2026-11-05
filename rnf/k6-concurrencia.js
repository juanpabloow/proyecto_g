// RNF-3 — Escalabilidad / concurrencia (Módulo 7).
//
// Simula 200 usuarios concurrentes leyendo el estado del sistema, que es la
// operación real que varios analistas harían a la vez. Es de SOLO LECTURA
// (GET): no modifica la base, así que se puede repetir sin ensuciar datos.
//
//   GET /api/alertas  -> recalcula las dos señales sobre todo el histórico
//   GET /             -> la página estática (línea base del servidor web)
//
// Cómo correrlo (con el servidor DAC en http://localhost:8080):
//   k6 run rnf/k6-concurrencia.js
//   k6 run -e BASE_URL=http://localhost:8080 rnf/k6-concurrencia.js
//
// Objetivo (umbral que hace fallar la prueba si no se cumple):
//   - menos del 1 % de peticiones con error
//   - p95 del endpoint dinámico /api/alertas por debajo de 1500 ms
//
// Requiere k6 (https://k6.io):  brew install k6

import http from "k6/http";
import { check } from "k6";

const BASE = __ENV.BASE_URL || "http://localhost:8080";

export const options = {
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
  // Lectura del estado: el camino caro (recalcula reincidencia y fraccionamiento).
  const alertas = http.get(`${BASE}/api/alertas`, { tags: { endpoint: "alertas" } });
  check(alertas, {
    "alertas -> 200": (r) => r.status === 200,
    "alertas -> JSON": (r) => (r.headers["Content-Type"] || "").includes("application/json"),
    "alertas -> no-store": (r) => (r.headers["Cache-Control"] || "").includes("no-store"),
  });

  // Línea base: servir la página estática bajo la misma carga.
  const pagina = http.get(`${BASE}/`, { tags: { endpoint: "pagina" } });
  check(pagina, { "pagina -> 200": (r) => r.status === 200 });
}
