package com.cc.ww2blitz

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
      fireCooldownTimer = player.vulcanInterval()
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
        bullet.y += bullet.vy * dt
        if (bullet.y < 0f || bullet.x < -40f || bullet.x > maxX) {
          bullet.isActive = false
        }
      }
      i++
    }
  }

  private fun spawnWeaponStream(player: PlayerShip) {
    val sx = player.getHitboxX()
    val sy = player.getHitboxY()
    if (player.chosenFighterIndex == 1) {
      spawnPlayerBullet(sx, sy - 15f, 0f, HELLCAT_CENTER_VY)
      spawnPlayerBullet(sx, sy - 15f, -HELLCAT_FLANK_VX, HELLCAT_FLANK_VY)
      spawnPlayerBullet(sx, sy - 15f, HELLCAT_FLANK_VX, HELLCAT_FLANK_VY)
    } else {
      spawnPlayerBullet(sx - P38_WING_OFFSET_X, sy - P38_MUZZLE_OFFSET_Y, 0f, P38_VY)
      spawnPlayerBullet(sx + P38_WING_OFFSET_X, sy - P38_MUZZLE_OFFSET_Y, 0f, P38_VY)
    }
    SoundManager.instance.playSFX(SoundManager.SFX_VULCAN)
  }

  fun spawnPlayerBullet(x: Float, y: Float, vx: Float, vy: Float) {
    var i = 0
    while (i < poolSize) {
      val bullet = bulletPool[i]
      if (!bullet.isActive) {
        bullet.x = x
        bullet.y = y
        bullet.vx = vx
        bullet.vy = vy
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
   * Euclidean core vs graze vs [EnemyBullet] centers. No RectF.intersects on the physical path.
   * @return true if [PlayerShip.takeDamage] destroyed the ship this frame.
   */
  fun resolveEnemyBulletsVsPlayer(
    player: PlayerShip,
    enemyBullets: Array<EnemyBullet>,
    enemyBulletCount: Int,
    particles: ParticleManager,
    awardScore: Boolean,
  ): Boolean {
    if (!player.isOnField()) return false
    val px = player.centerX()
    val py = player.centerY()
    val coreR = player.coreHitboxRadius
    val grazeR = player.grazeRadius
    val coreSq = coreR * coreR
    val grazeSq = grazeR * grazeR
    var i = 0
    while (i < enemyBulletCount) {
      val b = enemyBullets[i]
      if (b.isActive) {
        val dx = b.x - px
        val dy = b.y - py
        if ((b.flags and EnemyBullet.FLAG_LASER) != 0) {
          val hw = EnemyWeaponSystem.S6_LASER_HW + coreR
          val hh = EnemyWeaponSystem.S6_LASER_HH + coreR
          if (dx <= hw && dx >= -hw && dy <= hh && dy >= -hh) {
            b.isActive = false
            b.flags = 0
            if (player.takeDamage()) {
              particles.triggerExplosion(px, py)
              return true
            }
            return false
          }
        } else {
          val distSq = (dx * dx) + (dy * dy)
          if (distSq <= coreSq) {
            b.isActive = false
            b.flags = 0
            if (player.takeDamage()) {
              particles.triggerExplosion(px, py)
              return true
            }
            return false
          }
          if (distSq <= grazeSq && (b.flags and EnemyBullet.FLAG_GRAZED) == 0) {
            b.flags = b.flags or EnemyBullet.FLAG_GRAZED
            if (awardScore) {
              ScoreManager.instance.addGrazeScore(ScoreManager.GRAZE_POINTS)
            }
            var sparkVx = 0f
            var sparkVy = SPARK_FALLBACK_VY
            if (distSq > 0.0001f) {
              val inv = SPARK_SPEED / kotlin.math.sqrt(distSq)
              sparkVx = dx * inv
              sparkVy = dy * inv
            }
            particles.triggerSpark(b.x, b.y, sparkVx, sparkVy)
          }
        }
      }
      i++
    }
    return false
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
    const val HALF_BULLET_WIDTH = 6f
    const val HALF_BULLET_HEIGHT = 16f
    const val MISSILE_INTERVAL = 0.480f
    const val SPARK_SPEED = 280f
    const val SPARK_FALLBACK_VY = -280f
    const val P38_WING_OFFSET_X = 18f
    const val P38_MUZZLE_OFFSET_Y = 10f
    const val P38_VY = -1600f
    const val HELLCAT_CENTER_VY = -1350f
    const val HELLCAT_FLANK_VX = 280.68f
    const val HELLCAT_FLANK_VY = -1320.55f
  }
}
