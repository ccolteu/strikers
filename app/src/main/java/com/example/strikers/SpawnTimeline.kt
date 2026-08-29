package com.example.strikers

/**
 * Pre-ordered wave cues. [update] only advances a cursor; the event arrays
 * are allocated once at construction.
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
    SpawnEvent(1.0f, -0.08f, 0.12f, KAMI_VX, KAMI_VY, TYPE_KAMIKAZE),
    SpawnEvent(1.18f, 1.08f, 0.08f, -KAMI_VX, KAMI_VY, TYPE_KAMIKAZE),
    SpawnEvent(1.36f, -0.08f, 0.22f, KAMI_VX, KAMI_VY_FAST, TYPE_KAMIKAZE),
    SpawnEvent(1.54f, 1.08f, 0.18f, -KAMI_VX, KAMI_VY_FAST, TYPE_KAMIKAZE),
    SpawnEvent(1.72f, -0.08f, 0.05f, KAMI_VX_FAST, KAMI_VY, TYPE_KAMIKAZE),
    SpawnEvent(1.90f, 1.08f, 0.28f, -KAMI_VX_FAST, KAMI_VY, TYPE_KAMIKAZE),
    SpawnEvent(5.0f, -0.08f, 0.10f, KAMI_VX_FAST, KAMI_VY_FAST, TYPE_KAMIKAZE),
    SpawnEvent(5.12f, 1.08f, 0.10f, -KAMI_VX_FAST, KAMI_VY_FAST, TYPE_KAMIKAZE),
    SpawnEvent(5.24f, -0.08f, 0.25f, KAMI_VX, KAMI_VY, TYPE_KAMIKAZE),
    SpawnEvent(5.36f, 1.08f, 0.25f, -KAMI_VX, KAMI_VY, TYPE_KAMIKAZE),
    SpawnEvent(5.48f, -0.08f, 0.02f, KAMI_VX_FAST, KAMI_VY, TYPE_KAMIKAZE),
    SpawnEvent(5.60f, 1.08f, 0.40f, -KAMI_VX_FAST, KAMI_VY, TYPE_KAMIKAZE),
    SpawnEvent(10.0f, -0.08f, 0.08f, KAMI_VX, KAMI_VY_FAST, TYPE_KAMIKAZE),
    SpawnEvent(10.15f, 1.08f, 0.16f, -KAMI_VX, KAMI_VY_FAST, TYPE_KAMIKAZE),
    SpawnEvent(10.30f, -0.08f, 0.24f, KAMI_VX_FAST, KAMI_VY, TYPE_KAMIKAZE),
    SpawnEvent(10.45f, 1.08f, 0.08f, -KAMI_VX_FAST, KAMI_VY, TYPE_KAMIKAZE),
    SpawnEvent(10.60f, -0.08f, 0.32f, KAMI_VX, KAMI_VY, TYPE_KAMIKAZE),
    SpawnEvent(10.75f, 1.08f, 0.32f, -KAMI_VX, KAMI_VY, TYPE_KAMIKAZE),
    SpawnEvent(16.0f, -0.08f, 0.06f, KAMI_VX_FAST, KAMI_VY_FAST, TYPE_KAMIKAZE),
    SpawnEvent(16.12f, 1.08f, 0.06f, -KAMI_VX_FAST, KAMI_VY_FAST, TYPE_KAMIKAZE),
    SpawnEvent(16.24f, -0.08f, 0.20f, KAMI_VX, KAMI_VY_FAST, TYPE_KAMIKAZE),
    SpawnEvent(16.36f, 1.08f, 0.20f, -KAMI_VX, KAMI_VY_FAST, TYPE_KAMIKAZE),
    SpawnEvent(16.48f, -0.08f, 0.34f, KAMI_VX_FAST, KAMI_VY, TYPE_KAMIKAZE),
    SpawnEvent(16.60f, 1.08f, 0.34f, -KAMI_VX_FAST, KAMI_VY, TYPE_KAMIKAZE),
    SpawnEvent(22.0f, -0.08f, 0.10f, KAMI_VX_FAST, KAMI_VY_FAST, TYPE_KAMIKAZE),
    SpawnEvent(22.10f, 1.08f, 0.10f, -KAMI_VX_FAST, KAMI_VY_FAST, TYPE_KAMIKAZE),
    SpawnEvent(22.20f, -0.08f, 0.22f, KAMI_VX, KAMI_VY_FAST, TYPE_KAMIKAZE),
    SpawnEvent(22.30f, 1.08f, 0.22f, -KAMI_VX, KAMI_VY_FAST, TYPE_KAMIKAZE),
    SpawnEvent(22.40f, -0.08f, 0.34f, KAMI_VX_FAST, KAMI_VY, TYPE_KAMIKAZE),
    SpawnEvent(22.50f, 1.08f, 0.34f, -KAMI_VX_FAST, KAMI_VY, TYPE_KAMIKAZE),
    SpawnEvent(26.0f, -0.08f, 0.15f, KAMI_VX_FAST, KAMI_VY_FAST, TYPE_KAMIKAZE),
    SpawnEvent(26.15f, 1.08f, 0.15f, -KAMI_VX_FAST, KAMI_VY_FAST, TYPE_KAMIKAZE),
    SpawnEvent(26.30f, 0.20f, 0f, 0f, FAST_DOWN, TYPE_DRONE),
    SpawnEvent(26.50f, 0.50f, 0f, 0f, FAST_DOWN, TYPE_DRONE),
    SpawnEvent(26.70f, 0.80f, 0f, 0f, FAST_DOWN, TYPE_DRONE),
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
      boss.beginEntrance()
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
