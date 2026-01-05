package app.gamenative.ui.data

import com.winlator.container.Container
import com.winlator.core.KeyValueSet
import com.winlator.core.WineInfo
import com.winlator.inputcontrols.ControlElement

data class XServerState(
    // Wine/Container configuration
    var winStarted: Boolean = false,
    val dxwrapper: String = Container.DEFAULT_DXWRAPPER,
    val dxwrapperConfig: KeyValueSet? = null,
    val screenSize: String = Container.DEFAULT_SCREEN_SIZE,
    val wineInfo: WineInfo = WineInfo.MAIN_WINE_VERSION,
    val graphicsDriver: String = Container.DEFAULT_GRAPHICS_DRIVER,
    val graphicsDriverVersion: String = "",
    val audioDriver: String = Container.DEFAULT_AUDIO_DRIVER,

    // UI Control State
    val areControlsVisible: Boolean = false,
    val isEditMode: Boolean = false,
    val showQuickMenu: Boolean = false,
    val showPhysicalControllerDialog: Boolean = false,
    val showElementEditor: Boolean = false,
    val hasPhysicalController: Boolean = false,

    // Element Editor State
    val elementToEdit: ControlElement? = null,
    val elementPositionsSnapshot: Map<ControlElement, Pair<Int, Int>> = emptyMap(),
)
