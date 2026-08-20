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

## 6. Alcance de esta entrega (MVP)

**Dentro:**

1. Cargar contratos desde un archivo CSV, validando las columnas
   requeridas.
2. Ignorar contratos ya cargados (idempotencia dentro de la carga).
3. Detectar **una** señal: reincidencia contratista–funcionario.
4. Mostrar cada alerta con los contratos que la originaron.

**Fuera, y documentado como trabajo futuro:**

| Fuera del MVP | Por qué | Dónde queda registrado |
|---|---|---|
| Detección de fraccionamiento de contratos | Preferimos una señal bien sustentada a cuatro a medias | [HU-03](historias-usuario.md) |
| Tablero visual | El valor está en la señal, no en la presentación | Roadmap, abajo |
| Integración con SECOP | Requiere credenciales y modelo de datos real | [Decisión 3](decisiones-tecnicas.md) |
| Persistencia entre ejecuciones | Sin base de datos todavía | [Problema duro, §5](problema-duro.md) |
| Carga concurrente por dos usuarios | Riesgo conocido, no resuelto | [Problema duro, §5](problema-duro.md) |
| Normalización de nombres ("ACME SAS" vs "ACME S.A.S.") | Requiere reglas de negocio que aún no están validadas con un analista | [Reglas de negocio, R-5](reglas-de-negocio.md) |

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

| Iteración | Foco |
|---|---|
| **Actual** | Esqueleto andante: carga idempotente + señal de reincidencia + pruebas |
| Siguiente | Persistir contratos (base de datos) para que la idempotencia sobreviva entre ejecuciones |
| Después | Segunda señal: fraccionamiento ([HU-03](historias-usuario.md)) |
| Después | Tablero visual y exportación del reporte de alertas |
| Largo plazo | Integración con SECOP y manejo de concurrencia |
