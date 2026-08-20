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


def test_agrupa_todos_los_contratos_del_par_en_una_sola_alerta():
    """Un par con 3 contratos = 1 alerta con 3 contratos de evidencia."""
    contratos = [
        {"numero_contrato": "1", "contratista": "ACME SAS", "funcionario": "Juan Pérez"},
        {"numero_contrato": "2", "contratista": "ACME SAS", "funcionario": "Juan Pérez"},
        {"numero_contrato": "3", "contratista": "ACME SAS", "funcionario": "Juan Pérez"},
    ]

    alertas = detectar_reincidencias(contratos)

    assert len(alertas) == 1
    assert len(alertas[0]["contratos"]) == 3


def test_mismo_contratista_con_funcionarios_distintos_no_alerta():
    """La señal es el par, no el contratista solo."""
    contratos = [
        {"numero_contrato": "1", "contratista": "ACME SAS", "funcionario": "Juan Pérez"},
        {"numero_contrato": "2", "contratista": "ACME SAS", "funcionario": "Ana Ruiz"},
    ]

    assert detectar_reincidencias(contratos) == []


def test_toda_alerta_trae_su_evidencia():
    """
    Invariante del producto: una alerta sin los contratos que la
    originaron es un defecto (ver docs/vision-producto.md, §4).
    """
    contratos = [
        {"numero_contrato": "1", "contratista": "ACME SAS", "funcionario": "Juan Pérez"},
        {"numero_contrato": "2", "contratista": "ACME SAS", "funcionario": "Juan Pérez"},
        {"numero_contrato": "3", "contratista": "Otra Ltda", "funcionario": "Ana Ruiz"},
        {"numero_contrato": "4", "contratista": "Otra Ltda", "funcionario": "Ana Ruiz"},
    ]

    alertas = detectar_reincidencias(contratos)

    assert len(alertas) == 2
    for alerta in alertas:
        assert len(alerta["contratos"]) >= 2
