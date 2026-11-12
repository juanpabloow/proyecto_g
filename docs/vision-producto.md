# Visión del producto — Detector de Alertas de Contratación (DAC)

## 1. Declaración de visión

> Para **analistas de investigación e inteligencia** que hoy cruzan
> contratos públicos a mano en hojas de cálculo, **DAC** es una
> **herramienta de detección de señales de riesgo en contratación
> pública** que **encuentra automáticamente patrones sospechosos y
> muestra la evidencia exacta que los originó**.
> A diferencia del **cruce manual en Excel**, DAC es **repetible,
> trazable y no vuelve a alertar cuando el mismo contrato se carga dos
> veces**.

## 2. El problema

Hoy el analista detecta posibles redes de soborno o extorsión abriendo
varias hojas de cálculo y comparando a ojo. Eso produce tres dolores
concretos:

| Dolor | Qué pasa hoy | Qué cuesta |
|---|---|---|
| **Lento** | El cruce se hace fila por fila, a mano | Horas por lote; los lotes grandes simplemente no se revisan |
| **No trazable** | La alerta queda como una nota o un color en una celda | No se puede reconstruir *por qué* se marcó algo |
| **No repetible** | El resultado depende de quién revisó y de qué tan cansado estaba | Dos analistas sobre los mismos datos llegan a conclusiones distintas |

A eso se suma la causa de error más frecuente en el flujo real: **el
mismo contrato llega duplicado** (archivo reenviado, copia mal hecha en
Excel). Un duplicado no detectado se convierte en una alerta falsa, y
una alerta falsa le quita al analista lo único que hace útil a la
herramienta: la confianza. Por eso la idempotencia es el problema duro
de esta entrega (ver [problema duro](problema-duro.md)).

## 3. La solución

DAC recibe los contratos (entidad, contratista, funcionario, monto,
fecha), descarta los que ya había cargado y aplica **señales de alerta**
sobre el resto. Cada alerta viene acompañada de los contratos exactos
que la originaron, para que el analista pueda verificarla en segundos en
vez de reconstruirla.

La señal de esta entrega es **reincidencia contratista–funcionario**: el
mismo contratista adjudicado repetidamente por el mismo funcionario. No
es prueba de nada, pero es el patrón más simple y más citado como punto
de partida en una investigación.

## 4. Qué **no** es DAC (límite ético y legal)

DAC **no acusa a nadie y no decide nada**. Una alerta es una *hipótesis
a verificar por una persona*, no un hallazgo ni una sanción.
Consecuencias de diseño que se derivan de esto:

- Toda alerta debe mostrar su evidencia; una alerta sin evidencia es un
  defecto, no una alerta.
- El sistema no calcula "culpabilidad", "puntajes de corrupción" ni
  rankings de personas.
- Los nombres que aparecen en una alerta corresponden a personas reales
  amparadas por la presunción de inocencia y por la normativa de datos
  personales (ver [stakeholders](stakeholders.md), fila *Contratistas y
  funcionarios señalados*).

## 5. Usuarios objetivo

- **Usuario primario:** analista de investigación e inteligencia que hoy
  hace el cruce a mano.
- **Usuarios secundarios:** coordinador del área (necesita ver el
  volumen y el estado del análisis) y control interno / auditoría.

El mapa completo de partes interesadas está en
[stakeholders.md](stakeholders.md).

## 6. Alcance de la entrega final

El MVP empezó con una sola señal y sin base de datos. Al cierre (noviembre
de 2026) el alcance es este; el estado de cada historia está en
[backlog.md](backlog.md).

**Dentro:**

1. Cargar contratos desde un archivo CSV, validando columnas, campos y
   tipos en la frontera (HU-01, HU-04).
2. **Idempotencia dentro de la carga y entre ejecuciones**: los contratos se
   guardan en PostgreSQL y una recarga no duplica nada (HU-07, el
   [problema duro](problema-duro.md)).
3. Dos señales, cada una con los contratos que la originaron:
   reincidencia contratista–funcionario (HU-02) y fraccionamiento (HU-03).
4. Nombres escritos de forma distinta reconocidos como el mismo (HU-05) y
   duplicados con datos distintos reportados (HU-06).
5. Interfaz web con inicio de sesión y exportación del reporte a Excel (HU-08).

**Fuera, y documentado:**

| Fuera del alcance | Por qué | Dónde queda registrado |
|---|---|---|
| Integración con SECOP | Requiere credenciales y modelo de datos real | [Decisión 3](decisiones-tecnicas.md) |
| Roles y varios usuarios | Hoy hay un solo analista en `localhost` | [DT-03](deuda-tecnica.md) |
| Despliegue en un servidor (HTTPS, cifrado, endurecimiento de la base) | Sin despliegue no hay tráfico por la red; condición para usar datos reales | [DT-14, DT-19, DT-21](deuda-tecnica.md) |
| Carga concurrente por dos usuarios con prueba automática | La base ya impide el duplicado y responde 409; falta la prueba | [DT-09](deuda-tecnica.md) |
| Reglas de negocio validadas por un analista real | Umbral, ventana, tope y normalización los propuso el equipo | [DT-02](deuda-tecnica.md) |

## 7. Cómo se ve el éxito

| Indicador | Meta de esta entrega |
|---|---|
| Alertas duplicadas al recargar el mismo archivo | **0** |
| Alertas sin evidencia (sin contratos que las originen) | **0** |
| Tiempo de análisis de un lote de ejemplo | Menor al cruce manual equivalente |
| Un integrante cualquiera puede explicar cómo se genera una alerta | **Sí** |

## 8. Supuestos

- Los datos de contratación llegan en un archivo tabular con las seis
  columnas requeridas.
- El campo `numero_contrato` es único dentro de una misma entidad (ver
  [R-2](reglas-de-negocio.md)).
- El analista revisa manualmente cada alerta antes de actuar.

## 9. Roadmap

| Iteración | Foco | Estado |
|---|---|---|
| Esqueleto andante | Carga idempotente + señal de reincidencia + pruebas | ✅ Hecho (agosto–septiembre de 2026) |
| Persistencia | Contratos y alertas en PostgreSQL: la idempotencia sobrevive entre ejecuciones (HU-07) | ✅ Hecho (septiembre de 2026) |
| Backlog completo | Fraccionamiento, nombres, conflictos, exportación e interfaz web | ✅ Hecho (octubre de 2026) |
| Seguridad y RNF | Checklist, login, RNF medidos, observabilidad | ✅ Hecho (octubre–noviembre de 2026) |
| Siguiente | Validar las reglas con un analista real (DT-02) y guardar el fraccionamiento (DT-01) | Pendiente |
| Largo plazo | Despliegue seguro, roles, integración con SECOP | Pendiente |
