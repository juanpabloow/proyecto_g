// Interfaz web de DAC. Sin librerías: habla con la misma API REST que usan
// las pruebas (POST /api/contratos/cargar, GET /api/alertas).
// Seguridad: todo dato que viene del CSV se inserta con textContent, nunca
// con innerHTML, para que un nombre como <script> no se ejecute.
(function () {
  "use strict";

  const $ = (id) => document.getElementById(id);
  const pesos = new Intl.NumberFormat("es-CO", { style: "currency", currency: "COP", maximumFractionDigits: 0 });
  const fechaLarga = new Intl.DateTimeFormat("es-CO", { day: "2-digit", month: "2-digit", year: "numeric", timeZone: "UTC" });

  const entrada = $("archivo");
  const zona = $("zona");
  const botonCargar = $("btn-cargar");
  const estado = $("estado");

  // ---------- Utilidades de DOM ----------
  function el(etiqueta, opciones, hijos) {
    const nodo = document.createElement(etiqueta);
    if (opciones) {
      if (opciones.clase) nodo.className = opciones.clase;
      if (opciones.texto !== undefined) nodo.textContent = opciones.texto;
    }
    (hijos || []).forEach((h) => nodo.appendChild(h));
    return nodo;
  }

  function monto(valor) {
    if (valor === null || valor === undefined || valor === "") return "—";
    const n = Number(valor);
    return Number.isFinite(n) ? pesos.format(n) : String(valor);
  }

  function fecha(valor) {
    if (!valor) return "—";
    const d = new Date(valor + "T00:00:00Z");
    return isNaN(d) ? String(valor) : fechaLarga.format(d);
  }

  function vacio(texto) {
    return el("p", { clase: "vacio", texto });
  }

  function mostrarEstado(texto, tipo) {
    estado.textContent = texto;
    estado.className = "estado" + (tipo ? " estado--" + tipo : "");
  }

  // ---------- Tablas de evidencia ----------
  function tablaEvidencia(evidencia, conPersonas) {
    const columnas = ["N.º contrato", "Entidad"];
    if (conPersonas) columnas.push("Funcionario");
    columnas.push("Fecha", "Monto");

    const cabeza = el("tr", null, columnas.map((c) =>
      el("th", { texto: c, clase: c === "Monto" ? "num" : "" })));

    const filas = evidencia.map((c) => {
      const celdas = [el("td", { texto: c.numeroContrato }), el("td", { texto: c.entidad })];
      if (conPersonas) celdas.push(el("td", { texto: c.funcionario }));
      celdas.push(el("td", { texto: fecha(c.fecha), clase: "fecha" }), el("td", { texto: monto(c.monto), clase: "num" }));
      return el("tr", null, celdas);
    });

    return el("div", { clase: "tabla-envoltura" }, [
      el("table", null, [el("thead", null, [cabeza]), el("tbody", null, filas)])
    ]);
  }

  function entidadesDistintas(evidencia) {
    return new Set(evidencia.map((c) => c.entidad)).size;
  }

  // ---------- Pintar cada sección ----------
  function pintarReincidencias(alertas) {
    const lista = $("lista-reincidencia");
    lista.replaceChildren();
    if (!alertas.length) { lista.appendChild(vacio("No hay pares contratista–funcionario repetidos.")); return; }

    alertas.forEach((a) => {
      const total = a.evidencia.reduce((s, c) => s + (Number(c.monto) || 0), 0);
      const nEnt = entidadesDistintas(a.evidencia);
      lista.appendChild(el("article", { clase: "alerta" }, [
        el("div", { clase: "alerta__cabeza" }, [
          el("h3", { texto: a.contratista + "  +  " + a.funcionario }),
          el("span", { clase: "insignia", texto: "REINCIDENCIA" })
        ]),
        el("p", { clase: "alerta__meta", texto:
          a.evidencia.length + " contratos en " + nEnt + (nEnt === 1 ? " entidad" : " entidades") +
          " · suma " + pesos.format(total) }),
        tablaEvidencia(a.evidencia, false)
      ]));
    });
  }

  function pintarFraccionamientos(alertas) {
    const lista = $("lista-fraccionamiento");
    lista.replaceChildren();
    if (!alertas.length) { lista.appendChild(vacio("No se encontraron contratos fraccionados.")); return; }

    alertas.forEach((a) => {
      lista.appendChild(el("article", { clase: "alerta" }, [
        el("div", { clase: "alerta__cabeza" }, [
          el("h3", { texto: a.contratista + "  en  " + a.entidad }),
          el("span", { clase: "insignia insignia--fracc", texto: "FRACCIONAMIENTO" })
        ]),
        el("p", { clase: "alerta__meta", texto:
          a.evidencia.length + " contratos en menos de " + a.ventanaDias + " días suman " +
          monto(a.montoTotal) + " (tope: " + monto(a.tope) + ")" }),
        tablaEvidencia(a.evidencia, true)
      ]));
    });
  }

  function pintarDescartes(descartes) {
    const lista = $("lista-descartes");
    lista.replaceChildren();
    if (!descartes) { lista.appendChild(vacio("Carga un archivo para ver sus filas descartadas.")); return; }
    if (!descartes.length) { lista.appendChild(vacio("Ninguna fila se descartó en la última carga.")); return; }
    descartes.forEach((d) => {
      const p = el("p", { clase: "fila-simple" });
      p.appendChild(el("strong", { texto: "Fila " + d.fila + ": " }));
      p.appendChild(document.createTextNode(d.motivo));
      lista.appendChild(p);
    });
  }

  function pintarConflictos(conflictos) {
    const lista = $("lista-conflictos");
    lista.replaceChildren();
    if (!conflictos) { lista.appendChild(vacio("Carga un archivo para ver sus conflictos.")); return; }
    if (!conflictos.length) { lista.appendChild(vacio("Ningún duplicado traía datos distintos.")); return; }
    conflictos.forEach((c) => {
      const p = el("p", { clase: "fila-simple" });
      p.appendChild(el("strong", { texto: "Contrato " + c.numeroContrato + " (" + c.entidad + "), fila " + c.fila + ": " }));
      p.appendChild(document.createTextNode("choca con " + c.conflictoCon + " en "));
      c.camposDistintos.forEach((campo) => p.appendChild(el("span", { clase: "campo", texto: campo })));
      lista.appendChild(p);
    });
  }

  function pintarResumen(datos) {
    const alertas = datos.alertas || [];
    const fracc = datos.alertasFraccionamiento || [];
    $("m-total").textContent = datos.totalEnBd;
    $("m-nuevos").textContent = datos.contratosNuevos === undefined ? "—" : datos.contratosNuevos;
    $("m-alertas").textContent = alertas.length + fracc.length;
    const revisar = datos.filasDescartadas ? datos.filasDescartadas.length + datos.conflictos.length : null;
    $("m-revisar").textContent = revisar === null ? "—" : revisar;

    $("c-reincidencia").textContent = alertas.length;
    $("c-fraccionamiento").textContent = fracc.length;
    $("c-descartes").textContent = datos.filasDescartadas ? datos.filasDescartadas.length : 0;
    $("c-conflictos").textContent = datos.conflictos ? datos.conflictos.length : 0;

    pintarReincidencias(alertas);
    pintarFraccionamientos(fracc);
    pintarDescartes(datos.filasDescartadas);
    pintarConflictos(datos.conflictos);
  }

  // ---------- Llamadas a la API ----------
  async function consultar() {
    try {
      const r = await fetch("api/alertas");
      if (!r.ok) throw new Error("HTTP " + r.status);
      pintarResumen(await r.json());
    } catch (e) {
      mostrarEstado("No se pudo consultar el estado actual. ¿Está corriendo el servidor y la base de datos?", "error");
    }
  }

  async function cargar(evento) {
    evento.preventDefault();
    const archivo = entrada.files[0];
    if (!archivo) return;

    const datos = new FormData();
    datos.append("archivo", archivo);
    botonCargar.disabled = true;
    mostrarEstado("Analizando " + archivo.name + "…");

    try {
      const r = await fetch("api/contratos/cargar", { method: "POST", body: datos });
      const cuerpo = await r.json().catch(() => ({}));
      if (!r.ok) {
        mostrarEstado("No se cargó el archivo: " + (cuerpo.error || "error " + r.status) + ". No se guardó nada.", "error");
        return;
      }
      pintarResumen(cuerpo);
      const avisos = [];
      if (cuerpo.duplicadosIgnorados) avisos.push(cuerpo.duplicadosIgnorados + " duplicado(s) ignorado(s)");
      if (cuerpo.filasDescartadas.length) avisos.push(cuerpo.filasDescartadas.length + " fila(s) descartada(s)");
      if (cuerpo.conflictos.length) avisos.push(cuerpo.conflictos.length + " conflicto(s) para revisar");
      mostrarEstado("Listo: " + cuerpo.contratosNuevos + " contrato(s) nuevo(s)" +
        (avisos.length ? " · " + avisos.join(" · ") : "") + ".", "ok");
    } catch (e) {
      mostrarEstado("No hubo respuesta del servidor. Revisa que esté corriendo.", "error");
    } finally {
      botonCargar.disabled = !entrada.files.length;
    }
  }

  // ---------- Selección de archivo y arrastrar/soltar ----------
  function archivoElegido() {
    const archivo = entrada.files[0];
    botonCargar.disabled = !archivo;
    zona.classList.toggle("zona--lista", Boolean(archivo));
    $("zona-texto").textContent = archivo ? archivo.name + " (" + Math.ceil(archivo.size / 1024) + " KB)"
                                          : "Arrastra el archivo aquí o haz clic para elegirlo";
  }

  entrada.addEventListener("change", archivoElegido);
  ["dragenter", "dragover"].forEach((t) => zona.addEventListener(t, (e) => {
    e.preventDefault(); zona.classList.add("zona--activa");
  }));
  ["dragleave", "drop"].forEach((t) => zona.addEventListener(t, (e) => {
    e.preventDefault(); zona.classList.remove("zona--activa");
  }));
  zona.addEventListener("drop", (e) => {
    if (e.dataTransfer.files.length) { entrada.files = e.dataTransfer.files; archivoElegido(); }
  });
  $("form-carga").addEventListener("submit", cargar);

  // ---------- Pestañas accesibles (flechas izquierda/derecha) ----------
  const pestanas = Array.from(document.querySelectorAll('[role="tab"]'));
  function activar(pestana) {
    pestanas.forEach((p) => {
      const activa = p === pestana;
      p.setAttribute("aria-selected", activa);
      p.tabIndex = activa ? 0 : -1;
      $(p.getAttribute("aria-controls")).hidden = !activa;
    });
  }
  pestanas.forEach((p, i) => {
    p.addEventListener("click", () => activar(p));
    p.addEventListener("keydown", (e) => {
      const paso = e.key === "ArrowRight" ? 1 : e.key === "ArrowLeft" ? -1 : 0;
      if (!paso) return;
      const siguiente = pestanas[(i + paso + pestanas.length) % pestanas.length];
      activar(siguiente); siguiente.focus();
    });
  });

  consultar();
})();
