# Modelo de dominio

Las clases reales del paquete `dac.dominio` (`java/src/main/java/dac/dominio/`),
con sus campos, sus métodos públicos y sus relaciones. Los *records* de
Java se marcan `<<record>>`: sus componentes son los campos y cada uno
tiene su método de acceso con el mismo nombre (`contrato.monto()`). Los
miembros estáticos llevan `$`.

El dominio no conoce la base de datos: no importa nada de
`dac.persistencia` ni de Spring Data. Solo `CargadorDeContratos` y
`DetectorDeReincidencias` son `@Component`, para poder inyectarlos.

## 1. Dominio

```mermaid
classDiagram
    direction LR

    class Contrato {
        <<record>>
        +String numeroContrato
        +String entidad
        +String contratista
        +String funcionario
        +String monto
        +String fecha
        +Map~String,String~ camposAdicionales
        +de(numeroContrato, entidad, contratista, funcionario, monto, fecha)$ Contrato
        +claveNatural() String
        +par() String
        +contratistaEnEntidad() String
    }

    class CargadorDeContratos {
        <<Component>>
        +List~String~ COLUMNAS_REQUERIDAS$
        +List~String~ CAMPOS_OBLIGATORIOS$
        +Map~String,Integer~ LONGITUD_MAXIMA$
        +cargarDesde(InputStream stream) ResultadoCarga
        +camposDistintos(Contrato primera, Contrato otra)$ List~String~
    }

    class LectorCSV {
        <<package-private>>
        ~filas(String texto)$ List~List~String~~
        ~estaEnBlanco(List~String~ fila)$ boolean
    }

    class ResultadoCarga {
        <<record>>
        +List~Contrato~ contratos
        +List~FilaDescartada~ filasDescartadas
        +int duplicadosIgnorados
        +List~ConflictoDeDuplicado~ conflictos
        +Map~String,Integer~ filaPorClave
        +cantidadDescartadas() int
        +filaDe(Contrato contrato) int
    }

    class FilaDescartada {
        <<record>>
        +int fila
        +String motivo
    }

    class ConflictoDeDuplicado {
        <<record>>
        +int fila
        +String numeroContrato
        +String entidad
        +String conflictoCon
        +List~String~ camposDistintos
        +String YA_EN_LA_BASE$
    }

    class NormalizadorDeNombres {
        +normalizar(String nombre)$ String
    }

    class DetectorDeReincidencias {
        <<Component>>
        -int umbral = 2
        +detectar(List~Contrato~ contratos) List~Alerta~
    }

    class Alerta {
        <<record>>
        +String contratista
        +String funcionario
        +List~Contrato~ evidencia
        +cantidadContratos() int
    }

    class DetectorDeFraccionamiento {
        +BigDecimal TOPE_POR_DEFECTO$
        +int VENTANA_POR_DEFECTO$
        -BigDecimal tope
        -int ventanaDias
        +DetectorDeFraccionamiento(BigDecimal tope, int ventanaDias)
        +detectar(List~Contrato~ contratos) List~AlertaFraccionamiento~
        +tope() BigDecimal
        +ventanaDias() int
    }

    class AlertaFraccionamiento {
        <<record>>
        +String contratista
        +String entidad
        +List~Contrato~ evidencia
        +BigDecimal montoTotal
        +BigDecimal tope
        +int ventanaDias
        +tipo() String
    }

    class ExportadorDeAlertas {
        +char SEPARADOR$
        +String ENCABEZADO$
        +exportar(List~Alerta~ reincidencias, List~AlertaFraccionamiento~ fraccionamientos)$ String
        ~celda(String valor)$ String
    }

    CargadorDeContratos ..> LectorCSV : lee el texto con
    CargadorDeContratos ..> ResultadoCarga : produce
    ResultadoCarga "1" *-- "*" Contrato : contratos
    ResultadoCarga "1" *-- "*" FilaDescartada : filasDescartadas
    ResultadoCarga "1" *-- "*" ConflictoDeDuplicado : conflictos
    Contrato ..> NormalizadorDeNombres : par() y contratistaEnEntidad()
    DetectorDeReincidencias ..> Contrato : agrupa por par()
    DetectorDeReincidencias ..> Alerta : genera
    Alerta "1" o-- "2..*" Contrato : evidencia
    DetectorDeFraccionamiento ..> Contrato : agrupa por contratistaEnEntidad()
    DetectorDeFraccionamiento ..> AlertaFraccionamiento : genera
    AlertaFraccionamiento "1" o-- "2..*" Contrato : evidencia
    ExportadorDeAlertas ..> Alerta : lee
    ExportadorDeAlertas ..> AlertaFraccionamiento : lee
```

**Invariantes que el código hace cumplir:**

- `Alerta` y `AlertaFraccionamiento` lanzan `IllegalArgumentException` si
  reciben menos de 2 contratos de evidencia: una alerta sin evidencia es un
  defecto (HU-02, criterio 5).
- `Contrato.claveNatural()` es `numeroContrato` + `entidad` (R-2). `par()` y
  `contratistaEnEntidad()` comparan los nombres **normalizados** (R-5), pero
  los campos guardan el nombre original.
- `monto` y `fecha` son `String` en el dominio (R-8, deuda DT-07). Se
  convierten a `BigDecimal` y `LocalDate` en `DetectorDeFraccionamiento` y al
  guardar, en `ContratoServicio`.
- Las listas de los *records* se copian con `List.copyOf`: son inmutables.

## 2. Cómo se usa desde la aplicación y la persistencia

El dominio se orquesta en `dac.aplicacion` y se guarda con entidades JPA
aparte (`dac.persistencia`), que mapean las 6 tablas de
[esquema-bd.md](esquema-bd.md).

```mermaid
classDiagram
    direction LR

    class ContratoServicio {
        <<Service>>
        +cargarYDetectar(InputStream csv) ResultadoServicio
        +consultarAlertas() ResumenDeAlertas
        +exportarAlertas() String
    }
    class ResultadoServicio {
        <<record>>
        int contratosNuevos
        int totalEnBd
        int duplicadosIgnorados
        List~FilaDescartada~ filasDescartadas
        List~ConflictoDeDuplicado~ conflictos
        List~Alerta~ alertas
        List~AlertaFraccionamiento~ alertasFraccionamiento
    }
    class ResumenDeAlertas {
        <<record>>
        int totalEnBd
        List~Alerta~ alertas
        List~AlertaFraccionamiento~ alertasFraccionamiento
    }
    class ContratoControlador {
        <<RestController>>
        +cargar(MultipartFile archivo) ResponseEntity
    }
    class AlertaControlador {
        <<RestController>>
        +consultar() ResumenDeAlertas
        +exportar() ResponseEntity
    }
    class EntidadJpa {
        <<Entity>>
        Long id
        String nombre
    }
    class ContratistaJpa {
        <<Entity>>
        Long id
        String nombre
    }
    class FuncionarioJpa {
        <<Entity>>
        Long id
        String nombre
    }
    class ContratoJpa {
        <<Entity>>
        Long id
        String numeroContrato
        BigDecimal monto
        LocalDate fecha
    }
    class AlertaJpa {
        <<Entity>>
        Long id
        String tipo
        OffsetDateTime generadaEn
    }

    ContratoControlador --> ContratoServicio
    AlertaControlador --> ContratoServicio
    ContratoServicio ..> CargadorDeContratos
    ContratoServicio ..> DetectorDeReincidencias
    ContratoServicio ..> DetectorDeFraccionamiento
    ContratoServicio ..> ExportadorDeAlertas
    ContratoServicio ..> ResultadoServicio : devuelve
    ContratoServicio ..> ResumenDeAlertas : devuelve
    ContratoJpa "*" --> "1" EntidadJpa
    ContratoJpa "*" --> "1" ContratistaJpa
    ContratoJpa "*" --> "1" FuncionarioJpa
    AlertaJpa "*" --> "1" ContratistaJpa
    AlertaJpa "*" --> "1" FuncionarioJpa
    AlertaJpa "*" --> "*" ContratoJpa : evidencia (alerta_contrato)
```

| Clase de dominio | Se guarda como | Tabla |
|---|---|---|
| `Contrato` | `ContratoJpa` (+ `EntidadJpa`, `ContratistaJpa`, `FuncionarioJpa`) | `contrato`, `entidad`, `contratista`, `funcionario` |
| `Contrato.claveNatural()` | restricción única | `uq_contrato_clave_natural (id_entidad, numero_contrato)` |
| `Alerta` | `AlertaJpa` con `tipo = REINCIDENCIA` | `alerta` |
| `Alerta.evidencia` | `AlertaJpa.evidencia` (`@ManyToMany`) | `alerta_contrato` |
| `AlertaFraccionamiento` | **No se guarda** (deuda DT-01) | — |
| `ConflictoDeDuplicado`, `FilaDescartada` | **No se guardan**: viajan en la respuesta (DT-08) | — |

Cada repositorio (`EntidadRepo`, `ContratistaRepo`, `FuncionarioRepo`,
`ContratoRepo`, `AlertaRepo`) extiende `JpaRepository`. Ver el patrón
Repository en [decisiones-tecnicas.md](decisiones-tecnicas.md).
