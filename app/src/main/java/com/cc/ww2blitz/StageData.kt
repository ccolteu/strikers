package com.cc.ww2blitz

class StageData {
    // Dev playlist: change order to jump into a stage (e.g. intArrayOf(3, 2, 1)).
    private val STAGE_SEQUENCE = intArrayOf(1, 2, 3, 4)
    private var sequenceIndex = 0

    var currentStage = STAGE_SEQUENCE[0]
    var scrollSpeedY = 180f
    var targetBossTimelineSeconds = 38f

    init {
        applyStageMetrics()
    }

    fun advanceToNextStage() {
        sequenceIndex++
        if (sequenceIndex >= STAGE_SEQUENCE.size) {
            sequenceIndex = 0
        }
        currentStage = STAGE_SEQUENCE[sequenceIndex]
        applyStageMetrics()
    }

    fun resetToStart() {
        sequenceIndex = 0
        currentStage = STAGE_SEQUENCE[0]
        applyStageMetrics()
    }

    private fun applyStageMetrics() {
        when (currentStage) {
            2 -> {
                scrollSpeedY = 260f
                targetBossTimelineSeconds = 30f
            }
            3 -> {
                scrollSpeedY = 200f
                targetBossTimelineSeconds = 25f
            }
            4 -> {
                scrollSpeedY = 310f
                targetBossTimelineSeconds = 45f
            }
            else -> {
                scrollSpeedY = 180f
                targetBossTimelineSeconds = 38f
            }
        }
    }
}
