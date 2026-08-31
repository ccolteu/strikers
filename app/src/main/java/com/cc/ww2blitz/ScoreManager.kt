package com.cc.ww2blitz

class ScoreManager private constructor() {

  private var score = 0

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

  fun addGrazeScore(points: Int) {
    addScore(points)
  }

  fun reset() {
    score = 0
  }

  companion object {
    const val GRAZE_POINTS = 100
    private const val MAX_SCORE = 99_999_999

    val instance: ScoreManager by lazy { ScoreManager() }
  }
}
