package com.cc.ww2blitz

class ScoreManager private constructor() {

  private var score = 0
  private var grazeCount = 0
  private var recapPhase = PHASE_IDLE
  private var recapTimer = 0f
  private var recapFrame = 0
  private var livesCount = 0
  private var bombsCount = 0
  private var livesBonusTarget = 0
  private var bombsBonusTarget = 0
  private var grazeBonusTarget = 0
  private var livesAwarded = 0
  private var bombsAwarded = 0
  private var grazeAwarded = 0
  private var recapActive = false
  private var activeMultiplier = 1.0f

  fun getScore(): Int = score

  fun setScore(value: Int) {
    var next = value
    if (next < 0) next = 0
    if (next > MAX_SCORE) next = MAX_SCORE
    score = next
  }

  fun addScore(points: Int) {
    setScore(score + points)
  }

  fun syncDifficultyMultiplier(difficultyIndex: Int) {
    activeMultiplier = when (difficultyIndex) {
      1 -> 0.5f
      2 -> 0.8f
      3 -> 1.0f
      4 -> 1.2f
      5 -> 1.5f
      6 -> 2.0f
      7 -> 3.0f
      else -> 1.0f
    }
  }

  fun scalePoints(base: Int): Int {
    var v = (base * activeMultiplier).toInt()
    if (v < 0) v = 0
    if (v > MAX_SCORE) v = MAX_SCORE
    return v
  }

  fun addFlankBreakBonus(x: Float, y: Float) {
    val awarded = scalePoints(FLANK_BREAK_POINTS)
    addScore(awarded)
    queuePopup(x, y, awarded)
  }

  fun addCoreKillBonus(x: Float, y: Float) {
    val awarded = scalePoints(CORE_KILL_POINTS)
    addScore(awarded)
    queuePopup(x, y, awarded)
  }

  fun addBulletCancelBonus(x: Float, y: Float) {
    val awarded = scalePoints(BULLET_CANCEL_POINTS)
    addScore(awarded)
    queuePopup(x, y, awarded)
  }

  fun hasPopup(): Boolean = popupCount > 0

  fun popupX(): Float = if (popupCount > 0) popX[0] else 0f

  fun popupY(): Float = if (popupCount > 0) popY[0] else 0f

  fun popupValue(): Int = if (popupCount > 0) popV[0] else 0

  fun consumePopup() {
    if (popupCount <= 0) return
    var i = 0
    while (i < popupCount - 1) {
      popX[i] = popX[i + 1]
      popY[i] = popY[i + 1]
      popV[i] = popV[i + 1]
      i++
    }
    popupCount--
  }

  fun addGrazeScore(points: Int) {
    grazeCount++
    if (grazeCount > MAX_GRAZE) grazeCount = MAX_GRAZE
    addScore(scalePoints(points))
  }

  fun getGrazeCount(): Int = grazeCount

  fun recapPhase(): Int = recapPhase

  fun recapFrame(): Int = recapFrame

  fun recapLivesCount(): Int = livesCount

  fun recapBombsCount(): Int = bombsCount

  fun recapGrazeCount(): Int = grazeCount

  fun recapLivesAwarded(): Int = livesAwarded

  fun recapBombsAwarded(): Int = bombsAwarded

  fun recapGrazeAwarded(): Int = grazeAwarded

  fun recapBonusTotal(): Int {
    var sum = livesBonusTarget + bombsBonusTarget + grazeBonusTarget
    if (sum < 0) sum = 0
    if (sum > MAX_SCORE) sum = MAX_SCORE
    return sum
  }

  fun isRecapReady(): Boolean = recapActive && recapPhase == PHASE_TOTAL

  fun beginRecap(remainingLives: Int, remainingBombs: Int) {
    var lives = remainingLives
    var bombs = remainingBombs
    if (lives < 0) lives = 0
    if (bombs < 0) bombs = 0
    livesCount = lives
    bombsCount = bombs
    livesBonusTarget = lives * LIFE_BONUS
    bombsBonusTarget = bombs * BOMB_BONUS
    grazeBonusTarget = grazeCount * GRAZE_BONUS
    livesAwarded = 0
    bombsAwarded = 0
    grazeAwarded = 0
    recapPhase = PHASE_LIVES
    recapTimer = 0f
    recapFrame = 0
    recapActive = true
  }

  fun updateRecap(dt: Float) {
    if (!recapActive) return
    recapTimer += dt
    recapFrame++
    if (recapPhase == PHASE_LIVES) {
      tickPhase(livesBonusTarget, 0)
      if (recapTimer >= PHASE_DUR) {
        snapPhase(0)
        recapPhase = PHASE_BOMBS
        recapTimer = 0f
      }
    } else if (recapPhase == PHASE_BOMBS) {
      tickPhase(bombsBonusTarget, 1)
      if (recapTimer >= PHASE_DUR) {
        snapPhase(1)
        recapPhase = PHASE_GRAZE
        recapTimer = 0f
      }
    } else if (recapPhase == PHASE_GRAZE) {
      tickPhase(grazeBonusTarget, 2)
      if (recapTimer >= PHASE_DUR) {
        snapPhase(2)
        recapPhase = PHASE_TOTAL
        recapTimer = 0f
      }
    }
  }

  fun resetStageCounters() {
    grazeCount = 0
    recapPhase = PHASE_IDLE
    recapTimer = 0f
    recapFrame = 0
    livesCount = 0
    bombsCount = 0
    livesBonusTarget = 0
    bombsBonusTarget = 0
    grazeBonusTarget = 0
    livesAwarded = 0
    bombsAwarded = 0
    grazeAwarded = 0
    recapActive = false
  }

  fun reset() {
    score = 0
    popupCount = 0
    resetStageCounters()
  }

  private fun tickPhase(target: Int, which: Int) {
    val awarded = awardedOf(which)
    var want: Int
    if (recapTimer >= TALLY_DUR || target <= 0) {
      want = target
    } else {
      want = (target.toFloat() * (recapTimer / TALLY_DUR)).toInt()
      if (want > target) want = target
    }
    val delta = want - awarded
    if (delta > 0) {
      addScore(delta)
      setAwarded(which, want)
    }
    if (target > 0 && awardedOf(which) < target && (recapFrame % CLICK_EVERY_FRAMES) == 0) {
      SoundManager.instance.playSFX(SoundManager.SFX_VULCAN)
    }
  }

  private fun snapPhase(which: Int) {
    val target = if (which == 0) {
      livesBonusTarget
    } else if (which == 1) {
      bombsBonusTarget
    } else {
      grazeBonusTarget
    }
    val awarded = awardedOf(which)
    val remain = target - awarded
    if (remain > 0) {
      addScore(remain)
      setAwarded(which, target)
    }
  }

  private fun awardedOf(which: Int): Int {
    return if (which == 0) {
      livesAwarded
    } else if (which == 1) {
      bombsAwarded
    } else {
      grazeAwarded
    }
  }

  private fun setAwarded(which: Int, value: Int) {
    if (which == 0) {
      livesAwarded = value
    } else if (which == 1) {
      bombsAwarded = value
    } else {
      grazeAwarded = value
    }
  }

  private fun queuePopup(x: Float, y: Float, value: Int) {
    if (popupCount >= POPUP_SLOTS) return
    popX[popupCount] = x
    popY[popupCount] = y
    popV[popupCount] = value
    popupCount++
  }

  companion object {
    const val GRAZE_POINTS = 100
    const val BULLET_CANCEL_POINTS = 100
    const val FLANK_BREAK_POINTS = 25_000
    const val CORE_KILL_POINTS = 100_000
    const val LIFE_BONUS = 50_000
    const val BOMB_BONUS = 20_000
    const val GRAZE_BONUS = 500
    const val PHASE_IDLE = -1
    const val PHASE_LIVES = 0
    const val PHASE_BOMBS = 1
    const val PHASE_GRAZE = 2
    const val PHASE_TOTAL = 3
    private const val PHASE_DUR = 1.5f
    private const val TALLY_DUR = 1.0f
    private const val CLICK_EVERY_FRAMES = 5
    private const val MAX_SCORE = 99_999_999
    private const val MAX_GRAZE = 99_999
    private const val POPUP_SLOTS = 48

    val instance = ScoreManager()
  }

  private val popX = FloatArray(POPUP_SLOTS)
  private val popY = FloatArray(POPUP_SLOTS)
  private val popV = IntArray(POPUP_SLOTS)
  private var popupCount = 0
}
