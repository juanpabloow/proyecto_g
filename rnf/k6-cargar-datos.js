// Datos de prueba para RNF-3 (k6) y para el EXPLAIN ANALYZE (Módulo 7).
//
// Genera contratos FICTICIOS con una semilla fija y los sube por la API real
// (POST /api/contratos/cargar), autenticado con HTTP Basic. La misma semilla
// produce siempre el mismo archivo, en cualquier computador, así que la
// medición se puede repetir con los mismos datos.
//
// Los contratos se numeran K6-00001, K6-00002…; con la misma semilla, los
// primeros N de una carga de 5.000 son exactamente los de una carga de N.
// Por eso pasar de 1.200 a 5.000 solo agrega los que faltan (idempotencia).
//
// Uso (con el servidor DAC corriendo y la base limpia):
//   set -a; source .env; set +a          # DAC_AUTH_USER y DAC_AUTH_PASSWORD
//   k6 run -e CONTRATOS=1200 rnf/k6-cargar-datos.js
//
// Variables: CONTRATOS (por defecto 1200), SEMILLA (por defecto 2026),
// BASE_URL (por defecto http://localhost:8080).

import http from "k6/http";
import encoding from "k6/encoding";

const BASE = __ENV.BASE_URL || "http://localhost:8080";
const USUARIO = __ENV.DAC_AUTH_USER;
const CLAVE = __ENV.DAC_AUTH_PASSWORD;
const CONTRATOS = parseInt(__ENV.CONTRATOS || "1200", 10);
const SEMILLA = parseInt(__ENV.SEMILLA || "2026", 10);

if (!USUARIO || !CLAVE) {
  throw new Error("Faltan DAC_AUTH_USER y DAC_AUTH_PASSWORD (cárgalas con: set -a; source .env; set +a)");
}

export const options = { vus: 1, iterations: 1 };

// Generador pseudoaleatorio con semilla (mulberry32): mismo resultado en
// cualquier máquina, a diferencia de Math.random().
function generador(semilla) {
  let a = semilla >>> 0;
  return function () {
    a = (a + 0x6d2b79f5) >>> 0;
    let t = a;
    t = Math.imul(t ^ (t >>> 15), t | 1);
    t ^= t + Math.imul(t ^ (t >>> 7), t | 61);
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}

function entero(azar, n) {
  return Math.floor(azar() * n);
}

function dosDigitos(n) {
  return String(n).padStart(2, "0");
}

export function generarCsv(cantidad, semilla) {
  const azar = generador(semilla);
  const inicio = Date.UTC(2025, 0, 1);
  const filas = ["numero_contrato,entidad,contratista,funcionario,monto,fecha"];
  for (let i = 1; i <= cantidad; i++) {
    // Cada contratista trabaja casi siempre con los mismos 3 funcionarios y
    // en 2 entidades: así aparecen reincidencias y fraccionamientos, como en
    // los datos reales que motivan las dos señales.
    const contratista = entero(azar, 600);
    const funcionario = (contratista * 13 + entero(azar, 3)) % 250;
    const entidad = (contratista + entero(azar, 2)) % 20;
    const monto = (1 + entero(azar, 150)) * 1000000;
    const fecha = new Date(inicio + entero(azar, 730) * 86400000);
    const textoFecha = `${fecha.getUTCFullYear()}-${dosDigitos(fecha.getUTCMonth() + 1)}-${dosDigitos(fecha.getUTCDate())}`;
    filas.push([
      `K6-${String(i).padStart(5, "0")}`,
      `Entidad Ficticia ${dosDigitos(entidad + 1)}`,
      `Contratista Ficticio ${String(contratista + 1).padStart(3, "0")} SAS`,
      `Funcionario Ficticio ${String(funcionario + 1).padStart(3, "0")}`,
      monto,
      textoFecha,
    ].join(","));
  }
  return filas.join("\n") + "\n";
}

export default function () {
  const csv = generarCsv(CONTRATOS, SEMILLA);
  const cabeceras = { Authorization: `Basic ${encoding.b64encode(`${USUARIO}:${CLAVE}`)}` };
  const r = http.post(`${BASE}/api/contratos/cargar`,
    { archivo: http.file(csv, "k6-contratos.csv", "text/csv") },
    { headers: cabeceras, timeout: "120s" });
  if (r.status !== 200) {
    throw new Error(`La carga respondió ${r.status}: ${r.body}`);
  }
  const cuerpo = r.json();
  console.log(`Carga de ${CONTRATOS} contratos (semilla ${SEMILLA}): `
    + `nuevos=${cuerpo.contratosNuevos} totalEnBd=${cuerpo.totalEnBd} `
    + `reincidencias=${cuerpo.alertas.length} fraccionamientos=${cuerpo.alertasFraccionamiento.length}`);
}
