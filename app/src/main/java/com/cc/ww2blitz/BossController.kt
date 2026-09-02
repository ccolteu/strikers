package com.cc.ww2blitz

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
  private var s7RingTimer = 0f
  private var s7RingAngle = 0f
  private var entering = false
  private var active = false
  private var isExploding = false
  private var explosionTimer = 0f
  private var activeExpFrame = 0
  private var currentStage = 1
  private var combatKind = BossCombatKind.PLANE
  private var triPartBoss = false
  private var loadedStage = -1
  private var corePhaseOpen = false
  private var visualFlags = 0
  private val partSfxPlayed = BooleanArray(MAX_PART_COUNT)
  private val preservedDestroyed = BooleanArray(MAX_PART_COUNT)
  private val preservedHealth = IntArray(MAX_PART_COUNT)
  private var bodyHalfW = 0f
  private var bodyHalfH = 0f
  private val leftFlankBounds = RectF()
  private val rightFlankBounds = RectF()
  private val coreBounds = RectF()
  private val leftFlankShudder = FloatArray(1)
  private val rightFlankShudder = FloatArray(1)
  private val coreShudder = FloatArray(1)
  private val s5AreaQuery = RectF()
  private var s5CoreArmorBank = 0f
  private var s5HitDestroyed = false
  private var s5HitDestroyX = 0f
  private var s5HitDestroyY = 0f
  private var hasDroppedLeftReward = false
  private var hasDroppedRightReward = false
  private var hasDroppedCoreReward = false
  private var s5WeaponPower = 1
  private var s5BombStock = 3
  private var victorySequenceTimer = 0f
  private var victoryCascadeGate = 0f
  private var victoryShatterFired = false
  private var s5ShowTerminalWreck = false
  private var victoryLcg = 2463534242L
  private var s6SawOneFlankDown = false
  private var s6SawBothFlanksDown = false
  private var stageTimeline: SpawnTimeline? = null

  fun onSizeChanged(width: Int, height: Int) {
    screenW = width.toFloat()
    screenH = height.toFloat()
    hoverY = height * HOVER_Y_FRAC
    applyKit(currentStage)
    if (bodySheet == null) {
      loadKeyedSheet(currentStage)
    }
    cacheSrcRects()
    recomputeBodySize(width)
    layoutOffsets()
  }

  fun bindStage(stage: Int) {
    applyKit(stage)
    if (screenW <= 0f) return
    loadKeyedSheet(currentStage)
    cacheSrcRects()
    recomputeBodySize(screenW.toInt())
    layoutOffsets()
  }

  private fun applyKit(stage: Int) {
    currentStage = stage
    val def = StageCatalog.get(stage)
    combatKind = def.bossCombat
    triPartBoss = def.boss.triPart
  }

  fun beginEntrance() {
    beginEntranceForStage(currentStage)
  }

  fun beginEntranceForStage(stage: Int) {
    if (screenW <= 0f) return
    if (currentStage != stage || loadedStage != stage || bodySheet == null) {
      bindStage(stage)
    } else {
      applyKit(stage)
      layoutOffsets()
    }
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
    s7RingTimer = 0f
    s7RingAngle = 0f
    leftFlankShudder[0] = 0f
    rightFlankShudder[0] = 0f
    coreShudder[0] = 0f
    turretAngle = 1.5707964f
    coreX = screenW * 0.5f
    coreY = -bodyHalfH - 24f
    isExploding = false
    explosionTimer = 0f
    activeExpFrame = 0
    victorySequenceTimer = 0f
    victoryCascadeGate = 0f
    victoryShatterFired = false
    s5ShowTerminalWreck = false
    corePhaseOpen = false
    visualFlags = 0
    s5CoreArmorBank = 0f
    s5HitDestroyed = false
    hasDroppedLeftReward = false
    hasDroppedRightReward = false
    hasDroppedCoreReward = false
    s6SawOneFlankDown = false
    s6SawBothFlanksDown = false
    var i = 0
    while (i < MAX_PART_COUNT) {
      val p = parts[i]
      if (p.halfW > 0f) {
        p.isDestroyed = false
        p.health = p.maxHealth
        p.shudderTimer = 0f
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

  fun isVictorySequence(): Boolean = isTriPartBoss() && isExploding

  fun locksWorldScroll(): Boolean = isTriPartBoss() && isExploding

  fun isTriPartBoss(): Boolean = triPartBoss

  fun isCoreVulnerable(): Boolean {
    var i = 1
    while (i < MAX_PART_COUNT) {
      val p = parts[i]
      if (p.halfW > 0f && !p.isDestroyed) return false
      i++
    }
    return true
  }

  fun refreshPhaseFlags() {
    if (!active || isExploding || entering) {
      corePhaseOpen = isCoreVulnerable()
      return
    }
    val open = isCoreVulnerable()
    if (open && !corePhaseOpen) {
      visualFlags = visualFlags or FX_PHASE
    }
    corePhaseOpen = open
  }

  fun consumeVisualFlags(): Int {
    val flags = visualFlags
    visualFlags = 0
    return flags
  }

  fun deactivate() {
    active = false
    entering = false
    isExploding = false
    explosionTimer = 0f
    activeExpFrame = 0
    victorySequenceTimer = 0f
    victoryCascadeGate = 0f
    victoryShatterFired = false
    s5ShowTerminalWreck = false
    corePhaseOpen = false
    visualFlags = 0
    SoundManager.instance.stopAlarm()
    s4MortarTimer = 0f
    s4GatlingTimer = 0f
    s4SpiralTimer = 0f
    s4SpiralAngle = 0f
    s7RingTimer = 0f
    s7RingAngle = 0f
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

  fun usesStage5Hitboxes(): Boolean = isTriPartBoss() && active && !isExploding

  fun checkStage5Collision(bullet: PlayerBullet): Boolean {
    if (!bullet.isActive) return false
    return checkStage5CollisionAt(bullet.x, bullet.y, 1)
  }

  fun checkCollision(bullet: PlayerBullet): Boolean = checkStage5Collision(bullet)

  fun checkCollisionAt(worldX: Float, worldY: Float, damage: Int): Boolean =
    checkStage5CollisionAt(worldX, worldY, damage)

  fun checkStage5CollisionAt(worldX: Float, worldY: Float, damage: Int): Boolean {
    if (!active || isExploding || !isTriPartBoss()) return false
    s5HitDestroyed = false
    val left = parts[TYPE_LEFT_FLANK]
    val right = parts[TYPE_RIGHT_FLANK]
    val core = parts[TYPE_CORE]
    val leftFlankDestroyed = left.isDestroyed
    val rightFlankDestroyed = right.isDestroyed
    if (!leftFlankDestroyed && leftFlankBounds.contains(worldX, worldY)) {
      applyStage5PartHit(left, damage)
      return true
    }
    if (!rightFlankDestroyed && rightFlankBounds.contains(worldX, worldY)) {
      applyStage5PartHit(right, damage)
      return true
    }
    if (!core.isDestroyed && coreBounds.contains(worldX, worldY)) {
      applyStage5CoreHit(core, left, right, damage)
      return true
    }
    return false
  }

  fun applyStage5AreaDamage(left: Float, top: Float, right: Float, bottom: Float, damage: Int) {
    if (!active || isExploding || !isTriPartBoss() || damage <= 0) return
    s5HitDestroyed = false
    val leftPart = parts[TYPE_LEFT_FLANK]
    val rightPart = parts[TYPE_RIGHT_FLANK]
    val core = parts[TYPE_CORE]
    s5AreaQuery.set(left, top, right, bottom)
    if (!leftPart.isDestroyed && RectF.intersects(leftFlankBounds, s5AreaQuery)) {
      applyStage5PartHit(leftPart, damage)
    }
    if (!rightPart.isDestroyed && RectF.intersects(rightFlankBounds, s5AreaQuery)) {
      applyStage5PartHit(rightPart, damage)
    }
    if (!core.isDestroyed && RectF.intersects(coreBounds, s5AreaQuery)) {
      applyStage5CoreHit(core, leftPart, rightPart, damage)
    }
  }

  fun consumeStage5Break(): Boolean {
    val broke = s5HitDestroyed
    s5HitDestroyed = false
    return broke
  }

  fun stage5BreakX(): Float = s5HitDestroyX

  fun stage5BreakY(): Float = s5HitDestroyY

  private fun applyStage5PartHit(part: BossComponent, damage: Int) {
    var dmg = damage
    if (dmg < 1) dmg = 1
    part.health -= dmg
    part.shudderTimer = BossComponent.SHUDDER_DURATION
    pulseShudder(part.componentType)
    if (part.health <= 0) {
      part.health = 0
      part.isDestroyed = true
      s5HitDestroyed = true
      s5HitDestroyX = part.x
      s5HitDestroyY = part.y
      grantStage5ModuleReward(part.componentType)
    }
  }

  private fun applyStage5CoreHit(
    core: BossComponent,
    left: BossComponent,
    right: BossComponent,
    damage: Int,
  ) {
    var dmg = damage
    if (dmg < 1) dmg = 1
    val armored = !left.isDestroyed && !right.isDestroyed
    if (!armored) {
      core.shudderTimer = BossComponent.SHUDDER_DURATION
      pulseShudder(TYPE_CORE)
    }
    if (armored) {
      s5CoreArmorBank += dmg * 0.5f
      while (s5CoreArmorBank >= 1f && core.health > 0) {
        s5CoreArmorBank -= 1f
        core.health -= 1
      }
    } else {
      core.health -= dmg
    }
    if (core.health <= 0) {
      core.health = 0
      core.isDestroyed = true
      s5HitDestroyed = true
      s5HitDestroyX = core.x
      s5HitDestroyY = core.y
      grantStage5ModuleReward(TYPE_CORE)
    }
  }

  private fun grantStage5ModuleReward(componentType: Int) {
    if (!triPartBoss) return
    if (componentType == TYPE_LEFT_FLANK && !hasDroppedLeftReward) {
      hasDroppedLeftReward = true
      val x = leftFlankBounds.centerX()
      val y = leftFlankBounds.centerY()
      ScoreManager.instance.addFlankBreakBonus(x, y)
      PowerUpManager.instance.spawnGuaranteedDrop(x, y, ItemType.WEAPON_UP)
    } else if (componentType == TYPE_RIGHT_FLANK && !hasDroppedRightReward) {
      hasDroppedRightReward = true
      val x = rightFlankBounds.centerX()
      val y = rightFlankBounds.centerY()
      ScoreManager.instance.addFlankBreakBonus(x, y)
      PowerUpManager.instance.spawnRightFlankDrop(x, y, s5WeaponPower, s5BombStock)
    } else if (componentType == TYPE_CORE && !hasDroppedCoreReward) {
      hasDroppedCoreReward = true
      ScoreManager.instance.addCoreKillBonus(coreBounds.centerX(), coreBounds.centerY())
    }
  }

  private fun syncStage5Hitboxes() {
    val bossWidth = bodyHalfW * 2f
    val bossHeight = bodyHalfH * 2f
    val centerX = coreX
    val centerY = coreY
    val leftL: Float
    val leftR: Float
    val rightL: Float
    val rightR: Float
    val flankT: Float
    val flankB: Float
    val coreL: Float
    val coreR: Float
    val coreT: Float
    val coreB: Float
    if (combatKind == BossCombatKind.ORBIT) {
      leftL = S6_LEFT_L
      leftR = S6_LEFT_R
      rightL = S6_RIGHT_L
      rightR = S6_RIGHT_R
      flankT = S6_FLANK_T
      flankB = S6_FLANK_B
      coreL = S6_CORE_L
      coreR = S6_CORE_R
      coreT = S6_CORE_T
      coreB = S6_CORE_B
    } else {
      leftL = S5_LEFT_L
      leftR = S5_LEFT_R
      rightL = S5_RIGHT_L
      rightR = S5_RIGHT_R
      flankT = S5_FLANK_T
      flankB = S5_FLANK_B
      coreL = S5_CORE_L
      coreR = S5_CORE_R
      coreT = S5_CORE_T
      coreB = S5_CORE_B
    }
    leftFlankBounds.set(
      centerX + leftL * bossWidth,
      centerY + flankT * bossHeight,
      centerX + leftR * bossWidth,
      centerY + flankB * bossHeight,
    )
    rightFlankBounds.set(
      centerX + rightL * bossWidth,
      centerY + flankT * bossHeight,
      centerX + rightR * bossWidth,
      centerY + flankB * bossHeight,
    )
    coreBounds.set(
      centerX + coreL * bossWidth,
      centerY + coreT * bossHeight,
      centerX + coreR * bossWidth,
      centerY + coreB * bossHeight,
    )
  }

  private fun pulseShudder(componentType: Int) {
    val d = BossComponent.SHUDDER_DURATION
    if (componentType == TYPE_LEFT_FLANK) {
      leftFlankShudder[0] = d
      parts[TYPE_LEFT_FLANK].shudderTimer = d
    } else if (componentType == TYPE_RIGHT_FLANK) {
      rightFlankShudder[0] = d
      parts[TYPE_RIGHT_FLANK].shudderTimer = d
    } else if (componentType == TYPE_CORE) {
      coreShudder[0] = d
      parts[TYPE_CORE].shudderTimer = d
    }
  }

  fun update(dt: Float, playerX: Float, playerY: Float, weapons: EnemyWeaponSystem) {
    update(dt, playerX, playerY, weapons, 1, 3)
  }

  fun update(
    dt: Float,
    playerX: Float,
    playerY: Float,
    weapons: EnemyWeaponSystem,
    playerWeaponPower: Int,
    bombStock: Int,
    timeline: SpawnTimeline? = null,
  ) {
    s5WeaponPower = playerWeaponPower
    s5BombStock = bombStock
    stageTimeline = timeline
    if (!active) return
    tickShudder(dt)
    if (isExploding) {
      if (isTriPartBoss()) {
        updateStage5Victory(dt)
        return
      }
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
      if (isTriPartBoss()) {
        beginStage5Victory(weapons)
        return
      }
      SoundManager.instance.stopAlarm()
      SoundManager.instance.playSFX(SoundManager.SFX_HEAVY_EXPLOSION)
      isExploding = true
      explosionTimer = 0f
      activeExpFrame = 0
      visualFlags = visualFlags or FX_DEATH
      return
    }
    if (entering) {
      coreY += ENTER_SPEED * dt
      if (coreY >= hoverY) {
        coreY = hoverY
        entering = false
        SoundManager.instance.stopAlarm()
      }
    } else if (combatKind == BossCombatKind.TANK) {
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
    if (isTriPartBoss()) {
      syncStage5Hitboxes()
    }
    if (combatKind == BossCombatKind.ORBIT) {
      updateStage6Combat(dt, playerX, playerY, weapons)
    } else if (combatKind == BossCombatKind.CANOPY) {
      updateStage5Combat(dt, playerX, playerY, weapons)
    } else if (combatKind == BossCombatKind.WINTER) {
      updateWinterKeepCombat(dt, playerX, playerY, weapons)
    } else if (combatKind == BossCombatKind.JUNGLE) {
      updateJungleFortressCombat(dt, playerX, playerY, weapons)
    } else if (combatKind == BossCombatKind.BATTLESHIP) {
      updateStage3BossWeapons(dt, playerX, playerY, weapons)
    } else if (combatKind == BossCombatKind.TANK) {
      updateTankCombat(dt, playerX, playerY, weapons)
    } else {
      updatePlaneCombat(dt, playerX, playerY, weapons)
    }
  }

  private fun tickShudder(dt: Float) {
    var i = 0
    while (i < MAX_PART_COUNT) {
      val p = parts[i]
      if (p.shudderTimer > 0f) {
        p.shudderTimer -= dt
      }
      i++
    }
    if (leftFlankShudder[0] > 0f) leftFlankShudder[0] -= dt
    if (rightFlankShudder[0] > 0f) rightFlankShudder[0] -= dt
    if (coreShudder[0] > 0f) coreShudder[0] -= dt
  }

  fun draw(canvas: Canvas) {
    if (!active) return
    dstRect.set(coreX - bodyHalfW, coreY - bodyHalfH, coreX + bodyHalfW, coreY + bodyHalfH)
    val hideBody = isExploding && activeExpFrame >= EXPLODE_HIDE_BODY_FRAME
    if (!hideBody || isTriPartBoss()) {
      val sheet = bodySheet
      if (sheet != null) {
        if (isTriPartBoss()) {
          drawStage5(canvas, sheet, hideBody)
        } else if (!hideBody) {
          var shudderTimer = 0f
          var i = 0
          while (i < MAX_PART_COUNT) {
            if (parts[i].shudderTimer > 0f) {
              shudderTimer = parts[i].shudderTimer
              break
            }
            i++
          }
          val shuddering = shudderTimer > 0f
          if (shuddering) {
            val currentOffset = shudderDx(shudderTimer)
            canvas.save()
            canvas.translate(currentOffset, 0f)
          }
          blitOutlined(canvas, sheet, srcCore, bodyPaint)
          drawWreckOverlays(canvas)
          if (shuddering) {
            canvas.restore()
          }
        }
      }
    }
    if (isExploding && !isTriPartBoss()) {
      val exp = expFrames[activeExpFrame]
      if (exp != null) {
        canvas.drawBitmap(exp, srcExp, dstRect, bodyPaint)
      }
    }
  }

  private fun shudderDx(timer: Float): Float {
    if (timer <= 0f) return 0f
    return if ((timer * 100f).toInt() % 2 == 0) 2.0f else -2.0f
  }

  private fun drawStage5(canvas: Canvas, bgStage5FullBmp: Bitmap, hideBody: Boolean) {
    val leftFlankDestroyed = parts[TYPE_LEFT_FLANK].isDestroyed
    val rightFlankDestroyed = parts[TYPE_RIGHT_FLANK].isDestroyed
    val coreDestroyed = parts[TYPE_CORE].isDestroyed
    val leftFlankShake = leftFlankShudder[0]
    val rightFlankShake = rightFlankShudder[0]
    val coreShake = coreShudder[0]
    val bgStage5WreckLeftBmp = leftWreckSheet
    val bgStage5WreckRightBmp = rightWreckSheet
    val bgStage5WreckCenterBmp = centerWreckSheet
    dstRect.set(coreX - bodyHalfW, coreY - bodyHalfH, coreX + bodyHalfW, coreY + bodyHalfH)
    if (coreDestroyed && s5ShowTerminalWreck) {
      if (bgStage5WreckCenterBmp != null) {
        canvas.drawBitmap(bgStage5WreckCenterBmp, srcCore, dstRect, bodyPaint)
      }
      return
    }
    if (hideBody) return
    val coreExposed = leftFlankDestroyed && rightFlankDestroyed
    if (coreExposed && coreShake > 0f) {
      canvas.save()
      canvas.translate(shudderDx(coreShake), 0f)
      blitOutlined(canvas, bgStage5FullBmp, srcCore, bodyPaint)
      canvas.restore()
    } else {
      blitOutlined(canvas, bgStage5FullBmp, srcCore, bodyPaint)
      if (!leftFlankDestroyed && leftFlankShake > 0f) {
        canvas.save()
        canvas.clipRect(leftFlankBounds)
        canvas.translate(shudderDx(leftFlankShake), 0f)
        blitOutlined(canvas, bgStage5FullBmp, srcCore, bodyPaint)
        canvas.restore()
      }
      if (!rightFlankDestroyed && rightFlankShake > 0f) {
        canvas.save()
        canvas.clipRect(rightFlankBounds)
        canvas.translate(shudderDx(rightFlankShake), 0f)
        blitOutlined(canvas, bgStage5FullBmp, srcCore, bodyPaint)
        canvas.restore()
      }
    }
    if (leftFlankDestroyed && bgStage5WreckLeftBmp != null) {
      canvas.save()
      canvas.clipRect(dstRect.left, dstRect.top, coreX, dstRect.bottom)
      if (leftFlankShake > 0f) {
        val currentOffset = if ((leftFlankShake * 100f).toInt() % 2 == 0) 2.0f else -2.0f
        canvas.translate(currentOffset, 0f)
      }
      canvas.drawBitmap(bgStage5WreckLeftBmp, srcCore, dstRect, bodyPaint)
      canvas.restore()
    }
    if (rightFlankDestroyed && bgStage5WreckRightBmp != null) {
      canvas.save()
      canvas.clipRect(coreX, dstRect.top, dstRect.right, dstRect.bottom)
      if (rightFlankShake > 0f) {
        val currentOffset = if ((rightFlankShake * 100f).toInt() % 2 == 0) 2.0f else -2.0f
        canvas.translate(currentOffset, 0f)
      }
      canvas.drawBitmap(bgStage5WreckRightBmp, srcCore, dstRect, bodyPaint)
      canvas.restore()
    }
  }

  private fun drawWreckOverlays(canvas: Canvas) {
    val leftWreck = leftWreckSheet
    val rightWreck = rightWreckSheet
    val centerWreck = centerWreckSheet

    when (combatKind) {
      BossCombatKind.ORBIT, BossCombatKind.CANOPY -> {
        // Multi-part module wrecks are handled independently via drawStage5()
        return
      }
      BossCombatKind.WINTER -> {
        if (leftWreck != null && parts[TYPE_WINTER_LEFT_HOWITZER].isDestroyed) {
          canvas.drawBitmap(leftWreck, srcCore, dstRect, bodyPaint)
        }
        if (rightWreck != null && parts[TYPE_WINTER_RIGHT_HOWITZER].isDestroyed) {
          canvas.drawBitmap(rightWreck, srcCore, dstRect, bodyPaint)
        }
        if (centerWreck != null && (parts[TYPE_WINTER_BLIZZARD].isDestroyed || parts[TYPE_CORE].isDestroyed)) {
          canvas.drawBitmap(centerWreck, srcCore, dstRect, bodyPaint)
        }
      }
      BossCombatKind.JUNGLE -> {
        if (leftWreck != null && parts[TYPE_STAGE4_LEFT_MORTAR].isDestroyed) {
          canvas.drawBitmap(leftWreck, srcCore, dstRect, bodyPaint)
        }
        if (rightWreck != null && parts[TYPE_STAGE4_RIGHT_MORTAR].isDestroyed) {
          canvas.drawBitmap(rightWreck, srcCore, dstRect, bodyPaint)
        }
        if (centerWreck != null && (parts[TYPE_STAGE4_HEAVY_GATLING].isDestroyed || parts[TYPE_CORE].isDestroyed)) {
          canvas.drawBitmap(centerWreck, srcCore, dstRect, bodyPaint)
        }
      }
      BossCombatKind.BATTLESHIP -> {
        if (leftWreck != null && parts[TYPE_STAGE3_LEFT_FLAK].isDestroyed) {
          canvas.drawBitmap(leftWreck, srcCore, dstRect, bodyPaint)
        }
        if (rightWreck != null && parts[TYPE_STAGE3_RIGHT_FLAK].isDestroyed) {
          canvas.drawBitmap(rightWreck, srcCore, dstRect, bodyPaint)
        }
        if (centerWreck != null && (parts[TYPE_STAGE3_MEGA_CANNON].isDestroyed || parts[TYPE_CORE].isDestroyed)) {
          canvas.drawBitmap(centerWreck, srcCore, dstRect, bodyPaint)
        }
      }
      BossCombatKind.TANK -> {
        if (leftWreck != null && parts[TYPE_STAGE2_LEFT_TREAD].isDestroyed) {
          canvas.drawBitmap(leftWreck, srcCore, dstRect, bodyPaint)
        }
        if (rightWreck != null && parts[TYPE_STAGE2_RIGHT_TREAD].isDestroyed) {
          canvas.drawBitmap(rightWreck, srcCore, dstRect, bodyPaint)
        }
        if (centerWreck != null && (parts[TYPE_STAGE2_MAIN_TURRET].isDestroyed || parts[TYPE_CORE].isDestroyed)) {
          canvas.drawBitmap(centerWreck, srcCore, dstRect, bodyPaint)
        }
      }
      else -> {
        if (leftWreck != null && parts[TYPE_LEFT_WING].isDestroyed) {
          canvas.drawBitmap(leftWreck, srcCore, dstRect, bodyPaint)
        }
        if (rightWreck != null && parts[TYPE_RIGHT_WING].isDestroyed) {
          canvas.drawBitmap(rightWreck, srcCore, dstRect, bodyPaint)
        }
        if (centerWreck != null && (parts[TYPE_TURRET].isDestroyed || parts[TYPE_CORE].isDestroyed)) {
          canvas.drawBitmap(centerWreck, srcCore, dstRect, bodyPaint)
        }
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
    if (entering) {
      weapons.resetStage1Boss()
      return
    }
    val left = parts[S1_PART_LEFT_WING]
    val right = parts[S1_PART_RIGHT_WING]
    val leftDead = left.isDestroyed || left.halfW <= 0f
    val rightDead = right.isDestroyed || right.halfW <= 0f
    weapons.updateStage1Boss(
      dt,
      coreX,
      coreY,
      bodyHalfW * 2f,
      playerX,
      playerY,
      leftDead,
      rightDead,
    )
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

  private fun updateStage5Combat(
    dt: Float,
    playerX: Float,
    playerY: Float,
    weapons: EnemyWeaponSystem,
  ) {
    playNewModuleSfx()
    if (entering) {
      weapons.resetStage5Boss()
      return
    }
    val left = parts[TYPE_LEFT_FLANK]
    val right = parts[TYPE_RIGHT_FLANK]
    weapons.updateStage5Boss(
      dt,
      coreX,
      coreY,
      bodyHalfW * 2f,
      playerX,
      playerY,
      left.isDestroyed,
      right.isDestroyed,
    )
  }

  private fun updateStage6Combat(
    dt: Float,
    playerX: Float,
    playerY: Float,
    weapons: EnemyWeaponSystem,
  ) {
    playNewModuleSfx()
    if (entering) {
      weapons.resetStage6Boss()
      return
    }
    val left = parts[TYPE_LEFT_FLANK]
    val right = parts[TYPE_RIGHT_FLANK]
    val leftDead = left.isDestroyed
    val rightDead = right.isDestroyed
    if (leftDead && rightDead) {
      if (!s6SawBothFlanksDown) {
        s6SawBothFlanksDown = true
        s6SawOneFlankDown = true
        stageTimeline?.forceElapsed(S6_PHASE3_CLOCK)
      }
    } else if (leftDead || rightDead) {
      if (!s6SawOneFlankDown) {
        s6SawOneFlankDown = true
        stageTimeline?.forceElapsed(S6_PHASE2_CLOCK)
      }
    }
    val bossWidth = bodyHalfW * 2f
    val bossHeight = bodyHalfH * 2f
    val leftMuzzleXOffset = S6_LEFT_MUZZLE_X * bossWidth
    val leftMuzzleYOffset = S6_LEFT_MUZZLE_Y * bossHeight
    val rightMuzzleXOffset = S6_RIGHT_MUZZLE_X * bossWidth
    val rightMuzzleYOffset = S6_RIGHT_MUZZLE_Y * bossHeight
    val coreLensXOffset = S6_CORE_LENS_X * bossWidth
    val coreLensYOffset = S6_CORE_LENS_Y * bossHeight
    val leftX = coreX + leftMuzzleXOffset
    val leftY = coreY + leftMuzzleYOffset
    val rightX = coreX + rightMuzzleXOffset
    val rightY = coreY + rightMuzzleYOffset
    val lensX = coreX + coreLensXOffset
    val lensY = coreY + coreLensYOffset
    weapons.updateStage6Boss(
      dt,
      leftX,
      leftY,
      rightX,
      rightY,
      lensX,
      lensY,
      playerX,
      playerY,
      left.isDestroyed,
      right.isDestroyed,
    )
  }

  private fun beginStage5Victory(weapons: EnemyWeaponSystem) {
    SoundManager.instance.stopAlarm()
    SoundManager.instance.stopBGM()
    SoundManager.instance.playSFX(SoundManager.SFX_HEAVY_EXPLOSION)
    weapons.convertActiveToScoreItems()
    isExploding = true
    explosionTimer = 0f
    activeExpFrame = 0
    victorySequenceTimer = 0f
    victoryCascadeGate = 0f
    victoryShatterFired = false
    s5ShowTerminalWreck = false
    visualFlags = visualFlags or FX_VICTORY_START
  }

  private fun updateStage5Victory(dt: Float) {
    val prev = victorySequenceTimer
    victorySequenceTimer += dt
    val t = victorySequenceTimer
    if (prev < VICTORY_CASCADE_START && t >= VICTORY_CASCADE_START) {
      victoryCascadeGate = 0f
    }
    if (t >= VICTORY_CASCADE_START && t < VICTORY_SHATTER_AT) {
      victoryCascadeGate -= dt
      if (victoryCascadeGate <= 0f) {
        victoryCascadeGate += VICTORY_CASCADE_STEP
        spawnVictoryExplosionInChassis()
        visualFlags = visualFlags or FX_VICTORY_CASCADE
      }
    }
    if (!victoryShatterFired && t >= VICTORY_SHATTER_AT) {
      victoryShatterFired = true
      s5ShowTerminalWreck = true
      spawnVictoryShatterCluster()
      SoundManager.instance.playSFX(SoundManager.SFX_BOMB)
      visualFlags = visualFlags or FX_VICTORY_SHATTER
    }
    if (t >= VICTORY_END) {
      isExploding = false
      active = false
    }
  }

  private fun spawnVictoryExplosionInChassis() {
    val pick = (nextVictoryUnit() * 3f).toInt()
    val bounds = if (pick <= 0) {
      leftFlankBounds
    } else if (pick == 1) {
      rightFlankBounds
    } else {
      coreBounds
    }
    val w = bounds.right - bounds.left
    val h = bounds.bottom - bounds.top
    val x = bounds.left + nextVictoryUnit() * w
    val y = bounds.top + nextVictoryUnit() * h
    ParticleManager.instance.spawnExplosion(x, y)
  }

  private fun spawnVictoryShatterCluster() {
    val left = coreX - bodyHalfW
    val span = bodyHalfW * 2f
    var i = 0
    while (i < VICTORY_CLUSTER) {
      val u = (i + 0.5f) / VICTORY_CLUSTER.toFloat()
      val x = left + u * span
      val y = coreY + (nextVictoryUnit() - 0.5f) * bodyHalfH
      ParticleManager.instance.spawnExplosion(x, y)
      i++
    }
  }

  private fun nextVictoryUnit(): Float {
    victoryLcg = victoryLcg * 1664525L + 1013904223L
    return ((victoryLcg ushr 8) and 0xFFFFFFL).toFloat() / 16777215f
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
          i == TYPE_STAGE4_HEAVY_GATLING ||
          i == TYPE_WINTER_LEFT_HOWITZER ||
          i == TYPE_WINTER_RIGHT_HOWITZER ||
          i == TYPE_WINTER_BLIZZARD ||
          i == TYPE_LEFT_FLANK ||
          i == TYPE_RIGHT_FLANK
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
    if (entering) {
      weapons.resetStage3Boss()
      return
    }
    val leftFlak = parts[S3_PART_LEFT_FLAK]
    val rightFlak = parts[S3_PART_RIGHT_FLAK]
    val cannon = parts[S3_PART_MEGA_CANNON]
    val leftDead = leftFlak.isDestroyed || leftFlak.halfW <= 0f
    val rightDead = rightFlak.isDestroyed || rightFlak.halfW <= 0f
    val cannonDead = cannon.isDestroyed || cannon.halfW <= 0f
    weapons.updateStage3Boss(
      dt,
      coreX,
      coreY,
      bodyHalfW * 2f,
      playerX,
      playerY,
      leftDead,
      rightDead,
      cannonDead,
    )
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

  private fun updateJungleFortressCombat(
    dt: Float,
    playerX: Float,
    playerY: Float,
    weapons: EnemyWeaponSystem,
  ) {
    playNewModuleSfx()
    if (entering) {
      weapons.resetStage4Boss()
      return
    }
    val leftMortar = parts[TYPE_STAGE4_LEFT_MORTAR]
    val rightMortar = parts[TYPE_STAGE4_RIGHT_MORTAR]
    val gatling = parts[TYPE_STAGE4_HEAVY_GATLING]
    val leftDead = leftMortar.isDestroyed || leftMortar.halfW <= 0f
    val rightDead = rightMortar.isDestroyed || rightMortar.halfW <= 0f
    val gatlingDead = gatling.isDestroyed || gatling.halfW <= 0f
    weapons.updateStage4Boss(
      dt,
      coreX,
      coreY,
      bodyHalfW * 2f,
      bodyHalfH,
      playerX,
      playerY,
      leftDead,
      rightDead,
      gatlingDead,
    )
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

  private fun updateWinterKeepCombat(
    dt: Float,
    playerX: Float,
    playerY: Float,
    weapons: EnemyWeaponSystem,
  ) {
    playNewModuleSfx()
    if (entering) {
      weapons.resetStage7Boss()
      return
    }
    val leftGun = parts[TYPE_WINTER_LEFT_HOWITZER]
    val rightGun = parts[TYPE_WINTER_RIGHT_HOWITZER]
    val blizzard = parts[TYPE_WINTER_BLIZZARD]
    val leftDead = leftGun.isDestroyed || leftGun.halfW <= 0f
    val rightDead = rightGun.isDestroyed || rightGun.halfW <= 0f
    val blizzardDead = blizzard.isDestroyed || blizzard.halfW <= 0f
    weapons.updateStage7Boss(
      dt,
      coreX,
      coreY,
      bodyHalfW * 2f,
      bodyHalfH,
      playerX,
      playerY,
      leftDead,
      rightDead,
      blizzardDead,
    )
    if (isCoreVulnerable()) {
      s7RingAngle += S7_RING_SPIN * dt
      s7RingTimer -= dt
      if (s7RingTimer <= 0f) {
        s7RingTimer = S7_RING_INTERVAL
        val ox = coreX
        val oy = coreY
        val spd = S7_RING_SPEED
        var k = 0
        while (k < S7_RING_COUNT) {
          val ang = s7RingAngle + k * S7_RING_STEP
          weapons.fireBullet(ox, oy, cos(ang) * spd, sin(ang) * spd)
          k++
        }
      }
    }
  }

  private fun updateTankCombat(dt: Float, playerX: Float, playerY: Float, weapons: EnemyWeaponSystem) {
    playNewModuleSfx()
    if (entering) {
      weapons.resetStage2Boss()
      return
    }
    val turret = parts[S2_PART_MAIN_TURRET]
    val leftTread = parts[TYPE_STAGE2_LEFT_TREAD]
    val rightTread = parts[TYPE_STAGE2_RIGHT_TREAD]
    val turretDead = turret.isDestroyed || turret.halfW <= 0f
    val leftDead = leftTread.isDestroyed || leftTread.halfW <= 0f
    val rightDead = rightTread.isDestroyed || rightTread.halfW <= 0f
    weapons.updateStage2Boss(
      dt,
      coreX,
      coreY,
      bodyHalfW * 2f,
      playerX,
      playerY,
      leftDead,
      rightDead,
      turretDead,
    )
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

  private fun layoutOffsets() {
    val preserve = active
    if (preserve) {
      var i = 0
      while (i < MAX_PART_COUNT) {
        preservedDestroyed[i] = parts[i].isDestroyed
        preservedHealth[i] = parts[i].health
        i++
      }
    }
    var idx = 0
    while (idx < MAX_PART_COUNT) {
      disablePart(idx)
      idx++
    }
    when (combatKind) {
      BossCombatKind.ORBIT -> {
        val bw = bodyHalfW * 2f
        val bh = bodyHalfH * 2f
        setupPart(
          TYPE_CORE,
          (S6_CORE_L + S6_CORE_R) * 0.5f * bw,
          (S6_CORE_T + S6_CORE_B) * 0.5f * bh,
          (S6_CORE_R - S6_CORE_L) * 0.5f * bw,
          (S6_CORE_B - S6_CORE_T) * 0.5f * bh,
          S6_CORE_HP,
        )
        setupPart(
          TYPE_LEFT_FLANK,
          (S6_LEFT_L + S6_LEFT_R) * 0.5f * bw,
          (S6_FLANK_T + S6_FLANK_B) * 0.5f * bh,
          (S6_LEFT_R - S6_LEFT_L) * 0.5f * bw,
          (S6_FLANK_B - S6_FLANK_T) * 0.5f * bh,
          S6_FLANK_HP,
        )
        setupPart(
          TYPE_RIGHT_FLANK,
          (S6_RIGHT_L + S6_RIGHT_R) * 0.5f * bw,
          (S6_FLANK_T + S6_FLANK_B) * 0.5f * bh,
          (S6_RIGHT_R - S6_RIGHT_L) * 0.5f * bw,
          (S6_FLANK_B - S6_FLANK_T) * 0.5f * bh,
          S6_FLANK_HP,
        )
        syncStage5Hitboxes()
      }
      BossCombatKind.CANOPY -> {
        val bw = bodyHalfW * 2f
        val bh = bodyHalfH * 2f
        setupPart(
          TYPE_CORE,
          (S5_CORE_L + S5_CORE_R) * 0.5f * bw,
          (S5_CORE_T + S5_CORE_B) * 0.5f * bh,
          (S5_CORE_R - S5_CORE_L) * 0.5f * bw,
          (S5_CORE_B - S5_CORE_T) * 0.5f * bh,
          S5_CORE_HP,
        )
        setupPart(
          TYPE_LEFT_FLANK,
          (S5_LEFT_L + S5_LEFT_R) * 0.5f * bw,
          (S5_FLANK_T + S5_FLANK_B) * 0.5f * bh,
          (S5_LEFT_R - S5_LEFT_L) * 0.5f * bw,
          (S5_FLANK_B - S5_FLANK_T) * 0.5f * bh,
          S5_FLANK_HP,
        )
        setupPart(
          TYPE_RIGHT_FLANK,
          (S5_RIGHT_L + S5_RIGHT_R) * 0.5f * bw,
          (S5_FLANK_T + S5_FLANK_B) * 0.5f * bh,
          (S5_RIGHT_R - S5_RIGHT_L) * 0.5f * bw,
          (S5_FLANK_B - S5_FLANK_T) * 0.5f * bh,
          S5_FLANK_HP,
        )
        syncStage5Hitboxes()
      }
      BossCombatKind.WINTER -> {
        setupPart(TYPE_CORE, 0f, 0f, bodyHalfW * 0.28f, bodyHalfH * 0.38f, S7_CORE_HP)
        setupPart(
          TYPE_WINTER_LEFT_HOWITZER,
          -bodyHalfW * 0.70f,
          -bodyHalfH * 0.06f,
          bodyHalfW * 0.20f,
          bodyHalfH * 0.24f,
          S7_HOWITZER_HP,
        )
        setupPart(
          TYPE_WINTER_RIGHT_HOWITZER,
          bodyHalfW * 0.70f,
          -bodyHalfH * 0.06f,
          bodyHalfW * 0.20f,
          bodyHalfH * 0.24f,
          S7_HOWITZER_HP,
        )
        setupPart(
          TYPE_WINTER_BLIZZARD,
          0f,
          bodyHalfH * 0.52f,
          bodyHalfW * 0.24f,
          bodyHalfH * 0.24f,
          S7_BLIZZARD_HP,
        )
      }
      BossCombatKind.JUNGLE -> {
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
      }
      BossCombatKind.BATTLESHIP -> {
        setupPart(TYPE_CORE, 0f, -bodyHalfH * 0.1f, bodyHalfW * 0.35f, bodyHalfH * 0.40f, S3_CORE_HP)
        setupPart(TYPE_STAGE3_LEFT_FLAK, -bodyHalfW * 0.70f, bodyHalfH * 0.15f, bodyHalfW * 0.20f, bodyHalfH * 0.20f, S3_FLAK_HP)
        setupPart(TYPE_STAGE3_RIGHT_FLAK, bodyHalfW * 0.70f, bodyHalfH * 0.15f, bodyHalfW * 0.20f, bodyHalfH * 0.20f, S3_FLAK_HP)
        setupPart(TYPE_STAGE3_MEGA_CANNON, 0f, bodyHalfH * 0.45f, bodyHalfW * 0.30f, bodyHalfH * 0.25f, S3_CANNON_HP)
      }
      BossCombatKind.TANK -> {
        setupPart(TYPE_CORE, 0f, 0f, bodyHalfW * 0.28f, bodyHalfH * 0.38f, S2_CORE_HP)
        setupPart(TYPE_STAGE2_LEFT_TREAD, -bodyHalfW * 0.72f, 0f, bodyHalfW * 0.26f, bodyHalfH * 0.52f, S2_TREAD_HP)
        setupPart(TYPE_STAGE2_RIGHT_TREAD, bodyHalfW * 0.72f, 0f, bodyHalfW * 0.26f, bodyHalfH * 0.52f, S2_TREAD_HP)
        setupPart(TYPE_STAGE2_MAIN_TURRET, 0f, bodyHalfH * 0.18f, bodyHalfW * 0.16f, bodyHalfH * 0.28f, S2_TURRET_HP)
      }
      else -> {
        setupPart(TYPE_CORE, 0f, 0f, bodyHalfW * 0.28f, bodyHalfH * 0.52f, S1_CORE_HP)
        setupPart(TYPE_LEFT_WING, -bodyHalfW * 0.5f, -bodyHalfH * 0.08f, bodyHalfW * 0.30f, bodyHalfH * 0.20f, S1_WING_HP)
        setupPart(TYPE_RIGHT_WING, bodyHalfW * 0.5f, -bodyHalfH * 0.08f, bodyHalfW * 0.30f, bodyHalfH * 0.20f, S1_WING_HP)
        setupPart(TYPE_TURRET, 0f, bodyHalfH * 0.46f, bodyHalfW * 0.12f, bodyHalfW * 0.12f, S1_TURRET_HP)
      }
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
    val kit = StageCatalog.get(stage)
    val assets = resources.assets
    bodySheet = StageBitmaps.decode(assets, kit.bossBodyPath(), keyed = true)
    leftWreckSheet = StageBitmaps.decode(assets, kit.wreckLeftPath(), keyed = true)
    rightWreckSheet = StageBitmaps.decode(assets, kit.wreckRightPath(), keyed = true)
    centerWreckSheet = StageBitmaps.decode(assets, kit.wreckCenterPath(), keyed = true)
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
    const val FX_PHASE = 1
    const val FX_DEATH = 2
    const val FX_VICTORY_START = 4
    const val FX_VICTORY_CASCADE = 8
    const val FX_VICTORY_SHATTER = 16
    const val TYPE_LEFT_WING = 1
    const val TYPE_RIGHT_WING = 2
    const val TYPE_LEFT_FLANK = 1
    const val TYPE_RIGHT_FLANK = 2
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
    const val TYPE_WINTER_LEFT_HOWITZER = 10
    const val TYPE_WINTER_RIGHT_HOWITZER = 11
    const val TYPE_WINTER_BLIZZARD = 12
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
    const val S1_WING_HP = 70
    const val S1_TURRET_HP = 58
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
    const val S2_TREAD_HP = 100
    const val S2_TURRET_HP = 88
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
    const val S3_FLAK_HP = 125
    const val S3_CANNON_HP = 190
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
    const val S4_CORE_HP = 520
    const val S4_MORTAR_HP = 140
    const val S4_GATLING_HP = 160
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
    const val S7_CORE_HP = 500
    const val S7_HOWITZER_HP = 150
    const val S7_BLIZZARD_HP = 170
    const val S7_RING_INTERVAL = 0.20f
    const val S7_RING_SPEED = 280f
    const val S7_RING_SPIN = 2.8f
    const val S7_RING_COUNT = 8
    const val S7_RING_STEP = (Math.PI * 2.0 / S7_RING_COUNT).toFloat()
    const val S5_CORE_HP = 350
    const val S5_FLANK_HP = 180
    const val S5_LEFT_L = -0.50f
    const val S5_LEFT_R = -0.18f
    const val S5_RIGHT_L = 0.18f
    const val S5_RIGHT_R = 0.50f
    const val S5_FLANK_T = -0.35f
    const val S5_FLANK_B = 0.35f
    const val S5_CORE_L = -0.18f
    const val S5_CORE_R = 0.18f
    const val S5_CORE_T = -0.45f
    const val S5_CORE_B = 0.48f
    const val S6_CORE_HP = 380
    const val S6_FLANK_HP = 190
    const val S6_LEFT_L = -0.50f
    const val S6_LEFT_R = -0.20f
    const val S6_RIGHT_L = 0.20f
    const val S6_RIGHT_R = 0.50f
    const val S6_FLANK_T = -0.38f
    const val S6_FLANK_B = 0.38f
    const val S6_CORE_L = -0.20f
    const val S6_CORE_R = 0.20f
    const val S6_CORE_T = -0.48f
    const val S6_CORE_B = 0.48f
    const val S6_LEFT_MUZZLE_X = -0.44f
    const val S6_LEFT_MUZZLE_Y = 0.38f
    const val S6_RIGHT_MUZZLE_X = 0.44f
    const val S6_RIGHT_MUZZLE_Y = 0.38f
    const val S6_CORE_LENS_X = 0f
    const val S6_CORE_LENS_Y = -0.02f
    const val S6_PHASE2_CLOCK = 15.1f
    const val S6_PHASE3_CLOCK = 35.1f
    const val VICTORY_CASCADE_START = 0.5f
    const val VICTORY_SHATTER_AT = 3.0f
    const val VICTORY_END = 4.5f
    const val VICTORY_CASCADE_STEP = 0.15f
    const val VICTORY_CLUSTER = 8
    const val EXPLODE_FRAME_COUNT = 8
    const val EXPLODE_FRAME_SEC = 0.13f
    const val EXPLODE_HIDE_BODY_FRAME = 3
    const val SHADOW_PX = 2
    const val OUTLINE_PX = 3
  }
}
