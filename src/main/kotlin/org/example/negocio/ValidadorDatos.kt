package org.example.negocio

import org.example.modelo.TipoDueno

/**
 * Validaciones de datos de entrada del sistema.
 * Un dato inválido no registra al paciente y genera un error controlado.
 */
object ValidadorDatos {

    /**
     * Formato del código de atención: dos letras, dos dígitos, dos letras.
     * Ejemplo válido: CA12CD. Ejemplo inválido: 123ABC.
     */
    private val FORMATO_CODIGO = Regex("^[A-Za-z]{2}\\d{2}[A-Za-z]{2}$")

    /** Indica si el código de atención cumple el formato del sistema. */
    fun codigoValido(codigo: String): Boolean =
        codigo.isNotBlank() && FORMATO_CODIGO.matches(codigo.trim())

    /** Valida el tipo de dueño. Devuelve null si no es uno de los tres valores permitidos. */
    fun validarTipoDueno(texto: String): TipoDueno? = TipoDueno.desdeTexto(texto)

    /**
     * Interpreta una respuesta sí/no de forma estricta.
     * Solo acepta: "si", "sí", "s" (true) y "no", "n" (false).
     */
    fun interpretarSiNo(texto: String): Boolean? {
        return when (texto.trim().lowercase()) {
            "si", "sí", "s" -> true
            "no", "n" -> false
            else -> null
        }
    }
}
