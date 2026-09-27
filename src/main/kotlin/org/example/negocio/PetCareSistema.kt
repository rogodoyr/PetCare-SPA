package org.example.negocio

import kotlinx.coroutines.runBlocking
import org.example.asincrono.ResultadoOperacion
import org.example.asincrono.ServicioSensores
import org.example.modelo.*
import java.time.LocalDateTime

/**
 * Sistema principal de gestión de boxes veterinarios PetCare.
 * Capacidad: 10 boxes. Registra recaudación total y por tipo de paciente.
 *
 * Coordina:
 * - Alta de pacientes (entrada) y cobro (salida)
 * - Estados de los boxes
 * - Historial del turno
 * - Operaciones asíncronas con sensores
 */
class PetCareSistema(
    val cantidadBoxes: Int = 10,
    private val sensores: ServicioSensores = ServicioSensores()
) {

    /** Boxes del sistema, numerados de 1 a [cantidadBoxes]. */
    private val boxes: List<Box> = (1..cantidadBoxes).map { Box(it) }

    /** Historial de pacientes atendidos en el turno (tickets + paciente). */
    val historial: MutableList<AtencionCompletada> = mutableListOf()

    /** Secuencia de números de ticket. */
    private var siguienteTicket = 1

    /** Recaudación total del turno. */
    var recaudacionTotal: Double = 0.0
        private set

    /** Recaudación por tipo de paciente. */
    val recaudacionPorTipo: MutableMap<String, Double> = mutableMapOf()

    // ------------------------------------------------------------------
    // Operaciones de entrada y salida (asíncronas con sensores)
    // ------------------------------------------------------------------

    /**
     * Registra la entrada de un paciente:
     * 1. Busca el primer box libre.
     * 2. Deja el box en EnProceso ("Registrando entrada") mientras espera al sensor.
     * 3. Tras la confirmación (delay), el box pasa a EnAtencion.
     */
    fun registrarEntrada(paciente: Paciente): ResultadoOperacion {
        // Validación de código de atención
        if (!ValidadorDatos.codigoValido(paciente.codigoAtencion)) {
            return ResultadoOperacion.Error(
                "Código de atención inválido: '${paciente.codigoAtencion}' no cumple el formato LL DD LL (ej. CA12CD). Registro cancelado."
            )
        }

        // El código es único: no puede repetirse en boxes activos ni en el historial del turno
        if (codigoYaRegistrado(paciente.codigoAtencion)) {
            return ResultadoOperacion.Error(
                "Código de atención duplicado: '${paciente.codigoAtencion}' ya está registrado en el turno. Registro cancelado."
            )
        }

        // Búsqueda del primer box libre
        val boxLibre = boxes.firstOrNull { it.estaLibre() }
            ?: return ResultadoOperacion.Error(
                "Sistema sin capacidad: no hay boxes libres disponibles. No se realizó el registro."
            )

        // Mientras espera al sensor, el box queda en EnProceso con el motivo
        boxLibre.cambiarEstado(EstadoBox.EnProceso("Registrando entrada de ${paciente.codigoAtencion}"))

        // Operación asíncrona: espera de confirmación del sensor (3 segundos)
        val confirmado = runBlocking { sensores.confirmarEntrada() }

        return if (confirmado) {
            boxLibre.cambiarEstado(EstadoBox.EnAtencion(paciente))
            ResultadoOperacion.EntradaExitosa(boxLibre.numero, paciente)
        } else {
            // Si el sensor no confirma, se libera el box
            boxLibre.cambiarEstado(EstadoBox.Libre)
            ResultadoOperacion.Error("El sensor no confirmó la entrada de ${paciente.codigoAtencion}.")
        }
    }

    /**
     * Registra la salida de un paciente por su código de atención:
     * 1. Localiza el box que tiene al paciente.
     * 2. Deja el box en EnProceso ("Calculando tarifa") mientras se procesa.
     * 3. Tras la espera (6,5 s), emite ticket, actualiza recaudación y libera el box.
     */
    fun registrarSalida(codigoAtencion: String, minutosUso: Double): ResultadoOperacion {
        // Validación de formato de código
        if (!ValidadorDatos.codigoValido(codigoAtencion)) {
            return ResultadoOperacion.Error(
                "Código de atención inválido: '$codigoAtencion' no cumple el formato LL DD LL (ej. CA12CD)."
            )
        }

        // Localizar el box del paciente
        val boxConPaciente = boxes.firstOrNull { box ->
            val estado = box.estado
            estado is EstadoBox.EnAtencion && estado.paciente.codigoAtencion.equals(codigoAtencion.trim(), ignoreCase = true)
        } ?: return ResultadoOperacion.Error(
            "Paciente no encontrado: no hay ningún box atendiendo el código '$codigoAtencion'. Operación cancelada."
        )

        val estado = boxConPaciente.estado
        val paciente = (estado as EstadoBox.EnAtencion).paciente

        // Proceso de salida: calcular tarifa
        boxConPaciente.cambiarEstado(EstadoBox.EnProceso("Calculando tarifa de ${paciente.codigoAtencion}"))

        // Operación asíncrona: espera de 6,5 segundos
        val confirmado = runBlocking { sensores.confirmarSalida() }

        if (!confirmado) {
            boxConPaciente.cambiarEstado(EstadoBox.EnAtencion(paciente))
            return ResultadoOperacion.Error("El sensor no confirmó la salida de ${paciente.codigoAtencion}.")
        }

        // Cálculo del monto con validación de tarifa inválida
        val monto = try {
            CalculadoraTarifas.calcularMontoFinal(paciente, minutosUso)
        } catch (e: IllegalArgumentException) {
            // Se retoma el estado de atención ante error de tarifa
            boxConPaciente.cambiarEstado(EstadoBox.EnAtencion(paciente))
            return ResultadoOperacion.Error(e.message ?: "Resultado de tarifa inválido.")
        }

        // Emisión del ticket
        val ticket = Ticket(
            numero = siguienteTicket++,
            tipoPaciente = paciente.tipoPaciente,
            codigoAtencion = paciente.codigoAtencion,
            tiempoUsoMinutos = minutosUso,
            montoPagado = monto
        )

        // Actualización de recaudación total y por tipo
        recaudacionTotal += monto
        recaudacionPorTipo.merge(paciente.tipoPaciente, monto) { a, b -> a + b }

        // Se agrega al historial del turno
        historial.add(AtencionCompletada(paciente, ticket, minutosUso))

        // Se libera el box
        boxConPaciente.cambiarEstado(EstadoBox.Libre)

        return ResultadoOperacion.SalidaExitosa(ticket)
    }

    // ------------------------------------------------------------------
    // Gestión de estados de boxes
    // ------------------------------------------------------------------

    /** Marca un box como fuera de servicio, registrando el motivo. */
    fun ponerBoxFueraDeServicio(numeroBox: Int, motivo: String): Boolean {
        val box = boxes.firstOrNull { it.numero == numeroBox } ?: return false
        return box.cambiarEstado(EstadoBox.FueraDeServicio(motivo))
    }

    /** Reactiva un box que estaba fuera de servicio. */
    fun reactivarBox(numeroBox: Int): Boolean {
        val box = boxes.firstOrNull { it.numero == numeroBox } ?: return false
        return box.cambiarEstado(EstadoBox.Libre)
    }

    /** Lista de boxes con su estado actual. */
    fun listarBoxes(): List<Box> = boxes.toList()

    // ------------------------------------------------------------------
    // Consultas de negocio (funciones de orden superior sobre List)
    // ------------------------------------------------------------------

    /** ¿Cuántos boxes están disponibles en este momento? */
    fun boxesDisponibles(): Int = boxes.count { it.estaLibre() }

    /** Indica si queda al menos un box libre. */
    fun hayBoxesLibres(): Boolean = boxes.any { it.estaLibre() }

    /**
     * Indica si un código de atención ya fue registrado en el turno
     * (en un box activo o en el historial).
     */
    fun codigoYaRegistrado(codigo: String): Boolean {
        val cod = codigo.trim()
        val enBoxActivo = boxes.any { box ->
            val estado = box.estado
            estado is EstadoBox.EnAtencion && estado.paciente.codigoAtencion.equals(cod, ignoreCase = true)
        }
        val enHistorial = historial.any {
            it.paciente.codigoAtencion.equals(cod, ignoreCase = true)
        }
        return enBoxActivo || enHistorial
    }

    /** Pacientes del historial del turno que pertenecen a clientes convenio. */
    fun pacientesConvenio(): List<Paciente> =
        historial.map { it.paciente }.filter { it.tipoDueno == TipoDueno.CONVENIO }

    /** Ingreso promedio por paciente atendido en el turno. */
    fun ingresoPromedio(): Double =
        if (historial.isEmpty()) 0.0
        else historial.sumOf { it.ticket.montoPagado } / historial.size

    /** Códigos de todos los pacientes que han finalizado durante el turno. */
    fun codigosFinalizados(): List<String> =
        historial.map { it.ticket.codigoAtencion }

    /** Paciente que tuvo más tiempo de uso durante el turno. */
    fun pacienteMayorUso(): AtencionCompletada? =
        historial.maxByOrNull { it.minutosUso }

    /** Tipo de paciente que más ingresos generó en el turno. */
    fun tipoMayorIngreso(): String? =
        recaudacionPorTipo.entries.maxByOrNull { it.value }?.key

    /** Cantidad total de pacientes atendidos en el turno. */
    fun cantidadPacientesAtendidos(): Int = historial.size

    /** Estado actual de un box por número. */
    fun estadoBox(numero: Int): EstadoBox? =
        boxes.firstOrNull { it.numero == numero }?.estado
}

/**
 * Registro de una atención finalizada en el turno.
 */
data class AtencionCompletada(
    val paciente: Paciente,
    val ticket: Ticket,
    val minutosUso: Double
)
