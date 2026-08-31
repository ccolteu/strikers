package com.cc.ww2blitz

class VictoryScorecard {
    var isActive = false
    var elapsedTime = 0f

    // Tally animation sequence steps
    var currentDisplayLine = 0
    var isCountingDone = false

    // Calculated score parameters
    var lifeBonus = 0
    var bombBonus = 0
    var totalStageScore = 0

    // String calculation visual tickers to prevent runtime heap garbage
    var visibleLifeBonus = 0
    var visibleBombBonus = 0
    var visibleTotalScore = 0

    fun trigger(remainingLives: Int, remainingBombs: Int, carriedScore: Int) {
        elapsedTime = 0f
        currentDisplayLine = 0
        isCountingDone = false

        // Classic Psikyo balancing multipliers
        lifeBonus = remainingLives * 10_000
        bombBonus = remainingBombs * 5_000
        totalStageScore = carriedScore + lifeBonus + bombBonus
        if (totalStageScore < 0) totalStageScore = 0
        if (totalStageScore > 99_999_999) totalStageScore = 99_999_999

        visibleLifeBonus = 0
        visibleBombBonus = 0
        visibleTotalScore = carriedScore
        if (visibleTotalScore < 0) visibleTotalScore = 0
        if (visibleTotalScore > 99_999_999) visibleTotalScore = 99_999_999

        isActive = true
    }

    fun update(dt: Float) {
        if (!isActive) return
        elapsedTime += dt

        // Sequence line triggers timed precisely by fractions of a second
        if (currentDisplayLine == 0 && elapsedTime >= 0.8f) {
            currentDisplayLine = 1 // Show "STAGE CLEAR" header
        }
        if (currentDisplayLine == 1 && elapsedTime >= 1.6f) {
            currentDisplayLine = 2 // Roll "LIFE BONUS"
        }
        if (currentDisplayLine == 2) {
            // Smoothly animate the numeric ticker upwards
            if (visibleLifeBonus < lifeBonus) {
                visibleLifeBonus += 400
                if (visibleLifeBonus > lifeBonus) visibleLifeBonus = lifeBonus
            } else if (elapsedTime >= 2.6f) {
                currentDisplayLine = 3 // Roll "BOMB BONUS"
            }
        }
        if (currentDisplayLine == 3) {
            if (visibleBombBonus < bombBonus) {
                visibleBombBonus += 200
                if (visibleBombBonus > bombBonus) visibleBombBonus = bombBonus
            } else if (elapsedTime >= 3.6f) {
                currentDisplayLine = 4 // Roll "TOTAL SCORE"
            }
        }
        if (currentDisplayLine == 4) {
            if (visibleTotalScore < totalStageScore) {
                visibleTotalScore += 600
                if (visibleTotalScore > totalStageScore) visibleTotalScore = totalStageScore
            } else {
                isCountingDone = true
            }
        }
    }
}
