package com.cc.ww2blitz

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

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
  private val pinkPaint = Paint().apply {
    color = 0xFFFF4EC8.toInt()
    style = Paint.Style.FILL
    isAntiAlias = false
  }
  private val laserPaint = Paint().apply {
    color = 0xFFE040FB.toInt()
    style = Paint.Style.FILL
    isAntiAlias = false
  }
  private val cyanPaint = Paint().apply {
    color = 0xFF18F0FF.toInt()
    style = Paint.Style.FILL
    isAntiAlias = false
  }
  private val drawRect = RectF()
  private var screenW = 0f
  private var screenH = 0f
  private val clearX = FloatArray(CLEAR_SLOTS)
  private val clearY = FloatArray(CLEAR_SLOTS)
  private val clearT = FloatArray(CLEAR_SLOTS)

  private var turretTimer = 0f
  private var coreVentTimer = 0f
  private var magmaTimer = 0f
  private var spiralAngle = 0f
  private var spiralGate = S6_SPIRAL_INTERVAL
  private var s5RingAlt = 0
  private var s5MagmaSeed = 1L
  private var s6RailAlt = 0
  private var s6LaserTimer = S6_WAVE_INTERVAL
  private var s6RingTimer = S6_RING_INTERVAL
  private var s6BurstRemaining = 0
  private var s6BurstGap = S6_BURST_GAP
  private var s6ColumnRemaining = 0
  private var s6ColumnGap = S6_BURST_GAP
  private var s1NoseTimer = 0f
  private var s1WingTimer = 0f
  private var s2MainTimer = 0f
  private var s2TreadTimer = 0f
  private var s3FlakTimer = 0f
  private var s3CannonTimer = 0f
  private var s3FlakLeft = true
  private var s4MortarTimer = 0f
  private var s4GatlingTimer = 0f

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

  fun convertActiveToScoreItems() {
    var i = 0
    while (i < POOL_SIZE) {
      val b = pool[i]
      if (b.isActive) {
        PowerUpManager.instance.spawnBulletCancelDrop(b.x, b.y)
        b.isActive = false
        b.flags = 0
        b.vx = 0f
        b.vy = 0f
      }
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
    fireBullet(startX, startY, velX, velY, 0)
  }

  fun fireBullet(startX: Float, startY: Float, velX: Float, velY: Float, flags: Int) {
    var i = 0
    while (i < POOL_SIZE) {
      val b = pool[i]
      if (!b.isActive) {
        b.x = startX
        b.y = startY
        b.vx = velX
        b.vy = velY
        b.flags = flags
        b.isActive = true
        return
      }
      i++
    }
  }

  fun resetStage1Boss() {
    s1NoseTimer = scaledInterval(S1_NOSE_INTERVAL)
    s1WingTimer = scaledInterval(S1_WING_INTERVAL)
  }

  fun resetStage2Boss() {
    s2MainTimer = scaledInterval(S2_MAIN_INTERVAL)
    s2TreadTimer = scaledInterval(S2_TREAD_INTERVAL)
  }

  fun resetStage3Boss() {
    s3FlakTimer = scaledInterval(S3_FLAK_INTERVAL)
    s3CannonTimer = scaledInterval(S3_CANNON_INTERVAL)
    s3FlakLeft = true
  }

  fun resetStage4Boss() {
    s4MortarTimer = scaledInterval(S4_MORTAR_INTERVAL)
    s4GatlingTimer = scaledInterval(S4_GATLING_INTERVAL)
  }

  fun resetStage5Boss() {
    turretTimer = scaledInterval(S5_VOLLEY_INTERVAL)
    coreVentTimer = scaledInterval(S5_RING_INTERVAL)
    magmaTimer = 0f
    spiralAngle = 0f
    spiralGate = 0f
    s5RingAlt = 0
  }

  fun resetStage6Boss() {
    turretTimer = scaledInterval(S6_RAIL_INTERVAL)
    s6LaserTimer = scaledInterval(S6_WAVE_INTERVAL)
    s6RingTimer = scaledInterval(S6_RING_INTERVAL)
    spiralAngle = 0f
    spiralGate = scaledInterval(S6_SPIRAL_INTERVAL)
    s6RailAlt = 0
    s6BurstRemaining = 0
    s6BurstGap = scaledInterval(S6_BURST_GAP)
    s6ColumnRemaining = 0
    s6ColumnGap = scaledInterval(S6_BURST_GAP)
  }

  fun updateStage1Boss(
    dt: Float,
    centerX: Float,
    centerY: Float,
    bossW: Float,
    playerX: Float,
    playerY: Float,
    leftWingDead: Boolean,
    rightWingDead: Boolean,
  ) {
    s1NoseTimer -= dt
    if (s1NoseTimer <= 0f) {
      s1NoseTimer = scaledInterval(S1_NOSE_INTERVAL)
      val noseX = centerX
      val noseY = centerY + bossW * 0.48f
      val ang = atan2(playerY - noseY, playerX - noseX)
      val spd = scaledSpeed(S1_NOSE_SPEED)
      fireBullet(noseX, noseY, cos(ang) * spd, sin(ang) * spd)
    }
    val leftLive = !leftWingDead
    val rightLive = !rightWingDead
    if (!leftLive && !rightLive) return
    s1WingTimer -= dt
    if (s1WingTimer > 0f) return
    s1WingTimer = scaledInterval(S1_WING_INTERVAL)
    val wingY = centerY + bossW * 0.05f
    if (leftLive) {
      fireS1WingSpread(centerX - bossW * 0.25f, wingY)
    }
    if (rightLive) {
      fireS1WingSpread(centerX + bossW * 0.25f, wingY)
    }
  }

  private fun fireS1WingSpread(spawnX: Float, spawnY: Float) {
    val spd = scaledSpeed(S1_WING_SPEED)
    fireBullet(spawnX, spawnY, cos(S1_ANG_75) * spd, sin(S1_ANG_75) * spd)
    fireBullet(spawnX, spawnY, cos(S1_ANG_90) * spd, sin(S1_ANG_90) * spd)
    fireBullet(spawnX, spawnY, cos(S1_ANG_105) * spd, sin(S1_ANG_105) * spd)
  }

  fun updateStage2Boss(
    dt: Float,
    cX: Float,
    cY: Float,
    w: Float,
    pX: Float,
    pY: Float,
    leftTreadDead: Boolean,
    rightTreadDead: Boolean,
    turretDead: Boolean,
  ) {
    if (!turretDead) {
      s2MainTimer -= dt
      if (s2MainTimer <= 0f) {
        s2MainTimer = scaledInterval(S2_MAIN_INTERVAL)
        val barrelY = cY + w * 0.45f
        val shotSpd = scaledSpeed(TANK_SHOT_SPEED)
        fireBullet(cX - TANK_BARREL_SEP, barrelY, 0f, shotSpd)
        fireBullet(cX + TANK_BARREL_SEP, barrelY, 0f, shotSpd)
      }
    }
    val leftLive = !leftTreadDead
    val rightLive = !rightTreadDead
    if (!leftLive && !rightLive) return
    s2TreadTimer -= dt
    if (s2TreadTimer > 0f) return
    s2TreadTimer = scaledInterval(S2_TREAD_INTERVAL)
    val treadY = cY + w * 0.10f
    if (leftLive) {
      fireS2SponsonFan(cX - w * 0.35f, treadY, true)
    }
    if (rightLive) {
      fireS2SponsonFan(cX + w * 0.35f, treadY, false)
    }
  }

  private fun fireS2SponsonFan(spawnX: Float, spawnY: Float, downAndLeft: Boolean) {
    val spd = scaledSpeed(S2_SPONSON_SPEED)
    var i = 0
    while (i < 4) {
      val ang = if (downAndLeft) {
        S2_DOWN_ANGLE + i * S2_SPONSON_STEP
      } else {
        S2_DOWN_ANGLE - i * S2_SPONSON_STEP
      }
      fireBullet(spawnX, spawnY, cos(ang) * spd, sin(ang) * spd)
      i++
    }
  }

  fun updateStage3Boss(
    dt: Float,
    cX: Float,
    cY: Float,
    w: Float,
    pX: Float,
    pY: Float,
    leftFlakDead: Boolean,
    rightFlakDead: Boolean,
    cannonDead: Boolean,
  ) {
    val leftLive = !leftFlakDead
    val rightLive = !rightFlakDead
    if (leftLive || rightLive) {
      s3FlakTimer -= dt
      if (s3FlakTimer <= 0f) {
        s3FlakTimer = scaledInterval(S3_FLAK_INTERVAL)
        val spawnY = cY + w * 0.025f
        if (s3FlakLeft && leftLive) {
          fireS3FlakBurst(cX - w * 0.29f, spawnY, pX, pY)
        } else if (!s3FlakLeft && rightLive) {
          fireS3FlakBurst(cX + w * 0.29f, spawnY, pX, pY)
        } else if (leftLive) {
          fireS3FlakBurst(cX - w * 0.29f, spawnY, pX, pY)
        } else {
          fireS3FlakBurst(cX + w * 0.29f, spawnY, pX, pY)
        }
        s3FlakLeft = !s3FlakLeft
      }
    }
    if (cannonDead) return
    s3CannonTimer -= dt
    if (s3CannonTimer > 0f) return
    s3CannonTimer = scaledInterval(S3_CANNON_INTERVAL)
    val ox = cX
    val oy = cY + w * 0.46f
    val wallSpd = scaledSpeed(S3_WALL_SPEED)
    var i = 0
    while (i < S3_WALL_COUNT) {
      val ang = S2_DOWN_ANGLE - S3_WALL_HALF + i * S3_WALL_STEP
      fireBullet(ox, oy, cos(ang) * wallSpd, sin(ang) * wallSpd)
      i++
    }
  }

  private fun fireS3FlakBurst(spawnX: Float, spawnY: Float, playerX: Float, playerY: Float) {
    fireS3Aimed(spawnX - S3_FLAK_SEP, spawnY, playerX, playerY)
    fireS3Aimed(spawnX, spawnY, playerX, playerY)
    fireS3Aimed(spawnX + S3_FLAK_SEP, spawnY, playerX, playerY)
  }

  private fun fireS3Aimed(originX: Float, originY: Float, targetX: Float, targetY: Float) {
    val dx = targetX - originX
    val dy = targetY - originY
    val lenSq = dx * dx + dy * dy
    if (lenSq < 0.0001f) return
    val inv = scaledSpeed(S3_FLAK_SPEED) / sqrt(lenSq)
    fireBullet(originX, originY, dx * inv, dy * inv)
  }

  fun updateStage4Boss(
    dt: Float,
    cX: Float,
    cY: Float,
    bossW: Float,
    bossHalfH: Float,
    pX: Float,
    pY: Float,
    leftMortarDead: Boolean,
    rightMortarDead: Boolean,
    gatlingDead: Boolean,
  ) {
    if (!leftMortarDead || !rightMortarDead) {
      s4MortarTimer -= dt
      if (s4MortarTimer <= 0f) {
        s4MortarTimer = scaledInterval(S4_MORTAR_INTERVAL)
        if (!leftMortarDead) {
          fireS4MortarFan(cX - bossW * 0.16f, cY + bossHalfH * 0.28f, true)
        }
        if (!rightMortarDead) {
          fireS4MortarFan(cX + bossW * 0.16f, cY + bossHalfH * 0.28f, false)
        }
      }
    }
    if (gatlingDead) return
    s4GatlingTimer -= dt
    if (s4GatlingTimer > 0f) return
    s4GatlingTimer = scaledInterval(S4_GATLING_INTERVAL)
    val spawnY = cY + (bossHalfH * 0.88f)
    val gatlingSpd = scaledSpeed(720f)
    var index = -2
    while (index <= 2) {
      val spawnX = cX + index * 24f
      fireBullet(spawnX, spawnY, 0f, gatlingSpd)
      index++
    }
  }

  private fun fireS4MortarFan(spawnX: Float, spawnY: Float, downAndRight: Boolean) {
    val base = if (downAndRight) {
      S2_DOWN_ANGLE - S4_FAN_HALF
    } else {
      S2_DOWN_ANGLE + S4_FAN_HALF
    }
    val spd = scaledSpeed(S4_MORTAR_SPEED)
    var i = -1
    while (i <= 1) {
      val ang = base + i * S4_FAN_STEP
      fireBullet(spawnX, spawnY, cos(ang) * spd, sin(ang) * spd)
      i++
    }
  }

  fun updateStage5Boss(
    dt: Float,
    centerX: Float,
    centerY: Float,
    bossWidth: Float,
    playerX: Float,
    playerY: Float,
    leftDestroyed: Boolean,
    rightDestroyed: Boolean,
  ) {
    val leftTurretX = centerX - 0.38f * bossWidth
    val leftTurretY = centerY
    val rightTurretX = centerX + 0.38f * bossWidth
    val rightTurretY = centerY
    val coreVentX = centerX
    val coreVentY = centerY + 0.10f * bossWidth
    val leftLive = !leftDestroyed
    val rightLive = !rightDestroyed
    if (leftLive && rightLive) {
      turretTimer -= dt
      if (turretTimer <= 0f) {
        turretTimer = scaledInterval(S5_VOLLEY_INTERVAL)
        fireS5DownSpread(leftTurretX, leftTurretY)
        fireS5DownSpread(rightTurretX, rightTurretY)
      }
      coreVentTimer -= dt
      if (coreVentTimer <= 0f) {
        coreVentTimer = scaledInterval(S5_RING_INTERVAL)
        fireS5Ring(coreVentX, coreVentY)
      }
      return
    }
    if (leftLive || rightLive) {
      turretTimer -= dt
      if (turretTimer <= 0f) {
        turretTimer = scaledInterval(S5_PANIC_INTERVAL)
        if (leftLive) {
          fireS5PlayerFan(leftTurretX, leftTurretY, playerX, playerY)
        } else {
          fireS5PlayerFan(rightTurretX, rightTurretY, playerX, playerY)
        }
      }
      magmaTimer -= dt
      if (magmaTimer <= 0f) {
        magmaTimer = scaledInterval(S5_MAGMA_INTERVAL)
        var n = 0
        while (n < 3) {
          s5MagmaSeed = s5MagmaSeed * 1664525L + 1013904223L
          val u = ((s5MagmaSeed ushr 8) and 0xFFFFFFL).toFloat() / 16777215f
          val ox = (u * 0.30f - 0.15f) * bossWidth
          fireBullet(coreVentX + ox, coreVentY, 0f, scaledSpeed(S5_MAGMA_VY))
          n++
        }
      }
      return
    }
    spiralAngle += S5_SPIRAL_SPIN * dt
    spiralGate -= dt
    if (spiralGate <= 0f) {
      spiralGate = scaledInterval(S5_SPIRAL_INTERVAL)
      fireS5SpiralPair(coreVentX, coreVentY, spiralAngle)
      fireS5SpiralPair(coreVentX, coreVentY, -spiralAngle)
    }
  }

  private fun fireS5DownSpread(originX: Float, originY: Float) {
    var i = 0
    while (i < 3) {
      val ox = originX + (i - 1) * S5_BARREL_SEP
      fireBullet(ox, originY, 0f, scaledSpeed(S5_VOLLEY_VY))
      i++
    }
  }

  private fun fireS5Ring(originX: Float, originY: Float) {
    val twoPi = 6.2831855f
    val step = twoPi / 8f
    val phase = if (s5RingAlt == 0) 0f else step * 0.5f
    s5RingAlt = if (s5RingAlt == 0) 1 else 0
    var i = 0
    val ringSpd = scaledSpeed(S5_RING_SPEED)
    while (i < 8) {
      val ang = phase + i * step
      fireBullet(originX, originY, cos(ang) * ringSpd, sin(ang) * ringSpd)
      i++
    }
  }

  private fun fireS5PlayerFan(originX: Float, originY: Float, playerX: Float, playerY: Float) {
    val targetAngle = atan2(playerY - originY, playerX - originX)
    val fanSpd = scaledSpeed(S5_FAN_SPEED)
    var i = 0
    while (i < 5) {
      val ang = targetAngle + (i - 2) * S5_FAN_STEP
      fireBullet(originX, originY, cos(ang) * fanSpd, sin(ang) * fanSpd)
      i++
    }
  }

  fun updateStage6Boss(
    dt: Float,
    leftX: Float,
    leftY: Float,
    rightX: Float,
    rightY: Float,
    lensX: Float,
    lensY: Float,
    playerX: Float,
    playerY: Float,
    leftDestroyed: Boolean,
    rightDestroyed: Boolean,
  ) {
    val leftLive = !leftDestroyed
    val rightLive = !rightDestroyed
    if (leftLive && rightLive) {
      tickStage6CyanBurstGate(dt, leftX, leftY, rightX, rightY, playerX, playerY)
      tickStage6PinkColumnGate(dt, lensX, lensY)
      return
    }
    if (leftLive || rightLive) {
      tickStage6CyanStreamGate(dt, leftLive, leftX, leftY, rightX, rightY)
      tickStage6PinkRingGate(dt, lensX, lensY)
      return
    }
    tickStage6PinkSpiralGate(dt, lensX, lensY)
  }

  /** Rail charge XOR burst gap: one cyan array consumes dt per step. */
  private fun tickStage6CyanBurstGate(
    dt: Float,
    leftX: Float,
    leftY: Float,
    rightX: Float,
    rightY: Float,
    playerX: Float,
    playerY: Float,
  ) {
    if (s6BurstRemaining > 0) {
      s6BurstGap -= dt
      if (s6BurstGap > 0f) return
      val ox = if (s6RailAlt == 0) leftX else rightX
      val oy = if (s6RailAlt == 0) leftY else rightY
      fireS6AimedTriple(ox, oy, playerX, playerY)
      s6BurstRemaining -= 1
      s6BurstGap = scaledInterval(S6_BURST_GAP)
      if (s6BurstRemaining == 0) {
        s6RailAlt = if (s6RailAlt == 0) 1 else 0
      }
      return
    }
    turretTimer -= dt
    if (turretTimer > 0f) return
    turretTimer = scaledInterval(S6_RAIL_INTERVAL)
    s6BurstRemaining = S6_BURST_COUNT
    s6BurstGap = scaledInterval(S6_BURST_GAP)
  }

  private fun tickStage6PinkColumnGate(dt: Float, lensX: Float, lensY: Float) {
    if (s6ColumnRemaining > 0) {
      s6ColumnGap -= dt
      if (s6ColumnGap > 0f) return
      fireS6CoreColumn(lensX, lensY)
      s6ColumnRemaining -= 1
      s6ColumnGap = scaledInterval(S6_BURST_GAP)
      return
    }
    s6LaserTimer -= dt
    if (s6LaserTimer > 0f) return
    s6LaserTimer = scaledInterval(S6_WAVE_INTERVAL)
    s6ColumnRemaining = S6_CORE_SHOTS
    s6ColumnGap = scaledInterval(S6_BURST_GAP)
  }

  private fun tickStage6CyanStreamGate(
    dt: Float,
    leftLive: Boolean,
    leftX: Float,
    leftY: Float,
    rightX: Float,
    rightY: Float,
  ) {
    turretTimer -= dt
    if (turretTimer > 0f) return
    turretTimer = scaledInterval(S6_OVERCHARGE_INTERVAL)
    val ox = if (leftLive) leftX else rightX
    val oy = if (leftLive) leftY else rightY
    fireBullet(ox, oy, 0f, scaledSpeed(S6_STREAM_SPEED), EnemyBullet.FLAG_CYAN)
  }

  private fun tickStage6PinkRingGate(dt: Float, lensX: Float, lensY: Float) {
    s6RingTimer -= dt
    if (s6RingTimer > 0f) return
    s6RingTimer = scaledInterval(S6_RING_INTERVAL)
    fireS6CoreRing(lensX, lensY)
  }

  private fun tickStage6PinkSpiralGate(dt: Float, lensX: Float, lensY: Float) {
    spiralAngle += S6_SPIRAL_SPIN * dt
    if (spiralAngle > 6.2831855f) {
      spiralAngle -= 6.2831855f
    }
    spiralGate -= dt
    if (spiralGate > 0f) return
    spiralGate = scaledInterval(S6_SPIRAL_INTERVAL)
    val a0 = spiralAngle
    val a1 = spiralAngle + S6_HELIX_A1
    val a2 = spiralAngle + S6_HELIX_A2
    val a3 = spiralAngle + S6_HELIX_A3
    val spd = scaledSpeed(S6_SPIRAL_SPEED)
    fireBullet(lensX, lensY, cos(a0) * spd, sin(a0) * spd, EnemyBullet.FLAG_PINK)
    fireBullet(lensX, lensY, cos(a1) * spd, sin(a1) * spd, EnemyBullet.FLAG_PINK)
    fireBullet(lensX, lensY, cos(a2) * spd, sin(a2) * spd, EnemyBullet.FLAG_PINK)
    fireBullet(lensX, lensY, cos(a3) * spd, sin(a3) * spd, EnemyBullet.FLAG_PINK)
    val cc0 = -spiralAngle
    val cc1 = -spiralAngle + S6_HELIX_A1
    val cc2 = -spiralAngle + S6_HELIX_A2
    val cc3 = -spiralAngle + S6_HELIX_A3
    fireBullet(lensX, lensY, cos(cc0) * spd, sin(cc0) * spd, EnemyBullet.FLAG_PINK)
    fireBullet(lensX, lensY, cos(cc1) * spd, sin(cc1) * spd, EnemyBullet.FLAG_PINK)
    fireBullet(lensX, lensY, cos(cc2) * spd, sin(cc2) * spd, EnemyBullet.FLAG_PINK)
    fireBullet(lensX, lensY, cos(cc3) * spd, sin(cc3) * spd, EnemyBullet.FLAG_PINK)
  }

  private fun fireS6AimedTriple(muzzleX: Float, muzzleY: Float, playerX: Float, playerY: Float) {
    val angle = atan2(playerY - muzzleY, playerX - muzzleX)
    val spd = scaledSpeed(S6_BURST_SPEED)
    fireBullet(muzzleX, muzzleY, cos(angle) * spd, sin(angle) * spd, EnemyBullet.FLAG_CYAN)
    fireBullet(muzzleX, muzzleY, cos(angle - 0.08f) * spd, sin(angle - 0.08f) * spd, EnemyBullet.FLAG_CYAN)
    fireBullet(muzzleX, muzzleY, cos(angle + 0.08f) * spd, sin(angle + 0.08f) * spd, EnemyBullet.FLAG_CYAN)
  }

  private fun fireS6CoreColumn(lensX: Float, lensY: Float) {
    fireBullet(lensX, lensY, 0f, scaledSpeed(S6_CORE_VY), EnemyBullet.FLAG_PINK)
  }

  private fun fireS6CoreRing(originX: Float, originY: Float) {
    var i = 0
    while (i < S6_RING_COUNT) {
      val ang = i * S6_RING_STEP
      val ringSpd = scaledSpeed(S6_RING_SPEED)
      val vx = cos(ang) * ringSpd
      val vy = sin(ang) * ringSpd
      fireBullet(originX, originY, vx, vy, EnemyBullet.FLAG_PINK)
      i++
    }
  }

  private fun fireS5SpiralPair(originX: Float, originY: Float, ang: Float) {
    val spd = scaledSpeed(S5_SPIRAL_SPEED)
    fireBullet(originX, originY, cos(ang) * spd, sin(ang) * spd)
    fireBullet(
      originX,
      originY,
      cos(ang + 3.1415927f) * spd,
      sin(ang + 3.1415927f) * spd,
    )
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
        val cullX = if ((b.flags and EnemyBullet.FLAG_LASER) != 0) S6_LASER_HW else r
        val cullY = if ((b.flags and EnemyBullet.FLAG_LASER) != 0) S6_LASER_HH else r
        if (b.x + cullX < 0f || b.x - cullX > w || b.y + cullY < 0f || b.y - cullY > h) {
          b.isActive = false
          b.flags = 0
        }
      }
      i++
    }
    updateDeathClears(dt)
  }

  private fun scaledSpeed(base: Float): Float {
    val s = StageData.liveInstance
    return if (s != null) base * s.shotSpeedScale() else base
  }

  private fun scaledInterval(base: Float): Float {
    val s = StageData.liveInstance
    val div = if (s != null) s.fireIntervalDivider() else 1f
    return if (div < 0.01f) base else base / div
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
        val laser = (b.flags and EnemyBullet.FLAG_LASER) != 0
        val pink = (b.flags and EnemyBullet.FLAG_PINK) != 0
        val cyan = (b.flags and EnemyBullet.FLAG_CYAN) != 0
        if (laser) {
          drawRect.set(b.x - S6_LASER_HW, b.y - S6_LASER_HH, b.x + S6_LASER_HW, b.y + S6_LASER_HH)
          canvas.drawRect(drawRect, laserPaint)
        } else {
          val fill = if (pink) pinkPaint else if (cyan) cyanPaint else bulletPaint
          drawRect.set(b.x - hw, b.y - hh, b.x + hw, b.y + hh)
          canvas.drawOval(drawRect, fill)
          drawRect.set(b.x - hw + 5f, b.y - hh + 5f, b.x + hw - 5f, b.y + hh - 5f)
          canvas.drawOval(drawRect, innerGlowPaint)
        }
      }
      i++
    }
  }

  companion object {
    const val POOL_SIZE = 720
    const val HALF_BULLET_WIDTH = 18f
    const val HALF_BULLET_HEIGHT = 18f
    const val BULLET_HIT_RADIUS = 10f
    const val CLEAR_SLOTS = 4
    const val CLEAR_RADIUS = 150f
    const val CLEAR_LIFE = 0.15f
    const val S1_NOSE_INTERVAL = 0.22f
    const val S1_WING_INTERVAL = 1.5f
    const val S1_NOSE_SPEED = 620f
    const val S1_WING_SPEED = 420f
    const val S1_ANG_75 = 1.309f
    const val S1_ANG_90 = 1.5707964f
    const val S1_ANG_105 = 1.8326f
    const val S2_MAIN_INTERVAL = 1.50f
    const val S2_TREAD_INTERVAL = 2.20f
    const val TANK_SHOT_SPEED = 700f
    const val TANK_BARREL_SEP = 32f
    const val S2_DOWN_ANGLE = 1.5707964f
    const val S2_SPONSON_SPEED = 480f
    const val S2_SPONSON_STEP = 0.2617994f
    const val S3_FLAK_INTERVAL = 0.60f
    const val S3_CANNON_INTERVAL = 3.00f
    const val S3_FLAK_SPEED = 560f
    const val S3_FLAK_SEP = 8f
    const val S3_WALL_COUNT = 7
    const val S3_WALL_HALF = 0.5235988f
    const val S3_WALL_STEP = 0.1745329f
    const val S3_WALL_SPEED = 400f
    const val S4_MORTAR_INTERVAL = 1.20f
    const val S4_GATLING_INTERVAL = 0.90f
    const val S4_FAN_STEP = 0.2618f
    const val S4_FAN_HALF = 0.2618f
    const val S4_MORTAR_SPEED = 580f
    const val S5_VOLLEY_INTERVAL = 1.8f
    const val S5_PANIC_INTERVAL = 0.9f
    const val S5_RING_INTERVAL = 3.0f
    const val S5_VOLLEY_VY = 400f
    const val S5_BARREL_SEP = 14f
    const val S5_RING_SPEED = 360f
    const val S5_FAN_STEP = 0.1745329f
    const val S5_FAN_SPEED = 480f
    const val S5_MAGMA_INTERVAL = 1.5f
    const val S5_MAGMA_VY = 190f
    const val S5_SPIRAL_SPIN = 6.5f
    const val S5_SPIRAL_INTERVAL = 0.15f
    const val S5_SPIRAL_SPEED = 440f
    const val S6_RAIL_INTERVAL = 1.4f
    const val S6_BURST_COUNT = 3
    const val S6_BURST_GAP = 0.22f
    const val S6_BURST_SPEED = 500f
    const val S6_OVERCHARGE_INTERVAL = 0.4f
    const val S6_STREAM_SPEED = 500f
    const val S6_WAVE_INTERVAL = 2.5f
    const val S6_CORE_SHOTS = 4
    const val S6_CORE_VY = 220f
    const val S6_RING_INTERVAL = 1.8f
    const val S6_SPIRAL_INTERVAL = 0.08f
    const val S6_SPIRAL_SPIN = 9.5f
    const val S6_HELIX_A1 = 1.57f
    const val S6_HELIX_A2 = 3.14f
    const val S6_HELIX_A3 = 4.71f
    const val S6_LASER_HW = 28f
    const val S6_LASER_HH = 6f
    const val S6_RING_COUNT = 12
    const val S6_RING_STEP = 0.5235988f
    const val S6_RING_SPEED = 340f
    const val S6_SPIRAL_SPEED = 480f
  }
}
