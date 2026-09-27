# PetCare — Sistema de Gestión de Boxes Veterinarios

Proyecto de la evaluación parcial 1 de **DSY1105 — Desarrollo de Aplicaciones Móviles**  
**EA1: Fundamentos de Kotlin y POO**

Aplicación de consola en **Kotlin** que controla el ingreso y salida de pacientes animales en boxes veterinarios, aplica tarifas diferenciadas según el tipo de paciente y el perfil del dueño, y genera reportes al cierre de cada turno.

---

## Cómo ejecutar

### En IntelliJ IDEA

1. Abrir la carpeta del proyecto.
2. Usar **JDK 17 o superior** (el proyecto usa toolchain 25).
3. `Build` → `Rebuild Project`.
4. Ejecutar `src/main/kotlin/org/example/Main.kt`.
5. Se recomienda la configuración de ejecución **PetCare** (tipo *Application*, main class `org.example.MainKt`).

> Si se ejecuta vía Gradle (`:org.example.MainKt.main()`), la consola interactiva puede no aceptar teclado o mostrar el menú a medias. Para uso normal, ejecutar como *Application*.

### Por terminal

```bash
./gradlew run
# o en Windows
gradlew.bat run
```

---

## Menú del sistema

```
1. Registrar entrada de paciente
2. Registrar salida de paciente
3. Estado de los boxes
4. Consultas de negocio
5. Reporte de cierre de turno
0. Salir
```

---

## Reglas de negocio

### Tipos de paciente y tarifas base

| Tipo    | Tarifa base | Regla especial |
|---------|-------------|----------------|
| Canino  | $12.000/hr  | 20% de descuento si el dueño tiene convenio |
| Felino  | $9.000/hr   | Cobro $0 si la atención dura menos de 20 minutos |
| Exótico | $20.000/hr  | Recargo del 30% si el animal es silvestre |

### Orden del cálculo del monto

1. Costo por tiempo y reglas del tipo de paciente.
2. IVA del **19%**.
3. Si el dueño es **municipal**, descuento del **50%** sobre el monto con IVA.

### Tipos de dueño

- `particular` — paga la tarifa completa.
- `convenio` — descuento del 20% (aplicado en Canino según el enunciado).
- `municipal` — descuento del 50% sobre el monto con IVA.

### Código de atención

- Formato: dos letras, dos dígitos, dos letras (ej. `CA12CD`).
- Es único dentro del turno; no se permite duplicar.
- Se valida al ingreso: si es inválido o ya existe, se cancela y se vuelve al menú sin pedir más datos.

### Estados de los boxes (10 boxes)

| Estado           | Significado |
|------------------|-------------|
| Libre            | Disponible para un paciente |
| EnAtencion       | Tiene un paciente asignado |
| EnProceso        | Registrando entrada o calculando tarifa (espera al sensor) |
| FueraDeServicio  | Inhabilitado, con motivo registrado |

### Registro asíncrono (sensores)

- **Entrada:** el box queda en `EnProceso` y espera **3 segundos** (`delay`).
- **Salida:** el box queda en `EnProceso` (cálculo de tarifa) y espera **6,5 segundos** (`delay`).

### Validaciones y errores (el programa no se detiene)

| Situación | Comportamiento |
|-----------|----------------|
| Código de atención inválido | Error inmediato y regreso al menú |
| Código duplicado | Error inmediato y regreso al menú |
| Sin boxes libres | Error inmediato y regreso al menú (sin pedir datos) |
| Tipo de dueño o paciente inválido | Error y regreso al menú |
| Valor de silvestre distinto de `si`/`no` | Reintenta; no avanza con datos basura |
| Tarifa resultante negativa o cero (caso no exento) | Error sin cerrar la sesión |
| Salida de un paciente inexistente | Error y continúa operando |

---

## Estructura del proyecto

```
src/main/kotlin/org/example/
├── Main.kt                        # Punto de entrada
├── modelo/
│   ├── TipoDueno.kt               # PARTICULAR / CONVENIO / MUNICIPAL
│   ├── EstadoBox.kt               # sealed class de estados del box
│   ├── Box.kt                     # Box con transiciones de estado
│   ├── Ticket.kt                  # Comprobante de atención
│   └── Paciente.kt                # open class + Canino, Felino, Exótico
├── negocio/
│   ├── CalculadoraTarifas.kt      # IVA 19% + descuento municipal
│   ├── ValidadorDatos.kt          # Formato de código, si/no, tipo de dueño
│   ├── PetCareSistema.kt          # Boxes, historial, recaudación, consultas
│   └── ReporteService.kt          # Consultas y reporte de cierre
├── asincrono/
│   ├── ResultadoOperacion.kt      # sealed class de resultado
│   └── ServicioSensores.kt        # suspend fun + delay (3s / 6,5s)
└── presentacion/
    └── ConsolaApp.kt              # Menú e interacción por consola
```

---

## Cobertura de la rúbrica

| Criterio | Implementación |
|----------|----------------|
| Lógica básica (tipos, operadores, condicionales) | `CalculadoraTarifas`, `ValidadorDatos` |
| POO: `open class` + herencia + `override` | `Paciente` → `Canino`, `Felino`, `Exotico` (`calcularMontoBase`) |
| Colecciones `List` + `filter` / `map` / `sumOf` | Historial y consultas en `PetCareSistema` |
| `suspend fun` + `delay` | `ServicioSensores` |
| `sealed class` + `when` | `EstadoBox`, `ResultadoOperacion` |
| Manejo de errores (`try-catch` / validaciones) | `CalculadoraTarifas`, `ConsolaApp` |
| Archivos `.kt` con responsabilidades separadas | Paquetes `modelo`, `negocio`, `asincrono`, `presentacion` |
| Dependencia de corrutinas | `kotlinx-coroutines-core` en `build.gradle.kts` |
| Convenciones Kotlin | PascalCase clases, camelCase funciones/propiedades |

---

## Tecnologías

- Kotlin 1.9+ (proyecto configurado con Kotlin JVM plugin)
- `kotlinx-coroutines-core`
- Gradle (Kotlin DSL)
- JDK 17+ (toolchain 25)

---

## Notas de implementación

- El sistema permanece operativo ante errores de datos: un error no detiene ni reinicia el programa.
- La validación del campo *silvestre* es estricta (`si` / `no`); otros valores no se aceptan porque alteran el recargo del 30%.
- Al llenar los 10 boxes, se avisa al intentar una nueva entrada **antes** de pedir datos.
- Los códigos de atención no se repiten dentro del turno (ni en boxes activos ni en el historial).
