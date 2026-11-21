@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.roproduction.radio.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.roproduction.radio.data.Station
import kotlin.math.abs

private enum class Tab(val title: String, val icon: ImageVector) {
    ALL("Станции", Icons.Filled.Radio),
    FAVORITES("Избранное", Icons.Filled.Favorite),
    SEARCH("Поиск", Icons.Filled.Search),
}

@Composable
fun RadioApp(vm: RadioViewModel) {
    var tab by rememberSaveable { mutableStateOf(if (vm.favoriteIds.isEmpty()) Tab.ALL else Tab.FAVORITES) }
    var editing by remember { mutableStateOf<Station?>(null) }
    var adding by remember { mutableStateOf(false) }
    var showTimer by remember { mutableStateOf(false) }
    val sleepRemaining by vm.sleepRemainingMs.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(tab.title, fontWeight = FontWeight.SemiBold) },
                actions = {
                    IconButton(onClick = { showTimer = true }) {
                        Icon(
                            if (sleepRemaining != null) Icons.Filled.Bedtime else Icons.Outlined.Bedtime,
                            contentDescription = "Таймер сна",
                            tint = if (sleepRemaining != null) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )
        },
        bottomBar = {
            Column {
                MiniPlayer(vm, sleepRemaining, onTimerClick = { showTimer = true })
                NavigationBar {
                    Tab.entries.forEach { t ->
                        NavigationBarItem(
                            selected = tab == t,
                            onClick = { tab = t },
                            icon = { Icon(t.icon, contentDescription = null) },
                            label = { Text(t.title) },
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (tab != Tab.SEARCH) {
                FloatingActionButton(onClick = { adding = true }) {
                    Icon(Icons.Filled.Add, contentDescription = "Добавить свою станцию")
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (tab) {
                Tab.ALL -> StationList(
                    vm = vm,
                    stations = vm.stations,
                    onEdit = { editing = it },
                    empty = "Список пуст",
                )
                Tab.FAVORITES -> StationList(
                    vm = vm,
                    stations = vm.favoriteIds.mapNotNull { id -> vm.stations.firstOrNull { it.id == id } },
                    onEdit = { editing = it },
                    empty = "Здесь пока пусто.\nНажмите ♡ рядом со станцией, чтобы добавить её в избранное.",
                    reorder = true,
                )
                Tab.SEARCH -> SearchTab(vm, onEdit = { editing = it })
            }
        }
    }

    if (showTimer) {
        TimerDialog(vm, sleepRemaining, onDismiss = { showTimer = false })
    }
    editing?.let { st ->
        StationDialog(vm, station = st, onDismiss = { editing = null })
    }
    if (adding) {
        StationDialog(vm, station = null, onDismiss = { adding = false })
    }
}

// ---------------------------------------------------------------- Списки

@Composable
private fun StationList(
    vm: RadioViewModel,
    stations: List<Station>,
    onEdit: (Station) -> Unit,
    empty: String,
    reorder: Boolean = false,
) {
    if (stations.isEmpty()) {
        Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Text(empty, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    LazyColumn(contentPadding = PaddingValues(bottom = 88.dp)) {
        items(stations, key = { it.id }) { st ->
            StationRow(
                station = st,
                vm = vm,
                onEdit = { onEdit(st) },
                extraMenu = if (reorder) listOf(
                    "Переместить выше" to { vm.moveFavorite(st.id, up = true) },
                    "Переместить ниже" to { vm.moveFavorite(st.id, up = false) },
                ) else emptyList(),
            )
        }
    }
}

@Composable
private fun StationRow(
    station: Station,
    vm: RadioViewModel,
    onEdit: (() -> Unit)?,
    extraMenu: List<Pair<String, () -> Unit>> = emptyList(),
) {
    val isCurrent = vm.current?.id == station.id
    val fav = vm.isFavorite(station.id)
    var menu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isCurrent) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f) else Color.Transparent)
            .combinedClickable(
                onClick = { if (isCurrent) vm.togglePlay() else vm.play(station) },
                onLongClick = onEdit,
            )
            .padding(start = 16.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StationAvatar(station, playing = isCurrent && vm.isPlaying)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                station.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
            )
            if (station.description.isNotBlank()) {
                Text(
                    station.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        IconButton(onClick = { vm.toggleFavorite(station) }) {
            Icon(
                if (fav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = if (fav) "Убрать из избранного" else "В избранное",
                tint = if (fav) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (onEdit != null || extraMenu.isNotEmpty()) {
            Box {
                IconButton(onClick = { menu = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Ещё")
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    extraMenu.forEach { (title, action) ->
                        DropdownMenuItem(text = { Text(title) }, onClick = { menu = false; action() })
                    }
                    if (onEdit != null) {
                        DropdownMenuItem(text = { Text("Изменить ссылку") }, onClick = { menu = false; onEdit() })
                    }
                }
            }
        }
    }
}

@Composable
private fun StationAvatar(station: Station, playing: Boolean, sizeDp: Int = 44) {
    val palette = listOf(
        Color(0xFFD8343F), Color(0xFF1E6FD9), Color(0xFF2E9D57), Color(0xFFE08A1E),
        Color(0xFF8E44AD), Color(0xFF00897B), Color(0xFF5D6D7E), Color(0xFFC2185B),
    )
    val bg = palette[abs(station.name.hashCode()) % palette.size]
    val initials = station.name.split(' ', '-').filter { it.isNotBlank() }
        .take(2).joinToString("") { it.first().uppercase() }
    Box(
        Modifier.size(sizeDp.dp).clip(CircleShape).background(bg),
        contentAlignment = Alignment.Center,
    ) {
        if (playing) {
            Icon(Icons.Filled.GraphicEq, contentDescription = "Играет", tint = Color.White)
        } else {
            Text(initials, color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

// ---------------------------------------------------------------- Поиск по каталогу

@Composable
private fun SearchTab(vm: RadioViewModel, onEdit: (Station) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val focus = LocalFocusManager.current

    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("Название станции") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                focus.clearFocus()
                vm.search(name = query)
            }),
        )
        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AssistChip(onClick = { vm.search(name = "Томск") }, label = { Text("Томск") })
            AssistChip(onClick = { vm.search(tag = "news") }, label = { Text("Новости") })
            AssistChip(onClick = { vm.search(tag = "talk") }, label = { Text("Разговорное") })
            AssistChip(onClick = { vm.search() }, label = { Text("Популярные") })
            AssistChip(onClick = { vm.search(tag = "retro") }, label = { Text("Ретро") })
            AssistChip(onClick = { vm.search(tag = "rock") }, label = { Text("Рок") })
        }
        Text(
            "Каталог radio-browser.info. Нажмите ♡ — станция сохранится в избранном.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
        HorizontalDivider()
        when {
            vm.searchLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            vm.searchError != null -> Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Text(vm.searchError!!, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            else -> LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
                items(vm.searchResults, key = { it.id }) { st ->
                    // Если станция уже сохранена — показываем сохранённую версию.
                    val saved = vm.stations.firstOrNull { it.id == st.id }
                    StationRow(
                        station = saved ?: st,
                        vm = vm,
                        onEdit = if (saved != null) ({ onEdit(saved) }) else null,
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Мини-плеер

@Composable
private fun MiniPlayer(vm: RadioViewModel, sleepRemaining: Long?, onTimerClick: () -> Unit) {
    val st = vm.current ?: return
    Surface(tonalElevation = 3.dp, shadowElevation = 6.dp) {
        Column {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StationAvatar(st, playing = vm.isPlaying, sizeDp = 48)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        st.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val status = when {
                        vm.isBuffering -> "Подключение…"
                        vm.isPlaying -> vm.nowPlaying ?: "В эфире"
                        else -> "Пауза"
                    }
                    Text(
                        status,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (sleepRemaining != null) {
                    TextButton(onClick = onTimerClick) {
                        Icon(Icons.Filled.Bedtime, contentDescription = "Таймер сна", modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(formatDuration(sleepRemaining))
                    }
                }
                IconButton(onClick = vm::stop) {
                    Icon(Icons.Filled.Stop, contentDescription = "Стоп")
                }
                FilledIconButton(onClick = vm::togglePlay, modifier = Modifier.size(52.dp)) {
                    if (vm.isBuffering) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.5.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Icon(
                            if (vm.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (vm.isPlaying) "Пауза" else "Играть",
                            modifier = Modifier.size(30.dp),
                        )
                    }
                }
            }
            vm.error?.let { msg ->
                Text(
                    msg,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { vm.dismissError() }
                        .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                )
            }
        }
    }
}

// ---------------------------------------------------------------- Таймер сна

@Composable
private fun TimerDialog(vm: RadioViewModel, remaining: Long?, onDismiss: () -> Unit) {
    val options = listOf(5, 10, 15, 20, 30, 45, 60, 90, 120, 180)
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.Bedtime, contentDescription = null) },
        title = { Text("Таймер сна") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (remaining != null) {
                    Text(
                        "Радио выключится через ${formatDuration(remaining)}",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { vm.extendTimer(15) }) { Text("+15 мин") }
                        OutlinedButton(onClick = { vm.cancelTimer(); onDismiss() }) { Text("Отключить") }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("Начать заново:", style = MaterialTheme.typography.labelLarge)
                } else {
                    Text(
                        "Через выбранное время звук плавно стихнет и радио остановится.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                options.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        row.forEach { m ->
                            val label = if (m < 60) "$m мин" else if (m % 60 == 0) "${m / 60} ч" else "${m / 60} ч ${m % 60} мин"
                            val selected = m == vm.lastTimerMinutes
                            if (selected) {
                                FilledTonalButton(
                                    onClick = { vm.startTimer(m); onDismiss() },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 4.dp),
                                ) { Text(label, maxLines = 1) }
                            } else {
                                OutlinedButton(
                                    onClick = { vm.startTimer(m); onDismiss() },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 4.dp),
                                ) { Text(label, maxLines = 1) }
                            }
                        }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Закрыть") } },
    )
}

// ---------------------------------------------------------------- Добавление / изменение станции

@Composable
private fun StationDialog(vm: RadioViewModel, station: Station?, onDismiss: () -> Unit) {
    val isNew = station == null
    var name by rememberSaveable { mutableStateOf(station?.name ?: "") }
    var url by rememberSaveable { mutableStateOf(station?.streamUrl ?: "") }
    val defaultUrl = station?.let { vm.defaultUrlOf(it) }
    val canSave = name.isNotBlank() && (url.startsWith("http://") || url.startsWith("https://"))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isNew) "Своя станция" else station!!.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isNew || station!!.isCustom) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Название") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it.trim() },
                    label = { Text("Ссылка на поток") },
                    placeholder = { Text("https://…") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "Подходят прямые ссылки на MP3/AAC/HLS-поток. Если оставить ссылку пустой, " +
                        "приложение поищет поток в каталоге по названию.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (station != null && !station.isCustom && defaultUrl != null && station.streamUrl != defaultUrl) {
                    TextButton(onClick = { vm.resetStationUrl(station); onDismiss() }) {
                        Text("Вернуть исходную ссылку")
                    }
                }
                if (station != null && station.isCustom) {
                    TextButton(onClick = { vm.deleteStation(station); onDismiss() }) {
                        Text("Удалить станцию", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = canSave || (!isNew && url.isBlank()),
                onClick = {
                    if (station == null) vm.addCustomStation(name, url)
                    else vm.updateStation(station, name, url)
                    onDismiss()
                },
            ) { Text("Сохранить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}

private fun formatDuration(ms: Long): String {
    val total = (ms + 999) / 1000
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
