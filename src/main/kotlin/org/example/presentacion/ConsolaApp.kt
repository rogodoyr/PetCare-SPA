package org.example.presentacion

import org.example.asincrono.ResultadoOperacion
import org.example.modelo.*
import org.example.negocio.PetCareSistema
import org.example.negocio.ReporteService
import org.example.negocio.ValidadorDatos
import java.time.LocalDateTime

/**
 * Interfaz de consola del sistema PetCare.
 */
class ConsolaApp(private val sistema: PetCareSistema = PetCareSistema()) {

    fun iniciar() {
        println("============================================================")
        println("   PetCare - Sistema de Gestion de Boxes Veterinarios")
        println("============================================================")

        while (true) {
            mostrarMenu()
            val entrada = leerTexto("Seleccione una opcion:") ?: break
            try {
                when (entrada.trim().toIntOrNull() ?: -1) {
                    1 -> registrarEntradaInteractivo()
                    2 -> registrarSalidaInteractiva()
                    3 -> ReporteService.mostrarBoxes(sistema)
                    4 -> ReporteService.mostrarConsultas(sistema)
                    5 -> ReporteService.reporteCierreTurno(sistema)
                    0 -> {
                        println("")
                        println("Saliendo del sistema PetCare. Hasta luego!")
                        return
                    }
                    else -> println("Opcion no valida. Intente nuevamente.")
                }
            } catch (e: Exception) {
                // Nunca caer fuera del menu por un error inesperado
                println("ERROR: ${e.message ?: "Ocurrio un problema. Intente nuevamente."}")
            }
        }
        println("Fin de la entrada. Saliendo del sistema PetCare.")
    }

    private fun mostrarMenu() {
        println("")
        println("--- MENU PRINCIPAL ---")
        println("1. Registrar entrada de paciente")
        println("2. Registrar salida de paciente")
        println("3. Estado de los boxes")
        println("4. Consultas de negocio")
        println("5. Reporte de cierre de turno")
        println("0. Salir")
    }

    // ------------------------------------------------------------------
    // Entrada interactiva
    // ------------------------------------------------------------------

    private fun registrarEntradaInteractivo() {
        println("")
        println("--- Registrar entrada ---")

        // 1) Si no hay boxes libres, no se pide ningun dato
        if (!sistema.hayBoxesLibres()) {
            println("ERROR: Sistema sin capacidad. No hay boxes libres.")
            println("Libere un box (opcion 2) o intente mas tarde.")
            println(">> Volviendo al menu principal...")
            return
        }

        // 2) El codigo se valida de inmediato (formato y unicidad)
        val codigo = leerTexto("Codigo de atencion (ej. CA12CD):") ?: return
        if (!ValidadorDatos.codigoValido(codigo)) {
            println("ERROR DE DATOS: '$codigo' no cumple el formato LL DD LL (ej. CA12CD).")
            println(">> Volviendo al menu principal...")
            return
        }
        if (sistema.codigoYaRegistrado(codigo)) {
            println("ERROR DE DATOS: El codigo '$codigo' ya esta registrado en el turno.")
            println(">> Volviendo al menu principal...")
            return
        }

        // 3) Recien ahora se piden el resto de los campos
        val nombre = leerTexto("Nombre de la mascota:") ?: return
        val especie = leerTexto("Especie / raza:") ?: return

        val tipoTexto = leerTexto("Tipo de dueno (particular/convenio/municipal):") ?: return
        val tipoDueno = ValidadorDatos.validarTipoDueno(tipoTexto)
        if (tipoDueno == null) {
            println("ERROR DE DATOS: Tipo de dueno invalido. Solo se acepta particular, convenio o municipal.")
            println(">> Volviendo al menu principal...")
            return
        }

        val tipoPaciente = leerTexto("Tipo de paciente (canino/felino/exotico):") ?: return
        val paciente: Paciente = try {
            crearPaciente(tipoPaciente, codigo, nombre, especie, tipoDueno)
        } catch (e: IllegalArgumentException) {
            println("ERROR DE DATOS: ${e.message}")
            println(">> Volviendo al menu principal...")
            return
        }

        procesarResultado(sistema.registrarEntrada(paciente))
    }

    private fun crearPaciente(
        tipo: String,
        codigo: String,
        nombre: String,
        especie: String,
        tipoDueno: TipoDueno
    ): Paciente {
        val ahora = LocalDateTime.now()
        return when (tipo.trim().lowercase()) {
            "canino" -> Canino(codigo.trim().uppercase(), nombre, especie, ahora, tipoDueno)
            "felino" -> Felino(codigo.trim().uppercase(), nombre, especie, ahora, tipoDueno)
            "exotico", "exótico" -> {
                val silvestre = leerSiNo("Es animal silvestre? (si/no):")
                    ?: throw IllegalArgumentException(
                        "Valor de silvestre invalido. Solo se acepta 'si' o 'no'. Registro cancelado."
                    )
                Exotico(codigo.trim().uppercase(), nombre, especie, ahora, tipoDueno, silvestre)
            }
            else -> throw IllegalArgumentException("Tipo de paciente invalido: '$tipo'. Use canino, felino o exotico.")
        }
    }

    // ------------------------------------------------------------------
    // Salida interactiva
    // ------------------------------------------------------------------

    private fun registrarSalidaInteractiva() {
        println("")
        println("--- Registrar salida ---")
        val codigo = leerTexto("Codigo de atencion del paciente:") ?: return
        val minutos = leerDouble("Tiempo de uso (minutos):")
        procesarResultado(sistema.registrarSalida(codigo, minutos))
    }

    // ------------------------------------------------------------------
    // Manejo del resultado de la operacion (sealed class + when)
    // ------------------------------------------------------------------

    private fun procesarResultado(resultado: ResultadoOperacion) {
        when (resultado) {
            is ResultadoOperacion.EntradaExitosa -> {
                println("OK: Entrada registrada en Box #${resultado.numeroBox}.")
                println("   ${resultado.paciente.detalle()}")
            }
            is ResultadoOperacion.SalidaExitosa -> {
                val t = resultado.ticket
                println("OK: Salida registrada. Ticket #${t.numero} emitido.")
                println("   Tipo: ${t.tipoPaciente} | Codigo: ${t.codigoAtencion}")
                println("   Tiempo: ${ReporteService.formatoMinutos(t.tiempoUsoMinutos)}")
                println("   Monto pagado: $${ReporteService.formatoMonto(t.montoPagado)}")
            }
            is ResultadoOperacion.Error -> {
                println("ERROR: ${resultado.mensaje}")
            }
        }
    }

    // ------------------------------------------------------------------
    // Utilidades de lectura por consola
    // ------------------------------------------------------------------

    private fun leerTexto(mensaje: String): String? {
        println(mensaje)
        System.out.flush()
        return readlnOrNull()?.trim()
    }

    /** Lee si/no con validacion estricta. Reintenta si el valor es invalido. */
    private fun leerSiNo(mensaje: String): Boolean? {
        while (true) {
            val texto = leerTexto(mensaje) ?: return null
            val valor = ValidadorDatos.interpretarSiNo(texto)
            if (valor != null) return valor
            println("ERROR DE DATOS: '$texto' no es valido. Escriba exactamente 'si' o 'no'.")
        }
    }

    private fun leerDouble(mensaje: String): Double {
        return leerTexto(mensaje)?.toDoubleOrNull() ?: 0.0
    }
}
