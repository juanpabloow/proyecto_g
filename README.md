# Detector de Alertas de Contratación (DAC)

Sistema de apoyo para analistas de investigación e inteligencia que
detecta señales de posible soborno o extorsión en la contratación
pública, cruzando automáticamente datos que hoy se revisan a mano en
hojas de cálculo.

Cada alerta viene con **los contratos exactos que la originaron**, para
que el analista pueda verificarla en segundos. DAC **no acusa a nadie**:
una alerta es una hipótesis a verificar por una persona
([por qué](docs/vision-producto.md)).

## Integrantes

| Integrante | Rol | Ficha |
|---|---|---|
| Juan Pablo Cardozo Rivera | Ingeniero de backend | [ficha](docs/Equipo/juan-pablo-cardozo-rivera.md) |
| Yerson Andrés Pérez Cadena | Ingeniero de QA y pruebas | [ficha](docs/Equipo/yerson_cadena.md) |
| Gerson Geovanni Rojo Rodríguez | Ingeniero de requisitos y analista de negocio | [ficha](docs/Equipo/gerson_rojo.md) |

## Estado actual (Semana 3)

Esqueleto andante, funcionando de punta a punta:

- Carga contratos desde CSV validando las columnas requeridas.
- **Problema duro resuelto:** un contrato duplicado no genera una alerta
  falsa ([detalle](docs/problema-duro.md)).
- Detecta la señal del MVP: reincidencia contratista–funcionario.
- 12 pruebas automatizadas, todas pasando.

## Cómo correrlo

```bash
pip install pytest
python3 main.py          # flujo completo con los datos de ejemplo
python3 -m pytest -v     # las 12 pruebas
```

Salida esperada de `main.py`: 5 contratos cargados y 1 alerta
(`ACME SAS` + `Juan Pérez`, con 3 contratos como evidencia).

> Los datos de `data/contratos_ejemplo.csv` son **ficticios**. Los
> archivos con datos reales van en `data/privado/`, que no se sube al
> repositorio.

## Documentación

Empieza por el [mapa del proyecto](docs/mapa-del-proyecto.md) si no sabes
dónde buscar algo.

| Documento | Qué responde |
|---|---|
| [Mapa del proyecto](docs/mapa-del-proyecto.md) | Dónde está cada cosa, cómo funciona de punta a punta, qué falta |
| [Visión del producto](docs/vision-producto.md) | Qué problema resuelve, para quién, alcance y métricas de éxito |
| [Stakeholders](docs/stakeholders.md) | Quiénes están involucrados y qué exige cada uno del diseño |
| [Problema duro](docs/problema-duro.md) | Idempotencia: por qué es el problema duro y hasta dónde está resuelto |
| [Historias de usuario](docs/historias-usuario.md) | Backlog priorizado y la historia central desarrollada a fondo |
| [Reglas de negocio](docs/reglas-de-negocio.md) | Qué cuenta como alerta, glosario del dominio y limitaciones conocidas |
| [Decisiones técnicas](docs/decisiones-tecnicas.md) | Por qué se decidió cada cosa y qué se descartó |
| [Equipo](docs/Equipo/) | Rol y aporte de cada integrante |
| [Uso de IA](docs/uso-ia.md) | Cómo se usó la IA y la postura del equipo |
