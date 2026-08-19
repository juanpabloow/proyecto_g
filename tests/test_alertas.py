from src.alertas import detectar_reincidencias


def test_detecta_reincidencia_simple():
    contratos = [
        {"numero_contrato": "1", "contratista": "ACME SAS", "funcionario": "Juan Pérez"},
        {"numero_contrato": "2", "contratista": "ACME SAS", "funcionario": "Juan Pérez"},
        {"numero_contrato": "3", "contratista": "Otra Empresa", "funcionario": "Ana Ruiz"},
    ]

    alertas = detectar_reincidencias(contratos)

    assert len(alertas) == 1
    assert alertas[0]["contratista"] == "ACME SAS"
    assert alertas[0]["funcionario"] == "Juan Pérez"
    assert len(alertas[0]["contratos"]) == 2


def test_no_genera_alerta_si_no_hay_repeticion():
    contratos = [
        {"numero_contrato": "1", "contratista": "ACME SAS", "funcionario": "Juan Pérez"},
        {"numero_contrato": "2", "contratista": "Otra Empresa", "funcionario": "Ana Ruiz"},
    ]

    alertas = detectar_reincidencias(contratos)

    assert len(alertas) == 0
