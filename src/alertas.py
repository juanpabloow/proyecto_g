"""
Detección de la señal de alerta del MVP: reincidencia contratista-funcionario.

Si un mismo contratista aparece con el mismo funcionario en más de un
contrato, se genera una alerta que incluye los contratos exactos que
la originaron.
"""

from collections import defaultdict


def detectar_reincidencias(contratos):
    """
    Recibe una lista de contratos (dicts con al menos 'contratista' y
    'funcionario') y devuelve una lista de alertas: cada alerta indica
    el par contratista-funcionario repetido y los contratos involucrados.
    """
    grupos = defaultdict(list)

    for contrato in contratos:
        clave = (contrato["contratista"], contrato["funcionario"])
        grupos[clave].append(contrato)

    alertas = []
    for (contratista, funcionario), lista_contratos in grupos.items():
        if len(lista_contratos) > 1:
            alertas.append(
                {
                    "contratista": contratista,
                    "funcionario": funcionario,
                    "contratos": lista_contratos,
                }
            )

    return alertas
