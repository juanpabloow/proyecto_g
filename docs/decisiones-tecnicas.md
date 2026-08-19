# Decisiones técnicas

| # | Decisión | Alternativa descartada | Por qué |
|---|---|---|---|
| 1 | Elegimos resolver **idempotencia** como problema duro de esta entrega | Concurrencia | Es más frecuente en el flujo manual real (archivos reenviados o duplicados) y es más simple de resolver bien en el tiempo disponible |
| 2 | El MVP cubre **una sola señal de alerta**: reincidencia contratista-funcionario | Incluir también fraccionamiento desde ya | Preferimos una funcionalidad bien sustentada que cuatro a medias; el fraccionamiento queda documentado como trabajo futuro |
| 3 | Los datos se cargan desde un archivo **CSV** | Conectar directo a una base de datos o a SECOP | No hay tiempo ni necesidad todavía; un CSV es suficiente para demostrar el flujo completo end-to-end |
| 4 | Lenguaje: **Python** | Java / JavaScript | Es el más simple de leer y explicar para el equipo, y tiene librerías simples para manejar datos tabulares (pandas) |

Nuevas decisiones se van agregando aquí a medida que el equipo avanza.
