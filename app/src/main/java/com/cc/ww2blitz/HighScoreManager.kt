package com.cc.ww2blitz

import android.content.Context

object HighScoreManager {

  const val SLOT_COUNT = 10
  const val DIFF_TABLES = 7

  private val topScores = IntArray(DIFF_TABLES * SLOT_COUNT)
  private val topNames = CharArray(DIFF_TABLES * SLOT_COUNT * 3) { 'A' }
  private val topStages = IntArray(DIFF_TABLES * SLOT_COUNT)
  private val fallbackScores = intArrayOf(
    100000, 90000, 80000, 70000, 60000, 50000, 40000, 30000, 20000, 10000,
  )
  private val fallbackStages = intArrayOf(4, 3, 3, 2, 2, 1, 1, 1, 1, 1)
  private val nameWriteBuf = StringBuilder(3)
  private var hydrated = false

  private const val PREFS_NAME = "arcade_leaderboard"
  private val LEGACY_SCORE_KEYS = arrayOf(
    "score_0", "score_1", "score_2", "score_3", "score_4",
    "score_5", "score_6", "score_7", "score_8", "score_9",
  )
  private val LEGACY_STAGE_KEYS = arrayOf(
    "stage_0", "stage_1", "stage_2", "stage_3", "stage_4",
    "stage_5", "stage_6", "stage_7", "stage_8", "stage_9",
  )
  private val LEGACY_NAME_KEYS = arrayOf(
    "name_0", "name_1", "name_2", "name_3", "name_4",
    "name_5", "name_6", "name_7", "name_8", "name_9",
  )
  private val FALLBACK_NAMES = arrayOf(
    "PSK", "STK", "ACE", "SHM", "AAA", "AAA", "AAA", "AAA", "AAA", "AAA",
  )

  init {
    var table = 0
    while (table < DIFF_TABLES) {
      seedTable(table)
      table++
    }
  }

  fun scoreAt(difficultyIndex: Int, index: Int): Int = topScores[slot(difficultyIndex, index)]

  fun stageAt(difficultyIndex: Int, index: Int): Int = topStages[slot(difficultyIndex, index)]

  fun nameChar(difficultyIndex: Int, index: Int, charIndex: Int): Char =
    topNames[slot(difficultyIndex, index) * 3 + charIndex]

  fun loadHighScores(context: Context) {
    if (hydrated) return
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    var table = 0
    while (table < DIFF_TABLES) {
      val dip = table + 1
      val migrated = table == 2 && !prefs.contains(scoreKey(dip, 0)) && prefs.contains(LEGACY_SCORE_KEYS[0])
      var i = 0
      while (i < SLOT_COUNT) {
        val si = slot(dip, i)
        if (migrated) {
          topScores[si] = prefs.getInt(LEGACY_SCORE_KEYS[i], fallbackScores[i])
          topStages[si] = prefs.getInt(LEGACY_STAGE_KEYS[i], fallbackStages[i])
          writeName(si, prefs.getString(LEGACY_NAME_KEYS[i], FALLBACK_NAMES[i]))
        } else {
          topScores[si] = prefs.getInt(scoreKey(dip, i), fallbackScores[i])
          topStages[si] = prefs.getInt(stageKey(dip, i), fallbackStages[i])
          writeName(si, prefs.getString(nameKey(dip, i), FALLBACK_NAMES[i]))
        }
        i++
      }
      table++
    }
    hydrated = true
    persist(context)
  }

  fun checkIfQualifies(newScore: Int, difficultyIndex: Int): Boolean {
    var score = newScore
    if (score < 0) score = 0
    if (score > 99_999_999) score = 99_999_999
    return score > topScores[slot(difficultyIndex, SLOT_COUNT - 1)]
  }

  fun checkAndInsertNewScore(
    context: Context,
    newScore: Int,
    char1: Char,
    char2: Char,
    char3: Char,
    maxStage: Int,
    difficultyIndex: Int,
  ): Boolean {
    var score = newScore
    if (score < 0) score = 0
    if (score > 99_999_999) score = 99_999_999
    var stage = maxStage
    if (stage < 0) stage = 0
    val last = slot(difficultyIndex, SLOT_COUNT - 1)
    if (score <= topScores[last]) return false
    var targetIndex = -1
    var scan = 0
    while (scan < SLOT_COUNT) {
      if (score > topScores[slot(difficultyIndex, scan)]) {
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
        copyRow(difficultyIndex, i, next)
      }
      i--
    }
    val dest = slot(difficultyIndex, targetIndex)
    topScores[dest] = score
    topStages[dest] = stage
    val nameBase = dest * 3
    topNames[nameBase] = char1
    topNames[nameBase + 1] = char2
    topNames[nameBase + 2] = char3
    persist(context)
    return true
  }

  private fun copyRow(difficultyIndex: Int, fromIndex: Int, toIndex: Int) {
    val src = slot(difficultyIndex, fromIndex)
    val dst = slot(difficultyIndex, toIndex)
    topScores[dst] = topScores[src]
    topStages[dst] = topStages[src]
    val sb = src * 3
    val db = dst * 3
    topNames[db] = topNames[sb]
    topNames[db + 1] = topNames[sb + 1]
    topNames[db + 2] = topNames[sb + 2]
  }

  private fun persist(context: Context) {
    val editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
    var table = 0
    while (table < DIFF_TABLES) {
      val dip = table + 1
      var i = 0
      while (i < SLOT_COUNT) {
        val si = slot(dip, i)
        editor.putInt(scoreKey(dip, i), topScores[si])
        editor.putInt(stageKey(dip, i), topStages[si])
        nameWriteBuf.setLength(0)
        val base = si * 3
        nameWriteBuf.append(topNames[base])
        nameWriteBuf.append(topNames[base + 1])
        nameWriteBuf.append(topNames[base + 2])
        editor.putString(nameKey(dip, i), nameWriteBuf.toString())
        i++
      }
      table++
    }
    editor.apply()
  }

  private fun seedTable(table: Int) {
    val dip = table + 1
    var i = 0
    while (i < SLOT_COUNT) {
      val si = slot(dip, i)
      topScores[si] = fallbackScores[i]
      topStages[si] = fallbackStages[i]
      writeFallbackName(si, i)
      i++
    }
  }

  private fun writeName(si: Int, stored: String?) {
    val base = si * 3
    if (stored != null && stored.length >= 3) {
      topNames[base] = stored[0]
      topNames[base + 1] = stored[1]
      topNames[base + 2] = stored[2]
    } else {
      topNames[base] = 'A'
      topNames[base + 1] = 'A'
      topNames[base + 2] = 'A'
    }
  }

  private fun writeFallbackName(si: Int, i: Int) {
    val base = si * 3
    val fb = FALLBACK_NAMES[i]
    topNames[base] = fb[0]
    topNames[base + 1] = fb[1]
    topNames[base + 2] = fb[2]
  }

  private fun slot(difficultyIndex: Int, index: Int): Int {
    var d = difficultyIndex - 1
    if (d < 0) d = 2
    if (d >= DIFF_TABLES) d = DIFF_TABLES - 1
    var i = index
    if (i < 0) i = 0
    if (i >= SLOT_COUNT) i = SLOT_COUNT - 1
    return d * SLOT_COUNT + i
  }

  private fun scoreKey(dip: Int, i: Int): String = "d${dip}_score_$i"

  private fun stageKey(dip: Int, i: Int): String = "d${dip}_stage_$i"

  private fun nameKey(dip: Int, i: Int): String = "d${dip}_name_$i"
}
