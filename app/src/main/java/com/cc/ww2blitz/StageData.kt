package com.cc.ww2blitz

class StageData {
    private var sequenceIndex = 0

    private var stageId = STAGE_SEQUENCE[0]
    val currentStage: Int
        get() = stageId
    var scrollSpeedY = 180f
    var targetBossTimelineSeconds = 38f
    var stageMusicTrack = SoundManager.BGM_STAGE1

    init {
        applyStageMetrics(stageId)
    }

    fun isLastInSequence(): Boolean {
        return sequenceIndex >= STAGE_SEQUENCE.size - 1
    }

    fun advanceToNextStage() {
        sequenceIndex++
        if (sequenceIndex >= STAGE_SEQUENCE.size) {
            sequenceIndex = 0
        }
        stageId = STAGE_SEQUENCE[sequenceIndex]
        applyStageMetrics(stageId)
    }

    fun setCurrentStage(stage: Int) {
        var s = stage
        if (s < 1) s = 1
        if (s > STAGE_6) s = STAGE_6
        stageId = s
        var i = 0
        sequenceIndex = 0
        while (i < STAGE_SEQUENCE.size) {
            if (STAGE_SEQUENCE[i] == s) {
                sequenceIndex = i
                break
            }
            i++
        }
        applyStageMetrics(stageId)
    }

    fun resetToStart() {
        sequenceIndex = 0
        stageId = STAGE_SEQUENCE[0]
        applyStageMetrics(stageId)
    }

    fun applyStageMetrics(stage: Int) {
        when (stage) {
            2 -> {
                scrollSpeedY = 260f
                targetBossTimelineSeconds = 30f
                stageMusicTrack = SoundManager.BGM_STAGE2
            }
            3 -> {
                scrollSpeedY = 200f
                targetBossTimelineSeconds = 25f
                stageMusicTrack = SoundManager.BGM_STAGE2
            }
            4 -> {
                scrollSpeedY = 310f
                targetBossTimelineSeconds = 45f
                stageMusicTrack = SoundManager.BGM_STAGE2
            }
            STAGE_5 -> {
                scrollSpeedY = 280f
                targetBossTimelineSeconds = 45f
                stageMusicTrack = SoundManager.MUSIC_STAGE_5
            }
            STAGE_6 -> {
                scrollSpeedY = 180f
                targetBossTimelineSeconds = 50f
                stageMusicTrack = SoundManager.BGM_STAGE1
            }
            else -> {
                scrollSpeedY = 180f
                targetBossTimelineSeconds = 38f
                stageMusicTrack = SoundManager.BGM_STAGE1
            }
        }
    }

    companion object {
        const val STAGE_5 = 5
        const val STAGE_6 = 6
        val STAGE_SEQUENCE = intArrayOf(1, 2, 3, 4, 5, 6)
    }
}
