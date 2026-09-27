package org.example.asincrono

import org.example.modelo.Paciente
import org.example.modelo.Ticket

/**
 * Clase sellada que modela los estados/posibles resultados de la operación asíncrona.
 * Se gestiona con expresiones `when` exhaustivas.
 */
sealed class ResultadoOperacion {

    /** Entrada registrada con éxito: box asignado y paciente en atención. */
    data class EntradaExitosa(val numeroBox: Int, val paciente: Paciente) : ResultadoOperacion()

    /** Salida registrada con éxito: ticket emitido. */
    data class SalidaExitosa(val ticket: Ticket) : ResultadoOperacion()

    /** Error de datos o de operación. No interrumpe la sesión. */
    data class Error(val mensaje: String) : ResultadoOperacion()
}
