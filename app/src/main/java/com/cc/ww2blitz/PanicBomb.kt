package com.cc.ww2blitz

class PanicBomb {
    var x = 0f
    var y = 0f
    var currentFrameTime = 0f
    var currentFrameIndex = 0
    var isActive = false

    fun activate(startX: Float, startY: Float) {
        x = startX
        y = startY
        currentFrameTime = 0f
        currentFrameIndex = 0
        isActive = true
    }

    companion object {
        const val FRAME_COUNT = 6
        const val FRAME_DURATION = 0.083f // ~83ms per frame (Smooth 12 FPS arcade playback)
        const val TOTAL_DURATION_SEC = FRAME_COUNT * FRAME_DURATION // ~0.5 seconds per cycle loop
        const val MAX_BOMB_SCALE_FRAC = 2.2f // Expands to swallow the entire screen layout
    }
}
