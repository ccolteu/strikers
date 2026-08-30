package com.example.strikers

/**
 * Stage 1 is driven by elapsed-time spawn loops (no per-frame alloc).
 * Stage 2 still uses a pre-ordered [SpawnEvent] cursor.
 */
class SpawnTimeline {

  private val stage2Events = arrayOf(
    // =========================================================================
    // PHASE 1: THE CROSSING FINGER-PINCER SWEEP (0.5s - 5.0s)
    // Fast scouts rushing from both top corners, staggered to weave between each other
    // =========================================================================
    SpawnEvent(0.5f, -0.05f, -0.05f, SWEEP_VX * 1.3f, SWEEP_VY * 1.1f, TYPE_DRONE),
    SpawnEvent(0.8f, 1.05f, -0.05f, -SWEEP_VX * 1.3f, SWEEP_VY * 1.1f, TYPE_DRONE),

    SpawnEvent(1.4f, -0.05f, -0.05f, SWEEP_VX * 1.3f, SWEEP_VY * 1.1f, TYPE_DRONE),
    SpawnEvent(1.7f, 1.05f, -0.05f, -SWEEP_VX * 1.3f, SWEEP_VY * 1.1f, TYPE_DRONE),

    SpawnEvent(2.3f, -0.05f, -0.05f, SWEEP_VX * 1.3f, SWEEP_VY * 1.1f, TYPE_DRONE),
    SpawnEvent(2.6f, 1.05f, -0.05f, -SWEEP_VX * 1.3f, SWEEP_VY * 1.1f, TYPE_DRONE),

    SpawnEvent(3.2f, -0.05f, -0.05f, SWEEP_VX * 1.3f, SWEEP_VY * 1.1f, TYPE_DRONE),
    SpawnEvent(3.5f, 1.05f, -0.05f, -SWEEP_VX * 1.3f, SWEEP_VY * 1.1f, TYPE_DRONE),

    // =========================================================================
    // PHASE 2: LOW-ALTITUDE ARMORED BOMBER ENTRANCE (6.5s - 11.0s)
    // Dual heavy bullet-vanguard ships drop down the outer flanks, while
    // fast weavers cross down the center line to split player focus
    // =========================================================================
    SpawnEvent(6.5f, 0.20f, -0.12f, 0f, HEAVY_VY * 1.2f, TYPE_HEAVY, 0),  // Left Flank Blockade
    SpawnEvent(6.5f, 0.80f, -0.12f, 0f, HEAVY_VY * 1.2f, TYPE_HEAVY, 0),  // Right Flank Blockade

    SpawnEvent(8.0f, 0.50f, -0.05f, 0f, WEAVE_VY * 1.4f, TYPE_DRONE, PATTERN_WEAVE),
    SpawnEvent(9.0f, 0.50f, -0.05f, 0f, WEAVE_VY * 1.4f, TYPE_DRONE, PATTERN_WEAVE),
    SpawnEvent(10.0f, 0.50f, -0.05f, 0f, WEAVE_VY * 1.4f, TYPE_DRONE, PATTERN_WEAVE),

    // =========================================================================
    // PHASE 3: SUICIDE DIAMOND BREAKTHROUGH (13.0s - 17.5s)
    // Interceptors holding in position at the top to fire area-denial spreads,
    // shielding a high-speed diamond of Kamikaze fighters bursting through the center
    // =========================================================================
    SpawnEvent(13.0f, 0.15f, -0.08f, 0f, INTERCEPT_VY, TYPE_INTERCEPTOR, PATTERN_V_HOLD), // Left Turret Guard
    SpawnEvent(13.0f, 0.85f, -0.08f, 0f, INTERCEPT_VY, TYPE_INTERCEPTOR, PATTERN_V_HOLD), // Right Turret Guard

    SpawnEvent(14.0f, 0.50f, -0.05f, 0f, KAMI_VY_FAST, TYPE_KAMIKAZE),           // Diamond Tip
    SpawnEvent(14.5f, 0.38f, -0.05f, KAMI_VX, KAMI_VY, TYPE_KAMIKAZE),           // Left Wing
    SpawnEvent(14.5f, 0.62f, -0.05f, -KAMI_VX, KAMI_VY, TYPE_KAMIKAZE),          // Right Wing
    SpawnEvent(15.0f, 0.50f, -0.05f, 0f, KAMI_VY_FAST, TYPE_KAMIKAZE),           // Diamond Tail

    // =========================================================================
    // PHASE 4: REAR-GUARD AMBUSH & STRIKE FORCES (20.0s - 25.5s)
    // Quick vertical drop walls from alternating sides to force fast horizontal movement,
    // ending with dual interlocking cross-weaving units right before the boss
    // =========================================================================
    SpawnEvent(20.0f, 0.10f, -0.05f, 0f, FAST_DOWN, TYPE_DRONE),
    SpawnEvent(20.3f, 0.30f, -0.05f, 0f, FAST_DOWN, TYPE_DRONE),
    SpawnEvent(20.6f, 0.50f, -0.05f, 0f, FAST_DOWN, TYPE_DRONE),

    SpawnEvent(22.0f, 0.90f, -0.05f, 0f, FAST_DOWN, TYPE_DRONE),
    SpawnEvent(22.3f, 0.70f, -0.05f, 0f, FAST_DOWN, TYPE_DRONE),
    SpawnEvent(22.6f, 0.50f, -0.05f, 0f, FAST_DOWN, TYPE_DRONE),

    // Final pre-boss crossing waves
    SpawnEvent(24.5f, 0.25f, -0.05f, 0f, WEAVE_VY * 1.5f, TYPE_DRONE, PATTERN_WEAVE),
    SpawnEvent(24.5f, 0.75f, -0.05f, 0f, WEAVE_VY * 1.5f, TYPE_DRONE, PATTERN_WEAVE),

    SpawnEvent(25.5f, 0.50f, -0.08f, 0f, INTERCEPT_VY, TYPE_INTERCEPTOR, PATTERN_V_HOLD) // Final heavy center guard
  )

  private var elapsedTime = 0f
  private var bossCueFired = false
  private var nextIndex = 0
  private var flankGap = 0f
  private var weaveGap = 0f
  private var vFormSpawned = false
  private var wallSpawned = false

  fun update(
    dt: Float,
    enemyManager: EnemyPoolManager,
    screenWidth: Int,
    screenHeight: Int,
    boss: BossController,
    currentStage: Int,
    bossEnterSeconds: Float,
    allowBoss: Boolean,
  ) {
    if (screenWidth <= 0 || screenHeight <= 0) return
    elapsedTime += dt
    val w = screenWidth.toFloat()
    val h = screenHeight.toFloat()
    if (currentStage < 2) {
      updateStage1(dt, enemyManager, w, h)
    } else {
      val events = stage2Events
      while (nextIndex < events.size) {
        val cue = events[nextIndex]
        if (cue.timestampSeconds > elapsedTime) break
        enemyManager.spawnEnemy(
          cue.spawnXFraction * w,
          cue.spawnYFraction * h,
          cue.velocityX,
          cue.velocityY,
          cue.enemyType,
          cue.pattern,
        )
        nextIndex++
      }
    }
    if (allowBoss && !bossCueFired && elapsedTime >= bossEnterSeconds) {
      boss.beginEntranceForStage(currentStage)
      bossCueFired = true
    }
  }

  fun reset() {
    elapsedTime = 0f
    bossCueFired = false
    nextIndex = 0
    flankGap = FLANK_SPACING
    weaveGap = 0f
    vFormSpawned = false
    wallSpawned = false
  }

  private fun updateStage1(
    dt: Float,
    enemies: EnemyPoolManager,
    w: Float,
    h: Float,
  ) {
    if (elapsedTime <= FLANK_END) {
      flankGap += dt
      var safeguard = 0
      while (flankGap >= FLANK_SPACING && safeguard < 3) {
        if (enemies.countActive() >= MAX_ACTIVE) break
        flankGap -= FLANK_SPACING
        safeguard++
        enemies.spawnEnemy(-0.06f * w, -0.06f * h, SWEEP_VX, SWEEP_VY, TYPE_DRONE)
        enemies.spawnEnemy(1.06f * w, -0.06f * h, -SWEEP_VX, SWEEP_VY, TYPE_DRONE)
      }
    }
    if (!vFormSpawned && elapsedTime >= V_FORM_AT) {
      if (enemies.countActive() < MAX_ACTIVE) {
        vFormSpawned = true
        spawnVFormation(enemies, w, h)
      }
    }
    if (!wallSpawned && elapsedTime >= WALL_AT) {
      if (enemies.countActive() < MAX_ACTIVE) {
        wallSpawned = true
        enemies.spawnEnemy(0.30f * w, -0.10f * h, 0f, HEAVY_VY, TYPE_HEAVY, 0, HEAVY_HP)
        enemies.spawnEnemy(0.70f * w, -0.10f * h, 0f, HEAVY_VY, TYPE_HEAVY, 0, HEAVY_HP)
        weaveGap = WEAVE_SPACING
      }
    }
    if (elapsedTime >= WALL_AT && elapsedTime <= WALL_END) {
      weaveGap += dt
      var safeguard = 0
      while (weaveGap >= WEAVE_SPACING && safeguard < 2) {
        if (enemies.countActive() >= MAX_ACTIVE) break
        weaveGap -= WEAVE_SPACING
        safeguard++
        enemies.spawnEnemy(0.30f * w, -0.02f * h, 0f, WEAVE_VY, TYPE_DRONE, PATTERN_WEAVE)
        enemies.spawnEnemy(0.70f * w, -0.02f * h, 0f, WEAVE_VY, TYPE_DRONE, PATTERN_WEAVE)
      }
    }
  }

  private fun spawnVFormation(enemies: EnemyPoolManager, w: Float, h: Float) {
    enemies.spawnEnemy(0.50f * w, -0.02f * h, 0f, INTERCEPT_VY, TYPE_INTERCEPTOR, PATTERN_V_HOLD, 2)
    enemies.spawnEnemy(0.38f * w, -0.10f * h, 0f, INTERCEPT_VY, TYPE_INTERCEPTOR, PATTERN_V_HOLD, 2)
    enemies.spawnEnemy(0.62f * w, -0.10f * h, 0f, INTERCEPT_VY, TYPE_INTERCEPTOR, PATTERN_V_HOLD, 2)
  }

  private companion object {
    const val TYPE_DRONE = 0
    const val TYPE_KAMIKAZE = 1
    const val TYPE_INTERCEPTOR = 2
    const val TYPE_HEAVY = 3
    const val PATTERN_WEAVE = 2
    const val PATTERN_V_HOLD = 1
    const val FAST_DOWN = 320f
    const val SWEEP_VX = 210f
    const val SWEEP_VY = 240f
    const val KAMI_VX = 260f
    const val KAMI_VY = 420f
    const val KAMI_VX_FAST = 340f
    const val KAMI_VY_FAST = 520f
    const val FLANK_END = 8f
    const val FLANK_SPACING = 0.75f
    const val V_FORM_AT = 12f
    const val WALL_AT = 24f
    const val WALL_END = 32f
    const val WEAVE_SPACING = 1.35f
    const val WEAVE_VY = 150f
    const val HEAVY_VY = 78f
    const val HEAVY_HP = 6
    const val INTERCEPT_VY = 210f
    const val MAX_ACTIVE = 10
  }
}
