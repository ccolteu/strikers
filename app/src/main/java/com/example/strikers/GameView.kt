package com.example.strikers

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
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
  private val choreographer = Choreographer.getInstance()
  private var running = false
  private var lastNanos = 0L
  private var screenW = 0
  private var screenH = 0
  private val uiTextPaint = Paint().apply {
    color = Color.WHITE
    isFakeBoldText = true
    typeface = Typeface.MONOSPACE
    textSize = 52f
    isAntiAlias = true
  }
  private val uiShadowPaint = Paint().apply {
    color = Color.BLACK
    isFakeBoldText = true
    typeface = Typeface.MONOSPACE
    textSize = 52f
    isAntiAlias = true
  }
  private val uiStringBuilder = StringBuilder(32)

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
    screenW = w
    screenH = h
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
    screenW = width
    screenH = height
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
    parallax.update(GROUND_PX_PER_SEC * dt)
    player.update(dt)
    bullets.update(dt, player)
    timeline.update(dt, enemies, screenW, screenH)
    enemies.update(dt, player.getHitboxX(), player.getHitboxY(), enemyShots)
    enemyShots.update(dt)
    particles.update(dt)
    resolveCollisions()
    val canvas = lockGameCanvas()
    if (canvas != null) {
      try {
        parallax.draw(canvas)
        enemies.draw(canvas)
        player.draw(canvas)
        bullets.draw(canvas)
        enemyShots.draw(canvas)
        particles.draw(canvas)
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
    uiStringBuilder.setLength(0)
    uiStringBuilder.append("1P LIFE: ")
    val currentLives = player.getHealth()
    var i = 0
    while (i < currentLives) {
      uiStringBuilder.append('★')
      i++
    }
    val end = uiStringBuilder.length
    canvas.drawText(uiStringBuilder, 0, end, 44f, 84f, uiShadowPaint)
    canvas.drawText(uiStringBuilder, 0, end, 40f, 80f, uiTextPaint)
  }

  private fun resolveCollisions() {
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
    super.onDetachedFromWindow()
  }

  override fun onTouchEvent(event: MotionEvent): Boolean {
    return player.onTouch(event)
  }

  private companion object {
    const val GROUND_PX_PER_SEC = 140f
    const val MAX_FRAME_NS = 50_000_000L
    const val RADIUS_SUM_THRESHOLD = 28f
    const val PLAYER_HIT_RADIUS = 12f
  }
}
