package com.example.strikers

/**
 * Pre-ordered wave cues. [update] only advances a cursor; the event array
 * is allocated once at construction.
 */
class SpawnTimeline {

  private val events = arrayOf(
    SpawnEvent(1.5f, 0.25f, 0f, 0f, FAST_DOWN, TYPE_DRONE),
    SpawnEvent(1.7f, 0.50f, 0f, 0f, FAST_DOWN, TYPE_DRONE),
    SpawnEvent(1.9f, 0.75f, 0f, 0f, FAST_DOWN, TYPE_DRONE),
    SpawnEvent(4.0f, -0.05f, -0.05f, SWEEP_VX, SWEEP_VY, TYPE_DRONE),
    SpawnEvent(4.3f, -0.12f, -0.15f, SWEEP_VX, SWEEP_VY, TYPE_DRONE),
    SpawnEvent(4.6f, -0.19f, -0.25f, SWEEP_VX, SWEEP_VY, TYPE_DRONE),
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
  ) {
    if (screenWidth <= 0 || screenHeight <= 0) return
    elapsedTime += dt
    val w = screenWidth.toFloat()
    val h = screenHeight.toFloat()
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
    if (!bossCueFired && elapsedTime >= BOSS_ENTER_SEC) {
      boss.beginEntrance()
      bossCueFired = true
    }
  }

  private companion object {
    const val TYPE_DRONE = 0
    const val FAST_DOWN = 320f
    const val SWEEP_VX = 210f
    const val SWEEP_VY = 240f
    const val BOSS_ENTER_SEC = 8f
  }
}
