package com.example.strikers

/**
 * Stage 1 is driven by elapsed-time spawn loops (no per-frame alloc).
 * Stage 2 still uses a pre-ordered [SpawnEvent] cursor.
 * Stage 3 (Ocean Fleet) uses interval waves, then a 25s boss gate that
 * freezes the timeline cursor until the fight is over.
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
    SpawnEvent(6.5f, 0.20f, -0.12f, 0f, HEAVY_VY * 1.2f, TYPE_HEAVY, 0, SpawnEvent.CUE_DEATH_CLEAR),
    SpawnEvent(6.5f, 0.80f, -0.12f, 0f, HEAVY_VY * 1.2f, TYPE_HEAVY, 0, SpawnEvent.CUE_DEATH_CLEAR),

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

    SpawnEvent(14.0f, 0.50f, -0.05f, 0f, KAMI_VY_FAST, TYPE_KAMIKAZE, 0, SpawnEvent.CUE_DIAMOND_LEADER),
    SpawnEvent(14.5f, 0.28f, -0.05f, KAMI_VX, KAMI_VY, TYPE_KAMIKAZE, 0, SpawnEvent.CUE_DIAMOND_WING_L),
    SpawnEvent(14.5f, 0.72f, -0.05f, -KAMI_VX, KAMI_VY, TYPE_KAMIKAZE, 0, SpawnEvent.CUE_DIAMOND_WING_R),
    SpawnEvent(15.0f, 0.50f, -0.05f, 0f, KAMI_VY_FAST, TYPE_KAMIKAZE),

    // =========================================================================
    // PHASE 4: REAR-GUARD AMBUSH & STRIKE FORCES (20.0s - 25.5s)
    // Quick vertical drop walls from alternating sides to force fast horizontal movement,
    // ending with dual interlocking cross-weaving units right before the boss
    // =========================================================================
    SpawnEvent(20.0f, 0.14f, -0.05f, 0f, BASE_STAGE2_SPEED * 1.15f, TYPE_DRONE),
    SpawnEvent(20.3f, 0.42f, -0.05f, 0f, BASE_STAGE2_SPEED * 0.90f, TYPE_DRONE),
    SpawnEvent(20.6f, 0.70f, -0.05f, 0f, BASE_STAGE2_SPEED * 1.30f, TYPE_DRONE),

    SpawnEvent(22.0f, 0.86f, -0.05f, 0f, BASE_STAGE2_SPEED * 1.30f, TYPE_DRONE),
    SpawnEvent(22.3f, 0.58f, -0.05f, 0f, BASE_STAGE2_SPEED * 0.90f, TYPE_DRONE),
    SpawnEvent(22.6f, 0.30f, -0.05f, 0f, BASE_STAGE2_SPEED * 1.15f, TYPE_DRONE),

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
  private var weaveStarted = false
  private var s3ScoutGap = 0f
  private var s3FlankGap = 0f
  private var s3CruiserSpawned = false
  private var s3FlankFromLeft = true
  private var openingPowerVSpawned = false
  private var powerUpWaveQueued = false
  private var powerUpWaveTimer = 0f

  fun update(
    dt: Float,
    enemyManager: EnemyPoolManager,
    screenWidth: Int,
    screenHeight: Int,
    boss: BossController,
    currentStage: Int,
    bossEnterSeconds: Float,
    allowBoss: Boolean,
    playerWeaponPower: Int,
  ) {
    if (screenWidth <= 0 || screenHeight <= 0) return
    // Stage 3 locks the cursor at the 25s boss gate until the fight ends.
    if (!(currentStage == 3 && bossCueFired)) {
      elapsedTime += dt
    }
    val w = screenWidth.toFloat()
    val h = screenHeight.toFloat()
    if (!openingPowerVSpawned && elapsedTime >= 0f && elapsedTime <= OPENING_END) {
      openingPowerVSpawned = true
      spawnOpeningPowerV(enemyManager, w, h)
    }
    updatePowerSafeguard(dt, enemyManager, w, h, playerWeaponPower, boss)
    if (currentStage < 2) {
      updateStage1(dt, enemyManager, w, h)
    } else if (currentStage == 2) {
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
          hpFor(cue.enemyType),
          spawnCue = cue.spawnCue,
        )
        nextIndex++
      }
    } else if (currentStage == 3) {
      updateStage3(dt, enemyManager, w, h)
    }
    if (allowBoss && !bossCueFired) {
      val bossAt = if (currentStage == 3) S3_BOSS_AT else bossEnterSeconds
      if (elapsedTime >= bossAt) {
        boss.beginEntranceForStage(currentStage)
        bossCueFired = true
        nextIndex = stage2Events.size
      }
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
    weaveStarted = false
    s3ScoutGap = S3_SCOUT_SPACING
    s3FlankGap = S3_FLANK_SPACING
    s3CruiserSpawned = false
    s3FlankFromLeft = true
    openingPowerVSpawned = false
    powerUpWaveQueued = false
    powerUpWaveTimer = 0f
  }

  private fun updateStage1(
    dt: Float,
    enemies: EnemyPoolManager,
    w: Float,
    h: Float,
  ) {
    if (elapsedTime >= OPENING_END && elapsedTime <= FLANK_END) {
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
    if (!weaveStarted && elapsedTime >= WEAVE_AT) {
      weaveStarted = true
      weaveGap = WEAVE_SPACING
    }
    if (elapsedTime >= WEAVE_AT && elapsedTime <= WEAVE_END) {
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
    if (!wallSpawned && elapsedTime >= WALL_AT) {
      if (enemies.countActive() < MAX_ACTIVE) {
        wallSpawned = true
        enemies.spawnEnemy(0.30f * w, -0.10f * h, 0f, HEAVY_VY, TYPE_HEAVY, 0, HEAVY_HP)
        enemies.spawnEnemy(0.70f * w, -0.10f * h, 0f, HEAVY_VY, TYPE_HEAVY, 0, HEAVY_HP)
      }
    }
  }

  private fun updatePowerSafeguard(
    dt: Float,
    enemies: EnemyPoolManager,
    w: Float,
    h: Float,
    playerWeaponPower: Int,
    boss: BossController,
  ) {
    val bossOnScreen = bossCueFired || boss.isActive() || boss.isExploding()
    val emergencyLive = enemies.hasActiveRedShipAnchor()
    if (playerWeaponPower >= 2 || bossOnScreen) {
      powerUpWaveQueued = false
      powerUpWaveTimer = 0f
      return
    }
    if (!powerUpWaveQueued && !emergencyLive) {
      powerUpWaveQueued = true
      powerUpWaveTimer = 0f
    }
    if (!powerUpWaveQueued) return
    powerUpWaveTimer += dt
    if (powerUpWaveTimer < POWER_WAVE_DELAY) return
    spawnResupplyColumn(enemies, w, h)
    powerUpWaveQueued = false
    powerUpWaveTimer = 0f
  }

  private fun spawnOpeningPowerV(enemies: EnemyPoolManager, w: Float, h: Float) {
    spawnSweepArcSquadron(enemies, h)
  }

  private fun spawnResupplyColumn(enemies: EnemyPoolManager, w: Float, h: Float) {
    spawnSweepArcSquadron(enemies, h)
  }

  private fun spawnSweepArcSquadron(enemies: EnemyPoolManager, h: Float) {
    val parkX = -64f
    val parkY = h * 0.08f
    val profile = Enemy.FLIGHT_PROFILE_SWEEP_ARC
    val gap = enemies.sweepArcTailDelay()
    enemies.spawnEnemy(
      parkX, parkY, 0f, 0f, TYPE_DRONE,
      isRedShipAnchor = true,
      flightProfile = profile,
      patternDelay = 0.0f,
    )
    enemies.spawnEnemy(
      parkX, parkY, 0f, 0f, TYPE_DRONE,
      flightProfile = profile,
      patternDelay = gap,
    )
    enemies.spawnEnemy(
      parkX, parkY, 0f, 0f, TYPE_DRONE,
      flightProfile = profile,
      patternDelay = gap * 2f,
    )
    enemies.spawnEnemy(
      parkX, parkY, 0f, 0f, TYPE_DRONE,
      flightProfile = profile,
      patternDelay = gap * 3f,
    )
    enemies.spawnEnemy(
      parkX, parkY, 0f, 0f, TYPE_DRONE,
      flightProfile = profile,
      patternDelay = gap * 4f,
    )
  }

  private fun spawnVFormation(enemies: EnemyPoolManager, w: Float, h: Float) {
    val gapX = formGapX(enemies, TYPE_INTERCEPTOR)
    val gapY = formGapY(enemies, TYPE_INTERCEPTOR)
    val cx = 0.50f * w
    val cy = -0.02f * h
    enemies.spawnEnemy(cx, cy, 0f, INTERCEPT_VY, TYPE_INTERCEPTOR, PATTERN_V_HOLD, INTERCEPT_HP)
    enemies.spawnEnemy(cx - gapX, cy - gapY, 0f, INTERCEPT_VY, TYPE_INTERCEPTOR, PATTERN_V_HOLD, INTERCEPT_HP)
    enemies.spawnEnemy(cx + gapX, cy - gapY, 0f, INTERCEPT_VY, TYPE_INTERCEPTOR, PATTERN_V_HOLD, INTERCEPT_HP)
  }

  private fun updateStage3(
    dt: Float,
    enemies: EnemyPoolManager,
    w: Float,
    h: Float,
  ) {
    if (bossCueFired) return
    if (elapsedTime >= S3_SCOUT_START && elapsedTime <= S3_SCOUT_END) {
      s3ScoutGap += dt
      var safeguard = 0
      while (s3ScoutGap >= S3_SCOUT_SPACING && safeguard < 2) {
        if (enemies.countActive() >= MAX_ACTIVE) break
        s3ScoutGap -= S3_SCOUT_SPACING
        safeguard++
        spawnStage3ScoutV(enemies, w, h)
      }
    }
    if (!s3CruiserSpawned && elapsedTime >= S3_CRUISER_AT) {
      if (enemies.countActive() < MAX_ACTIVE) {
        s3CruiserSpawned = true
        enemies.spawnEnemy(
          0.50f * w,
          -0.10f * h,
          0f,
          HEAVY_VY * 0.85f,
          TYPE_HEAVY,
          PATTERN_V_HOLD,
          S3_CRUISER_HP,
        )
      }
    }
    if (elapsedTime >= S3_FLANK_START && elapsedTime <= S3_FLANK_END) {
      s3FlankGap += dt
      var safeguard = 0
      while (s3FlankGap >= S3_FLANK_SPACING && safeguard < 2) {
        if (enemies.countActive() >= MAX_ACTIVE) break
        s3FlankGap -= S3_FLANK_SPACING
        safeguard++
        val laneY = if (s3FlankFromLeft) {
          0.10f * h
        } else {
          0.10f * h + formGapY(enemies, TYPE_INTERCEPTOR)
        }
        if (s3FlankFromLeft) {
          enemies.spawnEnemy(
            -0.08f * w,
            laneY,
            SWEEP_VX * 1.35f,
            FAST_DOWN * 1.15f,
            TYPE_INTERCEPTOR,
            0,
            INTERCEPT_HP,
          )
        } else {
          enemies.spawnEnemy(
            1.08f * w,
            laneY,
            -SWEEP_VX * 1.35f,
            FAST_DOWN * 1.15f,
            TYPE_INTERCEPTOR,
            0,
            INTERCEPT_HP,
          )
        }
        s3FlankFromLeft = !s3FlankFromLeft
      }
    }
  }

  private fun spawnStage3ScoutV(enemies: EnemyPoolManager, w: Float, h: Float) {
    val vx = 0f
    val vy = WEAVE_VY * 1.7f
    val gapX = formGapX(enemies, TYPE_DRONE)
    val gapY = formGapY(enemies, TYPE_DRONE)
    val cx = 0.50f * w
    val cy = -0.04f * h
    enemies.spawnEnemy(cx, cy, vx, vy, TYPE_DRONE, PATTERN_WEAVE)
    enemies.spawnEnemy(cx - gapX, cy - gapY, vx, vy, TYPE_DRONE, PATTERN_WEAVE)
    enemies.spawnEnemy(cx + gapX, cy - gapY, vx, vy, TYPE_DRONE, PATTERN_WEAVE)
    enemies.spawnEnemy(cx - gapX * 2f, cy - gapY * 2f, vx, vy, TYPE_DRONE, PATTERN_WEAVE)
    enemies.spawnEnemy(cx + gapX * 2f, cy - gapY * 2f, vx, vy, TYPE_DRONE, PATTERN_WEAVE)
  }

  private fun formGapX(enemies: EnemyPoolManager, type: Int): Float =
    enemies.halfWOf(type) * FORM_CLEAR

  private fun formGapY(enemies: EnemyPoolManager, type: Int): Float =
    enemies.halfHOf(type) * FORM_CLEAR

  private companion object {
    const val TYPE_DRONE = 0
    const val TYPE_KAMIKAZE = 1
    const val TYPE_INTERCEPTOR = 2
    const val TYPE_HEAVY = 3
    const val PATTERN_WEAVE = 2
    const val PATTERN_V_HOLD = 1
    const val FAST_DOWN = 320f
    const val BASE_STAGE2_SPEED = FAST_DOWN
    const val SWEEP_VX = 210f
    const val SWEEP_VY = 240f
    const val KAMI_VX = 260f
    const val KAMI_VY = 420f
    const val KAMI_VX_FAST = 340f
    const val KAMI_VY_FAST = 520f
    const val FLANK_END = 8f
    const val FLANK_SPACING = 1.05f
    const val V_FORM_AT = 12f
    const val WEAVE_AT = 20.5f
    const val WEAVE_END = 23.6f
    const val WALL_AT = 24.8f
    const val WEAVE_SPACING = 1.35f
    const val WEAVE_VY = 150f
    const val HEAVY_VY = 78f
    const val HEAVY_HP = 6
    const val INTERCEPT_HP = 4
    const val KAMI_HP = 2
    const val INTERCEPT_VY = 210f
    const val MAX_ACTIVE = 10
    const val OPENING_END = 5f
    const val POWER_WAVE_DELAY = 3.0f
    const val S3_SCOUT_START = 1.5f
    const val S3_SCOUT_END = 5.5f
    const val S3_SCOUT_SPACING = 1.65f
    const val S3_CRUISER_AT = 7.5f
    const val S3_CRUISER_HP = 12
    const val S3_FLANK_START = 14.0f
    const val S3_FLANK_END = 19.5f
    const val S3_FLANK_SPACING = 0.95f
    const val S3_BOSS_AT = 25.0f
    const val FORM_CLEAR = 2.4f
  }

  private fun hpFor(enemyType: Int): Int {
    if (enemyType == TYPE_HEAVY) return HEAVY_HP
    if (enemyType == TYPE_INTERCEPTOR) return INTERCEPT_HP
    if (enemyType == TYPE_KAMIKAZE) return KAMI_HP
    return 1
  }
}
