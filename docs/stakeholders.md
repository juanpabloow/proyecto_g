# Stakeholders (partes interesadas)

Quién se ve afectado por DAC, qué espera del sistema y qué exige eso del
diseño. Se incluyen tanto los que lo usan como los que *aparecen en los
datos* sin haberlo pedido.

## 1. Mapa de stakeholders

| # | Stakeholder | Tipo | Qué espera de DAC | Qué exige del diseño | Influencia |
|---|---|---|---|---|---|
| S-1 | **Analista de investigación e inteligencia** | Usuario primario | Dejar de cruzar contratos a mano y confiar en lo que el sistema marca | Alertas con evidencia; cero alertas duplicadas | **Alta** |
| S-2 | **Coordinador / jefe de la unidad de análisis** | Usuario secundario, decide la adopción | Ver cuántos contratos se procesaron y cuántas alertas salieron | Conteos claros y reproducibles | Alta |
| S-3 | **Investigador judicial / fiscal** | Consumidor del resultado | Usar la alerta como punto de partida de una investigación formal | Trazabilidad: poder reconstruir por qué se generó la alerta | Alta |
| S-4 | **Control interno / auditoría / contraloría** | Usuario secundario potencial | Aplicar la misma revisión a sus propios contratos | Que la regla de alerta esté escrita y sea auditable | Media |
| S-5 | **Entidad contratante** (alcaldía, gobernación) | Dueña de los datos y sujeto del análisis | No ser señalada por un error de datos | Manejo explícito de duplicados y datos incompletos | Media |
| S-6 | **Contratistas y funcionarios señalados** | **Afectados, no usuarios** | No ser tratados como culpables por una coincidencia | Presunción de inocencia; una alerta es hipótesis, no hallazgo | Baja (interés muy alto) |
| S-7 | **Área de TI / seguridad de la información** | Habilitador | Que los datos sensibles no queden sueltos en el repositorio | Datos reales fuera de Git ([R-7](reglas-de-negocio.md)) | Media |
| S-8 | **Jurídico / protección de datos personales** | Restricción | Cumplimiento en el tratamiento de datos personales | Minimizar datos; no perfilar personas | Media |
| S-9 | **Ciudadanía, veedurías y medios** | Beneficiario indirecto | Que el gasto público se vigile mejor | El método debe poder explicarse en público | Baja |
| S-10 | **SECOP / Colombia Compra Eficiente** | Proveedor de datos futuro | — (aún no hay integración) | Modelo de datos compatible a futuro | Baja hoy |
| S-11 | **Equipo de desarrollo** (los 3 integrantes) | Ejecutor | Poder explicar y sostener lo entregado | Alcance pequeño y bien cubierto con pruebas | Alta |
| S-12 | **Docente del curso** | Patrocinador académico | Ver criterio de ingeniería, no solo código | Decisiones justificadas y documentadas | **Alta** |

## 2. Matriz influencia / interés

```
            INTERÉS BAJO            INTERÉS ALTO
INFLUENCIA  ┌───────────────────────┬───────────────────────────────┐
ALTA        │ (vacío)               │ S-1 Analista                  │
            │                       │ S-2 Coordinador               │
            │                       │ S-3 Fiscal                    │
            │                       │ S-11 Equipo · S-12 Docente    │
            ├───────────────────────┼───────────────────────────────┤
INFLUENCIA  │ S-10 SECOP            │ S-4 Control interno           │
BAJA        │ S-9 Ciudadanía        │ S-5 Entidad contratante       │
            │                       │ S-6 Señalados (¡proteger!)    │
            │                       │ S-7 TI · S-8 Jurídico         │
            └───────────────────────┴───────────────────────────────┘
```

- **Arriba a la derecha:** se gestionan de cerca; sus necesidades son
  requisitos del MVP.
- **Abajo a la derecha:** no deciden, pero **el diseño los protege**.
  S-6 es el caso importante: son los únicos que pueden salir
  perjudicados por un defecto del sistema, y no tienen forma de
  reclamar. De ahí la regla de que una alerta nunca es una acusación
  ([visión, §4](vision-producto.md)).

## 3. De stakeholder a requisito

| Stakeholder | Requisito que originó |
|---|---|
| S-1 | Idempotencia como problema duro ([problema-duro.md](problema-duro.md)) |
| S-1, S-3 | Cada alerta lista los contratos que la originaron ([HU-02](historias-usuario.md)) |
| S-2 | `main.py` imprime contratos cargados y alertas encontradas |
| S-5, S-6 | El sistema no puntúa personas; solo describe coincidencias ([R-4](reglas-de-negocio.md)) |
| S-7, S-8 | Los datos reales van en `data/privado/`, fuera de Git ([R-7](reglas-de-negocio.md)) |
| S-11, S-12 | Alcance de una sola señal, con pruebas y decisiones registradas |

## 4. Pendiente de validar

Ningún analista real (S-1) ha revisado todavía la señal de reincidencia.
Es el supuesto más frágil del proyecto: **si el umbral correcto no es
"aparece más de una vez" sino "más de N veces en un periodo", la regla
cambia**. Queda como la primera pregunta a validar en la siguiente
iteración (ver [R-3](reglas-de-negocio.md)).
