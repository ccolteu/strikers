package com.example.strikers

class StageData {
    var currentStage = 1
    var scrollSpeedY = 180f // Stage 1 baseline scroll speed
    var targetBossTimelineSeconds = 25f // Time before boss arrives

    fun advanceToNextStage() {
        currentStage++
        when (currentStage) {
            2 -> {
                scrollSpeedY = 260f // Stage 2 scrolls significantly faster for high-speed action!
                targetBossTimelineSeconds = 30f
            }
            3 -> {
                scrollSpeedY = 200f
                targetBossTimelineSeconds = 35f
            }
        }
    }

    fun resetToStart() {
        currentStage = 1
        scrollSpeedY = 180f
        targetBossTimelineSeconds = 25f
    }
}
