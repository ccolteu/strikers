package com.example.strikers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Build
import android.view.Choreographer
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView

/**
 * Choreographer-driven SurfaceView loop. Scale parallax in [onSizeChanged],
 * then [ParallaxBackground.update] / [ParallaxBackground.draw] every frame.
 */
class GameView(context: Context) : SurfaceView(context), SurfaceHolder.Callback, Choreographer.FrameCallback {

  private val parallax = ParallaxBackground(resources)
  private val player = PlayerShip(resources)
  private val bullets = BulletManager()
  private val homingMissiles = HomingMissileManager(resources)
  private val enemies = EnemyPoolManager(resources)
  private val enemyShots = EnemyWeaponSystem()
  private val timeline = SpawnTimeline()
  private val particles = ParticleManager(resources)
  private val boss = BossController(resources)
  private val scorecard = VictoryScorecard()
  private var campaignScore = 0
  private val panicBomb = PanicBomb()
  private val powerUpItem = PowerUpItem()
  private val scorePool = Array(12) { FloatingScore() }
  private val stageManager = StageData()
  private val srcCore = Rect()
  private val bombDstRect = RectF()
  private val hudIconDst = RectF()
  private val powerUpDst = RectF()
  private val bgmSliderRect = RectF()
  private val sfxSliderRect = RectF()
  private val backButtonRect = RectF()
  private val openSettingsButtonRect = RectF()
  private val bodyPaint = Paint().apply { isFilterBitmap = true }
  private val hudOutlinePaint = Paint().apply {
    isFilterBitmap = true
    colorFilter = PorterDuffColorFilter(Color.BLACK, PorterDuff.Mode.SRC_IN)
  }
  private val hudShadowPaint = Paint().apply {
    isFilterBitmap = true
    colorFilter = PorterDuffColorFilter(0xCC000000.toInt(), PorterDuff.Mode.SRC_IN)
  }
  private val bombSheets = arrayOfNulls<Bitmap>(6)
  private var lifeIconBmp: Bitmap? = null
  private var bombIconBmp: Bitmap? = null
  private var availableBombs = 3
  private var gameState = STATE_TITLE
  private var isSettingsMenuOpen = false
  private var logoBmp: Bitmap? = null
  private var powerUpBmp: Bitmap? = null
  private var bombPickupBmp: Bitmap? = null
  private val medalFrames = arrayOfNulls<Bitmap>(PowerUpItem.MEDAL_FRAME_COUNT)
  private var bgStage2Bmp: Bitmap? = null
  private var bgStage3Bmp: Bitmap? = null
  private var lastTapUpMs = 0L
  private var touchDownMs = 0L
  private var touchDownX = 0f
  private var touchDownY = 0f
  private var awaitingSecondTap = false
  private var enemyBombDmgBank = 0f
  private var bossBombDmgBank = 0f
  private var bossFought = false
  private var lastBgmRes = 0
  private var idleT = 0f
  private var demoT = 0f
  private var gameOverT = 0f
  private val choreographer = Choreographer.getInstance()
  private var running = false
  private var lastNanos = 0L
  private var screenShakeTrauma = 0f
  private var shakeSeed = 14352451L
  private var lootSeed = 2463534242L
  private var bossWasExploding = false
  private var screenW = 0
  private var screenH = 0
  private var arcadeTypeface: Typeface? = null
  private val uiTextPaint = Paint().apply {
    color = Color.WHITE
    typeface = Typeface.DEFAULT_BOLD
    textSize = 32f
    isAntiAlias = true
  }
  private val uiShadowPaint = Paint().apply {
    color = Color.BLACK
    typeface = Typeface.DEFAULT_BOLD
    textSize = 32f
    isAntiAlias = true
  }
  private val uiGoldPaint = Paint().apply {
    color = Color.YELLOW
    typeface = Typeface.DEFAULT_BOLD
    textSize = 42f
    isAntiAlias = true
  }
  private val uiGoldShadowPaint = Paint().apply {
    color = Color.BLACK
    typeface = Typeface.DEFAULT_BOLD
    textSize = 42f
    isAntiAlias = true
  }
  private val uiStringBuilder = StringBuilder(80)
  private val uiSmallPaint = Paint().apply {
    color = Color.WHITE
    typeface = Typeface.DEFAULT_BOLD
    textSize = 20f
    isAntiAlias = true
  }
  private val uiSmallShadowPaint = Paint().apply {
    color = Color.BLACK
    typeface = Typeface.DEFAULT_BOLD
    textSize = 20f
    isAntiAlias = true
  }
  private val accentShadowPaint = Paint().apply {
    color = 0xFF3A3A3A.toInt()
    typeface = Typeface.DEFAULT_BOLD
    textSize = 32f
    isAntiAlias = true
  }
  private val popupPaint = Paint().apply {
    color = Color.YELLOW
    typeface = Typeface.DEFAULT_BOLD
    textSize = 28f
    isAntiAlias = true
  }
  private val popupShadowPaint = Paint().apply {
    color = Color.BLACK
    typeface = Typeface.DEFAULT_BOLD
    textSize = 28f
    isAntiAlias = true
  }
  private val uiTrackPaint = Paint().apply {
    color = 0xAA1A1A1A.toInt()
    style = Paint.Style.FILL
    isAntiAlias = true
  }
  private val uiTrackStrokePaint = Paint().apply {
    color = 0xFFFFD54A.toInt()
    style = Paint.Style.STROKE
    strokeWidth = 5f
    isAntiAlias = true
  }
  private val uiHitFullPaint = Paint().apply {
    color = 0xFF3DFF4A.toInt()
    style = Paint.Style.FILL
    isAntiAlias = false
  }
  private val uiHitMidPaint = Paint().apply {
    color = 0xFFC9A000.toInt()
    style = Paint.Style.FILL
    isAntiAlias = false
  }
  private val uiHitWarnPaint = Paint().apply {
    color = 0xFFE53935.toInt()
    style = Paint.Style.FILL
    isAntiAlias = false
  }

  init {
    holder.addCallback(this)
    isFocusable = true
    isFocusableInTouchMode = true
    setWillNotDraw(true)
    try {
      arcadeTypeface = Typeface.createFromAsset(context.assets, "fonts/arcade_font.ttf")
    } catch (_: Exception) {
    }
    val face = arcadeTypeface ?: Typeface.DEFAULT_BOLD
    uiTextPaint.typeface = face
    uiShadowPaint.typeface = face
    uiGoldPaint.typeface = face
    uiGoldShadowPaint.typeface = face
    uiSmallPaint.typeface = face
    uiSmallShadowPaint.typeface = face
    accentShadowPaint.typeface = face
    popupPaint.typeface = face
    popupShadowPaint.typeface = face
  }

  override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
    super.onSizeChanged(w, h, oldw, oldh)
    parallax.onSizeChanged(w, h)
    player.onSizeChanged(w, h)
    enemies.onSizeChanged(w, h)
    enemyShots.onSizeChanged(w, h)
    homingMissiles.onSizeChanged(w, h)
    particles.onSizeChanged(w, h)
    boss.onSizeChanged(w, h)
    screenW = w
    screenH = h
    loadBombSheetsIfNeeded()
    loadHudIconsIfNeeded()
    loadStage2Background(w, h)
    loadStage3Background(w, h)
  }

  override fun surfaceCreated(holder: SurfaceHolder) {
    running = true
    lastNanos = 0L
    choreographer.postFrameCallback(this)
  }

  override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
    parallax.onSizeChanged(width, height)
    player.onSizeChanged(width, height)
    enemies.onSizeChanged(width, height)
    enemyShots.onSizeChanged(width, height)
    homingMissiles.onSizeChanged(width, height)
    particles.onSizeChanged(width, height)
    boss.onSizeChanged(width, height)
    screenW = width
    screenH = height
    loadBombSheetsIfNeeded()
    loadHudIconsIfNeeded()
    loadStage2Background(width, height)
    loadStage3Background(width, height)
  }

  override fun surfaceDestroyed(holder: SurfaceHolder) {
    running = false
    choreographer.removeFrameCallback(this)
  }

  override fun doFrame(frameTimeNanos: Long) {
    if (!running) return
    val dt = if (lastNanos == 0L) 0f else {
      ((frameTimeNanos - lastNanos).coerceIn(0L, MAX_FRAME_NS) / 1_000_000_000f)
    }
    lastNanos = frameTimeNanos
    screenShakeTrauma = (screenShakeTrauma - 1.2f * dt).coerceAtLeast(0f)
    when (gameState) {
      STATE_TITLE -> {
        parallax.update(TITLE_SCROLL_PX * dt)
        if (!isSettingsMenuOpen) {
          idleT += dt
          if (idleT >= IDLE_SECS && screenW > 0 && screenH > 0) {
            beginDemo()
          }
        }
      }
      STATE_CLEAR -> {
        parallax.update(0f)
        scorecard.update(dt)
        updateFloatingScores(dt)
        particles.update(dt)
      }
      STATE_GAMEOVER -> {
        parallax.update(0f)
        particles.update(dt)
        gameOverT += dt
        if (gameOverT >= GAMEOVER_SECS) {
          returnToTitle()
        }
      }
      else -> {
        parallax.update(stageManager.scrollSpeedY * dt)
        scorecard.update(dt)
        if (gameState == STATE_DEMO) {
          demoPilot(dt)
        }
        player.update(dt)
        bullets.update(dt, player, screenW, homingMissiles)
        homingMissiles.update(dt, enemies.getEnemyPool(), enemies.getPoolSize(), boss)
        powerUpItem.update(dt, screenW, screenH)
        updateFloatingScores(dt)
        timeline.update(
          dt,
          enemies,
          screenW,
          screenH,
          boss,
          stageManager.currentStage,
          stageManager.targetBossTimelineSeconds,
          true,
        )
        enemies.update(dt, player.getHitboxX(), player.getHitboxY(), enemyShots)
        boss.update(dt, player.getHitboxX(), player.getHitboxY(), enemyShots)
        if (boss.isActive() || boss.isExploding()) {
          bossFought = true
        }
        val exploding = boss.isExploding()
        if (exploding && !bossWasExploding) {
          addScreenShake(1.0f)
        }
        bossWasExploding = exploding
        enemyShots.update(dt)
        updatePanicBomb(dt)
        particles.update(dt)
        resolveCollisions()
        if (
          gameState == STATE_PLAYING &&
          bossFought &&
          !boss.isActive() &&
          !boss.isExploding()
        ) {
          scorecard.trigger(player.getHealth(), availableBombs, campaignScore)
          campaignScore = scorecard.totalStageScore
          gameState = STATE_CLEAR
        }
        if (gameState == STATE_DEMO) {
          demoT += dt
          val demoBossDone = bossFought && !boss.isActive() && !boss.isExploding()
          if (player.isGameOver() || demoBossDone || demoT >= DEMO_SECS) {
            exitDemo()
          }
        }
      }
    }
    syncBgm()
    val canvas = lockGameCanvas()
    if (canvas != null) {
      try {
        parallax.draw(
          canvas,
          stageGroundBitmap(),
          stageManager.currentStage == 1,
        )
        val shaking = screenShakeTrauma > 0f && gameState != STATE_TITLE
        if (shaking) {
          val power = screenShakeTrauma * screenShakeTrauma * 30f
          canvas.save()
          canvas.translate(nextShakeUnit() * power, nextShakeUnit() * power)
        }
        if (gameState != STATE_TITLE) {
          enemies.draw(canvas)
          boss.draw(canvas)
          player.draw(canvas)
          drawPowerUpItem(canvas)
          bullets.draw(canvas)
          homingMissiles.draw(canvas)
          enemyShots.draw(canvas)
          particles.draw(canvas)
          drawPanicBomb(canvas)
        }
        if (shaking) {
          canvas.restore()
        }
        drawArcadeUI(canvas)
      } finally {
        holder.unlockCanvasAndPost(canvas)
      }
    }
    choreographer.postFrameCallback(this)
  }

  fun addScreenShake(intensity: Float) {
    screenShakeTrauma = (screenShakeTrauma + intensity).coerceAtMost(1.0f)
  }

  private fun nextShakeUnit(): Float {
    shakeSeed = shakeSeed * 1664525L + 1013904223L
    return (((shakeSeed ushr 8) and 0xFFFFFFL).toFloat() / 16777215f) * 2f - 1f
  }

  /**
   * GPU-backed canvas on API 26+ ([SurfaceHolder.lockHardwareCanvas]).
   * The platform method landed in Oreo; pre-O devices fall back to software
   * [SurfaceHolder.lockCanvas].
   */
  private fun drawArcadeUI(canvas: Canvas) {
    if (gameState == STATE_TITLE) {
      drawTitleScreen(canvas)
      return
    }
    val bombWidth = 80f
    val bombHeight = 80f
    val lifeSize = 72f
    val marginBottom = 45f
    val bombStartY = screenH - marginBottom - bombHeight
    val lifeStartY = bombStartY + (bombHeight - lifeSize) * 0.5f
    val lifeBmp = lifeIconBmp
    if (lifeBmp != null) {
      val currentLives = player.getHealth()
      val marginLeft = 45f
      val spacing = 16f
      var i = 0
      while (i < currentLives) {
        val posX = marginLeft + i * (lifeSize + spacing)
        hudIconDst.set(posX, lifeStartY, posX + lifeSize, lifeStartY + lifeSize)
        blitHudIcon(canvas, lifeBmp)
        i++
      }
    }
    val bombBmp = bombIconBmp
    if (bombBmp != null) {
      val marginRight = 45f
      val spacing = 20f
      var i = 0
      while (i < availableBombs) {
        val posX = screenW - marginRight - bombWidth - (i * (bombWidth + spacing))
        hudIconDst.set(posX, bombStartY, posX + bombWidth, bombStartY + bombHeight)
        blitHudIcon(canvas, bombBmp)
        i++
      }
    }
    if (player.getHealth() > 0) {
      val currentHits = player.getHitsLeft()
      val maxHits = player.getMaxHitsPerLife()
      val barWidth = 240f
      val barH = 12f
      val barLeft = (screenW - barWidth) * 0.5f
      val barTop = lifeStartY + lifeSize + 8f
      hudIconDst.set(barLeft, barTop, barLeft + barWidth, barTop + barH)
      canvas.drawRect(hudIconDst, uiShadowPaint)
      val gap = 2f
      val segW = (barWidth - gap * (maxHits + 1)) / maxHits
      val fillPaint = if (currentHits >= 3) {
        uiHitFullPaint
      } else if (currentHits == 2) {
        uiHitMidPaint
      } else {
        uiHitWarnPaint
      }
      val warnVisible = currentHits > 1 || ((System.currentTimeMillis() / 180L) and 1L) == 0L
      var seg = 0
      while (seg < maxHits) {
        if (seg < currentHits && warnVisible) {
          val sx = barLeft + gap + seg * (segW + gap)
          hudIconDst.set(sx, barTop + gap, sx + segW, barTop + barH - gap)
          canvas.drawRect(hudIconDst, fillPaint)
        }
        seg++
      }
    }
    val topTextY = 80f
    uiStringBuilder.setLength(0)
    uiStringBuilder.append("1PADV")
    val startEnd = uiStringBuilder.length
    drawHudTextAt(canvas, uiStringBuilder, 0, startEnd, 30f, topTextY, uiTextPaint, uiShadowPaint)
    uiStringBuilder.setLength(0)
    var score = if (gameState == STATE_CLEAR) scorecard.visibleTotalScore else campaignScore
    if (score < 0) score = 0
    if (score > 99_999_999) score = 99_999_999
    var digits = 1
    var tally = score
    while (tally >= 10) {
      tally /= 10
      digits++
    }
    var pad = 8 - digits
    while (pad > 0) {
      uiStringBuilder.append('0')
      pad--
    }
    uiStringBuilder.append(score)
    val scoreEnd = uiStringBuilder.length
    val scoreW = uiTextPaint.measureText(uiStringBuilder, 0, scoreEnd)
    val scoreX = screenW - scoreW - 30f
    drawHudTextAt(canvas, uiStringBuilder, 0, scoreEnd, scoreX, topTextY, uiTextPaint, uiShadowPaint)
    drawFloatingScores(canvas)
    if (gameState == STATE_DEMO && (demoT * 2f).toInt() % 2 == 0) {
      uiStringBuilder.setLength(0)
      uiStringBuilder.append("DEMO")
      drawCenteredHud(
        canvas,
        uiStringBuilder,
        screenW * 0.5f,
        screenH * 0.16f,
        uiGoldPaint,
        uiGoldShadowPaint,
      )
    }
    if (gameState == STATE_GAMEOVER) {
      if ((gameOverT * 2f).toInt() % 2 == 0) {
        uiStringBuilder.setLength(0)
        uiStringBuilder.append("GAME OVER")
        drawCenteredHud(
          canvas,
          uiStringBuilder,
          screenW * 0.5f,
          screenH * 0.45f,
          uiGoldPaint,
          uiGoldShadowPaint,
        )
      }
      return
    }
    if (gameState != STATE_CLEAR) return
    val cx = screenW * 0.5f
    if (scorecard.currentDisplayLine >= 1) {
      uiStringBuilder.setLength(0)
      uiStringBuilder.append("STAGE ")
      uiStringBuilder.append(stageManager.currentStage)
      uiStringBuilder.append(" CLEAR")
      drawCenteredHud(canvas, uiStringBuilder, cx, screenH * 0.22f, uiGoldPaint, uiGoldShadowPaint)
    }
    if (scorecard.currentDisplayLine >= 2) {
      uiStringBuilder.setLength(0)
      uiStringBuilder.append("LIFE BONUS: ")
      uiStringBuilder.append(scorecard.visibleLifeBonus)
      drawCenteredHud(canvas, uiStringBuilder, cx, screenH * 0.34f, uiTextPaint, uiShadowPaint)
    }
    if (scorecard.currentDisplayLine >= 3) {
      uiStringBuilder.setLength(0)
      uiStringBuilder.append("BOMB BONUS: ")
      uiStringBuilder.append(scorecard.visibleBombBonus)
      drawCenteredHud(canvas, uiStringBuilder, cx, screenH * 0.44f, uiTextPaint, uiShadowPaint)
    }
    if (scorecard.currentDisplayLine >= 4) {
      uiStringBuilder.setLength(0)
      uiStringBuilder.append("TOTAL SCORE: ")
      uiStringBuilder.append(scorecard.visibleTotalScore)
      drawCenteredHud(canvas, uiStringBuilder, cx, screenH * 0.54f, uiGoldPaint, uiGoldShadowPaint)
    }
    if (scorecard.isCountingDone && ((scorecard.elapsedTime * 3f).toInt() and 1) == 0) {
      uiStringBuilder.setLength(0)
      uiStringBuilder.append("TOUCH SCREEN TO CONTINUE")
      drawCenteredHud(canvas, uiStringBuilder, cx, screenH * 0.82f, uiTextPaint, uiShadowPaint)
    }
  }

  private fun drawTitleScreen(canvas: Canvas) {
    if (isSettingsMenuOpen) {
      drawSettingsOverlay(canvas)
      return
    }
    val logo = logoBmp
    if (logo != null && screenW > 0 && screenH > 0) {
      val maxH = screenH * 0.35f
      val srcW = logo.width.toFloat().coerceAtLeast(1f)
      val srcH = logo.height.toFloat().coerceAtLeast(1f)
      var destH = maxH
      var destW = destH * (srcW / srcH)
      val maxW = screenW * 0.92f
      if (destW > maxW) {
        destW = maxW
        destH = destW * (srcH / srcW)
      }
      val left = (screenW - destW) * 0.5f
      val top = screenH * 0.06f
      hudIconDst.set(left, top, left + destW, top + destH)
      canvas.drawBitmap(logo, null, hudIconDst, bodyPaint)
    }
    if ((System.currentTimeMillis() / 600L) % 2L == 0L) {
      uiStringBuilder.setLength(0)
      uiStringBuilder.append("TOUCH SCREEN TO START")
      drawCenteredHud(canvas, uiStringBuilder, screenW * 0.5f, screenH * 0.52f, uiGoldPaint, uiGoldShadowPaint)
    }
    uiStringBuilder.setLength(0)
    uiStringBuilder.append("CREDIT 00")
    drawCenteredHud(canvas, uiStringBuilder, screenW * 0.5f, screenH - 88f, uiSmallPaint, uiSmallShadowPaint)
    uiStringBuilder.setLength(0)
    uiStringBuilder.append("[ AUDIO SETTINGS ]")
    val settingsY = screenH * 0.68f
    drawCenteredHud(canvas, uiStringBuilder, screenW * 0.5f, settingsY, uiTextPaint, uiShadowPaint)
    val settingsW = uiTextPaint.measureText(uiStringBuilder, 0, uiStringBuilder.length)
    val settingsX = screenW * 0.5f - settingsW * 0.5f
    openSettingsButtonRect.set(
      settingsX,
      settingsY + uiTextPaint.ascent(),
      settingsX + settingsW,
      settingsY + uiTextPaint.descent(),
    )
  }

  private fun drawSettingsOverlay(canvas: Canvas) {
    canvas.drawColor(0x88000000.toInt())
    val cx = screenW * 0.5f
    uiStringBuilder.setLength(0)
    uiStringBuilder.append("SOUND CONFIGURATION")
    drawCenteredHud(canvas, uiStringBuilder, cx, screenH * 0.16f, uiGoldPaint, uiGoldShadowPaint)

    val trackW = screenW * 0.60f
    val trackH = 28f
    val trackLeft = cx - trackW * 0.5f
    val corner = trackH * 0.45f

    val bgmScale = SoundManager.instance.getBgmVolumeScale()
    uiStringBuilder.setLength(0)
    uiStringBuilder.append("BGM ")
    uiStringBuilder.append((bgmScale * 100f).toInt())
    uiStringBuilder.append('%')
    drawCenteredHud(canvas, uiStringBuilder, cx, screenH * 0.32f, uiTextPaint, uiShadowPaint)
    val bgmTop = screenH * 0.38f - trackH * 0.5f
    bgmSliderRect.set(trackLeft, bgmTop, trackLeft + trackW, bgmTop + trackH)
    drawCabinetSlider(canvas, bgmSliderRect, bgmScale, corner)

    val sfxScale = SoundManager.instance.getSfxVolumeScale()
    uiStringBuilder.setLength(0)
    uiStringBuilder.append("SFX ")
    uiStringBuilder.append((sfxScale * 100f).toInt())
    uiStringBuilder.append('%')
    drawCenteredHud(canvas, uiStringBuilder, cx, screenH * 0.48f, uiTextPaint, uiShadowPaint)
    val sfxTop = screenH * 0.54f - trackH * 0.5f
    sfxSliderRect.set(trackLeft, sfxTop, trackLeft + trackW, sfxTop + trackH)
    drawCabinetSlider(canvas, sfxSliderRect, sfxScale, corner)

    uiStringBuilder.setLength(0)
    uiStringBuilder.append("[ RETURN TO TITLE ]")
    val backY = screenH * 0.74f
    drawCenteredHud(canvas, uiStringBuilder, cx, backY, uiGoldPaint, uiGoldShadowPaint)
    val backW = uiGoldPaint.measureText(uiStringBuilder, 0, uiStringBuilder.length)
    val backX = cx - backW * 0.5f
    backButtonRect.set(
      backX,
      backY + uiGoldPaint.ascent(),
      backX + backW,
      backY + uiGoldPaint.descent(),
    )
  }

  private fun drawCabinetSlider(canvas: Canvas, track: RectF, fill: Float, corner: Float) {
    canvas.drawRoundRect(track, corner, corner, uiTrackPaint)
    if (fill > 0f) {
      val inset = 4f
      val innerRight = track.left + (track.width() - inset * 2f) * fill + inset
      hudIconDst.set(track.left + inset, track.top + inset, innerRight, track.bottom - inset)
      canvas.drawRoundRect(hudIconDst, corner * 0.7f, corner * 0.7f, uiGoldPaint)
    }
    canvas.drawRoundRect(track, corner, corner, uiTrackStrokePaint)
  }

  private fun drawCenteredHud(
    canvas: Canvas,
    text: StringBuilder,
    centerX: Float,
    y: Float,
    fill: Paint,
    shadow: Paint,
  ) {
    val end = text.length
    val w = fill.measureText(text, 0, end)
    val x = centerX - w * 0.5f
    drawHudTextAt(canvas, text, 0, end, x, y, fill, shadow)
  }

  private fun drawHudTextAt(
    canvas: Canvas,
    text: StringBuilder,
    start: Int,
    end: Int,
    x: Float,
    y: Float,
    fill: Paint,
    shadow: Paint,
  ) {
    accentShadowPaint.typeface = arcadeTypeface ?: fill.typeface
    accentShadowPaint.textSize = fill.textSize
    if (fill === uiGoldPaint) {
      accentShadowPaint.color = 0xFFB35400.toInt()
    } else {
      accentShadowPaint.color = 0xFF3A3A3A.toInt()
    }
    canvas.drawText(text, start, end, x + 4f, y + 4f, shadow)
    canvas.drawText(text, start, end, x + 2f, y + 2f, accentShadowPaint)
    canvas.drawText(text, start, end, x, y, fill)
  }

  private fun updatePanicBomb(dt: Float) {
    if (!panicBomb.isActive) return
    panicBomb.currentFrameTime += dt
    if (panicBomb.currentFrameTime >= PanicBomb.FRAME_DURATION) {
      panicBomb.currentFrameTime = 0f
      panicBomb.currentFrameIndex++
      if (panicBomb.currentFrameIndex > 5) {
        panicBomb.isActive = false
        enemyBombDmgBank = 0f
        bossBombDmgBank = 0f
        return
      }
    }
    val bombLeft = bombDstRect.left
    val bombTop = bombDstRect.top
    val bombRight = bombDstRect.right
    val bombBottom = bombDstRect.bottom

    val shotPool = enemyShots.getBulletPool()
    val shotCount = enemyShots.getPoolSize()
    var si = 0
    while (si < shotCount) {
      val shot = shotPool[si]
      if (shot.isActive) {
        if (
          shot.x >= bombLeft && shot.x <= bombRight &&
          shot.y >= bombTop && shot.y <= bombBottom
        ) {
          shot.isActive = false
        }
      }
      si++
    }

    enemyBombDmgBank += BOMB_ENEMY_DPS * dt
    val enemyDmg = enemyBombDmgBank.toInt()
    if (enemyDmg > 0) {
      enemyBombDmgBank -= enemyDmg
      val enemyPool = enemies.getEnemyPool()
      val enemyCount = enemies.getPoolSize()
      var ei = 0
      while (ei < enemyCount) {
        val enemy = enemyPool[ei]
        if (enemy.isActive) {
          val ew = enemies.halfWOf(enemy.type)
          val eh = enemies.halfHOf(enemy.type)
          if (
            enemy.x + ew >= bombLeft && enemy.x - ew <= bombRight &&
            enemy.y + eh >= bombTop && enemy.y - eh <= bombBottom
          ) {
            enemy.health -= enemyDmg
            if (enemy.health <= 0) {
              enemy.isActive = false
              particles.triggerExplosion(enemy.x, enemy.y, true)
              dropEnemyLoot(enemy.x, enemy.y, enemy.type)
            }
          }
        }
        ei++
      }
    }

    if (boss.isActive() && !boss.isExploding()) {
      bossBombDmgBank += BOMB_BOSS_DPS * dt
      val dmg = bossBombDmgBank.toInt()
      if (dmg > 0) {
        bossBombDmgBank -= dmg
        val parts = boss.getComponents()
        val partCount = boss.getComponentCount()
        var pi = partCount - 1
        while (pi >= 0) {
          val part = parts[pi]
          if (!part.isDestroyed && part.halfW > 0f && part.halfH > 0f) {
            if (
              part.x + part.halfW >= bombLeft && part.x - part.halfW <= bombRight &&
              part.y + part.halfH >= bombTop && part.y - part.halfH <= bombBottom
            ) {
              if (part.componentType == BossController.TYPE_CORE && !boss.isCoreVulnerable()) {
                pi--
                continue
              }
              part.health -= dmg
              if (part.health <= 0) {
                part.health = 0
                part.isDestroyed = true
                particles.triggerExplosion(part.x, part.y)
              }
            }
          }
          pi--
        }
      }
    }
  }

  private fun drawPanicBomb(canvas: Canvas) {
    if (!panicBomb.isActive) return
    val frame = panicBomb.currentFrameIndex
    if (frame < 0 || frame > 5) return
    val activeBmp = bombSheets[frame] ?: return
    canvas.drawBitmap(activeBmp, srcCore, bombDstRect, bodyPaint)
  }

  private fun loadBombSheetsIfNeeded() {
    if (bombSheets[0] == null) {
      bombSheets[0] = decodeKeyed(R.drawable.player_bomb_1)
      bombSheets[1] = decodeKeyed(R.drawable.player_bomb_2)
      bombSheets[2] = decodeKeyed(R.drawable.player_bomb_3)
      bombSheets[3] = decodeKeyed(R.drawable.player_bomb_4)
      bombSheets[4] = decodeKeyed(R.drawable.player_bomb_5)
      bombSheets[5] = decodeKeyed(R.drawable.player_bomb_6)
    }
    val referenceBmp = bombSheets[0] ?: return
    srcCore.set(0, 0, referenceBmp.width, referenceBmp.height)
    if (screenW <= 0 || screenH <= 0) return
    val targetDisplayH = screenH.toFloat()
    val inverseAspect = referenceBmp.width.toFloat() / referenceBmp.height.toFloat()
    val targetDisplayW = targetDisplayH * inverseAspect
    val leftOffset = (screenW - targetDisplayW) * 0.5f
    bombDstRect.set(leftOffset, 0f, leftOffset + targetDisplayW, targetDisplayH)
  }

  private fun loadHudIconsIfNeeded() {
    if (lifeIconBmp == null) {
      lifeIconBmp = decodeKeyed(R.drawable.hud_life_icon)
    }
    if (bombIconBmp == null) {
      bombIconBmp = decodeKeyed(R.drawable.hud_bomb_icon)
    }
    if (logoBmp == null) {
      logoBmp = decodeKeyed(R.drawable.game_logo)
    }
    if (powerUpBmp == null) {
      powerUpBmp = decodeKeyed(R.drawable.item_powerup)
    }
    if (bombPickupBmp == null) {
      bombPickupBmp = decodeKeyed(R.drawable.item_bomb)
    }
    if (medalFrames[0] == null) {
      medalFrames[0] = decodeKeyed(R.drawable.item_medal_0)
      medalFrames[1] = decodeKeyed(R.drawable.item_medal_1)
      medalFrames[2] = decodeKeyed(R.drawable.item_medal_2)
      medalFrames[3] = decodeKeyed(R.drawable.item_medal_3)
      medalFrames[4] = decodeKeyed(R.drawable.item_medal_4)
      medalFrames[5] = decodeKeyed(R.drawable.item_medal_5)
      medalFrames[6] = decodeKeyed(R.drawable.item_medal_6)
      medalFrames[7] = decodeKeyed(R.drawable.item_medal_7)
    }
  }

  private fun stageGroundBitmap(): Bitmap? {
    return when (stageManager.currentStage) {
      2 -> bgStage2Bmp
      3 -> bgStage3Bmp
      else -> null
    }
  }

  private fun loadStage2Background(width: Int, height: Int) {
    if (width <= 0 || height <= 0) return
    val existing = bgStage2Bmp
    if (existing != null && !existing.isRecycled && existing.width == width && existing.height == height) {
      return
    }
    if (existing != null && !existing.isRecycled) existing.recycle()
    bgStage2Bmp = null
    bgStage2Bmp = decodeCoverScaled(resources, R.drawable.stage2_bg_layer1_ground, width, height)
  }

  private fun loadStage3Background(width: Int, height: Int) {
    if (width <= 0 || height <= 0) return
    val existing = bgStage3Bmp
    if (existing != null && !existing.isRecycled && existing.width == width && existing.height == height) {
      return
    }
    if (existing != null && !existing.isRecycled) existing.recycle()
    bgStage3Bmp = null
    bgStage3Bmp = decodeCoverScaled(resources, R.drawable.stage3_bg_layer1_ocean, width, height)
  }

  private fun decodeKeyed(drawableId: Int): Bitmap {
    val opts = BitmapFactory.Options().apply {
      inScaled = false
      inPreferredConfig = Bitmap.Config.ARGB_8888
      inMutable = true
    }
    val src = BitmapFactory.decodeResource(resources, drawableId, opts)
      ?: error("Missing drawable $drawableId")
    val bmp = if (src.isMutable) src else src.copy(Bitmap.Config.ARGB_8888, true).also { src.recycle() }
    keyGreen(bmp)
    return bmp
  }

  private fun keyGreen(bmp: Bitmap) {
    val w = bmp.width
    val h = bmp.height
    val row = IntArray(w)
    var rowY = 0
    while (rowY < h) {
      bmp.getPixels(row, 0, w, 0, rowY, w, 1)
      var i = 0
      while (i < w) {
        val c = row[i]
        val r = (c ushr 16) and 0xFF
        val g = (c ushr 8) and 0xFF
        val b = c and 0xFF
        if (g > 160 && g > r + 40 && g > b + 40) {
          row[i] = 0
        }
        i++
      }
      bmp.setPixels(row, 0, w, 0, rowY, w, 1)
      rowY++
    }
  }

  private fun resolveCollisions() {
    if (player.getHealth() <= 0) {
      if (gameState == STATE_PLAYING) enterGameOver()
      return
    }
    val bulletPool = bullets.getBulletPool()
    val bulletCount = bullets.getPoolSize()
    val enemyPool = enemies.getEnemyPool()
    val enemyCount = enemies.getPoolSize()
    val radiusSq = RADIUS_SUM_THRESHOLD * RADIUS_SUM_THRESHOLD
    var bi = 0
    while (bi < bulletCount) {
      val bullet = bulletPool[bi]
      if (bullet.isActive) {
        var ei = 0
        while (ei < enemyCount) {
          val enemy = enemyPool[ei]
          if (enemy.isActive) {
            val dx = bullet.x - enemy.x
            val dy = bullet.y - enemy.y
            val distanceSquared = (dx * dx) + (dy * dy)
            if (distanceSquared <= radiusSq) {
              bullet.isActive = false
              enemy.health -= 1
              if (enemy.health <= 0) {
                enemy.isActive = false
                particles.triggerExplosion(enemy.x, enemy.y, true)
                dropEnemyLoot(enemy.x, enemy.y, enemy.type)
              }
              break
            }
          }
          ei++
        }
      }
      bi++
    }

    val missilePool = homingMissiles.getPool()
    val missileCount = homingMissiles.getPoolSize()
    var mi = 0
    while (mi < missileCount) {
      val missile = missilePool[mi]
      if (missile.isActive) {
        var ei = 0
        while (ei < enemyCount) {
          val enemy = enemyPool[ei]
          if (enemy.isActive) {
            val dx = missile.x - enemy.x
            val dy = missile.y - enemy.y
            val distanceSquared = (dx * dx) + (dy * dy)
            if (distanceSquared <= radiusSq) {
              missile.isActive = false
              enemy.health -= 1
              if (enemy.health <= 0) {
                enemy.isActive = false
                particles.triggerExplosion(enemy.x, enemy.y, true)
                dropEnemyLoot(enemy.x, enemy.y, enemy.type)
              }
              break
            }
          }
          ei++
        }
      }
      mi++
    }

    if (boss.isActive()) {
      val parts = boss.getComponents()
      val partCount = boss.getComponentCount()
      bi = 0
      while (bi < bulletCount) {
        val bullet = bulletPool[bi]
        if (bullet.isActive) {
          var pi = partCount - 1
          while (pi >= 0) {
            val part = parts[pi]
            if (!part.isDestroyed && part.halfW > 0f && part.halfH > 0f) {
              val hw = part.halfW + BOSS_BULLET_PAD_X
              val hh = part.halfH + BOSS_BULLET_PAD_Y
              val dx = (bullet.x - part.x) / hw
              val dy = (bullet.y - part.y) / hh
              if ((dx * dx) + (dy * dy) <= 1f) {
                bullet.isActive = false
                if (part.componentType != BossController.TYPE_CORE || boss.isCoreVulnerable()) {
                  part.health -= 1
                  if (part.health <= 0) {
                    part.health = 0
                    part.isDestroyed = true
                    particles.triggerExplosion(part.x, part.y, false)
                  }
                }
                break
              }
            }
            pi--
          }
        }
        bi++
      }
      mi = 0
      while (mi < missileCount) {
        val missile = missilePool[mi]
        if (missile.isActive) {
          var pi = partCount - 1
          while (pi >= 0) {
            val part = parts[pi]
            if (!part.isDestroyed && part.halfW > 0f && part.halfH > 0f) {
              val hw = part.halfW + BOSS_BULLET_PAD_X
              val hh = part.halfH + BOSS_BULLET_PAD_Y
              val dx = (missile.x - part.x) / hw
              val dy = (missile.y - part.y) / hh
              if ((dx * dx) + (dy * dy) <= 1f) {
                missile.isActive = false
                if (part.componentType != BossController.TYPE_CORE || boss.isCoreVulnerable()) {
                  part.health -= 1
                  if (part.health <= 0) {
                    part.health = 0
                    part.isDestroyed = true
                    particles.triggerExplosion(part.x, part.y, false)
                  }
                }
                break
              }
            }
            pi--
          }
        }
        mi++
      }
    }

    val enemyBullets = enemyShots.getBulletPool()
    val playerX = player.getHitboxX()
    val playerY = player.getHitboxY()
    val playerRadius = PLAYER_HIT_RADIUS
    val sumRadius = playerRadius + EnemyWeaponSystem.HALF_BULLET_WIDTH
    val sumSq = sumRadius * sumRadius
    val enemyBulletCount = enemyShots.getPoolSize()
    var i = 0
    while (i < enemyBulletCount) {
      val b = enemyBullets[i]
      if (b.isActive) {
        val dx = b.x - playerX
        val dy = b.y - playerY
        val distSq = (dx * dx) + (dy * dy)
        if (distSq <= sumSq) {
          b.isActive = false
          if (player.takeDamage()) {
            particles.triggerExplosion(playerX, playerY)
            if (player.isGameOver() && gameState == STATE_PLAYING) enterGameOver()
          }
          break
        }
      }
      i++
    }

    if (!player.isGameOver()) {
      val ramBody = ENEMY_RAM_BODY_FRAC
      val playerRadius = PLAYER_HIT_RADIUS
      var ei = 0
      while (ei < enemyCount) {
        val enemy = enemyPool[ei]
        if (enemy.isActive) {
          val sx = playerRadius + enemies.halfWOf(enemy.type) * ramBody
          val sy = playerRadius + enemies.halfHOf(enemy.type) * ramBody
          if (sx > 0f && sy > 0f) {
            val nx = (enemy.x - playerX) / sx
            val ny = (enemy.y - playerY) / sy
            if ((nx * nx) + (ny * ny) <= 1f) {
              enemy.isActive = false
              particles.triggerExplosion(enemy.x, enemy.y)
              dropEnemyLoot(enemy.x, enemy.y, enemy.type)
              if (player.takeDamage()) {
                particles.triggerExplosion(playerX, playerY)
                if (player.isGameOver() && gameState == STATE_PLAYING) enterGameOver()
              }
              break
            }
          }
        }
        ei++
      }
    }

    if (!player.isGameOver() && boss.isActive()) {
      val parts = boss.getComponents()
      val partCount = boss.getComponentCount()
      var pi = 0
      while (pi < partCount) {
        val part = parts[pi]
        if (!part.isDestroyed && part.halfW > 0f && part.halfH > 0f) {
          val rx = part.halfW + playerRadius
          val ry = part.halfH + playerRadius
          val nx = (playerX - part.x) / rx
          val ny = (playerY - part.y) / ry
          if ((nx * nx) + (ny * ny) <= 1f) {
            if (player.takeDamage()) {
              particles.triggerExplosion(playerX, playerY)
              if (player.isGameOver() && gameState == STATE_PLAYING) enterGameOver()
            }
            break
          }
        }
        pi++
      }
    }

    val itemPool = powerUpItem.getPool()
    val itemCount = powerUpItem.getPoolSize()
    var ii = 0
    while (ii < itemCount) {
      val item = itemPool[ii]
      if (item.isActive) {
        val dx = playerX - item.x
        val dy = playerY - item.y
        val half = if (item.itemType == PowerUpItem.ITEM_TYPE_MEDAL) {
          PowerUpItem.MEDAL_HALF
        } else {
          PowerUpItem.POWERUP_HALF
        }
        val pickup = PLAYER_HIT_RADIUS + half
        if ((dx * dx) + (dy * dy) <= pickup * pickup) {
          item.isActive = false
          if (item.itemType == PowerUpItem.ITEM_TYPE_MEDAL) {
            collectMedal(item)
          } else if (item.itemType == PowerUpItem.ITEM_TYPE_BOMB) {
            collectBomb(item.x, item.y)
          } else {
            player.upgradeWeapon()
            SoundManager.instance.playSFX(SoundManager.SFX_PICKUP)
          }
        }
      }
      ii++
    }
  }

  private fun dropEnemyLoot(x: Float, y: Float, enemyType: Int) {
    powerUpItem.spawn(x, y, PowerUpItem.ITEM_TYPE_MEDAL)
    val dropChance = if (
      enemyType == ENEMY_TYPE_HEAVY ||
      enemyType == ENEMY_TYPE_INTERCEPTOR
    ) {
      0.40f
    } else {
      0.15f
    }
    if (nextLootUnit() >= dropChance) return
    val pickupType = if (nextLootUnit() < 0.2f) {
      PowerUpItem.ITEM_TYPE_BOMB
    } else {
      PowerUpItem.ITEM_TYPE_POWERUP
    }
    powerUpItem.spawn(x, y, pickupType)
  }

  private fun nextLootUnit(): Float {
    lootSeed = lootSeed * 1664525L + 1013904223L
    return ((lootSeed ushr 8) and 0xFFFFFFL).toFloat() / 16777215f
  }

  private fun collectBomb(x: Float, y: Float) {
    if (availableBombs < MAX_BOMBS) {
      availableBombs++
    } else {
      campaignScore += BOMB_FULL_SCORE
      if (campaignScore > 99_999_999) campaignScore = 99_999_999
      triggerFloatingScore(x, y, BOMB_FULL_SCORE)
    }
    SoundManager.instance.playSFX(SoundManager.SFX_PICKUP)
  }

  private fun collectMedal(item: PowerUpSlot) {
    val points = if (item.medalFrameIndex == 0) MEDAL_SCORE_FACE else MEDAL_SCORE_EDGE
    campaignScore += points
    if (campaignScore > 99_999_999) campaignScore = 99_999_999
    triggerFloatingScore(item.x, item.y, points)
    if (item.medalFrameIndex == 0) {
      particles.triggerExplosion(item.x, item.y, false)
    }
    SoundManager.instance.playSFX(SoundManager.SFX_PICKUP)
  }

  private fun triggerFloatingScore(startX: Float, startY: Float, value: Int) {
    var i = 0
    while (i < 12) {
      val p = scorePool[i]
      if (!p.isActive) {
        p.x = startX
        p.y = startY
        p.scoreValue = value
        p.age = 0f
        p.isActive = true
        return
      }
      i++
    }
  }

  private fun updateFloatingScores(dt: Float) {
    var i = 0
    while (i < 12) {
      val p = scorePool[i]
      if (p.isActive) {
        p.y -= FLOATING_SPEED * dt
        p.age += dt
        if (p.age >= FLOATING_LIFE) p.isActive = false
      }
      i++
    }
  }

  private fun deactivateScorePopups() {
    var i = 0
    while (i < 12) {
      scorePool[i].isActive = false
      i++
    }
  }

  private fun drawFloatingScores(canvas: Canvas) {
    var i = 0
    while (i < 12) {
      val p = scorePool[i]
      if (p.isActive) {
        val fade = (1f - p.age / FLOATING_LIFE).coerceIn(0f, 1f)
        val alpha = (255f * fade).toInt()
        popupPaint.alpha = alpha
        popupShadowPaint.alpha = alpha
        uiStringBuilder.setLength(0)
        uiStringBuilder.append(p.scoreValue)
        val end = uiStringBuilder.length
        val w = popupPaint.measureText(uiStringBuilder, 0, end)
        canvas.drawText(uiStringBuilder, 0, end, p.x - w * 0.5f + 2f, p.y + 2f, popupShadowPaint)
        canvas.drawText(uiStringBuilder, 0, end, p.x - w * 0.5f, p.y, popupPaint)
      }
      i++
    }
    popupPaint.alpha = 255
    popupShadowPaint.alpha = 255
  }

  private fun drawPowerUpItem(canvas: Canvas) {
    powerUpItem.draw(canvas, powerUpBmp, bombPickupBmp, medalFrames, bodyPaint)
  }

  private fun lockGameCanvas(): Canvas? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      holder.lockHardwareCanvas()
    } else {
      holder.lockCanvas()
    }
  }

  override fun onDetachedFromWindow() {
    running = false
    choreographer.removeFrameCallback(this)
    parallax.release()
    player.release()
    enemies.release()
    particles.release()
    boss.release()
    homingMissiles.release()
    var i = 0
    while (i < bombSheets.size) {
      val bmp = bombSheets[i]
      if (bmp != null && !bmp.isRecycled) bmp.recycle()
      bombSheets[i] = null
      i++
    }
    val lifeIcon = lifeIconBmp
    if (lifeIcon != null && !lifeIcon.isRecycled) lifeIcon.recycle()
    lifeIconBmp = null
    val bombIcon = bombIconBmp
    if (bombIcon != null && !bombIcon.isRecycled) bombIcon.recycle()
    bombIconBmp = null
    val logo = logoBmp
    if (logo != null && !logo.isRecycled) logo.recycle()
    logoBmp = null
    val powerUp = powerUpBmp
    if (powerUp != null && !powerUp.isRecycled) powerUp.recycle()
    powerUpBmp = null
    val bombPickup = bombPickupBmp
    if (bombPickup != null && !bombPickup.isRecycled) bombPickup.recycle()
    bombPickupBmp = null
    var medalI = 0
    while (medalI < medalFrames.size) {
      val medal = medalFrames[medalI]
      if (medal != null && !medal.isRecycled) medal.recycle()
      medalFrames[medalI] = null
      medalI++
    }
    val stage2 = bgStage2Bmp
    if (stage2 != null && !stage2.isRecycled) stage2.recycle()
    bgStage2Bmp = null
    val stage3 = bgStage3Bmp
    if (stage3 != null && !stage3.isRecycled) stage3.recycle()
    bgStage3Bmp = null
    super.onDetachedFromWindow()
  }

  private fun resetStage() {
    // FORCE-CLEAR SCORING FLAGS TO PREVENT INSTANT TICKER SKIPPING ON NEXT LEVELS
    scorecard.isActive = false
    scorecard.isCountingDone = false
    scorecard.currentDisplayLine = 0
    scorecard.elapsedTime = 0f
    scorecard.visibleLifeBonus = 0
    scorecard.visibleBombBonus = 0
    scorecard.visibleTotalScore = 0

    panicBomb.isActive = false
    bossWasExploding = false
    screenShakeTrauma = 0f
    enemyBombDmgBank = 0f
    bossBombDmgBank = 0f
    awaitingSecondTap = false
    bossFought = false
    player.resetForStage()
    powerUpItem.deactivateAll()
    deactivateScorePopups()
    bullets.deactivateAll()
    homingMissiles.deactivateAll()
    enemies.deactivateAll()
    enemyShots.deactivateAll()
    SoundManager.instance.stopAlarm()
    boss.deactivate()
    boss.bindStage(stageManager.currentStage)
    timeline.reset()
    parallax.resetScroll()
    lastBgmRes = 0
  }

  private fun beginDemo() {
    idleT = 0f
    demoT = 0f
    stageManager.resetToStart()
    if ((System.nanoTime() and 1L) != 0L) {
      stageManager.advanceToNextStage()
    }
    player.resetWeaponPower()
    player.restoreLives()
    availableBombs = 3
    resetStage()
    player.upgradeWeapon()
    player.upgradeWeapon()
    player.setAutoFire(true)
    lastBgmRes = 0
    gameState = STATE_DEMO
  }

  private fun enterGameOver() {
    gameOverT = 0f
    gameState = STATE_GAMEOVER
  }

  private fun returnToTitle() {
    player.setAutoFire(false)
    stageManager.resetToStart()
    player.resetWeaponPower()
    player.restoreLives()
    availableBombs = 3
    campaignScore = 0
    resetStage()
    idleT = 0f
    demoT = 0f
    gameOverT = 0f
    lastBgmRes = 0
    isSettingsMenuOpen = false
    gameState = STATE_TITLE
  }

  private fun exitDemo() {
    returnToTitle()
  }

  private fun demoPilot(dt: Float) {
    val px = player.getHitboxX()
    val py = player.getHitboxY()
    val w = screenW.toFloat()
    val h = screenH.toFloat()
    var huntX = w * 0.5f + kotlin.math.sin(demoT * 1.15f) * (w * 0.16f)
    val huntY = h * 0.78f
    var bestY = -1f
    val enemyPool = enemies.getEnemyPool()
    val enemyCount = enemies.getPoolSize()
    var ei = 0
    while (ei < enemyCount) {
      val e = enemyPool[ei]
      if (e.isActive && e.y > 48f && e.y < py - 56f) {
        if (e.y > bestY) {
          bestY = e.y
          huntX = e.x
        }
      }
      ei++
    }
    if (boss.isActive()) {
      val parts = boss.getComponents()
      val partCount = boss.getComponentCount()
      var pi = 0
      while (pi < partCount) {
        val part = parts[pi]
        if (!part.isDestroyed && part.halfW > 0f && part.y < py - 40f) {
          if (part.y > bestY) {
            bestY = part.y
            huntX = part.x
          }
        }
        pi++
      }
    }
    val shots = enemyShots.getBulletPool()
    val shotCount = enemyShots.getPoolSize()
    val dodgeMargin = 78f
    var si = 0
    while (si < shotCount) {
      val b = shots[si]
      if (b.isActive && b.vy > 0f && b.y < py && b.y > py - 240f) {
        val dx = b.x - px
        if (dx * dx < dodgeMargin * dodgeMargin) {
          huntX += if (b.x >= px) -120f else 120f
          break
        }
      }
      si++
    }
    val minX = w * 0.12f
    val maxX = w * 0.88f
    if (huntX < minX) huntX = minX
    if (huntX > maxX) huntX = maxX
    player.steerToward(huntX, huntY, dt)
  }

  override fun onTouchEvent(event: MotionEvent): Boolean {
    val down = event.actionMasked == MotionEvent.ACTION_DOWN ||
      event.actionMasked == MotionEvent.ACTION_POINTER_DOWN
    when (gameState) {
      STATE_TITLE -> {
        if (down || event.actionMasked == MotionEvent.ACTION_MOVE) {
          idleT = 0f
          val x = event.x
          val y = event.y
          if (isSettingsMenuOpen) {
            val pad = 40f
            if (
              x >= bgmSliderRect.left && x <= bgmSliderRect.right &&
              y >= bgmSliderRect.top - pad && y <= bgmSliderRect.bottom + pad
            ) {
              val trackW = bgmSliderRect.width().coerceAtLeast(1f)
              val pct = ((x - bgmSliderRect.left) / trackW).coerceIn(0f, 1f)
              SoundManager.instance.setBgmVolumeScale(pct)
            } else if (
              x >= sfxSliderRect.left && x <= sfxSliderRect.right &&
              y >= sfxSliderRect.top - pad && y <= sfxSliderRect.bottom + pad
            ) {
              val trackW = sfxSliderRect.width().coerceAtLeast(1f)
              val pct = ((x - sfxSliderRect.left) / trackW).coerceIn(0f, 1f)
              SoundManager.instance.setSfxVolumeScale(pct)
              if (down || ((event.eventTime.toInt() * 1103515245 + 12345) ushr 16 and 7) == 0) {
                SoundManager.instance.playSFX(SoundManager.SFX_VULCAN)
              }
            } else if (down && backButtonRect.contains(x, y)) {
              isSettingsMenuOpen = false
              SoundManager.instance.playSFX(SoundManager.SFX_PICKUP)
            }
          } else if (down && openSettingsButtonRect.contains(x, y)) {
            isSettingsMenuOpen = true
            SoundManager.instance.playSFX(SoundManager.SFX_PICKUP)
          } else if (down) {
            stageManager.resetToStart()
            player.resetWeaponPower()
            player.restoreLives()
            availableBombs = 3
            campaignScore = 0
            resetStage()
            gameState = STATE_PLAYING
          }
        }
        return true
      }
      STATE_DEMO -> {
        if (down) exitDemo()
        return true
      }
      STATE_CLEAR -> {
        if (scorecard.isCountingDone && down) {
          stageManager.advanceToNextStage()
          resetStage()
          gameState = STATE_PLAYING
        }
        return true
      }
      STATE_GAMEOVER -> {
        return true
      }
    }
    if (player.getHealth() <= 0) return true
    when (event.actionMasked) {
      MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
        val now = event.eventTime
        if (
          awaitingSecondTap &&
          now - lastTapUpMs <= DOUBLE_TAP_MS &&
          availableBombs > 0 &&
          !player.isGameOver()
        ) {
          availableBombs--
          panicBomb.activate(player.getHitboxX(), player.getHitboxY())
          addScreenShake(0.8f)
          SoundManager.instance.playSFX(SoundManager.SFX_BOMB)
          awaitingSecondTap = false
        }
        touchDownMs = now
        touchDownX = event.x
        touchDownY = event.y
      }
      MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
        val dx = event.x - touchDownX
        val dy = event.y - touchDownY
        val dur = event.eventTime - touchDownMs
        awaitingSecondTap = (dx * dx + dy * dy) <= TAP_SLOP_SQ && dur <= TAP_MAX_MS
        lastTapUpMs = event.eventTime
      }
    }
    return player.onTouch(event)
  }

  private fun syncBgm() {
    val want = when (gameState) {
      STATE_TITLE -> R.raw.bgm_title
      STATE_CLEAR -> R.raw.bgm_victory
      STATE_PLAYING, STATE_DEMO -> {
        if (boss.isActive()) {
          R.raw.bgm_boss
        } else if (stageManager.currentStage >= 2) {
          R.raw.bgm_stage2
        } else {
          R.raw.bgm_stage1
        }
      }
      else -> 0
    }
    if (want != 0 && want != lastBgmRes) {
      if (SoundManager.instance.switchBGM(want)) {
        lastBgmRes = want
      }
    }
  }

  private fun blitHudIcon(canvas: Canvas, bmp: Bitmap) {
    hudIconDst.offset(HUD_SHADOW_PX, HUD_SHADOW_PX)
    canvas.drawBitmap(bmp, null, hudIconDst, hudShadowPaint)
    hudIconDst.offset(-HUD_SHADOW_PX, -HUD_SHADOW_PX)
    var oy = -HUD_OUTLINE_PX
    while (oy <= HUD_OUTLINE_PX) {
      var ox = -HUD_OUTLINE_PX
      while (ox <= HUD_OUTLINE_PX) {
        if (ox != 0f || oy != 0f) {
          hudIconDst.offset(ox, oy)
          canvas.drawBitmap(bmp, null, hudIconDst, hudOutlinePaint)
          hudIconDst.offset(-ox, -oy)
        }
        ox += HUD_OUTLINE_PX
      }
      oy += HUD_OUTLINE_PX
    }
    canvas.drawBitmap(bmp, null, hudIconDst, bodyPaint)
  }

  private companion object {
    const val STATE_TITLE = 0
    const val STATE_PLAYING = 1
    const val STATE_CLEAR = 2
    const val STATE_GAMEOVER = 3
    const val STATE_DEMO = 4
    const val IDLE_SECS = 10f
    const val DEMO_SECS = 30f
    const val GAMEOVER_SECS = 5f
    const val TITLE_SCROLL_PX = 50f
    const val MAX_FRAME_NS = 50_000_000L
    const val RADIUS_SUM_THRESHOLD = 28f
    const val PLAYER_HIT_RADIUS = 12f
    const val BOSS_BULLET_PAD_X = 6f
    const val BOSS_BULLET_PAD_Y = 16f
    const val ENEMY_RAM_BODY_FRAC = 0.45f
    const val MEDAL_SCORE_FACE = 2000
    const val MEDAL_SCORE_EDGE = 200
    const val FLOATING_SPEED = 90f
    const val FLOATING_LIFE = 0.75f
    const val BOMB_ENEMY_DPS = 250f
    const val BOMB_BOSS_DPS = 400f
    const val ENEMY_TYPE_INTERCEPTOR = 2
    const val ENEMY_TYPE_HEAVY = 3
    const val MAX_BOMBS = 3
    const val BOMB_FULL_SCORE = 5000
    const val DOUBLE_TAP_MS = 280L
    const val TAP_MAX_MS = 220L
    const val TAP_SLOP_SQ = 48f * 48f
    const val HUD_SHADOW_PX = 2f
    const val HUD_OUTLINE_PX = 3f
  }
}

private class FloatingScore {
  var x = 0f
  var y = 0f
  var scoreValue = 0
  var age = 0f
  var isActive = false
}
