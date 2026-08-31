package com.cc.ww2blitz

import android.content.Context

object HighScoreManager {

  const val SLOT_COUNT = 10

  private val topScores = IntArray(SLOT_COUNT)
  private val topNames = CharArray(SLOT_COUNT * 3) { 'A' }
  private val topStages = IntArray(SLOT_COUNT)
  private val fallbackScores = intArrayOf(
    100000, 90000, 80000, 70000, 60000, 50000, 40000, 30000, 20000, 10000,
  )
  private val fallbackStages = intArrayOf(4, 3, 3, 2, 2, 1, 1, 1, 1, 1)
  private val nameWriteBuf = StringBuilder(3)
  private var hydrated = false

  private const val PREFS_NAME = "arcade_leaderboard"
  private val SCORE_KEYS = arrayOf(
    "score_0", "score_1", "score_2", "score_3", "score_4",
    "score_5", "score_6", "score_7", "score_8", "score_9",
  )
  private val STAGE_KEYS = arrayOf(
    "stage_0", "stage_1", "stage_2", "stage_3", "stage_4",
    "stage_5", "stage_6", "stage_7", "stage_8", "stage_9",
  )
  private val NAME_KEYS = arrayOf(
    "name_0", "name_1", "name_2", "name_3", "name_4",
    "name_5", "name_6", "name_7", "name_8", "name_9",
  )
  private val FALLBACK_NAMES = arrayOf(
    "PSK", "STK", "ACE", "SHM", "AAA", "AAA", "AAA", "AAA", "AAA", "AAA",
  )

  init {
    var i = 0
    while (i < SLOT_COUNT) {
      topScores[i] = fallbackScores[i]
      topStages[i] = fallbackStages[i]
      val base = i * 3
      when (i) {
        0 -> {
          topNames[base] = 'P'
          topNames[base + 1] = 'S'
          topNames[base + 2] = 'K'
        }
        1 -> {
          topNames[base] = 'S'
          topNames[base + 1] = 'T'
          topNames[base + 2] = 'K'
        }
        2 -> {
          topNames[base] = 'A'
          topNames[base + 1] = 'C'
          topNames[base + 2] = 'E'
        }
        3 -> {
          topNames[base] = 'S'
          topNames[base + 1] = 'H'
          topNames[base + 2] = 'M'
        }
        else -> {
          topNames[base] = 'A'
          topNames[base + 1] = 'A'
          topNames[base + 2] = 'A'
        }
      }
      i++
    }
  }

  fun scoreAt(index: Int): Int = topScores[index]

  fun stageAt(index: Int): Int = topStages[index]

  fun nameChar(index: Int, charIndex: Int): Char = topNames[index * 3 + charIndex]

  fun loadHighScores(context: Context) {
    if (hydrated) return
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    var i = 0
    while (i < SLOT_COUNT) {
      topScores[i] = prefs.getInt(SCORE_KEYS[i], fallbackScores[i])
      topStages[i] = prefs.getInt(STAGE_KEYS[i], fallbackStages[i])
      val stored = prefs.getString(NAME_KEYS[i], FALLBACK_NAMES[i])
      val base = i * 3
      if (stored != null && stored.length >= 3) {
        topNames[base] = stored[0]
        topNames[base + 1] = stored[1]
        topNames[base + 2] = stored[2]
      } else {
        val fb = FALLBACK_NAMES[i]
        topNames[base] = fb[0]
        topNames[base + 1] = fb[1]
        topNames[base + 2] = fb[2]
      }
      i++
    }
    hydrated = true
  }

  fun checkIfQualifies(newScore: Int): Boolean {
    var score = newScore
    if (score < 0) score = 0
    if (score > 99_999_999) score = 99_999_999
    return score > topScores[SLOT_COUNT - 1]
  }

  fun checkAndInsertNewScore(
    context: Context,
    newScore: Int,
    char1: Char,
    char2: Char,
    char3: Char,
    maxStage: Int,
  ): Boolean {
    var score = newScore
    if (score < 0) score = 0
    if (score > 99_999_999) score = 99_999_999
    var stage = maxStage
    if (stage < 0) stage = 0
    if (score <= topScores[SLOT_COUNT - 1]) return false
    var targetIndex = -1
    var scan = 0
    while (scan < SLOT_COUNT) {
      if (score > topScores[scan]) {
        targetIndex = scan
        break
      }
      scan++
    }
    if (targetIndex < 0) return false
    var i = SLOT_COUNT - 2
    while (i >= targetIndex) {
      val next = i + 1
      if (i >= 0 && next < SLOT_COUNT) {
        topScores[next] = topScores[i]
        topStages[next] = topStages[i]
        val dst = next * 3
        val src = i * 3
        if (dst <= topNames.size - 3 && src <= topNames.size - 3 && src >= 0 && dst >= 0) {
          topNames[dst] = topNames[src]
          topNames[dst + 1] = topNames[src + 1]
          topNames[dst + 2] = topNames[src + 2]
        }
      }
      i--
    }
    if (targetIndex in 0 until SLOT_COUNT) {
      topScores[targetIndex] = score
      topStages[targetIndex] = stage
      val nameBase = targetIndex * 3
      if (nameBase <= topNames.size - 3) {
        topNames[nameBase] = char1
        topNames[nameBase + 1] = char2
        topNames[nameBase + 2] = char3
      }
    }
    persist(context)
    return true
  }

  private fun persist(context: Context) {
    val editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
    var i = 0
    while (i < SLOT_COUNT) {
      editor.putInt(SCORE_KEYS[i], topScores[i])
      editor.putInt(STAGE_KEYS[i], topStages[i])
      nameWriteBuf.setLength(0)
      val base = i * 3
      if (base <= topNames.size - 3) {
        nameWriteBuf.append(topNames[base])
        nameWriteBuf.append(topNames[base + 1])
        nameWriteBuf.append(topNames[base + 2])
        editor.putString(NAME_KEYS[i], nameWriteBuf.toString())
      }
      i++
    }
    editor.apply()
  }
}
