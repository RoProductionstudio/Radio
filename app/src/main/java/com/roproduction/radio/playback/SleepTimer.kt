package com.roproduction.radio.playback

import android.os.SystemClock
import androidx.media3.common.Player
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Таймер сна: через заданное время плавно убавляет громкость и ставит радио на паузу.
 * Живёт в процессе приложения и продолжает работать, когда экран выключен.
 */
object SleepTimer {
    private const val FADE_MS = 20_000L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var job: Job? = null
    private var player: Player? = null

    private val _remainingMs = MutableStateFlow<Long?>(null)
    /** Сколько осталось до выключения, или null, если таймер не запущен. */
    val remainingMs: StateFlow<Long?> = _remainingMs.asStateFlow()

    internal fun attach(p: Player) {
        player = p
    }

    internal fun detach() {
        cancel()
        player = null
    }

    fun start(minutes: Int) = startMs(minutes * 60_000L)

    private fun startMs(durationMs: Long) {
        job?.cancel()
        player?.volume = 1f
        val endAt = SystemClock.elapsedRealtime() + durationMs
        job = scope.launch {
            while (isActive) {
                val left = endAt - SystemClock.elapsedRealtime()
                if (left <= 0) break
                _remainingMs.value = left
                if (left < FADE_MS) player?.volume = (left.toFloat() / FADE_MS).coerceIn(0f, 1f)
                delay(if (left < FADE_MS) 250 else 1000)
            }
            player?.let {
                it.pause()
                it.volume = 1f
            }
            _remainingMs.value = null
        }
    }

    /** Добавить минуты к работающему таймеру. */
    fun extend(minutes: Int) {
        val left = _remainingMs.value ?: return
        startMs(left + minutes * 60_000L)
    }

    fun cancel() {
        job?.cancel()
        job = null
        player?.volume = 1f
        _remainingMs.value = null
    }
}
