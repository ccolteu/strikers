package com.cc.ww2blitz

import android.content.Context

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
    private var currentDifficulty = Difficulty.NORMAL
    private var combatRank = 0f
    private var savedFighterIndex = 0

    init {
        liveInstance = this
        applyStageMetrics(stageId)
    }

    fun getDifficulty(): Difficulty = currentDifficulty

    fun resetCombatRank() {
        combatRank = 0f
    }

    fun dumpCombatRankOnDeath() {
        combatRank *= DEATH_KEEP
    }

    fun tickCombatRank(dt: Float, atMaxGun: Boolean) {
        if (dt <= 0.0001f || !atMaxGun) return
        combatRank += dt / RISE_SECS
        val cap = rankCap()
        if (combatRank > cap) combatRank = cap
    }

    fun shotSpeedScale(): Float =
        currentDifficulty.speedMultiplier * (1f + SPEED_GAIN * combatRank)

    fun fireIntervalDivider(): Float {
        val div = currentDifficulty.intervalDivider * (1f + FIRE_GAIN * combatRank)
        return if (div < 0.01f) 0.01f else div
    }

    fun burstBonus(): Int {
        val extra = if (combatRank >= BURST_AT) 1 else 0
        return currentDifficulty.burstBonus + extra
    }

    fun aimSlopRad(): Float {
        if (currentDifficulty.index >= 3) return 0f
        return Enemy.AIM_SLOP_RAD * (1f - combatRank)
    }

    fun shouldLeadShots(): Boolean =
        currentDifficulty.index >= 5 || combatRank >= LEAD_AT

    fun kamikazeSeeks(): Boolean =
        currentDifficulty.index >= 3 || combatRank >= KAMI_AT

    fun lootChanceScale(): Float {
        val idx = currentDifficulty.index
        return if (idx <= 1) {
            0.75f
        } else if (idx == 2) {
            0.85f
        } else if (idx == 4) {
            1.08f
        } else if (idx == 5) {
            1.15f
        } else if (idx == 6) {
            1.20f
        } else if (idx >= 7) {
            1.25f
        } else {
            1.0f
        }
    }

    fun revengeOnDeath(): Boolean =
        currentDifficulty.index >= 5 || combatRank >= REVENGE_AT

    fun popcornSuicide(): Boolean = currentDifficulty.index >= 3

    private fun rankCap(): Float {
        val idx = currentDifficulty.index
        return if (idx <= 1) {
            0.30f
        } else if (idx == 2) {
            0.55f
        } else {
            1f
        }
    }

    fun setDifficulty(diff: Difficulty) {
        currentDifficulty = diff
    }

    fun setCurrentDifficulty(diff: Difficulty) {
        currentDifficulty = diff
    }

    fun initPersistentSettings(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedIndex = prefs.getInt(KEY_DIFFICULTY, Difficulty.NORMAL.index)
        currentDifficulty = difficultyFromIndex(savedIndex)
        val fighter = prefs.getInt(KEY_FIGHTER, 0)
        savedFighterIndex = if (fighter == 1) 1 else 0
    }

    fun saveDifficultySetting(context: Context, diff: Difficulty) {
        currentDifficulty = diff
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_DIFFICULTY, diff.index).apply()
    }

    fun getSavedFighterIndex(): Int = savedFighterIndex

    fun saveFighterSetting(context: Context, typeIndex: Int) {
        savedFighterIndex = if (typeIndex == 1) 1 else 0
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_FIGHTER, savedFighterIndex).apply()
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
        @JvmField
        var liveInstance: StageData? = null
        // val STAGE_SEQUENCE = intArrayOf(1) // test just one level
        // val STAGE_SEQUENCE = intArrayOf(3,2,6,6) // test any order even with duplicates
        val STAGE_SEQUENCE = intArrayOf(2)//intArrayOf(1, 2, 3, 4, 5, 6)
        private const val PREFS_NAME = "shmup_arcade_settings"
        private const val KEY_DIFFICULTY = "target_difficulty"
        private const val KEY_FIGHTER = "chosen_fighter"
        private const val RISE_SECS = 48f
        private const val DEATH_KEEP = 0.40f
        private const val SPEED_GAIN = 0.22f
        private const val FIRE_GAIN = 0.28f
        private const val LEAD_AT = 0.60f
        private const val KAMI_AT = 0.50f
        private const val REVENGE_AT = 0.70f
        private const val BURST_AT = 0.80f

        fun difficultyFromIndex(index: Int): Difficulty {
            return when (index) {
                1 -> Difficulty.MONKEY
                2 -> Difficulty.EASY
                3 -> Difficulty.NORMAL
                4 -> Difficulty.HARD
                5 -> Difficulty.VERY_HARD
                6 -> Difficulty.EXPERT
                7 -> Difficulty.HARDCORE
                else -> Difficulty.NORMAL
            }
        }
    }

    enum class Difficulty(
        val index: Int,
        val speedMultiplier: Float,
        val intervalDivider: Float,
        val burstBonus: Int,
    ) {
        MONKEY(1, 0.65f, 0.75f, -1),
        EASY(2, 0.85f, 0.90f, 0),
        NORMAL(3, 1.00f, 1.00f, 0),
        HARD(4, 1.15f, 1.15f, 0),
        VERY_HARD(5, 1.30f, 1.25f, 1),
        EXPERT(6, 1.45f, 1.40f, 1),
        HARDCORE(7, 1.60f, 1.55f, 2),
    }
}
