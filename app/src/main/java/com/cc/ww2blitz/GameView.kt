package com.cc.ww2blitz

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.PorterDuffXfermode
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

  private val parallax = ParallaxBackground()
  private val player = PlayerShip(resources)
  private val bullets = BulletManager()
  private val homingMissiles = HomingMissileManager(resources)
  private val enemies = EnemyPoolManager(resources)
  private val enemyShots = EnemyWeaponSystem()
  private val timeline = SpawnTimeline()
  private val particles = ParticleManager(resources)
  private val boss = BossController(resources)
  private val uiController = UIController()
  private var campaignScore: Int
    get() = ScoreManager.instance.getScore()
    set(value) { ScoreManager.instance.setScore(value) }
  private val panicBomb = PanicBomb()
  private val powerUpItem = PowerUpManager.instance.items
  private val scorePool = Array(SCORE_POPUP_SLOTS) { FloatingScore() }
  private val stageManager = StageData()
  private val srcCore = Rect()
  private val bombDstRect = RectF()
  private val hudIconDst = RectF()
  private val titleDstRect = RectF()
  private val powerUpDst = RectF()
  private val bgmSliderRect = RectF()
  private val sfxSliderRect = RectF()
  private val bgmVolumeDownRect = RectF()
  private val bgmVolumeUpRect = RectF()
  private val sfxVolumeDownRect = RectF()
  private val sfxVolumeUpRect = RectF()
  private val backButtonRect = RectF()
  private val openSettingsButtonRect = RectF()
  private val openDifficultyButtonRect = RectF()
  private val openFighterSelectButtonRect = RectF()
  private val openContinueButtonRect = RectF()
  private val continueButtons = Array(3) { RectF() }
  private val continueBackButtonRect = RectF()
  private val difficultyButtons = Array(7) { RectF() }
  private val diffBackButtonRect = RectF()
  private val shipLeftSelectRect = RectF()
  private val shipRightSelectRect = RectF()
  private val fighterReturnToTitleRect = RectF()
  private var selectedFighterIndex = 0
  private val difficultyTiers = arrayOf(
    StageData.Difficulty.MONKEY,
    StageData.Difficulty.EASY,
    StageData.Difficulty.NORMAL,
    StageData.Difficulty.HARD,
    StageData.Difficulty.VERY_HARD,
    StageData.Difficulty.EXPERT,
    StageData.Difficulty.HARDCORE,
  )
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
  private var hitPipFullBmp: Bitmap? = null
  private var hitPipMidBmp: Bitmap? = null
  private var hitPipWarnBmp: Bitmap? = null
  private var hitPipEmptyBmp: Bitmap? = null
  private var availableBombs = START_BOMBS
  private var gameState = STATE_TITLE
  private var isSettingsMenuOpen = false
  private var logoBmp: Bitmap? = null
  private var titleBackdropBmp: Bitmap? = null
  var onFirstFramePosted: (() -> Unit)? = null
  private var firstPresentSent = false
  private var selectPreviewP38: Bitmap? = null
  private var selectPreviewHellcat: Bitmap? = null
  private var powerUpBmp: Bitmap? = null
  private var bombPickupBmp: Bitmap? = null
  private val medalFrames = arrayOfNulls<Bitmap>(PowerUpItem.MEDAL_FRAME_COUNT)
  private val theater = StageTheater()
  private var interstitialTimer = 0f
  private var lastTapUpMs = 0L
  private var touchDownMs = 0L
  private var touchDownX = 0f
  private var touchDownY = 0f
  private var steerFingerX = 0f
  private var steerFingerY = 0f
  private var grabOffsetX = 0f
  private var grabOffsetY = 0f
  private var isDraggingShip = false
  private var dragPointerId = -1
  private var awaitingSecondTap = false
  private var enemyBombDmgBank = 0f
  private var bossBombDmgBank = 0f
  private var bombCoreWasOpen = false
  private var bossFought = false
  private var lastBgmRes = 0
  private var attractCycleState = ATTRACT_TITLE
  private var attractCycleTimer = 0f
  private var demoT = 0f
  private var lastDemoStage = 0
  private var campaignCompleteT = 0f
  private var maxStageCleared = 0
  private var gameOverT = 0f
  private var continuePromptT = 0f
  private var continuesRemaining = 0
  private var continuedThisCredit = false
  private var registrationActiveCharIndex = 0
  private var registrationCurrentCharValue = 'A'
  private var registrationTextFlashTimer = 0f
  private val pendingInitials = CharArray(3) { 'A' }
  private val registrationSetRect = RectF()
  private val registrationLeftWing = RectF()
  private val registrationRightWing = RectF()
  private val uiRegRedPaint = Paint().apply {
    color = 0xFFE53935.toInt()
    typeface = Typeface.DEFAULT_BOLD
    textSize = 42f
    isAntiAlias = true
  }
  private val uiNeonStrokePaint = Paint().apply {
    color = 0xFF00E5FF.toInt()
    style = Paint.Style.STROKE
    strokeWidth = 4f
    isAntiAlias = true
  }
  private val uiSelectWellPaint = Paint().apply {
    color = 0x99000000.toInt()
    style = Paint.Style.FILL
    isAntiAlias = false
  }
  private val uiSelectIdleStrokePaint = Paint().apply {
    color = 0xFF6E6E6E.toInt()
    style = Paint.Style.STROKE
    strokeWidth = 5f
    strokeJoin = Paint.Join.MITER
    strokeMiter = 8f
    isAntiAlias = false
  }
  private val uiSelectIdleInnerPaint = Paint().apply {
    color = 0xFF3A3A3A.toInt()
    style = Paint.Style.STROKE
    strokeWidth = 2f
    strokeJoin = Paint.Join.MITER
    strokeMiter = 8f
    isAntiAlias = false
  }
  private val uiSelectFocusStrokePaint = Paint().apply {
    color = 0xFFFFD54A.toInt()
    style = Paint.Style.STROKE
    strokeWidth = 6f
    strokeJoin = Paint.Join.MITER
    strokeMiter = 8f
    isAntiAlias = false
  }
  private val uiSelectFocusInnerPaint = Paint().apply {
    color = 0xFF00E5FF.toInt()
    style = Paint.Style.STROKE
    strokeWidth = 2f
    strokeJoin = Paint.Join.MITER
    strokeMiter = 8f
    isAntiAlias = false
  }
  private val uiSelectCornerPaint = Paint().apply {
    color = 0xFFFFD54A.toInt()
    style = Paint.Style.STROKE
    strokeWidth = 8f
    strokeCap = Paint.Cap.SQUARE
    isAntiAlias = false
  }
  private val choreographer = Choreographer.getInstance()
  private var running = false
  private var lastNanos = 0L
  private var shakeDuration = 0f
  private var shakeIntensity = 0f
  private var flashDuration = 0f
  private var flashPeak = 0.25f
  private var flashWhiteDecay = false
  private var shakeSeed = 14352451L
  private val flashPaint = Paint().apply {
    color = 0x66FFFFFF.toInt()
    style = Paint.Style.FILL
    isAntiAlias = false
    xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)
  }
  private val flashRect = RectF()
  private var lootSeed = 2463534242L
  private var bossWasExploding = false
  private var screenW = 0
  private var screenH = 0
  private var hudScale = 1f
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
  private val appVersionName: String
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

  init {
    HighScoreManager.loadHighScores(context)
    stageManager.initPersistentSettings(context)
    player.applyFighterConfiguration(stageManager.getSavedFighterIndex())
    selectedFighterIndex = player.chosenFighterIndex
    attractCycleState = ATTRACT_TITLE
    attractCycleTimer = 0f
    gameState = STATE_TITLE
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
    uiRegRedPaint.typeface = face
    uiSmallPaint.typeface = face
    uiSmallShadowPaint.typeface = face
    accentShadowPaint.typeface = face
    popupPaint.typeface = face
    popupShadowPaint.typeface = face
    uiController.bindTypeface(face)
    var ver = "1.0.5"
    try {
      val name = context.packageManager.getPackageInfo(context.packageName, 0).versionName
      if (name != null && name.isNotEmpty()) ver = name
    } catch (_: Exception) {
    }
    appVersionName = ver
    titleBackdropBmp = decodeOpaque(R.drawable.title_screen_backdrop)
  }

  override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
    super.onSizeChanged(w, h, oldw, oldh)
    applyViewportSize(w, h)
  }

  override fun surfaceCreated(holder: SurfaceHolder) {
    HighScoreManager.loadHighScores(context)
    running = true
    lastNanos = 0L
    choreographer.postFrameCallback(this)
  }

  override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
    applyViewportSize(width, height)
  }

  private fun applyViewportSize(width: Int, height: Int) {
    if (width <= 0 || height <= 0) return
    val firstViewport = screenW <= 0 || screenH <= 0
    val sizeChanged = width != screenW || height != screenH
    parallax.onSizeChanged(width, height)
    player.onSizeChanged(width, height)
    enemies.onSizeChanged(width, height)
    enemyShots.onSizeChanged(width, height)
    homingMissiles.onSizeChanged(width, height)
    particles.onSizeChanged(width, height)
    boss.onSizeChanged(width, height)
    uiController.onSizeChanged(width, height)
    hudScale = width / HUD_DESIGN_WIDTH
    applyHudTypeSizes()
    screenW = width
    screenH = height
    if (!sizeChanged) return
    uiController.loadInterstitials(width, height)
    loadBombSheetsIfNeeded()
    loadHudIconsIfNeeded()
    if (firstViewport) bootLaunchStageIfNeeded()
    reloadStageBackgrounds(width, height, restartPlayback = false)
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
    when (gameState) {
      STATE_TITLE -> {
        if (isSettingsMenuOpen) {
          registrationTextFlashTimer += dt
        } else {
          attractCycleTimer += dt
          when (attractCycleState) {
            ATTRACT_TITLE -> {
              if (attractCycleTimer >= ATTRACT_TITLE_SECS && screenW > 0 && screenH > 0) {
                attractCycleTimer = 0f
                attractCycleState = ATTRACT_CPU_DEMO
                beginDemo()
              }
            }
            ATTRACT_HIGH_SCORE -> {
              if (attractCycleTimer >= ATTRACT_HIGH_SCORE_SECS) {
                attractCycleTimer = 0f
                attractCycleState = ATTRACT_TITLE
              }
            }
          }
        }
      }
      STATE_CLEAR -> {
        tickParallax(0f, dt)
        ScoreManager.instance.updateRecap(dt)
        applyPendingExtends()
        updateFloatingScores(dt)
        particles.update(dt)
      }
      STATE_GAMEOVER -> {
        tickParallax(0f, dt)
        particles.update(dt)
        gameOverT += dt
        if (gameOverT >= GAMEOVER_SECS) {
          routeAfterGameOver()
        }
      }
      STATE_CONTINUE -> {
        tickParallax(0f, dt)
        particles.update(dt)
        continuePromptT += dt
        if (continuePromptT >= CONTINUE_SECS) {
          enterGameOver()
        }
      }
      STATE_REGISTRATION -> {
        tickParallax(TITLE_SCROLL_PX, dt)
        registrationTextFlashTimer += dt
      }
      STATE_CAMPAIGN_COMPLETE -> {
        tickParallax(0f, dt)
        particles.update(dt)
        updateFloatingScores(dt)
        campaignCompleteT += dt
      }
      STATE_INTERSTITIAL -> {
        interstitialTimer -= dt
        if (interstitialTimer <= 0f) {
          interstitialTimer = 0f
          gameState = STATE_PLAYING
        }
      }
      STATE_DIFFICULTY_SELECT, STATE_CHARACTER_SELECT, STATE_CONTINUE_SELECT -> {
      }
      else -> {
        if (boss.locksWorldScroll()) {
          stageManager.scrollSpeedY = 0f
          tickParallax(0f, dt)
        } else {
          tickParallax(stageManager.scrollSpeedY, dt)
        }
        if (gameState == STATE_DEMO) {
          demoPilot(dt)
        } else if (gameState == STATE_PLAYING && dt > 0.0001f) {
          applyShipTether(dt)
        }
        player.update(dt)
        if (gameState == STATE_PLAYING && dt > 0.0001f && player.isOnField()) {
          stageManager.tickCombatRank(dt, player.getWeaponPower() >= 3)
        }
        if (gameState == STATE_PLAYING && player.consumeRespawnPowerDrop()) {
          spawnRespawnPowerUp()
        }
        bullets.update(dt, player, screenW, homingMissiles)
        homingMissiles.update(dt, enemies.getEnemyPool(), enemies.getPoolSize(), boss)
        powerUpItem.update(
          dt,
          screenW,
          screenH,
          player.getHitboxX(),
          player.getHitboxY(),
          player.isOnField(),
        )
        updateFloatingScores(dt)
        timeline.update(
          dt,
          enemies,
          screenW,
          screenH,
          boss,
          stageManager.targetBossTimelineSeconds,
          true,
          player.getWeaponPower(),
          stageManager,
        )
        maybeSwapStage6Floor()
        enemies.update(dt, player.getHitboxX(), player.getHitboxY(), enemyShots)
        boss.update(
          dt,
          player.getHitboxX(),
          player.getHitboxY(),
          enemyShots,
          player.getWeaponPower(),
          availableBombs,
          timeline,
        )
        maybeSwapStage6Floor()
        if (boss.isActive() || boss.isExploding()) {
          bossFought = true
        }
        enemyShots.update(dt)
        updatePanicBomb(dt)
        particles.update(dt)
        resolveCollisions()
        if (gameState == STATE_PLAYING) {
          applyPendingExtends()
        }
        boss.refreshPhaseFlags()
        val pulse = boss.consumeVisualFlags()
        if ((pulse and BossController.FX_PHASE) != 0) {
          triggerScreenShake(0.18f, 10f)
          triggerScreenFlash(0.08f)
        }
        if ((pulse and BossController.FX_DEATH) != 0) {
          triggerScreenShake(0.42f, 22f)
          triggerScreenFlash(0.14f)
        }
        if ((pulse and BossController.FX_VICTORY_CASCADE) != 0) {
          triggerScreenShake(0.1f, 8.0f)
        }
        if ((pulse and BossController.FX_VICTORY_SHATTER) != 0) {
          triggerWhiteFlash(0.25f)
        }
        bossWasExploding = boss.isExploding()
        if (
          gameState == STATE_PLAYING &&
          bossFought &&
          !boss.isActive() &&
          !boss.isExploding()
        ) {
          ScoreManager.instance.beginRecap(player.getHealth(), availableBombs)
          if (stageManager.missionNumber > maxStageCleared) {
            maxStageCleared = stageManager.missionNumber
          }
          sweepPlayfieldForClear()
          gameState = STATE_CLEAR
        }
        if (gameState == STATE_DEMO) {
          demoT += dt
          attractCycleTimer += dt
          if (attractCycleTimer >= ATTRACT_DEMO_SECS) {
            finishDemoToHighScore()
          }
        }
      }
    }
    syncBgm()
    val canvas = lockGameCanvas()
    if (canvas != null) {
      try {
        val shaking = shakeDuration > 0f
        if (shaking) {
          val mag = shakeIntensity
          val dx = (nextShakeUnit() * 2f - 1f) * mag
          val dy = (nextShakeUnit() * 2f - 1f) * mag
          canvas.save()
          canvas.translate(dx, dy)
        }
        if (
          gameState == STATE_TITLE ||
          gameState == STATE_DIFFICULTY_SELECT ||
          gameState == STATE_CHARACTER_SELECT ||
          gameState == STATE_CONTINUE_SELECT
        ) {
          val bmp = titleBackdropBmp
          if (bmp != null && !bmp.isRecycled && bmp.width > 0 && bmp.height > 0) {
            val scaleX = screenW.toFloat() / bmp.width.toFloat()
            val scaleY = screenH.toFloat() / bmp.height.toFloat()
            val fillScale = scaleX.coerceAtLeast(scaleY)
            val finalDrawW = bmp.width.toFloat() * fillScale
            val finalDrawH = bmp.height.toFloat() * fillScale
            val leftOffset = (screenW.toFloat() - finalDrawW) * 0.5f
            val topOffset = (screenH.toFloat() - finalDrawH) * 0.5f
            titleDstRect.set(leftOffset, topOffset, leftOffset + finalDrawW, topOffset + finalDrawH)
            canvas.drawBitmap(bmp, null, titleDstRect, bodyPaint)
          } else {
            canvas.drawColor(Color.BLACK)
          }
        } else if (gameState == STATE_INTERSTITIAL) {
          uiController.drawStageInterstitial(
            canvas,
            screenW.toFloat(),
            screenH.toFloat(),
            interstitialTimer,
            stageManager.currentStage,
            theater.briefing,
          )
        } else if (stageManager.isFacilityTheater && gameState != STATE_REGISTRATION) {
          if (
            gameState == STATE_CLEAR ||
            gameState == STATE_CAMPAIGN_COMPLETE
          ) {
            parallax.drawStage7(canvas, theater.floor, theater.canopy)
          } else {
            parallax.drawStage7Floor(canvas, theater.floor)
          }
        } else {
          parallax.draw(
            canvas,
            when {
              gameState == STATE_REGISTRATION -> null
              gameState == STATE_CAMPAIGN_COMPLETE -> theater.activeFloor ?: theater.floor
              else -> stageGroundBitmap()
            },
            gameState == STATE_REGISTRATION ||
              (gameState != STATE_CAMPAIGN_COMPLETE && stageManager.hasOverlayClouds),
          )
        }
        if (
          gameState != STATE_TITLE &&
          gameState != STATE_DIFFICULTY_SELECT &&
          gameState != STATE_CHARACTER_SELECT &&
          gameState != STATE_CONTINUE_SELECT &&
          gameState != STATE_CLEAR &&
          gameState != STATE_REGISTRATION &&
          gameState != STATE_CAMPAIGN_COMPLETE &&
          gameState != STATE_INTERSTITIAL
        ) {
          enemies.draw(canvas)
          boss.draw(canvas)
          enemyShots.draw(canvas)
          if (stageManager.isFacilityTheater) {
            parallax.drawStage7Canopy(canvas, theater.canopy)
          } else if (
            stageManager.isAscentTheater &&
            timeline.elapsedSeconds() >= stageManager.def.canopyAt
          ) {
            val canopyAsset = theater.canopy
            if (canopyAsset != null && !canopyAsset.isRecycled) {
              parallax.drawStage8Canopy(canvas, canopyAsset)
            }
          }
          homingMissiles.draw(canvas)
          bullets.draw(canvas)
          player.draw(canvas)
          drawPowerUpItem(canvas)
          particles.draw(canvas)
          drawPanicBomb(canvas)
        }
        if (shaking) {
          canvas.restore()
          shakeDuration -= dt
          if (shakeDuration < 0f) shakeDuration = 0f
        }
        if (flashDuration > 0f) {
          flashRect.set(0f, 0f, screenW.toFloat(), screenH.toFloat())
          if (flashWhiteDecay) {
            var a = (flashDuration / flashPeak * 255f).toInt()
            if (a < 0) a = 0
            if (a > 255) a = 255
            flashPaint.color = Color.WHITE
            flashPaint.alpha = a
          } else {
            flashPaint.color = 0x66FFFFFF.toInt()
          }
          canvas.drawRect(flashRect, flashPaint)
          flashDuration -= dt
          if (flashDuration < 0f) {
            flashDuration = 0f
            flashWhiteDecay = false
          }
        }
        if (gameState == STATE_CLEAR) {
          canvas.drawColor(0x66000000.toInt())
        }
        drawArcadeUI(canvas)
      } finally {
        holder.unlockCanvasAndPost(canvas)
        if (!firstPresentSent && screenW > 0) {
          firstPresentSent = true
          val done = onFirstFramePosted
          onFirstFramePosted = null
          if (done != null) {
            post(done)
          }
        }
      }
    }
    choreographer.postFrameCallback(this)
  }

  fun triggerScreenShake(duration: Float, intensity: Float) {
    val dur = duration
    val mag = intensity
    shakeDuration = if (dur < 0f) 0f else dur
    shakeIntensity = if (mag < 0f) 0f else mag
  }

  fun triggerScreenFlash(duration: Float) {
    val dur = duration
    flashDuration = if (dur < 0f) 0f else dur
    flashWhiteDecay = false
  }

  fun triggerWhiteFlash(duration: Float) {
    val dur = if (duration < 0f) 0f else duration
    flashDuration = dur
    flashPeak = if (dur <= 0f) 0.25f else dur
    flashWhiteDecay = true
  }

  fun addScreenShake(intensity: Float) {
    val mag = intensity
    triggerScreenShake(0.28f, 12f + mag * 22f)
  }

  private fun nextShakeUnit(): Float {
    shakeSeed = shakeSeed * 1664525L + 1013904223L
    return ((shakeSeed ushr 8) and 0xFFFFFFL).toFloat() / 16777215f
  }

  /**
   * GPU-backed canvas on API 26+ ([SurfaceHolder.lockHardwareCanvas]).
   * The platform method landed in Oreo; pre-O devices fall back to software
   * [SurfaceHolder.lockCanvas].
   */
  private fun drawArcadeUI(canvas: Canvas) {
    if (gameState == STATE_INTERSTITIAL) {
      return
    }
    if (gameState == STATE_DIFFICULTY_SELECT) {
      drawDifficultySelectScreen(canvas)
      return
    }
    if (gameState == STATE_CONTINUE_SELECT) {
      drawContinueSelectScreen(canvas)
      return
    }
    if (gameState == STATE_CHARACTER_SELECT) {
      drawCharacterSelectScreen(canvas)
      return
    }
    if (gameState == STATE_TITLE) {
      if (attractCycleState == ATTRACT_HIGH_SCORE) {
        drawHighScoreScreen(canvas)
      } else {
        drawTitleScreen(canvas)
      }
      return
    }
    if (gameState == STATE_REGISTRATION) {
      drawRegistrationScreen(canvas)
      return
    }
    if (gameState == STATE_CAMPAIGN_COMPLETE) {
      canvas.drawColor(0x66000000.toInt())
      uiController.drawCampaignCompleteCredits(canvas, screenW, screenH, campaignCompleteT)
      if (campaignCompleteT >= 22.0f) {
        if ((campaignCompleteT * 3f).toInt() % 2 == 0) {
          uiStringBuilder.setLength(0)
          uiStringBuilder.append("TOUCH SCREEN TO REGISTER SCORE")
          val originalSize = uiGoldPaint.textSize
          uiGoldPaint.textSize = originalSize * 0.65f
          uiGoldShadowPaint.textSize = originalSize * 0.65f
          uiController.drawCenteredHud(
            canvas,
            uiStringBuilder,
            screenW * 0.5f,
            screenH * 0.85f,
            uiGoldPaint,
            uiGoldShadowPaint,
          )
          uiGoldPaint.textSize = originalSize
          uiGoldShadowPaint.textSize = originalSize
        }
      }
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
      if (currentLives > 0) {
        val marginLeft = 45f
        hudIconDst.set(marginLeft, lifeStartY, marginLeft + lifeSize, lifeStartY + lifeSize)
        blitHudIcon(canvas, lifeBmp)
        uiStringBuilder.setLength(0)
        uiStringBuilder.append('x')
        uiStringBuilder.append(currentLives)
        val countX = marginLeft + lifeSize + 10f
        val countY = lifeStartY + lifeSize * 0.70f
        drawHudTextAt(
          canvas,
          uiStringBuilder,
          0,
          uiStringBuilder.length,
          countX,
          countY,
          uiTextPaint,
          uiShadowPaint,
        )
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
      val pipW = 70f
      val pipH = 32f
      val pipGap = 8f
      val rowW = maxHits * pipW + (maxHits - 1) * pipGap
      val pipLeft = (screenW - rowW) * 0.5f
      val pipTop = lifeStartY + (lifeSize - pipH) * 0.5f
      val filled = if (currentHits >= 3) {
        hitPipFullBmp
      } else if (currentHits == 2) {
        hitPipMidBmp
      } else {
        hitPipWarnBmp
      }
      val empty = hitPipEmptyBmp
      val warnVisible = currentHits > 1 || ((System.currentTimeMillis() / 180L) and 1L) == 0L
      var seg = 0
      while (seg < maxHits) {
        val sx = pipLeft + seg * (pipW + pipGap)
        hudIconDst.set(sx, pipTop, sx + pipW, pipTop + pipH)
        val lit = seg < currentHits && warnVisible
        val pip = if (lit) filled else empty
        if (pip != null) {
          blitHudIcon(canvas, pip)
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
    var score = campaignScore
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
    }
    if (gameState == STATE_CONTINUE) {
      if ((continuePromptT * 2f).toInt() % 2 == 0) {
        uiStringBuilder.setLength(0)
        uiStringBuilder.append("CONTINUE?")
        drawCenteredHud(
          canvas,
          uiStringBuilder,
          screenW * 0.5f,
          screenH * 0.40f,
          uiGoldPaint,
          uiGoldShadowPaint,
        )
      }
      val left = (CONTINUE_SECS - continuePromptT).toInt()
      uiStringBuilder.setLength(0)
      if (left < 1) {
        uiStringBuilder.append('0')
      } else {
        uiStringBuilder.append(left)
      }
      drawCenteredHud(
        canvas,
        uiStringBuilder,
        screenW * 0.5f,
        screenH * 0.50f,
        uiGoldPaint,
        uiGoldShadowPaint,
      )
      uiStringBuilder.setLength(0)
      uiStringBuilder.append("CREDIT ")
      uiStringBuilder.append(continuesRemaining)
      drawCenteredHud(
        canvas,
        uiStringBuilder,
        screenW * 0.5f,
        screenH * 0.58f,
        uiGoldPaint,
        uiGoldShadowPaint,
      )
    }
    if (gameState != STATE_CLEAR) return
    uiController.drawStageClear(canvas, screenW, screenH, ScoreManager.instance, stageManager.missionNumber)
  }

  private fun drawTitleScreen(canvas: Canvas) {
    if (isSettingsMenuOpen) {
      drawSettingsOverlay(canvas)
      return
    }
    val logo = logoBmp
    if (logo != null && screenW > 0 && screenH > 0) {
      val maxH = screenH * 0.35f * 0.67f * 1.20f
      val srcW = logo.width.toFloat().coerceAtLeast(1f)
      val srcH = logo.height.toFloat().coerceAtLeast(1f)
      var destH = maxH
      var destW = destH * (srcW / srcH)
      val maxW = screenW * 0.92f * 0.67f * 1.20f
      if (destW > maxW) {
        destW = maxW
        destH = destW * (srcH / srcW)
      }
      val left = (screenW - destW) * 0.5f
      val top = screenH * 0.06f
      hudIconDst.set(left, top, left + destW, top + destH)
      canvas.drawBitmap(logo, null, hudIconDst, bodyPaint)
    }
    val cx = screenW * 0.5f
    val versionY = screenH - hudPx(32f)
    val creditY = versionY - hudPx(28f)
    val menuBottomAnchorY = creditY - hudPx(130f)
    val menuBlockStride = hudPx(148f)
    val tagSubPadding = hudPx(56f)
    val fighterMenuY = menuBottomAnchorY
    val fighterTagY = fighterMenuY + tagSubPadding
    val continueMenuY = fighterMenuY - menuBlockStride
    val continueTagY = continueMenuY + tagSubPadding
    val difficultyMenuY = continueMenuY - menuBlockStride
    val difficultyTagY = difficultyMenuY + tagSubPadding
    val audioMenuY = difficultyMenuY - menuBlockStride
    val startPrompterY = audioMenuY - hudPx(160f)
    if ((System.currentTimeMillis() / 600L) % 2L == 0L) {
      uiStringBuilder.setLength(0)
      uiStringBuilder.append("1P START")
      drawCenteredHud(canvas, uiStringBuilder, cx, startPrompterY, uiGoldPaint, uiGoldShadowPaint)
    }
    uiStringBuilder.setLength(0)
    uiStringBuilder.append("[ AUDIO ]")
    drawCenteredHud(canvas, uiStringBuilder, cx, audioMenuY, uiGoldPaint, uiGoldShadowPaint)
    val settingsW = uiGoldPaint.measureText(uiStringBuilder, 0, uiStringBuilder.length)
    openSettingsButtonRect.set(
      cx - settingsW * 0.5f,
      audioMenuY + uiGoldPaint.ascent(),
      cx + settingsW * 0.5f,
      audioMenuY + uiGoldPaint.descent(),
    )
    openSettingsButtonRect.inset(-hudPx(60f), -hudPx(30f))
    uiStringBuilder.setLength(0)
    uiStringBuilder.append("[ DIFFICULTY ]")
    drawCenteredHud(canvas, uiStringBuilder, cx, difficultyMenuY, uiGoldPaint, uiGoldShadowPaint)
    val diffW = uiGoldPaint.measureText(uiStringBuilder, 0, uiStringBuilder.length)
    openDifficultyButtonRect.set(
      cx - diffW * 0.5f,
      difficultyMenuY + uiGoldPaint.ascent(),
      cx + diffW * 0.5f,
      difficultyMenuY + uiGoldPaint.descent(),
    )
    openDifficultyButtonRect.inset(-hudPx(60f), -hudPx(30f))
    uiStringBuilder.setLength(0)
    appendDifficultyName(stageManager.getDifficulty().index - 1)
    val savedTag = uiSmallPaint.textSize
    val savedTagShadow = uiSmallShadowPaint.textSize
    uiSmallPaint.textSize = hudPx(32f)
    uiSmallShadowPaint.textSize = hudPx(32f)
    drawCenteredHud(canvas, uiStringBuilder, cx, difficultyTagY, uiSmallPaint, uiSmallShadowPaint)
    uiStringBuilder.setLength(0)
    uiStringBuilder.append("[ CONTINUE ]")
    drawCenteredHud(canvas, uiStringBuilder, cx, continueMenuY, uiGoldPaint, uiGoldShadowPaint)
    val contW = uiGoldPaint.measureText(uiStringBuilder, 0, uiStringBuilder.length)
    openContinueButtonRect.set(
      cx - contW * 0.5f,
      continueMenuY + uiGoldPaint.ascent(),
      cx + contW * 0.5f,
      continueMenuY + uiGoldPaint.descent(),
    )
    openContinueButtonRect.inset(-hudPx(60f), -hudPx(30f))
    uiStringBuilder.setLength(0)
    appendContinueDipName(stageManager.getContinueDip())
    drawCenteredHud(canvas, uiStringBuilder, cx, continueTagY, uiSmallPaint, uiSmallShadowPaint)
    uiStringBuilder.setLength(0)
    uiStringBuilder.append("[ FIGHTER ]")
    drawCenteredHud(canvas, uiStringBuilder, cx, fighterMenuY, uiGoldPaint, uiGoldShadowPaint)
    val fightW = uiGoldPaint.measureText(uiStringBuilder, 0, uiStringBuilder.length)
    openFighterSelectButtonRect.set(
      cx - fightW * 0.5f,
      fighterMenuY + uiGoldPaint.ascent(),
      cx + fightW * 0.5f,
      fighterMenuY + uiGoldPaint.descent(),
    )
    openFighterSelectButtonRect.inset(-hudPx(60f), -hudPx(30f))
    uiStringBuilder.setLength(0)
    if (player.chosenFighterIndex == 1) {
      uiStringBuilder.append("TYPE-02: F6F HELLCAT")
    } else {
      uiStringBuilder.append("TYPE-01: P-38 LIGHTNING")
    }
    drawCenteredHud(canvas, uiStringBuilder, cx, fighterTagY, uiSmallPaint, uiSmallShadowPaint)
    uiSmallPaint.textSize = savedTag
    uiSmallShadowPaint.textSize = savedTagShadow
    uiStringBuilder.setLength(0)
    uiStringBuilder.append("2026 Claudiu Colteu. All rights reserved.")
    drawCenteredHud(canvas, uiStringBuilder, cx, creditY, uiSmallPaint, uiSmallShadowPaint)
    uiStringBuilder.setLength(0)
    uiStringBuilder.append("VER ")
    uiStringBuilder.append(appVersionName)
    drawCenteredHud(canvas, uiStringBuilder, cx, versionY, uiSmallPaint, uiSmallShadowPaint)
  }

  private fun drawDifficultySelectScreen(canvas: Canvas) {
    canvas.drawColor(0x66000000.toInt())
    val cx = screenW * 0.5f
    val savedGold = uiGoldPaint.textSize
    val savedGoldShadow = uiGoldShadowPaint.textSize
    uiGoldPaint.textSize = hudPx(42f)
    uiGoldShadowPaint.textSize = hudPx(42f)
    uiStringBuilder.setLength(0)
    uiStringBuilder.append("SELECT DIFFICULTY")
    drawCenteredHud(canvas, uiStringBuilder, cx, screenH * 0.16f, uiGoldPaint, uiGoldShadowPaint)
    uiGoldPaint.textSize = savedGold
    uiGoldShadowPaint.textSize = savedGoldShadow

    val top = screenH * 0.28f
    val bottom = screenH * 0.76f
    val step = (bottom - top) / 6f
    val rowHalf = step * 0.42f
    val hitLeft = screenW * 0.10f
    val hitRight = screenW * 0.90f
    val selected = stageManager.getDifficulty()
    var i = 0
    while (i < 7) {
      val lineY = top + i * step
      difficultyButtons[i].set(hitLeft, lineY - rowHalf, hitRight, lineY + rowHalf)
      uiStringBuilder.setLength(0)
      uiStringBuilder.append(i + 1)
      uiStringBuilder.append(". ")
      appendDifficultyName(i)
      val gold = selected === difficultyTiers[i]
      if (gold) {
        drawCenteredHud(canvas, uiStringBuilder, cx, lineY, uiGoldPaint, uiGoldShadowPaint)
      } else {
        drawCenteredHud(canvas, uiStringBuilder, cx, lineY, uiTextPaint, uiShadowPaint)
      }
      i++
    }

    uiStringBuilder.setLength(0)
    uiStringBuilder.append("[ RETURN TO TITLE ]")
    val backY = screenH * 0.88f
    drawCenteredHud(canvas, uiStringBuilder, cx, backY, uiGoldPaint, uiGoldShadowPaint)
    val backW = uiGoldPaint.measureText(uiStringBuilder, 0, uiStringBuilder.length)
    val backX = cx - backW * 0.5f
    diffBackButtonRect.set(
      backX,
      backY + uiGoldPaint.ascent(),
      backX + backW,
      backY + uiGoldPaint.descent(),
    )
  }

  private fun drawContinueSelectScreen(canvas: Canvas) {
    canvas.drawColor(0x66000000.toInt())
    val cx = screenW * 0.5f
    val savedGold = uiGoldPaint.textSize
    val savedGoldShadow = uiGoldShadowPaint.textSize
    uiGoldPaint.textSize = hudPx(42f)
    uiGoldShadowPaint.textSize = hudPx(42f)
    uiStringBuilder.setLength(0)
    uiStringBuilder.append("SELECT CONTINUE")
    drawCenteredHud(canvas, uiStringBuilder, cx, screenH * 0.16f, uiGoldPaint, uiGoldShadowPaint)
    uiGoldPaint.textSize = savedGold
    uiGoldShadowPaint.textSize = savedGoldShadow
    val selected = stageManager.getContinueDip()
    val top = screenH * 0.34f
    val step = screenH * 0.12f
    val rowHalf = step * 0.40f
    val hitLeft = screenW * 0.10f
    val hitRight = screenW * 0.90f
    var i = 0
    while (i < 3) {
      val lineY = top + i * step
      continueButtons[i].set(hitLeft, lineY - rowHalf, hitRight, lineY + rowHalf)
      uiStringBuilder.setLength(0)
      uiStringBuilder.append(i)
      uiStringBuilder.append(". ")
      appendContinueDipName(i)
      if (i == selected) {
        drawCenteredHud(canvas, uiStringBuilder, cx, lineY, uiGoldPaint, uiGoldShadowPaint)
      } else {
        drawCenteredHud(canvas, uiStringBuilder, cx, lineY, uiTextPaint, uiShadowPaint)
      }
      i++
    }
    uiStringBuilder.setLength(0)
    uiStringBuilder.append("[ RETURN TO TITLE ]")
    val backY = screenH * 0.88f
    drawCenteredHud(canvas, uiStringBuilder, cx, backY, uiGoldPaint, uiGoldShadowPaint)
    val backW = uiGoldPaint.measureText(uiStringBuilder, 0, uiStringBuilder.length)
    val backX = cx - backW * 0.5f
    continueBackButtonRect.set(
      backX,
      backY + uiGoldPaint.ascent(),
      backX + backW,
      backY + uiGoldPaint.descent(),
    )
  }

  private fun drawCharacterSelectScreen(canvas: Canvas) {
    canvas.drawColor(0x66000000.toInt())
    val savedGold = uiGoldPaint.textSize
    val savedGoldShadow = uiGoldShadowPaint.textSize
    val savedText = uiTextPaint.textSize
    val savedTextShadow = uiShadowPaint.textSize
    uiGoldPaint.textSize = hudPx(42f)
    uiGoldShadowPaint.textSize = hudPx(42f)

    val cx = screenW * 0.5f
    val cy = screenH * 0.5f
    uiStringBuilder.setLength(0)
    uiStringBuilder.append("SELECT FIGHTER")
    drawCenteredHud(canvas, uiStringBuilder, cx, screenH * 0.14f, uiGoldPaint, uiGoldShadowPaint)

    uiGoldPaint.textSize = hudPx(32f)
    uiGoldShadowPaint.textSize = hudPx(32f)
    uiTextPaint.textSize = hudPx(26f)
    uiShadowPaint.textSize = hudPx(26f)

    val boxHeight = 320f
    val textBlockHeight = 340f
    val totalGroupHeight = boxHeight + textBlockHeight
    val boxTop = cy - (totalGroupHeight * 0.5f)
    val boxBottom = boxTop + boxHeight
    shipLeftSelectRect.set(screenW * 0.08f, boxTop, screenW * 0.46f, boxBottom)
    shipRightSelectRect.set(screenW * 0.54f, boxTop, screenW * 0.92f, boxBottom)
    drawArcadeSelectFrame(canvas, shipLeftSelectRect, focused = false)
    drawArcadeSelectFrame(canvas, shipRightSelectRect, focused = false)
    blitSelectPreview(canvas, selectPreviewP38, shipLeftSelectRect)
    blitSelectPreview(canvas, selectPreviewHellcat, shipRightSelectRect)
    val focusOn = (System.currentTimeMillis() / 400L) % 2L == 0L
    if (focusOn) {
      if (selectedFighterIndex == 1) {
        drawArcadeSelectFrame(canvas, shipRightSelectRect, focused = true)
      } else {
        drawArcadeSelectFrame(canvas, shipLeftSelectRect, focused = true)
      }
    }

    val leftColumnCenterX = (shipLeftSelectRect.left + shipLeftSelectRect.right) * 0.5f
    val rightColumnCenterX = (shipRightSelectRect.left + shipRightSelectRect.right) * 0.5f
    val line1Y = boxBottom + hudPx(110f)
    val line2Y = line1Y + hudPx(64f)
    val line3Y = line2Y + hudPx(72f)
    uiStringBuilder.setLength(0)
    uiStringBuilder.append("TYPE-01:")
    drawCenteredHud(canvas, uiStringBuilder, leftColumnCenterX, line1Y, uiGoldPaint, uiGoldShadowPaint)
    uiStringBuilder.setLength(0)
    uiStringBuilder.append("P-38 LIGHTNING")
    drawCenteredHud(canvas, uiStringBuilder, leftColumnCenterX, line2Y, uiGoldPaint, uiGoldShadowPaint)
    uiStringBuilder.setLength(0)
    uiStringBuilder.append("- FOCUS STORM -")
    drawCenteredHud(canvas, uiStringBuilder, leftColumnCenterX, line3Y, uiTextPaint, uiShadowPaint)
    uiStringBuilder.setLength(0)
    uiStringBuilder.append("TYPE-02:")
    drawCenteredHud(canvas, uiStringBuilder, rightColumnCenterX, line1Y, uiGoldPaint, uiGoldShadowPaint)
    uiStringBuilder.setLength(0)
    uiStringBuilder.append("F6F HELLCAT")
    drawCenteredHud(canvas, uiStringBuilder, rightColumnCenterX, line2Y, uiGoldPaint, uiGoldShadowPaint)
    uiStringBuilder.setLength(0)
    uiStringBuilder.append("- LIGHTNING BLITZ -")
    drawCenteredHud(canvas, uiStringBuilder, rightColumnCenterX, line3Y, uiTextPaint, uiShadowPaint)

    uiGoldPaint.textSize = savedGold
    uiGoldShadowPaint.textSize = savedGoldShadow
    uiTextPaint.textSize = savedText
    uiShadowPaint.textSize = savedTextShadow

    uiStringBuilder.setLength(0)
    uiStringBuilder.append("[ RETURN TO TITLE ]")
    val returnBtnY = screenH * 0.88f
    drawCenteredHud(canvas, uiStringBuilder, cx, returnBtnY, uiGoldPaint, uiGoldShadowPaint)
    val btnW = uiGoldPaint.measureText(uiStringBuilder, 0, uiStringBuilder.length)
    fighterReturnToTitleRect.set(
      cx - btnW * 0.5f,
      returnBtnY + uiGoldPaint.ascent(),
      cx + btnW * 0.5f,
      returnBtnY + uiGoldPaint.descent(),
    )
    fighterReturnToTitleRect.inset(-hudPx(60f), -hudPx(30f))
  }

  private fun drawArcadeSelectFrame(canvas: Canvas, rect: RectF, focused: Boolean) {
    if (!focused) {
      canvas.drawRect(rect, uiSelectWellPaint)
    }
    val outer = if (focused) uiSelectFocusStrokePaint else uiSelectIdleStrokePaint
    val inner = if (focused) uiSelectFocusInnerPaint else uiSelectIdleInnerPaint
    canvas.drawRect(rect, outer)
    val inset = 8f
    canvas.drawRect(
      rect.left + inset,
      rect.top + inset,
      rect.right - inset,
      rect.bottom - inset,
      inner,
    )
    uiSelectCornerPaint.color = if (focused) 0xFFFFD54A.toInt() else 0xFF6E6E6E.toInt()
    val tick = 26f
    val l = rect.left
    val t = rect.top
    val r = rect.right
    val b = rect.bottom
    canvas.drawLine(l, t, l + tick, t, uiSelectCornerPaint)
    canvas.drawLine(l, t, l, t + tick, uiSelectCornerPaint)
    canvas.drawLine(r, t, r - tick, t, uiSelectCornerPaint)
    canvas.drawLine(r, t, r, t + tick, uiSelectCornerPaint)
    canvas.drawLine(l, b, l + tick, b, uiSelectCornerPaint)
    canvas.drawLine(l, b, l, b - tick, uiSelectCornerPaint)
    canvas.drawLine(r, b, r - tick, b, uiSelectCornerPaint)
    canvas.drawLine(r, b, r, b - tick, uiSelectCornerPaint)
  }

  private fun blitSelectPreview(canvas: Canvas, bmp: Bitmap?, box: RectF) {
    if (bmp == null || bmp.isRecycled) return
    val maxW = box.width() * 0.78f
    val maxH = box.height() * 0.78f
    val srcW = bmp.width.toFloat().coerceAtLeast(1f)
    val srcH = bmp.height.toFloat().coerceAtLeast(1f)
    val scale = (maxW / srcW).coerceAtMost(maxH / srcH)
    val dw = srcW * scale
    val dh = srcH * scale
    val px = (box.left + box.right) * 0.5f
    val py = (box.top + box.bottom) * 0.5f
    hudIconDst.set(px - dw * 0.5f, py - dh * 0.5f, px + dw * 0.5f, py + dh * 0.5f)
    canvas.drawBitmap(bmp, null, hudIconDst, bodyPaint)
  }

  private fun appendDifficultyName(index: Int) {
    when (index) {
      0 -> uiStringBuilder.append("MONKEY")
      1 -> uiStringBuilder.append("EASY")
      2 -> uiStringBuilder.append("NORMAL")
      3 -> uiStringBuilder.append("HARD")
      4 -> uiStringBuilder.append("VERY HARD")
      5 -> uiStringBuilder.append("EXPERT")
      else -> uiStringBuilder.append("HARDCORE")
    }
  }

  private fun appendContinueDipName(credits: Int) {
    if (credits <= 0) {
      uiStringBuilder.append("OFF")
    } else if (credits == 1) {
      uiStringBuilder.append("1 CREDIT")
    } else {
      uiStringBuilder.append("2 CREDITS")
    }
  }

  private fun drawHighScoreScreen(canvas: Canvas) {
    val cx = screenW * 0.5f
    val savedGold = uiGoldPaint.textSize
    val savedGoldShadow = uiGoldShadowPaint.textSize
    uiGoldPaint.textSize = hudPx(48f)
    uiGoldShadowPaint.textSize = hudPx(48f)
    uiStringBuilder.setLength(0)
    uiStringBuilder.append("TOP SCORES")
    drawCenteredHud(canvas, uiStringBuilder, cx, screenH * 0.08f, uiGoldPaint, uiGoldShadowPaint)
    uiGoldPaint.textSize = savedGold
    uiGoldShadowPaint.textSize = savedGoldShadow
    val dip = stageManager.getDifficulty().index
    uiStringBuilder.setLength(0)
    appendDifficultyName(dip - 1)
    drawCenteredHud(canvas, uiStringBuilder, cx, screenH * 0.13f, uiTextPaint, uiShadowPaint)
    val savedSmall = uiSmallPaint.textSize
    val savedSmallShadow = uiSmallShadowPaint.textSize
    uiSmallPaint.textSize = hudPx(36f)
    uiSmallShadowPaint.textSize = hudPx(36f)
    val rowStep = screenH * 0.065f
    var i = 0
    while (i < HighScoreManager.SLOT_COUNT) {
      uiStringBuilder.setLength(0)
      uiStringBuilder.append(i + 1)
      uiStringBuilder.append(' ')
      var score = HighScoreManager.scoreAt(dip, i)
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
      uiStringBuilder.append("  ")
      uiStringBuilder.append(HighScoreManager.nameChar(dip, i, 0))
      uiStringBuilder.append(HighScoreManager.nameChar(dip, i, 1))
      uiStringBuilder.append(HighScoreManager.nameChar(dip, i, 2))
      uiStringBuilder.append("  ST")
      uiStringBuilder.append(HighScoreManager.stageAt(dip, i))
      val rowY = screenH * 0.20f + i * rowStep
      drawCenteredHud(canvas, uiStringBuilder, cx, rowY, uiSmallPaint, uiSmallShadowPaint)
      i++
    }
    uiSmallPaint.textSize = savedSmall
    uiSmallShadowPaint.textSize = savedSmallShadow
    if ((System.currentTimeMillis() / 600L) % 2L == 0L) {
      uiStringBuilder.setLength(0)
      uiStringBuilder.append("1P START")
      drawCenteredHud(canvas, uiStringBuilder, cx, screenH * 0.90f, uiGoldPaint, uiGoldShadowPaint)
    }
  }

  private fun layoutRegistrationHitZones() {
    registrationLeftWing.set(0f, screenH * 0.25f, screenW * 0.45f, screenH * 0.65f)
    registrationRightWing.set(screenW * 0.55f, screenH * 0.25f, screenW * 1.0f, screenH * 0.65f)
    registrationSetRect.set(screenW * 0.10f, screenH * 0.75f, screenW * 0.90f, screenH * 0.85f)
  }

  private fun drawRegistrationScreen(canvas: Canvas) {
    canvas.drawColor(0x66000000.toInt())
    layoutRegistrationHitZones()
    val cx = screenW * 0.5f
    uiRegRedPaint.textAlign = Paint.Align.LEFT
    uiTextPaint.textAlign = Paint.Align.LEFT
    uiGoldPaint.textAlign = Paint.Align.LEFT
    uiGoldShadowPaint.textAlign = Paint.Align.LEFT
    uiShadowPaint.textAlign = Paint.Align.LEFT
    uiStringBuilder.setLength(0)
    uiStringBuilder.append("REGISTRATION")
    drawCenteredHud(canvas, uiStringBuilder, cx, screenH * 0.18f, uiRegRedPaint, uiGoldShadowPaint)
    uiStringBuilder.setLength(0)
    uiStringBuilder.append("HI-SCORE ENTRY")
    drawCenteredHud(canvas, uiStringBuilder, cx, screenH * 0.24f, uiTextPaint, uiShadowPaint)
    uiStringBuilder.setLength(0)
    appendDifficultyName(stageManager.getDifficulty().index - 1)
    drawCenteredHud(canvas, uiStringBuilder, cx, screenH * 0.29f, uiGoldPaint, uiGoldShadowPaint)
    val originalGoldSize = uiGoldPaint.textSize
    val originalGoldShadowSize = uiGoldShadowPaint.textSize
    uiGoldPaint.textSize = hudPx(72f)
    uiGoldShadowPaint.textSize = hudPx(72f)
    val charSpacing = hudPx(110f)
    val totalWidth = 2f * charSpacing
    val startX = cx - (totalWidth * 0.5f)
    val letterY = screenH * 0.46f
    val blink = kotlin.math.sin(registrationTextFlashTimer * 14f) * 0.5f + 0.5f
    val blinkAlpha = (80f + blink * 175f).toInt()
    val wingOffset = hudPx(54f)
    var idx = 0
    while (idx < 3) {
      val slotX = startX + (idx * charSpacing)
      val charToDraw = if (idx == registrationActiveCharIndex) {
        registrationCurrentCharValue
      } else {
        pendingInitials[idx]
      }
      if (idx == registrationActiveCharIndex) {
        uiTextPaint.textSize = hudPx(72f)
        uiShadowPaint.textSize = hudPx(72f)
        uiTextPaint.alpha = blinkAlpha
        uiShadowPaint.alpha = blinkAlpha
        uiStringBuilder.setLength(0)
        uiStringBuilder.append('<')
        drawCenteredHud(canvas, uiStringBuilder, slotX - wingOffset, letterY, uiTextPaint, uiShadowPaint)
        uiStringBuilder.setLength(0)
        uiStringBuilder.append('>')
        drawCenteredHud(canvas, uiStringBuilder, slotX + wingOffset, letterY, uiTextPaint, uiShadowPaint)
        uiTextPaint.alpha = 255
        uiShadowPaint.alpha = 255
        uiTextPaint.textSize = hudPx(32f)
        uiShadowPaint.textSize = hudPx(32f)
        uiGoldPaint.alpha = blinkAlpha
        uiGoldShadowPaint.alpha = blinkAlpha
      }
      uiStringBuilder.setLength(0)
      uiStringBuilder.append(charToDraw)
      drawCenteredHud(canvas, uiStringBuilder, slotX, letterY, uiGoldPaint, uiGoldShadowPaint)
      uiGoldPaint.alpha = 255
      uiGoldShadowPaint.alpha = 255
      idx++
    }
    uiGoldPaint.textSize = hudPx(26f)
    uiGoldShadowPaint.textSize = hudPx(26f)
    uiStringBuilder.setLength(0)
    uiStringBuilder.append("[ PRESS ENTER TO LOCK INITIAL ]")
    drawCenteredHud(canvas, uiStringBuilder, cx, screenH * 0.78f, uiGoldPaint, uiGoldShadowPaint)
    uiGoldPaint.textSize = originalGoldSize
    uiGoldShadowPaint.textSize = originalGoldShadowSize
  }

  private fun drawSettingsOverlay(canvas: Canvas) {
    canvas.drawColor(0x66000000.toInt())
    val cx = screenW * 0.5f
    val savedGold = uiGoldPaint.textSize
    val savedGoldShadow = uiGoldShadowPaint.textSize
    uiGoldPaint.textSize = hudPx(42f)
    uiGoldShadowPaint.textSize = hudPx(42f)
    uiStringBuilder.setLength(0)
    uiStringBuilder.append("AUDIO SETTINGS")
    drawCenteredHud(canvas, uiStringBuilder, cx, screenH * 0.16f, uiGoldPaint, uiGoldShadowPaint)
    uiGoldPaint.textSize = savedGold
    uiGoldShadowPaint.textSize = savedGoldShadow

    val savedText = uiTextPaint.textSize
    val savedTextShadow = uiShadowPaint.textSize
    uiTextPaint.textSize = hudPx(52f)
    uiShadowPaint.textSize = hudPx(52f)

    val mid = screenH * 0.52f
    val bgmScale = SoundManager.instance.getBgmVolumeScale()
    uiStringBuilder.setLength(0)
    uiStringBuilder.append("BGM ")
    uiStringBuilder.append((bgmScale * 100f).toInt())
    uiStringBuilder.append('%')
    drawCenteredHud(canvas, uiStringBuilder, cx, mid - screenH * 0.14f, uiTextPaint, uiShadowPaint)
    drawVolumeGage(
      canvas,
      cx,
      mid - screenH * 0.06f,
      bgmScale,
      bgmSliderRect,
      bgmVolumeDownRect,
      bgmVolumeUpRect,
    )

    val sfxScale = SoundManager.instance.getSfxVolumeScale()
    uiStringBuilder.setLength(0)
    uiStringBuilder.append("SFX ")
    uiStringBuilder.append((sfxScale * 100f).toInt())
    uiStringBuilder.append('%')
    drawCenteredHud(canvas, uiStringBuilder, cx, mid + screenH * 0.06f, uiTextPaint, uiShadowPaint)
    drawVolumeGage(
      canvas,
      cx,
      mid + screenH * 0.14f,
      sfxScale,
      sfxSliderRect,
      sfxVolumeDownRect,
      sfxVolumeUpRect,
    )
    uiTextPaint.textSize = savedText
    uiShadowPaint.textSize = savedTextShadow

    uiStringBuilder.setLength(0)
    uiStringBuilder.append("[ RETURN TO TITLE ]")
    val backY = screenH * 0.88f
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

  private fun drawVolumeGage(
    canvas: Canvas,
    cx: Float,
    baseline: Float,
    fill: Float,
    track: RectF,
    downRect: RectF,
    upRect: RectF,
  ) {
    val savedGold = uiGoldPaint.textSize
    val savedGoldShadow = uiGoldShadowPaint.textSize
    val savedText = uiTextPaint.textSize
    val savedShadow = uiShadowPaint.textSize
    val caretSize = hudPx(56f)
    val gageSize = hudPx(88f)
    uiGoldPaint.textSize = gageSize
    uiGoldShadowPaint.textSize = gageSize
    uiTextPaint.textSize = caretSize
    uiShadowPaint.textSize = caretSize
    uiStringBuilder.setLength(0)
    uiStringBuilder.append('\u25A0')
    val slotW = uiGoldPaint.measureText(uiStringBuilder, 0, 1)
    val slots = 10
    val squaresW = slotW * slots
    val squaresLeft = cx - squaresW * 0.5f
    uiTextPaint.getTextBounds("<", 0, 1, srcCore)
    val caretCenterY = baseline + (srcCore.top + srcCore.bottom) * 0.5f
    uiGoldPaint.getTextBounds("\u25A0", 0, 1, srcCore)
    val squareBaseline = caretCenterY - (srcCore.top + srcCore.bottom) * 0.5f
    val squareTop = squareBaseline + srcCore.top
    val squareBottom = squareBaseline + srcCore.bottom
    val lit = (fill * slots + 0.5f).toInt().coerceIn(0, slots)
    val savedWhite = uiTextPaint.color
    var i = 0
    while (i < slots) {
      val x = squaresLeft + i * slotW
      if (i < lit) {
        drawHudTextAt(canvas, uiStringBuilder, 0, 1, x, squareBaseline, uiGoldPaint, uiGoldShadowPaint)
      } else {
        uiTextPaint.textSize = gageSize
        uiShadowPaint.textSize = gageSize
        uiTextPaint.color = 0xFF5A5A5A.toInt()
        drawHudTextAt(canvas, uiStringBuilder, 0, 1, x, squareBaseline, uiTextPaint, uiShadowPaint)
        uiTextPaint.color = savedWhite
        uiTextPaint.textSize = caretSize
        uiShadowPaint.textSize = caretSize
      }
      i++
    }
    track.set(squaresLeft, squareTop, squaresLeft + squaresW, squareBottom)
    val blink = kotlin.math.sin(registrationTextFlashTimer * 14f) * 0.5f + 0.5f
    val blinkAlpha = (80f + blink * 175f).toInt()
    uiTextPaint.alpha = blinkAlpha
    uiShadowPaint.alpha = blinkAlpha
    uiStringBuilder.setLength(0)
    uiStringBuilder.append('<')
    val caretW = uiTextPaint.measureText(uiStringBuilder, 0, 1)
    val caretGap = hudPx(18f)
    drawCenteredHud(
      canvas,
      uiStringBuilder,
      squaresLeft - caretGap - caretW * 0.5f,
      baseline,
      uiTextPaint,
      uiShadowPaint,
    )
    uiStringBuilder.setLength(0)
    uiStringBuilder.append('>')
    drawCenteredHud(
      canvas,
      uiStringBuilder,
      squaresLeft + squaresW + caretGap + caretW * 0.5f,
      baseline,
      uiTextPaint,
      uiShadowPaint,
    )
    uiTextPaint.alpha = 255
    uiShadowPaint.alpha = 255
    layoutVolumeCaretHits(track, downRect, upRect)
    uiGoldPaint.textSize = savedGold
    uiGoldShadowPaint.textSize = savedGoldShadow
    uiTextPaint.textSize = savedText
    uiShadowPaint.textSize = savedShadow
  }

  private fun layoutVolumeCaretHits(track: RectF, downRect: RectF, upRect: RectF) {
    val pad = hudPx(40f)
    downRect.set(0f, track.top - pad, track.left, track.bottom + pad)
    upRect.set(track.right, track.top - pad, screenW.toFloat(), track.bottom + pad)
  }

  private fun bumpVolumeScale(current: Float, up: Boolean): Float {
    val slots = 10
    val cur = (current * slots + 0.5f).toInt().coerceIn(0, slots)
    val next = (if (up) cur + 1 else cur - 1).coerceIn(0, slots)
    return next / slots.toFloat()
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
    accentShadowPaint.textAlign = fill.textAlign
    if (fill === uiGoldPaint) {
      accentShadowPaint.color = 0xFFB35400.toInt()
    } else {
      accentShadowPaint.color = 0xFF3A3A3A.toInt()
    }
    val drop = hudPx(4f)
    val rim = hudPx(2f)
    canvas.drawText(text, start, end, x + drop, y + drop, shadow)
    canvas.drawText(text, start, end, x + rim, y + rim, accentShadowPaint)
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
        bombCoreWasOpen = false
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
          val ew = enemies.halfWOf(enemy)
          val eh = enemies.halfHOf(enemy)
          if (
            enemy.x + ew >= bombLeft && enemy.x - ew <= bombRight &&
            enemy.y + eh >= bombTop && enemy.y - eh <= bombBottom
          ) {
            enemy.health -= enemyDmg
            if (enemy.type == ENEMY_TYPE_HEAVY) {
              enemy.triggerMicroShudder()
            }
            if (enemy.health <= 0) {
              onEnemyKilled(enemy)
              enemy.isActive = false
              particles.triggerExplosion(enemy.x, enemy.y, true)
              dropEnemyLoot(enemy)
            }
          }
        }
        ei++
      }
    }

    if (boss.isActive() && !boss.isExploding()) {
      val bossDps = BOMB_BOSS_DPS
      bossBombDmgBank += bossDps * dt
      val raw = bossBombDmgBank.toInt()
      if (raw > 0) {
        bossBombDmgBank -= raw
        var dmg = raw
        if (dmg > BOMB_BOSS_DPS_FRAME_CAP) dmg = BOMB_BOSS_DPS_FRAME_CAP
        if (boss.usesStage7Hitboxes()) {
          boss.applyStage7AreaDamage(bombLeft, bombTop, bombRight, bombBottom, dmg)
          if (boss.consumeStage7Break()) {
            particles.triggerExplosion(boss.stage7BreakX(), boss.stage7BreakY())
          }
        } else {
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
                if (part.componentType == BossController.TYPE_CORE && !bombCoreWasOpen) {
                  pi--
                  continue
                }
                part.health -= dmg
                part.triggerMicroShudder()
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
  }

  private fun drawPanicBomb(canvas: Canvas) {
    if (!panicBomb.isActive) return
    val frame = panicBomb.currentFrameIndex
    if (frame < 0 || frame > 5) return
    val activeBmp = bombSheets[frame] ?: return
    canvas.drawBitmap(activeBmp, null, bombDstRect, bodyPaint)
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
    if (hitPipFullBmp == null) {
      hitPipFullBmp = decodeKeyedLime(R.drawable.hud_hit_pip_full)
    }
    if (hitPipMidBmp == null) {
      hitPipMidBmp = decodeKeyedLime(R.drawable.hud_hit_pip_mid)
    }
    if (hitPipWarnBmp == null) {
      hitPipWarnBmp = decodeKeyedLime(R.drawable.hud_hit_pip_warn)
    }
    if (hitPipEmptyBmp == null) {
      hitPipEmptyBmp = decodeKeyedLime(R.drawable.hud_hit_pip_empty)
    }
    if (logoBmp == null) {
      logoBmp = decodeKeyed(R.drawable.game_logo)
    }
    if (titleBackdropBmp == null) {
      titleBackdropBmp = decodeOpaque(R.drawable.title_screen_backdrop)
    }
    if (selectPreviewP38 == null) {
      selectPreviewP38 = decodeKeyed(R.drawable.player_ship_4)
    }
    if (selectPreviewHellcat == null) {
      selectPreviewHellcat = decodeKeyed(R.drawable.player_b_4)
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

  private fun tickParallax(scrollSpeedY: Float, dt: Float) {
    if (stageManager.isFacilityTheater) {
      parallax.updateStage7(scrollSpeedY, dt)
    } else if (stageManager.isAscentTheater) {
      parallax.updateStage8(scrollSpeedY, dt, timeline.elapsedSeconds())
    } else {
      parallax.update(scrollSpeedY * dt)
    }
  }

  private fun stageGroundBitmap(): Bitmap? {
    val def = stageManager.def
    if (def.theaterKind == StageTheaterKind.SCROLL && def.hasOverlayClouds) {
      return null
    }
    return if (def.theaterKind == StageTheaterKind.ASCENT) theater.activeFloor else theater.floor
  }

  private fun reloadStageBackgrounds(width: Int, height: Int, restartPlayback: Boolean = false) {
    if (width <= 0 || height <= 0) return
    val def = stageManager.def
    enemies.bindTheaterSkins(null, null, null)
    theater.load(context.assets, def, width, restartPlayback)
    enemies.bindTheaterSkins(
      theater.skinTank,
      theater.skinDestroyer,
      theater.skinWagon,
      theater.skinHelicopter,
    )
    val floorLayer =
      if (def.theaterKind == StageTheaterKind.ASCENT) theater.activeFloor else theater.floor
    parallax.setScrollLayers(floorLayer, theater.mid, theater.high)
  }

  private fun maybeSwapStage6Floor() {
    val def = stageManager.def
    if (def.theaterKind != StageTheaterKind.ASCENT) return
    if (theater.floorSwapped) return
    if (timeline.elapsedSeconds() < def.spaceSwapAt) return
    theater.swapToFloorAlt()
    parallax.setScrollLayers(theater.activeFloor, theater.mid, theater.high)
  }

  private fun bootLaunchStageIfNeeded() {
    if (gameState != STATE_TITLE) return
    stageManager.resetToStart()
    ScoreManager.instance.syncDifficultyMultiplier(stageManager.getDifficulty().index)
    timeline.reset()
    enemies.deactivateAll()
    enemyShots.deactivateAll()
    boss.bindStage(stageManager.currentStage)
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
    StageBitmaps.keyGreen(bmp)
    return bmp
  }

  private fun decodeKeyedLime(drawableId: Int): Bitmap {
    val opts = BitmapFactory.Options().apply {
      inScaled = false
      inPreferredConfig = Bitmap.Config.ARGB_8888
      inMutable = true
    }
    val src = BitmapFactory.decodeResource(resources, drawableId, opts)
      ?: error("Missing drawable $drawableId")
    val bmp = if (src.isMutable) src else src.copy(Bitmap.Config.ARGB_8888, true).also { src.recycle() }
    keyLimeBackdrop(bmp)
    return bmp
  }

  private fun decodeOpaque(drawableId: Int): Bitmap {
    val opts = BitmapFactory.Options().apply {
      inScaled = false
      inPreferredConfig = Bitmap.Config.ARGB_8888
    }
    return BitmapFactory.decodeResource(resources, drawableId, opts)
      ?: error("Missing drawable $drawableId")
  }

  private fun keyLimeBackdrop(bmp: Bitmap) {
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
        if (g > 200 && r < 50 && b < 50) {
          row[i] = 0
        }
        i++
      }
      bmp.setPixels(row, 0, w, 0, rowY, w, 1)
      rowY++
    }
    bmp.setHasAlpha(true)
  }

  private fun resolveCollisions() {
    if (player.getHealth() <= 0) {
      if (gameState == STATE_PLAYING) offerContinueOrGameOver()
      return
    }
    val bulletPool = bullets.getBulletPool()
    val bulletCount = bullets.getPoolSize()
    val enemyPool = enemies.getEnemyPool()
    val enemyCount = enemies.getPoolSize()
    var bi = 0
    while (bi < bulletCount) {
      val bullet = bulletPool[bi]
      if (bullet.isActive) {
        var ei = 0
        while (ei < enemyCount) {
          val enemy = enemyPool[ei]
          if (enemy.isActive) {
            if (shotHitsEnemy(bullet.x - enemy.x, bullet.y - enemy.y, enemy)) {
              bullet.isActive = false
              enemy.health -= 1
              if (enemy.type == ENEMY_TYPE_HEAVY) {
                enemy.triggerMicroShudder()
              }
              if (enemy.health <= 0) {
                onEnemyKilled(enemy)
                fireRevengeIfNeeded(enemy)
                enemy.isActive = false
                particles.triggerExplosion(enemy.x, enemy.y, true)
                dropEnemyLoot(enemy)
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
            if (shotHitsEnemy(missile.x - enemy.x, missile.y - enemy.y, enemy)) {
              missile.isActive = false
              enemy.health -= 1
              if (enemy.type == ENEMY_TYPE_HEAVY) {
                enemy.triggerMicroShudder()
              }
              if (enemy.health <= 0) {
                onEnemyKilled(enemy)
                fireRevengeIfNeeded(enemy)
                enemy.isActive = false
                particles.triggerExplosion(enemy.x, enemy.y, true)
                dropEnemyLoot(enemy)
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
      if (boss.usesStage7Hitboxes()) {
        bi = 0
        while (bi < bulletCount) {
          val bullet = bulletPool[bi]
          if (bullet.isActive && boss.checkStage7Collision(bullet)) {
            bullet.isActive = false
            if (boss.consumeStage7Break()) {
              particles.triggerExplosion(boss.stage7BreakX(), boss.stage7BreakY(), false)
            }
          }
          bi++
        }
        mi = 0
        while (mi < missileCount) {
          val missile = missilePool[mi]
          if (missile.isActive && boss.checkCollisionAt(missile.x, missile.y, 1)) {
            missile.isActive = false
            if (boss.consumeStage7Break()) {
              particles.triggerExplosion(boss.stage7BreakX(), boss.stage7BreakY(), false)
            }
          }
          mi++
        }
      } else {
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
                    part.triggerMicroShudder()
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
                    part.triggerMicroShudder()
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
    }

    val enemyBullets = enemyShots.getBulletPool()
    val playerX = player.getHitboxX()
    val playerY = player.getHitboxY()
    if (bullets.resolveEnemyBulletsVsPlayer(
        player,
        enemyBullets,
        enemyShots.getPoolSize(),
        particles,
        gameState == STATE_PLAYING,
      ) && player.isGameOver() && gameState == STATE_PLAYING
    ) {
      offerContinueOrGameOver()
    }

    if (!player.isGameOver() && player.isOnField()) {
      val ramBody = ENEMY_RAM_BODY_FRAC
      val playerRadius = PLAYER_HIT_RADIUS
      var ei = 0
      while (ei < enemyCount) {
        val enemy = enemyPool[ei]
        if (enemy.isActive) {
          val sx = playerRadius + enemies.halfWOf(enemy) * ramBody
          val sy = playerRadius + enemies.halfHOf(enemy) * ramBody
          if (sx > 0f && sy > 0f) {
            val nx = (enemy.x - playerX) / sx
            val ny = (enemy.y - playerY) / sy
            if ((nx * nx) + (ny * ny) <= 1f) {
              onEnemyKilled(enemy)
              enemy.isActive = false
              particles.triggerExplosion(enemy.x, enemy.y)
              dropEnemyLoot(enemy)
              if (player.takeDamage()) {
                particles.triggerExplosion(playerX, playerY)
                if (player.isGameOver() && gameState == STATE_PLAYING) offerContinueOrGameOver()
              }
              break
            }
          }
        }
        ei++
      }
    }

    if (!player.isGameOver() && player.isOnField() && boss.isActive() && !boss.isExploding()) {
      val playerRadius = PLAYER_HIT_RADIUS
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
              if (player.isGameOver() && gameState == STATE_PLAYING) offerContinueOrGameOver()
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
    while (ii < itemCount && player.isOnField()) {
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
          } else if (item.itemType == PowerUpItem.ITEM_TYPE_SHIELD) {
            player.restoreHits()
            SoundManager.instance.playSFX(SoundManager.SFX_PICKUP)
          } else {
            collectPowerUp(item.x, item.y)
          }
        }
      }
      ii++
    }
  }

  private fun shotHitsEnemy(dx: Float, dy: Float, enemy: Enemy): Boolean {
    val type = enemy.type
    val popcorn = type != ENEMY_TYPE_INTERCEPTOR && type != ENEMY_TYPE_HEAVY
    val pad = if (popcorn) SHOT_HIT_PAD_POPCORN else SHOT_HIT_PAD
    val frac = if (popcorn) SHOT_HIT_BODY_FRAC_POPCORN else SHOT_HIT_BODY_FRAC
    val sx = pad + enemies.halfWOf(enemy) * frac
    val sy = pad + enemies.halfHOf(enemy) * frac
    if (sx <= 0f || sy <= 0f) return false
    val nx = dx / sx
    val ny = dy / sy
    return (nx * nx) + (ny * ny) <= 1f
  }

  private fun onEnemyKilled(enemy: Enemy) {
    if (gameState == STATE_PLAYING) {
      awardKillScore(enemy)
    }
    if (enemy.deathClearBullets) {
      enemyShots.beginDeathClear(enemy.x, enemy.y)
    }
    if (enemy.diamondLeader) {
      enemies.triggerDiamondSplinter()
    }
  }

  private fun awardKillScore(enemy: Enemy) {
    val base = when (enemy.type) {
      ENEMY_TYPE_HEAVY -> KILL_SCORE_HEAVY
      ENEMY_TYPE_INTERCEPTOR -> KILL_SCORE_INTERCEPTOR
      else -> KILL_SCORE_DRONE
    }
    val awarded = ScoreManager.instance.scalePoints(base)
    if (awarded <= 0) return
    ScoreManager.instance.addScore(awarded)
    triggerFloatingScore(enemy.x, enemy.y, awarded)
  }

  private fun applyPendingExtends() {
    val scores = ScoreManager.instance
    while (scores.consumeExtend()) {
      if (player.grantExtraLife()) {
        SoundManager.instance.playSFX(SoundManager.SFX_PICKUP)
      }
    }
  }

  private fun fireRevengeIfNeeded(enemy: Enemy) {
    if (enemy.deathClearBullets) return
    if (enemy.isMidBoss) return
    val popcorn =
      enemy.type != ENEMY_TYPE_INTERCEPTOR && enemy.type != ENEMY_TYPE_HEAVY
    if (!stageManager.revengeOnDeath()) {
      if (!popcorn || !stageManager.popcornSuicide()) return
    }
    val px = player.getHitboxX()
    val py = player.getHitboxY()
    val dx = px - enemy.x
    val dy = py - enemy.y
    val lenSq = dx * dx + dy * dy
    if (lenSq <= 0.0001f) return
    val speed = REVENGE_SHOT_SPEED * stageManager.shotSpeedScale()
    val inv = speed / kotlin.math.sqrt(lenSq)
    val vx = dx * inv
    val vy = dy * inv
    if (enemy.type == ENEMY_TYPE_HEAVY && stageManager.getDifficulty().index == 7) {
      val ang = kotlin.math.atan2(vy, vx)
      val left = ang - REVENGE_SPREAD_RAD
      val right = ang + REVENGE_SPREAD_RAD
      enemyShots.fireBullet(enemy.x, enemy.y, kotlin.math.cos(left) * speed, kotlin.math.sin(left) * speed)
      enemyShots.fireBullet(enemy.x, enemy.y, vx, vy)
      enemyShots.fireBullet(enemy.x, enemy.y, kotlin.math.cos(right) * speed, kotlin.math.sin(right) * speed)
    } else {
      enemyShots.fireBullet(enemy.x, enemy.y, vx, vy)
    }
  }

  private fun dropEnemyLoot(enemy: Enemy) {
    val x = enemy.x
    val y = enemy.y
    if (enemy.isMidBoss) {
      powerUpItem.spawn(x - 28f, y, PowerUpItem.ITEM_TYPE_MEDAL)
      powerUpItem.spawn(x, y, PowerUpItem.ITEM_TYPE_MEDAL)
      powerUpItem.spawn(x + 28f, y, PowerUpItem.ITEM_TYPE_MEDAL)
      val extra = if (availableBombs < MAX_BOMBS) {
        PowerUpItem.ITEM_TYPE_BOMB
      } else {
        PowerUpItem.ITEM_TYPE_POWERUP
      }
      powerUpItem.spawn(x, y + 36f, extra)
      return
    }
    powerUpItem.spawn(x, y, PowerUpItem.ITEM_TYPE_MEDAL)
    if (enemy.isRedShipAnchor) {
      powerUpItem.spawn(x, y, PowerUpItem.ITEM_TYPE_POWERUP)
      return
    }
    val armored =
      enemy.type == ENEMY_TYPE_HEAVY || enemy.type == ENEMY_TYPE_INTERCEPTOR
    var dropChance = if (armored) 0.40f else 0.15f
    dropChance *= stageManager.lootChanceScale()
    val cap = if (armored) 0.50f else 0.20f
    if (dropChance > cap) dropChance = cap
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

  private fun spawnRespawnPowerUp() {
    val x = player.getHitboxX()
    var y = player.getHitboxY() - RESPAWN_POWERUP_LIFT
    if (y < RESPAWN_POWERUP_MIN_Y) y = RESPAWN_POWERUP_MIN_Y
    powerUpItem.spawnSway(x, y, PowerUpItem.ITEM_TYPE_POWERUP)
  }

  private fun collectPowerUp(x: Float, y: Float) {
    if (player.getWeaponPower() < 3) {
      player.upgradeWeapon()
    } else {
      val awarded = ScoreManager.instance.scalePoints(POWERUP_FULL_SCORE)
      ScoreManager.instance.addScore(awarded)
      triggerFloatingScore(x, y, awarded)
    }
    SoundManager.instance.playSFX(SoundManager.SFX_PICKUP)
  }

  private fun collectBomb(x: Float, y: Float) {
    if (availableBombs < MAX_BOMBS) {
      availableBombs++
    } else {
      val awarded = ScoreManager.instance.scalePoints(BOMB_FULL_SCORE)
      ScoreManager.instance.addScore(awarded)
      triggerFloatingScore(x, y, awarded)
    }
    SoundManager.instance.playSFX(SoundManager.SFX_PICKUP)
  }

  private fun collectMedal(item: PowerUpSlot) {
    val points = if (item.pickupPoints > 0) {
      item.pickupPoints
    } else if (item.medalFrameIndex == 0) {
      MEDAL_SCORE_FACE
    } else {
      MEDAL_SCORE_EDGE
    }
    val awarded = ScoreManager.instance.scalePoints(points)
    ScoreManager.instance.addScore(awarded)
    if (item.isSecretMedal) {
      ScoreManager.instance.collectSecretMedal()
    }
    triggerFloatingScore(item.x, item.y, awarded)
    SoundManager.instance.playSFX(SoundManager.SFX_PICKUP)
  }

  private fun triggerFloatingScore(startX: Float, startY: Float, value: Int) {
    var free = -1
    var oldest = 0
    var oldestAge = -1f
    var i = 0
    while (i < SCORE_POPUP_SLOTS) {
      val p = scorePool[i]
      if (!p.isActive) {
        free = i
        break
      }
      if (p.age > oldestAge) {
        oldestAge = p.age
        oldest = i
      }
      i++
    }
    val p = scorePool[if (free >= 0) free else oldest]
    p.x = startX
    p.y = startY
    p.scoreValue = value
    p.age = 0f
    p.isActive = true
  }

  private fun updateFloatingScores(dt: Float) {
    val scores = ScoreManager.instance
    while (scores.hasPopup()) {
      triggerFloatingScore(scores.popupX(), scores.popupY(), scores.popupValue())
      scores.consumePopup()
    }
    var i = 0
    while (i < SCORE_POPUP_SLOTS) {
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
    while (i < SCORE_POPUP_SLOTS) {
      scorePool[i].isActive = false
      i++
    }
  }

  private fun drawFloatingScores(canvas: Canvas) {
    var i = 0
    while (i < SCORE_POPUP_SLOTS) {
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
        val drop = hudPx(2f)
        canvas.drawText(uiStringBuilder, 0, end, p.x - w * 0.5f + drop, p.y + drop, popupShadowPaint)
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
    enemies.bindTheaterSkins(null, null, null)
    theater.recycle()
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
    val hitFull = hitPipFullBmp
    if (hitFull != null && !hitFull.isRecycled) hitFull.recycle()
    hitPipFullBmp = null
    val hitMid = hitPipMidBmp
    if (hitMid != null && !hitMid.isRecycled) hitMid.recycle()
    hitPipMidBmp = null
    val hitWarn = hitPipWarnBmp
    if (hitWarn != null && !hitWarn.isRecycled) hitWarn.recycle()
    hitPipWarnBmp = null
    val hitEmpty = hitPipEmptyBmp
    if (hitEmpty != null && !hitEmpty.isRecycled) hitEmpty.recycle()
    hitPipEmptyBmp = null
    val logo = logoBmp
    if (logo != null && !logo.isRecycled) logo.recycle()
    logoBmp = null
    val titleBackdrop = titleBackdropBmp
    if (titleBackdrop != null && !titleBackdrop.isRecycled) titleBackdrop.recycle()
    titleBackdropBmp = null
    val previewP38 = selectPreviewP38
    if (previewP38 != null && !previewP38.isRecycled) previewP38.recycle()
    selectPreviewP38 = null
    val previewHellcat = selectPreviewHellcat
    if (previewHellcat != null && !previewHellcat.isRecycled) previewHellcat.recycle()
    selectPreviewHellcat = null
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
    uiController.releaseInterstitials()
    super.onDetachedFromWindow()
  }

  private fun sweepPlayfieldForClear() {
    bullets.deactivateAll()
    homingMissiles.deactivateAll()
    enemies.deactivateAll()
    enemyShots.deactivateAll()
    powerUpItem.deactivateAll()
    deactivateScorePopups()
    panicBomb.isActive = false
    boss.deactivate()
    shakeDuration = 0f
    shakeIntensity = 0f
    flashDuration = 0f
    flashWhiteDecay = false
  }

  private fun beginCampaignFromMenu() {
    stageManager.resetToStart()
    stageManager.resetCombatRank()
    ScoreManager.instance.syncDifficultyMultiplier(stageManager.getDifficulty().index)
    player.resetWeaponPower()
    player.restoreLives()
    availableBombs = START_BOMBS
    continuesRemaining = stageManager.getContinueDip()
    continuedThisCredit = false
    ScoreManager.instance.reset()
    ScoreManager.instance.armExtends()
    maxStageCleared = 0
    resetStage()
    attractCycleTimer = 0f
    attractCycleState = ATTRACT_TITLE
    interstitialTimer = INTERSTITIAL_SECS
    gameState = STATE_INTERSTITIAL
  }

  private fun resetStage() {
    ScoreManager.instance.resetStageCounters()
    ScoreManager.instance.syncDifficultyMultiplier(stageManager.getDifficulty().index)
    panicBomb.isActive = false
    bossWasExploding = false
    shakeDuration = 0f
    shakeIntensity = 0f
    flashDuration = 0f
    flashWhiteDecay = false
    enemyBombDmgBank = 0f
    bossBombDmgBank = 0f
    bombCoreWasOpen = false
    awaitingSecondTap = false
    isDraggingShip = false
    grabOffsetX = 0f
    grabOffsetY = 0f
    dragPointerId = -1
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
    HiddenMedalRoute.bind(stageManager.currentStage)
    ScoreManager.instance.armSecretRoute(HiddenMedalRoute.cueCount())
    if (stageManager.def.introOnly) {
      enemies.deactivateAll()
      enemyShots.deactivateAll()
    }
    parallax.resetScroll()
    lastBgmRes = 0
    resetRegistration()
    reloadStageBackgrounds(screenW, screenH, restartPlayback = true)
  }

  private fun resetRegistration() {
    registrationActiveCharIndex = 0
    registrationCurrentCharValue = 'A'
    registrationTextFlashTimer = 0f
    pendingInitials[0] = 'A'
    pendingInitials[1] = 'A'
    pendingInitials[2] = 'A'
  }

  private fun pickAttractStage(lastId: Int): Int {
    val catalog = StageCatalog.all
    var eligible = 0
    var i = 0
    while (i < catalog.size) {
      if (!catalog[i].introOnly) eligible++
      i++
    }
    if (eligible <= 0) return catalog[0].id
    var lcgSeed = System.nanoTime()
    var tries = 0
    var pick = lastId
    while (tries < 32) {
      lcgSeed = lcgSeed * 1664525L + 1013904223L
      val def = catalog[(((lcgSeed ushr 16) and 0xFFFFL).toInt() and 0x7fffffff) % catalog.size]
      if (!def.introOnly) {
        pick = def.id
        if (eligible == 1 || pick != lastId) return pick
      }
      tries++
    }
    i = 0
    while (i < catalog.size) {
      if (!catalog[i].introOnly) return catalog[i].id
      i++
    }
    return catalog[0].id
  }

  private fun beginDemo() {
    demoT = 0f
    attractCycleTimer = 0f
    attractCycleState = ATTRACT_CPU_DEMO
    val nextDemoStage = pickAttractStage(lastDemoStage)
    lastDemoStage = nextDemoStage
    stageManager.setCurrentStage(nextDemoStage)
    stageManager.resetCombatRank()
    ScoreManager.instance.syncDifficultyMultiplier(stageManager.getDifficulty().index)
    player.resetWeaponPower()
    player.restoreLives()
    availableBombs = START_BOMBS
    resetStage()
    player.upgradeWeapon()
    player.upgradeWeapon()
    player.setAutoFire(true)
    lastBgmRes = 0
    gameState = STATE_DEMO
  }

  private fun offerContinueOrGameOver() {
    if (continuesRemaining > 0) {
      SoundManager.instance.stopAlarm()
      continuePromptT = 0f
      gameState = STATE_CONTINUE
      return
    }
    enterGameOver()
  }

  private fun acceptContinueCredit() {
    if (continuesRemaining <= 0) {
      enterGameOver()
      return
    }
    continuesRemaining--
    continuedThisCredit = true
    availableBombs = START_BOMBS
    player.resetWeaponPower()
    player.acceptContinueBody()
    SoundManager.instance.playSFX(SoundManager.SFX_PICKUP)
    gameState = STATE_PLAYING
  }

  private fun enterGameOver() {
    SoundManager.instance.stopAlarm()
    gameOverT = 0f
    gameState = STATE_GAMEOVER
  }

  private fun routeAfterGameOver() {
    if (qualifiesForRanking()) {
      beginRegistration()
    } else {
      finishDemoToHighScore()
    }
  }

  private fun qualifiesForRanking(): Boolean {
    if (continuedThisCredit) return false
    return HighScoreManager.checkIfQualifies(campaignScore, stageManager.getDifficulty().index)
  }

  private fun beginRegistration() {
    SoundManager.instance.stopAlarm()
    resetRegistration()
    lastBgmRes = 0
    gameState = STATE_REGISTRATION
  }

  private fun bumpRegistrationChar(up: Boolean) {
    var code = registrationCurrentCharValue.code
    if (up) {
      code++
      if (code > 'Z'.code) code = 'A'.code
    } else {
      code--
      if (code < 'A'.code) code = 'Z'.code
    }
    registrationCurrentCharValue = code.toChar()
    SoundManager.instance.playSFX(SoundManager.SFX_PICKUP)
  }

  private fun confirmRegistrationLetter() {
    pendingInitials[registrationActiveCharIndex] = registrationCurrentCharValue
    registrationActiveCharIndex++
    SoundManager.instance.playSFX(SoundManager.SFX_PICKUP)
    if (registrationActiveCharIndex >= 3) {
      HighScoreManager.checkAndInsertNewScore(
        context,
        campaignScore,
        pendingInitials[0],
        pendingInitials[1],
        pendingInitials[2],
        maxStageCleared,
        stageManager.getDifficulty().index,
      )
      finishDemoToHighScore()
    } else {
      registrationCurrentCharValue = 'A'
    }
  }

  private fun returnToTitle() {
    player.setAutoFire(false)
    stageManager.resetToStart()
    stageManager.resetCombatRank()
    ScoreManager.instance.syncDifficultyMultiplier(stageManager.getDifficulty().index)
    player.resetWeaponPower()
    player.restoreLives()
    availableBombs = START_BOMBS
    ScoreManager.instance.reset()
    campaignScore = 0
    maxStageCleared = 0
    resetStage()
    attractCycleTimer = 0f
    attractCycleState = ATTRACT_TITLE
    demoT = 0f
    gameOverT = 0f
    lastBgmRes = 0
    isSettingsMenuOpen = false
    gameState = STATE_TITLE
  }

  private fun abortAttractToTitle() {
    returnToTitle()
  }

  private fun finishDemoToHighScore() {
    returnToTitle()
    attractCycleState = ATTRACT_HIGH_SCORE
    attractCycleTimer = 0f
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

  private fun applyShipTether(dt: Float) {
    if (!isDraggingShip) return
    player.followTether(steerFingerX, steerFingerY, grabOffsetX, grabOffsetY, dt)
  }

  private fun tryGrabShip(fingerX: Float, fingerY: Float, pointerId: Int) {
    if (isDraggingShip) return
    val gx = fingerX - player.getHitboxX()
    val gy = fingerY - player.getHitboxY()
    val grab = player.touchGrabRadius()
    if ((gx * gx + gy * gy) > grab * grab) return
    isDraggingShip = true
    grabOffsetX = gx
    grabOffsetY = gy
    steerFingerX = fingerX
    steerFingerY = fingerY
    dragPointerId = pointerId
  }

  override fun onTouchEvent(event: MotionEvent): Boolean {
    val down = event.actionMasked == MotionEvent.ACTION_DOWN ||
      event.actionMasked == MotionEvent.ACTION_POINTER_DOWN
    if (
      down &&
      (attractCycleState == ATTRACT_CPU_DEMO || attractCycleState == ATTRACT_HIGH_SCORE)
    ) {
      abortAttractToTitle()
      return true
    }
    when (gameState) {
      STATE_TITLE -> {
        if (down || event.actionMasked == MotionEvent.ACTION_MOVE) {
          attractCycleTimer = 0f
          attractCycleState = ATTRACT_TITLE
          val x = event.x
          val y = event.y
          if (isSettingsMenuOpen) {
            if (down) {
              if (bgmVolumeDownRect.contains(x, y)) {
                SoundManager.instance.setBgmVolumeScale(
                  bumpVolumeScale(SoundManager.instance.getBgmVolumeScale(), false),
                )
              } else if (bgmVolumeUpRect.contains(x, y)) {
                SoundManager.instance.setBgmVolumeScale(
                  bumpVolumeScale(SoundManager.instance.getBgmVolumeScale(), true),
                )
              } else if (sfxVolumeDownRect.contains(x, y)) {
                SoundManager.instance.setSfxVolumeScale(
                  bumpVolumeScale(SoundManager.instance.getSfxVolumeScale(), false),
                )
                SoundManager.instance.playSFX(SoundManager.SFX_VULCAN)
              } else if (sfxVolumeUpRect.contains(x, y)) {
                SoundManager.instance.setSfxVolumeScale(
                  bumpVolumeScale(SoundManager.instance.getSfxVolumeScale(), true),
                )
                SoundManager.instance.playSFX(SoundManager.SFX_VULCAN)
              } else if (backButtonRect.contains(x, y)) {
                isSettingsMenuOpen = false
                SoundManager.instance.playSFX(SoundManager.SFX_PICKUP)
              }
            }
          } else if (down && openSettingsButtonRect.contains(x, y)) {
            isSettingsMenuOpen = true
            registrationTextFlashTimer = 0f
            SoundManager.instance.playSFX(SoundManager.SFX_PICKUP)
          } else if (down && openDifficultyButtonRect.contains(x, y)) {
            attractCycleTimer = 0f
            attractCycleState = ATTRACT_TITLE
            gameState = STATE_DIFFICULTY_SELECT
            SoundManager.instance.playSFX(SoundManager.SFX_PICKUP)
          } else if (down && openContinueButtonRect.contains(x, y)) {
            attractCycleTimer = 0f
            attractCycleState = ATTRACT_TITLE
            gameState = STATE_CONTINUE_SELECT
            SoundManager.instance.playSFX(SoundManager.SFX_PICKUP)
          } else if (down && openFighterSelectButtonRect.contains(x, y)) {
            attractCycleTimer = 0f
            attractCycleState = ATTRACT_TITLE
            selectedFighterIndex = player.chosenFighterIndex
            gameState = STATE_CHARACTER_SELECT
            SoundManager.instance.playSFX(SoundManager.SFX_PICKUP)
          } else if (down) {
            beginCampaignFromMenu()
          }
        }
        return true
      }
      STATE_CHARACTER_SELECT -> {
        if (down) {
          val x = event.x
          val y = event.y
          if (shipLeftSelectRect.contains(x, y)) {
            selectedFighterIndex = 0
            player.applyFighterConfiguration(0)
            stageManager.saveFighterSetting(context, 0)
            SoundManager.instance.playSFX(SoundManager.SFX_PICKUP)
          } else if (shipRightSelectRect.contains(x, y)) {
            selectedFighterIndex = 1
            player.applyFighterConfiguration(1)
            stageManager.saveFighterSetting(context, 1)
            SoundManager.instance.playSFX(SoundManager.SFX_PICKUP)
          } else if (fighterReturnToTitleRect.contains(x, y)) {
            SoundManager.instance.playSFX(SoundManager.SFX_PICKUP)
            attractCycleTimer = 0f
            attractCycleState = ATTRACT_TITLE
            gameState = STATE_TITLE
          }
        }
        return true
      }
      STATE_CONTINUE_SELECT -> {
        if (down) {
          val x = event.x
          val y = event.y
          if (continueBackButtonRect.contains(x, y)) {
            attractCycleTimer = 0f
            attractCycleState = ATTRACT_TITLE
            gameState = STATE_TITLE
            SoundManager.instance.playSFX(SoundManager.SFX_PICKUP)
          } else {
            var i = 0
            while (i < 3) {
              if (continueButtons[i].contains(x, y)) {
                stageManager.saveContinueSetting(context, i)
                SoundManager.instance.playSFX(SoundManager.SFX_PICKUP)
                break
              }
              i++
            }
          }
        }
        return true
      }
      STATE_DIFFICULTY_SELECT -> {
        if (down) {
          val x = event.x
          val y = event.y
          if (diffBackButtonRect.contains(x, y)) {
            attractCycleTimer = 0f
            attractCycleState = ATTRACT_TITLE
            gameState = STATE_TITLE
            SoundManager.instance.playSFX(SoundManager.SFX_PICKUP)
          } else {
            var i = 0
            while (i < 7) {
              if (difficultyButtons[i].contains(x, y)) {
                stageManager.saveDifficultySetting(context, difficultyTiers[i])
                SoundManager.instance.playSFX(SoundManager.SFX_PICKUP)
                break
              }
              i++
            }
          }
        }
        return true
      }
      STATE_DEMO -> {
        return true
      }
      STATE_INTERSTITIAL -> {
        return true
      }
      STATE_CLEAR -> {
        val up = event.actionMasked == MotionEvent.ACTION_UP ||
          event.actionMasked == MotionEvent.ACTION_POINTER_UP
        if (ScoreManager.instance.isRecapReady() && up) {
          ScoreManager.instance.resetStageCounters()
          stageManager.advanceToNextStage()
          if (stageManager.isCampaignFinished) {
            campaignCompleteT = 0f
            lastBgmRes = 0
            SoundManager.instance.stopAlarm()
            gameState = STATE_CAMPAIGN_COMPLETE
          } else {
            ScoreManager.instance.syncDifficultyMultiplier(stageManager.getDifficulty().index)
            resetStage()
            interstitialTimer = INTERSTITIAL_SECS
            gameState = STATE_INTERSTITIAL
          }
        }
        return true
      }
      STATE_GAMEOVER -> {
        if (down) routeAfterGameOver()
        return true
      }
      STATE_CONTINUE -> {
        if (down) acceptContinueCredit()
        return true
      }
      STATE_REGISTRATION -> {
        if (down) {
          layoutRegistrationHitZones()
          val x = event.x
          val y = event.y
          if (registrationSetRect.contains(x, y)) {
            confirmRegistrationLetter()
          } else if (registrationLeftWing.contains(x, y)) {
            bumpRegistrationChar(false)
          } else if (registrationRightWing.contains(x, y)) {
            bumpRegistrationChar(true)
          }
        }
        return true
      }
      STATE_CAMPAIGN_COMPLETE -> {
        if (down) {
          if (qualifiesForRanking()) {
            beginRegistration()
          } else {
            finishDemoToHighScore()
          }
        }
        return true
      }
    }
    if (player.getHealth() <= 0) return true
    when (event.actionMasked) {
      MotionEvent.ACTION_DOWN -> {
        val now = event.eventTime
        if (
          awaitingSecondTap &&
          now - lastTapUpMs <= DOUBLE_TAP_MS &&
          availableBombs > 0 &&
          !player.isGameOver()
        ) {
          if (!panicBomb.isActive) {
            availableBombs--
            panicBomb.activate(player.getHitboxX(), player.getHitboxY())
            ScoreManager.instance.markBombUsed()
            bombCoreWasOpen = boss.isCoreVulnerable()
            bossBombDmgBank = 0f
            addScreenShake(0.8f)
            SoundManager.instance.playSFX(SoundManager.SFX_BOMB)
          }
          awaitingSecondTap = false
        }
        touchDownMs = now
        touchDownX = event.x
        touchDownY = event.y
        dragPointerId = event.getPointerId(0)
        tryGrabShip(event.x, event.y, dragPointerId)
      }
      MotionEvent.ACTION_POINTER_DOWN -> {
        val now = event.eventTime
        if (
          awaitingSecondTap &&
          now - lastTapUpMs <= DOUBLE_TAP_MS &&
          availableBombs > 0 &&
          !player.isGameOver()
        ) {
          if (!panicBomb.isActive) {
            availableBombs--
            panicBomb.activate(player.getHitboxX(), player.getHitboxY())
            ScoreManager.instance.markBombUsed()
            bombCoreWasOpen = boss.isCoreVulnerable()
            bossBombDmgBank = 0f
            addScreenShake(0.8f)
            SoundManager.instance.playSFX(SoundManager.SFX_BOMB)
          }
          awaitingSecondTap = false
        }
        touchDownMs = now
        val pIndex = event.actionIndex
        touchDownX = event.getX(pIndex)
        touchDownY = event.getY(pIndex)
        tryGrabShip(touchDownX, touchDownY, event.getPointerId(pIndex))
      }
      MotionEvent.ACTION_MOVE -> {
        if (isDraggingShip) {
          val index = if (dragPointerId >= 0) event.findPointerIndex(dragPointerId) else 0
          if (index >= 0) {
            steerFingerX = event.getX(index)
            steerFingerY = event.getY(index)
          } else {
            steerFingerX = event.x
            steerFingerY = event.y
          }
        }
      }
      MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_OUTSIDE -> {
        val dx = event.x - touchDownX
        val dy = event.y - touchDownY
        val dur = event.eventTime - touchDownMs
        awaitingSecondTap = (dx * dx + dy * dy) <= TAP_SLOP_SQ && dur <= TAP_MAX_MS
        lastTapUpMs = event.eventTime
        isDraggingShip = false
        dragPointerId = -1
      }
      MotionEvent.ACTION_POINTER_UP -> {
        val dx = event.x - touchDownX
        val dy = event.y - touchDownY
        val dur = event.eventTime - touchDownMs
        awaitingSecondTap = (dx * dx + dy * dy) <= TAP_SLOP_SQ && dur <= TAP_MAX_MS
        lastTapUpMs = event.eventTime
        if (event.getPointerId(event.actionIndex) == dragPointerId) {
          isDraggingShip = false
          dragPointerId = -1
        }
      }
    }
    return player.onTouch(event)
  }

  private fun syncBgm() {
    val want = when (gameState) {
      STATE_TITLE, STATE_DIFFICULTY_SELECT, STATE_CHARACTER_SELECT, STATE_CONTINUE_SELECT -> R.raw.bgm_title
      STATE_CLEAR, STATE_REGISTRATION, STATE_CAMPAIGN_COMPLETE -> R.raw.bgm_victory
      STATE_PLAYING, STATE_DEMO, STATE_INTERSTITIAL, STATE_CONTINUE -> {
        if (boss.isVictorySequence()) {
          0
        } else if (boss.isActive()) {
          if (boss.isCoreVulnerable()) {
            R.raw.bgm_boss2
          } else {
            R.raw.bgm_boss
          }
        } else {
          stageManager.stageMusicTrack
        }
      }
      else -> 0
    }
    if (want == 0) {
      if (lastBgmRes != 0) {
        SoundManager.instance.stopBGM()
        lastBgmRes = 0
      }
      return
    }
    if (want == lastBgmRes && SoundManager.instance.isBgmPlaying(want)) return
    if (SoundManager.instance.switchBGM(want)) {
      lastBgmRes = want
    } else {
      lastBgmRes = 0
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

  private fun hudPx(designPx: Float): Float = designPx * hudScale

  private fun applyHudTypeSizes() {
    uiTextPaint.textSize = hudPx(32f)
    uiShadowPaint.textSize = hudPx(32f)
    uiGoldPaint.textSize = hudPx(42f)
    uiGoldShadowPaint.textSize = hudPx(42f)
    uiSmallPaint.textSize = hudPx(20f)
    uiSmallShadowPaint.textSize = hudPx(20f)
    uiRegRedPaint.textSize = hudPx(42f)
    accentShadowPaint.textSize = hudPx(32f)
    popupPaint.textSize = hudPx(28f)
    popupShadowPaint.textSize = hudPx(28f)
  }

  private companion object {
    const val HUD_DESIGN_WIDTH = 1080f
    const val STATE_TITLE = 0
    const val STATE_PLAYING = 1
    const val STATE_CLEAR = 2
    const val STATE_GAMEOVER = 3
    const val STATE_DEMO = 4
    const val SCREEN_REGISTRATION = 5
    const val STATE_REGISTRATION = SCREEN_REGISTRATION
    const val STATE_CAMPAIGN_COMPLETE = 6
    const val STATE_INTERSTITIAL = 7
    const val STATE_DIFFICULTY_SELECT = 8
    const val STATE_CHARACTER_SELECT = 9
    const val STATE_CONTINUE_SELECT = 10
    const val STATE_CONTINUE = 11
    const val INTERSTITIAL_SECS = 3.0f
    const val ATTRACT_TITLE = 0
    const val ATTRACT_CPU_DEMO = 1
    const val ATTRACT_HIGH_SCORE = 2
    const val ATTRACT_TITLE_SECS = 4f
    const val ATTRACT_DEMO_SECS = 30f
    const val ATTRACT_HIGH_SCORE_SECS = 4f
    const val GAMEOVER_SECS = 9f
    const val CONTINUE_SECS = 9f
    const val TITLE_SCROLL_PX = 50f
    const val MAX_FRAME_NS = 50_000_000L
    const val PLAYER_HIT_RADIUS = 12f
    const val SHOT_HIT_PAD = 10f
    const val SHOT_HIT_BODY_FRAC = 0.55f
    const val SHOT_HIT_PAD_POPCORN = 3f
    const val SHOT_HIT_BODY_FRAC_POPCORN = 0.28f
    const val BOSS_BULLET_PAD_X = 6f
    const val BOSS_BULLET_PAD_Y = 16f
    const val ENEMY_RAM_BODY_FRAC = 0.45f
    const val MEDAL_SCORE_FACE = 2000
    const val MEDAL_SCORE_EDGE = 200
    const val KILL_SCORE_DRONE = 100
    const val KILL_SCORE_INTERCEPTOR = 300
    const val KILL_SCORE_HEAVY = 1000
    const val FLOATING_SPEED = 90f
    const val FLOATING_LIFE = 0.75f
    const val SCORE_POPUP_SLOTS = 48
    const val BOMB_ENEMY_DPS = 250f
    const val BOMB_BOSS_DPS = 200f
    const val BOMB_BOSS_DPS_FRAME_CAP = 12
    const val ENEMY_TYPE_INTERCEPTOR = 2
    const val ENEMY_TYPE_HEAVY = 3
    const val REVENGE_SHOT_SPEED = 550f
    const val REVENGE_SPREAD_RAD = 0.18f
    const val MAX_BOMBS = 3
    const val START_BOMBS = 2
    const val BOMB_FULL_SCORE = 5000
    const val POWERUP_FULL_SCORE = 2000
    const val RESPAWN_POWERUP_LIFT = 160f
    const val RESPAWN_POWERUP_MIN_Y = 96f
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
