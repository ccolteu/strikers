package com.cc.ww2blitz

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface

/**
 * Zero-alloc stage-clear recap layout. Labels live in pre-cached char arrays;
 * numbers are written with integer-to-digit parsing into [line].
 */
class UIController {

  private val line = CharArray(LINE_CAP)
  private val fillPaint = Paint().apply {
    color = Color.WHITE
    typeface = Typeface.DEFAULT_BOLD
    textSize = 32f
    isAntiAlias = true
    isFilterBitmap = false
  }
  private val shadowPaint = Paint().apply {
    color = Color.BLACK
    typeface = Typeface.DEFAULT_BOLD
    textSize = 32f
    isAntiAlias = true
    isFilterBitmap = false
  }
  private val goldPaint = Paint().apply {
    color = Color.YELLOW
    typeface = Typeface.DEFAULT_BOLD
    textSize = 42f
    isAntiAlias = true
    isFilterBitmap = false
  }
  private val goldShadowPaint = Paint().apply {
    color = Color.BLACK
    typeface = Typeface.DEFAULT_BOLD
    textSize = 42f
    isAntiAlias = true
    isFilterBitmap = false
  }
  private val accentPaint = Paint().apply {
    color = 0xFF3A3A3A.toInt()
    typeface = Typeface.DEFAULT_BOLD
    textSize = 32f
    isAntiAlias = true
  }

  fun bindTypeface(face: Typeface) {
    fillPaint.typeface = face
    shadowPaint.typeface = face
    goldPaint.typeface = face
    goldShadowPaint.typeface = face
    accentPaint.typeface = face
  }

  fun onSizeChanged(width: Int, height: Int) {
    val short = if (width < height) width else height
    val body = short * 0.038f
    val title = short * 0.048f
    fillPaint.textSize = body
    shadowPaint.textSize = body
    accentPaint.textSize = body
    goldPaint.textSize = title
    goldShadowPaint.textSize = title
  }

  fun drawStageClear(canvas: Canvas, screenW: Int, screenH: Int, scores: ScoreManager, stage: Int) {
    val cx = screenW * 0.5f
    val phase = scores.recapPhase()
    var n = writeChars(line, 0, HDR_A)
    n = writeInt(line, n, stage)
    n = writeChars(line, n, HDR_B)
    drawCentered(canvas, line, n, cx, screenH * 0.20f, goldPaint, goldShadowPaint)

    if (phase >= ScoreManager.PHASE_LIVES) {
      n = writeChars(line, 0, LIVES_LABEL)
      n = writeInt(line, n, scores.recapLivesCount())
      n = writeChars(line, n, LIVES_MULT)
      drawCentered(canvas, line, n, cx, screenH * 0.34f, fillPaint, shadowPaint)
    }
    if (phase >= ScoreManager.PHASE_BOMBS) {
      n = writeChars(line, 0, BOMBS_LABEL)
      n = writeInt(line, n, scores.recapBombsCount())
      n = writeChars(line, n, BOMBS_MULT)
      drawCentered(canvas, line, n, cx, screenH * 0.44f, fillPaint, shadowPaint)
    }
    if (phase >= ScoreManager.PHASE_GRAZE) {
      n = writeChars(line, 0, GRAZE_LABEL)
      n = writeInt(line, n, scores.recapGrazeCount())
      n = writeChars(line, n, GRAZE_MULT)
      drawCentered(canvas, line, n, cx, screenH * 0.54f, fillPaint, shadowPaint)
    }
    if (phase >= ScoreManager.PHASE_TOTAL) {
      n = writeChars(line, 0, TOTAL_LABEL)
      drawCentered(canvas, line, n, cx, screenH * 0.66f, goldPaint, goldShadowPaint)
      n = writeInt(line, 0, scores.recapBonusTotal())
      drawCentered(canvas, line, n, cx, screenH * 0.74f, goldPaint, goldShadowPaint)
      if ((scores.recapFrame() % FLASH_PERIOD) < FLASH_VISIBLE) {
        n = writeChars(line, 0, PRESS_FIRE)
        drawCentered(canvas, line, n, cx, screenH * 0.86f, fillPaint, shadowPaint)
      }
    }
  }

  private fun drawCentered(
    canvas: Canvas,
    buf: CharArray,
    count: Int,
    centerX: Float,
    y: Float,
    fill: Paint,
    shadow: Paint,
  ) {
    if (count <= 0) return
    val w = fill.measureText(buf, 0, count)
    val x = centerX - w * 0.5f
    accentPaint.textSize = fill.textSize
    accentPaint.typeface = fill.typeface
    if (fill === goldPaint) {
      accentPaint.color = 0xFFB35400.toInt()
    } else {
      accentPaint.color = 0xFF3A3A3A.toInt()
    }
    canvas.drawText(buf, 0, count, x + 4f, y + 4f, shadow)
    canvas.drawText(buf, 0, count, x + 2f, y + 2f, accentPaint)
    canvas.drawText(buf, 0, count, x, y, fill)
  }

  private fun writeChars(dst: CharArray, offset: Int, src: CharArray): Int {
    var i = 0
    var o = offset
    while (i < src.size && o < LINE_CAP) {
      dst[o] = src[i]
      o++
      i++
    }
    return o
  }

  private fun writeInt(dst: CharArray, offset: Int, value: Int): Int {
    var v = value
    if (v < 0) v = 0
    if (offset >= LINE_CAP) return offset
    if (v == 0) {
      dst[offset] = '0'
      return offset + 1
    }
    var digits = 0
    var tmp = v
    while (tmp > 0) {
      digits++
      tmp /= 10
    }
    if (offset + digits > LINE_CAP) {
      digits = LINE_CAP - offset
    }
    tmp = v
    var i = digits - 1
    while (i >= 0) {
      dst[offset + i] = ('0'.code + (tmp % 10)).toChar()
      tmp /= 10
      i--
    }
    return offset + digits
  }

  companion object {
    private const val LINE_CAP = 80
    private const val FLASH_PERIOD = 60
    private const val FLASH_VISIBLE = 30
    private val HDR_A = charArrayOf('S', 'T', 'A', 'G', 'E', ' ')
    private val HDR_B = charArrayOf(' ', 'C', 'L', 'E', 'A', 'R')
    private val LIVES_LABEL = charArrayOf(
      'L', 'I', 'V', 'E', 'S', ' ', 'B', 'O', 'N', 'U', 'S', ':', ' ',
    )
    private val LIVES_MULT = charArrayOf(' ', 'x', ' ', '5', '0', ',', '0', '0', '0')
    private val BOMBS_LABEL = charArrayOf(
      'B', 'O', 'M', 'B', 'S', ' ', 'B', 'O', 'N', 'U', 'S', ':', ' ',
    )
    private val BOMBS_MULT = charArrayOf(' ', 'x', ' ', '2', '0', ',', '0', '0', '0')
    private val GRAZE_LABEL = charArrayOf(
      'G', 'R', 'A', 'Z', 'E', ' ', 'B', 'O', 'N', 'U', 'S', ':', ' ',
    )
    private val GRAZE_MULT = charArrayOf(' ', 'x', ' ', '5', '0', '0')
    private val TOTAL_LABEL = charArrayOf(
      'S', 'T', 'A', 'G', 'E', ' ', 'C', 'L', 'E', 'A', 'R', ' ', 'T', 'O', 'T', 'A', 'L', ':',
    )
    private val PRESS_FIRE = charArrayOf(
      'P', 'R', 'E', 'S', 'S', ' ', 'F', 'I', 'R', 'E', ' ',
      'T', 'O', ' ', 'C', 'O', 'N', 'T', 'I', 'N', 'U', 'E',
    )
  }
}
