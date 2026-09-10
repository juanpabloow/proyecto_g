from src.contratos import cargar_contratos
from src.alertas import detectar_reincidencias
from src import repositorio

RUTA_EJEMPLO = "data/contratos_ejemplo.csv"


def main():
    # 1. Cargar desde CSV (idempotencia dentro de la carga — problema duro)
    contratos_csv = cargar_contratos(RUTA_EJEMPLO)
    print(f"Contratos en CSV: {len(contratos_csv)}")

    # 2. Persistir en BD e trabajar sobre el histórico completo (HU-07)
    conn = None
    try:
        conn = repositorio.conectar()
        nuevos = repositorio.guardar_contratos(conn, contratos_csv)
        print(f"Contratos nuevos guardados en BD: {nuevos}")

        contratos = repositorio.cargar_contratos_bd(conn)
        print(f"Contratos totales en BD: {len(contratos)}")
    except Exception as e:
        print(f"[BD no disponible: {e}]")
        print("Trabajando solo en memoria (sin persistencia entre ejecuciones).")
        contratos = contratos_csv

    # 3. Detectar reincidencias
    alertas = detectar_reincidencias(contratos)
    print(f"Alertas encontradas: {len(alertas)}\n")

    for alerta in alertas:
        print(f"⚠️  {alerta['contratista']} + {alerta['funcionario']}")
        for c in alerta["contratos"]:
            print(f"   - Contrato {c['numero_contrato']} ({c['entidad']}, {c['fecha']}, ${c['monto']})")
        print()

    # 4. Persistir alertas
    if conn:
        repositorio.guardar_alertas(conn, alertas)
        conn.close()


if __name__ == "__main__":
    main()
