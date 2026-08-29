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
import kotlin.math.sqrt

class BossController(private val resources: Resources) {

  private val parts = Array(MAX_PART_COUNT) { BossComponent() }
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
  private var barrelIndex = 0
  private var entering = false
  private var active = false
  private var currentStage = 1
  private var loadedStage = -1
  private var bodyHalfW = 0f
  private var bodyHalfH = 0f

  fun onSizeChanged(width: Int, height: Int) {
    screenW = width.toFloat()
    screenH = height.toFloat()
    hoverY = height * HOVER_Y_FRAC
    if (bodySheet == null) {
      loadKeyedSheet(currentStage)
    }
    cacheSrcRects()
    recomputeBodySize(width)
    layoutOffsets(currentStage)
  }

  fun bindStage(stage: Int) {
    currentStage = if (stage >= 2) 2 else 1
    if (screenW <= 0f) return
    loadKeyedSheet(currentStage)
    cacheSrcRects()
    recomputeBodySize(screenW.toInt())
    layoutOffsets(currentStage)
  }

  fun beginEntrance() {
    beginEntranceForStage(currentStage)
  }

  fun beginEntranceForStage(stage: Int) {
    if (screenW <= 0f) return
    bindStage(stage)
    active = true
    entering = true
    sweepPhase = 0f
    fireTimer = if (currentStage >= 2) TANK_FIRE_INTERVAL else FIRE_INTERVAL
    barrelIndex = 0
    turretAngle = 1.5707964f
    coreX = screenW * 0.5f
    coreY = -bodyHalfH - 24f
    var i = 0
    while (i < MAX_PART_COUNT) {
      val p = parts[i]
      if (p.halfW > 0f) {
        p.isDestroyed = false
        p.health = p.maxHealth
      }
      i++
    }
  }

  fun isActive(): Boolean = active

  fun deactivate() {
    active = false
    entering = false
  }

  fun getComponents(): Array<BossComponent> = parts

  fun getComponentCount(): Int = MAX_PART_COUNT

  fun update(dt: Float, playerX: Float, playerY: Float, weapons: EnemyWeaponSystem) {
    if (!active) return
    if (entering) {
      coreY += ENTER_SPEED * dt
      if (coreY >= hoverY) {
        coreY = hoverY
        entering = false
      }
    } else if (currentStage < 2) {
      sweepPhase += dt * SWEEP_RATE
      coreX = screenW * 0.5f + sin(sweepPhase) * screenW * SWEEP_AMP
    } else {
      coreX = screenW * 0.5f
    }
    val core = parts[TYPE_CORE]
    if (core.isDestroyed) {
      active = false
      return
    }
    var i = 0
    while (i < MAX_PART_COUNT) {
      val p = parts[i]
      p.x = coreX + p.relOffsetX
      p.y = coreY + p.relOffsetY
      i++
    }
    if (currentStage >= 2) {
      updateTankCombat(dt, playerX, playerY, weapons)
    } else {
      updatePlaneCombat(dt, playerX, playerY, weapons)
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
    val rightWreck = rightWreckSheet
    if (currentStage >= 2) {
      if (leftWreck != null && parts[TYPE_STAGE2_LEFT_TREAD].isDestroyed) {
        canvas.drawBitmap(leftWreck, srcCore, dstRect, bodyPaint)
      }
      if (rightWreck != null && parts[TYPE_STAGE2_RIGHT_TREAD].isDestroyed) {
        canvas.drawBitmap(rightWreck, srcCore, dstRect, bodyPaint)
      }
    } else {
      if (leftWreck != null && parts[TYPE_LEFT_WING].isDestroyed) {
        canvas.drawBitmap(leftWreck, srcCore, dstRect, bodyPaint)
      }
      if (rightWreck != null && parts[TYPE_RIGHT_WING].isDestroyed) {
        canvas.drawBitmap(rightWreck, srcCore, dstRect, bodyPaint)
      }
    }
  }

  fun release() {
    recycle(bodySheet)
    recycle(leftWreckSheet)
    recycle(rightWreckSheet)
    bodySheet = null
    leftWreckSheet = null
    rightWreckSheet = null
    loadedStage = -1
  }

  private fun updatePlaneCombat(dt: Float, playerX: Float, playerY: Float, weapons: EnemyWeaponSystem) {
    val turret = parts[TYPE_TURRET]
    if (turret.isDestroyed) return
    turretAngle = atan2(playerY - turret.y, playerX - turret.x)
    if (entering) return
    fireTimer -= dt
    if (fireTimer > 0f) return
    fireRing(weapons, turret.x + cos(turretAngle) * turret.halfH, turret.y + sin(turretAngle) * turret.halfH)
    fireTimer = FIRE_INTERVAL
  }

  private fun updateTankCombat(dt: Float, playerX: Float, playerY: Float, weapons: EnemyWeaponSystem) {
    val turret = parts[TYPE_STAGE2_MAIN_TURRET]
    if (turret.isDestroyed) return
    if (entering) return
    fireTimer -= dt
    if (fireTimer > 0f) return
    val sep = bodyHalfW * TANK_BARREL_SEP_FRAC
    val ox = if (barrelIndex == 0) turret.x - sep else turret.x + sep
    val oy = turret.y - turret.halfH
    fireDirect(weapons, ox, oy, playerX, playerY)
    barrelIndex = 1 - barrelIndex
    fireTimer = TANK_FIRE_INTERVAL
  }

  private fun fireDirect(
    weapons: EnemyWeaponSystem,
    originX: Float,
    originY: Float,
    playerX: Float,
    playerY: Float,
  ) {
    val dx = playerX - originX
    val dy = playerY - originY
    val lenSq = dx * dx + dy * dy
    if (lenSq < 0.0001f) return
    val inv = TANK_SHOT_SPEED / sqrt(lenSq)
    weapons.fireBullet(originX, originY, dx * inv, dy * inv)
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

  private fun recomputeBodySize(width: Int) {
    val cellW = srcCore.width().coerceAtLeast(1)
    val cellH = srcCore.height().coerceAtLeast(1)
    val targetW = (width * CORE_WIDTH_FRAC).toInt()
    val targetH = (targetW * (cellH.toFloat() / cellW.toFloat())).toInt()
    bodyHalfW = targetW * 0.5f
    bodyHalfH = targetH * 0.5f
  }

  private fun layoutOffsets(stage: Int) {
    disablePart(TYPE_CORE)
    disablePart(TYPE_LEFT_WING)
    disablePart(TYPE_RIGHT_WING)
    disablePart(TYPE_TURRET)
    disablePart(TYPE_STAGE2_LEFT_TREAD)
    disablePart(TYPE_STAGE2_RIGHT_TREAD)
    disablePart(TYPE_STAGE2_MAIN_TURRET)
    if (stage >= 2) {
      setupPart(TYPE_CORE, 0f, 0f, bodyHalfW * 0.34f, bodyHalfH * 0.40f, S2_CORE_HP)
      setupPart(TYPE_STAGE2_LEFT_TREAD, -bodyHalfW * 0.58f, 0f, bodyHalfW * 0.22f, bodyHalfH * 0.48f, S2_TREAD_HP)
      setupPart(TYPE_STAGE2_RIGHT_TREAD, bodyHalfW * 0.58f, 0f, bodyHalfW * 0.22f, bodyHalfH * 0.48f, S2_TREAD_HP)
      setupPart(TYPE_STAGE2_MAIN_TURRET, 0f, -bodyHalfH * 0.08f, bodyHalfW * 0.18f, bodyHalfH * 0.22f, S2_TURRET_HP)
    } else {
      setupPart(TYPE_CORE, 0f, 0f, bodyHalfW * 0.28f, bodyHalfH * 0.52f, S1_CORE_HP)
      setupPart(TYPE_LEFT_WING, -bodyHalfW * 0.5f, -bodyHalfH * 0.08f, bodyHalfW * 0.30f, bodyHalfH * 0.20f, S1_WING_HP)
      setupPart(TYPE_RIGHT_WING, bodyHalfW * 0.5f, -bodyHalfH * 0.08f, bodyHalfW * 0.30f, bodyHalfH * 0.20f, S1_WING_HP)
      setupPart(TYPE_TURRET, 0f, bodyHalfH * 0.46f, bodyHalfW * 0.12f, bodyHalfW * 0.12f, S1_TURRET_HP)
    }
  }

  private fun disablePart(type: Int) {
    val p = parts[type]
    p.componentType = type
    p.relOffsetX = 0f
    p.relOffsetY = 0f
    p.halfW = 0f
    p.halfH = 0f
    p.maxHealth = 0
    p.health = 0
    p.isDestroyed = true
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

  private fun loadKeyedSheet(stage: Int) {
    if (loadedStage == stage && bodySheet != null) return
    recycle(bodySheet)
    recycle(leftWreckSheet)
    recycle(rightWreckSheet)
    bodySheet = null
    leftWreckSheet = null
    rightWreckSheet = null
    if (stage >= 2) {
      bodySheet = decodeKeyed(R.drawable.boss_stage2_tank_full)
      leftWreckSheet = decodeKeyed(R.drawable.boss_stage2_wreck_left)
      rightWreckSheet = decodeKeyed(R.drawable.boss_stage2_wreck_right)
    } else {
      bodySheet = decodeKeyed(R.drawable.boss_stage1_full)
      leftWreckSheet = decodeKeyed(R.drawable.boss_stage1_wreck_left)
      rightWreckSheet = decodeKeyed(R.drawable.boss_stage1_wreck_right)
    }
    loadedStage = stage
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
    const val TYPE_STAGE2_LEFT_TREAD = 4
    const val TYPE_STAGE2_RIGHT_TREAD = 5
    const val TYPE_STAGE2_MAIN_TURRET = 6
    const val MAX_PART_COUNT = 7
    const val HOVER_Y_FRAC = 0.25f
    const val CORE_WIDTH_FRAC = 0.85f
    const val ENTER_SPEED = 90f
    const val SWEEP_RATE = 0.55f
    const val SWEEP_AMP = 0.18f
    const val FIRE_INTERVAL = 1.5f
    const val TANK_FIRE_INTERVAL = 0.42f
    const val TANK_SHOT_SPEED = 780f
    const val TANK_BARREL_SEP_FRAC = 0.09f
    const val RING_COUNT = 5
    const val RING_STEP = (Math.PI * 2.0 / RING_COUNT).toFloat()
    const val RING_SPEED = 420f
    const val S1_CORE_HP = 60
    const val S1_WING_HP = 25
    const val S1_TURRET_HP = 20
    const val S2_CORE_HP = 90
    const val S2_TREAD_HP = 35
    const val S2_TURRET_HP = 30
  }
}
