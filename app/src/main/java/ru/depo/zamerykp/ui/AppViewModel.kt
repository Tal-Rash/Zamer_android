package ru.depo.zamerykp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.depo.zamerykp.AppContainer
import ru.depo.zamerykp.data.db.LocomotiveEntity
import ru.depo.zamerykp.data.db.AppSettingsEntity
import ru.depo.zamerykp.data.db.WheelPairProfileEntity
import ru.depo.zamerykp.data.db.WheelSideMeasurementEntity
import ru.depo.zamerykp.data.db.PendingMeasurementRow
import ru.depo.zamerykp.domain.ArchiveItem
import ru.depo.zamerykp.domain.ImportPayload
import ru.depo.zamerykp.domain.MeasurementField
import ru.depo.zamerykp.domain.MeasurementExportDto
import ru.depo.zamerykp.domain.VoiceCommand
import ru.depo.zamerykp.domain.VoiceCommandParser
import ru.depo.zamerykp.domain.VoiceParseResult
import ru.depo.zamerykp.domain.WheelSide
import ru.depo.zamerykp.domain.suggestedFileName
import ru.depo.zamerykp.data.repository.ServerSyncRepository
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

enum class ImportPayloadKind {
    NONE,
    MEASUREMENT,
    REFERENCE_DATA,
    ARCHIVE_DATA
}

enum class MeasurementInputMode {
    MANUAL,
    VOICE
}

enum class VoiceFlowState {
    IDLE,
    WAIT_SIDE,
    WAIT_VALUES,
    SPEAKING
}

enum class KpAttentionState {
    NONE,
    SOON,
    OVERDUE
}

data class ImportUiState(
    val kind: ImportPayloadKind = ImportPayloadKind.NONE,
    val summary: String = "",
    val measurementSummary: String = "",
    val referenceSummary: String = "",
    val archiveSummary: String = "",
    val hasMeasurement: Boolean = false,
    val hasReference: Boolean = false,
    val hasArchive: Boolean = false,
    val importMeasurementArchive: Boolean = true,
    val importMeasurementLocomotives: Boolean = true,
    val importMeasurementWheelPairs: Boolean = true,
    val importReferenceLocomotives: Boolean = true,
    val importReferenceWheelPairs: Boolean = true,
    val importArchiveLocomotives: Boolean = true,
    val importArchiveWheelPairs: Boolean = true,
    val importArchiveMeasurements: Boolean = true,
)

data class MeasurementUiState(
    val activeSessionId: String? = null,
    val selectedLocomotiveId: Long? = null,
    val selectedWheelPair: Int = 1,
    val selectedSide: WheelSide = WheelSide.LEFT,
    val inputMode: MeasurementInputMode = MeasurementInputMode.MANUAL,
    val measurementDate: String = LocalDate.now().toString(),
    val repairType: String = "ТО-3",
    val sides: List<WheelSideMeasurementEntity> = emptyList(),
    val selectedArchiveMeasurementId: String? = null,
    val selectedArchiveMeasurement: MeasurementExportDto? = null,
    val pendingVoice: VoiceParseResult? = null,
    val exportPreview: String = "",
    val exportFileName: String = "",
    val exportShareRequestNonce: Long = 0L,
    val archiveExportPreview: String = "",
    val archiveExportFileName: String = "",
    val archiveExportShareRequestNonce: Long = 0L,
    val backupPreview: String = "",
    val backupFileName: String = "",
    val backupShareRequestNonce: Long = 0L,
    val backupStatusMessage: String = "",
    val syncStatusMessage: String = "",
    val syncConflictSummary: String = "",
    val importCheckMessage: String = "",
    val importUi: ImportUiState = ImportUiState(),
    val voiceFlowState: VoiceFlowState = VoiceFlowState.IDLE,
    val voiceFlowResumeState: VoiceFlowState? = null,
    val voiceMeasurementArmed: Boolean = false,
    val lastHeardText: String = "",
    val lastSavedVoiceText: String = "",
    val lastValidationText: String = "",
    val voiceAnnouncement: String = "",
    val voiceAnnouncementNonce: Long = 0L,
) {
    val filledWheelPairs: Int
        get() = sides.groupBy { it.wheelPairNumber }.count { (_, pairSides) ->
            pairSides.size == 2 && pairSides.all {
                it.flangeThickness != null &&
                    it.flangeWear != null &&
                    it.flangeSteepness != null &&
                    it.bandageThickness != null
            }
        }
}

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModel(private val container: AppContainer) : ViewModel() {
    private val parser = VoiceCommandParser()
    private var pendingImport: ru.depo.zamerykp.domain.ImportEnvelope? = null
    private var voiceAnnouncementToken = 0L

    init {
        restoreLatestDraft()
    }

    val locomotives: StateFlow<List<LocomotiveEntity>> =
        container.locomotiveRepository.observeLocomotives()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val archive: StateFlow<List<ArchiveItem>> =
        container.measurementRepository.observeArchive()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val pendingMeasurements: StateFlow<List<PendingMeasurementRow>> =
        container.measurementRepository.observePendingMeasurements()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val settings: StateFlow<AppSettingsEntity> =
        container.settingsRepository.observe()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettingsEntity())

    private val sessionState = kotlinx.coroutines.flow.MutableStateFlow(MeasurementUiState())

    val measurementState: StateFlow<MeasurementUiState> =
        sessionState.flatMapLatest { state ->
            val id = state.activeSessionId
            if (id == null) flowOf(state) else {
                container.measurementRepository.observeSides(id).map { sides ->
                    state.copy(sides = sides)
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MeasurementUiState())

    val activeProfiles: StateFlow<List<WheelPairProfileEntity>> =
        sessionState.map { it.selectedLocomotiveId }
            .flatMapLatest { id ->
                if (id == null) flowOf(emptyList()) else container.locomotiveRepository.observeProfiles(id)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun saveLocomotive(id: Long = 0, series: String, number: String, count: Int, comment: String) {
        viewModelScope.launch {
            container.locomotiveRepository.saveLocomotive(id, series, number, count, comment)
        }
    }

    fun deleteLocomotive(id: Long) {
        viewModelScope.launch { container.locomotiveRepository.deleteLocomotive(id) }
    }

    fun selectLocomotive(id: Long?) {
        sessionState.value = sessionState.value.copy(selectedLocomotiveId = id)
    }

    fun updateRepairType(value: String) {
        sessionState.value = sessionState.value.copy(repairType = value)
    }

    fun updateSyncServerUrl(value: String) {
        viewModelScope.launch {
            container.settingsRepository.updateSyncServerUrl(value)
        }
    }

    fun updateSyncPassword(value: String) {
        viewModelScope.launch {
            container.settingsRepository.updateSyncPassword(value)
        }
    }

    fun updateDate(value: String) {
        sessionState.value = sessionState.value.copy(measurementDate = value)
    }

    fun setMeasurementMode(mode: MeasurementInputMode) {
        sessionState.value = sessionState.value.copy(inputMode = mode)
    }

    fun clearCurrentMeasurement() {
        val id = sessionState.value.activeSessionId ?: return
        viewModelScope.launch {
            if (container.measurementRepository.deleteMeasurement(id)) {
                sessionState.value = resetMeasurementState()
            }
        }
    }

    fun openArchiveMeasurement(id: String) {
        sessionState.value = sessionState.value.copy(
            selectedArchiveMeasurementId = id,
            selectedArchiveMeasurement = null,
        )
        viewModelScope.launch {
            val dto = runCatching { container.exportRepository.buildExport(id) }.getOrNull()
            sessionState.value = sessionState.value.copy(selectedArchiveMeasurement = dto)
        }
    }

    suspend fun buildArchiveMeasurement(id: String): MeasurementExportDto =
        container.exportRepository.buildExport(id)

    fun closeArchiveMeasurement() {
        sessionState.value = sessionState.value.copy(
            selectedArchiveMeasurementId = null,
            selectedArchiveMeasurement = null,
        )
    }

    fun openPendingMeasurement(id: String) {
        viewModelScope.launch {
            val session = container.measurementRepository.getSession(id) ?: return@launch
            sessionState.value = sessionState.value.copy(
                activeSessionId = session.id,
                selectedLocomotiveId = session.locomotiveId,
                selectedWheelPair = 1,
                selectedSide = WheelSide.LEFT,
                measurementDate = session.measurementDate,
                repairType = session.repairType,
                inputMode = MeasurementInputMode.MANUAL,
                voiceFlowState = VoiceFlowState.IDLE,
                voiceFlowResumeState = null,
                voiceMeasurementArmed = false,
                exportPreview = "",
                exportFileName = "",
                exportShareRequestNonce = 0L,
                archiveExportPreview = "",
                archiveExportFileName = "",
                archiveExportShareRequestNonce = 0L,
            )
        }
    }

    fun startMeasurement() {
        val locomotiveId = sessionState.value.selectedLocomotiveId ?: return
        viewModelScope.launch {
            val id = container.measurementRepository.startSession(
                locomotiveId = locomotiveId,
                measurementDate = sessionState.value.measurementDate,
                repairType = sessionState.value.repairType,
            )
            sessionState.value = sessionState.value.copy(
                activeSessionId = id,
                voiceFlowState = VoiceFlowState.IDLE,
                voiceFlowResumeState = null,
                voiceMeasurementArmed = false,
            )
        }
    }

    fun startNewMeasurement() {
        val locomotiveId = sessionState.value.selectedLocomotiveId ?: return
        val currentDraftId = sessionState.value.activeSessionId
        viewModelScope.launch {
            currentDraftId?.let { container.measurementRepository.deleteMeasurement(it) }
            val id = container.measurementRepository.startSession(
                locomotiveId = locomotiveId,
                measurementDate = sessionState.value.measurementDate,
                repairType = sessionState.value.repairType,
            )
            sessionState.value = sessionState.value.copy(
                activeSessionId = id,
                selectedWheelPair = 1,
                selectedSide = WheelSide.LEFT,
                voiceMeasurementArmed = false,
                voiceFlowState = VoiceFlowState.IDLE,
                voiceFlowResumeState = null,
                exportPreview = "",
                exportFileName = "",
                lastSavedVoiceText = "",
                lastValidationText = "",
            )
        }
    }

    fun selectSide(wheelPair: Int, side: WheelSide) {
        sessionState.value = sessionState.value.copy(selectedWheelPair = wheelPair.coerceAtLeast(1), selectedSide = side)
    }

    fun saveCurrentSide(
        flangeThickness: Double?,
        flangeWear: Double?,
        flangeSteepness: Double?,
        bandageThickness: Double?,
        kcLeft: Double? = null,
        kcRight: Double? = null,
    ) {
        saveSide(
            wheelPairNumber = sessionState.value.selectedWheelPair,
            side = sessionState.value.selectedSide,
            flangeThickness = flangeThickness,
            flangeWear = flangeWear,
            flangeSteepness = flangeSteepness,
            bandageThickness = bandageThickness,
            kcLeft = kcLeft,
            kcRight = kcRight,
            advance = true,
        )
    }

    fun saveCurrentWheelPair(
        leftFlangeThickness: Double?,
        leftFlangeWear: Double?,
        leftFlangeSteepness: Double?,
        leftBandageThickness: Double?,
        rightFlangeThickness: Double?,
        rightFlangeWear: Double?,
        rightFlangeSteepness: Double?,
        rightBandageThickness: Double?,
        kcLeft: Double? = null,
        kcRight: Double? = null,
    ): Boolean {
        val state = sessionState.value
        val sessionId = state.activeSessionId ?: return false
        val isLastWheelPair = state.selectedWheelPair >= selectedWheelPairLimit()
        validateSideValues(leftFlangeThickness, leftFlangeWear, leftFlangeSteepness, leftBandageThickness)?.let { message ->
            sessionState.value = state.copy(lastValidationText = message)
            return false
        }
        validateSideValues(rightFlangeThickness, rightFlangeWear, rightFlangeSteepness, rightBandageThickness)?.let { message ->
            sessionState.value = state.copy(lastValidationText = message)
            return false
        }
        val leftDiameter = if (kcLeft != null && leftBandageThickness != null) (kcLeft + leftBandageThickness * 2).roundToInt().toDouble() else null
        val rightDiameter = if (kcRight != null && rightBandageThickness != null) (kcRight + rightBandageThickness * 2).roundToInt().toDouble() else null
        viewModelScope.launch {
            container.measurementRepository.saveSideValue(
                sessionId = sessionId,
                wheelPairNumber = state.selectedWheelPair,
                side = WheelSide.LEFT,
                flangeThickness = leftFlangeThickness,
                flangeWear = leftFlangeWear,
                flangeSteepness = leftFlangeSteepness,
                bandageThickness = leftBandageThickness,
                bandageDiameter = leftDiameter,
            )
            container.measurementRepository.saveSideValue(
                sessionId = sessionId,
                wheelPairNumber = state.selectedWheelPair,
                side = WheelSide.RIGHT,
                flangeThickness = rightFlangeThickness,
                flangeWear = rightFlangeWear,
                flangeSteepness = rightFlangeSteepness,
                bandageThickness = rightBandageThickness,
                bandageDiameter = rightDiameter,
            )
            if (isLastWheelPair) {
                sessionState.value = state.copy(lastValidationText = "")
            } else {
                sessionState.value = nextSelection(state.selectedWheelPair, WheelSide.RIGHT).copy(lastValidationText = "")
            }
        }
        return true
    }

    fun clearCurrentWheelPair() {
        val state = sessionState.value
        val sessionId = state.activeSessionId ?: return
        viewModelScope.launch {
            container.measurementRepository.clearWheelPair(sessionId, state.selectedWheelPair)
            sessionState.value = state.copy(lastValidationText = "")
        }
    }

    fun saveManualSide(
        side: WheelSide,
        flangeThickness: Double?,
        flangeWear: Double?,
        flangeSteepness: Double?,
        bandageThickness: Double?,
        kcLeft: Double? = null,
        kcRight: Double? = null,
        advance: Boolean = true,
    ) {
        saveSide(
            wheelPairNumber = sessionState.value.selectedWheelPair,
            side = side,
            flangeThickness = flangeThickness,
            flangeWear = flangeWear,
            flangeSteepness = flangeSteepness,
            bandageThickness = bandageThickness,
            kcLeft = kcLeft,
            kcRight = kcRight,
            advance = advance,
        )
    }

    private fun saveSide(
        wheelPairNumber: Int,
        side: WheelSide,
        flangeThickness: Double?,
        flangeWear: Double?,
        flangeSteepness: Double?,
        bandageThickness: Double?,
        kcLeft: Double? = null,
        kcRight: Double? = null,
        advance: Boolean,
    ) {
        val state = sessionState.value
        val sessionId = state.activeSessionId ?: return
        validateSideValues(flangeThickness, flangeWear, flangeSteepness, bandageThickness)?.let { message ->
            sessionState.value = state.copy(selectedWheelPair = wheelPairNumber, selectedSide = side, lastValidationText = message)
            return
        }
        val kc = if (side == WheelSide.LEFT) kcLeft else kcRight
        val bandageDiameter = if (kc != null && bandageThickness != null) {
            (kc + bandageThickness * 2).roundToInt().toDouble()
        } else {
            null
        }
        viewModelScope.launch {
            container.measurementRepository.saveSideValue(
                sessionId = sessionId,
                wheelPairNumber = wheelPairNumber,
                side = side,
                flangeThickness = flangeThickness,
                flangeWear = flangeWear,
                flangeSteepness = flangeSteepness,
                bandageThickness = bandageThickness,
                bandageDiameter = bandageDiameter,
            )
            val nextState = if (advance) {
                nextSelection(wheelPairNumber, side)
            } else {
                sessionState.value.copy(selectedWheelPair = wheelPairNumber, selectedSide = side)
            }
            sessionState.value = nextState.copy(lastValidationText = "")
        }
    }

    fun goNext() {
        val state = sessionState.value
        sessionState.value = nextSelection(state.selectedWheelPair, state.selectedSide)
    }

    fun finishMeasurement() {
        val id = sessionState.value.activeSessionId ?: return
        viewModelScope.launch {
            container.measurementRepository.finishSession(id)
            sessionState.value = resetMeasurementState()
        }
    }

    fun parseVoice(text: String, confidence: Float = 1f): VoiceParseResult {
        val result = parser.parse(text, confidence)
        sessionState.value = sessionState.value.copy(lastHeardText = result.normalizedText)
        if (result.command is VoiceCommand.Unknown) {
            sessionState.value = sessionState.value.copy(pendingVoice = null)
            announceVoice(result.normalizedText)
        } else {
            sessionState.value = sessionState.value.copy(pendingVoice = null)
            applyVoiceCommand(result.command)
        }
        return result
    }

    fun updateVoiceRecognizedText(text: String) {
        val normalized = text.trim()
        if (normalized.isBlank()) return
        sessionState.value = sessionState.value.copy(lastHeardText = normalized)
    }

    fun confirmVoiceCommand() {
        sessionState.value = sessionState.value.copy(pendingVoice = null)
    }

    fun dismissVoiceCommand() {
        sessionState.value = sessionState.value.copy(pendingVoice = null)
    }

    fun applyVoiceCommand(command: VoiceCommand) {
        when (command) {
            VoiceCommand.StartMeasurementFlow -> {
                if (sessionState.value.activeSessionId == null) {
                    announceVoice("Сначала выберите локомотив")
                    return
                }
                sessionState.value = sessionState.value.copy(
                    voiceFlowState = VoiceFlowState.WAIT_SIDE,
                    voiceFlowResumeState = null,
                    voiceMeasurementArmed = true,
                )
            }
            is VoiceCommand.StartMeasurementWithSide -> {
                if (sessionState.value.activeSessionId == null) {
                    announceVoice("Сначала выберите локомотив")
                    return
                }
                selectSide(command.wheelPairNumber ?: sessionState.value.selectedWheelPair, command.side)
                sessionState.value = sessionState.value.copy(
                    voiceFlowState = VoiceFlowState.WAIT_VALUES,
                    voiceFlowResumeState = null,
                    voiceMeasurementArmed = true,
                )
                announceVoice("готов")
            }
            is VoiceCommand.SelectSide -> {
                val flowState = sessionState.value.voiceFlowState
                if (flowState != VoiceFlowState.IDLE) {
                    selectSide(command.wheelPairNumber ?: sessionState.value.selectedWheelPair, command.side)
                    sessionState.value = sessionState.value.copy(
                        voiceFlowState = VoiceFlowState.WAIT_VALUES,
                        voiceMeasurementArmed = true,
                    )
                    announceVoice("готов")
                } else {
                    announceVoice("Скажите новый замер")
                }
            }
            is VoiceCommand.SetValue -> applyVoiceValue(command.field, command.value)
            is VoiceCommand.FillSide -> applyVoiceFillSide(command)
            is VoiceCommand.FillCurrentSide -> applyVoiceFillCurrentSide(command)
            VoiceCommand.Next -> {
                goNext()
                announceVoiceWithResult("дальше")
            }
            VoiceCommand.FinishMeasurements -> {
                finishMeasurement()
                announceVoiceWithResult("замер завершен")
            }
            VoiceCommand.SendBluetooth, VoiceCommand.SendEmail, VoiceCommand.SaveFile -> {
                buildExportPreview()
                announceVoiceWithResult("экспорт подготовлен")
            }
            is VoiceCommand.Unknown -> Unit
        }
    }

    private fun applyVoiceFillCurrentSide(command: VoiceCommand.FillCurrentSide) {
        val state = sessionState.value
        when (state.voiceFlowState) {
            VoiceFlowState.IDLE -> {
                announceVoice("Скажите новый замер")
                return
            }
            VoiceFlowState.WAIT_SIDE -> {
                announceVoice("Сначала скажите сторону КП")
                return
            }
            VoiceFlowState.WAIT_VALUES, VoiceFlowState.SPEAKING -> Unit
        }
        if (state.activeSessionId == null) {
            announceVoice("Скажите новый замер")
            return
        }
        applyVoiceFillSide(
            VoiceCommand.FillSide(
                wheelPairNumber = null,
                side = state.selectedSide,
                flangeThickness = command.flangeThickness,
                flangeWear = command.flangeWear,
                flangeSteepness = command.flangeSteepness,
                bandageThickness = command.bandageThickness,
            )
        )
    }

    private fun applyVoiceFillSide(command: VoiceCommand.FillSide) {
        val side = command.side
        val targetPair = command.wheelPairNumber ?: sessionState.value.selectedWheelPair
        val flowState = sessionState.value.voiceFlowState
        if (flowState == VoiceFlowState.IDLE) {
            announceVoice("Скажите новый замер")
            return
        }
        sessionState.value = sessionState.value.copy(selectedWheelPair = targetPair, selectedSide = side)
        val state = sessionState.value
        val sessionId = state.activeSessionId ?: return
        validateSideValues(
            command.flangeThickness,
            command.flangeWear,
            command.flangeSteepness,
            command.bandageThickness,
        )?.let { message ->
            sessionState.value = state.copy(
                lastSavedVoiceText = "Не записано КП $targetPair ${side.label}",
                lastValidationText = message,
                voiceMeasurementArmed = true,
            )
            announceVoiceWithResult(message)
            return
        }
        viewModelScope.launch {
            container.measurementRepository.saveSideValue(
                sessionId = sessionId,
                wheelPairNumber = targetPair,
                side = side,
                flangeThickness = command.flangeThickness,
                flangeWear = command.flangeWear,
                flangeSteepness = command.flangeSteepness,
                bandageThickness = command.bandageThickness,
                bandageDiameter = null,
            )
            sessionState.value = sessionState.value.copy(
                selectedWheelPair = targetPair,
                selectedSide = side,
                lastSavedVoiceText = "Записано КП $targetPair ${side.label}: ${command.flangeThickness}, ${command.flangeWear}, ${command.flangeSteepness}, ${command.bandageThickness}",
                lastValidationText = "",
                voiceMeasurementArmed = false,
                voiceFlowState = VoiceFlowState.IDLE,
                voiceFlowResumeState = null,
            )
            announceVoiceWithResult("записано")
        }
    }

    private fun applyVoiceValue(field: MeasurementField, value: Double) {
        val current = currentSide()
        val flangeThickness = if (field == MeasurementField.FLANGE_THICKNESS) value else current?.flangeThickness
        val flangeWear = if (field == MeasurementField.FLANGE_WEAR) value else current?.flangeWear
        val flangeSteepness = if (field == MeasurementField.FLANGE_STEEPNESS) value else current?.flangeSteepness
        val bandageThickness = if (field == MeasurementField.BANDAGE_THICKNESS) value else current?.bandageThickness
        saveSideWithoutAdvance(flangeThickness, flangeWear, flangeSteepness, bandageThickness)
    }

    private fun saveSideWithoutAdvance(
        flangeThickness: Double?,
        flangeWear: Double?,
        flangeSteepness: Double?,
        bandageThickness: Double?,
    ) {
        val state = sessionState.value
        val sessionId = state.activeSessionId ?: return
        validateSideValues(flangeThickness, flangeWear, flangeSteepness, bandageThickness)?.let { message ->
            sessionState.value = state.copy(lastValidationText = message)
            announceVoiceWithResult(message)
            return
        }
        viewModelScope.launch {
            container.measurementRepository.saveSideValue(
                sessionId = sessionId,
                wheelPairNumber = state.selectedWheelPair,
                side = state.selectedSide,
                flangeThickness = flangeThickness,
                flangeWear = flangeWear,
                flangeSteepness = flangeSteepness,
                bandageThickness = bandageThickness,
                bandageDiameter = null,
            )
            sessionState.value = sessionState.value.copy(
                lastValidationText = "",
                voiceMeasurementArmed = false,
                voiceFlowState = VoiceFlowState.IDLE,
                voiceFlowResumeState = null,
            )
            announceVoiceWithResult("записано")
        }
    }

    private fun validateSideValues(
        flangeThickness: Double?,
        flangeWear: Double?,
        flangeSteepness: Double?,
        bandageThickness: Double?,
    ): String? {
        if (flangeThickness != null && flangeThickness !in 20.0..33.0) {
            return "Гребень ошибка"
        }
        if (flangeWear != null && flangeWear !in 0.0..10.0) {
            return "Прокат ошибка"
        }
        if (flangeSteepness != null && flangeSteepness !in 5.0..15.0) {
            return "Крутизна ошибка"
        }
        if (bandageThickness != null && bandageThickness < 30.0) {
            return "Бандаж ошибка"
        }
        return null
    }

    private fun currentSide(): WheelSideMeasurementEntity? =
        measurementState.value.sides.firstOrNull {
            it.wheelPairNumber == sessionState.value.selectedWheelPair && it.side == sessionState.value.selectedSide
        }

    private fun selectedWheelPairLimit(): Int =
        locomotives.value.firstOrNull { it.id == sessionState.value.selectedLocomotiveId }
            ?.wheelPairCount
            ?.coerceAtLeast(1)
            ?: Int.MAX_VALUE

    private fun nextSelection(wheelPairNumber: Int, side: WheelSide): MeasurementUiState {
        val limit = selectedWheelPairLimit()
        val state = sessionState.value
        return if (side == WheelSide.LEFT) {
            state.copy(selectedWheelPair = wheelPairNumber, selectedSide = WheelSide.RIGHT)
        } else if (wheelPairNumber < limit) {
            state.copy(selectedWheelPair = wheelPairNumber + 1, selectedSide = WheelSide.LEFT)
        } else {
            state.copy(selectedWheelPair = wheelPairNumber, selectedSide = WheelSide.RIGHT)
        }
    }

    private fun resetMeasurementState(): MeasurementUiState {
        val state = sessionState.value
        return state.copy(
            activeSessionId = null,
            selectedWheelPair = 1,
            selectedSide = WheelSide.LEFT,
            sides = emptyList(),
            pendingVoice = null,
            exportPreview = "",
            exportFileName = "",
            exportShareRequestNonce = 0L,
            voiceFlowState = VoiceFlowState.IDLE,
            voiceFlowResumeState = null,
            voiceMeasurementArmed = false,
            lastHeardText = "",
            lastSavedVoiceText = "",
            lastValidationText = "",
            voiceAnnouncement = "",
            voiceAnnouncementNonce = 0L,
        )
    }

    fun deleteMeasurement(id: String) {
        viewModelScope.launch {
            container.measurementRepository.deleteMeasurement(id)
        }
    }

    fun buildExportPreview() {
        val id = sessionState.value.activeSessionId ?: return
        viewModelScope.launch {
            val dto = container.exportRepository.buildExport(id)
            val json = container.exportRepository.exportJson(id)
            sessionState.value = sessionState.value.copy(exportPreview = json, exportFileName = dto.suggestedFileName())
        }
    }

    fun requestExportShare() {
        val id = sessionState.value.activeSessionId ?: return
        requestExportShare(id)
    }

    fun requestExportShare(measurementId: String) {
        viewModelScope.launch {
            if (measurementId == sessionState.value.activeSessionId) {
                container.measurementRepository.finishSession(measurementId)
                container.measurementRepository.markExported(measurementId)
            }
            val dto = container.exportRepository.buildExport(measurementId)
            val json = container.exportRepository.exportJson(measurementId)
            val isActiveDraft = measurementId == sessionState.value.activeSessionId
            val resetState = if (isActiveDraft) resetMeasurementState() else sessionState.value
            sessionState.value = resetState.copy(
                exportPreview = json,
                exportFileName = dto.suggestedFileName(),
                exportShareRequestNonce = System.currentTimeMillis(),
            )
        }
    }

    fun completeExport() {
        val id = sessionState.value.activeSessionId ?: return
        viewModelScope.launch {
            container.measurementRepository.finishSession(id)
            container.measurementRepository.markExported(id)
            sessionState.value = sessionState.value.copy(activeSessionId = null)
        }
    }

    fun buildArchiveExportPreview() {
        viewModelScope.launch {
            val dto = container.exportRepository.buildArchiveExport()
            val json = container.exportRepository.exportArchiveJson()
            sessionState.value = sessionState.value.copy(
                archiveExportPreview = json,
                archiveExportFileName = dto.suggestedFileName(),
            )
        }
    }

    fun requestArchiveExportShare() {
        viewModelScope.launch {
            val dto = container.exportRepository.buildArchiveExport()
            val json = container.exportRepository.exportArchiveJson()
            sessionState.value = sessionState.value.copy(
                archiveExportPreview = json,
                archiveExportFileName = dto.suggestedFileName(),
                archiveExportShareRequestNonce = System.currentTimeMillis(),
            )
        }
    }

    fun syncWithServer(serverBaseUrl: String, password: String) {
        viewModelScope.launch {
            sessionState.value = sessionState.value.copy(syncStatusMessage = "Синхронизация...")
            try {
                val result = container.serverSyncRepository.sync(
                    serverBaseUrl = serverBaseUrl,
                    password = password,
                )
                sessionState.value = sessionState.value.copy(
                    syncStatusMessage = buildString {
                        append("Синхронизация завершена. ")
                        append("Отправлено: черновики ${result.pendingPushed}, замеры ${result.archivePushed}. ")
                        append("Получено: справочник ${result.referencePulled}, архив ${result.archivePulled}.")
                    }
                        .plus(
                            if (result.referenceConflicts.isNotEmpty()) {
                                "\nЕсть конфликты справочника: ${result.referenceConflicts.size}. " +
                                    "Можно отправить локальные изменения на сервер или оставить серверную версию."
                            } else {
                                ""
                            }
                        ),
                    syncConflictSummary = if (result.referenceConflicts.isNotEmpty()) {
                        result.referenceConflicts.joinToString(separator = "\n") { conflict ->
                            "${conflict.series} ${conflict.number}: ${conflict.reason} " +
                                "(локально ${conflict.localUpdatedAt.toSyncTime()}, сервер ${conflict.serverUpdatedAt.toSyncTime()})"
                        }
                    } else {
                        ""
                    }
                )
            } catch (error: Exception) {
                sessionState.value = sessionState.value.copy(
                    syncStatusMessage = "Ошибка синхронизации: ${error.message ?: error}",
                    syncConflictSummary = ""
                )
            }
        }
    }

    fun pushLocalReferenceToServer(serverBaseUrl: String, password: String) {
        viewModelScope.launch {
            sessionState.value = sessionState.value.copy(syncStatusMessage = "Отправка локального справочника...")
            try {
                val count = container.serverSyncRepository.pushLocalReferenceSnapshot(serverBaseUrl, password)
                sessionState.value = sessionState.value.copy(
                    syncStatusMessage = "Локальный справочник отправлен на сервер: ${count} локомотивов.",
                    syncConflictSummary = ""
                )
            } catch (error: Exception) {
                sessionState.value = sessionState.value.copy(
                    syncStatusMessage = "Ошибка отправки справочника: ${error.message ?: error}"
                )
            }
        }
    }

    fun clearSyncConflictSummary() {
        sessionState.value = sessionState.value.copy(syncConflictSummary = "")
    }

    fun requestFullBackupShare() {
        viewModelScope.launch {
            runCatching {
                val dto = container.backupRepository.buildBackup()
                val json = container.backupRepository.exportBackupJson()
                sessionState.value = sessionState.value.copy(
                    backupPreview = json,
                    backupFileName = dto.suggestedFileName(),
                    backupShareRequestNonce = System.currentTimeMillis(),
                    backupStatusMessage = "Резервная копия создана"
                )
            }.onFailure {
                sessionState.value = sessionState.value.copy(
                    backupStatusMessage = "Ошибка резервной копии: ${it.message}"
                )
            }
        }
    }

    fun restoreFullBackup(text: String) {
        viewModelScope.launch {
            val message = runCatching {
                val backup = container.backupRepository.restoreBackupJson(text)
                restoreLatestDraft()
                "Восстановлено: локомотивов ${backup.locomotives.size}, КП ${backup.wheelPairProfiles.size}, замеров ${backup.measurementSessions.size}"
            }.getOrElse { "Ошибка восстановления: ${it.message}" }
            sessionState.value = sessionState.value.copy(backupStatusMessage = message)
        }
    }

    fun validateImportJson(text: String) {
        loadImportPreview(text)
    }

    fun loadImportPreview(text: String) {
        runCatching {
            val envelope = container.exportRepository.parseImportEnvelope(text)
            pendingImport = envelope
            val hasMeasurement = envelope.measurement != null
            val hasReference = envelope.referenceData != null
            val hasArchive = envelope.archiveData != null
            val measurementSummary = envelope.measurement?.let {
                "${it.locomotive.series} ${it.locomotive.number}, КП: ${it.wheelPairs.size}"
            }.orEmpty()
            val referenceSummary = envelope.referenceData?.let {
                "Локомотивов: ${it.locomotives.size}"
            }.orEmpty()
            val archiveSummary = envelope.archiveData?.let {
                "Архивных замеров: ${it.archive.size}"
            }.orEmpty()
            val kind = when {
                hasArchive -> ImportPayloadKind.ARCHIVE_DATA
                hasMeasurement && hasReference -> ImportPayloadKind.MEASUREMENT
                hasMeasurement -> ImportPayloadKind.MEASUREMENT
                hasReference -> ImportPayloadKind.REFERENCE_DATA
                else -> ImportPayloadKind.NONE
            }
            if (kind == ImportPayloadKind.NONE) {
                throw IllegalArgumentException("Не найден объект замера или справочника в JSON")
            }
            sessionState.value = sessionState.value.copy(
                importCheckMessage = buildString {
                    append("Файл распознан: ")
                    when {
                        hasArchive -> append("архив замеров")
                        hasMeasurement && hasReference -> append("замер и справочник")
                        hasMeasurement -> append("замер")
                        hasReference -> append("справочник локомотивов")
                    }
                },
                importUi = ImportUiState(
                    kind = kind,
                    summary = when {
                        hasArchive -> archiveSummary
                        hasMeasurement && hasReference -> "В файле есть замер и справочник."
                        else -> measurementSummary.ifBlank { referenceSummary }
                    },
                    measurementSummary = measurementSummary,
                    referenceSummary = referenceSummary,
                    archiveSummary = archiveSummary,
                    hasMeasurement = hasMeasurement,
                    hasReference = hasReference,
                    hasArchive = hasArchive,
                    importMeasurementArchive = true,
                    importMeasurementLocomotives = true,
                    importMeasurementWheelPairs = true,
                    importReferenceLocomotives = true,
                    importReferenceWheelPairs = true,
                    importArchiveLocomotives = true,
                    importArchiveWheelPairs = true,
                    importArchiveMeasurements = true,
                )
            )
        }.getOrElse { error ->
            pendingImport = null
            sessionState.value = sessionState.value.copy(
                importCheckMessage = "Ошибка импорта: ${error.message}",
                importUi = ImportUiState()
            )
        }
    }

    fun setImportMeasurementLocomotives(enabled: Boolean) {
        sessionState.value = sessionState.value.copy(importUi = sessionState.value.importUi.copy(importMeasurementLocomotives = enabled))
    }

    fun setImportMeasurementWheelPairs(enabled: Boolean) {
        sessionState.value = sessionState.value.copy(importUi = sessionState.value.importUi.copy(importMeasurementWheelPairs = enabled))
    }

    fun setImportMeasurementArchive(enabled: Boolean) {
        sessionState.value = sessionState.value.copy(importUi = sessionState.value.importUi.copy(importMeasurementArchive = enabled))
    }

    fun setImportReferenceLocomotives(enabled: Boolean) {
        sessionState.value = sessionState.value.copy(importUi = sessionState.value.importUi.copy(importReferenceLocomotives = enabled))
    }

    fun setImportReferenceWheelPairs(enabled: Boolean) {
        sessionState.value = sessionState.value.copy(importUi = sessionState.value.importUi.copy(importReferenceWheelPairs = enabled))
    }

    fun setImportArchiveLocomotives(enabled: Boolean) {
        sessionState.value = sessionState.value.copy(importUi = sessionState.value.importUi.copy(importArchiveLocomotives = enabled))
    }

    fun setImportArchiveWheelPairs(enabled: Boolean) {
        sessionState.value = sessionState.value.copy(importUi = sessionState.value.importUi.copy(importArchiveWheelPairs = enabled))
    }

    fun setImportArchiveMeasurements(enabled: Boolean) {
        sessionState.value = sessionState.value.copy(importUi = sessionState.value.importUi.copy(importArchiveMeasurements = enabled))
    }

    fun importJson() {
        val envelope = pendingImport ?: run {
            sessionState.value = sessionState.value.copy(importCheckMessage = "Сначала выберите JSON-файл.")
            return
        }
        val importUi = sessionState.value.importUi
        viewModelScope.launch {
            val message = runCatching {
                val hasPendingDrafts = container.measurementRepository.hasPendingMeasurements()
                val parts = mutableListOf<String>()
                envelope.measurement?.let { measurement ->
                    val importMeasurementArchive = importUi.importMeasurementArchive && !hasPendingDrafts
                    if (importUi.importMeasurementLocomotives || importUi.importMeasurementWheelPairs || importMeasurementArchive) {
                        container.measurementRepository.importMeasurement(
                            measurement,
                            importLocomotive = importUi.importMeasurementLocomotives,
                            importWheelPairs = importUi.importMeasurementWheelPairs,
                            importArchive = importMeasurementArchive,
                        )
                        parts += "замер ${measurement.locomotive.series} ${measurement.locomotive.number}"
                    }
                }
                envelope.referenceData?.let { reference ->
                    if (importUi.importReferenceLocomotives || importUi.importReferenceWheelPairs) {
                        val imported = container.measurementRepository.importReferenceData(
                            reference,
                            importLocomotives = importUi.importReferenceLocomotives,
                            importWheelPairs = importUi.importReferenceWheelPairs,
                        )
                        parts += "справочник: $imported локомотивов"
                    }
                }
                envelope.archiveData?.let { archive ->
                    val importArchiveMeasurements = importUi.importArchiveMeasurements && !hasPendingDrafts
                    if (importUi.importArchiveLocomotives || importUi.importArchiveWheelPairs || importArchiveMeasurements) {
                        if (importArchiveMeasurements) {
                            container.measurementRepository.replaceImportedArchive()
                        }
                        archive.archive.forEach { measurement ->
                            runCatching {
                                container.measurementRepository.importMeasurement(
                                    measurement,
                                    importLocomotive = importUi.importArchiveLocomotives,
                                    importWheelPairs = importUi.importArchiveWheelPairs,
                                    importArchive = importArchiveMeasurements,
                                    archivePayload = true,
                                )
                            }
                        }
                        parts += "архив: ${archive.archive.size} замеров"
                    }
                }
                if (parts.isEmpty()) "Нечего импортировать." else "Импортировано: ${parts.joinToString(", ")}"
            }.getOrElse { "Ошибка импорта: ${it.message}" }
            sessionState.value = sessionState.value.copy(importCheckMessage = message)
        }
    }

    fun saveVoskModelUri(uri: String) {
        viewModelScope.launch { container.settingsRepository.updateVoskModelUri(uri) }
    }

    fun setKeepScreenOn(enabled: Boolean) {
        viewModelScope.launch { container.settingsRepository.updateKeepScreenOn(enabled) }
    }

    private fun restoreLatestDraft() {
        viewModelScope.launch {
            val draft = container.measurementRepository.getLatestDraftSession() ?: return@launch
            sessionState.value = sessionState.value.copy(
                activeSessionId = draft.id,
                selectedLocomotiveId = draft.locomotiveId,
                measurementDate = draft.measurementDate,
                repairType = draft.repairType,
            )
        }
    }

    fun onVoiceAnnouncementFinished() {
        val state = sessionState.value
        val resumeState = state.voiceFlowResumeState ?: VoiceFlowState.IDLE
        sessionState.value = state.copy(
            voiceFlowState = resumeState,
            voiceFlowResumeState = null,
            voiceMeasurementArmed = resumeState != VoiceFlowState.IDLE && resumeState != VoiceFlowState.SPEAKING,
        )
    }

    private fun announceVoiceWithResult(result: String) {
        val heard = sessionState.value.lastHeardText.ifBlank { return }
        announceVoice("$heard. $result")
    }

    private fun announceVoice(text: String) {
        val normalized = text.trim()
        if (normalized.isBlank()) return
        val currentState = sessionState.value.voiceFlowState
        val resumeState = if (currentState == VoiceFlowState.SPEAKING) {
            sessionState.value.voiceFlowResumeState
        } else {
            currentState
        }
        sessionState.value = sessionState.value.copy(
            voiceFlowState = VoiceFlowState.SPEAKING,
            voiceFlowResumeState = resumeState,
            voiceAnnouncement = normalized,
            voiceAnnouncementNonce = ++voiceAnnouncementToken,
        )
    }
}

private fun Long.toSyncTime(): String =
    if (this <= 0L) {
        "нет"
    } else {
        Instant.ofEpochMilli(this)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()
            .toString()
    }

class AppViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return AppViewModel(container) as T
    }
}
