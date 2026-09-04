package com.generative.midi.sequencer.midi

import android.media.midi.MidiDevice
import android.media.midi.MidiDeviceInfo
import android.media.midi.MidiInputPort
import android.media.midi.MidiManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.io.IOException

/**
 * Gestión del envío MIDI por USB.
 *
 * [MidiOutput] es responsable de:
 *  - abrir el puerto de entrada de un dispositivo MIDI;
 *  - enviar mensajes MIDI (Note On/Off, Clock, Start, Stop, Control Change) como
 *    paquetes binary;
 *  - cerrar los puertos y liberar recursos correctamente, evitando notas colgadas.
 *
 * Utiliza la API nativa de Android (android.media.midi).
 *
 * Nota de API: en las versiones modernas de Android, `MidiOutputPort` (puerto de
 * salida del dispositivo) ya no expone `send(...)`. Para que la app ENVÍE datos al
 * dispositivo (p. ej. notas al Keystep Pro), se debe abrir su **puerto de entrada**
 * con [MidiDevice.openInputPort] y usar [MidiInputPort.send], que es un [MidiReceiver].
 *
 * Nota sobre permisos: a partir de la API de Android moderna, el sistema gestiona
 * automáticamente el acceso a los dispositivos MIDI USB, por lo que no es necesario
 * llamar a `requestDevicePermission` (API deprecada/eliminada). Basta con abrir el
 * dispositivo mediante [MidiManager.openDevice].
 */
class MidiOutput(
    private val midiManager: MidiManager
) {

    companion object {
        private const val TAG = "MidiOutput"

        // Códigos del estándar MIDI.
        const val STATUS_NOTE_OFF = 0x80
        const val STATUS_NOTE_ON = 0x90
        const val STATUS_CC = 0xB0
        const val STATUS_CLOCK = 0xF8
        const val STATUS_START = 0xFA
        const val STATUS_STOP = 0xFC
    }

    /** Puerto de entrada MIDI actualmente abierto (por el que la app envía). */
    private var inputPort: MidiInputPort? = null

    private var openCallback: ((Boolean, String) -> Unit)? = null

    /** Verdadero si existe un puerto abierto para enviar. */
    val isConnected: Boolean get() = inputPort != null

    /**
     * Abre el dispositivo y conecta su puerto de entrada para poder enviar MIDI.
     *
     * @return true si el dispositivo tenía un puerto de entrada válido y se inició
     *         la apertura (la conexión real llega por el [openCallback]).
     */
    fun open(device: MidiDeviceInfo): Boolean {
        val portNumber = getInputPortNumber(device)
        val name = deviceName(device)

        if (portNumber == -1) {
            val portTypes = device.ports.joinToString { p ->
                when (p.type) {
                    MidiDeviceInfo.PortInfo.TYPE_INPUT -> "IN"
                    MidiDeviceInfo.PortInfo.TYPE_OUTPUT -> "OUT"
                    else -> "?"
                } + "#" + p.portNumber
            }
            Log.w(TAG, "El dispositivo no posee puertos de entrada; puertos: [$portTypes] : $name")
            openCallback?.invoke(false, "Solo puertos: [$portTypes]")
            return false
        }

        midiManager.openDevice(device, object : MidiManager.OnDeviceOpenedListener {
            override fun onDeviceOpened(device: MidiDevice?) {
                if (device == null) {
                    Log.e(TAG, "No se pudo abrir el dispositivo MIDI")
                    openCallback?.invoke(false, "Failed to open device")
                    return
                }
                val port = device.openInputPort(portNumber)
                if (port == null) {
                    Log.e(TAG, "No se pudo abrir el puerto de entrada")
                    openCallback?.invoke(false, "Failed to open input port")
                    return
                }
                closePort()
                inputPort = port
                openCallback?.invoke(true, name)
            }
        }, Handler(Looper.getMainLooper()))

        return true
    }

    /** Obtiene el primer número de puerto de ENTRADA del dispositivo. */
    private fun getInputPortNumber(device: MidiDeviceInfo): Int {
        val ports = device.ports
        for (p in ports) {
            if (p.type == MidiDeviceInfo.PortInfo.TYPE_INPUT) {
                return p.portNumber
            }
        }
        return -1
    }

    private fun deviceName(device: MidiDeviceInfo): String =
        device.properties.getString(MidiDeviceInfo.PROPERTY_NAME) ?: "MIDI Device"

    /** Establece el callback de resultado al intentar abrir conexión. */
    fun setOpenCallback(cb: (Boolean, String) -> Unit) {
        openCallback = cb
    }

    /**
     * Envía un mensaje de 2 bytes (Note On/Off, CC, etc.) en el canal seleccionado.
     * Los canales MIDI van de 1 a 16 y se codifican como 0..15.
     */
    fun sendChannelMessage(status: Int, channel: Int, data1: Int, data2: Int) {
        val port = inputPort ?: return
        val ch = (channel - 1).coerceIn(0, 15)
        val cmd = status or ch
        sendRaw(port, byteArrayOf(cmd.toByte(), data1.toByte(), data2.toByte()))
    }

    /** Envía un mensaje de sistema (sin canal), p. ej. MIDI Clock Start/Stop. */
    fun sendSystemMessage(status: Int) {
        val port = inputPort ?: return
        sendRaw(port, byteArrayOf(status.toByte()))
    }

    /**
     * Envía All Notes Off (CC 123) y All Sound Off (CC 120) en el canal dado.
     * Útil como pánico en performance en vivo.
     */
    fun sendAllNotesOffCc(channel: Int) {
        val port = inputPort ?: return
        val ch = (channel - 1).coerceIn(0, 15)
        val cmd = STATUS_CC or ch
        sendRaw(port, byteArrayOf(cmd.toByte(), 123, 0))
        sendRaw(port, byteArrayOf(cmd.toByte(), 120, 0))
    }

    /** Envía un raw buffer MIDI a través del puerto dado. */
    private fun sendRaw(port: MidiInputPort, data: ByteArray) {
        try {
            port.send(data, 0, data.size)
        } catch (e: IOException) {
            Log.e(TAG, "Error enviando MIDI", e)
        }
    }

    /** Cierra y libera el puerto actual. */
    fun close() {
        closePort()
    }

    private fun closePort() {
        try {
            inputPort?.close()
        } catch (_: IOException) {
            // ignorar
        }
        inputPort = null
    }
}
