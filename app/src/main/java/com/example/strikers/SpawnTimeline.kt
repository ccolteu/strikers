package com.example.strikers

/**
 * Pre-ordered wave cues. [update] only advances a cursor; the event arrays
 * are allocated once at construction to prevent GC stutter in Grok/Cursor builds.
 */
class SpawnTimeline {

  private val stage1Events = arrayOf(
    SpawnEvent(1.5f, 0.25f, 0f, 0f, FAST_DOWN, TYPE_DRONE),
    SpawnEvent(1.7f, 0.50f, 0f, 0f, FAST_DOWN, TYPE_DRONE),
    SpawnEvent(1.9f, 0.75f, 0f, 0f, FAST_DOWN, TYPE_DRONE),
    SpawnEvent(4.0f, -0.05f, -0.05f, SWEEP_VX, SWEEP_VY, TYPE_DRONE),
    SpawnEvent(4.3f, -0.12f, -0.15f, SWEEP_VX, SWEEP_VY, TYPE_DRONE),
    SpawnEvent(4.6f, -0.19f, -0.25f, SWEEP_VX, SWEEP_VY, TYPE_DRONE),
    SpawnEvent(8.0f, 0.20f, 0f, 0f, FAST_DOWN, TYPE_DRONE),
    SpawnEvent(8.2f, 0.40f, 0f, 0f, FAST_DOWN, TYPE_DRONE),
    SpawnEvent(8.4f, 0.60f, 0f, 0f, FAST_DOWN, TYPE_DRONE),
    SpawnEvent(8.6f, 0.80f, 0f, 0f, FAST_DOWN, TYPE_DRONE),
    SpawnEvent(12.0f, 1.05f, -0.05f, -SWEEP_VX, SWEEP_VY, TYPE_DRONE),
    SpawnEvent(12.3f, 1.12f, -0.15f, -SWEEP_VX, SWEEP_VY, TYPE_DRONE),
    SpawnEvent(12.6f, 1.19f, -0.25f, -SWEEP_VX, SWEEP_VY, TYPE_DRONE),
    SpawnEvent(17.0f, 0.30f, 0f, 0f, FAST_DOWN, TYPE_DRONE),
    SpawnEvent(17.25f, 0.50f, 0f, 0f, FAST_DOWN, TYPE_DRONE),
    SpawnEvent(17.5f, 0.70f, 0f, 0f, FAST_DOWN, TYPE_DRONE),
    SpawnEvent(21.0f, -0.05f, -0.08f, SWEEP_VX, SWEEP_VY, TYPE_DRONE),
    SpawnEvent(21.2f, 1.05f, -0.08f, -SWEEP_VX, SWEEP_VY, TYPE_DRONE),
    SpawnEvent(21.4f, -0.12f, -0.18f, SWEEP_VX, SWEEP_VY, TYPE_DRONE),
    SpawnEvent(21.6f, 1.12f, -0.18f, -SWEEP_VX, SWEEP_VY, TYPE_DRONE),
  )

  private val stage2Events = arrayOf(
    // === FORMATION 1: THE PINCER ARROWHEAD CROSS ===
    // Sweeps from top-left diagonally down-right, and top-right diagonally down-left
    SpawnEvent(1.0f, -0.05f, -0.05f, SWEEP_VX, SWEEP_VY, TYPE_DRONE),
    SpawnEvent(1.0f, 1.05f, -0.05f, -SWEEP_VX, SWEEP_VY, TYPE_DRONE),

    SpawnEvent(1.4f, -0.10f, -0.10f, SWEEP_VX, SWEEP_VY, TYPE_DRONE),
    SpawnEvent(1.4f, 1.10f, -0.10f, -SWEEP_VX, SWEEP_VY, TYPE_DRONE),

    SpawnEvent(1.8f, -0.15f, -0.15f, SWEEP_VX, SWEEP_VY, TYPE_DRONE),
    SpawnEvent(1.8f, 1.15f, -0.15f, -SWEEP_VX, SWEEP_VY, TYPE_DRONE),

    // === FORMATION 2: THE KAMIKAZE V-FORMATION ===
    // Six aggressive suicide drones forming a perfect flying wedge downward
    SpawnEvent(6.0f, 0.50f, -0.05f, 0f, KAMI_VY_FAST, TYPE_KAMIKAZE), // The Tip

    SpawnEvent(6.4f, 0.38f, -0.05f, KAMI_VX, KAMI_VY, TYPE_KAMIKAZE),  // Left wing tier 1
    SpawnEvent(6.4f, 0.62f, -0.05f, -KAMI_VX, KAMI_VY, TYPE_KAMIKAZE), // Right wing tier 1

    SpawnEvent(6.8f, 0.26f, -0.05f, KAMI_VX_FAST, KAMI_VY, TYPE_KAMIKAZE),  // Left wing tier 2
    SpawnEvent(6.8f, 0.74f, -0.05f, -KAMI_VX_FAST, KAMI_VY, TYPE_KAMIKAZE), // Right wing tier 2

    SpawnEvent(7.2f, 0.50f, -0.10f, 0f, KAMI_VY_FAST, TYPE_KAMIKAZE), // Rear anchor slot

    // === FORMATION 3: THE ALTERNATING FLANK WALLS ===
    // Left side horizontal block, then right side horizontal block
    SpawnEvent(14.0f, 0.10f, -0.05f, 0f, FAST_DOWN, TYPE_DRONE),
    SpawnEvent(14.2f, 0.22f, -0.05f, 0f, FAST_DOWN, TYPE_DRONE),
    SpawnEvent(14.4f, 0.34f, -0.05f, 0f, FAST_DOWN, TYPE_DRONE),
    SpawnEvent(14.6f, 0.46f, -0.05f, 0f, FAST_DOWN, TYPE_DRONE),

    SpawnEvent(17.0f, 0.90f, -0.05f, 0f, FAST_DOWN, TYPE_DRONE),
    SpawnEvent(17.2f, 0.78f, -0.05f, 0f, FAST_DOWN, TYPE_DRONE),
    SpawnEvent(17.4f, 0.66f, -0.05f, 0f, FAST_DOWN, TYPE_DRONE),
    SpawnEvent(17.6f, 0.54f, -0.05f, 0f, FAST_DOWN, TYPE_DRONE),

    // === FORMATION 4: THE INFINITY LOOP CROSS ===
    // Interceptors rising quickly from the bottom sides to catch players resting below
    SpawnEvent(22.0f, -0.08f, 0.85f, KAMI_VX_FAST, -KAMI_VY, TYPE_DRONE),
    SpawnEvent(22.0f, 1.08f, 0.85f, -KAMI_VX_FAST, -KAMI_VY, TYPE_DRONE),

    SpawnEvent(22.4f, -0.08f, 0.70f, KAMI_VX_FAST, -KAMI_VY_FAST, TYPE_DRONE),
    SpawnEvent(22.4f, 1.08f, 0.70f, -KAMI_VX_FAST, -KAMI_VY_FAST, TYPE_DRONE),

    // Final pre-boss warning drones
    SpawnEvent(26.0f, 0.30f, -0.05f, 0f, FAST_DOWN, TYPE_DRONE),
    SpawnEvent(26.5f, 0.50f, -0.05f, 0f, FAST_DOWN, TYPE_DRONE),
    SpawnEvent(26.7f, 0.80f, -0.05f, 0f, FAST_DOWN, TYPE_DRONE),
  )

  private var elapsedTime = 0f
  private var bossCueFired = false
  private var nextIndex = 0

  fun update(
    dt: Float,
    enemyManager: EnemyPoolManager,
    screenWidth: Int,
    screenHeight: Int,
    boss: BossController,
    currentStage: Int,
    bossEnterSeconds: Float,
  ) {
    if (screenWidth <= 0 || screenHeight <= 0) return
    elapsedTime += dt
    val w = screenWidth.toFloat()
    val h = screenHeight.toFloat()
    val events = if (currentStage >= 2) stage2Events else stage1Events
    while (nextIndex < events.size) {
      val cue = events[nextIndex]
      if (cue.timestampSeconds > elapsedTime) break
      enemyManager.spawnEnemy(
        cue.spawnXFraction * w,
        cue.spawnYFraction * h,
        cue.velocityX,
        cue.velocityY,
        cue.enemyType,
      )
      nextIndex++
    }
    if (!bossCueFired && elapsedTime >= bossEnterSeconds) {
      boss.beginEntranceForStage(currentStage)
      bossCueFired = true
    }
  }

  fun reset() {
    elapsedTime = 0f
    bossCueFired = false
    nextIndex = 0
  }

  private companion object {
    const val TYPE_DRONE = 0
    const val TYPE_KAMIKAZE = 1
    const val FAST_DOWN = 320f
    const val SWEEP_VX = 210f
    const val SWEEP_VY = 240f
    const val KAMI_VX = 260f
    const val KAMI_VY = 420f
    const val KAMI_VX_FAST = 340f
    const val KAMI_VY_FAST = 520f
  }
}
