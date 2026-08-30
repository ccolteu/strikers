package com.example.strikers

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
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
  private val outlinePaint = Paint().apply {
    isFilterBitmap = true
    colorFilter = PorterDuffColorFilter(Color.BLACK, PorterDuff.Mode.SRC_IN)
  }
  private val shadowPaint = Paint().apply {
    isFilterBitmap = true
    colorFilter = PorterDuffColorFilter(0xCC000000.toInt(), PorterDuff.Mode.SRC_IN)
  }
  private var bodySheet: Bitmap? = null
  private var leftWreckSheet: Bitmap? = null
  private var rightWreckSheet: Bitmap? = null
  private var expFrame1: Bitmap? = null
  private var expFrame2: Bitmap? = null
  private var expFrame3: Bitmap? = null
  private val srcExp = Rect()
  private var screenW = 0f
  private var screenH = 0f
  private var coreX = 0f
  private var coreY = 0f
  private var hoverY = 0f
  private var sweepPhase = 0f
  private var turretAngle = 0f
  private var turretFireTimer = 0f
  private var wingFireTimer = 0f
  private var turretBurstRemaining = 0
  private var turretBurstTimer = 0f
  private var turretBurstAngle = 0f
  private var tankBarrelTimer = 0f
  private var tankTreadTimer = 0f
  private var fireLeftBarrel = true
  private var entering = false
  private var active = false
  private var isExploding = false
  private var explosionTimer = 0f
  private var activeExpFrame = 0
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
    turretFireTimer = TURRET_BURST_INTERVAL
    wingFireTimer = WING_FIRE_INTERVAL
    turretBurstRemaining = 0
    turretBurstTimer = 0f
    tankBarrelTimer = TANK_BARREL_INTERVAL
    tankTreadTimer = TANK_TREAD_INTERVAL
    fireLeftBarrel = true
    turretAngle = 1.5707964f
    coreX = screenW * 0.5f
    coreY = -bodyHalfH - 24f
    isExploding = false
    explosionTimer = 0f
    activeExpFrame = 0
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

  fun isExploding(): Boolean = isExploding

  fun deactivate() {
    active = false
    entering = false
    isExploding = false
    explosionTimer = 0f
    activeExpFrame = 0
  }

  fun getComponents(): Array<BossComponent> = parts

  fun getComponentCount(): Int = MAX_PART_COUNT

  fun update(dt: Float, playerX: Float, playerY: Float, weapons: EnemyWeaponSystem) {
    if (!active) return
    if (isExploding) {
      explosionTimer += dt
      if (explosionTimer < EXPLODE_FRAME_SEC) {
        activeExpFrame = 1
      } else if (explosionTimer < EXPLODE_FRAME_SEC * 2f) {
        activeExpFrame = 2
      } else if (explosionTimer < EXPLODE_FRAME_SEC * 3f) {
        activeExpFrame = 3
      } else {
        isExploding = false
        active = false
      }
      return
    }
    if (parts[TYPE_CORE].isDestroyed) {
      isExploding = true
      explosionTimer = 0f
      activeExpFrame = 1
      return
    }
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
    dstRect.set(coreX - bodyHalfW, coreY - bodyHalfH, coreX + bodyHalfW, coreY + bodyHalfH)
    blitOutlined(canvas, sheet, srcCore, bodyPaint)
    val leftWreck = leftWreckSheet
    val rightWreck = rightWreckSheet
    if (currentStage >= 2) {
      if (leftWreck != null && parts[TYPE_STAGE2_LEFT_TREAD].isDestroyed) {
        blitOutlined(canvas, leftWreck, srcCore, bodyPaint)
      }
      if (rightWreck != null && parts[TYPE_STAGE2_RIGHT_TREAD].isDestroyed) {
        blitOutlined(canvas, rightWreck, srcCore, bodyPaint)
      }
    } else {
      if (leftWreck != null && parts[TYPE_LEFT_WING].isDestroyed) {
        blitOutlined(canvas, leftWreck, srcCore, bodyPaint)
      }
      if (rightWreck != null && parts[TYPE_RIGHT_WING].isDestroyed) {
        blitOutlined(canvas, rightWreck, srcCore, bodyPaint)
      }
    }
    if (isExploding) {
      val exp = if (activeExpFrame == 1) {
        expFrame1
      } else if (activeExpFrame == 2) {
        expFrame2
      } else {
        expFrame3
      }
      if (exp != null) {
        canvas.drawBitmap(exp, srcExp, dstRect, bodyPaint)
      }
    }
  }

  private fun blitOutlined(canvas: Canvas, bmp: Bitmap, src: Rect, paint: Paint) {
    dstRect.offset(SHADOW_PX.toFloat(), SHADOW_PX.toFloat())
    canvas.drawBitmap(bmp, src, dstRect, shadowPaint)
    dstRect.offset(-SHADOW_PX.toFloat(), -SHADOW_PX.toFloat())
    var oy = -OUTLINE_PX
    while (oy <= OUTLINE_PX) {
      var ox = -OUTLINE_PX
      while (ox <= OUTLINE_PX) {
        if (ox != 0 || oy != 0) {
          dstRect.offset(ox.toFloat(), oy.toFloat())
          canvas.drawBitmap(bmp, src, dstRect, outlinePaint)
          dstRect.offset(-ox.toFloat(), -oy.toFloat())
        }
        ox += OUTLINE_PX
      }
      oy += OUTLINE_PX
    }
    canvas.drawBitmap(bmp, src, dstRect, paint)
  }

  fun release() {
    recycle(bodySheet)
    recycle(leftWreckSheet)
    recycle(rightWreckSheet)
    recycle(expFrame1)
    recycle(expFrame2)
    recycle(expFrame3)
    bodySheet = null
    leftWreckSheet = null
    rightWreckSheet = null
    expFrame1 = null
    expFrame2 = null
    expFrame3 = null
    loadedStage = -1
  }

  private fun updatePlaneCombat(dt: Float, playerX: Float, playerY: Float, weapons: EnemyWeaponSystem) {
    if (entering) return
    val turret = parts[TYPE_STAGE1_TURRET]
    if (turret.isDestroyed) {
      turretBurstRemaining = 0
    } else if (turretBurstRemaining > 0) {
      turretBurstTimer -= dt
      if (turretBurstTimer <= 0f) {
        fireAimed(weapons, turret.x, turret.y, turretBurstAngle, TURRET_SHOT_SPEED)
        turretBurstRemaining -= 1
        turretBurstTimer = TURRET_BURST_GAP
      }
    } else {
      turretFireTimer -= dt
      if (turretFireTimer <= 0f) {
        turretBurstAngle = atan2(playerY - turret.y, playerX - turret.x)
        fireAimed(weapons, turret.x, turret.y, turretBurstAngle, TURRET_SHOT_SPEED)
        turretBurstRemaining = TURRET_BURST_COUNT - 1
        turretBurstTimer = TURRET_BURST_GAP
        turretFireTimer = TURRET_BURST_INTERVAL
      }
    }
    wingFireTimer -= dt
    if (wingFireTimer > 0f) return
    wingFireTimer = WING_FIRE_INTERVAL
    val left = parts[TYPE_STAGE1_LEFT_WING]
    if (!left.isDestroyed) {
      fireWingVolley(weapons, left.x, left.y)
    }
    val right = parts[TYPE_STAGE1_RIGHT_WING]
    if (!right.isDestroyed) {
      fireWingVolley(weapons, right.x, right.y)
    }
  }

  private fun fireWingVolley(weapons: EnemyWeaponSystem, originX: Float, originY: Float) {
    fireAimed(weapons, originX, originY, DOWN_ANGLE, WING_SHOT_SPEED)
    fireAimed(weapons, originX, originY, DOWN_ANGLE + WING_DIAG, WING_SHOT_SPEED)
    fireAimed(weapons, originX, originY, DOWN_ANGLE - WING_DIAG, WING_SHOT_SPEED)
  }

  private fun fireAimed(weapons: EnemyWeaponSystem, originX: Float, originY: Float, ang: Float, speed: Float) {
    weapons.fireBullet(originX, originY, cos(ang) * speed, sin(ang) * speed)
  }

  private fun updateTankCombat(dt: Float, playerX: Float, playerY: Float, weapons: EnemyWeaponSystem) {
    if (entering) return
    tankBarrelTimer -= dt
    if (tankBarrelTimer <= 0f) {
      tankBarrelTimer = TANK_BARREL_INTERVAL
      val tankTurret = parts[TYPE_STAGE2_MAIN_TURRET]
      if (!tankTurret.isDestroyed) {
        val muzzleX = if (fireLeftBarrel) tankTurret.x - TANK_BARREL_SEP else tankTurret.x + TANK_BARREL_SEP
        val muzzleY = tankTurret.y + tankTurret.halfH
        fireDirect(weapons, muzzleX, muzzleY, playerX, playerY)
        fireLeftBarrel = !fireLeftBarrel
      }
    }
    tankTreadTimer -= dt
    if (tankTreadTimer > 0f) return
    tankTreadTimer = TANK_TREAD_INTERVAL
    val leftTread = parts[TYPE_STAGE2_LEFT_TREAD]
    if (!leftTread.isDestroyed) {
      fireTreadFan(weapons, leftTread.x, leftTread.y, true)
    }
    val rightTread = parts[TYPE_STAGE2_RIGHT_TREAD]
    if (!rightTread.isDestroyed) {
      fireTreadFan(weapons, rightTread.x, rightTread.y, false)
    }
  }

  private fun fireTreadFan(weapons: EnemyWeaponSystem, originX: Float, originY: Float, leftSide: Boolean) {
    fireAimed(weapons, originX, originY, DOWN_ANGLE, TANK_TREAD_SHOT_SPEED)
    if (leftSide) {
      fireAimed(weapons, originX, originY, DOWN_ANGLE + DEG_30, TANK_TREAD_SHOT_SPEED)
      fireAimed(weapons, originX, originY, DOWN_ANGLE + DEG_45, TANK_TREAD_SHOT_SPEED)
    } else {
      fireAimed(weapons, originX, originY, DOWN_ANGLE - DEG_30, TANK_TREAD_SHOT_SPEED)
      fireAimed(weapons, originX, originY, DOWN_ANGLE - DEG_45, TANK_TREAD_SHOT_SPEED)
    }
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
    if (expFrame1 == null) {
      expFrame1 = decodeKeyed(R.drawable.boss_explode_f1)
      expFrame2 = decodeKeyed(R.drawable.boss_explode_f2)
      expFrame3 = decodeKeyed(R.drawable.boss_explode_f3)
      val exp = expFrame1
      if (exp != null) {
        srcExp.set(0, 0, exp.width, exp.height)
      }
    }
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
    const val TYPE_STAGE1_LEFT_WING = 1
    const val TYPE_STAGE1_RIGHT_WING = 2
    const val TYPE_STAGE1_TURRET = 3
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
    const val TURRET_BURST_INTERVAL = 1.2f
    const val TURRET_BURST_GAP = 0.08f
    const val TURRET_BURST_COUNT = 3
    const val TURRET_SHOT_SPEED = 450f
    const val WING_FIRE_INTERVAL = 0.8f
    const val WING_SHOT_SPEED = 450f
    const val WING_DIAG = 0.15f
    const val DOWN_ANGLE = 1.5707964f
    const val TANK_BARREL_INTERVAL = 0.30f
    const val TANK_TREAD_INTERVAL = 1.5f
    const val TANK_BARREL_SEP = 16f
    const val TANK_SHOT_SPEED = 780f
    const val TANK_TREAD_SHOT_SPEED = 420f
    const val DEG_30 = 0.5235988f
    const val DEG_45 = 0.7853982f
    const val RING_COUNT = 5
    const val RING_STEP = (Math.PI * 2.0 / RING_COUNT).toFloat()
    const val RING_SPEED = 420f
    const val S1_CORE_HP = 60
    const val S1_WING_HP = 25
    const val S1_TURRET_HP = 20
    const val S2_CORE_HP = 90
    const val S2_TREAD_HP = 35
    const val S2_TURRET_HP = 30
    const val EXPLODE_FRAME_SEC = 0.35f
    const val SHADOW_PX = 2
    const val OUTLINE_PX = 3
  }
}
