package org.example.modelo

/**
 * Clase sellada que modela los cuatro estados posibles de un box.
 * Se gestiona con expresiones `when` exhaustivas en el resto del sistema.
 */
sealed class EstadoBox {
    /** El box está disponible y puede recibir un paciente. */
    data object Libre : EstadoBox()

    /** El box tiene un paciente asignado en atención. */
    data class EnAtencion(val paciente: Paciente) : EstadoBox()

    /** El box está siendo procesado (espera al sensor). Indica el motivo. */
    data class EnProceso(val motivo: String) : EstadoBox()

    /** El box está inhabilitado. Registra el motivo. */
    data class FueraDeServicio(val motivo: String) : EstadoBox()

    /** Nombre legible del estado, útil para reportes y consola. */
    val nombre: String
        get() = when (this) {
            is Libre -> "Libre"
            is EnAtencion -> "En atención"
            is EnProceso -> "En proceso"
            is FueraDeServicio -> "Fuera de servicio"
        }
}
