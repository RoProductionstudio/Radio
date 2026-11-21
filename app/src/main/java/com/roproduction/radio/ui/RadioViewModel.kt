package com.roproduction.radio.ui

import android.app.Application
import android.content.ComponentName
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.roproduction.radio.data.DefaultStations
import com.roproduction.radio.data.RadioBrowser
import com.roproduction.radio.data.Station
import com.roproduction.radio.data.StationRepository
import com.roproduction.radio.playback.PlaybackService
import com.roproduction.radio.playback.SleepTimer
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.UUID

class RadioViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = StationRepository(app)

    // ---- Состояние для UI ----
    var stations by mutableStateOf(loadStations())
        private set
    val favoriteIds = mutableStateListOf<String>().apply { addAll(repo.favorites()) }

    var current by mutableStateOf<Station?>(null)
        private set
    var isPlaying by mutableStateOf(false)
        private set
    var isBuffering by mutableStateOf(false)
        private set
    var nowPlaying by mutableStateOf<String?>(null)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    var searchResults by mutableStateOf<List<Station>>(emptyList())
        private set
    var searchLoading by mutableStateOf(false)
        private set
    var searchError by mutableStateOf<String?>(null)
        private set

    val sleepRemainingMs = SleepTimer.remainingMs
    var lastTimerMinutes by mutableStateOf(repo.lastTimerMinutes)
        private set

    // ---- Плеер ----
    private var controller: MediaController? = null
    private val controllerFuture: ListenableFuture<MediaController>
    private var playJob: Job? = null
    /** Ссылки, которые уже пробовали для текущей станции. */
    private val triedUrls = mutableSetOf<String>()
    private var fallbackUrls = ArrayDeque<String>()
    private var fallbackLoaded = false

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(playing: Boolean) {
            isPlaying = playing
            if (playing) error = null
        }

        override fun onPlaybackStateChanged(state: Int) {
            isBuffering = state == Player.STATE_BUFFERING
        }

        override fun onMediaMetadataChanged(metadata: MediaMetadata) {
            // В потоке ICY приходит название песни / передачи.
            val t = metadata.title?.toString()
            nowPlaying = t?.takeIf { it.isNotBlank() && it != current?.name }
        }

        override fun onPlayerError(e: PlaybackException) {
            tryNextUrl()
        }
    }

    init {
        val token = SessionToken(app, ComponentName(app, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(app, token).buildAsync()
        controllerFuture.addListener({
            val c = runCatching { controllerFuture.get() }.getOrNull() ?: return@addListener
            controller = c
            c.addListener(listener)
            syncFromController(c)
        }, ContextCompat.getMainExecutor(app))

        // Показываем последнюю станцию в мини-плеере, чтобы её можно было включить одной кнопкой.
        current = repo.lastStationId?.let { id -> stations.firstOrNull { it.id == id } }
    }

    private fun syncFromController(c: MediaController) {
        val id = c.currentMediaItem?.mediaId
        if (id != null) {
            current = stations.firstOrNull { it.id == id } ?: current
        }
        isPlaying = c.isPlaying
        isBuffering = c.playbackState == Player.STATE_BUFFERING
    }

    private fun loadStations(): List<Station> {
        val overrides = repo.urlOverrides()
        return (DefaultStations.all + repo.customStations()).map { s ->
            overrides[s.id]?.let { s.copy(streamUrl = it) } ?: s
        }
    }

    private fun reload() {
        stations = loadStations()
        current = current?.let { cur -> stations.firstOrNull { it.id == cur.id } ?: cur }
    }

    fun isFavorite(id: String) = id in favoriteIds

    // ---- Воспроизведение ----

    fun play(station: Station) {
        current = station
        repo.lastStationId = station.id
        error = null
        nowPlaying = null
        triedUrls.clear()
        fallbackUrls.clear()
        fallbackLoaded = false
        playJob?.cancel()
        if (station.streamUrl.isNotBlank()) {
            startUrl(station, station.streamUrl)
        } else {
            isBuffering = true
            tryNextUrl()
        }
    }

    fun togglePlay() {
        val c = controller ?: return
        val st = current ?: return
        when {
            c.isPlaying -> c.pause()
            c.currentMediaItem?.mediaId == st.id && c.playerError == null -> {
                // Для живого эфира возвращаемся к «прямому эфиру», а не к месту паузы.
                c.seekToDefaultPosition()
                c.prepare()
                c.play()
            }
            else -> play(st)
        }
    }

    fun stop() {
        controller?.stop()
        controller?.clearMediaItems()
        isPlaying = false
        isBuffering = false
        nowPlaying = null
    }

    private fun startUrl(station: Station, url: String) {
        val c = controller
        if (c == null) {
            // Контроллер ещё подключается — повторим, когда будет готов.
            controllerFuture.addListener({ if (current?.id == station.id) startUrl(station, url) },
                ContextCompat.getMainExecutor(getApplication()))
            return
        }
        triedUrls += url
        val uri = Uri.parse(url)
        val item = MediaItem.Builder()
            .setMediaId(station.id)
            .setUri(uri)
            .setRequestMetadata(MediaItem.RequestMetadata.Builder().setMediaUri(uri).build())
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(station.name)
                    .setArtist(station.description)
                    .setStation(station.name)
                    .setIsPlayable(true)
                    .build()
            )
            .build()
        c.setMediaItem(item)
        c.prepare()
        c.play()
    }

    /** Поток не открылся: ищем запасной в каталоге и пробуем его. */
    private fun tryNextUrl() {
        val station = current ?: return
        playJob?.cancel()
        playJob = viewModelScope.launch {
            if (!fallbackLoaded) {
                fallbackLoaded = true
                isBuffering = true
                val found = runCatching { RadioBrowser.findStreamUrls(station.searchQueries) }
                    .getOrDefault(emptyList())
                fallbackUrls.addAll(found.filter { it !in triedUrls })
            }
            val next = fallbackUrls.removeFirstOrNull()
            if (next == null || current?.id != station.id) {
                isBuffering = false
                if (current?.id == station.id) {
                    error = if (station.streamUrl.isBlank())
                        "Поток «${station.name}» не найден в каталоге. Найдите станцию во вкладке «Поиск» " +
                            "или укажите ссылку вручную (долгое нажатие на станцию)."
                    else
                        "Не удалось подключиться к «${station.name}». Проверьте интернет или укажите другую ссылку " +
                            "(долгое нажатие на станцию)."
                }
                return@launch
            }
            // Запоминаем найденную ссылку только если она заработает.
            val listenerOnce = object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) {
                    if (playing) {
                        repo.setUrlOverride(station.id, next)
                        controller?.removeListener(this)
                        reload()
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    controller?.removeListener(this)
                }
            }
            controller?.addListener(listenerOnce)
            startUrl(station, next)
        }
    }

    // ---- Избранное ----

    fun toggleFavorite(station: Station) {
        if (station.id in favoriteIds) {
            favoriteIds.remove(station.id)
        } else {
            // Станция из каталога: сохраняем её, чтобы она осталась в списке.
            if (stations.none { it.id == station.id }) {
                repo.saveCustomStations(repo.customStations() + station.copy(isCustom = true))
                reload()
            }
            favoriteIds.add(station.id)
        }
        repo.saveFavorites(favoriteIds.toList())
    }

    fun moveFavorite(id: String, up: Boolean) {
        val i = favoriteIds.indexOf(id)
        val j = if (up) i - 1 else i + 1
        if (i < 0 || j !in favoriteIds.indices) return
        favoriteIds[i] = favoriteIds[j].also { favoriteIds[j] = favoriteIds[i] }
        repo.saveFavorites(favoriteIds.toList())
    }

    // ---- Свои станции и ссылки ----

    fun addCustomStation(name: String, url: String) {
        val st = Station(
            id = "custom:" + UUID.randomUUID(),
            name = name.trim(),
            description = "Своя станция",
            streamUrl = url.trim(),
            searchQueries = listOf(name.trim()),
            isCustom = true,
        )
        repo.saveCustomStations(repo.customStations() + st)
        reload()
        favoriteIds.add(st.id)
        repo.saveFavorites(favoriteIds.toList())
    }

    fun updateStation(station: Station, name: String, url: String) {
        if (station.isCustom) {
            repo.saveCustomStations(repo.customStations().map {
                if (it.id == station.id) it.copy(name = name.trim(), streamUrl = url.trim()) else it
            })
            repo.setUrlOverride(station.id, null)
        } else {
            repo.setUrlOverride(station.id, url.trim())
        }
        reload()
    }

    fun resetStationUrl(station: Station) {
        repo.setUrlOverride(station.id, null)
        reload()
    }

    fun deleteStation(station: Station) {
        if (!station.isCustom) return
        repo.saveCustomStations(repo.customStations().filter { it.id != station.id })
        favoriteIds.remove(station.id)
        repo.saveFavorites(favoriteIds.toList())
        if (current?.id == station.id) stop()
        reload()
    }

    fun defaultUrlOf(station: Station): String? =
        DefaultStations.all.firstOrNull { it.id == station.id }?.streamUrl

    // ---- Каталог ----

    private var searchJob: Job? = null

    fun search(name: String? = null, state: String? = null, tag: String? = null) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            searchLoading = true
            searchError = null
            try {
                searchResults = RadioBrowser.search(name = name, state = state, tag = tag)
                if (searchResults.isEmpty()) searchError = "Ничего не найдено"
            } catch (e: Exception) {
                searchResults = emptyList()
                searchError = "Каталог недоступен. Проверьте интернет."
            } finally {
                searchLoading = false
            }
        }
    }

    // ---- Таймер ----

    fun startTimer(minutes: Int) {
        lastTimerMinutes = minutes
        repo.lastTimerMinutes = minutes
        SleepTimer.start(minutes)
    }

    fun extendTimer(minutes: Int) = SleepTimer.extend(minutes)
    fun cancelTimer() = SleepTimer.cancel()

    fun dismissError() {
        error = null
    }

    override fun onCleared() {
        controller?.removeListener(listener)
        MediaController.releaseFuture(controllerFuture)
        super.onCleared()
    }
}
