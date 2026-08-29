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
  private val fireRateInterval = 0.120f // 120ms delay between shots (Strikers classic speed)

  // Reusable drawing variables to achieve absolute zero allocation in the loop
  private val bulletPaint = Paint().apply {
    color = Color.YELLOW // Retro arcade bright yellow projectiles
    style = Paint.Style.FILL
  }

  // High-performance float bounding box container used for canvas drawing
  private val drawRect = RectF()

  /**
   * Updates the firing cooldown, spawns dual streams if held,
   * moves projectiles, and recycles off-screen bullets.
   */
  fun update(dt: Float, player: PlayerShip) {
    // 1. Process Automatic Machine Gun Firing Cooldown
    if (fireCooldownTimer > 0f) {
      fireCooldownTimer -= dt
    }

    // 2. If the user is touching the screen, handle continuous bullet generation
    if (player.isFiringHeld() && fireCooldownTimer <= 0f) {
      spawnDualBulletStream(player)
      fireCooldownTimer = fireRateInterval // Reset firing timer clock
    }

    // 3. Update active physics and clear out bound structures
    for (i in 0 until poolSize) {
      val bullet = bulletPool[i]
      if (bullet.isActive) {
        // Move bullet rapidly up the screen (negative Y axis)
        bullet.y -= BULLET_SPEED_PX_PER_SEC * dt

        // If bullet travels completely past the top display line, recycle it immediately
        if (bullet.y < 0f) {
          bullet.isActive = false
        }
      }
    }
  }

  /**
   * Loops through the object pool to find two inactive objects,
   * assigning them to start traveling from the left and right wing tips simultaneously.
   */
  private fun spawnDualBulletStream(player: PlayerShip) {
    var leftSpawned = false
    var rightSpawned = false

    // Look for the first two available inactive objects inside our pre-allocated array pool
    for (i in 0 until poolSize) {
      val bullet = bulletPool[i]
      if (!bullet.isActive) {
        if (!leftSpawned) {
          bullet.x = player.leftMuzzleX()
          bullet.y = player.muzzleY()
          bullet.isActive = true
          leftSpawned = true
          continue // Skip to look for the right wing container asset
        }
        if (!rightSpawned) {
          bullet.x = player.rightMuzzleX()
          bullet.y = player.muzzleY()
          bullet.isActive = true
          rightSpawned = true
        }
      }
      // Once both projectiles are securely decoupled out of inventory, exit immediately
      if (leftSpawned && rightSpawned) break
    }
  }

  fun getBulletPool(): Array<PlayerBullet> = bulletPool

  fun getPoolSize(): Int = poolSize

  /**
   * Iterates through your active projectile states, mapping points directly
   * to a single float drawRect template variable to keep memory generation completely clean.
   */
  fun draw(canvas: Canvas) {
    for (i in 0 until poolSize) {
      val bullet = bulletPool[i]
      if (bullet.isActive) {
        // Configure our zero-allocation primitive geometric coordinate layout window
        drawRect.set(
          bullet.x - HALF_BULLET_WIDTH,
          bullet.y - HALF_BULLET_HEIGHT,
          bullet.x + HALF_BULLET_WIDTH,
          bullet.y + HALF_BULLET_HEIGHT
        )

        // Draw high-performance hardware vector bullets onto the running environment
        canvas.drawRoundRect(drawRect, HALF_BULLET_WIDTH, HALF_BULLET_WIDTH, bulletPaint)
      }
    }
  }

  private companion object {
    // High velocity standard arcade machine gun travel calculations
    const val BULLET_SPEED_PX_PER_SEC = 1600f

    // Classic slender 1990s Psikyo bullet profiles (Adjust for larger/smaller scale)
    const val HALF_BULLET_WIDTH = 6f
    const val HALF_BULLET_HEIGHT = 16f
  }
}
