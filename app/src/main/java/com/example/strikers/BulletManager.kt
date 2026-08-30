package com.example.strikers

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF

class BulletManager {

  // Pre-allocated fixed-size array acting as our object pool
  private val poolSize = 100
  private val bulletPool = Array(poolSize) { PlayerBullet() }

  // Automatic machine gun firing timer parameters
  private var fireCooldownTimer = 0f
  private val fireRateInterval = 0.120f
  private var missileCooldown = 0f
  private var spawnedStream = false

  // Reusable drawing variables to achieve absolute zero allocation in the loop
  private val bulletPaint = Paint().apply {
    color = Color.YELLOW
    style = Paint.Style.FILL
  }

  // High-performance float bounding box container used for canvas drawing
  private val drawRect = RectF()

  /**
   * Updates vulcan cooldown independently of the homing-missile cadence.
   */
  fun update(dt: Float, player: PlayerShip, screenW: Int, homingMissiles: HomingMissileManager) {
    spawnedStream = false
    if (fireCooldownTimer > 0f) {
      fireCooldownTimer -= dt
    }
    if (missileCooldown > 0f) {
      missileCooldown -= dt
    }

    val held = player.isFiringHeld()
    if (held && fireCooldownTimer <= 0f) {
      spawnWeaponStream(player)
      spawnedStream = true
      fireCooldownTimer = fireRateInterval
    }
    if (
      player.getWeaponPower() >= 3 &&
      held &&
      missileCooldown <= 0f
    ) {
      homingMissiles.fireMissile(player.getHitboxX() - 30f, player.getHitboxY(), -150f, -400f)
      homingMissiles.fireMissile(player.getHitboxX() + 30f, player.getHitboxY(), 150f, -400f)
      missileCooldown = MISSILE_INTERVAL
    }

    val maxX = screenW + 40f
    var i = 0
    while (i < poolSize) {
      val bullet = bulletPool[i]
      if (bullet.isActive) {
        bullet.x += bullet.vx * dt
        bullet.y -= BULLET_SPEED_PX_PER_SEC * dt
        if (bullet.y < 0f || bullet.x < -40f || bullet.x > maxX) {
          bullet.isActive = false
        }
      }
      i++
    }
  }

  private fun spawnWeaponStream(player: PlayerShip) {
    val my = player.muzzleY()
    val powerLevel = player.getWeaponPower()

    when (powerLevel) {
      2 -> {
        spawnBullet(player.getHitboxX(), my, 0f)
        spawnBullet(player.leftMuzzleX(), my, LEVEL2_SPREAD_VX)
        spawnBullet(player.rightMuzzleX(), my, -LEVEL2_SPREAD_VX)
      }
      3 -> {
        spawnBullet(player.muzzleXAt(-LANE3_OUTER), my, 0f)
        spawnBullet(player.muzzleXAt(-LANE3_INNER), my, 0f)
        spawnBullet(player.muzzleXAt(LANE3_INNER), my, 0f)
        spawnBullet(player.muzzleXAt(LANE3_OUTER), my, 0f)
      }
      else -> {
        spawnBullet(player.leftMuzzleX(), my, 0f)
        spawnBullet(player.rightMuzzleX(), my, 0f)
      }
    }
    SoundManager.instance.playSFX(SoundManager.SFX_VULCAN)
  }

  private fun spawnBullet(x: Float, y: Float, vx: Float) {
    var i = 0
    while (i < poolSize) {
      val bullet = bulletPool[i]
      if (!bullet.isActive) {
        bullet.x = x
        bullet.y = y
        bullet.vx = vx
        bullet.isActive = true
        return
      }
      i++
    }
  }

  fun getBulletPool(): Array<PlayerBullet> = bulletPool

  fun getPoolSize(): Int = poolSize

  fun didSpawnStream(): Boolean = spawnedStream

  fun deactivateAll() {
    var i = 0
    while (i < poolSize) {
      bulletPool[i].isActive = false
      i++
    }
  }

  /**
   * Iterates through your active projectile states, mapping points directly
   * to a single float drawRect template variable to keep memory generation completely clean.
   */
  fun draw(canvas: Canvas) {
    var i = 0
    while (i < poolSize) {
      val bullet = bulletPool[i]
      if (bullet.isActive) {
        drawRect.set(
          bullet.x - HALF_BULLET_WIDTH,
          bullet.y - HALF_BULLET_HEIGHT,
          bullet.x + HALF_BULLET_WIDTH,
          bullet.y + HALF_BULLET_HEIGHT,
        )
        canvas.drawRoundRect(drawRect, HALF_BULLET_WIDTH, HALF_BULLET_WIDTH, bulletPaint)
      }
      i++
    }
  }

  private companion object {
    const val BULLET_SPEED_PX_PER_SEC = 1600f
    const val HALF_BULLET_WIDTH = 6f
    const val HALF_BULLET_HEIGHT = 16f
    const val LEVEL2_SPREAD_VX = -150f
    const val LANE3_OUTER = 0.72f
    const val LANE3_INNER = 0.28f
    const val MISSILE_INTERVAL = 0.480f
  }
}
