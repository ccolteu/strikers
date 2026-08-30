package com.example.strikers

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF

class EnemyWeaponSystem {

  private val pool = Array(POOL_SIZE) { EnemyBullet() }
  private val bulletPaint = Paint().apply {
    color = 0xFF00FF66.toInt()
    style = Paint.Style.FILL
    isAntiAlias = false
  }
  private val innerGlowPaint = Paint().apply {
    color = Color.WHITE
    style = Paint.Style.FILL
    isAntiAlias = false
  }
  private val drawRect = RectF()
  private var screenW = 0f
  private var screenH = 0f

  fun onSizeChanged(width: Int, height: Int) {
    screenW = width.toFloat()
    screenH = height.toFloat()
  }

  fun getBulletPool(): Array<EnemyBullet> = pool

  fun getPoolSize(): Int = POOL_SIZE

  fun deactivateAll() {
    var i = 0
    while (i < POOL_SIZE) {
      pool[i].isActive = false
      i++
    }
  }

  fun fireBullet(startX: Float, startY: Float, velX: Float, velY: Float) {
    var i = 0
    while (i < POOL_SIZE) {
      val b = pool[i]
      if (!b.isActive) {
        b.x = startX
        b.y = startY
        b.vx = velX
        b.vy = velY
        b.isActive = true
        return
      }
      i++
    }
  }

  fun update(dt: Float) {
    val w = screenW
    val h = screenH
    val r = HALF_BULLET_WIDTH
    var i = 0
    while (i < POOL_SIZE) {
      val b = pool[i]
      if (b.isActive) {
        b.x += b.vx * dt
        b.y += b.vy * dt
        if (b.x + r < 0f || b.x - r > w || b.y + r < 0f || b.y - r > h) {
          b.isActive = false
        }
      }
      i++
    }
  }

  fun draw(canvas: Canvas) {
    val hw = HALF_BULLET_WIDTH
    val hh = HALF_BULLET_HEIGHT
    var i = 0
    while (i < POOL_SIZE) {
      val b = pool[i]
      if (b.isActive) {
        drawRect.set(b.x - hw, b.y - hh, b.x + hw, b.y + hh)
        canvas.drawOval(drawRect, bulletPaint)
        drawRect.set(b.x - hw + 5f, b.y - hh + 5f, b.x + hw - 5f, b.y + hh - 5f)
        canvas.drawOval(drawRect, innerGlowPaint)
      }
      i++
    }
  }

  companion object {
    const val POOL_SIZE = 220
    const val HALF_BULLET_WIDTH = 18f
    const val HALF_BULLET_HEIGHT = 18f
  }
}
