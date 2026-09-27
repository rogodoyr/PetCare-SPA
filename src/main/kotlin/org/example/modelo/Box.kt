package org.example.modelo

/**
 * Unidad de atención donde se atiende a una mascota.
 * Cada box tiene un número único y un estado (ver [EstadoBox]).
 */
class Box(val numero: Int) {

    /** Estado actual del box. Solo puede estar en uno de los cuatro estados. */
    var estado: EstadoBox = EstadoBox.Libre
        private set

    /** Cambia el estado del box. Valida la transición según el estado actual. */
    fun cambiarEstado(nuevoEstado: EstadoBox): Boolean {
        // No se puede asignar un paciente a un box ocupado o fuera de servicio
        val estadoActual = estado
        val permitido = when (estadoActual) {
            is EstadoBox.Libre ->
                nuevoEstado is EstadoBox.EnProceso || nuevoEstado is EstadoBox.EnAtencion || nuevoEstado is EstadoBox.FueraDeServicio
            is EstadoBox.EnAtencion ->
                nuevoEstado is EstadoBox.EnProceso || nuevoEstado is EstadoBox.Libre || nuevoEstado is EstadoBox.FueraDeServicio
            is EstadoBox.EnProceso ->
                nuevoEstado is EstadoBox.EnAtencion || nuevoEstado is EstadoBox.Libre || nuevoEstado is EstadoBox.FueraDeServicio
            is EstadoBox.FueraDeServicio ->
                nuevoEstado is EstadoBox.Libre
        }
        if (!permitido) return false
        estado = nuevoEstado
        return true
    }

    /** Indica si el box puede recibir un paciente nuevo. */
    fun estaLibre(): Boolean = estado is EstadoBox.Libre

    override fun toString(): String = "Box #$numero [$estado]"
}
