package com.cc.ww2blitz

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
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
  private val creditBreakWidth = FloatArray(1)
  private val interstitialCards = arrayOfNulls<Bitmap>(6)
  private val interstitialDst = RectF()
  private val interstitialCardPaint = Paint().apply {
    isFilterBitmap = true
    isAntiAlias = false
  }

  fun bindTypeface(face: Typeface) {
    fillPaint.typeface = face
    shadowPaint.typeface = face
    goldPaint.typeface = face
    goldShadowPaint.typeface = face
    accentPaint.typeface = face
  }

  fun loadInterstitials(resources: Resources, width: Int, height: Int) {
    if (width <= 0 || height <= 0) return
    if (interstitialCards[0] != null && interstitialCards[0]?.isRecycled == false) {
      return
    }
    releaseInterstitials()
    var i = 0
    while (i < 6) {
      interstitialCards[i] = decodeNativeAspect(resources, INTERSTITIAL_IDS[i])
      i++
    }
  }

  fun releaseInterstitials() {
    var i = 0
    while (i < 6) {
      val bmp = interstitialCards[i]
      if (bmp != null && !bmp.isRecycled) bmp.recycle()
      interstitialCards[i] = null
      i++
    }
  }

  fun drawStageInterstitial(
    canvas: Canvas,
    screenW: Float,
    screenH: Float,
    timer: Float,
    stageId: Int,
  ) {
    var elapsed = INTERSTITIAL_DURATION - timer
    if (elapsed < 0f) elapsed = 0f
    var fade = 1f
    if (elapsed < INTERSTITIAL_FADE_IN) {
      fade = elapsed / INTERSTITIAL_FADE_IN
      if (fade < 0f) fade = 0f
      if (fade > 1f) fade = 1f
    }
    val cardA = (fade * 255f).toInt()
    interstitialCardPaint.alpha = cardA
    val slot = stageId - 1
    val bmp = if (slot >= 0 && slot < 6) interstitialCards[slot] else null
    canvas.drawColor(Color.BLACK)
    var calculatedTop = 0f
    if (bmp != null && !bmp.isRecycled && bmp.width > 0) {
      val scale = screenW / bmp.width.toFloat()
      val drawH = bmp.height.toFloat() * scale
      calculatedTop = (screenH - drawH) * 0.5f
      interstitialDst.set(0f, calculatedTop, screenW, calculatedTop + drawH)
      canvas.drawBitmap(bmp, null, interstitialDst, interstitialCardPaint)
    }
    val mission = operationHeader(stageId)
    val cx = screenW * 0.5f
    val line1Y = calculatedTop + (goldPaint.textSize * 4.2f)
    val line2Y = line1Y + goldPaint.textSize * 1.3f
    val goldA = goldPaint.alpha
    val goldShA = goldShadowPaint.alpha
    goldPaint.alpha = cardA
    goldShadowPaint.alpha = cardA
    drawCentered(canvas, OP_PREFIX, 0, OP_PREFIX.size, cx, line1Y, goldPaint, goldShadowPaint)
    drawCentered(canvas, mission, 0, mission.size, cx, line2Y, goldPaint, goldShadowPaint)
    goldPaint.alpha = goldA
    goldShadowPaint.alpha = goldShA
  }

  private fun operationHeader(stageId: Int): CharArray {
    if (stageId == 1) return OP_STAGE1
    if (stageId == 2) return OP_STAGE2
    if (stageId == 3) return OP_STAGE3
    if (stageId == 4) return OP_STAGE4
    if (stageId == 5) return OP_STAGE5
    if (stageId == 6) return OP_STAGE6
    return OP_STAGE1
  }

  private fun decodeNativeAspect(resources: Resources, id: Int): Bitmap {
    val opts = BitmapFactory.Options().apply {
      inScaled = false
      inPreferredConfig = Bitmap.Config.ARGB_8888
    }
    return BitmapFactory.decodeResource(resources, id, opts)
      ?: error("Missing drawable $id")
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

  fun drawCampaignCompleteCredits(
    canvas: Canvas,
    screenW: Int,
    screenH: Int,
    elapsedSeconds: Float,
  ) {
    val cx = screenW * 0.5f
    val h = screenH.toFloat()
    val maxWidth = screenW * CREDIT_MAX_WIDTH_FRAC
    var y = h - (elapsedSeconds * CREDIT_SCROLL_PX)
    var index = 0
    val count = CREDIT_LINES.size
    while (index < count) {
      val src = CREDIT_CHARS[index]
      val gold = index == 0 || index == CREDIT_THANK_INDEX
      if (gold) {
        y = wrapCreditLine(canvas, src, cx, y, h, maxWidth, goldPaint, goldShadowPaint)
      } else {
        y = wrapCreditLine(canvas, src, cx, y, h, maxWidth, fillPaint, shadowPaint)
      }
      index++
    }
  }

  /**
   * Word-wraps a pre-cached char row with [Paint.breakText]. No String, split,
   * StaticLayout, or iterator on the frame path.
   */
  private fun wrapCreditLine(
    canvas: Canvas,
    src: CharArray,
    centerX: Float,
    originY: Float,
    screenH: Float,
    maxWidth: Float,
    fill: Paint,
    shadow: Paint,
  ): Float {
    val n = src.size
    val gap = CREDIT_LINE_GAP
    var start = 0
    var y = originY
    var drew = false
    while (start < n) {
      while (start < n && src[start] == ' ') {
        start++
      }
      if (start >= n) break
      val remaining = n - start
      var fit = fill.breakText(src, start, remaining, maxWidth, creditBreakWidth)
      if (fit <= 0) {
        fit = 1
      }
      var end = start + fit
      if (fit < remaining) {
        var space = -1
        var i = end - 1
        while (i > start) {
          if (src[i] == ' ') {
            space = i
            break
          }
          i--
        }
        if (space > start) {
          end = space
        }
      }
      val len = end - start
      if (len > 0) {
        if (y >= -60f && y <= screenH + 60f) {
          drawCentered(canvas, src, start, len, centerX, y, fill, shadow)
        }
        y += gap
        drew = true
      }
      start = end
    }
    if (!drew) {
      y += gap
    }
    return y
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

  fun drawCenteredHud(
    canvas: Canvas,
    text: CharSequence,
    centerX: Float,
    y: Float,
    fill: Paint,
    shadow: Paint,
  ) {
    val n = text.length
    if (n <= 0) return
    var i = 0
    while (i < n && i < LINE_CAP) {
      line[i] = text[i]
      i++
    }
    drawCentered(canvas, line, i, centerX, y, fill, shadow)
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
    drawCentered(canvas, buf, 0, count, centerX, y, fill, shadow)
  }

  private fun drawCentered(
    canvas: Canvas,
    buf: CharArray,
    offset: Int,
    count: Int,
    centerX: Float,
    y: Float,
    fill: Paint,
    shadow: Paint,
  ) {
    if (count <= 0) return
    val w = fill.measureText(buf, offset, count)
    val x = centerX - w * 0.5f
    accentPaint.textSize = fill.textSize
    accentPaint.typeface = fill.typeface
    if (fill === goldPaint) {
      accentPaint.color = 0xFFB35400.toInt()
    } else {
      accentPaint.color = 0xFF3A3A3A.toInt()
    }
    canvas.drawText(buf, offset, count, x + 4f, y + 4f, shadow)
    canvas.drawText(buf, offset, count, x + 2f, y + 2f, accentPaint)
    canvas.drawText(buf, offset, count, x, y, fill)
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
    private const val CREDIT_SCROLL_PX = 75f
    private const val CREDIT_LINE_GAP = 55f
    private const val CREDIT_MAX_WIDTH_FRAC = 0.85f
    private const val CREDIT_THANK_INDEX = 18
    private val CREDIT_LINES = arrayOf(
      "WW2 BLITZ",
      " ",
      "ALL STAGES COMPLETED",
      "---------------------",
      "LEAD GAME DESIGNER",
      "CC / GEMINI",
      " ",
      "LEAD GAME DEVELOPER",
      "CURSOR / GROK",
      " ",
      "GRAPHICS ENGINEER",
      "32BIT ART POOL SELECTION",
      " ",
      "SOUND DESIGNER",
      "Arcade Cabinet FM-Synthesis",
      " ",
      "SPECIAL THANKS TO",
      "THE SHMUP COMMUNITY",
      " ",
      "THANK YOU FOR PLAYING!",
    )
    private val CREDIT_CHARS: Array<CharArray> = Array(CREDIT_LINES.size) { i ->
      CREDIT_LINES[i].toCharArray()
    }
    private const val INTERSTITIAL_DURATION = 3.0f
    private const val INTERSTITIAL_FADE_IN = 0.5f
    private val INTERSTITIAL_IDS = intArrayOf(
      R.drawable.interstitial_stage1,
      R.drawable.interstitial_stage2,
      R.drawable.interstitial_stage3,
      R.drawable.interstitial_stage4,
      R.drawable.interstitial_stage5,
      R.drawable.interstitial_stage6,
    )
    private val OP_PREFIX = charArrayOf(
      'O', 'P', 'E', 'R', 'A', 'T', 'I', 'O', 'N', ':',
    )
    private val OP_STAGE1 = charArrayOf(
      'C', 'L', 'O', 'U', 'D', ' ', 'F', 'O', 'R', 'T', 'R', 'E', 'S', 'S',
    )
    private val OP_STAGE2 = charArrayOf(
      'I', 'R', 'O', 'N', ' ', 'T', 'R', 'E', 'A', 'D', 'S',
    )
    private val OP_STAGE3 = charArrayOf(
      'S', 'T', 'E', 'E', 'L', ' ', 'A', 'T', 'L', 'A', 'N', 'T', 'I', 'C',
    )
    private val OP_STAGE4 = charArrayOf(
      'J', 'U', 'N', 'G', 'L', 'E', ' ', 'R', 'U', 'I', 'N', 'S',
    )
    private val OP_STAGE5 = charArrayOf(
      'A', 'S', 'C', 'E', 'N', 'T', ' ', 'C', 'A', 'N', 'O', 'P', 'Y',
    )
    private val OP_STAGE6 = charArrayOf(
      'O', 'R', 'B', 'I', 'T', ' ', 'T', 'H', 'R', 'E', 'S', 'H', 'O', 'L', 'D',
    )
  }
}
