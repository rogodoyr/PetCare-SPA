package org.example.modelo

import java.time.LocalDateTime

/**
 * Clase base (open class) de pacientes.
 * Los tres tipos de paciente heredan de ella y sobrescriben [calcularMontoBase]
 * para aplicar sus propias reglas de cobro (polimorfismo).
 *
 * Datos comunes a todos los pacientes:
 * - Código de atención (no cambia)
 * - Nombre y especie de la mascota (no cambian)
 * - Fecha y hora exacta de ingreso (no cambia)
 * - Tipo de dueño (no cambia)
 */
open class Paciente(
    val codigoAtencion: String,
    val nombre: String,
    val especie: String,
    val fechaIngreso: LocalDateTime,
    val tipoDueno: TipoDueno
) {
    /**
     * Calcula el monto base según el tiempo de uso y las reglas del tipo de paciente.
     * Cada subclase sobrescribe este método (polimorfismo).
     *
     * @param minutos tiempo de atención en minutos
     * @return monto base en pesos (antes de IVA y descuento municipal)
     */
    open fun calcularMontoBase(minutos: Double): Double = 0.0

    /** Nombre del tipo de paciente, para reportes. */
    open val tipoPaciente: String = "Paciente"

    /** Descripción detallada del paciente para pantalla. */
    open fun detalle(): String =
        "$tipoPaciente | $codigoAtencion | $nombre - $especie | Dueño: ${tipoDueno.descripcion} | Ingreso: $fechaIngreso"

    override fun toString(): String = detalle()
}

/**
 * Canino: tarifa base $12.000/hr.
 * Si el dueño tiene convenio se aplica 20% de descuento sobre la tarifa del tiempo.
 */
class Canino(
    codigoAtencion: String,
    nombre: String,
    especie: String,
    fechaIngreso: LocalDateTime,
    tipoDueno: TipoDueno
) : Paciente(codigoAtencion, nombre, especie, fechaIngreso, tipoDueno) {

    override val tipoPaciente: String = "Canino"

    override fun calcularMontoBase(minutos: Double): Double {
        // Costo por tiempo: tarifa * (minutos / 60)
        val bruto = TARIFA_BASE * (minutos / 60.0)
        // Descuento del 20% si el dueño tiene convenio
        return if (tipoDueno == TipoDueno.CONVENIO) bruto * (1 - DESCUENTO_CONVENIO) else bruto
    }

    override fun detalle(): String = super.detalle()

    companion object {
        const val TARIFA_BASE = 12_000.0
        const val DESCUENTO_CONVENIO = 0.20
    }
}

/**
 * Felino: tarifa base $9.000/hr.
 * Si el tiempo de atención es inferior a 20 minutos, el cobro es $0
 * independientemente del tipo de dueño.
 */
class Felino(
    codigoAtencion: String,
    nombre: String,
    especie: String,
    fechaIngreso: LocalDateTime,
    tipoDueno: TipoDueno
) : Paciente(codigoAtencion, nombre, especie, fechaIngreso, tipoDueno) {

    override val tipoPaciente: String = "Felino"

    override fun calcularMontoBase(minutos: Double): Double {
        // Atención corta: exenta de cobro
        if (minutos < MINUTOS_EXENTO) return 0.0
        return TARIFA_BASE * (minutos / 60.0)
    }

    companion object {
        const val TARIFA_BASE = 9_000.0
        const val MINUTOS_EXENTO = 20.0
    }
}

/**
 * Exótico: tarifa base $20.000/hr.
 * Si el animal es silvestre se añade un recargo del 30%.
 * El detalle en pantalla indica si es o no silvestre.
 */
class Exotico(
    codigoAtencion: String,
    nombre: String,
    especie: String,
    fechaIngreso: LocalDateTime,
    tipoDueno: TipoDueno,
    val silvestre: Boolean
) : Paciente(codigoAtencion, nombre, especie, fechaIngreso, tipoDueno) {

    override val tipoPaciente: String = "Exótico"

    override fun calcularMontoBase(minutos: Double): Double {
        val bruto = TARIFA_BASE * (minutos / 60.0)
        // Recargo del 30% si es animal silvestre
        return if (silvestre) bruto * (1 + RECARGO_SILVESTRE) else bruto
    }

    override fun detalle(): String =
        super.detalle() + " | Silvestre: ${if (silvestre) "Sí" else "No"}"

    companion object {
        const val TARIFA_BASE = 20_000.0
        const val RECARGO_SILVESTRE = 0.30
    }
}
