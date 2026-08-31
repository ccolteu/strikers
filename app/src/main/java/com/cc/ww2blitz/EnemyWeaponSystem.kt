package com.cc.ww2blitz

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
  private val clearX = FloatArray(CLEAR_SLOTS)
  private val clearY = FloatArray(CLEAR_SLOTS)
  private val clearT = FloatArray(CLEAR_SLOTS)

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
      pool[i].flags = 0
      i++
    }
    i = 0
    while (i < CLEAR_SLOTS) {
      clearT[i] = 0f
      i++
    }
  }

  fun beginDeathClear(originX: Float, originY: Float) {
    var i = 0
    while (i < CLEAR_SLOTS) {
      if (clearT[i] <= 0f) {
        clearX[i] = originX
        clearY[i] = originY
        clearT[i] = CLEAR_LIFE
        return
      }
      i++
    }
    clearX[0] = originX
    clearY[0] = originY
    clearT[0] = CLEAR_LIFE
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
        b.flags = 0
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
          b.flags = 0
        }
      }
      i++
    }
    updateDeathClears(dt)
  }

  private fun updateDeathClears(dt: Float) {
    var i = 0
    while (i < CLEAR_SLOTS) {
      if (clearT[i] > 0f) {
        clearT[i] -= dt
        val age = CLEAR_LIFE - clearT[i]
        var u = age / CLEAR_LIFE
        if (u < 0f) u = 0f
        if (u > 1f) u = 1f
        val radius = CLEAR_RADIUS * u
        val rSq = radius * radius
        val ox = clearX[i]
        val oy = clearY[i]
        var bi = 0
        while (bi < POOL_SIZE) {
          val b = pool[bi]
          if (b.isActive) {
            val dx = b.x - ox
            val dy = b.y - oy
            if ((dx * dx) + (dy * dy) <= rSq) {
              b.isActive = false
              b.flags = 0
            }
          }
          bi++
        }
        if (clearT[i] < 0f) clearT[i] = 0f
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
    const val CLEAR_SLOTS = 4
    const val CLEAR_RADIUS = 150f
    const val CLEAR_LIFE = 0.15f
  }
}
