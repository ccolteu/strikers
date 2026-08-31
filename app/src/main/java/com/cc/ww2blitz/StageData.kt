package com.cc.ww2blitz

class StageData {
    private var sequenceIndex = 0

    private var stageId = STAGE_SEQUENCE[0]
    val currentStage: Int
        get() = stageId
    val hasOverlayClouds: Boolean
        get() = stageId == STAGE_1
    val isStage1Script: Boolean
        get() = stageId == STAGE_1
    val isStage2Script: Boolean
        get() = stageId == STAGE_2
    val isStage3Script: Boolean
        get() = stageId == STAGE_3
    val isStage4Script: Boolean
        get() = stageId == STAGE_4
    val isStage5Backdrop: Boolean
        get() = stageId == STAGE_5
    val isStage6Backdrop: Boolean
        get() = stageId == STAGE_6
    /** Stages 3–5 halt elapsedTime after the boss cue (identity, not sequence order). */
    val locksElapsedAtBoss: Boolean
        get() = isStage3Script || isStage4Script || isStage5Backdrop
    /** Opening power-V + shared boss-at-elapsed cue (not Stage 5/6 theaters). */
    val usesOpeningPowerV: Boolean
        get() = isStage1Script || isStage2Script || isStage3Script || isStage4Script
    val usesSharedBossEntranceCue: Boolean
        get() = usesOpeningPowerV
    private var campaignFinishedLatch = false
    val isCampaignFinished: Boolean
        get() = campaignFinishedLatch
    var scrollSpeedY = 180f
    var targetBossTimelineSeconds = 38f
    var stageMusicTrack = SoundManager.BGM_STAGE1

    init {
        applyStageMetrics(stageId)
    }

    fun isLastInSequence(): Boolean {
        val n = STAGE_SEQUENCE.size
        if (n <= 0) return true
        return sequenceIndex >= n - 1
    }

    fun advanceToNextStage() {
        val n = STAGE_SEQUENCE.size
        if (n <= 0) {
            campaignFinishedLatch = true
            return
        }
        sequenceIndex++
        if (sequenceIndex >= n) {
            sequenceIndex = n - 1
            campaignFinishedLatch = true
            return
        }
        stageId = STAGE_SEQUENCE[sequenceIndex]
        applyStageMetrics(stageId)
    }

    fun setCurrentStage(stage: Int) {
        val n = STAGE_SEQUENCE.size
        if (n <= 0) return
        campaignFinishedLatch = false

        var targetIndex = -1
        var i = 0
        while (i < n) {
            if (STAGE_SEQUENCE[i] == stage) {
                targetIndex = i
                break
            }
            i++
        }

        if (targetIndex == -1) {
            sequenceIndex = 0
        } else {
            sequenceIndex = targetIndex
        }

        stageId = STAGE_SEQUENCE[sequenceIndex]
        applyStageMetrics(stageId)
    }

    fun resetToStart() {
        campaignFinishedLatch = false
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
        const val STAGE_1 = 1
        const val STAGE_2 = 2
        const val STAGE_3 = 3
        const val STAGE_4 = 4
        const val STAGE_5 = 5
        const val STAGE_6 = 6
        // val STAGE_SEQUENCE = intArrayOf(1) // test just one level
        // val STAGE_SEQUENCE = intArrayOf(3,2,6,6) // test any order even with duplicates
        val STAGE_SEQUENCE = intArrayOf(1, 2, 3, 4, 5, 6)
    }
}
