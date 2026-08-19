# Problema duro: idempotencia

## Qué es
Idempotencia significa que si el mismo contrato se carga más de una vez
por error (por ejemplo, alguien vuelve a subir el mismo archivo CSV, o
una fila queda duplicada), el sistema **no debe generarlo como una alerta
nueva ni contarlo dos veces**.

## Por qué es el problema duro de esta entrega
En un flujo manual real, es común que un mismo contrato llegue duplicado
(archivo reenviado, copia mal hecha en Excel). Si el sistema no maneja
esto, generaría alertas falsas y le haría perder confianza al analista
que lo usa — justo el problema que hoy ya existe con el cruce manual.

## Cómo lo resolvemos en el MVP
Cada contrato se identifica con una clave única (por ejemplo, número de
contrato + entidad). Antes de procesar un contrato, el sistema verifica
si esa clave ya fue cargada; si ya existe, se ignora en vez de generar
una alerta duplicada.

## Qué queda fuera (riesgo futuro documentado)
- **Concurrencia:** qué pasa si dos personas cargan archivos al mismo
  tiempo. No se resuelve en este MVP; queda como riesgo conocido para
  una iteración futura.
