package org.example.asincrono

import kotlinx.coroutines.delay

/**
 * Simula la comunicación con los sensores físicos del sistema.
 * Las operaciones toman tiempo real (delay) y el sistema no debe bloquearse
 * mientras las procesa: se implementan como `suspend fun`.
 */
class ServicioSensores {

    /**
     * Confirma la entrada con el sensor físico.
     * La espera simula la comunicación y dura 3 segundos.
     */
    suspend fun confirmarEntrada(): Boolean {
        // Simula latencia del sensor de entrada (3 segundos)
        delay(DELAY_ENTREGA_MS)
        return true
    }

    /**
     * Confirma la salida con el sensor físico.
     * La espera dura 6,5 segundos.
     */
    suspend fun confirmarSalida(): Boolean {
        // Simula latencia del sensor de salida (6,5 segundos)
        delay(DELAY_SALIDA_MS)
        return true
    }

    companion object {
        const val DELAY_ENTREGA_MS = 3_000L
        const val DELAY_SALIDA_MS = 6_500L
    }
}
