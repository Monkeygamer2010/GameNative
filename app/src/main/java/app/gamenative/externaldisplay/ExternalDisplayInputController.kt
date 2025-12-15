package app.gamenative.externaldisplay

import android.app.Presentation
import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.Looper
import android.view.Display
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import com.winlator.widget.InputControlsView
import com.winlator.widget.TouchpadView
import com.winlator.winhandler.WinHandler
import com.winlator.xserver.XServer

class ExternalDisplayInputController(
    private val context: Context,
    private val xServer: XServer,
    private val winHandler: WinHandler,
    private val inputControlsViewProvider: () -> InputControlsView?,
    private val touchpadViewProvider: () -> TouchpadView?,
) {
    enum class Mode { OFF, TOUCHPAD, KEYBOARD, HYBRID }

    companion object {
        fun fromConfig(value: String?): Mode = when (value?.lowercase()) {
            "touchpad" -> Mode.TOUCHPAD
            "keyboard" -> Mode.KEYBOARD
            "hybrid" -> Mode.HYBRID
            else -> Mode.OFF
        }
    }

    private val displayManager = context.getSystemService(DisplayManager::class.java)
    private var presentation: ExternalInputPresentation? = null
    private var mode: Mode = Mode.OFF

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) {
            updatePresentation()
        }

        override fun onDisplayRemoved(displayId: Int) {
            if (presentation?.display?.displayId == displayId) {
                dismissPresentation()
            }
            updatePresentation()
        }

        override fun onDisplayChanged(displayId: Int) {
            if (presentation?.display?.displayId == displayId) {
                updatePresentation()
            }
        }
    }

    fun start() {
        displayManager?.registerDisplayListener(displayListener, Handler(Looper.getMainLooper()))
        updatePresentation()
    }

    fun stop() {
        dismissPresentation()
        try {
            displayManager?.unregisterDisplayListener(displayListener)
        } catch (_: Exception) {
        }
    }

    fun setMode(mode: Mode) {
        this.mode = mode
        updatePresentation()
    }

    private fun updatePresentation() {
        if (mode == Mode.OFF) {
            dismissPresentation()
            return
        }

        val targetDisplay = findPresentationDisplay() ?: run {
            dismissPresentation()
            return
        }

        val needsNewPresentation = presentation?.display?.displayId != targetDisplay.displayId
        if (presentation == null || needsNewPresentation) {
            dismissPresentation()
            presentation = ExternalInputPresentation(
                context = context,
                display = targetDisplay,
                mode = mode,
                xServer = xServer,
                winHandler = winHandler,
                inputControlsViewProvider = inputControlsViewProvider,
                touchpadViewProvider = touchpadViewProvider,
            )
            presentation?.show()
        } else {
            presentation?.updateMode(mode)
        }
    }

    private fun dismissPresentation() {
        presentation?.dismiss()
        presentation = null
    }

    private fun findPresentationDisplay(): Display? {
        // Required detection logic for external presentation displays
        return displayManager
            ?.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION)
            ?.firstOrNull { display ->
                display.displayId != Display.DEFAULT_DISPLAY && display.name != "HiddenDisplay"
            }
    }
}

private class ExternalInputPresentation(
    context: Context,
    display: Display,
    private var mode: ExternalDisplayInputController.Mode,
    private val xServer: XServer,
    private val winHandler: WinHandler,
    private val inputControlsViewProvider: () -> InputControlsView?,
    private val touchpadViewProvider: () -> TouchpadView?,
) : Presentation(context, display) {

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        renderContent()
    }

    fun updateMode(newMode: ExternalDisplayInputController.Mode) {
        if (mode != newMode) {
            mode = newMode
            renderContent()
        }
    }

    private fun renderContent() {
        when (mode) {
            ExternalDisplayInputController.Mode.TOUCHPAD -> {
                val pad = TouchpadView(context, xServer, false).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    setBackgroundColor(0xFF121212.toInt())
                    touchpadViewProvider()?.let { primary ->
                        setSimTouchScreen(primary.isSimTouchScreen)
                    }
                }
                setContentView(pad)
            }
            ExternalDisplayInputController.Mode.KEYBOARD -> {
                val keyboardView = ExternalKeyboardView(
                    context = context,
                    xServer = xServer,
                    winHandler = winHandler,
                    inputControlsViewProvider = inputControlsViewProvider,
                ).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                }
                setContentView(keyboardView)
            }
            ExternalDisplayInputController.Mode.HYBRID -> {
                val hybrid = HybridInputLayout(
                    context = context,
                    xServer = xServer,
                    winHandler = winHandler,
                    inputControlsViewProvider = inputControlsViewProvider,
                    touchpadViewProvider = touchpadViewProvider,
                )
                setContentView(hybrid)
            }
            else -> {
                setContentView(FrameLayout(context))
            }
        }
    }
}

private class HybridInputLayout(
    context: Context,
    xServer: XServer,
    winHandler: WinHandler,
    inputControlsViewProvider: () -> InputControlsView?,
    touchpadViewProvider: () -> TouchpadView?,
) : FrameLayout(context) {

    private val headerHeightPx = (64 * resources.displayMetrics.density).toInt()
    private val touchpad = TouchpadView(context, xServer, false).apply {
        layoutParams = LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )
        setBackgroundColor(0xFF121212.toInt())
        touchpadViewProvider()?.let { primary ->
            setSimTouchScreen(primary.isSimTouchScreen)
        }
    }
    private val keyboard = ExternalKeyboardView(
        context = context,
        xServer = xServer,
        winHandler = winHandler,
        inputControlsViewProvider = inputControlsViewProvider,
        autoShowImeOnTouch = false,
    ).apply {
        layoutParams = LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )
        visibility = View.GONE
    }

    private val header = View(context).apply {
        layoutParams = LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            headerHeightPx,
        )
        setBackgroundColor(0xFF1E1E1E.toInt())
        setOnClickListener { toggleKeyboard() }
    }

    init {
        addView(touchpad)
        addView(keyboard)
        addView(header)
    }

    private fun toggleKeyboard() {
        if (keyboard.visibility == View.VISIBLE) {
            keyboard.visibility = View.GONE
            touchpad.visibility = View.VISIBLE
            keyboard.hideIme()
        } else {
            keyboard.visibility = View.VISIBLE
            touchpad.visibility = View.GONE
            keyboard.requestFocus()
            keyboard.showIme()
        }
    }
}

private class ExternalKeyboardView(
    context: Context,
    private val xServer: XServer,
    private val winHandler: WinHandler,
    private val inputControlsViewProvider: () -> InputControlsView?,
    private val autoShowImeOnTouch: Boolean = true,
) : FrameLayout(context) {

    private val inputMethodManager = context.getSystemService(InputMethodManager::class.java)
    private val keyCharacterMap = KeyCharacterMap.load(KeyCharacterMap.VIRTUAL_KEYBOARD)

    init {
        isFocusable = true
        isFocusableInTouchMode = true
        setBackgroundColor(0xFF0F0F0F.toInt())
        post { if (autoShowImeOnTouch) showIme() }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (autoShowImeOnTouch) showIme()
    }

    override fun onCheckIsTextEditor(): Boolean = true

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection {
        outAttrs.inputType = EditorInfo.TYPE_CLASS_TEXT
        outAttrs.imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI or EditorInfo.IME_FLAG_NO_FULLSCREEN
        return KeyboardInputConnection(this, true)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val handledByControls = inputControlsViewProvider()?.onKeyEvent(event) == true
        if (handledByControls) return true
        return xServer.keyboard.onKeyEvent(event)
    }

    override fun onGenericMotionEvent(event: MotionEvent?): Boolean {
        event ?: return false
        val handledByControls = inputControlsViewProvider()?.onGenericMotionEvent(event) == true
        if (handledByControls) return true
        return winHandler.onGenericMotionEvent(event)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (autoShowImeOnTouch) showIme()
        return super.onTouchEvent(event)
    }

    fun showIme() {
        requestFocus()
        inputMethodManager?.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
    }

    fun hideIme() {
        inputMethodManager?.hideSoftInputFromWindow(windowToken, 0)
    }

    private inner class KeyboardInputConnection(
        targetView: View,
        fullEditor: Boolean,
    ) : BaseInputConnection(targetView, fullEditor) {
        private var composingText: String = ""

        override fun sendKeyEvent(event: KeyEvent): Boolean {
            return dispatchKeyEvent(event)
        }

        override fun commitText(text: CharSequence?, newCursorPosition: Int): Boolean {
            if (text.isNullOrEmpty()) return true
            if (text == "\n") {
                composingText = ""
                return true // Do not inject newline via commit; let raw enter key events handle it
            }
            val newText = text.toString()
            if (newText != composingText) {
                sendChars(newText)
            }
            composingText = ""
            return true
        }

        override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean {
            composingText = ""
            repeat(beforeLength) {
                dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
                dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL))
            }
            return true
        }

        override fun setComposingText(text: CharSequence?, newCursorPosition: Int): Boolean {
            if (text.isNullOrEmpty()) {
                composingText = ""
                return true
            }
            val newText = text.toString()
            when {
                newText.isEmpty() -> composingText = ""
                newText.length <= composingText.length && newText.startsWith(composingText.take(newText.length)) -> {
                    // IME is trimming composition (likely from backspace); do not resend characters
                    composingText = newText
                }
                newText.startsWith(composingText) -> {
                    val delta = newText.substring(composingText.length)
                    if (delta.isNotEmpty()) sendChars(delta)
                    composingText = newText
                }
                else -> {
                    sendChars(newText)
                    composingText = newText
                }
            }
            return true
        }

        override fun finishComposingText(): Boolean {
            composingText = ""
            return true
        }

        private fun sendChars(text: CharSequence) {
            val events = keyCharacterMap.getEvents(text.toString().toCharArray())
            if (events != null) {
                events.forEach { dispatchKeyEvent(it) }
            } else {
                text.forEach { ch ->
                    val down = KeyEvent(
                        0, 0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_UNKNOWN, 0, 0, 0, 0,
                        KeyEvent.FLAG_SOFT_KEYBOARD or KeyEvent.FLAG_KEEP_TOUCH_MODE, ch.code,
                    )
                    val up = KeyEvent(
                        0, 0, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_UNKNOWN, 0, 0, 0, 0,
                        KeyEvent.FLAG_SOFT_KEYBOARD or KeyEvent.FLAG_KEEP_TOUCH_MODE, ch.code,
                    )
                    dispatchKeyEvent(down)
                    dispatchKeyEvent(up)
                }
            }
        }
    }
}
