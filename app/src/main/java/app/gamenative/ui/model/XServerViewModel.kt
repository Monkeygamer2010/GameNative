package app.gamenative.ui.model

import androidx.lifecycle.ViewModel
import app.gamenative.ui.data.XServerState
import com.winlator.core.KeyValueSet
import com.winlator.core.WineInfo
import com.winlator.inputcontrols.ControlElement
import com.winlator.inputcontrols.ControllerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import timber.log.Timber

@HiltViewModel
class XServerViewModel @Inject constructor() : ViewModel() {

    private val _state = MutableStateFlow(XServerState())
    val state: StateFlow<XServerState> = _state.asStateFlow()

    init {
        // Check for physical controller on init
        val controllerManager = ControllerManager.getInstance()
        controllerManager.scanForDevices()
        val hasController = controllerManager.detectedDevices.isNotEmpty()
        _state.update { it.copy(hasPhysicalController = hasController) }
    }

    // Wine/Container Configuration Setters

    fun setDxwrapper(dxwrapper: String) {
        _state.update { it.copy(dxwrapper = dxwrapper) }
    }

    fun setDxwrapperConfig(dxwrapperConfig: KeyValueSet?) {
        Timber.d("Setting dxwrapperConfig to $dxwrapperConfig")
        _state.update { it.copy(dxwrapperConfig = dxwrapperConfig) }
    }

    fun setWineInfo(wineInfo: WineInfo) {
        _state.update { it.copy(wineInfo = wineInfo) }
    }

    fun setWinStarted(started: Boolean) {
        _state.update { it.copy(winStarted = started) }
    }

    // UI State Setters

    fun setControlsVisible(visible: Boolean) {
        _state.update { it.copy(areControlsVisible = visible) }
    }

    fun toggleControlsVisible() {
        _state.update { it.copy(areControlsVisible = !it.areControlsVisible) }
    }

    fun setEditMode(enabled: Boolean) {
        _state.update { it.copy(isEditMode = enabled) }
    }

    fun setShowQuickMenu(show: Boolean) {
        _state.update { it.copy(showQuickMenu = show) }
    }

    fun setShowPhysicalControllerDialog(show: Boolean) {
        _state.update { it.copy(showPhysicalControllerDialog = show) }
    }

    fun setShowElementEditor(show: Boolean) {
        _state.update { it.copy(showElementEditor = show) }
    }

    fun setElementToEdit(element: ControlElement?) {
        _state.update { it.copy(elementToEdit = element) }
    }

    // Edit Mode Operations

    fun enterEditMode(currentElements: List<ControlElement>) {
        val snapshot = mutableMapOf<ControlElement, Pair<Int, Int>>()
        currentElements.forEach { element ->
            snapshot[element] = Pair(element.getX().toInt(), element.getY().toInt())
        }
        _state.update {
            it.copy(
                isEditMode = true,
                elementPositionsSnapshot = snapshot,
            )
        }
    }

    fun exitEditMode(saveChanges: Boolean) {
        if (!saveChanges) {
            // Restore positions from snapshot
            val snapshot = _state.value.elementPositionsSnapshot
            snapshot.forEach { (element, position) ->
                element.setX(position.first)
                element.setY(position.second)
            }
        }
        _state.update {
            it.copy(
                isEditMode = false,
                elementPositionsSnapshot = emptyMap(),
            )
        }
    }

    // State Initialization

    fun initializeFromContainer(
        graphicsDriver: String,
        graphicsDriverVersion: String,
        audioDriver: String,
        dxwrapper: String,
        dxwrapperConfig: KeyValueSet?,
        screenSize: String,
    ) {
        _state.update {
            it.copy(
                graphicsDriver = graphicsDriver,
                graphicsDriverVersion = graphicsDriverVersion,
                audioDriver = audioDriver,
                dxwrapper = dxwrapper,
                dxwrapperConfig = dxwrapperConfig,
                screenSize = screenSize,
            )
        }
    }
}
