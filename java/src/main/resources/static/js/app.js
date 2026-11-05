// Interfaz web de DAC. Sin librerías: habla con la misma API REST que usan
// las pruebas (POST /api/contratos/cargar, GET /api/alertas).
// Seguridad: todo dato que viene del CSV se inserta con textContent, nunca
// como HTML, para que un nombre como <script> no se ejecute (S-04). No hay
// estilos ni scripts en línea: todo respeta la CSP de CabecerasDeSeguridad.
(function () {
  "use strict";

  const $ = (id) => document.getElementById(id);
  const pesos = new Intl.NumberFormat("es-CO", { style: "currency", currency: "COP", maximumFractionDigits: 0 });
  const fechaLarga = new Intl.DateTimeFormat("es-CO", { day: "2-digit", month: "2-digit", year: "numeric", timeZone: "UTC" });
  const horaCorta = new Intl.DateTimeFormat("es-CO", { hour: "2-digit", minute: "2-digit", second: "2-digit" });
  const colador = new Intl.Collator("es", { sensitivity: "base", numeric: true });

  const MAX_BYTES = 10 * 1024 * 1024; // el servidor rechaza con 413 por encima de 10 MB

  const entrada = $("archivo");
  const zona = $("zona");
  const botonCargar = $("btn-cargar");
  const estado = $("estado");
  const buscador = $("buscador");

  // Estado de la pantalla: lo último que respondió la API + el filtro actual.
  let datos = {
    totalEnBd: 0, contratosNuevos: undefined, duplicadosIgnorados: undefined,
    alertas: [], fracc: [], descartes: null, conflictos: null,
  };
  let filtro = "";            // texto de búsqueda, ya normalizado
  const historial = [];       // cargas de esta sesión, la más reciente primero (HU-07)

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

  // Normaliza solo para comparar/buscar (minúsculas y sin tildes). No cambia
  // lo que se muestra: en pantalla siempre va el texto original del archivo.
  function plano(valor) {
    return (valor == null ? "" : String(valor)).toLowerCase().normalize("NFD").replace(/[̀-ͯ]/g, "");
  }

  // ---------- Tablas de evidencia ----------
  // HU-05: el contratista y el funcionario se muestran TAL COMO vinieron en
  // cada contrato. Si una fila los escribió distinto al nombre que encabeza la
  // alerta (el del primer contrato del par), se resalta para que el analista lo vea.
  function tablaEvidencia(evidencia, opciones) {
    const o = opciones || {};
    const columnas = ["N.º contrato", "Entidad"];
    if (o.contratista) columnas.push("Contratista");
    if (o.funcionario) columnas.push("Funcionario");
    columnas.push("Fecha", "Monto");

    const cabeza = el("tr", null, columnas.map((c) =>
      el("th", { texto: c, clase: c === "Monto" ? "num" : "" })));

    let hayVariantes = false;
    const filas = evidencia.map((c) => {
      const celdas = [el("td", { texto: c.numeroContrato }), el("td", { texto: c.entidad })];
      if (o.contratista) {
        const distinto = o.heading && o.heading.contratista != null && c.contratista !== o.heading.contratista;
        if (distinto) hayVariantes = true;
        celdas.push(el("td", { texto: c.contratista, clase: distinto ? "variante" : "" }));
      }
      if (o.funcionario) {
        const distinto = o.heading && o.heading.funcionario != null && c.funcionario !== o.heading.funcionario;
        if (distinto) hayVariantes = true;
        celdas.push(el("td", { texto: c.funcionario, clase: distinto ? "variante" : "" }));
      }
      celdas.push(el("td", { texto: fecha(c.fecha), clase: "fecha" }), el("td", { texto: monto(c.monto), clase: "num" }));
      return el("tr", null, celdas);
    });

    const partes = [el("div", { clase: "tabla-envoltura" }, [
      el("table", null, [el("thead", null, [cabeza]), el("tbody", null, filas)])
    ])];
    if (hayVariantes) {
      partes.push(el("p", { clase: "nota-variantes",
        texto: "✎ El nombre se escribió distinto al del encabezado; se muestra tal como vino en el archivo (HU-05)." }));
    }
    return el("div", null, partes);
  }

  function entidadesDistintas(evidencia) {
    return new Set(evidencia.map((c) => c.entidad)).size;
  }

  // ---------- Orden alfabético (R-4: sin puntajes ni rankings) ----------
  function ordenarReincidencias(alertas) {
    return alertas.slice().sort((a, b) =>
      colador.compare(a.contratista, b.contratista) || colador.compare(a.funcionario || "", b.funcionario || ""));
  }
  function ordenarFraccionamientos(alertas) {
    return alertas.slice().sort((a, b) =>
      colador.compare(a.contratista, b.contratista) || colador.compare(a.entidad, b.entidad));
  }

  // ---------- Filtro del buscador ----------
  function coincide(campos) {
    if (!filtro) return true;
    return campos.some((v) => plano(v).includes(filtro));
  }
  function coincideReincidencia(a) {
    return coincide([a.contratista, a.funcionario,
      ...a.evidencia.map((c) => c.entidad),
      ...a.evidencia.map((c) => c.contratista),
      ...a.evidencia.map((c) => c.funcionario)]);
  }
  function coincideFraccionamiento(a) {
    return coincide([a.contratista, a.entidad,
      ...a.evidencia.map((c) => c.contratista),
      ...a.evidencia.map((c) => c.funcionario)]);
  }

  // ---------- Pintar cada sección ----------
  function pintarReincidencias(alertas) {
    const lista = $("lista-reincidencia");
    lista.replaceChildren();
    if (!alertas.length) {
      lista.appendChild(vacio(filtro ? "Ninguna reincidencia coincide con tu búsqueda."
                                     : "No hay pares contratista–funcionario repetidos."));
      return;
    }
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
        tablaEvidencia(a.evidencia, { contratista: true, funcionario: true,
          heading: { contratista: a.contratista, funcionario: a.funcionario } })
      ]));
    });
  }

  function pintarFraccionamientos(alertas) {
    const lista = $("lista-fraccionamiento");
    lista.replaceChildren();
    if (!alertas.length) {
      lista.appendChild(vacio(filtro ? "Ningún fraccionamiento coincide con tu búsqueda."
                                     : "No se encontraron contratos fraccionados."));
      return;
    }
    alertas.forEach((a) => {
      lista.appendChild(el("article", { clase: "alerta" }, [
        el("div", { clase: "alerta__cabeza" }, [
          el("h3", { texto: a.contratista + "  en  " + a.entidad }),
          el("span", { clase: "insignia insignia--fracc", texto: "FRACCIONAMIENTO" })
        ]),
        el("p", { clase: "alerta__meta", texto:
          a.evidencia.length + " contratos en menos de " + a.ventanaDias + " días suman " +
          monto(a.montoTotal) + " (tope: " + monto(a.tope) + ")" }),
        tablaEvidencia(a.evidencia, { contratista: true, funcionario: true,
          heading: { contratista: a.contratista, funcionario: null } })
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

  // ---------- Historial de cargas de la sesión (HU-07) ----------
  function chip(texto, modificador) {
    return el("span", { clase: "chip" + (modificador ? " chip--" + modificador : ""), texto });
  }

  function pintarHistorial() {
    const lista = $("historial-lista");
    lista.replaceChildren();
    if (!historial.length) return;
    $("seccion-historial").hidden = false;
    historial.forEach((h) => {
      const chips = el("div", { clase: "historial__chips" }, [
        chip(h.nuevos + " nuevos", h.nuevos === 0 ? "cero" : "nuevos"),
        chip(h.repetidos + " repetidos", "cero"),
        chip(h.descartadas + " descartadas", h.descartadas ? "aviso" : "cero"),
        chip(h.conflictos + " conflictos", h.conflictos ? "aviso" : "cero"),
      ]);
      lista.appendChild(el("li", { clase: "historial__item" }, [
        el("span", { clase: "historial__archivo", texto: h.archivo }),
        el("span", { clase: "historial__hora", texto: horaCorta.format(h.hora) }),
        chips,
      ]));
    });
  }

  function agregarAlHistorial(nombre, cuerpo) {
    historial.unshift({
      archivo: nombre,
      hora: new Date(),
      nuevos: cuerpo.contratosNuevos || 0,
      repetidos: cuerpo.duplicadosIgnorados || 0,
      descartadas: (cuerpo.filasDescartadas || []).length,
      conflictos: (cuerpo.conflictos || []).length,
    });
    pintarHistorial();
  }

  // ---------- Render: ordena (R-4), filtra (buscador) y pinta ----------
  function render() {
    const reincidencias = ordenarReincidencias((datos.alertas || []).filter(coincideReincidencia));
    const fraccionamientos = ordenarFraccionamientos((datos.fracc || []).filter(coincideFraccionamiento));

    pintarReincidencias(reincidencias);
    pintarFraccionamientos(fraccionamientos);
    pintarDescartes(datos.descartes);
    pintarConflictos(datos.conflictos);

    $("c-reincidencia").textContent = reincidencias.length;
    $("c-fraccionamiento").textContent = fraccionamientos.length;
    $("c-descartes").textContent = datos.descartes ? datos.descartes.length : 0;
    $("c-conflictos").textContent = datos.conflictos ? datos.conflictos.length : 0;

    $("m-total").textContent = datos.totalEnBd;
    $("m-nuevos").textContent = datos.contratosNuevos === undefined ? "—" : datos.contratosNuevos;
    $("m-alertas").textContent = (datos.alertas || []).length + (datos.fracc || []).length;
    const revisar = datos.descartes ? datos.descartes.length + (datos.conflictos ? datos.conflictos.length : 0) : null;
    $("m-revisar").textContent = revisar === null ? "—" : revisar;
  }

  function guardarResumen(cuerpo) {
    datos = {
      totalEnBd: cuerpo.totalEnBd,
      contratosNuevos: cuerpo.contratosNuevos,                 // undefined en GET /api/alertas
      duplicadosIgnorados: cuerpo.duplicadosIgnorados,
      alertas: cuerpo.alertas || [],
      fracc: cuerpo.alertasFraccionamiento || [],
      descartes: cuerpo.filasDescartadas || null,              // GET no los trae
      conflictos: cuerpo.conflictos || null,
    };
    render();
  }

  // ---------- Validación en el navegador (HU-01) ----------
  function validar(archivo) {
    if (!archivo) return "Elige un archivo CSV.";
    if (!(archivo.name || "").toLowerCase().endsWith(".csv")) return "Solo se aceptan archivos .csv.";
    if (archivo.size === 0) return "El archivo está vacío.";
    if (archivo.size > MAX_BYTES) return "El archivo supera el máximo de 10 MB.";
    return null;
  }

  // ---------- Llamadas a la API ----------
  async function consultar() {
    try {
      const r = await fetch("api/alertas");
      if (!r.ok) throw new Error("HTTP " + r.status);
      guardarResumen(await r.json());
    } catch (e) {
      mostrarEstado("No se pudo consultar el estado actual. ¿Está corriendo el servidor y la base de datos?", "error");
    }
  }

  async function cargar(evento) {
    evento.preventDefault();
    const archivo = entrada.files[0];
    const problema = validar(archivo);
    if (problema) { mostrarEstado(problema + " No se envió nada.", "error"); return; }

    const cuerpoPeticion = new FormData();
    cuerpoPeticion.append("archivo", archivo);
    botonCargar.disabled = true;
    mostrarEstado("Analizando " + archivo.name + "…");

    try {
      const r = await fetch("api/contratos/cargar", { method: "POST", body: cuerpoPeticion });
      const cuerpo = await r.json().catch(() => ({}));
      if (!r.ok) {
        // El servidor manda el mensaje tal cual (400, 409, 413, 500), con el
        // número de fila cuando el problema está en una fila concreta.
        mostrarEstado("No se cargó el archivo: " + (cuerpo.error || "error " + r.status) + ". No se guardó nada.", "error");
        return;
      }
      guardarResumen(cuerpo);
      agregarAlHistorial(archivo.name, cuerpo);
      const avisos = [];
      if (cuerpo.duplicadosIgnorados) avisos.push(cuerpo.duplicadosIgnorados + " repetido(s) ignorado(s)");
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
    zona.classList.toggle("zona--lista", Boolean(archivo));
    $("zona-texto").textContent = archivo ? archivo.name + " (" + Math.ceil(archivo.size / 1024) + " KB)"
                                          : "Arrastra el archivo aquí o haz clic para elegirlo";
    const problema = archivo ? validar(archivo) : null;
    botonCargar.disabled = !archivo || Boolean(problema);
    if (problema) mostrarEstado(problema, "error");
    else if (archivo) mostrarEstado("");
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

  // ---------- Buscador ----------
  buscador.addEventListener("input", () => { filtro = plano(buscador.value.trim()); render(); });

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
