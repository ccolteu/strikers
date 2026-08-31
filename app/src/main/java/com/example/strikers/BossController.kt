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
  private var centerWreckSheet: Bitmap? = null
  private val expFrames = arrayOfNulls<Bitmap>(EXPLODE_FRAME_COUNT)
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
  private var s1TurretTimer = 0f
  private var s1WingTimer = 0f
  private var s1SweepTimer = 0f
  private var s1SweepFireTimer = 0f
  private var s1SweepDirection = 1f
  private var s2TurretTimer = 0f
  private var s2SideGunsTimer = 0f
  private var s2DesperationTimer = 0f
  private var s2RingTimer = 0f
  private var s2SniperTimer = 0f
  private var tankBarrelTimer = 0f
  private var tankTreadTimer = 0f
  private var fireLeftBarrel = true
  private var s3FlakTimer = 0f
  private var s3MegaCannonTimer = 0f
  private var s3DesperationAngle = 0f
  private var s3SpiralTimer = 0f
  private var s4MortarTimer = 0f
  private var s4GatlingTimer = 0f
  private var s4SpiralTimer = 0f
  private var s4SpiralAngle = 0f
  private var entering = false
  private var active = false
  private var isExploding = false
  private var explosionTimer = 0f
  private var activeExpFrame = 0
  private var currentStage = 1
  private var loadedStage = -1
  private val partSfxPlayed = BooleanArray(MAX_PART_COUNT)
  private val preservedDestroyed = BooleanArray(MAX_PART_COUNT)
  private val preservedHealth = IntArray(MAX_PART_COUNT)
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
    currentStage = if (stage >= 4) 4 else if (stage >= 3) 3 else if (stage >= 2) 2 else 1
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
    s1TurretTimer = 0f
    s1WingTimer = 0f
    s1SweepTimer = 0f
    s1SweepFireTimer = 0f
    s1SweepDirection = 1f
    s2TurretTimer = 0f
    s2SideGunsTimer = 0f
    s2DesperationTimer = 0f
    s2RingTimer = 0f
    s2SniperTimer = 0f
    tankBarrelTimer = TANK_BARREL_INTERVAL
    tankTreadTimer = TANK_TREAD_INTERVAL
    fireLeftBarrel = true
    s3FlakTimer = 0f
    s3MegaCannonTimer = 0f
    s3DesperationAngle = 0f
    s3SpiralTimer = 0f
    s4MortarTimer = 0f
    s4GatlingTimer = 0f
    s4SpiralTimer = 0f
    s4SpiralAngle = 0f
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
    var si = 0
    while (si < MAX_PART_COUNT) {
      partSfxPlayed[si] = false
      si++
    }
    SoundManager.instance.playSFX(SoundManager.SFX_ALARM)
  }

  fun isActive(): Boolean = active

  fun isExploding(): Boolean = isExploding

  fun isCoreVulnerable(): Boolean {
    var i = 1
    while (i < MAX_PART_COUNT) {
      val p = parts[i]
      if (p.halfW > 0f && !p.isDestroyed) return false
      i++
    }
    return true
  }

  fun deactivate() {
    active = false
    entering = false
    isExploding = false
    explosionTimer = 0f
    activeExpFrame = 0
    SoundManager.instance.stopAlarm()
    s4MortarTimer = 0f
    s4GatlingTimer = 0f
    s4SpiralTimer = 0f
    s4SpiralAngle = 0f
    s1TurretTimer = 0f
    s1WingTimer = 0f
    s1SweepTimer = 0f
    s1SweepFireTimer = 0f
    s1SweepDirection = 1f
    s2TurretTimer = 0f
    s2SideGunsTimer = 0f
    s2DesperationTimer = 0f
    s2RingTimer = 0f
    s2SniperTimer = 0f
    s3FlakTimer = 0f
    s3MegaCannonTimer = 0f
    s3DesperationAngle = 0f
    s3SpiralTimer = 0f
  }

  fun getComponents(): Array<BossComponent> = parts

  fun getComponentCount(): Int = MAX_PART_COUNT

  fun update(dt: Float, playerX: Float, playerY: Float, weapons: EnemyWeaponSystem) {
    if (!active) return
    if (isExploding) {
      explosionTimer += dt
      val idx = (explosionTimer / EXPLODE_FRAME_SEC).toInt()
      if (idx >= EXPLODE_FRAME_COUNT) {
        isExploding = false
        active = false
      } else {
        activeExpFrame = idx
      }
      return
    }
    if (parts[TYPE_CORE].isDestroyed) {
      SoundManager.instance.stopAlarm()
      SoundManager.instance.playSFX(SoundManager.SFX_HEAVY_EXPLOSION)
      isExploding = true
      explosionTimer = 0f
      activeExpFrame = 0
      return
    }
    if (entering) {
      coreY += ENTER_SPEED * dt
      if (coreY >= hoverY) {
        coreY = hoverY
        entering = false
        SoundManager.instance.stopAlarm()
      }
    } else if (currentStage == 2) {
      coreX = screenW * 0.5f
    } else {
      sweepPhase += dt * SWEEP_RATE
      coreX = screenW * 0.5f + sin(sweepPhase) * screenW * SWEEP_AMP
    }
    var i = 0
    while (i < MAX_PART_COUNT) {
      val p = parts[i]
      p.x = coreX + p.relOffsetX
      p.y = coreY + p.relOffsetY
      i++
    }
    if (currentStage == 4) {
      updateJungleFortressCombat(dt, playerX, playerY, weapons)
    } else if (currentStage == 3) {
      updateStage3BossWeapons(dt, playerX, playerY, weapons)
    } else if (currentStage == 2) {
      updateTankCombat(dt, playerX, playerY, weapons)
    } else {
      updatePlaneCombat(dt, playerX, playerY, weapons)
    }
  }

  fun draw(canvas: Canvas) {
    if (!active) return
    dstRect.set(coreX - bodyHalfW, coreY - bodyHalfH, coreX + bodyHalfW, coreY + bodyHalfH)
    val hideBody = isExploding && activeExpFrame >= EXPLODE_HIDE_BODY_FRAME
    if (!hideBody) {
      val sheet = bodySheet
      if (sheet != null) {
        blitOutlined(canvas, sheet, srcCore, bodyPaint)
        drawWreckOverlays(canvas)
      }
    }
    if (isExploding) {
      val exp = expFrames[activeExpFrame]
      if (exp != null) {
        canvas.drawBitmap(exp, srcExp, dstRect, bodyPaint)
      }
    }
  }

  private fun drawWreckOverlays(canvas: Canvas) {
    val leftWreck = leftWreckSheet
    val rightWreck = rightWreckSheet
    val centerWreck = centerWreckSheet
    if (currentStage == 4) {
      if (leftWreck != null && parts[TYPE_STAGE4_LEFT_MORTAR].isDestroyed) {
        canvas.drawBitmap(leftWreck, srcCore, dstRect, bodyPaint)
      }
      if (rightWreck != null && parts[TYPE_STAGE4_RIGHT_MORTAR].isDestroyed) {
        canvas.drawBitmap(rightWreck, srcCore, dstRect, bodyPaint)
      }
      if (
        centerWreck != null &&
        (parts[TYPE_STAGE4_HEAVY_GATLING].isDestroyed || parts[TYPE_CORE].isDestroyed)
      ) {
        canvas.drawBitmap(centerWreck, srcCore, dstRect, bodyPaint)
      }
    } else if (currentStage == 3) {
      if (leftWreck != null && parts[TYPE_STAGE3_LEFT_FLAK].isDestroyed) {
        canvas.drawBitmap(leftWreck, srcCore, dstRect, bodyPaint)
      }
      if (rightWreck != null && parts[TYPE_STAGE3_RIGHT_FLAK].isDestroyed) {
        canvas.drawBitmap(rightWreck, srcCore, dstRect, bodyPaint)
      }
      if (
        centerWreck != null &&
        (parts[TYPE_STAGE3_MEGA_CANNON].isDestroyed || parts[TYPE_CORE].isDestroyed)
      ) {
        canvas.drawBitmap(centerWreck, srcCore, dstRect, bodyPaint)
      }
    } else if (currentStage >= 2) {
      if (leftWreck != null && parts[TYPE_STAGE2_LEFT_TREAD].isDestroyed) {
        canvas.drawBitmap(leftWreck, srcCore, dstRect, bodyPaint)
      }
      if (rightWreck != null && parts[TYPE_STAGE2_RIGHT_TREAD].isDestroyed) {
        canvas.drawBitmap(rightWreck, srcCore, dstRect, bodyPaint)
      }
      if (
        centerWreck != null &&
        (parts[TYPE_STAGE2_MAIN_TURRET].isDestroyed || parts[TYPE_CORE].isDestroyed)
      ) {
        canvas.drawBitmap(centerWreck, srcCore, dstRect, bodyPaint)
      }
    } else {
      // Wreck overlays are torn-edge PNGs; an outline would halo leftover chroma.
      if (leftWreck != null && parts[TYPE_LEFT_WING].isDestroyed) {
        canvas.drawBitmap(leftWreck, srcCore, dstRect, bodyPaint)
      }
      if (rightWreck != null && parts[TYPE_RIGHT_WING].isDestroyed) {
        canvas.drawBitmap(rightWreck, srcCore, dstRect, bodyPaint)
      }
      if (
        centerWreck != null &&
        (parts[TYPE_TURRET].isDestroyed || parts[TYPE_CORE].isDestroyed)
      ) {
        canvas.drawBitmap(centerWreck, srcCore, dstRect, bodyPaint)
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
    recycle(centerWreckSheet)
    var ei = 0
    while (ei < EXPLODE_FRAME_COUNT) {
      recycle(expFrames[ei])
      expFrames[ei] = null
      ei++
    }
    bodySheet = null
    leftWreckSheet = null
    rightWreckSheet = null
    centerWreckSheet = null
    loadedStage = -1
  }

  private fun updatePlaneCombat(dt: Float, playerX: Float, playerY: Float, weapons: EnemyWeaponSystem) {
    playNewModuleSfx()
    if (entering) return
    val chin = parts[S1_PART_CHIN_TURRET]
    if (!chin.isDestroyed && chin.halfW > 0f) {
      s1TurretTimer -= dt
      if (s1TurretTimer <= 0f) {
        s1TurretTimer = S1_CHIN_INTERVAL
        val spawnX = coreX
        val spawnY = coreY + bodyHalfH * 0.95f
        fireAtPlayer(weapons, spawnX - S1_CHIN_SEP, spawnY, playerX, playerY, S1_CHIN_SPEED)
        fireAtPlayer(weapons, spawnX + S1_CHIN_SEP, spawnY, playerX, playerY, S1_CHIN_SPEED)
      }
    }
    val left = parts[S1_PART_LEFT_WING]
    val right = parts[S1_PART_RIGHT_WING]
    val leftLive = !left.isDestroyed && left.halfW > 0f
    val rightLive = !right.isDestroyed && right.halfW > 0f
    if (leftLive || rightLive) {
      s1WingTimer -= dt
      if (s1WingTimer <= 0f) {
        s1WingTimer = S1_WING_INTERVAL
        val spawnY = coreY + bodyHalfH * 0.10f
        if (leftLive) {
          fireS1EngineFan(weapons, coreX - bodyHalfW * 0.50f, spawnY)
        }
        if (rightLive) {
          fireS1EngineFan(weapons, coreX + bodyHalfW * 0.50f, spawnY)
        }
      }
    }
    if (isCoreVulnerable()) {
      s1SweepTimer += dt * s1SweepDirection
      s1SweepFireTimer -= dt
      if (s1SweepFireTimer <= 0f) {
        s1SweepFireTimer = S1_SWEEP_INTERVAL
        val baseAngle = DOWN_ANGLE + sin(s1SweepTimer * S1_SWEEP_FREQ) * S1_SWEEP_AMP
        val ox = coreX
        val oy = coreY - bodyHalfH * 0.10f
        var i = -2
        while (i <= 2) {
          val ang = baseAngle + i * S1_SWEEP_ARC_STEP
          weapons.fireBullet(ox, oy, cos(ang) * S1_SWEEP_SPEED, sin(ang) * S1_SWEEP_SPEED)
          i++
        }
      }
    }
  }

  private fun fireS1EngineFan(weapons: EnemyWeaponSystem, spawnX: Float, spawnY: Float) {
    weapons.fireBullet(spawnX, spawnY, cos(S1_ANG_75) * S1_WING_SPEED, sin(S1_ANG_75) * S1_WING_SPEED)
    weapons.fireBullet(spawnX, spawnY, cos(S1_ANG_90) * S1_WING_SPEED, sin(S1_ANG_90) * S1_WING_SPEED)
    weapons.fireBullet(spawnX, spawnY, cos(S1_ANG_105) * S1_WING_SPEED, sin(S1_ANG_105) * S1_WING_SPEED)
  }

  private fun fireAimed(weapons: EnemyWeaponSystem, originX: Float, originY: Float, ang: Float, speed: Float) {
    weapons.fireBullet(originX, originY, cos(ang) * speed, sin(ang) * speed)
  }

  private fun playNewModuleSfx() {
    var i = 0
    while (i < MAX_PART_COUNT) {
      if (i != TYPE_CORE && parts[i].isDestroyed && parts[i].halfW > 0f && !partSfxPlayed[i]) {
        partSfxPlayed[i] = true
        if (
          i == TYPE_STAGE1_LEFT_WING ||
          i == TYPE_STAGE1_RIGHT_WING ||
          i == TYPE_STAGE2_LEFT_TREAD ||
          i == TYPE_STAGE2_RIGHT_TREAD ||
          i == TYPE_STAGE3_LEFT_FLAK ||
          i == TYPE_STAGE3_RIGHT_FLAK ||
          i == TYPE_STAGE3_MEGA_CANNON ||
          i == TYPE_STAGE4_LEFT_MORTAR ||
          i == TYPE_STAGE4_RIGHT_MORTAR ||
          i == TYPE_STAGE4_HEAVY_GATLING
        ) {
          // Play the punchy boss segment destruction clip instead of stack tracking
          SoundManager.instance.playSFX(SoundManager.SFX_HEAVY_EXPLOSION)
        } else {
          SoundManager.instance.playSFX(SoundManager.SFX_SMALL_EXPLOSION)
        }
      }
      i++
    }
  }

  private fun updateStage3BossWeapons(
    dt: Float,
    playerX: Float,
    playerY: Float,
    weapons: EnemyWeaponSystem,
  ) {
    playNewModuleSfx()
    if (entering) return
    val leftFlak = parts[S3_PART_LEFT_FLAK]
    val rightFlak = parts[S3_PART_RIGHT_FLAK]
    val cannon = parts[S3_PART_MEGA_CANNON]
    val leftLive = !leftFlak.isDestroyed && leftFlak.halfW > 0f
    val rightLive = !rightFlak.isDestroyed && rightFlak.halfW > 0f
    if (leftLive || rightLive) {
      s3FlakTimer -= dt
      if (s3FlakTimer <= 0f) {
        s3FlakTimer = S3_FLAK_INTERVAL
        val spawnY = coreY + bodyHalfH * 0.05f
        if (fireLeftBarrel && leftLive) {
          fireS3FlakBurst(weapons, coreX - bodyHalfW * 0.58f, spawnY, playerX, playerY)
        } else if (!fireLeftBarrel && rightLive) {
          fireS3FlakBurst(weapons, coreX + bodyHalfW * 0.58f, spawnY, playerX, playerY)
        } else if (leftLive) {
          fireS3FlakBurst(weapons, coreX - bodyHalfW * 0.58f, spawnY, playerX, playerY)
        } else {
          fireS3FlakBurst(weapons, coreX + bodyHalfW * 0.58f, spawnY, playerX, playerY)
        }
        fireLeftBarrel = !fireLeftBarrel
      }
    }
    if (!cannon.isDestroyed && cannon.halfW > 0f) {
      s3MegaCannonTimer -= dt
      if (s3MegaCannonTimer <= 0f) {
        s3MegaCannonTimer = S3_CANNON_CHARGE
        val ox = coreX
        val oy = coreY + bodyHalfH * 0.92f
        var i = 0
        while (i < S3_WALL_COUNT) {
          val ang = DOWN_ANGLE - S3_WALL_HALF + i * S3_WALL_STEP
          weapons.fireBullet(ox, oy, cos(ang) * S3_WALL_SPEED, sin(ang) * S3_WALL_SPEED)
          i++
        }
      }
    }
    if (isCoreVulnerable()) {
      s3DesperationAngle += S3_SPIRAL_SPIN * dt
      s3SpiralTimer -= dt
      if (s3SpiralTimer <= 0f) {
        s3SpiralTimer = S3_SPIRAL_INTERVAL
        val ox = coreX
        val oy = coreY - bodyHalfH * 0.12f
        var k = 0
        while (k < S3_SPIRAL_COUNT) {
          val ang = s3DesperationAngle + k * S3_SPIRAL_STEP
          weapons.fireBullet(ox, oy, cos(ang) * S3_SPIRAL_SPEED, sin(ang) * S3_SPIRAL_SPEED)
          k++
        }
      }
    }
  }

  private fun fireS3FlakBurst(
    weapons: EnemyWeaponSystem,
    spawnX: Float,
    spawnY: Float,
    playerX: Float,
    playerY: Float,
  ) {
    fireAtPlayer(weapons, spawnX - S3_FLAK_SEP, spawnY, playerX, playerY, S3_FLAK_SPEED)
    fireAtPlayer(weapons, spawnX, spawnY, playerX, playerY, S3_FLAK_SPEED)
    fireAtPlayer(weapons, spawnX + S3_FLAK_SEP, spawnY, playerX, playerY, S3_FLAK_SPEED)
  }

  private fun updateJungleFortressCombat(
    dt: Float,
    playerX: Float,
    playerY: Float,
    weapons: EnemyWeaponSystem,
  ) {
    playNewModuleSfx()
    if (entering) return
    val leftMortar = parts[TYPE_STAGE4_LEFT_MORTAR]
    val rightMortar = parts[TYPE_STAGE4_RIGHT_MORTAR]
    val gatling = parts[TYPE_STAGE4_HEAVY_GATLING]
    val leftLive = !leftMortar.isDestroyed && leftMortar.halfW > 0f
    val rightLive = !rightMortar.isDestroyed && rightMortar.halfW > 0f
    if (leftLive || rightLive) {
      s4MortarTimer -= dt
      if (s4MortarTimer <= 0f) {
        s4MortarTimer = S4_MORTAR_INTERVAL
        val spawnY = coreY + bodyHalfH * 0.15f
        if (leftLive) {
          val spawnLeftX = coreX - bodyHalfW * 0.40f
          fireS4MortarFan(weapons, spawnLeftX, spawnY, true)
        }
        if (rightLive) {
          val spawnRightX = coreX + bodyHalfW * 0.40f
          fireS4MortarFan(weapons, spawnRightX, spawnY, false)
        }
      }
    }
    if (!gatling.isDestroyed && gatling.halfW > 0f) {
      s4GatlingTimer -= dt
      if (s4GatlingTimer <= 0f) {
        s4GatlingTimer = S4_GATLING_INTERVAL
        val spawnY = gatling.y + gatling.halfH
        var index = -2
        while (index <= 2) {
          val spawnX = coreX + index * S4_GATLING_BARREL_SEP
          weapons.fireBullet(spawnX, spawnY, 0f, S4_GATLING_SPEED)
          index++
        }
      }
    }
    if (isCoreVulnerable()) {
      s4SpiralAngle += S4_SPIRAL_SPIN * dt
      s4SpiralTimer -= dt
      if (s4SpiralTimer <= 0f) {
        s4SpiralTimer = S4_SPIRAL_INTERVAL
        val ox = coreX
        val oy = coreY
        val spd = S4_SPIRAL_SPEED
        var k = 0
        while (k < S4_SPIRAL_COUNT) {
          val step = k * S4_SPIRAL_STEP
          val aCw = s4SpiralAngle + step
          val aCcw = -s4SpiralAngle + step
          weapons.fireBullet(ox, oy, cos(aCw) * spd, sin(aCw) * spd)
          weapons.fireBullet(ox, oy, cos(aCcw) * spd, sin(aCcw) * spd)
          k++
        }
      }
    }
  }

  private fun fireS4MortarFan(
    weapons: EnemyWeaponSystem,
    spawnX: Float,
    spawnY: Float,
    downAndRight: Boolean,
  ) {
    val base = if (downAndRight) {
      DOWN_ANGLE - S4_FAN_HALF
    } else {
      DOWN_ANGLE + S4_FAN_HALF
    }
    var i = -1
    while (i <= 1) {
      val ang = base + i * S4_FAN_STEP
      weapons.fireBullet(spawnX, spawnY, cos(ang) * S4_FLAK_SPEED, sin(ang) * S4_FLAK_SPEED)
      i++
    }
  }

  private fun updateTankCombat(dt: Float, playerX: Float, playerY: Float, weapons: EnemyWeaponSystem) {
    playNewModuleSfx()
    if (entering) return
    val turret = parts[S2_PART_MAIN_TURRET]
    if (!turret.isDestroyed && turret.halfW > 0f) {
      s2TurretTimer -= dt
      if (s2TurretTimer <= 0f) {
        s2TurretTimer = S2_TURRET_INTERVAL
        val spawnY = coreY + bodyHalfH * 0.90f
        weapons.fireBullet(coreX - S2_TURRET_BARREL_SEP, spawnY, 0f, S2_TURRET_SPEED)
        weapons.fireBullet(coreX + S2_TURRET_BARREL_SEP, spawnY, 0f, S2_TURRET_SPEED)
      }
    }
    val leftTread = parts[TYPE_STAGE2_LEFT_TREAD]
    val rightTread = parts[TYPE_STAGE2_RIGHT_TREAD]
    val leftLive = !leftTread.isDestroyed && leftTread.halfW > 0f
    val rightLive = !rightTread.isDestroyed && rightTread.halfW > 0f
    if (leftLive || rightLive) {
      s2SideGunsTimer -= dt
      if (s2SideGunsTimer <= 0f) {
        s2SideGunsTimer = S2_SPONSON_INTERVAL
        val spawnY = coreY + bodyHalfH * 0.20f
        if (leftLive) {
          fireS2SponsonFan(weapons, coreX - bodyHalfW * 0.70f, spawnY, true)
        }
        if (rightLive) {
          fireS2SponsonFan(weapons, coreX + bodyHalfW * 0.70f, spawnY, false)
        }
      }
    }
    if (isCoreVulnerable()) {
      s2DesperationTimer += dt
      s2RingTimer -= dt
      if (s2RingTimer <= 0f) {
        s2RingTimer = S2_RING_INTERVAL
        val ox = coreX
        val oy = coreY - bodyHalfH * 0.40f
        var k = 0
        while (k < S2_RING_COUNT) {
          val ang = k * S2_RING_STEP
          weapons.fireBullet(ox, oy, cos(ang) * S2_RING_SPEED, sin(ang) * S2_RING_SPEED)
          k++
        }
      }
      s2SniperTimer -= dt
      if (s2SniperTimer <= 0f) {
        s2SniperTimer = S2_SNIPER_INTERVAL
        val ox = coreX
        val oy = coreY - bodyHalfH * 0.40f
        fireAtPlayer(weapons, ox - S2_SNIPER_SEP, oy, playerX, playerY, S2_SNIPER_SPEED)
        fireAtPlayer(weapons, ox + S2_SNIPER_SEP, oy, playerX, playerY, S2_SNIPER_SPEED)
      }
    }
  }

  private fun fireS2SponsonFan(
    weapons: EnemyWeaponSystem,
    spawnX: Float,
    spawnY: Float,
    downAndLeft: Boolean,
  ) {
    var i = 0
    while (i < 4) {
      val ang = if (downAndLeft) {
        DOWN_ANGLE + i * S2_SPONSON_STEP
      } else {
        DOWN_ANGLE - i * S2_SPONSON_STEP
      }
      weapons.fireBullet(spawnX, spawnY, cos(ang) * S2_SPONSON_SPEED, sin(ang) * S2_SPONSON_SPEED)
      i++
    }
  }

  private fun fireAtPlayer(
    weapons: EnemyWeaponSystem,
    originX: Float,
    originY: Float,
    targetX: Float,
    targetY: Float,
    speed: Float,
  ) {
    val dx = targetX - originX
    val dy = targetY - originY
    val lenSq = dx * dx + dy * dy
    if (lenSq < 0.0001f) return
    val inv = speed / sqrt(lenSq)
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
    val preserve = active
    if (preserve) {
      var i = 0
      while (i < MAX_PART_COUNT) {
        preservedDestroyed[i] = parts[i].isDestroyed
        preservedHealth[i] = parts[i].health
        i++
      }
    }
    disablePart(TYPE_CORE)
    disablePart(TYPE_LEFT_WING)
    disablePart(TYPE_RIGHT_WING)
    disablePart(TYPE_TURRET)
    disablePart(TYPE_STAGE2_LEFT_TREAD)
    disablePart(TYPE_STAGE2_RIGHT_TREAD)
    disablePart(TYPE_STAGE2_MAIN_TURRET)
    disablePart(TYPE_STAGE3_LEFT_FLAK)
    disablePart(TYPE_STAGE3_RIGHT_FLAK)
    disablePart(TYPE_STAGE3_MEGA_CANNON)
    disablePart(TYPE_STAGE4_LEFT_MORTAR)
    disablePart(TYPE_STAGE4_RIGHT_MORTAR)
    disablePart(TYPE_STAGE4_HEAVY_GATLING)
    if (stage >= 4) {
      setupPart(TYPE_CORE, 0f, 0f, bodyHalfW * 0.30f, bodyHalfH * 0.40f, S4_CORE_HP)
      setupPart(
        TYPE_STAGE4_LEFT_MORTAR,
        -bodyHalfW * 0.75f,
        -bodyHalfH * 0.10f,
        bodyHalfW * 0.18f,
        bodyHalfH * 0.22f,
        S4_MORTAR_HP,
      )
      setupPart(
        TYPE_STAGE4_RIGHT_MORTAR,
        bodyHalfW * 0.75f,
        -bodyHalfH * 0.10f,
        bodyHalfW * 0.18f,
        bodyHalfH * 0.22f,
        S4_MORTAR_HP,
      )
      setupPart(
        TYPE_STAGE4_HEAVY_GATLING,
        0f,
        bodyHalfH * 0.48f,
        bodyHalfW * 0.22f,
        bodyHalfH * 0.26f,
        S4_GATLING_HP,
      )
    } else if (stage >= 3) {
      setupPart(TYPE_CORE, 0f, -bodyHalfH * 0.1f, bodyHalfW * 0.35f, bodyHalfH * 0.40f, S3_CORE_HP)
      setupPart(TYPE_STAGE3_LEFT_FLAK, -bodyHalfW * 0.70f, bodyHalfH * 0.15f, bodyHalfW * 0.20f, bodyHalfH * 0.20f, S3_FLAK_HP)
      setupPart(TYPE_STAGE3_RIGHT_FLAK, bodyHalfW * 0.70f, bodyHalfH * 0.15f, bodyHalfW * 0.20f, bodyHalfH * 0.20f, S3_FLAK_HP)
      setupPart(TYPE_STAGE3_MEGA_CANNON, 0f, bodyHalfH * 0.45f, bodyHalfW * 0.30f, bodyHalfH * 0.25f, S3_CANNON_HP)
    } else if (stage >= 2) {
      setupPart(TYPE_CORE, 0f, 0f, bodyHalfW * 0.28f, bodyHalfH * 0.38f, S2_CORE_HP)
      setupPart(TYPE_STAGE2_LEFT_TREAD, -bodyHalfW * 0.72f, 0f, bodyHalfW * 0.26f, bodyHalfH * 0.52f, S2_TREAD_HP)
      setupPart(TYPE_STAGE2_RIGHT_TREAD, bodyHalfW * 0.72f, 0f, bodyHalfW * 0.26f, bodyHalfH * 0.52f, S2_TREAD_HP)
      setupPart(TYPE_STAGE2_MAIN_TURRET, 0f, bodyHalfH * 0.18f, bodyHalfW * 0.16f, bodyHalfH * 0.28f, S2_TURRET_HP)
    } else {
      setupPart(TYPE_CORE, 0f, 0f, bodyHalfW * 0.28f, bodyHalfH * 0.52f, S1_CORE_HP)
      setupPart(TYPE_LEFT_WING, -bodyHalfW * 0.5f, -bodyHalfH * 0.08f, bodyHalfW * 0.30f, bodyHalfH * 0.20f, S1_WING_HP)
      setupPart(TYPE_RIGHT_WING, bodyHalfW * 0.5f, -bodyHalfH * 0.08f, bodyHalfW * 0.30f, bodyHalfH * 0.20f, S1_WING_HP)
      setupPart(TYPE_TURRET, 0f, bodyHalfH * 0.46f, bodyHalfW * 0.12f, bodyHalfW * 0.12f, S1_TURRET_HP)
    }
    if (preserve) {
      var i = 0
      while (i < MAX_PART_COUNT) {
        val p = parts[i]
        if (p.halfW > 0f) {
          val dead = preservedDestroyed[i]
          p.isDestroyed = dead
          p.health = if (dead) 0 else preservedHealth[i].coerceIn(0, p.maxHealth)
        }
        i++
      }
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
    partSfxPlayed[type] = true
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
    if (expFrames[0] == null) {
      val ids = intArrayOf(
        R.drawable.boss_explode_f1,
        R.drawable.boss_explode_f2,
        R.drawable.boss_explode_f3,
        R.drawable.boss_explode_f4,
        R.drawable.boss_explode_f5,
        R.drawable.boss_explode_f6,
        R.drawable.boss_explode_f7,
        R.drawable.boss_explode_f8,
      )
      var i = 0
      while (i < EXPLODE_FRAME_COUNT) {
        expFrames[i] = decodeKeyed(ids[i])
        i++
      }
      val exp = expFrames[0]
      if (exp != null) {
        srcExp.set(0, 0, exp.width, exp.height)
      }
    }
    if (loadedStage == stage && bodySheet != null) return
    recycle(bodySheet)
    recycle(leftWreckSheet)
    recycle(rightWreckSheet)
    recycle(centerWreckSheet)
    bodySheet = null
    leftWreckSheet = null
    rightWreckSheet = null
    centerWreckSheet = null
    if (stage >= 4) {
      bodySheet = decodeKeyed(R.drawable.boss_stage4_jungle_full)
      leftWreckSheet = decodeKeyed(R.drawable.boss_stage4_wreck_left)
      rightWreckSheet = decodeKeyed(R.drawable.boss_stage4_wreck_right)
      centerWreckSheet = decodeKeyed(R.drawable.boss_stage4_wreck_center)
    } else if (stage >= 3) {
      bodySheet = decodeKeyed(R.drawable.boss_stage3_battleship_full)
      leftWreckSheet = decodeKeyed(R.drawable.boss_stage3_wreck_left)
      rightWreckSheet = decodeKeyed(R.drawable.boss_stage3_wreck_right)
      centerWreckSheet = decodeKeyed(R.drawable.boss_stage3_wreck_center)
    } else if (stage >= 2) {
      bodySheet = decodeKeyed(R.drawable.boss_stage2_tank_full)
      leftWreckSheet = decodeKeyed(R.drawable.boss_stage2_wreck_left)
      rightWreckSheet = decodeKeyed(R.drawable.boss_stage2_wreck_right)
      centerWreckSheet = decodeKeyed(R.drawable.boss_stage2_wreck_center)
    } else {
      bodySheet = decodeKeyed(R.drawable.boss_stage1_full)
      leftWreckSheet = decodeKeyed(R.drawable.boss_stage1_wreck_left)
      rightWreckSheet = decodeKeyed(R.drawable.boss_stage1_wreck_right)
      centerWreckSheet = decodeKeyed(R.drawable.boss_stage1_wreck_center)
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
        val maxRb = if (r > b) r else b
        val excess = g - maxRb
        val chroma =
          (g > 160 && g > r + 40 && g > b + 40) ||
            (excess > 24 && g > 48 && g > r + 16 && g > b + 16) ||
            (r + b < 90 && g > 22 && g > r + 10 && g > b + 10)
        if (chroma) {
          row[i] = 0
        } else if (excess > 6) {
          val ng = maxRb + 3
          row[i] = (r shl 16) or (ng shl 8) or b or (0xFF shl 24)
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
    const val S1_PART_LEFT_WING = 1
    const val S1_PART_RIGHT_WING = 2
    const val S1_PART_CHIN_TURRET = 3
    const val TYPE_STAGE2_LEFT_TREAD = 4
    const val TYPE_STAGE2_RIGHT_TREAD = 5
    const val TYPE_STAGE2_MAIN_TURRET = 6
    const val S2_PART_MAIN_TURRET = 6
    const val TYPE_STAGE3_LEFT_FLAK = 7
    const val TYPE_STAGE3_RIGHT_FLAK = 8
    const val TYPE_STAGE3_MEGA_CANNON = 9
    const val S3_PART_LEFT_FLAK = 7
    const val S3_PART_RIGHT_FLAK = 8
    const val S3_PART_MEGA_CANNON = 9
    const val TYPE_STAGE4_LEFT_MORTAR = 10
    const val TYPE_STAGE4_RIGHT_MORTAR = 11
    const val TYPE_STAGE4_HEAVY_GATLING = 12
    const val MAX_PART_COUNT = 14
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
    const val S1_CORE_HP = 240
    const val S1_WING_HP = 55
    const val S1_TURRET_HP = 45
    const val S1_CHIN_INTERVAL = 0.70f
    const val S1_CHIN_SPEED = 620f
    const val S1_CHIN_SEP = 6f
    const val S1_WING_INTERVAL = 1.80f
    const val S1_WING_SPEED = 420f
    const val S1_ANG_75 = 1.309f
    const val S1_ANG_90 = 1.5707964f
    const val S1_ANG_105 = 1.8326f
    const val S1_SWEEP_INTERVAL = 0.16f
    const val S1_SWEEP_SPEED = 480f
    const val S1_SWEEP_FREQ = 3.5f
    const val S1_SWEEP_AMP = 0.6f
    const val S1_SWEEP_ARC_STEP = 0.12f
    const val S2_CORE_HP = 330
    const val S2_TREAD_HP = 80
    const val S2_TURRET_HP = 70
    const val S2_TURRET_INTERVAL = 1.50f
    const val S2_TURRET_BARREL_SEP = 32f
    const val S2_TURRET_SPEED = 700f
    const val S2_SPONSON_INTERVAL = 2.20f
    const val S2_SPONSON_SPEED = 480f
    const val S2_SPONSON_STEP = 0.2617994f
    const val S2_RING_INTERVAL = 0.80f
    const val S2_RING_SPEED = 340f
    const val S2_RING_COUNT = 16
    const val S2_RING_STEP = (Math.PI * 2.0 / S2_RING_COUNT).toFloat()
    const val S2_SNIPER_INTERVAL = 0.25f
    const val S2_SNIPER_SPEED = 600f
    const val S2_SNIPER_SEP = 8f
    const val S3_CORE_HP = 480
    const val S3_FLAK_HP = 100
    const val S3_CANNON_HP = 160
    const val S3_FLAK_INTERVAL = 0.60f
    const val S3_FLAK_SPEED = 560f
    const val S3_FLAK_SEP = 8f
    const val S3_CANNON_CHARGE = 3.00f
    const val S3_WALL_COUNT = 7
    const val S3_WALL_HALF = 0.5235988f
    const val S3_WALL_STEP = 0.1745329f
    const val S3_WALL_SPEED = 400f
    const val S3_SPIRAL_INTERVAL = 0.10f
    const val S3_SPIRAL_SPEED = 440f
    const val S3_SPIRAL_SPIN = 5.2f
    const val S3_SPIRAL_COUNT = 4
    const val S3_SPIRAL_STEP = (Math.PI * 2.0 / S3_SPIRAL_COUNT).toFloat()
    const val S4_CORE_HP = 600
    const val S4_MORTAR_HP = 140
    const val S4_GATLING_HP = 200
    const val S4_FLAK_SPEED = 580f
    const val S4_GATLING_SPEED = 720f
    const val S4_GATLING_INTERVAL = 0.90f
    const val S4_GATLING_BARREL_SEP = 24f
    const val S4_MORTAR_INTERVAL = 1.20f
    const val S4_FAN_STEP = 0.2617994f
    const val S4_FAN_HALF = 0.2617994f
    const val S4_SPIRAL_INTERVAL = 0.12f
    const val S4_SPIRAL_SPEED = 310f
    const val S4_SPIRAL_SPIN = 4.5f
    const val S4_SPIRAL_COUNT = 6
    const val S4_SPIRAL_STEP = (Math.PI * 2.0 / S4_SPIRAL_COUNT).toFloat()
    const val EXPLODE_FRAME_COUNT = 8
    const val EXPLODE_FRAME_SEC = 0.13f
    const val EXPLODE_HIDE_BODY_FRAME = 3
    const val SHADOW_PX = 2
    const val OUTLINE_PX = 3
  }
}
