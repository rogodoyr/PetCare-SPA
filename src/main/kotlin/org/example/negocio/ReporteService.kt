package org.example.negocio

import org.example.modelo.EstadoBox

/**
 * Generación de reportes y resúmenes de pantalla del sistema PetCare.
 * Separa la presentación de los datos de la lógica de negocio.
 */
object ReporteService {

    /** Reporte de cierre de turno completo. */
    fun reporteCierreTurno(sistema: PetCareSistema) {
        println("\n" + "=".repeat(60))
        println("           REPORTE DE CIERRE DE TURNO — PetCare")
        println("=".repeat(60))

        if (sistema.historial.isEmpty()) {
            println("No hubo pacientes atendidos en este turno.")
        } else {
            println("\nResumen por paciente atendido:")
            println("-".repeat(60))
            for (atencion in sistema.historial) {
                val t = atencion.ticket
                println(
                    "Ticket #${t.numero} | ${t.tipoPaciente} | ${t.codigoAtencion} | " +
                        "Tiempo: ${formatoMinutos(t.tiempoUsoMinutos)} | Pagado: $${formatoMonto(t.montoPagado)}"
                )
            }
        }

        println("\n" + "-".repeat(60))
        println("Total recaudado:            $${formatoMonto(sistema.recaudacionTotal)}")
        println("Cantidad de pacientes:      ${sistema.cantidadPacientesAtendidos()}")
        println("Ingreso promedio:           $${formatoMonto(sistema.ingresoPromedio())}")
        println("Tipo con mayor ingreso:     ${sistema.tipoMayorIngreso() ?: "N/A"}")
        println("Boxes disponibles al cierre: ${sistema.boxesDisponibles()} / ${sistema.cantidadBoxes}")
        println("=".repeat(60))
    }

    /** Listado de boxes y su estado. */
    fun mostrarBoxes(sistema: PetCareSistema) {
        println("\n--- Estado de los boxes ---")
        for (box in sistema.listarBoxes()) {
            val detalle = when (val estado = box.estado) {
                is EstadoBox.Libre -> "Disponible"
                is EstadoBox.EnAtencion -> "Paciente: ${estado.paciente.codigoAtencion} (${estado.paciente.nombre})"
                is EstadoBox.EnProceso -> "Motivo: ${estado.motivo}"
                is EstadoBox.FueraDeServicio -> "Motivo: ${estado.motivo}"
            }
            println("Box #${box.numero}: ${box.estado.nombre} — $detalle")
        }
    }

    /** Consultas de negocio del turno. */
    fun mostrarConsultas(sistema: PetCareSistema) {
        println("\n" + "=".repeat(60))
        println("              CONSULTAS DE NEGOCIO — Turno actual")
        println("=".repeat(60))

        println("\n1) Boxes disponibles en este momento: ${sistema.boxesDisponibles()}")

        val convenio = sistema.pacientesConvenio()
        println("\n2) Pacientes del historial con cliente convenio (${convenio.size}):")
        if (convenio.isEmpty()) {
            println("   (ninguno)")
        } else {
            for (p in convenio) println("   - ${p.codigoAtencion}: ${p.nombre} (${p.especie})")
        }

        println("\n3) Ingreso promedio por paciente atendido: $${formatoMonto(sistema.ingresoPromedio())}")

        val codigos = sistema.codigosFinalizados()
        println("\n4) Códigos de pacientes finalizados (${codigos.size}):")
        if (codigos.isEmpty()) {
            println("   (ninguno)")
        } else {
            println("   " + codigos.joinToString(", "))
        }

        val mayor = sistema.pacienteMayorUso()
        println("\n5) Paciente con más tiempo de uso:")
        if (mayor == null) {
            println("   (ninguno)")
        } else {
            println(
                "   ${mayor.paciente.codigoAtencion}: ${mayor.paciente.nombre} — " +
                    "${formatoMinutos(mayor.minutosUso)}"
            )
        }
    }

    /** Formato de minutos a "X h Y min". */
    fun formatoMinutos(minutos: Double): String {
        val h = (minutos / 60).toInt()
        val m = (minutos % 60).toInt()
        return if (h > 0) "${h}h ${m}min" else "${m}min"
    }

    /** Formato de moneda sin decimales cuando es entero. */
    fun formatoMonto(monto: Double): String {
        val entero = monto.toLong()
        return if (monto == entero.toDouble()) "%,d".format(entero) else "%,.2f".format(monto)
    }
}
