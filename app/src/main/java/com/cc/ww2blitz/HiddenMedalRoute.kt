package com.cc.ww2blitz

/**
 * Per-map secret medal beats. Cues are screen fractions; collect them for the recap SECRET line.
 */
object HiddenMedalRoute {

  private const val MAX_CUES = 5
  private val atSec = FloatArray(MAX_CUES)
  private val xFrac = FloatArray(MAX_CUES)
  private val yFrac = FloatArray(MAX_CUES)
  private val fired = BooleanArray(MAX_CUES)
  private var boundStage = -1
  private var cueCount = 0

  fun cueCount(): Int = cueCount

  fun reset() {
    boundStage = -1
    cueCount = 0
    var i = 0
    while (i < MAX_CUES) {
      fired[i] = false
      i++
    }
  }

  fun bind(stageId: Int) {
    if (boundStage == stageId) return
    boundStage = stageId
    cueCount = loadCues(stageId)
    var i = 0
    while (i < MAX_CUES) {
      fired[i] = false
      i++
    }
  }

  fun tick(elapsed: Float, screenW: Float, screenH: Float, items: PowerUpItem) {
    var i = 0
    while (i < cueCount) {
      if (!fired[i] && elapsed >= atSec[i]) {
        fired[i] = true
        items.spawnSecretMedal(xFrac[i] * screenW, yFrac[i] * screenH)
      }
      i++
    }
  }

  private fun loadCues(stageId: Int): Int {
    return when (stageId) {
      1 -> pack(
        6f, 0.10f, 0.08f,
        14f, 0.90f, 0.10f,
        22f, 0.08f, 0.12f,
        30f, 0.92f, 0.08f,
        35f, 0.50f, 0.06f,
      )
      2 -> pack(
        5f, 0.88f, 0.08f,
        11f, 0.10f, 0.10f,
        17f, 0.90f, 0.08f,
        23f, 0.12f, 0.12f,
        28f, 0.50f, 0.07f,
      )
      3 -> pack(
        4f, 0.08f, 0.08f,
        9f, 0.92f, 0.10f,
        14f, 0.10f, 0.08f,
        19f, 0.88f, 0.10f,
        23f, 0.50f, 0.06f,
      )
      4 -> pack(
        7f, 0.10f, 0.08f,
        15f, 0.90f, 0.10f,
        23f, 0.08f, 0.08f,
        31f, 0.92f, 0.10f,
        38f, 0.50f, 0.07f,
      )
      5 -> pack(
        6f, 0.88f, 0.08f,
        14f, 0.10f, 0.10f,
        22f, 0.90f, 0.08f,
        30f, 0.12f, 0.10f,
        36f, 0.50f, 0.07f,
      )
      6 -> pack(
        8f, 0.12f, 0.08f,
        16f, 0.88f, 0.10f,
        24f, 0.08f, 0.08f,
        32f, 0.92f, 0.10f,
        40f, 0.50f, 0.07f,
      )
      7 -> pack(
        8f, 0.10f, 0.08f,
        16f, 0.90f, 0.10f,
        24f, 0.08f, 0.08f,
        32f, 0.92f, 0.10f,
        40f, 0.50f, 0.07f,
      )
      8 -> pack(
        1.2f, 0.12f, 0.10f,
        2.6f, 0.88f, 0.10f,
        3.8f, 0.20f, 0.08f,
      )
      else -> 0
    }
  }

  private fun pack(vararg triples: Float): Int {
    val n = triples.size / 3
    var i = 0
    while (i < n && i < MAX_CUES) {
      val o = i * 3
      atSec[i] = triples[o]
      xFrac[i] = triples[o + 1]
      yFrac[i] = triples[o + 2]
      i++
    }
    return n.coerceAtMost(MAX_CUES)
  }
}
