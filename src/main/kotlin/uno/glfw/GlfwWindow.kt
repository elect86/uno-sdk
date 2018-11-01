package uno.glfw

import glm_.BYTES
import glm_.bool
import glm_.f
import glm_.i
import glm_.vec2.Vec2
import glm_.vec2.Vec2d
import glm_.vec2.Vec2i
import glm_.vec4.Vec4i
import gln.debug.GlDebugSeverity
import gln.debug.GlDebugSource
import gln.debug.GlDebugType
import kool.stak
import org.lwjgl.glfw.*
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL43C
import org.lwjgl.opengl.GLUtil
import org.lwjgl.system.Callback
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil.*
import uno.kotlin.getOrfirst
import java.nio.ByteBuffer
import java.nio.FloatBuffer

/**
 * Created by GBarbieri on 24.04.2017.
 */


/*  TODO
    icon
    glfwGetJoystickHats, GLFW_JOYSTICK_HAT_BUTTONS
    glfwSetJoystickUserPointer
    glfwSetMonitorUserPointer
    glfwSetWindowMaximizeCallback
    glfwGetKeyScancode
    glfwGetWindowContentScale, glfwGetMonitorContentScale and glfwSetWindowContentScaleCallback
    glfwGetGamepadState function, GLFW_GAMEPAD_* and GLFWgamepadstate
    glfwGetJoystickGUID
    glfwGetGamepadName
    glfwJoystickIsGamepad
    glfwUpdateGamepadMappings
 */

open class GlfwWindow(var handle: GlfwWindowHandle) {

    constructor(windowSize: Vec2i,
                title: String,
                monitor: GlfwMonitor = NULL,
                position: Vec2i = Vec2i(Int.MIN_VALUE),
                installCallbacks: Boolean = true) : this(windowSize.x, windowSize.y, title, monitor, position, installCallbacks)

    constructor(x: Int,
                title: String,
                monitor: GlfwMonitor = NULL,
                position: Vec2i = Vec2i(Int.MIN_VALUE),
                installCallbacks: Boolean = true) : this(x, x, title, monitor, position, installCallbacks)

    constructor(width: Int, height: Int,
                title: String,
                monitor: GlfwMonitor = NULL,
                position: Vec2i = Vec2i(Int.MIN_VALUE),
                installCallbacks: Boolean = true) : this(glfwCreateWindow(width, height, title, monitor, NULL)) {

        this.title = title

        if (position != Vec2i(Int.MIN_VALUE))
            glfwSetWindowPos(handle, position.x, position.y)

        if (installCallbacks) {
            glfwSetCharCallback(handle, nCharCallback)
            glfwSetCursorPosCallback(handle, nCursorPosCallback)
            glfwSetFramebufferSizeCallback(handle, nFramebufferSizeCallback)
            glfwSetKeyCallback(handle, nKeyCallback)
            glfwSetMouseButtonCallback(handle, nMouseButtonCallback)
            glfwSetScrollCallback(handle, nScrollCallback)
            glfwSetWindowCloseCallback(handle, nWindowCloseCallback)
            glfwSetWindowContentScaleCallback(handle, nWindowContentScaleCallback)
            cursorPosCallback = defaultCursorPosCallback
            framebufferSizeCallback = defaultFramebufferSizeCallback
            keyCallback = defaultKeyCallback
            mouseButtonCallback = defaultMouseButtonCallback
            scrollCallback = defaultScrollCallback
            windowCloseCallback = defaultWindowCloseCallback
            windowContentScaleCallback = defaultWindowContentScaleCallback
        }
    }

    init {
        if (handle == NULL) {
            glfw.terminate()
            throw RuntimeException("Failed to create the GLFW window")
        }
    }

    var debugProc: Callback? = null

    fun init(show: Boolean = true) {
        makeContextCurrent()
        /*  This line is critical for LWJGL's interoperation with GLFW's OpenGL context,
            or any context that is managed externally.
            LWJGL detects the context that is current in the current thread, creates the GLCapabilities instance and
            makes the OpenGL bindings available for use. */
        GL.createCapabilities()
        show(show)
        if (windowHint.debug) {
            debugProc = GLUtil.setupDebugMessageCallback()
            // turn off notifications only
            GL43C.nglDebugMessageControl(GlDebugSource.DONT_CARE.i, GlDebugType.DONT_CARE.i, GlDebugSeverity.NOTIFICATION.i, 0, NULL, false)
        }
    }

    val isOpen: Boolean
        get() = !shouldClose

    var shouldClose: Boolean
        get() = glfwWindowShouldClose(handle)
        set(value) = glfwSetWindowShouldClose(handle, value)

    var title: String = ""
        set(value) {
            glfwSetWindowTitle(handle, value)
            field = value
        }

    fun setSizeLimit(width: IntRange, height: IntRange) = glfwSetWindowSizeLimits(handle, width.start, height.start, width.endInclusive, height.endInclusive)

    var pos = Vec2i()
        get() = stak {
            val x = it.nmalloc(Int.BYTES * 2)
            val y = x + Int.BYTES
            nglfwGetWindowPos(handle, x, y)
            field(memGetInt(x), memGetInt(y))
        }
        set(value) = glfwSetWindowPos(handle, value.x, value.y)

    var size = Vec2i()
        get() = stak {
            val x = it.nmalloc(Int.BYTES * 2)
            val y = x + Int.BYTES
            nglfwGetWindowSize(handle, x, y)
            field(memGetInt(x), memGetInt(y))
        }
        set(value) = glfwSetWindowSize(handle, value.x, value.y)

    fun sizeLimit(minWidth: Int, minHeight: Int, maxWidth: Int, maxHeight: Int) = glfwSetWindowSizeLimits(handle, minWidth, minHeight, maxWidth, maxHeight)

    val aspect: Float
        get() = size.aspect
//        set(value) = glfwSetWindowAspectRatio(handle, (value * 1_000).i, 1_000)

    var aspectRatio = Vec2i()
        get() = field(size.x, size.y)
        set(value) = glfwSetWindowAspectRatio(handle, value.x, value.y)

    val framebufferSize = Vec2i()
        get() = stak {
            val x = it.nmalloc(Int.BYTES * 2)
            val y = x + Int.BYTES
            nglfwGetFramebufferSize(handle, x, y)
            field(memGetInt(x), memGetInt(y))
        }

    val frameSize = Vec4i()
        get() = stak {
            val x = it.nmalloc(Int.BYTES * 4)
            val y = x + Int.BYTES
            val z = y + Int.BYTES
            val w = z + Int.BYTES
            nglfwGetWindowFrameSize(handle, x, y, z, w)
            field(memGetInt(x), memGetInt(y), memGetInt(z), memGetInt(w))
        }

    val contentScale = Vec2()
        get() = stak {
            val x = it.nmalloc(Float.BYTES * 2)
            val y = x + Float.BYTES
            nglfwGetWindowContentScale(handle, x, y)
            field(memGetFloat(x), memGetFloat(y))
        }

    var opacity: Float
        get() = glfwGetWindowOpacity(handle)
        set(value) = glfwSetWindowOpacity(handle, value)

    var stickyKeys: Boolean
        get() = glfwGetInputMode(handle, GLFW_STICKY_KEYS).bool
        set(value) = glfwSetInputMode(handle, GLFW_STICKY_KEYS, value.i)

    var lockKeyMods: Boolean
        get() = glfwGetInputMode(handle, GLFW_LOCK_KEY_MODS).bool
        set(value) = glfwSetInputMode(handle, GLFW_LOCK_KEY_MODS, value.i)

    fun defaultHints() = glfwDefaultWindowHints()

    fun iconify() = glfwIconifyWindow(handle)
    fun restore() = glfwRestoreWindow(handle)
    fun maximize() = glfwMaximizeWindow(handle)
    fun show(show: Boolean = true) = if (show) glfwShowWindow(handle) else glfwHideWindow(handle)
    fun hide() = glfwHideWindow(handle)
    fun focus() = glfwFocusWindow(handle)

    data class Monitor(val monitor: Long, val xPos: Int, val yPos: Int, val width: Int, val height: Int, val refreshRate: Int = GLFW_DONT_CARE)

    var monitor: Monitor
        get() {
            val monitor = glfwGetWindowMonitor(handle)
            return Monitor(monitor, pos.x, pos.y, size.x, size.y)
        }
        set(value) = glfwSetWindowMonitor(handle, value.monitor, value.xPos, value.yPos, value.width, value.height, value.refreshRate)

    val isFocused: Boolean
        get() = glfwGetWindowAttrib(handle, GLFW_FOCUSED).bool
    val isIconified: Boolean
        get() = glfwGetWindowAttrib(handle, GLFW_ICONIFIED).bool
    val isMaximized: Boolean
        get() = glfwGetWindowAttrib(handle, GLFW_MAXIMIZED).bool
    val isVisible: Boolean
        get() = glfwGetWindowAttrib(handle, GLFW_VISIBLE).bool
    val isHovered: Boolean
        get() = glfwGetWindowAttrib(handle, GLFW_HOVERED).bool

    var resizable: Boolean
        get() = glfwGetWindowAttrib(handle, GLFW_RESIZABLE).bool
        set(value) = glfwSetWindowAttrib(handle, GLFW_RESIZABLE, value.i)
    var decorated: Boolean
        get() = glfwGetWindowAttrib(handle, GLFW_DECORATED).bool
        set(value) = glfwSetWindowAttrib(handle, GLFW_DECORATED, value.i)
    var floating: Boolean
        get() = glfwGetWindowAttrib(handle, GLFW_FLOATING).bool
        set(value) = glfwSetWindowAttrib(handle, GLFW_FLOATING, value.i)
    var autoIconified: Boolean
        get() = glfwGetWindowAttrib(handle, GLFW_AUTO_ICONIFY).bool
        set(value) = glfwSetWindowAttrib(handle, GLFW_AUTO_ICONIFY, value.i)

    fun makeContextCurrent(windowHandle: GlfwWindowHandle = handle) = glfwMakeContextCurrent(windowHandle)

    /** Free the window callbacks and destroy the window and reset its handle back to NULL */
    fun destroy() {
        Callbacks.glfwFreeCallbacks(handle)
        debugProc?.free()
        glfwDestroyWindow(handle)
        handle = NULL
    }

    var cursorPos = Vec2d()
        get() = stak {
            val x = it.nmalloc(Double.BYTES * 2)
            val y = x + Double.BYTES
            nglfwGetCursorPos(handle, x, y)
            field(memGetDouble(x), memGetDouble(y))
        }
        set(value) = glfwSetCursorPos(handle, value.x, value.y)


    // ------------------- Callbacks -------------------

    val defaultKey = "0 - default"

    var charCallback: CharCallbackT? = null
        get() = charCallbacks.getOrfirst(defaultKey)
        set(value) {
            charCallbacks[defaultKey] = value
            field = value
        }

    val charCallbacks = sortedMapOf<String, CharCallbackT>()
    val nCharCallback = GLFWCharCallbackI { _, codePoint -> charCallbacks.values.forEach { it(codePoint) } }


    var cursorPosCallback: CursorPosCallbackT? = null
        get() = cursorPosCallbacks.getOrfirst(defaultKey)
        set(value) {
            cursorPosCallbacks[defaultKey] = value
            field = value
        }

    val cursorPosCallbacks = sortedMapOf<String, CursorPosCallbackT>()
    val nCursorPosCallback = GLFWCursorPosCallbackI { _, xPos, yPos -> cursorPosCallbacks.values.forEach { it(Vec2(xPos, yPos)) } }


    var framebufferSizeCallback: FramebufferSizeCallbackT? = null
        get() = framebufferSizeCallbacks.getOrfirst(defaultKey)
        set(value) {
            framebufferSizeCallbacks[defaultKey] = value
            field = value
        }
    val framebufferSizeCallbacks = sortedMapOf<String, FramebufferSizeCallbackT>()
    val nFramebufferSizeCallback = GLFWFramebufferSizeCallbackI { _, width, height -> framebufferSizeCallbacks.values.forEach { it(Vec2i(width, height)) } }


    var keyCallback: KeyCallbackT? = null
        get() = keyCallbacks.getOrfirst(defaultKey)
        set(value) {
            keyCallbacks[defaultKey] = value
            field = value
        }
    val keyCallbacks = sortedMapOf<String, KeyCallbackT>()
    val nKeyCallback = GLFWKeyCallbackI { _, key, scanCode, action, mods -> keyCallbacks.values.forEach { it(key, scanCode, action, mods) } }


    var mouseButtonCallback: MouseButtonCallbackT? = null
        get() = mouseButtonCallbacks.getOrfirst(defaultKey)
        set(value) {
            mouseButtonCallbacks[defaultKey] = value
            field = value
        }
    val mouseButtonCallbacks = sortedMapOf<String, MouseButtonCallbackT>()
    val nMouseButtonCallback = GLFWMouseButtonCallbackI { _, button, action, mods -> mouseButtonCallbacks.values.forEach { it(button, action, mods) } }


    var scrollCallback: ScrollCallbackT? = null
        get() = scrollCallbacks.getOrfirst(defaultKey)
        set(value) {
            scrollCallbacks[defaultKey] = value
            field = value
        }
    val scrollCallbacks = sortedMapOf<String, ScrollCallbackT>()
    val nScrollCallback = GLFWScrollCallbackI { _, xOffset, yOffset -> scrollCallbacks.values.forEach { it(Vec2d(xOffset, yOffset)) } }


    var windowCloseCallback: WindowCloseCallbackT? = null
        get() = windowCloseCallbacks.getOrfirst(defaultKey)
        set(value) {
            windowCloseCallbacks[defaultKey] = value
            field = value
        }
    val windowCloseCallbacks = sortedMapOf<String, WindowCloseCallbackT>()
    val nWindowCloseCallback = GLFWWindowCloseCallbackI { windowCloseCallbacks.values.forEach { it() } }


    var windowContentScaleCallback: WindowContentScaleCallbackT? = null
        get() = windowContentScaleCallbacks.getOrfirst(defaultKey)
        set(value) {
            windowContentScaleCallbacks[defaultKey] = value
            field = value
        }
    val windowContentScaleCallbacks = sortedMapOf<String, WindowContentScaleCallbackT>()
    val nWindowContentScaleCallback = GLFWWindowContentScaleCallbackI { _, xScale, yScale -> windowContentScaleCallbacks.values.forEach { it(Vec2(xScale, yScale)) } }


    val defaultKeyCallback: KeyCallbackT = { key, scanCode, action, mods -> onKey(key, scanCode, action, mods) }
    val defaultMouseButtonCallback: MouseButtonCallbackT = { button, action, mods -> onMouse(button, action, mods) }
    val defaultCursorPosCallback: CursorPosCallbackT = { pos -> onMouseMoved(pos) }
    val defaultScrollCallback: ScrollCallbackT = { scroll -> onMouseScrolled(scroll.y.f) }
    val defaultWindowCloseCallback: WindowCloseCallbackT = ::onWindowClosed
    val defaultWindowContentScaleCallback: WindowContentScaleCallbackT = { newScale -> onWindowContentScaled(newScale) }
    val defaultFramebufferSizeCallback: FramebufferSizeCallbackT = { size -> onWindowResized(size) }

    //
    // Event handlers are called by the GLFW callback mechanism and should not be called directly
    //

    open fun onWindowResized(newSize: Vec2i) {}
    open fun onWindowClosed() {}

    // Keyboard handling
    open fun onKey(key: Int, scanCode: Int, action: Int, mods: Int) {
        when (action) {
            GLFW_PRESS -> onKeyPressed(key, mods)
            GLFW_RELEASE -> onKeyReleased(key, mods)
        }
    }

    open fun onKeyPressed(key: Int, mods: Int) {}
    open fun onKeyReleased(key: Int, mods: Int) {}

    // Mouse handling
    open fun onMouse(button: Int, action: Int, mods: Int) {
        when (action) {
            GLFW_PRESS -> onMousePressed(button, mods)
            GLFW_RELEASE -> onMouseReleased(button, mods)
        }
    }

    open fun onMousePressed(button: Int, mods: Int) {}
    open fun onMouseReleased(button: Int, mods: Int) {}
    open fun onMouseMoved(newPos: Vec2) {}
    open fun onMouseScrolled(delta: Float) {}

    open fun onWindowContentScaled(newScale: Vec2) {}

    var cursorStatus: CursorStatus
        get() = when (glfwGetInputMode(handle, GLFW_CURSOR)) {
            GLFW_CURSOR_NORMAL -> CursorStatus.Normal
            GLFW_CURSOR_HIDDEN -> CursorStatus.Hidden
            GLFW_CURSOR_DISABLED -> CursorStatus.Disabled
            else -> throw Error()
        }
        set(value) = glfwSetInputMode(handle, GLFW_CURSOR, when (value) {
            CursorStatus.Normal -> GLFW_CURSOR_NORMAL
            CursorStatus.Hidden -> GLFW_CURSOR_HIDDEN
            CursorStatus.Disabled -> GLFW_CURSOR_DISABLED
        })
    var cursor: GlfwCursor
        get() = NULL
        set(value) = glfwSetCursor(handle, value)

    enum class CursorStatus { Normal, Hidden, Disabled }

    fun isPressed(key: Key) = glfwGetKey(handle, key.i) == GLFW_PRESS
    fun isReleased(key: Key) = glfwGetKey(handle, key.i) == GLFW_RELEASE

    fun isPressed(button: MouseButton) = glfwGetMouseButton(handle, button.i) == GLFW_PRESS
    fun isRelease(button: MouseButton) = glfwGetMouseButton(handle, button.i)  == GLFW_RELEASE

    val joystick1Buttons: ByteBuffer?
        get() = getJoystickButtons(GLFW_JOYSTICK_1)
    val joystick2Buttons: ByteBuffer?
        get() = getJoystickButtons(GLFW_JOYSTICK_2)
    val joystick3Buttons: ByteBuffer?
        get() = getJoystickButtons(GLFW_JOYSTICK_3)

    fun getJoystickButtons(joystickId: Int): ByteBuffer? = stak {
        val count = it.nmalloc(Int.BYTES)
        val result = nglfwGetJoystickButtons(joystickId, count)
        memByteBufferSafe(result, memGetInt(count))
    }

    val joystick1Axes: FloatBuffer?
        get() = getJoystickAxes(GLFW_JOYSTICK_1)
    val joystick2Axes: FloatBuffer?
        get() = getJoystickAxes(GLFW_JOYSTICK_2)
    val joystick3Axes: FloatBuffer?
        get() = getJoystickAxes(GLFW_JOYSTICK_3)

    fun getJoystickAxes(joystickId: Int): FloatBuffer? = stak {
        val count = it.nmalloc(Int.BYTES)
        val result = nglfwGetJoystickAxes(joystickId, count)
        memFloatBufferSafe(result, memGetInt(count))
    }

    var autoSwap = true

    inline fun loop(block: (MemoryStack) -> Unit) = loop({ isOpen }, block)

    /**
     *  The `stack` passed to `block` will be automatically a stack frame in place
     *  (i.e. it has been pushed exactly once, without popping).
     *  So you can do any allocation on that frame without pushing/popping further
     *  It's the user choice to pass it down the stacktrace to avoid TLS
     */
    inline fun loop(condition: () -> Boolean, block: (MemoryStack) -> Unit) {
        while (condition()) {
            glfwPollEvents()
            stak {
                block(it)
                if (autoSwap)
                    glfwSwapBuffers(handle)
            }
        }
    }

//    infix fun createSurface(instance: VkInstance) = glfw.createWindowSurface(handle, instance)

    fun swapBuffers() = glfwSwapBuffers(handle)
    fun present() = swapBuffers()

    fun requestAttention() = glfwRequestWindowAttention(handle)

    val hwnd: HWND
        get() = GLFWNativeWin32.glfwGetWin32Window(handle)

    companion object {
        infix fun fromWin32Window(hwnd: HWND) = GlfwWindow(GLFWNativeWin32.glfwAttachWin32Window(hwnd, NULL))
    }
}