from src.contratos import cargar_contratos
from src.alertas import detectar_reincidencias

RUTA_EJEMPLO = "data/contratos_ejemplo.csv"


def main():
    contratos = cargar_contratos(RUTA_EJEMPLO)
    print(f"Contratos cargados: {len(contratos)}")

    alertas = detectar_reincidencias(contratos)
    print(f"Alertas encontradas: {len(alertas)}\n")

    for alerta in alertas:
        print(f"⚠️  {alerta['contratista']} + {alerta['funcionario']}")
        for c in alerta["contratos"]:
            print(f"   - Contrato {c['numero_contrato']} ({c['entidad']}, {c['fecha']}, ${c['monto']})")
        print()


if __name__ == "__main__":
    main()
