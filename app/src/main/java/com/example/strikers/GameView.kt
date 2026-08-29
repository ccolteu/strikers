package com.example.strikers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
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
  private val enemies = EnemyPoolManager(resources)
  private val enemyShots = EnemyWeaponSystem()
  private val timeline = SpawnTimeline()
  private val particles = ParticleManager(resources)
  private val boss = BossController(resources)
  private val scorecard = VictoryScorecard()
  private val panicBomb = PanicBomb()
  private val srcCore = Rect()
  private val bombDstRect = RectF()
  private val hudIconDst = RectF()
  private val bodyPaint = Paint().apply { isFilterBitmap = true }
  private val bombSheets = arrayOfNulls<Bitmap>(6)
  private var lifeIconBmp: Bitmap? = null
  private var bombIconBmp: Bitmap? = null
  private var availableBombs = 3
  private var isGameOver = false
  private var lastTapUpMs = 0L
  private var touchDownMs = 0L
  private var touchDownX = 0f
  private var touchDownY = 0f
  private var awaitingSecondTap = false
  private var bossBombDmgBank = 0f
  private val choreographer = Choreographer.getInstance()
  private var running = false
  private var lastNanos = 0L
  private var screenW = 0
  private var screenH = 0
  private val uiTextPaint = Paint().apply {
    color = Color.WHITE
    typeface = Typeface.DEFAULT_BOLD
    textSize = 54f
    isAntiAlias = true
  }
  private val uiShadowPaint = Paint().apply {
    color = Color.BLACK
    typeface = Typeface.DEFAULT_BOLD
    textSize = 54f
    isAntiAlias = true
  }
  private val uiGoldPaint = Paint().apply {
    color = Color.YELLOW
    typeface = Typeface.DEFAULT_BOLD
    textSize = 68f
    isAntiAlias = true
  }
  private val uiGoldShadowPaint = Paint().apply {
    color = Color.BLACK
    typeface = Typeface.DEFAULT_BOLD
    textSize = 68f
    isAntiAlias = true
  }
  private val uiStringBuilder = StringBuilder(64)

  init {
    holder.addCallback(this)
    isFocusable = true
    isFocusableInTouchMode = true
    setWillNotDraw(true)
  }

  override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
    super.onSizeChanged(w, h, oldw, oldh)
    parallax.onSizeChanged(w, h)
    player.onSizeChanged(w, h)
    enemies.onSizeChanged(w, h)
    enemyShots.onSizeChanged(w, h)
    particles.onSizeChanged(w, h)
    boss.onSizeChanged(w, h)
    screenW = w
    screenH = h
    loadBombSheetsIfNeeded()
    loadHudIconsIfNeeded()
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
    particles.onSizeChanged(width, height)
    boss.onSizeChanged(width, height)
    screenW = width
    screenH = height
    loadBombSheetsIfNeeded()
    loadHudIconsIfNeeded()
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
    val simDt = if (isGameOver) 0f else dt
    parallax.update(GROUND_PX_PER_SEC * simDt)
    if (!isGameOver) {
      scorecard.update(dt)
    }
    if (!scorecard.isActive) {
      player.update(simDt)
      bullets.update(simDt, player)
      timeline.update(simDt, enemies, screenW, screenH, boss)
      enemies.update(simDt, player.getHitboxX(), player.getHitboxY(), enemyShots)
      boss.update(simDt, player.getHitboxX(), player.getHitboxY(), enemyShots)
      enemyShots.update(simDt)
      updatePanicBomb(simDt)
    }
    particles.update(simDt)
    if (!scorecard.isActive && !isGameOver) {
      resolveCollisions()
    }
    val canvas = lockGameCanvas()
    if (canvas != null) {
      try {
        parallax.draw(canvas)
        enemies.draw(canvas)
        boss.draw(canvas)
        player.draw(canvas)
        bullets.draw(canvas)
        enemyShots.draw(canvas)
        particles.draw(canvas)
        drawPanicBomb(canvas)
        drawArcadeUI(canvas)
      } finally {
        holder.unlockCanvasAndPost(canvas)
      }
    }
    choreographer.postFrameCallback(this)
  }

  /**
   * GPU-backed canvas on API 26+ ([SurfaceHolder.lockHardwareCanvas]).
   * The platform method landed in Oreo; pre-O devices fall back to software
   * [SurfaceHolder.lockCanvas].
   */
  private fun drawArcadeUI(canvas: Canvas) {
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
        canvas.drawBitmap(lifeBmp, null, hudIconDst, bodyPaint)
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
        canvas.drawBitmap(bombBmp, null, hudIconDst, bodyPaint)
        i++
      }
    }
    val topTextY = 80f
    uiStringBuilder.setLength(0)
    uiStringBuilder.append("1P-START")
    val startEnd = uiStringBuilder.length
    canvas.drawText(uiStringBuilder, 0, startEnd, 35f + 4f, topTextY + 4f, uiShadowPaint)
    canvas.drawText(uiStringBuilder, 0, startEnd, 30f, topTextY, uiTextPaint)
    uiStringBuilder.setLength(0)
    uiStringBuilder.append("00")
    val scoreEnd = uiStringBuilder.length
    val scoreW = uiTextPaint.measureText(uiStringBuilder, 0, scoreEnd)
    canvas.drawText(uiStringBuilder, 0, scoreEnd, screenW - scoreW - 35f + 4f, topTextY + 4f, uiShadowPaint)
    canvas.drawText(uiStringBuilder, 0, scoreEnd, screenW - scoreW - 30f, topTextY, uiTextPaint)
    if (isGameOver) {
      val goCx = screenW * 0.5f
      uiStringBuilder.setLength(0)
      uiStringBuilder.append("GAME OVER")
      drawCenteredHud(canvas, uiStringBuilder, goCx, screenH * 0.45f, uiGoldPaint, uiGoldShadowPaint)
      if ((System.currentTimeMillis() / 500L) % 2L == 0L) {
        uiStringBuilder.setLength(0)
        uiStringBuilder.append("TAP SCREEN TO RESTART")
        drawCenteredHud(canvas, uiStringBuilder, goCx, screenH * 0.55f, uiTextPaint, uiShadowPaint)
      }
      return
    }
    if (!scorecard.isActive) return
    val cx = screenW * 0.5f
    if (scorecard.currentDisplayLine >= 1) {
      uiStringBuilder.setLength(0)
      uiStringBuilder.append("STAGE 1 CLEAR")
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
      uiStringBuilder.append("TOTAL GAIN: ")
      uiStringBuilder.append(scorecard.visibleTotalScore)
      drawCenteredHud(canvas, uiStringBuilder, cx, screenH * 0.54f, uiGoldPaint, uiGoldShadowPaint)
    }
    if (scorecard.isCountingDone && ((scorecard.elapsedTime * 3f).toInt() and 1) == 0) {
      uiStringBuilder.setLength(0)
      uiStringBuilder.append("TOUCH SCREEN TO CONTINUE")
      drawCenteredHud(canvas, uiStringBuilder, cx, screenH * 0.82f, uiTextPaint, uiShadowPaint)
    }
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
    canvas.drawText(text, 0, end, x + 6f, y + 6f, shadow)
    canvas.drawText(text, 0, end, x, y, fill)
  }

  private fun updatePanicBomb(dt: Float) {
    if (!panicBomb.isActive) return
    panicBomb.currentFrameTime += dt
    if (panicBomb.currentFrameTime >= PanicBomb.FRAME_DURATION) {
      panicBomb.currentFrameTime = 0f
      panicBomb.currentFrameIndex++
      if (panicBomb.currentFrameIndex > 5) {
        panicBomb.isActive = false
        bossBombDmgBank = 0f
        return
      }
    }
    val currentProgress = (panicBomb.currentFrameIndex + 1).toFloat() / PanicBomb.FRAME_COUNT.toFloat()
    val maxClearRadius = screenH * 0.5f
    val rSq = (maxClearRadius * currentProgress) * (maxClearRadius * currentProgress)
    val bx = panicBomb.x
    val by = panicBomb.y

    val shotPool = enemyShots.getBulletPool()
    val shotCount = enemyShots.getPoolSize()
    var si = 0
    while (si < shotCount) {
      val shot = shotPool[si]
      if (shot.isActive) {
        val dx = shot.x - bx
        val dy = shot.y - by
        if ((dx * dx) + (dy * dy) <= rSq) {
          shot.isActive = false
        }
      }
      si++
    }

    val enemyPool = enemies.getEnemyPool()
    val enemyCount = enemies.getPoolSize()
    var ei = 0
    while (ei < enemyCount) {
      val enemy = enemyPool[ei]
      if (enemy.isActive) {
        val dx = enemy.x - bx
        val dy = enemy.y - by
        if ((dx * dx) + (dy * dy) <= rSq) {
          enemy.isActive = false
          particles.triggerExplosion(enemy.x, enemy.y)
        }
      }
      ei++
    }

    if (boss.isActive()) {
      bossBombDmgBank += BOSS_BOMB_DPS * dt
      val dmg = bossBombDmgBank.toInt()
      if (dmg > 0) {
        bossBombDmgBank -= dmg
        val parts = boss.getComponents()
        val partCount = boss.getComponentCount()
        var pi = 0
        while (pi < partCount) {
          val part = parts[pi]
          if (!part.isDestroyed) {
            val dx = part.x - bx
            val dy = part.y - by
            if ((dx * dx) + (dy * dy) <= rSq) {
              part.health -= dmg
              if (part.health <= 0) {
                part.health = 0
                part.isDestroyed = true
                particles.triggerExplosion(part.x, part.y)
                if (part.componentType == BossController.TYPE_CORE && !scorecard.isActive) {
                  scorecard.trigger(player.getHealth(), availableBombs)
                }
              }
            }
          }
          pi++
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
      isGameOver = true
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
              enemy.isActive = false
              particles.triggerExplosion(enemy.x, enemy.y)
              break
            }
          }
          ei++
        }
      }
      bi++
    }

    if (boss.isActive()) {
      val parts = boss.getComponents()
      val partCount = boss.getComponentCount()
      bi = 0
      while (bi < bulletCount) {
        val bullet = bulletPool[bi]
        if (bullet.isActive) {
          var pi = 0
          while (pi < partCount) {
            val part = parts[pi]
            if (!part.isDestroyed && part.halfW > 0f && part.halfH > 0f) {
              val dx = (bullet.x - part.x) / part.halfW
              val dy = (bullet.y - part.y) / part.halfH
              if ((dx * dx) + (dy * dy) <= 1f) {
                bullet.isActive = false
                part.health -= 1
                if (part.health <= 0) {
                  part.health = 0
                  part.isDestroyed = true
                  particles.triggerExplosion(part.x, part.y)
                  if (part.componentType == BossController.TYPE_CORE && !scorecard.isActive) {
                    scorecard.trigger(player.getHealth(), availableBombs)
                  }
                }
                break
              }
            }
            pi++
          }
        }
        bi++
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
          player.takeDamage()
          if (player.getHealth() <= 0) isGameOver = true
          break
        }
      }
      i++
    }
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
    super.onDetachedFromWindow()
  }

  private fun resetStage() {
    isGameOver = false
    scorecard.isActive = false
    scorecard.isCountingDone = false
    scorecard.currentDisplayLine = 0
    availableBombs = 3
    panicBomb.isActive = false
    bossBombDmgBank = 0f
    awaitingSecondTap = false
    player.resetForStage()
    bullets.deactivateAll()
    enemies.deactivateAll()
    enemyShots.deactivateAll()
    boss.deactivate()
    timeline.reset()
  }

  override fun onTouchEvent(event: MotionEvent): Boolean {
    if (scorecard.isActive) {
      if (
        scorecard.isCountingDone &&
        (event.actionMasked == MotionEvent.ACTION_DOWN || event.actionMasked == MotionEvent.ACTION_POINTER_DOWN)
      ) {
        resetStage()
      }
      return true
    }
    if (isGameOver) {
      if (event.actionMasked == MotionEvent.ACTION_DOWN || event.actionMasked == MotionEvent.ACTION_POINTER_DOWN) {
        resetStage()
      }
      return true
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

  private companion object {
    const val GROUND_PX_PER_SEC = 140f
    const val MAX_FRAME_NS = 50_000_000L
    const val RADIUS_SUM_THRESHOLD = 28f
    const val PLAYER_HIT_RADIUS = 12f
    const val BOSS_BOMB_DPS = 28f
    const val DOUBLE_TAP_MS = 280L
    const val TAP_MAX_MS = 220L
    const val TAP_SLOP_SQ = 48f * 48f
  }
}
