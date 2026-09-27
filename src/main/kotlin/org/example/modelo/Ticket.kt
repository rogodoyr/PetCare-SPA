package org.example.modelo

/**
 * Comprobante emitido al finalizar la atención.
 * Incluye tiempo atendido y monto cobrado.
 */
data class Ticket(
    val numero: Int,
    val tipoPaciente: String,
    val codigoAtencion: String,
    val tiempoUsoMinutos: Double,
    val montoPagado: Double
)
