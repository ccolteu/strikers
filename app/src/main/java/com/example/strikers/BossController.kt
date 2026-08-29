package com.example.strikers

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class BossController(private val resources: Resources) {

  private val parts = Array(PART_COUNT) { BossComponent() }
  private val dstRect = RectF()
  private val srcCore = Rect()
  private val bodyPaint = Paint().apply { isFilterBitmap = true }
  private var bodySheet: Bitmap? = null
  private var leftWreckSheet: Bitmap? = null
  private var rightWreckSheet: Bitmap? = null
  private var screenW = 0f
  private var screenH = 0f
  private var coreX = 0f
  private var coreY = 0f
  private var hoverY = 0f
  private var sweepPhase = 0f
  private var turretAngle = 0f
  private var fireTimer = 0f
  private var entering = false
  private var active = false
  private var bodyHalfW = 0f
  private var bodyHalfH = 0f

  fun onSizeChanged(width: Int, height: Int) {
    screenW = width.toFloat()
    screenH = height.toFloat()
    hoverY = height * HOVER_Y_FRAC
    if (bodySheet == null) {
      loadKeyedSheet()
    }
    cacheSrcRects()
    val cellW = srcCore.width().coerceAtLeast(1)
    val cellH = srcCore.height().coerceAtLeast(1)
    val targetW = (width * CORE_WIDTH_FRAC).toInt()
    val targetH = (targetW * (cellH.toFloat() / cellW.toFloat())).toInt()
    bodyHalfW = targetW * 0.5f
    bodyHalfH = targetH * 0.5f
    layoutOffsets()
  }

  fun beginEntrance() {
    if (screenW <= 0f) return
    active = true
    entering = true
    sweepPhase = 0f
    fireTimer = FIRE_INTERVAL
    turretAngle = 1.5707964f
    coreX = screenW * 0.5f
    coreY = -bodyHalfH - 24f
    var i = 0
    while (i < PART_COUNT) {
      val p = parts[i]
      p.isDestroyed = false
      p.health = p.maxHealth
      i++
    }
  }

  fun isActive(): Boolean = active

  fun deactivate() {
    active = false
    entering = false
  }

  fun getComponents(): Array<BossComponent> = parts

  fun getComponentCount(): Int = PART_COUNT

  fun update(dt: Float, playerX: Float, playerY: Float, weapons: EnemyWeaponSystem) {
    if (!active) return
    if (entering) {
      coreY += ENTER_SPEED * dt
      if (coreY >= hoverY) {
        coreY = hoverY
        entering = false
      }
    } else {
      sweepPhase += dt * SWEEP_RATE
      coreX = screenW * 0.5f + sin(sweepPhase) * screenW * SWEEP_AMP
    }
    val core = parts[TYPE_CORE]
    if (core.isDestroyed) {
      active = false
      return
    }
    var i = 0
    while (i < PART_COUNT) {
      val p = parts[i]
      p.x = coreX + p.relOffsetX
      p.y = coreY + p.relOffsetY
      i++
    }
    val turret = parts[TYPE_TURRET]
    if (!turret.isDestroyed) {
      turretAngle = atan2(playerY - turret.y, playerX - turret.x)
      if (!entering) {
        fireTimer -= dt
        if (fireTimer <= 0f) {
          val muzzleOffsetX = cos(turretAngle) * turret.halfH
          val muzzleOffsetY = sin(turretAngle) * turret.halfH
          fireRing(weapons, turret.x + muzzleOffsetX, turret.y + muzzleOffsetY)
          fireTimer = FIRE_INTERVAL
        }
      }
    }
  }

  fun draw(canvas: Canvas) {
    if (!active) return
    val sheet = bodySheet ?: return
    val core = parts[TYPE_CORE]
    if (core.isDestroyed) return
    dstRect.set(coreX - bodyHalfW, coreY - bodyHalfH, coreX + bodyHalfW, coreY + bodyHalfH)
    canvas.drawBitmap(sheet, srcCore, dstRect, bodyPaint)
    val leftWreck = leftWreckSheet
    if (leftWreck != null && parts[TYPE_LEFT_WING].isDestroyed) {
      canvas.drawBitmap(leftWreck, srcCore, dstRect, bodyPaint)
    }
    val rightWreck = rightWreckSheet
    if (rightWreck != null && parts[TYPE_RIGHT_WING].isDestroyed) {
      canvas.drawBitmap(rightWreck, srcCore, dstRect, bodyPaint)
    }
  }

  fun release() {
    recycle(bodySheet)
    recycle(leftWreckSheet)
    recycle(rightWreckSheet)
    bodySheet = null
    leftWreckSheet = null
    rightWreckSheet = null
  }

  private fun fireRing(weapons: EnemyWeaponSystem, originX: Float, originY: Float) {
    var i = 0
    while (i < RING_COUNT) {
      val ang = turretAngle + i * RING_STEP
      weapons.fireBullet(originX, originY, cos(ang) * RING_SPEED, sin(ang) * RING_SPEED)
      i++
    }
  }

  private fun cacheSrcRects() {
    val sheet = bodySheet ?: return
    srcCore.set(0, 0, sheet.width, sheet.height)
  }

  private fun layoutOffsets() {
    val coreW = bodyHalfW * 0.28f
    val coreH = bodyHalfH * 0.52f
    val wingW = bodyHalfW * 0.30f
    val wingH = bodyHalfH * 0.20f
    val turretSize = bodyHalfW * 0.12f
    setupPart(TYPE_CORE, 0f, 0f, coreW, coreH, CORE_HP)
    setupPart(TYPE_LEFT_WING, -bodyHalfW * 0.5f, -bodyHalfH * 0.08f, wingW, wingH, WING_HP)
    setupPart(TYPE_RIGHT_WING, bodyHalfW * 0.5f, -bodyHalfH * 0.08f, wingW, wingH, WING_HP)
    setupPart(TYPE_TURRET, 0f, bodyHalfH * 0.46f, turretSize, turretSize, TURRET_HP)
  }

  private fun setupPart(type: Int, ox: Float, oy: Float, hw: Float, hh: Float, hp: Int) {
    val p = parts[type]
    p.componentType = type
    p.relOffsetX = ox
    p.relOffsetY = oy
    p.halfW = hw
    p.halfH = hh
    p.maxHealth = hp
    p.health = hp
    p.isDestroyed = false
  }

  private fun loadKeyedSheet() {
    bodySheet = decodeKeyed(R.drawable.boss_stage1_full)
    leftWreckSheet = decodeKeyed(R.drawable.boss_stage1_wreck_left)
    rightWreckSheet = decodeKeyed(R.drawable.boss_stage1_wreck_right)
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

  private fun recycle(bmp: Bitmap?) {
    if (bmp != null && !bmp.isRecycled) bmp.recycle()
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

  companion object {
    const val TYPE_CORE = 0
    const val TYPE_LEFT_WING = 1
    const val TYPE_RIGHT_WING = 2
    const val TYPE_TURRET = 3
    const val PART_COUNT = 4
    const val HOVER_Y_FRAC = 0.25f
    const val CORE_WIDTH_FRAC = 0.85f
    const val ENTER_SPEED = 90f
    const val SWEEP_RATE = 0.55f
    const val SWEEP_AMP = 0.18f
    const val FIRE_INTERVAL = 1.5f
    const val RING_COUNT = 5
    const val RING_STEP = (Math.PI * 2.0 / RING_COUNT).toFloat()
    const val RING_SPEED = 420f
    const val CORE_HP = 60
    const val WING_HP = 25
    const val TURRET_HP = 20
  }
}
