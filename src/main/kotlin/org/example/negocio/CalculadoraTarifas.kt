package org.example.negocio

import org.example.modelo.Felino
import org.example.modelo.Paciente
import org.example.modelo.TipoDueno

/**
 * Calcula el monto total que paga cada dueño al finalizar el uso.
 * Orden obligatorio del cálculo:
 * 1) Costo por tiempo y reglas del tipo de paciente (polimorfismo en [Paciente.calcularMontoBase]).
 * 2) IVA del 19% sobre el resultado anterior.
 * 3) Si el dueño es municipal, 50% de descuento sobre el monto con IVA.
 */
object CalculadoraTarifas {

    /** Porcentaje de IVA aplicado en Chile. */
    const val IVA = 0.19

    /** Descuento del programa social municipal sobre el monto con IVA. */
    const val DESCUENTO_MUNICIPAL = 0.50

    /**
     * Calcula el monto final a cobrar.
     *
     * @throws IllegalArgumentException si el resultado es negativo o cero cuando no corresponde
     */
    fun calcularMontoFinal(paciente: Paciente, minutos: Double): Double {
        // 1) Costo según tiempo de uso y reglas del tipo de paciente
        val costoBase = paciente.calcularMontoBase(minutos)

        // 2) Aplicación del IVA del 19%
        val conIva = costoBase * (1 + IVA)

        // 3) Descuento municipal del 50% sobre el monto con IVA
        val montoFinal = if (paciente.tipoDueno == TipoDueno.MUNICIPAL) {
            conIva * (1 - DESCUENTO_MUNICIPAL)
        } else {
            conIva
        }

        // Validación: ninguna tarifa puede ser negativa ni cero fuera del caso exento
        validarResultadoTarifa(paciente, minutos, montoFinal)
        return montoFinal
    }

    /**
     * Reporta error de datos si la tarifa resultante es inválida.
     * El caso Felino < 20 min produce $0 de forma legítima y no se considera error.
     */
    private fun validarResultadoTarifa(paciente: Paciente, minutos: Double, monto: Double) {
        val esExentoFelino = paciente is Felino && minutos < Felino.MINUTOS_EXENTO
        if (monto < 0.0) {
            throw IllegalArgumentException(
                "Resultado de tarifa inválido: el monto calculado es negativo ($monto)."
            )
        }
        if (monto == 0.0 && !esExentoFelino) {
            throw IllegalArgumentException(
                "Resultado de tarifa inválido: el monto calculado es cero y no corresponde a un caso exento."
            )
        }
    }
}
