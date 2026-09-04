package com.generative.midi.sequencer.midi

import android.content.Context
import android.media.midi.MidiDeviceInfo
import android.media.midi.MidiDeviceStatus
import android.os.Handler
import android.os.Looper
import android.util.Log

/**
 * Façade de alto nivel de la capa MIDI.
 *
 * [MidiManager] (esta clase) se encarga de:
 *  - obtener el MidiManager del sistema de Android;
 *  - detectar / listar dispositivos USB MIDI conectados;
 *  - observar los cambios de conexión (conectar/desconectar);
 *  - crear y gestionar la [MidiOutput] y el [ClockManager] y el [TransportController].
 *
 * Separa completamente la lógica MIDI de la interfaz de usuario.
 */
class MidiManager(private val context: Context) {

    companion object {
        private const val TAG = "MidiManager"
    }

    private val midiManager = context.getSystemService(Context.MIDI_SERVICE) as android.media.midi.MidiManager

    /** Salida MIDI gestionada por esta clase. */
    val midiOutput = MidiOutput(midiManager)

    /** Reloj interno. */
    val clock = ClockManager()

    /** Controlador de transporte/sincronización. */
    val transport: TransportController

    /** Nombre del dispositivo actualmente conectado, o null. */
    var connectedDeviceName: String? = null
        private set

    /** Callback de cambio de estado de conexión. */
    var onConnectionChanged: ((Boolean, String) -> Unit)? = null

    init {
        transport = TransportController(midiOutput, clock)
        registerDeviceObserver()
    }

    /** Registra el observador de cambios en los dispositivos presentes. */
    private fun registerDeviceObserver() {
        try {
            midiManager.registerDeviceCallback(object : android.media.midi.MidiManager.DeviceCallback() {
                override fun onDeviceAdded(device: MidiDeviceInfo) {
                    Log.i(TAG, "MIDI device added: ${deviceName(device)}")
                    notifyIfConnected()
                }

                override fun onDeviceRemoved(device: MidiDeviceInfo) {
                    Log.i(TAG, "MIDI device removed: ${deviceName(device)}")
                    // Seguridad: apagar todas las notas al desconectar.
                    transport.shutdown()
                    connectedDeviceName = null
                    onConnectionChanged?.invoke(false, "")
                }

                override fun onDeviceStatusChanged(status: MidiDeviceStatus) {
                    // Un cambio de estado puede implicar una reconexión.
                    notifyIfConnected()
                }
            }, Handler(Looper.getMainLooper()))
        } catch (e: Exception) {
            Log.e(TAG, "Fallo al registrar observer MIDI", e)
        }
    }

    /** Lista los dispositivos MIDI USB conectados. Detecta si poseen puertos de
     *  entrada (para enviar notas) o de salida, indistintamente, para máxima
     *  compatibilidad con equipos que exponen su puerto de una u otra forma. */
    fun listDevices(): List<MidiDeviceInfo> {
        return midiManager.devices.filter { device ->
            device.ports.any {
                it.type == MidiDeviceInfo.PortInfo.TYPE_INPUT ||
                    it.type == MidiDeviceInfo.PortInfo.TYPE_OUTPUT
            }
        }
    }

    /** Verdadero si el dispositivo tiene un puerto de entrada (por el que la app
     *  puede enviar notas al equipo). */
    fun hasInputPort(device: MidiDeviceInfo): Boolean {
        return device.ports.any { it.type == MidiDeviceInfo.PortInfo.TYPE_INPUT }
    }

    /**
     * Conecta al primer dispositivo de salida disponible y notifica el estado.
     * Si no hay dispositivo, notifica "No device".
     */
    fun connectToFirstAvailable(onResult: (Boolean) -> Unit) {
        val devices = listDevices()
        if (devices.isEmpty()) {
            connectedDeviceName = null
            onConnectionChanged?.invoke(false, "")
            onResult(false)
            return
        }
        connectToDevice(devices.first()) { ok ->
            onResult(ok)
            onConnectionChanged?.invoke(ok, connectedDeviceName ?: "")
        }
    }

    /** Conecta a un dispositivo concreto. */
    fun connectToDevice(device: MidiDeviceInfo, onResult: ((Boolean) -> Unit)? = null) {
        midiOutput.setOpenCallback { ok, name ->
            if (ok) {
                connectedDeviceName = name
            } else {
                connectedDeviceName = null
            }
            onResult?.invoke(ok)
            onConnectionChanged?.invoke(ok, connectedDeviceName ?: "")
        }
        midiOutput.open(device)
    }

    /** Refresca los dispositivos conectados. */
    fun refresh() {
        connectToFirstAvailable { }
    }

    private fun notifyIfConnected() {
        if (midiOutput.isConnected) return
        refresh()
    }

    private fun deviceName(device: MidiDeviceInfo): String =
        device.properties.getString(MidiDeviceInfo.PROPERTY_NAME) ?: "MIDI Device"

    /** Libera recursos al finalizar. */
    fun close() {
        midiOutput.setOpenCallback { _, _ -> }
        midiOutput.close()
        transport.shutdown()
    }
}
