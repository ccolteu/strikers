package com.example.strikers

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF

class EnemyPoolManager(private val resources: Resources) {

  private val pool = Array(POOL_SIZE) { Enemy() }
  private val lock = Any()
  private val paint = Paint().apply {
    isFilterBitmap = true
    isAntiAlias = false
  }
  private val drawRect = RectF()
  private var droneSheet: Bitmap? = null
  private var screenW = 0f
  private var screenH = 0f
  private var halfW = 0f
  private var halfH = 0f
  private var rng = 2463534242

  fun onSizeChanged(width: Int, height: Int) {
    screenW = width.toFloat()
    screenH = height.toFloat()
    if (droneSheet == null) {
      droneSheet = loadKeyedDrone()
    }
    val sheet = droneSheet ?: return
    val targetDrawW = (width * ENEMY_WIDTH_FRAC).toInt().coerceAtLeast(1)
    val aspectRatio = sheet.height.toFloat() / sheet.width.toFloat()
    val targetDrawH = (targetDrawW * aspectRatio).toInt().coerceAtLeast(1)
    halfW = targetDrawW * 0.5f
    halfH = targetDrawH * 0.5f
  }

  fun getEnemyPool(): Array<Enemy> = pool

  fun getPoolSize(): Int = POOL_SIZE

  fun spawnEnemy(startX: Float, startY: Float, velocityX: Float, velocityY: Float, enemyType: Int) {
    synchronized(lock) {
      for (i in 0 until POOL_SIZE) {
        val e = pool[i]
        if (e.isActive) continue
        e.x = startX
        e.y = startY
        e.vx = velocityX
        e.vy = velocityY
        e.type = enemyType
        e.fireTimer = FIRE_DELAY_MIN + nextUnit() * (FIRE_DELAY_MAX - FIRE_DELAY_MIN)
        e.isActive = true
        return
      }
    }
  }

  fun update(dt: Float, playerX: Float, playerY: Float, weapons: EnemyWeaponSystem) {
    synchronized(lock) {
      val w = screenW
      val h = screenH
      val hw = halfW
      val hh = halfH
      val speed = AIMED_SHOT_SPEED
      for (i in 0 until POOL_SIZE) {
        val e = pool[i]
        if (!e.isActive) continue
        e.x += e.vx * dt
        e.y += e.vy * dt
        if (e.y - hh > h || e.x - hw > w || (e.x + hw < 0f && e.vx <= 0f) || (e.y + hh < 0f && e.vy <= 0f)) {
          e.isActive = false
          continue
        }
        e.fireTimer -= dt
        if (e.fireTimer <= 0f) {
          val dx = playerX - e.x
          val dy = playerY - e.y
          val lenSq = dx * dx + dy * dy
          if (lenSq > 0.0001f) {
            val inv = speed / kotlin.math.sqrt(lenSq)
            weapons.fireBullet(e.x, e.y, dx * inv, dy * inv)
          }
          e.fireTimer = FIRE_ONCE_LOCK
        }
      }
    }
  }

  private fun nextUnit(): Float {
    rng = rng * 1664525 + 1013904223
    return ((rng ushr 8) and 0xFFFFFF) / 16777215f
  }

  fun draw(canvas: Canvas) {
    val sheet = droneSheet ?: return
    synchronized(lock) {
      val hw = halfW
      val hh = halfH
      for (i in 0 until POOL_SIZE) {
        val e = pool[i]
        if (!e.isActive) continue
        canvas.save()
        canvas.translate(e.x, e.y)
        canvas.rotate(180f)
        drawRect.set(-hw, -hh, hw, hh)
        canvas.drawBitmap(sheet, null, drawRect, paint)
        canvas.restore()
      }
    }
  }

  fun release() {
    val sheet = droneSheet
    if (sheet != null && !sheet.isRecycled) sheet.recycle()
    droneSheet = null
  }

  private fun loadKeyedDrone(): Bitmap {
    val opts = BitmapFactory.Options().apply {
      inScaled = false
      inPreferredConfig = Bitmap.Config.ARGB_8888
      inMutable = true
    }
    val src = BitmapFactory.decodeResource(resources, R.drawable.enemy_drone, opts)
      ?: error("Missing drawable enemy_drone")
    val bmp = if (src.isMutable) src else src.copy(Bitmap.Config.ARGB_8888, true).also { src.recycle() }
    keyGreen(bmp)
    return bmp
  }

  private fun keyGreen(bmp: Bitmap) {
    val w = bmp.width
    val h = bmp.height
    val row = IntArray(w)
    for (rowY in 0 until h) {
      bmp.getPixels(row, 0, w, 0, rowY, w, 1)
      for (i in 0 until w) {
        val c = row[i]
        val r = (c ushr 16) and 0xFF
        val g = (c ushr 8) and 0xFF
        val b = c and 0xFF
        if (g > 160 && g > r + 40 && g > b + 40) {
          row[i] = 0
        }
      }
      bmp.setPixels(row, 0, w, 0, rowY, w, 1)
    }
  }

  private companion object {
    const val POOL_SIZE = 30
    const val ENEMY_WIDTH_FRAC = 0.18f
    const val FIRE_DELAY_MIN = 0.5f
    const val FIRE_DELAY_MAX = 1.5f
    const val FIRE_ONCE_LOCK = 999f
    const val AIMED_SHOT_SPEED = 550f
  }
}
