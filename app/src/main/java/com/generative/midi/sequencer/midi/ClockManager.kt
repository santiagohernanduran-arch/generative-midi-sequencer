package com.generative.midi.sequencer.midi

import android.os.Handler
import android.os.Looper
import android.os.SystemClock

/**
 * Generador de ticks de reloj de alta precisión.
 *
 * En lugar de depender de temporizadores imprecisos de la UI, este reloj calcula
 * el intervalo entre ticks en milisegundos a partir del BPM y usa [SystemClock.elapsedRealtime]
 * para mantener un timing lo más estable posible (esquema "double-buffer" / nextTick).
 *
 * El BPM se puede cambiar en cualquier momento, incluso durante la reproducción;
 * el intervalo se recalcula en el siguiente tick.
 */
class ClockManager {

    companion object {
        /** Número de ticks por negra (PPQ de la app). Con 96 ticks por negra, cada paso "1/16" ocupa 24 ticks. */
        const val TICKS_PER_BEAT = 96
    }

    private val handler = Handler(Looper.getMainLooper())

    /** Callback de tick. Recibe el tick actual (incrementado desde el arranque). */
    var onTick: ((tick: Long) -> Unit)? = null

    /** BPM actual (30..180). */
    var bpm: Int = 62
        set(value) {
            field = value.coerceIn(30, 180)
        }

    private var running = false
    private var nextTickTime = 0L
    private var tickCount = 0L

    /** Intervalo entre ticks en ms en función del BPM. */
    private fun computeTickIntervalMs(): Long {
        // Un cuarto de nota a BPM = 60000/bpm ms. Dividido por ticks por negra.
        return (60000L / bpm.toLong()) / TICKS_PER_BEAT
    }

    /** Inicia el reloj. Señal de start externo (primero envío de START se hace en el transporte). */
    fun start() {
        if (running) return
        running = true
        tickCount = 0
        nextTickTime = SystemClock.elapsedRealtime()
        scheduleNextTick()
    }

    /** Detiene el reloj. */
    fun stop() {
        running = false
        handler.removeCallbacksAndMessages(null)
    }

    private fun scheduleNextTick() {
        if (!running) return
        handler.postDelayed(this::tick, nextTickTime - SystemClock.elapsedRealtime())
    }

    private fun tick() {
        if (!running) return
        onTick?.invoke(tickCount)

        // Programar el siguiente tick manteniendo la base temporal.
        nextTickTime += computeTickIntervalMs()
        tickCount++

        // Si nos retrasamos, saltamos hacia adelante para volver a la base.
        if (nextTickTime < SystemClock.elapsedRealtime()) {
            nextTickTime = SystemClock.elapsedRealtime() + computeTickIntervalMs()
        }
        scheduleNextTick()
    }
}
