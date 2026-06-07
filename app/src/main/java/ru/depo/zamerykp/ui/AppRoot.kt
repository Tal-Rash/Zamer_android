package ru.depo.zamerykp.ui

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.compose.foundation.shape.CircleShape
import kotlinx.coroutines.launch
import ru.depo.zamerykp.data.db.LocomotiveEntity
import ru.depo.zamerykp.R
import ru.depo.zamerykp.domain.ArchiveItem
import ru.depo.zamerykp.data.db.WheelPairProfileEntity
import ru.depo.zamerykp.data.db.WheelSideMeasurementEntity
import ru.depo.zamerykp.ui.ImportPayloadKind
import ru.depo.zamerykp.domain.MeasurementExportDto
import ru.depo.zamerykp.domain.VoiceCommand
import ru.depo.zamerykp.domain.WheelSide
import ru.depo.zamerykp.share.ShareManager
import ru.depo.zamerykp.voice.SpeechRecognizerController
import ru.depo.zamerykp.voice.VoiceFeedback
import ru.depo.zamerykp.voice.VoiceForegroundService
import ru.depo.zamerykp.voice.VoiceSpeaker
import ru.depo.zamerykp.voice.VoskModelStore
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

private enum class RootTab(val title: String) {
    LOCOMOTIVES("Локо"),
    ARCHIVE("Архив"),
    NEW_MEASUREMENT("Замер"),
    SYNC("Обмен"),
    SETTINGS("Настройки")
}

private enum class MeasurementLocomotiveStep {
    SERIES,
    LOCOMOTIVE
}

private enum class MeasurementSeriesChoice {
    TEM,
    PE2M
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot(viewModel: AppViewModel) {
    val context = LocalContext.current
    val activity = context as? Activity
    val measurementState by viewModel.measurementState.collectAsState()
    var tab by rememberSaveable { mutableStateOf(RootTab.NEW_MEASUREMENT) }
    var archiveChromeVisible by rememberSaveable { mutableStateOf(true) }
    var showDraftsScreen by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(measurementState.exportShareRequestNonce) {
        if (measurementState.exportShareRequestNonce != 0L && measurementState.exportPreview.isNotBlank()) {
            ShareManager(context).shareJson(
                measurementState.exportFileName.ifBlank { "zamery_kp.json" },
                measurementState.exportPreview
            )
        }
    }
    LaunchedEffect(measurementState.archiveExportShareRequestNonce) {
        if (measurementState.archiveExportShareRequestNonce != 0L && measurementState.archiveExportPreview.isNotBlank()) {
            ShareManager(context).shareJson(
                measurementState.archiveExportFileName.ifBlank { "zamery_kp_archive.json" },
                measurementState.archiveExportPreview
            )
        }
    }
    LaunchedEffect(measurementState.backupShareRequestNonce) {
        if (measurementState.backupShareRequestNonce != 0L && measurementState.backupPreview.isNotBlank()) {
            ShareManager(context).shareBackup(
                measurementState.backupFileName.ifBlank { "zamery_kp_backup.json" },
                measurementState.backupPreview
            )
        }
    }
    DisposableEffect(tab, activity) {
        val previousOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        if (tab == RootTab.NEW_MEASUREMENT) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
        onDispose {
            activity?.requestedOrientation = previousOrientation
        }
    }
    Scaffold(
        modifier = Modifier.statusBarsPadding(),
        topBar = {
            if (tab == RootTab.SETTINGS) {
                LargeTopAppBar(
                    title = {
                        Text(
                            text = tab.title,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (tab != RootTab.ARCHIVE || archiveChromeVisible) {
                NavigationBar {
                    RootTab.entries.forEach { item ->
                        val selected = tab == item
                        NavigationBarItem(
                            selected = selected,
                            onClick = { tab = item },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = androidx.compose.ui.graphics.Color(0xFF7A7A7A)
                            ),
                            icon = {
                                Box(
                                    modifier = Modifier
                                        .width(34.dp)
                                        .height(34.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (item == RootTab.NEW_MEASUREMENT) {
                                        Icon(
                                            painter = painterResource(R.drawable.zamer_icon),
                                            contentDescription = item.title,
                                            modifier = Modifier.fillMaxSize(),
                                            tint = if (selected) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color(0xFF7A7A7A)
                                        )
                                    } else {
                                        Icon(
                                            item.icon(),
                                            contentDescription = item.title,
                                            modifier = Modifier.fillMaxSize(),
                                            tint = if (selected) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color(0xFF7A7A7A)
                                        )
                                    }
                                }
                            },
                            label = null
                        )
                    }
                }
            }
        }
    ) { padding ->
        when (tab) {
            RootTab.LOCOMOTIVES -> LocomotivesScreen(
                viewModel = viewModel,
                padding = padding,
                openMeasurement = { tab = RootTab.NEW_MEASUREMENT },
                openSync = { tab = RootTab.SYNC }
            )
            RootTab.NEW_MEASUREMENT -> MeasurementScreen(viewModel, padding, snackbar)
            RootTab.ARCHIVE -> ArchiveLocomotivesScreen(
                viewModel = viewModel,
                padding = padding,
                chromeVisible = archiveChromeVisible,
                onToggleChrome = { archiveChromeVisible = !archiveChromeVisible }
            )
            RootTab.SYNC -> if (showDraftsScreen) {
                DraftsScreen(
                    viewModel = viewModel,
                    padding = padding,
                    onBack = { showDraftsScreen = false },
                    onOpenDraft = { draftId ->
                        viewModel.openPendingMeasurement(draftId)
                        tab = RootTab.NEW_MEASUREMENT
                        showDraftsScreen = false
                    }
                )
            } else {
                SyncScreen(
                    viewModel = viewModel,
                    padding = padding,
                    onOpenMeasurementTab = {
                        tab = RootTab.NEW_MEASUREMENT
                        showDraftsScreen = false
                    },
                    onOpenDrafts = { showDraftsScreen = true }
                )
            }
            RootTab.SETTINGS -> SettingsScreen(viewModel, padding)
        }
    }
}

private fun RootTab.icon() = when (this) {
    RootTab.LOCOMOTIVES -> Icons.Default.Train
    RootTab.ARCHIVE -> Icons.Default.Archive
    RootTab.NEW_MEASUREMENT -> Icons.Default.Save
    RootTab.SYNC -> Icons.Default.Share
    RootTab.SETTINGS -> Icons.Default.Settings
}

@Composable
private fun LocomotivesScreen(
    viewModel: AppViewModel,
    padding: PaddingValues,
    openMeasurement: () -> Unit,
    openSync: () -> Unit,
) {
    val locomotives by viewModel.locomotives.collectAsState()
    val measurementState by viewModel.measurementState.collectAsState()
    val selectedId = measurementState.selectedLocomotiveId
    val profiles by viewModel.activeProfiles.collectAsState()
    val archive by viewModel.archive.collectAsState()
    val selectedLocomotive = locomotives.firstOrNull { it.id == selectedId }
    val sortedLocomotives = locomotives.sortedBy { it.sortOrder }
    var series by remember { mutableStateOf("") }
    var number by remember { mutableStateOf("") }
    var count by remember { mutableStateOf("8") }
    var comment by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var detailLocomotiveId by remember { mutableStateOf<Long?>(null) }
    var showKpDialog by remember { mutableStateOf(false) }

    val detailLocomotive = sortedLocomotives.firstOrNull { it.id == detailLocomotiveId }
    val detailIndex = sortedLocomotives.indexOfFirst { it.id == detailLocomotiveId }
    val locomotiveArchive = detailLocomotive?.let { locomotive ->
        archive.filter { it.locomotiveTitle == "${locomotive.series} ${locomotive.number}" }
    }.orEmpty()
    val latestArchive = locomotiveArchive.maxByOrNull { it.measurementDate.toIsoDateMillis() ?: Long.MIN_VALUE }
    val repairDates = linkedMapOf(
        "ТО-3" to locomotiveArchive.firstRepairDate("ТО-3"),
        "ТР-1" to locomotiveArchive.firstRepairDate("ТР-1"),
        "ТР-2" to locomotiveArchive.firstRepairDate("ТР-2"),
        "ТР-3" to locomotiveArchive.firstRepairDate("ТР-3"),
        "СР" to locomotiveArchive.firstRepairDate("СР"),
        "КР" to locomotiveArchive.firstRepairDate("КР"),
    )
    BackHandler(enabled = showKpDialog) {
        showKpDialog = false
    }
    BackHandler(enabled = showAddDialog) {
        showAddDialog = false
    }
    BackHandler(enabled = detailLocomotive != null) {
        detailLocomotiveId = null
        viewModel.selectLocomotive(null)
    }
    val hasActiveDraft = detailLocomotive != null &&
        measurementState.activeSessionId != null &&
        measurementState.selectedLocomotiveId == detailLocomotive.id
    val switchDetailLocomotive: (Int) -> Unit = { direction ->
        val current = detailIndex
        val target = current + direction
        if (current >= 0 && target in sortedLocomotives.indices) {
            val next = sortedLocomotives[target]
            detailLocomotiveId = next.id
            viewModel.selectLocomotive(next.id)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (detailLocomotive == null) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
            ) {
                items(sortedLocomotives, key = { it.id }) { locomotive ->
                    val locomotiveArchive = archive.filter { it.locomotiveTitle == "${locomotive.series} ${locomotive.number}" }
                    val latestArchive = locomotiveArchive.maxByOrNull { it.measurementDate.toIsoDateMillis() ?: Long.MIN_VALUE }
                    val attentionState = locomotive.kpAttentionState(latestArchive)
                    // Плашка локомотива в списке
                    Card(
                        shape = MaterialTheme.shapes.medium,
                        colors = when (attentionState) {
                            KpAttentionState.OVERDUE -> CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                            KpAttentionState.SOON -> CardDefaults.cardColors(
                                containerColor = Color(0xFFFFE8C8),
                                contentColor = Color(0xFF5D4037)
                            )
                            KpAttentionState.NONE -> CardDefaults.cardColors()
                        },
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            when (attentionState) {
                                KpAttentionState.OVERDUE -> MaterialTheme.colorScheme.error
                                KpAttentionState.SOON -> Color(0xFFFFB74D)
                                KpAttentionState.NONE -> MaterialTheme.colorScheme.outlineVariant
                            }
                        ),
                        onClick = {
                            detailLocomotiveId = locomotive.id
                            viewModel.selectLocomotive(locomotive.id)
                        }
                    ) {
                        Column(
                            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "${locomotive.series} ${locomotive.number}",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = latestArchive?.measurementDate?.displayDate() ?: "Нет замера",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            OutlinedButton(
                modifier = Modifier.fillMaxWidth().height(46.dp),
                shape = MaterialTheme.shapes.medium,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                onClick = { showAddDialog = true }
            ) {
                // Кнопка добавления локомотива
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Добавить локомотив", maxLines = 1, softWrap = false)
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(detailLocomotiveId) {
                        var totalDrag = 0f
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                when {
                                    totalDrag < -120f -> switchDetailLocomotive(1)
                                    totalDrag > 120f -> switchDetailLocomotive(-1)
                                }
                                totalDrag = 0f
                            },
                            onDragCancel = {
                                totalDrag = 0f
                            },
                            onHorizontalDrag = { _, dragAmount ->
                                totalDrag += dragAmount
                            }
                        )
                    }
            ) {
                LocomotiveDetailScreen(
                    viewModel = viewModel,
                    locomotive = detailLocomotive,
                    profiles = profiles,
                    latestArchive = latestArchive,
                    repairDates = repairDates,
                    hasActiveDraft = hasActiveDraft,
                    currentDraftFilledPairs = measurementState.filledWheelPairs,
                    onBack = {
                        detailLocomotiveId = null
                        viewModel.selectLocomotive(null)
                    },
                    onResumeDraft = {
                        viewModel.selectLocomotive(detailLocomotive.id)
                        openMeasurement()
                    },
                    onOpenWheelPairs = { showKpDialog = true },
                    onStartMeasurement = {
                        viewModel.selectLocomotive(detailLocomotive.id)
                        viewModel.startNewMeasurement()
                        openMeasurement()
                    },
                    onOpenVoiceMeasurement = {
                        viewModel.selectLocomotive(detailLocomotive.id)
                        if (measurementState.activeSessionId == null || measurementState.selectedLocomotiveId != detailLocomotive.id) {
                            viewModel.startNewMeasurement()
                        }
                        openMeasurement()
                    },
                    onDelete = { showDeleteDialog = true }
                )
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            shape = MaterialTheme.shapes.medium,
            title = { Text("Новый локомотив") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    BigTextField(series, { series = it }, "Серия")
                    BigTextField(number, { number = it }, "Номер")
                    BigTextField(count, { count = it.filter(Char::isDigit) }, "Количество КП", KeyboardType.Number)
                    BigTextField(comment, { comment = it }, "Комментарий")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (series.isNotBlank() && number.isNotBlank()) {
                            viewModel.saveLocomotive(
                                series = series,
                                number = number,
                                count = count.toIntOrNull() ?: 8,
                                comment = comment
                            )
                            series = ""
                            number = ""
                            count = "8"
                            comment = ""
                            showAddDialog = false
                        }
                    }
                ) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    if (showDeleteDialog && detailLocomotive != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            shape = MaterialTheme.shapes.medium,
            title = { Text("Удалить локомотив") },
            text = { Text("${detailLocomotive.series} ${detailLocomotive.number}") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteLocomotive(detailLocomotive.id)
                        showDeleteDialog = false
                        detailLocomotiveId = null
                        viewModel.selectLocomotive(null)
                    }
                ) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    if (selectedLocomotive != null && showKpDialog) {
        AlertDialog(
            onDismissRequest = { showKpDialog = false },
            shape = MaterialTheme.shapes.medium,
            title = { Text("${selectedLocomotive.series} ${selectedLocomotive.number}") },
            text = {
                if (profiles.isEmpty()) {
                    Text("КП данные не импортированы. Доступна только нумерация: 1..${selectedLocomotive.wheelPairCount}")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        profiles.forEach { profile ->
                            Card(
                                shape = MaterialTheme.shapes.medium,
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Row(
                                    Modifier.fillMaxWidth().padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("КП ${profile.number}, ось ${profile.axisNumber}")
                                    Text("КЦ: ${profile.kcDiameterLeft ?: "-"} / ${profile.kcDiameterRight ?: "-"}")
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showKpDialog = false }) {
                    Text("Закрыть")
                }
            }
        )
    }
}

@Composable
private fun LocomotiveDetailScreen(
    viewModel: AppViewModel,
    locomotive: LocomotiveEntity,
    profiles: List<WheelPairProfileEntity>,
    latestArchive: ru.depo.zamerykp.domain.ArchiveItem?,
    repairDates: Map<String, String?>,
    hasActiveDraft: Boolean,
    currentDraftFilledPairs: Int,
    onBack: () -> Unit,
    onResumeDraft: () -> Unit,
    onOpenWheelPairs: () -> Unit,
    onStartMeasurement: () -> Unit,
    onOpenVoiceMeasurement: () -> Unit,
    onDelete: () -> Unit,
    showDeleteButton: Boolean = true,
) {
    val measurementState by viewModel.measurementState.collectAsState()
    val totalPairs = locomotive.wheelPairCount.coerceAtLeast(1)
    val filledPairs = when {
        hasActiveDraft -> currentDraftFilledPairs
        latestArchive != null -> latestArchive.filledWheelPairs
        else -> 0
    }.coerceIn(0, totalPairs)
    val statusText = when {
        hasActiveDraft -> "Замер не завершен"
        latestArchive != null -> "Последний замер сохранен"
        else -> "Нет замеров"
    }
    val isFull = filledPairs == totalPairs
    val statusColor = if (isFull) Color(0xFFDFF3E0) else Color(0xFFFFE8C8)
    val statusContentColor = if (isFull) Color(0xFF2E7D32) else Color(0xFFB26A00)
    val progressText = "$filledPairs / $totalPairs"

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "${locomotive.series} ${locomotive.number}",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        item {
            Card(shape = MaterialTheme.shapes.medium) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "Даты ремонтов",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    repairDates.entries.forEachIndexed { index, (repair, date) ->
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(repair)
                            Text(date ?: "-")
                        }
                        if (index != repairDates.size - 1) {
                            Divider(
                                modifier = Modifier.fillMaxWidth(),
                                thickness = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                        }
                    }
                }
            }
        }
        item {
            Card(shape = MaterialTheme.shapes.medium) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Event, contentDescription = null, modifier = Modifier.size(20.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("Последний замер", style = MaterialTheme.typography.titleSmall, maxLines = 1, softWrap = false)
                            Text(latestArchive?.measurementDate?.displayDate() ?: "Нет данных", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .align(androidx.compose.ui.Alignment.CenterVertically)
                            .width(1.dp)
                            .height(42.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Storage, contentDescription = null, modifier = Modifier.size(20.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("Экспорт в базу", style = MaterialTheme.typography.titleSmall, maxLines = 1, softWrap = false)
                            Text(if (hasActiveDraft) "Да" else "Нет", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        }
        item {
            // Плашка текущего состояния замера
            Card(
                shape = MaterialTheme.shapes.medium,
                onClick = {
                    if (hasActiveDraft) {
                        onResumeDraft()
                    }
                },
                colors = CardDefaults.cardColors(
                    containerColor = statusColor,
                    contentColor = statusContentColor
                )
            ) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Заполнено колесных пар", style = MaterialTheme.typography.titleSmall, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                    Text(progressText, style = MaterialTheme.typography.titleLarge, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                    Text(statusText, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                }
            }
        }
        item {
            // Основные кнопки замера
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = MaterialTheme.shapes.medium,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    onClick = onStartMeasurement
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(2.dp))
                    Text("Новый замер", maxLines = 1, softWrap = false)
                }
                Button(
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = MaterialTheme.shapes.medium,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    onClick = onOpenVoiceMeasurement
                ) {
                    Icon(Icons.Default.Info, contentDescription = null)
                    Spacer(Modifier.width(2.dp))
                    Text("Замер", maxLines = 1, softWrap = false)
                }
            }
        }
        item {
            // Кнопки КП и экспорта
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = MaterialTheme.shapes.medium,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    onClick = onOpenWheelPairs
                ) {
                    Icon(Icons.Default.Info, contentDescription = null)
                    Spacer(Modifier.width(2.dp))
                    Text("КП", maxLines = 1, softWrap = false)
                }
                OutlinedButton(
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = MaterialTheme.shapes.medium,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    onClick = { latestArchive?.let { viewModel.requestExportShare(it.measurementId) } }
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(Modifier.width(2.dp))
                    Text("Экспорт", maxLines = 1, softWrap = false)
                }
            }
        }
        item {
            if (showDeleteButton) {
                // Кнопка удаления локомотива
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = MaterialTheme.shapes.medium,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.error),
                    onClick = onDelete
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(Modifier.width(2.dp))
                    Text("Удалить локомотив", maxLines = 1, softWrap = false)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MeasurementScreen(viewModel: AppViewModel, padding: PaddingValues, snackbar: SnackbarHostState) {
    val context = LocalContext.current
    val locomotives by viewModel.locomotives.collectAsState()
    val state by viewModel.measurementState.collectAsState()
    val profiles by viewModel.activeProfiles.collectAsState()
    val selectedLocomotive = locomotives.firstOrNull { it.id == state.selectedLocomotiveId }
    val totalWheelPairs = selectedLocomotive?.wheelPairCount?.coerceAtLeast(1) ?: 12
    val settings by viewModel.settings.collectAsState()
    val scope = rememberCoroutineScope()
    var showLocomotiveDialog by remember { mutableStateOf(false) }
    var locomotiveDialogStep by remember { mutableStateOf(MeasurementLocomotiveStep.SERIES) }
    var selectedSeriesChoice by remember { mutableStateOf<MeasurementSeriesChoice?>(null) }
    var showDateDialog by remember { mutableStateOf(false) }
    var showRepairDialog by remember { mutableStateOf(false) }
    var pendingLocomotiveId by remember { mutableStateOf<Long?>(null) }
    var pendingDate by remember { mutableStateOf(state.measurementDate) }
    val repairTypes = remember(pendingLocomotiveId, locomotives) {
        repairTypesFor(locomotives.firstOrNull { it.id == pendingLocomotiveId })
    }
    val datePickerState = androidx.compose.material3.rememberDatePickerState(
        initialSelectedDateMillis = LocalDate.now()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    )
    LaunchedEffect(showDateDialog) {
        if (showDateDialog) {
            datePickerState.selectedDateMillis = LocalDate.now()
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        }
    }
    LaunchedEffect(selectedLocomotive?.id, totalWheelPairs, state.selectedWheelPair) {
        if (state.selectedWheelPair > totalWheelPairs) {
            viewModel.selectSide(totalWheelPairs, state.selectedSide)
        }
    }
    val voiceRef = remember { arrayOfNulls<SpeechRecognizerController>(1) }
    val voiceFeedback = remember { VoiceFeedback() }
    val voiceSpeaker = remember { VoiceSpeaker(context) }
    val voiceServiceIntent = remember { Intent(context, VoiceForegroundService::class.java) }
    val modelStore = remember { VoskModelStore(context) }
    var resumeVoiceAfterSpeech by remember { mutableStateOf(false) }
    var voiceTimeoutJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    val voice = remember {
        SpeechRecognizerController(
            onTextRecognized = { text, confidence ->
                val result = viewModel.parseVoice(text, confidence)
                if (result.command is VoiceCommand.Unknown) {
                    voiceFeedback.signalNeedsConfirmation()
                } else {
                    voiceFeedback.signalRecognized()
                }
                if (result.command == VoiceCommand.FinishMeasurements) {
                    voiceTimeoutJob?.cancel()
                    voiceTimeoutJob = null
                    resumeVoiceAfterSpeech = false
                    voiceRef[0]?.stop()
                    runCatching { context.stopService(voiceServiceIntent) }
                }
            },
            onErrorMessage = { message -> scope.launch { snackbar.showSnackbar(message) } },
            onRecognitionText = viewModel::updateVoiceRecognizedText,
        )
    }
    fun stopVoiceListening() {
        resumeVoiceAfterSpeech = false
        voiceTimeoutJob?.cancel()
        voiceTimeoutJob = null
        voice.stop()
        runCatching { context.stopService(voiceServiceIntent) }
    }
    fun pauseVoiceForSpeech() {
        voiceTimeoutJob?.cancel()
        voiceTimeoutJob = null
        voice.stop()
        runCatching { context.stopService(voiceServiceIntent) }
    }
    fun startVoiceListening() {
        scope.launch {
            val modelPath = modelStore.prepareModel()
            if (modelPath.isNullOrBlank()) {
                snackbar.showSnackbar("Не удалось подготовить встроенную модель Vosk")
                return@launch
            }
            runCatching {
                ContextCompat.startForegroundService(context, voiceServiceIntent)
                voice.start(modelPath)
                voiceTimeoutJob?.cancel()
                voiceTimeoutJob = launch {
                    kotlinx.coroutines.delay(60L * 60L * 1000L)
                    stopVoiceListening()
                    snackbar.showSnackbar("Голосовой режим автоматически остановлен через час")
                }
            }.onFailure {
                stopVoiceListening()
                snackbar.showSnackbar(it.message ?: "Не удалось запустить офлайн-распознавание")
            }
        }
    }
    DisposableEffect(Unit) {
        voiceRef[0] = voice
        onDispose {
            stopVoiceListening()
            voice.destroy()
            voiceFeedback.release()
            voiceSpeaker.release()
            voiceRef[0] = null
        }
    }
    LaunchedEffect(state.voiceAnnouncementNonce) {
        if (state.voiceAnnouncement.isNotBlank()) {
            val shouldResume = voice.isActive()
            if (shouldResume) {
                resumeVoiceAfterSpeech = true
                pauseVoiceForSpeech()
            }
            voiceSpeaker.speak(state.voiceAnnouncement) {
                viewModel.onVoiceAnnouncementFinished()
                if (shouldResume && resumeVoiceAfterSpeech) {
                    scope.launch {
                        kotlinx.coroutines.delay(30)
                        if (resumeVoiceAfterSpeech) startVoiceListening()
                    }
                }
            }
        }
    }
    Box(Modifier.fillMaxSize().padding(padding)) {
        if (state.activeSessionId == null) {
            Button(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 28.dp)
                    .fillMaxWidth()
                    .height(60.dp),
                shape = MaterialTheme.shapes.medium,
                enabled = locomotives.isNotEmpty(),
                onClick = {
                    pendingLocomotiveId = state.selectedLocomotiveId
                    locomotiveDialogStep = MeasurementLocomotiveStep.SERIES
                    selectedSeriesChoice = null
                    showLocomotiveDialog = true
                }
            ) { Text("Новый замер") }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                item {
                    MeasurementTopCard(
                        locomotive = selectedLocomotive,
                        measurementDate = state.measurementDate.displayDate(),
                        repairType = state.repairType
                    )
                }
                item {
                    Spacer(Modifier.height(6.dp))
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedButton(
                            modifier = Modifier.weight(1f).height(46.dp),
                            shape = MaterialTheme.shapes.medium,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.error),
                            onClick = { viewModel.clearCurrentMeasurement() }
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                            Spacer(Modifier.width(2.dp))
                            Text("Очистить замер", maxLines = 1, softWrap = false)
                        }
                        Button(
                            modifier = Modifier.weight(1f).height(46.dp),
                            shape = MaterialTheme.shapes.medium,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            onClick = {
                                pendingLocomotiveId = state.selectedLocomotiveId
                                locomotiveDialogStep = MeasurementLocomotiveStep.SERIES
                                selectedSeriesChoice = null
                                showLocomotiveDialog = true
                            }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(2.dp))
                            Text("Новый замер", maxLines = 1, softWrap = false)
                        }
                    }
                }
                item {
                    Text(
                        "Заполнено КП: ${state.filledWheelPairs} из $totalWheelPairs",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
                item {
                    MeasurementWheelSelectionSection(
                        state = state,
                        totalWheelPairs = totalWheelPairs,
                        onWheelPairSelected = { viewModel.selectSide(it, state.selectedSide) }
                    )
                }
                item {
                    MeasurementEditor(
                        state = state,
                        profiles = profiles,
                        viewModel = viewModel,
                        voiceActive = voice.isActive(),
                        onOpenExchange = { viewModel.requestExportShare() },
                        onListen = {
                            if (voice.isActive()) stopVoiceListening() else startVoiceListening()
                        }
                    )
                }
                if (state.exportPreview.isNotBlank()) {
                    item { ExportPreview(state.exportFileName, state.exportPreview) }
                }
            }
        }
    }

    if (showLocomotiveDialog) {
        Dialog(
            onDismissRequest = {
                showLocomotiveDialog = false
                locomotiveDialogStep = MeasurementLocomotiveStep.SERIES
                selectedSeriesChoice = null
            }
        ) {
            Card(
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.widthIn(min = 360.dp)
            ) {
                val filteredLocomotives = remember(selectedSeriesChoice, locomotives) {
                    locomotives.sortedBy { it.sortOrder }
                        .filter { locomotive ->
                            val seriesKey = locomotive.series.seriesKey()
                            when (selectedSeriesChoice) {
                                MeasurementSeriesChoice.TEM -> seriesKey.startsWith("ТЭМ")
                                MeasurementSeriesChoice.PE2M -> seriesKey.startsWith("ПЭ2М")
                                null -> true
                            }
                        }
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Заголовок и выбор серии/локомотива
                    Text(
                        when (locomotiveDialogStep) {
                            MeasurementLocomotiveStep.SERIES -> "Выберите серию"
                            MeasurementLocomotiveStep.LOCOMOTIVE -> "Выберите локомотив"
                        },
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.headlineSmall
                    )
                    when (locomotiveDialogStep) {
                        MeasurementLocomotiveStep.SERIES -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Кнопки выбора серии
                                OutlinedButton(
                                    modifier = Modifier.weight(1f).height(46.dp),
                                    shape = MaterialTheme.shapes.medium,
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                    onClick = {
                                        selectedSeriesChoice = MeasurementSeriesChoice.TEM
                                        locomotiveDialogStep = MeasurementLocomotiveStep.LOCOMOTIVE
                                    }
                                ) {
                                    Icon(Icons.Default.Train, contentDescription = null)
                                    Spacer(Modifier.width(4.dp))
                                    Text("ТЭМ", maxLines = 1, softWrap = false)
                                }
                                OutlinedButton(
                                    modifier = Modifier.weight(1f).height(46.dp),
                                    shape = MaterialTheme.shapes.medium,
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                    onClick = {
                                        selectedSeriesChoice = MeasurementSeriesChoice.PE2M
                                        locomotiveDialogStep = MeasurementLocomotiveStep.LOCOMOTIVE
                                    }
                                ) {
                                    Icon(Icons.Default.Train, contentDescription = null)
                                    Spacer(Modifier.width(4.dp))
                                    Text("ПЭ-2М", maxLines = 1, softWrap = false)
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                TextButton(
                                    onClick = {
                                        showLocomotiveDialog = false
                                        locomotiveDialogStep = MeasurementLocomotiveStep.SERIES
                                        selectedSeriesChoice = null
                                    }
                                ) {
                                    Text("Отмена")
                                }
                            }
                        }
                        MeasurementLocomotiveStep.LOCOMOTIVE -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 320.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                // Список локомотивов выбранной серии
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    if (filteredLocomotives.isEmpty()) {
                                        Text("Для выбранной серии локомотивы не найдены")
                                    } else {
                                        filteredLocomotives.forEach { locomotive ->
                                            OutlinedButton(
                                                modifier = Modifier.fillMaxWidth().height(46.dp),
                                                shape = MaterialTheme.shapes.medium,
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                                onClick = {
                                                    pendingLocomotiveId = locomotive.id
                                                    showLocomotiveDialog = false
                                                    locomotiveDialogStep = MeasurementLocomotiveStep.SERIES
                                                    selectedSeriesChoice = null
                                                    showDateDialog = true
                                                }
                                            ) {
                                                Icon(Icons.Default.Train, contentDescription = null)
                                                Spacer(Modifier.width(4.dp))
                                                Text("${locomotive.series} ${locomotive.number}", maxLines = 1, softWrap = false)
                                            }
                                        }
                                    }
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                // Кнопки возврата и отмены
                                TextButton(
                                    onClick = { locomotiveDialogStep = MeasurementLocomotiveStep.SERIES }
                                ) {
                                    Text("Назад")
                                }
                                Spacer(Modifier.width(50.dp))
                                TextButton(
                                    onClick = {
                                        showLocomotiveDialog = false
                                        locomotiveDialogStep = MeasurementLocomotiveStep.SERIES
                                        selectedSeriesChoice = null
                                    }
                                ) {
                                    Text("Отмена")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDateDialog) {
        DatePickerDialog(
            onDismissRequest = { showDateDialog = false },
            shape = MaterialTheme.shapes.medium,
            confirmButton = {
                // Кнопка перехода к ремонту
                TextButton(
                    onClick = {
                        pendingDate = datePickerState.selectedDateMillis.toIsoLocalDateString() ?: state.measurementDate
                        showDateDialog = false
                        showRepairDialog = true
                    }
                ) {
                    Text("Далее")
                }
            },
            dismissButton = {
                // Кнопка отмены выбора даты
                TextButton(onClick = { showDateDialog = false }) {
                    Text("Отмена")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showRepairDialog) {
        Dialog(
            onDismissRequest = { showRepairDialog = false }
        ) {
            Card(
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.widthIn(min = 360.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Заголовок и кнопки выбора ремонта
                    Text(
                        "Выберите вид ремонта",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        repairTypes.chunked(2).forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Кнопки видов ремонта
                                rowItems.forEach { repairType ->
                                    OutlinedButton(
                                        modifier = Modifier.weight(1f).height(46.dp),
                                        shape = MaterialTheme.shapes.medium,
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                        onClick = {
                                            val locomotiveId = pendingLocomotiveId ?: return@OutlinedButton
                                            viewModel.selectLocomotive(locomotiveId)
                                            viewModel.updateDate(pendingDate)
                                            viewModel.updateRepairType(repairType)
                                            if (state.activeSessionId == null) {
                                                viewModel.startMeasurement()
                                            } else {
                                                viewModel.startNewMeasurement()
                                            }
                                            showRepairDialog = false
                                        }
                                    ) {
                                        Text(repairType, maxLines = 1, softWrap = false)
                                    }
                                }
                                if (rowItems.size == 1) {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        // Кнопка отмены ремонта
                        TextButton(onClick = { showRepairDialog = false }) {
                            Text("Отмена")
                        }
                    }
                }
            }
        }
    }

}

@Composable
private fun MeasurementTopCard(
    locomotive: LocomotiveEntity?,
    measurementDate: String,
    repairType: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth().height(72.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Train,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = Color.DarkGray
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = locomotive?.let { "${it.series} ${it.number}" } ?: "Локомотив",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = MaterialTheme.typography.headlineMedium.fontSize * 0.8f,
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    ),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$measurementDate  $repairType",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = MaterialTheme.typography.titleMedium.fontSize * 0.8f
                    ),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))

            Icon(
                imageVector = Icons.Default.Train,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = Color.DarkGray
            )
        }
    }
}

@Composable
private fun MeasurementWheelSelectionSection(
    state: MeasurementUiState,
    totalWheelPairs: Int,
    onWheelPairSelected: (Int) -> Unit,
) {
    val filledPairs = remember(state.sides) {
        state.sides.groupBy { it.wheelPairNumber }.filter { (_, pairSides) ->
            pairSides.size == 2 && pairSides.all {
                it.flangeThickness != null &&
                    it.flangeWear != null &&
                    it.flangeSteepness != null &&
                    it.bandageThickness != null
            }
        }.keys
    }
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        WheelPairButtons(
            total = totalWheelPairs,
            selected = state.selectedWheelPair,
            filledPairs = filledPairs,
            onSelect = onWheelPairSelected,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            WheelPairLegendItem(color = MaterialTheme.colorScheme.primary, label = "текущий")
            Spacer(Modifier.width(24.dp))
            WheelPairLegendItem(color = Color(0xFF61B865), label = "заполнен")
            Spacer(Modifier.width(24.dp))
            WheelPairLegendItem(color = MaterialTheme.colorScheme.surfaceVariant, label = "пустой")
        }
    }
}

@Composable
private fun WheelPairLegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .background(color = color, shape = CircleShape)
        )
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun MeasurementEditor(
    state: MeasurementUiState,
    profiles: List<WheelPairProfileEntity>,
    viewModel: AppViewModel,
    voiceActive: Boolean,
    onOpenExchange: () -> Unit,
    onListen: () -> Unit,
) {
    val leftMeasurement = state.sides.firstOrNull { it.wheelPairNumber == state.selectedWheelPair && it.side == WheelSide.LEFT }
    val rightMeasurement = state.sides.firstOrNull { it.wheelPairNumber == state.selectedWheelPair && it.side == WheelSide.RIGHT }
    val profile = profiles.firstOrNull { it.number == state.selectedWheelPair }
    val totalWheelPairs = profiles.size.coerceAtLeast(1)
    val focusManager = LocalFocusManager.current
    val leftGrebenFocus = remember { FocusRequester() }
    val leftProkatFocus = remember { FocusRequester() }
    val leftKrutFocus = remember { FocusRequester() }
    val leftBandageFocus = remember { FocusRequester() }
    val rightGrebenFocus = remember { FocusRequester() }
    val rightProkatFocus = remember { FocusRequester() }
    val rightKrutFocus = remember { FocusRequester() }
    val rightBandageFocus = remember { FocusRequester() }
    var pendingWheelPairFocusTarget by remember { mutableStateOf<Int?>(null) }
    var leftGreben by remember(leftMeasurement) { mutableStateOf(leftMeasurement?.flangeThickness?.toString().orEmpty()) }
    var leftProkat by remember(leftMeasurement) { mutableStateOf(leftMeasurement?.flangeWear?.toString().orEmpty()) }
    var leftKrut by remember(leftMeasurement) { mutableStateOf(leftMeasurement?.flangeSteepness?.toString().orEmpty()) }
    var leftBandage by remember(leftMeasurement) { mutableStateOf(leftMeasurement?.bandageThickness?.toString().orEmpty()) }
    var rightGreben by remember(rightMeasurement) { mutableStateOf(rightMeasurement?.flangeThickness?.toString().orEmpty()) }
    var rightProkat by remember(rightMeasurement) { mutableStateOf(rightMeasurement?.flangeWear?.toString().orEmpty()) }
    var rightKrut by remember(rightMeasurement) { mutableStateOf(rightMeasurement?.flangeSteepness?.toString().orEmpty()) }
    var rightBandage by remember(rightMeasurement) { mutableStateOf(rightMeasurement?.bandageThickness?.toString().orEmpty()) }

    val saveCurrentWheelPairAndMove: (Boolean) -> Unit = { advanceFocus ->
        val saved = viewModel.saveCurrentWheelPair(
            leftFlangeThickness = leftGreben.toDoubleOrNullComma(),
            leftFlangeWear = leftProkat.toDoubleOrNullComma(),
            leftFlangeSteepness = leftKrut.toDoubleOrNullComma(),
            leftBandageThickness = leftBandage.toDoubleOrNullComma(),
            rightFlangeThickness = rightGreben.toDoubleOrNullComma(),
            rightFlangeWear = rightProkat.toDoubleOrNullComma(),
            rightFlangeSteepness = rightKrut.toDoubleOrNullComma(),
            rightBandageThickness = rightBandage.toDoubleOrNullComma(),
            kcLeft = profile?.kcDiameterLeft,
            kcRight = profile?.kcDiameterRight,
        )
        if (saved && advanceFocus && state.selectedWheelPair < totalWheelPairs) {
            pendingWheelPairFocusTarget = state.selectedWheelPair + 1
        } else {
            pendingWheelPairFocusTarget = null
            focusManager.clearFocus()
        }
    }

    LaunchedEffect(state.selectedWheelPair, state.activeSessionId, pendingWheelPairFocusTarget) {
        val target = pendingWheelPairFocusTarget ?: return@LaunchedEffect
        if (state.activeSessionId != null && state.selectedWheelPair == target) {
            leftGrebenFocus.requestFocus()
            pendingWheelPairFocusTarget = null
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        "Левая",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = MaterialTheme.typography.titleLarge.fontSize * 0.85f
                        ),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "Правая",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = MaterialTheme.typography.titleLarge.fontSize * 0.85f
                        ),
                        textAlign = TextAlign.Center
                    )
                }
                MeasurementInputRow(
                    label = "ТГ",
                    leftValue = leftGreben,
                    onLeftValueChange = {
                        leftGreben = numberText(it)
                        viewModel.selectSide(state.selectedWheelPair, WheelSide.LEFT)
                    },
                    leftFocusRequester = leftGrebenFocus,
                    leftKeyboardActions = KeyboardActions(onNext = { leftProkatFocus.requestFocus() }),
                    rightValue = rightGreben,
                    onRightValueChange = {
                        rightGreben = numberText(it)
                        viewModel.selectSide(state.selectedWheelPair, WheelSide.RIGHT)
                    },
                    rightFocusRequester = rightGrebenFocus,
                    rightKeyboardActions = KeyboardActions(onNext = { rightProkatFocus.requestFocus() }),
                )
                MeasurementInputRow(
                    label = "ПР",
                    leftValue = leftProkat,
                    onLeftValueChange = {
                        leftProkat = numberText(it)
                        viewModel.selectSide(state.selectedWheelPair, WheelSide.LEFT)
                    },
                    leftFocusRequester = leftProkatFocus,
                    leftKeyboardActions = KeyboardActions(onNext = { leftKrutFocus.requestFocus() }),
                    rightValue = rightProkat,
                    onRightValueChange = {
                        rightProkat = numberText(it)
                        viewModel.selectSide(state.selectedWheelPair, WheelSide.RIGHT)
                    },
                    rightFocusRequester = rightProkatFocus,
                    rightKeyboardActions = KeyboardActions(onNext = { rightKrutFocus.requestFocus() }),
                )
                MeasurementInputRow(
                    label = "КР",
                    leftValue = leftKrut,
                    onLeftValueChange = {
                        leftKrut = numberText(it)
                        viewModel.selectSide(state.selectedWheelPair, WheelSide.LEFT)
                    },
                    leftFocusRequester = leftKrutFocus,
                    leftKeyboardActions = KeyboardActions(onNext = { leftBandageFocus.requestFocus() }),
                    rightValue = rightKrut,
                    onRightValueChange = {
                        rightKrut = numberText(it)
                        viewModel.selectSide(state.selectedWheelPair, WheelSide.RIGHT)
                    },
                    rightFocusRequester = rightKrutFocus,
                    rightKeyboardActions = KeyboardActions(onNext = { rightBandageFocus.requestFocus() }),
                )
                MeasurementInputRow(
                    label = "ТБ",
                    leftValue = leftBandage,
                    onLeftValueChange = {
                        leftBandage = numberText(it)
                        viewModel.selectSide(state.selectedWheelPair, WheelSide.LEFT)
                    },
                    leftFocusRequester = leftBandageFocus,
                    leftImeAction = ImeAction.Done,
                    leftKeyboardActions = KeyboardActions(onDone = { rightGrebenFocus.requestFocus() }),
                    rightValue = rightBandage,
                    onRightValueChange = {
                        rightBandage = numberText(it)
                        viewModel.selectSide(state.selectedWheelPair, WheelSide.RIGHT)
                    },
                    rightFocusRequester = rightBandageFocus,
                    rightImeAction = ImeAction.Done,
                    rightKeyboardActions = KeyboardActions(onDone = { saveCurrentWheelPairAndMove(true) }),
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedButton(
                modifier = Modifier.weight(1f).height(46.dp),
                shape = MaterialTheme.shapes.medium,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.error),
                onClick = {
                    viewModel.clearCurrentWheelPair()
                    focusManager.clearFocus()
                }
            ) {
                Icon(Icons.Default.Delete, contentDescription = null)
                Spacer(Modifier.width(2.dp))
                Text("Очистить КП", maxLines = 1, softWrap = false)
            }
            Button(
                modifier = Modifier.weight(1f).height(46.dp),
                shape = MaterialTheme.shapes.medium,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                onClick = { saveCurrentWheelPairAndMove(false) }
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(Modifier.width(2.dp))
                Text("Сохранить КП", maxLines = 1, softWrap = false)
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedButton(
                modifier = Modifier.weight(1f).height(50.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                onClick = onListen
            ) {
                Icon(Icons.Default.Mic, contentDescription = null)
                Spacer(Modifier.width(10.dp))
                Text(if (voiceActive) "Стоп" else "Голос")
            }
            OutlinedButton(
                modifier = Modifier.weight(1f).height(50.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                onClick = onOpenExchange
            ) {
                Icon(Icons.Default.Share, contentDescription = null)
                Spacer(Modifier.width(10.dp))
                Text("Экспорт")
            }
        }

        if (voiceActive || state.lastHeardText.isNotBlank()) {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                value = state.lastHeardText,
                onValueChange = {},
                label = { Text("Распознано голосом") },
                singleLine = false,
            )
        }
        if (state.lastValidationText.isNotBlank()) {
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Проверка", style = MaterialTheme.typography.titleMedium)
                    Text(state.lastValidationText)
                }
            }
        }
    }
}

@Composable
private fun MeasurementInputRow(
    label: String,
    leftValue: String,
    onLeftValueChange: (String) -> Unit,
    leftFocusRequester: FocusRequester? = null,
    leftImeAction: ImeAction = ImeAction.Next,
    leftKeyboardActions: KeyboardActions = KeyboardActions.Default,
    rightValue: String,
    onRightValueChange: (String) -> Unit,
    rightFocusRequester: FocusRequester? = null,
    rightImeAction: ImeAction = ImeAction.Next,
    rightKeyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        MeasurementMetricField(
            modifier = Modifier.weight(1f),
            value = leftValue,
            onValueChange = onLeftValueChange,
            focusRequester = leftFocusRequester,
            imeAction = leftImeAction,
            keyboardActions = leftKeyboardActions,
        )
        Box(
            modifier = Modifier.width(38.dp).height(55.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
        }
        MeasurementMetricField(
            modifier = Modifier.weight(1f),
            value = rightValue,
            onValueChange = onRightValueChange,
            focusRequester = rightFocusRequester,
            imeAction = rightImeAction,
            keyboardActions = rightKeyboardActions,
            mirrored = true,
        )
    }
}

@Composable
private fun MeasurementMetricField(
    modifier: Modifier = Modifier,
    value: String,
    onValueChange: (String) -> Unit,
    focusRequester: FocusRequester? = null,
    imeAction: ImeAction,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    mirrored: Boolean = false,
) {
    OutlinedTextField(
        modifier = modifier
            .height(55.dp)
            .let { base -> if (focusRequester != null) base.focusRequester(focusRequester) else base },
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(
                "---",
                modifier = Modifier.fillMaxWidth(),
                textAlign = if (mirrored) TextAlign.End else TextAlign.Start
            )
        },
        leadingIcon = if (mirrored) { { Text("мм") } } else null,
        trailingIcon = if (mirrored) null else { { Text("мм") } },
        singleLine = true,
        shape = MaterialTheme.shapes.medium,
        textStyle = TextStyle(
            textAlign = if (mirrored) TextAlign.End else TextAlign.Start,
            fontSize = 22.sp,
            lineHeight = 22.sp
        ),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = imeAction),
        keyboardActions = keyboardActions,
    )
}

@Composable
private fun WheelPairButtons(
    total: Int,
    selected: Int,
    filledPairs: Set<Int>,
    onSelect: (Int) -> Unit,
) {
    val rows = (1..total).chunked(6)
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                row.forEach { number ->
                    val isCurrent = number == selected
                    val isFilled = number in filledPairs
                    val containerColor = when {
                        isCurrent -> MaterialTheme.colorScheme.primary
                        isFilled -> Color(0xFF61B865)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                    val contentColor = when {
                        isCurrent -> MaterialTheme.colorScheme.onPrimary
                        isFilled -> Color.White
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(0.9f).aspectRatio(1f),
                            onClick = { onSelect(number) },
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(containerColor = containerColor, contentColor = contentColor)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = number.toString(),
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontSize = MaterialTheme.typography.titleLarge.fontSize * 0.9f
                                    ),
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArchiveLocomotivesScreen(
    viewModel: AppViewModel,
    padding: PaddingValues,
    chromeVisible: Boolean,
    onToggleChrome: () -> Unit,
) {
    val locomotives by viewModel.locomotives.collectAsState()
    val archive by viewModel.archive.collectAsState()
    val sortedLocomotives = locomotives.sortedBy { it.sortOrder }
    var detailLocomotiveId by rememberSaveable { mutableStateOf<Long?>(null) }
    var detailMeasurements by remember { mutableStateOf<List<ArchiveMeasurementEntry>>(emptyList()) }
    var detailLoading by remember { mutableStateOf(false) }

    val detailLocomotive = sortedLocomotives.firstOrNull { it.id == detailLocomotiveId }

    LaunchedEffect(detailLocomotiveId, archive) {
        val locomotive = detailLocomotive ?: return@LaunchedEffect
        detailLoading = true
        detailMeasurements = emptyList()
        val loaded = archive
            .filter { it.locomotiveTitle == "${locomotive.series} ${locomotive.number}" }
            .sortedBy { it.measurementDate.toIsoDateMillis() ?: Long.MAX_VALUE }
            .mapNotNull { item ->
                runCatching { viewModel.buildArchiveMeasurement(item.measurementId) }.getOrNull()?.let { dto ->
                    ArchiveMeasurementEntry(item, dto)
                }
            }
        detailMeasurements = loaded
        detailLoading = false
    }

    BackHandler(enabled = detailLocomotive != null) {
        detailLocomotiveId = null
        detailMeasurements = emptyList()
        detailLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (detailLocomotive == null) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
            ) {
                items(sortedLocomotives, key = { it.id }) { locomotive ->
                    val locomotiveArchiveItems = archive.filter { it.locomotiveTitle == "${locomotive.series} ${locomotive.number}" }
                    val latest = locomotiveArchiveItems.maxByOrNull { it.measurementDate.toIsoDateMillis() ?: Long.MIN_VALUE }
                    val attentionState = locomotive.kpAttentionState(latest)
                    Card(
                        shape = MaterialTheme.shapes.medium,
                        colors = when (attentionState) {
                            KpAttentionState.OVERDUE -> CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                            KpAttentionState.SOON -> CardDefaults.cardColors(
                                containerColor = Color(0xFFFFE8C8),
                                contentColor = Color(0xFF5D4037)
                            )
                            KpAttentionState.NONE -> CardDefaults.cardColors()
                        },
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            when (attentionState) {
                                KpAttentionState.OVERDUE -> MaterialTheme.colorScheme.error
                                KpAttentionState.SOON -> Color(0xFFFFB74D)
                                KpAttentionState.NONE -> MaterialTheme.colorScheme.outlineVariant
                            }
                        ),
                        onClick = {
                            detailLocomotiveId = locomotive.id
                        }
                    ) {
                        Column(
                            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "${locomotive.series} ${locomotive.number}",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = latest?.measurementDate?.displayDate() ?: "Нет замера",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.fillMaxWidth().height(48.dp))
        } else {
            ArchiveMeasurementTablesScreen(
                viewModel = viewModel,
                locomotive = detailLocomotive,
                measurements = detailMeasurements,
                loading = detailLoading,
                chromeVisible = chromeVisible,
                onToggleChrome = onToggleChrome,
                onDeleteMeasurement = { measurementId ->
                    viewModel.deleteMeasurement(measurementId)
                },
                onBack = {
                    detailLocomotiveId = null
                    detailMeasurements = emptyList()
                    detailLoading = false
                },
            )
        }
    }
}

@Composable
private fun ArchiveMeasurementTablesScreen(
    viewModel: AppViewModel,
    locomotive: LocomotiveEntity,
    measurements: List<ArchiveMeasurementEntry>,
    loading: Boolean,
    chromeVisible: Boolean,
    onToggleChrome: () -> Unit,
    onDeleteMeasurement: (String) -> Unit,
    onBack: () -> Unit,
) {
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var pendingDeleteMeasurementId by remember { mutableStateOf<String?>(null) }
    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        val nextScale = (scale * zoomChange).coerceIn(0.7f, 1.8f)
        scale = nextScale
        if (nextScale > 1f) {
            offset += panChange
        } else {
            offset = Offset.Zero
        }
    }
    LaunchedEffect(isPortrait) {
        if (!isPortrait) {
            scale = 1f
            offset = Offset.Zero
        }
    }
    val tableModifier = Modifier
        .fillMaxSize()
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            translationX = offset.x
            translationY = offset.y
            transformOrigin = TransformOrigin(0.5f, 0.5f)
        }
        .then(if (isPortrait) Modifier.transformable(state = transformState) else Modifier)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(chromeVisible) {
                detectTapGestures(onDoubleTap = { onToggleChrome() })
            }
    ) {
        Column(Modifier.fillMaxSize()) {
            BackHandler {
                if (scale != 1f || offset != Offset.Zero) {
                    scale = 1f
                    offset = Offset.Zero
                } else if (!chromeVisible) {
                    onToggleChrome()
                } else {
                    onBack()
                }
            }
            LazyColumn(
                modifier = tableModifier,
                state = rememberLazyListState(),
                contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                userScrollEnabled = scale == 1f,
            ) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${locomotive.series} ${locomotive.number} • ${measurements.size} ${measurementWord(measurements.size)}",
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            softWrap = false,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                if (loading) {
                    item {
                        Text(
                            text = "Загружаю замеры...",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                items(measurements, key = { it.item.measurementId }) { entry ->
                    val dto = entry.dto
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(0.25.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                modifier = Modifier.size(36.dp),
                                onClick = { viewModel.requestExportShare(entry.item.measurementId) }
                            ) {
                                Icon(
                                    Icons.Default.Share,
                                    contentDescription = "Экспорт замера",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "${dto.measurementDate.displayDate()} • ${dto.repairType} • ${dto.locomotive.series} ${dto.locomotive.number}",
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                softWrap = false,
                                textAlign = TextAlign.Center
                            )
                            if (entry.item.canDelete) {
                                IconButton(
                                    modifier = Modifier.size(36.dp),
                                    onClick = { pendingDeleteMeasurementId = entry.item.measurementId }
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Удалить замер",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                        BoxWithConstraints(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            val columnWidth = maxWidth / 11f
                            Column {
                                MeasurementArchiveGridHeader(columnWidth)
                                dto.wheelPairs.forEach { pair ->
                                    TableRow(pair, columnWidth)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    pendingDeleteMeasurementId?.let { measurementId ->
        AlertDialog(
            onDismissRequest = { pendingDeleteMeasurementId = null },
            shape = MaterialTheme.shapes.medium,
            title = { Text("Удалить замер?") },
            text = { Text("Запись будет удалена без возможности восстановления.") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteMeasurement(measurementId)
                    pendingDeleteMeasurementId = null
                }) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteMeasurementId = null }) {
                    Text("Отмена")
                }
            }
        )
    }

}

@Composable
private fun ArchiveTableCell(
    modifier: Modifier,
    text: String,
    bold: Boolean = false,
    textAlign: TextAlign = TextAlign.Start,
) {
    Text(
        text = text,
        modifier = modifier,
        style = if (bold) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis,
        textAlign = textAlign
    )
}

private data class ArchiveMeasurementEntry(
    val item: ArchiveItem,
    val dto: MeasurementExportDto,
)

@Composable
private fun MeasurementArchiveDetailScreen(
    dto: MeasurementExportDto,
    chromeVisible: Boolean,
    onToggleChrome: () -> Unit,
    onClose: () -> Unit,
) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var detailsExpanded by rememberSaveable { mutableStateOf(false) }
    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        val nextScale = (scale * zoomChange).coerceIn(0.7f, 1.8f)
        scale = nextScale
        if (nextScale > 1f) {
            offset += panChange
        } else {
            offset = Offset.Zero
        }
    }
    BackHandler {
        if (scale != 1f || offset != Offset.Zero) {
            scale = 1f
            offset = Offset.Zero
        } else {
            onClose()
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = { onToggleChrome() })
            }
            .transformable(state = transformState),
        contentAlignment = Alignment.Center
    ) {
        if (chromeVisible) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Card(
                        shape = MaterialTheme.shapes.medium,
                        onClick = { detailsExpanded = !detailsExpanded }
                    ) {
                        if (detailsExpanded) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "${dto.locomotive.series} ${dto.locomotive.number}",
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Text(
                                    text = dto.measurementDate.displayDate(),
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Text(
                                    text = dto.repairType,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Text(
                                    text = "КП: ${dto.wheelPairs.size}",
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${dto.locomotive.series} ${dto.locomotive.number}",
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Text(
                                    text = dto.measurementDate.displayDate(),
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Text(
                                    text = dto.repairType,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
                item {
                    BoxWithConstraints(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        val columnWidth = maxWidth / 11f
                        Column(
                            modifier = Modifier.graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                translationX = offset.x
                                translationY = offset.y
                                transformOrigin = TransformOrigin(0.5f, 0.5f)
                            }
                        ) {
                            MeasurementArchiveGridHeader(columnWidth)
                            dto.wheelPairs.forEach { pair ->
                                TableRow(pair, columnWidth)
                            }
                        }
                    }
                }
            }
        } else {
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                val columnWidth = maxWidth / 11f
                Column(
                    modifier = Modifier.graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                        transformOrigin = TransformOrigin(0.5f, 0.5f)
                    }
                ) {
                    MeasurementArchiveGridHeader(columnWidth)
                    dto.wheelPairs.forEach { pair ->
                        TableRow(pair, columnWidth)
                    }
                }
            }
        }
    }

}

@Composable
private fun TableRow(pair: ru.depo.zamerykp.domain.WheelPairExportDto, columnWidth: androidx.compose.ui.unit.Dp) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        TableCell(text = pair.number.toString(), width = columnWidth, height = 36.dp, bold = true)
        MeasurementGroupCells(pair.left.flangeWear, pair.right.flangeWear, columnWidth)
        MeasurementGroupCells(pair.left.flangeThickness, pair.right.flangeThickness, columnWidth)
        MeasurementGroupCells(pair.left.flangeSteepness, pair.right.flangeSteepness, columnWidth)
        MeasurementGroupCells(pair.left.bandageThickness, pair.right.bandageThickness, columnWidth)
        MeasurementGroupCells(pair.left.bandageDiameter, pair.right.bandageDiameter, columnWidth)
    }
}

@Composable
private fun MeasurementArchiveGridHeader(columnWidth: androidx.compose.ui.unit.Dp) {
    Row {
        TableCell(text = "№\nКП", width = columnWidth, height = 72.dp, bold = true, background = MaterialTheme.colorScheme.surfaceVariant)
        MeasurementGroupHeader("ПР", columnWidth)
        MeasurementGroupHeader("ТГ", columnWidth)
        MeasurementGroupHeader("КГ", columnWidth)
        MeasurementGroupHeader("ТБ", columnWidth)
        MeasurementGroupHeader("ДБ", columnWidth)
    }
}

@Composable
private fun MeasurementGroupHeader(title: String, columnWidth: androidx.compose.ui.unit.Dp) {
    Column {
        TableCell(text = title, width = columnWidth * 2f, height = 36.dp, bold = true, background = MaterialTheme.colorScheme.surfaceVariant)
        Row {
            TableCell(text = "лев", width = columnWidth, height = 36.dp, bold = true, background = MaterialTheme.colorScheme.surfaceVariant)
            TableCell(text = "прав", width = columnWidth, height = 36.dp, bold = true, background = MaterialTheme.colorScheme.surfaceVariant)
        }
    }
}

@Composable
private fun MeasurementGroupCells(left: Double?, right: Double?, columnWidth: androidx.compose.ui.unit.Dp) {
    Row {
        TableCell(text = left.archiveValue(), width = columnWidth, height = 36.dp)
        TableCell(text = right.archiveValue(), width = columnWidth, height = 36.dp)
    }
}

@Composable
private fun TableCell(
    text: String,
    width: androidx.compose.ui.unit.Dp,
    height: androidx.compose.ui.unit.Dp,
    bold: Boolean = false,
    background: Color = Color.Transparent,
) {
    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .background(background)
            .border(0.25.dp, Color.Black.copy(alpha = 0.3f), MaterialTheme.shapes.medium),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = if (bold) MaterialTheme.typography.bodySmall else MaterialTheme.typography.labelSmall,
            maxLines = 1,
            softWrap = false,
            textAlign = TextAlign.Center
        )
    }
}

private fun Double?.archiveValue(): String =
    this?.let {
        if (it % 1.0 == 0.0) it.toInt().toString() else it.toString()
    }.orEmpty()

@Composable
private fun SyncScreen(
    viewModel: AppViewModel,
    padding: PaddingValues,
    onOpenMeasurementTab: () -> Unit,
    onOpenDrafts: () -> Unit,
) {
    val context = LocalContext.current
    val state by viewModel.measurementState.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val pendingMeasurements by viewModel.pendingMeasurements.collectAsState()
    val hasDrafts = pendingMeasurements.isNotEmpty()
    var syncServerUrlText by rememberSaveable { mutableStateOf(settings.syncServerUrl) }
    var syncPasswordText by rememberSaveable { mutableStateOf(settings.syncPassword) }
    var syncServerUrlTouched by rememberSaveable { mutableStateOf(false) }
    var syncPasswordTouched by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(settings.syncServerUrl) {
        if (!syncServerUrlTouched && syncServerUrlText.isBlank() && settings.syncServerUrl.isNotBlank()) {
            syncServerUrlText = settings.syncServerUrl
        }
    }
    LaunchedEffect(settings.syncPassword) {
        if (!syncPasswordTouched && syncPasswordText.isBlank() && settings.syncPassword.isNotBlank()) {
            syncPasswordText = settings.syncPassword
        }
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val text = context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            viewModel.loadImportPreview(text)
        }
    }

    if (state.syncConflictSummary.isNotBlank()) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Найдены конфликты") },
            text = {
                Text(
                    "На сервере и в телефоне есть разные версии локомотивов.\n\n${state.syncConflictSummary}"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.pushLocalReferenceToServer(syncServerUrlText.trim(), syncPasswordText)
                    }
                ) {
                    Text("Отправить локальные")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.clearSyncConflictSummary()
                    }
                ) {
                    Text("Оставить сервер")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // Плашка онлайн-синхронизации
                Card(
                    shape = MaterialTheme.shapes.medium,
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Share, null, tint = MaterialTheme.colorScheme.primary)
                            Text("Онлайн-синхронизация", style = MaterialTheme.typography.titleLarge)
                        }
                        Text(
                            "Мобильное приложение подключается к веб-серверу по обычному HTTP и использует тот же вход, что и веб-версия.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            "Для отправки изменений нужен пароль редактирования из веб-входа.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        BigTextField(
                            value = syncServerUrlText,
                            onValueChange = {
                                syncServerUrlTouched = true
                                syncServerUrlText = it
                                viewModel.updateSyncServerUrl(it)
                            },
                            label = "Адрес сервера",
                            keyboardType = KeyboardType.Uri
                        )
                        BigTextField(
                            value = syncPasswordText,
                            onValueChange = {
                                syncPasswordTouched = true
                                syncPasswordText = it
                                viewModel.updateSyncPassword(it)
                            },
                            label = "Пароль веб-входа",
                            keyboardType = KeyboardType.Password,
                            visualTransformation = PasswordVisualTransformation()
                        )
                        Button(
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            onClick = { viewModel.syncWithServer(syncServerUrlText.trim(), syncPasswordText) },
                            enabled = syncServerUrlText.isNotBlank() && syncPasswordText.isNotBlank(),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text("Синхронизировать")
                        }
                        if (state.syncStatusMessage.isNotBlank()) {
                            Text(
                                state.syncStatusMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

        item {
            // Плашка статуса обмена
            StatusCard(hasDrafts, pendingMeasurements.size, onOpenDrafts)
        }

            if (state.importUi.kind != ImportPayloadKind.NONE) {
                item {
                    // Карточка настроек импорта
                    ImportConfigurationCard(
                        state = state,
                        hasDrafts = hasDrafts,
                        viewModel = viewModel
                    )
                }
            } else {
                item {
                    // Заглушка, если файл базы не выбран
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Storage, 
                                contentDescription = null, 
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.outlineVariant
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Файл базы не выбран",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Нижние кнопки обмена
            OutlinedButton(
                modifier = Modifier.weight(1f).height(56.dp),
                shape = MaterialTheme.shapes.medium,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                onClick = {
                    viewModel.requestArchiveExportShare()
                }
            ) {
                Icon(Icons.Default.Share, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Экспорт всей базы", maxLines = 1, softWrap = false)
            }
            OutlinedButton(
                modifier = Modifier.weight(1f).height(56.dp),
                shape = MaterialTheme.shapes.medium,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                onClick = { launcher.launch(arrayOf("application/json", "text/*", "*/*")) }
            ) {
                Icon(Icons.Default.FileDownload, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Импорт данных", maxLines = 1, softWrap = false)
            }
        }
    }
}

@Composable
private fun StatusCard(hasDrafts: Boolean, draftsCount: Int, onOpenDrafts: () -> Unit) {
    val color = if (hasDrafts) Color(0xFFE96B00) else Color(0xFF1B9E4B)
    val bgColor = if (hasDrafts) Color(0xFFFFF3E8) else Color(0xFFEAF7EE)

    Card(
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        if (hasDrafts) {
            // Плашка с черновиками
            Row(
                Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = color
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        "Есть черновики ($draftsCount)",
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.titleMedium,
                        color = color,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "Сначала завершите экспорт черновиков.",
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.bodySmall,
                        color = color.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                }
                Button(
                    onClick = onOpenDrafts,
                    shape = MaterialTheme.shapes.medium,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                ) {
                    // Кнопка открытия черновиков
                    Text("ОТКРЫТЬ", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                }
            }
        } else {
            // Зеленая плашка готовности к обмену
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Icon(
                    Icons.Default.Save,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.align(Alignment.CenterStart)
                )
                Column(
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Text(
                        "Архив готов к обмену",
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.titleMedium,
                        color = color,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "Все данные синхронизированы.",
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.bodySmall,
                        color = color.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun ImportConfigurationCard(
    state: MeasurementUiState,
    hasDrafts: Boolean,
    viewModel: AppViewModel
) {
    val importArchiveChecked =
        (state.importUi.importMeasurementArchive || state.importUi.importArchiveMeasurements) && !hasDrafts
    val importLocomotivesChecked =
        state.importUi.importMeasurementLocomotives ||
            state.importUi.importReferenceLocomotives ||
            state.importUi.importArchiveLocomotives
    val importWheelPairsChecked =
        state.importUi.importMeasurementWheelPairs ||
            state.importUi.importReferenceWheelPairs ||
            state.importUi.importArchiveWheelPairs
    val importEnabled = importLocomotivesChecked || importWheelPairsChecked || importArchiveChecked
    val importStatusMessage = state.importCheckMessage.trim()
    val isImportError = importStatusMessage.startsWith("Ошибка", ignoreCase = true)
    val isImportSuccess = importStatusMessage.startsWith("Импортировано", ignoreCase = true)

    Card(
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Заголовок блока импорта
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.FileDownload, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(4.dp))
                Text(
                    "Настройка импорта",
                    style = MaterialTheme.typography.titleLarge
                )
            }
            
            Text(state.importUi.summary, style = MaterialTheme.typography.bodyMedium)
            
            Divider(
				modifier = Modifier.padding(vertical = 8.dp),
				thickness = 1.dp,
				color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
)		 

            // Группировка опций импорта (Замеры / Справочники)
            if (state.importUi.hasMeasurement || state.importUi.hasArchive) {
                ImportSectionTitle("Данные архива")
                ImportChoiceRow(
                    label = "Замеры (только если нет черновиков)",
                    checked = importArchiveChecked,
                    enabled = !hasDrafts,
                    onCheckedChange = {
                        viewModel.setImportMeasurementArchive(it)
                        viewModel.setImportArchiveMeasurements(it)
                    }
                )
            }

            ImportSectionTitle("Справочники")
            ImportChoiceRow(
                label = "Локомотивы",
                checked = state.importUi.importMeasurementLocomotives || state.importUi.importReferenceLocomotives,
                onCheckedChange = {
                    viewModel.setImportMeasurementLocomotives(it)
                    viewModel.setImportReferenceLocomotives(it)
                }
            )
            ImportChoiceRow(
                label = "Данные КП",
                checked = state.importUi.importMeasurementWheelPairs || state.importUi.importReferenceWheelPairs,
                onCheckedChange = {
                    viewModel.setImportMeasurementWheelPairs(it)
                    viewModel.setImportReferenceWheelPairs(it)
                }
            )

            Button(
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = importEnabled,
                onClick = viewModel::importJson,
                shape = MaterialTheme.shapes.medium
            ) {
                // Кнопка запуска импорта
                Text("Загрузить в базу")
            }

            if (importStatusMessage.isNotEmpty()) {
                // Плашка результата импорта
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = when {
                            isImportError -> Icons.Default.Error
                            isImportSuccess -> Icons.Default.CheckCircle
                            else -> Icons.Default.Info
                        },
                        contentDescription = null,
                        tint = when {
                            isImportError -> MaterialTheme.colorScheme.error
                            isImportSuccess -> Color(0xFF2E7D32)
                            else -> MaterialTheme.colorScheme.primary
                        }
                    )
                    Text(
                        text = importStatusMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = when {
                            isImportError -> MaterialTheme.colorScheme.error
                            isImportSuccess -> Color(0xFF2E7D32)
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ExportCard(isEnabled: Boolean, onExport: () -> Unit) {
    val myShape = MaterialTheme.shapes.medium // Создаем форму 8dp
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = myShape, // Применяем скругление к карточке
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp, 
            MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(Icons.Default.Share, null, tint = MaterialTheme.colorScheme.secondary)
            Column(Modifier.weight(1f)) {
                // Карточка экспорта всей базы
                Text("Экспорт всей базы", style = MaterialTheme.typography.titleMedium)
                Text("Создать резервную копию JSON", style = MaterialTheme.typography.bodySmall)
            }
            IconButton(
                onClick = onExport,
                enabled = isEnabled,
                // Если IconButtonDefaults не работает, можно просто убрать colors
            ) {
                // Кнопка экспорта всей базы
                Icon(
                    Icons.Default.ArrowDownward, 
                    contentDescription = null,
                    tint = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}
@Composable
private fun ImportSectionTitle(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
private fun ImportChoiceRow(
    label: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Checkbox(checked = checked, enabled = enabled, onCheckedChange = onCheckedChange)
        Text(label, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun DraftsScreen(
    viewModel: AppViewModel,
    padding: PaddingValues,
    onBack: () -> Unit,
    onOpenDraft: (String) -> Unit,
) {
    val drafts by viewModel.pendingMeasurements.collectAsState()
    val draftsCount = drafts.size
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Заголовок вкладки черновиков
        Text(
            "Черновики",
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
        Text(
            "У вас есть незавершенные замеры: $draftsCount шт. Чтобы избежать потери данных, импорт архива заблокирован. Выполните экспорт текущих записей в базу или удалите их, если они были созданы ошибочно.",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Justify
        )
        if (drafts.isEmpty()) {
            Card {
                Text("Черновиков нет.", modifier = Modifier.padding(16.dp))
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                drafts.forEach { draft ->
                    // Кнопка черновика
                    Button(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = MaterialTheme.shapes.medium,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        onClick = { onOpenDraft(draft.measurementId) }
                    ) {
                        Text(
                            "${draft.series} ${draft.number} • ${draft.measurementDate.displayDate()} • ${draft.repairType}",
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
        Spacer(Modifier.weight(1f))
        // Кнопка возврата из черновиков
        Button(
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = MaterialTheme.shapes.medium,
            onClick = onBack
        ) {
            Text("Назад")
        }
    }
}

@Composable
private fun SettingsScreen(viewModel: AppViewModel, padding: PaddingValues) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    val state by viewModel.measurementState.collectAsState()
    val backupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val text = context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            viewModel.restoreFullBackup(text)
        }
    }
    Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    
        Divider()
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Не выключать экран")
            Checkbox(
                checked = settings.keepScreenOn,
                onCheckedChange = viewModel::setKeepScreenOn
            )
        }
        Divider()

        Text("Для надежной работы отключите оптимизацию батареи для приложения в настройках Android.")
        Button(
            modifier = Modifier.fillMaxWidth().height(56.dp),
            onClick = {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            }
        ) {
            Text("Настройки батареи")
        }
        Divider()
        Text("Резервная копия")
        Button(
            modifier = Modifier.fillMaxWidth().height(56.dp),
            onClick = viewModel::requestFullBackupShare
        ) {
            Text("Создать резервную копию")
        }
        OutlinedButton(
            modifier = Modifier.fillMaxWidth().height(52.dp),
            onClick = { backupLauncher.launch(arrayOf("application/json")) }
        ) {
            Text("Восстановить из резервной копии")
        }
        if (state.backupStatusMessage.isNotBlank()) {
            Text(state.backupStatusMessage, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocomotivePicker(
    locomotives: List<LocomotiveEntity>,
    selectedId: Long?,
    onSelect: (Long?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = locomotives.firstOrNull { it.id == selectedId }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            readOnly = true,
            value = selected?.let { "${it.series} ${it.number}" }.orEmpty(),
            onValueChange = {},
            label = { Text("Локомотив") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            locomotives.forEach { locomotive ->
                DropdownMenuItem(
                    text = { Text("${locomotive.series} ${locomotive.number}") },
                    onClick = {
                        onSelect(locomotive.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun BigTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    focusRequester: FocusRequester? = null,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    OutlinedTextField(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).let { base ->
            if (focusRequester != null) base.focusRequester(focusRequester) else base
        },
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        visualTransformation = visualTransformation,
        keyboardActions = keyboardActions,
    )
}

@Composable
private fun CompactNumberField(
    modifier: Modifier = Modifier,
    value: String,
    onValueChange: (String) -> Unit,
    focusRequester: FocusRequester? = null,
    imeAction: ImeAction,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    OutlinedTextField(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .let { base -> if (focusRequester != null) base.focusRequester(focusRequester) else base },
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = TextStyle(textAlign = TextAlign.Center).merge(
            MaterialTheme.typography.bodyMedium.copy(fontSize = 24.sp, lineHeight = 24.sp)
        ),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = imeAction),
        keyboardActions = keyboardActions,
    )
}

@Composable
private fun ExportPreview(fileName: String, json: String) {
    if (json.isBlank()) return
    val qrBitmap = remember(json) { runCatching { json.toQrBitmap() }.getOrNull() }
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(fileName.ifBlank { "zamery_kp.json" }, style = MaterialTheme.typography.titleMedium)
            if (qrBitmap != null) {
                Image(
                    bitmap = qrBitmap.asImageBitmap(),
                    contentDescription = "QR код экспорта",
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp)
                        .aspectRatio(1f)
                        .align(Alignment.CenterHorizontally),
                    contentScale = ContentScale.Fit
                )
            }
            Text(json.take(2500), style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun String.toQrBitmap(size: Int = 768): Bitmap {
    val matrix = QRCodeWriter().encode(this, BarcodeFormat.QR_CODE, size, size)
    val pixels = IntArray(size * size)
    for (y in 0 until size) {
        for (x in 0 until size) {
            pixels[y * size + x] = if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE
        }
    }
    return Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).also {
        it.setPixels(pixels, 0, size, 0, 0, size, size)
    }
}

private fun numberText(value: String): String = value.filter { it.isDigit() || it == ',' || it == '.' }
private fun String.toDoubleOrNullComma(): Double? = replace(',', '.').toDoubleOrNull()
private fun Double.formatValue(): String =
    if (this % 1.0 == 0.0) toInt().toString() else toString()

private fun String.normalizeSeriesUi(): String = trim().uppercase()

private fun String.seriesKey(): String =
    normalizeSeriesUi()
        .replace("-", "")
        .replace("–", "")
        .replace("—", "")
        .replace(" ", "")

private fun String.seriesRank(): Int = when {
    seriesKey().startsWith("ТЭМ2") -> 0
    seriesKey().startsWith("ТЭМ18") -> 1
    seriesKey().startsWith("ПЭ2М") -> 2
    else -> 99
}

private fun String.numberRank(): Int =
    filter(Char::isDigit).toIntOrNull() ?: Int.MAX_VALUE

private fun String.displayDate(): String {
    return runCatching {
        LocalDate.parse(this).format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
    }.getOrDefault(this)
}

private fun measurementWord(count: Int): String = when {
    count % 10 == 1 && count % 100 != 11 -> "замер"
    count % 10 in 2..4 && count % 100 !in 12..14 -> "замера"
    else -> "замеров"
}

private fun String.toIsoDateMillis(): Long? =
    runCatching {
        LocalDate.parse(this)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }.getOrNull()

private fun Long?.toIsoLocalDateString(): String? =
    this?.let {
        Instant.ofEpochMilli(it)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .toString()
    }

private fun List<ru.depo.zamerykp.domain.ArchiveItem>.firstRepairDate(type: String): String? =
    firstOrNull { it.repairType.normalizeRepairType() == type.normalizeRepairType() }
        ?.measurementDate
        ?.displayDate()

private fun String.normalizeRepairType(): String =
    uppercase().replace('–', '-').replace('—', '-').replace(" ", "")

private fun repairTypesFor(locomotive: LocomotiveEntity?): List<String> {
    val seriesKey = locomotive?.series?.seriesKey().orEmpty()
    return when {
        seriesKey.startsWith("ПЭ2М") -> listOf("ТО", "ТР")
        seriesKey.startsWith("ТЭМ") -> listOf("ТО-2", "ТО-3", "ТР-1", "ТР-2", "ТР-3", "СР", "КР", "ТО-4")
        else -> emptyList()
    }
}

private fun LocomotiveEntity.needsKpAttention(latestArchive: ru.depo.zamerykp.domain.ArchiveItem?): Boolean {
    return kpAttentionState(latestArchive) == KpAttentionState.OVERDUE
}

private fun LocomotiveEntity.kpAttentionState(latestArchive: ru.depo.zamerykp.domain.ArchiveItem?): KpAttentionState {
    if (!series.seriesKey().startsWith("ТЭМ")) return KpAttentionState.NONE
    val lastDate = latestArchive?.measurementDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return KpAttentionState.OVERDUE
    val dueDate = lastDate.plusDays(30)
    val today = LocalDate.now()
    return when {
        today.isAfter(dueDate) -> KpAttentionState.OVERDUE
        !today.isBefore(dueDate.minusDays(7)) -> KpAttentionState.SOON
        else -> KpAttentionState.NONE
    }
}

