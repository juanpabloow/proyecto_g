# Detector de Alertas de Contratación (DAC)

Sistema de apoyo para analistas de inteligencia que detecta posibles redes
de soborno o extorsión en la contratación pública, cruzando datos que hoy
se revisan manualmente en hojas de cálculo.

## Integrantes
- Juan Pablo Cardozo Rivera
- Yerson Andrés Pérez Cadena
- Gerson Geovanni Rojo Rodríguez

## Estado actual (Semana 3)
Esqueleto andante: carga contratos desde CSV y detecta el caso más simple
de reincidencia contratista-funcionario, con su primera prueba unitaria.

## Cómo correrlo
```bash
pip install pytest
python3 main.py          # corre el flujo con datos de ejemplo
python3 -m pytest -v     # corre la prueba unitaria
```

## Documentación
- [Visión del producto](docs/vision-producto.md)
- [Equipo](docs/Equipo/)
- [Problema duro](docs/problema-duro.md)
- [Decisiones técnicas](docs/decisiones-tecnicas.md)
- [Uso de IA](docs/uso-ia.md)
- [Historias de usuario](docs/historias-usuario.md)
