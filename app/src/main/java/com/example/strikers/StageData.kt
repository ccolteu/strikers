package com.example.strikers

class StageData {
    // Dev playlist: change order to jump into a stage (e.g. intArrayOf(3, 2, 1)).
    private val STAGE_SEQUENCE = intArrayOf(1, 2, 3)
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
            else -> {
                scrollSpeedY = 180f
                targetBossTimelineSeconds = 38f
            }
        }
    }
}
