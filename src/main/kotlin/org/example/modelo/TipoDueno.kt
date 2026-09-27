package org.example.modelo

/**
 * Tipos de dueño válidos en el sistema PetCare.
 * Cualquier otro valor se considera inválido.
 */
enum class TipoDueno(val descripcion: String) {
    PARTICULAR("Particular"),
    CONVENIO("Convenio"),
    MUNICIPAL("Municipal");

    companion object {
        /** Convierte texto de entrada a TipoDueno. Devuelve null si el valor no es válido. */
        fun desdeTexto(texto: String): TipoDueno? =
            when (texto.trim().lowercase()) {
                "particular" -> PARTICULAR
                "convenio" -> CONVENIO
                "municipal" -> MUNICIPAL
                else -> null
            }
    }
}
