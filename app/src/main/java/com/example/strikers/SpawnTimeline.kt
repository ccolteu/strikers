package com.example.strikers

/**
 * Stages 1–3 are driven by elapsed-time spawn loops (no per-frame alloc).
 * Stage 3 (Ocean Fleet) uses interval waves, then a 25s boss gate that
 * freezes the timeline cursor until the fight is over.
 */
class SpawnTimeline {

  private var elapsedTime = 0f
  private var bossCueFired = false
  private var flankGap = 0f
  private var weaveGap = 0f
  private var vFormSpawned = false
  private var wallSpawned = false
  private var weaveStarted = false
  private var s2FingerPincerCount = 0
  private var s2FingerPincerTimer = -1f
  private var s2FlankHeaviesSpawned = false
  private var s2CenterWeaveCount = 0
  private var s2CenterWeaveTimer = 0f
  private var s2TurretGuardsSpawned = false
  private var s2KamiDiamondSpawned = false
  private var s2KamiDiamondStep = 0
  private var s2LeftWallSpawned = false
  private var s2LeftWallCount = 0
  private var s2RightWallSpawned = false
  private var s2RightWallCount = 0
  private var s2PreBossWeavesSpawned = false
  private var s2CenterInterceptSpawned = false
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
      updateStage2(dt, enemyManager, w, h)
    } else if (currentStage == 3) {
      updateStage3(dt, enemyManager, w, h)
    }
    if (allowBoss && !bossCueFired) {
      val bossAt = if (currentStage == 3) S3_BOSS_AT else bossEnterSeconds
      if (elapsedTime >= bossAt) {
        boss.beginEntranceForStage(currentStage)
        bossCueFired = true
      }
    }
  }

  fun reset() {
    elapsedTime = 0f
    bossCueFired = false
    flankGap = FLANK_SPACING
    weaveGap = 0f
    vFormSpawned = false
    wallSpawned = false
    weaveStarted = false
    s2FingerPincerCount = 0
    s2FingerPincerTimer = -1f
    s2FlankHeaviesSpawned = false
    s2CenterWeaveCount = 0
    s2CenterWeaveTimer = 0f
    s2TurretGuardsSpawned = false
    s2KamiDiamondSpawned = false
    s2KamiDiamondStep = 0
    s2LeftWallSpawned = false
    s2LeftWallCount = 0
    s2RightWallSpawned = false
    s2RightWallCount = 0
    s2PreBossWeavesSpawned = false
    s2CenterInterceptSpawned = false
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

  private fun updateStage2(
    dt: Float,
    enemies: EnemyPoolManager,
    w: Float,
    h: Float,
  ) {
    if (elapsedTime >= S2_PINCER_AT && elapsedTime <= S2_PINCER_END && s2FingerPincerCount < S2_PINCER_PAIRS) {
      var safeguard = 0
      while (s2FingerPincerCount < S2_PINCER_PAIRS && safeguard < 4) {
        val pairAt = S2_PINCER_AT + s2FingerPincerCount * S2_PINCER_PAIR_GAP
        if (elapsedTime < pairAt) break
        if (s2FingerPincerTimer < 0f) {
          spawnS2Pincer(enemies, w, h, true)
          s2FingerPincerTimer = 0f
        }
        if (elapsedTime < pairAt + S2_PINCER_STAGGER) break
        spawnS2Pincer(enemies, w, h, false)
        s2FingerPincerCount++
        s2FingerPincerTimer = -1f
        safeguard++
      }
    }
    if (!s2FlankHeaviesSpawned && elapsedTime >= S2_HEAVIES_AT) {
      s2FlankHeaviesSpawned = true
      enemies.spawnEnemy(
        0.20f * w, -0.12f * h, 0f, HEAVY_VY * 1.2f, TYPE_HEAVY, 0, HEAVY_HP,
        spawnCue = SpawnEvent.CUE_DEATH_CLEAR,
      )
      enemies.spawnEnemy(
        0.80f * w, -0.12f * h, 0f, HEAVY_VY * 1.2f, TYPE_HEAVY, 0, HEAVY_HP,
        spawnCue = SpawnEvent.CUE_DEATH_CLEAR,
      )
    }
    if (elapsedTime >= S2_WEAVE_AT && s2CenterWeaveCount < S2_WEAVE_COUNT) {
      s2CenterWeaveTimer += dt
      var safeguard = 0
      while (s2CenterWeaveCount < S2_WEAVE_COUNT && safeguard < 3) {
        val at = S2_WEAVE_AT + s2CenterWeaveCount * S2_WEAVE_GAP
        if (elapsedTime < at) break
        enemies.spawnEnemy(0.50f * w, -0.05f * h, 0f, WEAVE_VY * 1.4f, TYPE_DRONE, PATTERN_WEAVE)
        s2CenterWeaveCount++
        s2CenterWeaveTimer = 0f
        safeguard++
      }
    }
    if (!s2TurretGuardsSpawned && elapsedTime >= S2_TURRETS_AT) {
      s2TurretGuardsSpawned = true
      enemies.spawnEnemy(
        0.15f * w, -0.08f * h, 0f, INTERCEPT_VY, TYPE_INTERCEPTOR, PATTERN_V_HOLD, INTERCEPT_HP,
      )
      enemies.spawnEnemy(
        0.85f * w, -0.08f * h, 0f, INTERCEPT_VY, TYPE_INTERCEPTOR, PATTERN_V_HOLD, INTERCEPT_HP,
      )
    }
    if (!s2KamiDiamondSpawned) {
      if (s2KamiDiamondStep == 0 && elapsedTime >= S2_KAMI_LEADER_AT) {
        enemies.spawnEnemy(
          0.50f * w, -0.05f * h, 0f, KAMI_VY_FAST, TYPE_KAMIKAZE, 0, KAMI_HP,
          spawnCue = SpawnEvent.CUE_DIAMOND_LEADER,
        )
        s2KamiDiamondStep = 1
      }
      if (s2KamiDiamondStep == 1 && elapsedTime >= S2_KAMI_WINGS_AT) {
        enemies.spawnEnemy(
          0.28f * w, -0.05f * h, KAMI_VX, KAMI_VY, TYPE_KAMIKAZE, 0, KAMI_HP,
          spawnCue = SpawnEvent.CUE_DIAMOND_WING_L,
        )
        enemies.spawnEnemy(
          0.72f * w, -0.05f * h, -KAMI_VX, KAMI_VY, TYPE_KAMIKAZE, 0, KAMI_HP,
          spawnCue = SpawnEvent.CUE_DIAMOND_WING_R,
        )
        s2KamiDiamondStep = 2
      }
      if (s2KamiDiamondStep == 2 && elapsedTime >= S2_KAMI_TAIL_AT) {
        enemies.spawnEnemy(0.50f * w, -0.05f * h, 0f, KAMI_VY_FAST, TYPE_KAMIKAZE, 0, KAMI_HP)
        s2KamiDiamondStep = 3
        s2KamiDiamondSpawned = true
      }
    }
    if (!s2LeftWallSpawned && elapsedTime >= S2_LEFT_WALL_AT) {
      var safeguard = 0
      while (s2LeftWallCount < 3 && safeguard < 3) {
        val at = S2_LEFT_WALL_AT + s2LeftWallCount * S2_WALL_STAGGER
        if (elapsedTime < at) break
        if (s2LeftWallCount == 0) {
          enemies.spawnEnemy(0.14f * w, -0.05f * h, 0f, BASE_STAGE2_SPEED * 1.15f, TYPE_DRONE)
        } else if (s2LeftWallCount == 1) {
          enemies.spawnEnemy(0.42f * w, -0.05f * h, 0f, BASE_STAGE2_SPEED * 0.90f, TYPE_DRONE)
        } else {
          enemies.spawnEnemy(0.70f * w, -0.05f * h, 0f, BASE_STAGE2_SPEED * 1.30f, TYPE_DRONE)
        }
        s2LeftWallCount++
        safeguard++
      }
      if (s2LeftWallCount >= 3) s2LeftWallSpawned = true
    }
    if (!s2RightWallSpawned && elapsedTime >= S2_RIGHT_WALL_AT) {
      var safeguard = 0
      while (s2RightWallCount < 3 && safeguard < 3) {
        val at = S2_RIGHT_WALL_AT + s2RightWallCount * S2_WALL_STAGGER
        if (elapsedTime < at) break
        if (s2RightWallCount == 0) {
          enemies.spawnEnemy(0.86f * w, -0.05f * h, 0f, BASE_STAGE2_SPEED * 1.30f, TYPE_DRONE)
        } else if (s2RightWallCount == 1) {
          enemies.spawnEnemy(0.58f * w, -0.05f * h, 0f, BASE_STAGE2_SPEED * 0.90f, TYPE_DRONE)
        } else {
          enemies.spawnEnemy(0.30f * w, -0.05f * h, 0f, BASE_STAGE2_SPEED * 1.15f, TYPE_DRONE)
        }
        s2RightWallCount++
        safeguard++
      }
      if (s2RightWallCount >= 3) s2RightWallSpawned = true
    }
    if (!s2PreBossWeavesSpawned && elapsedTime >= S2_PRE_BOSS_AT) {
      s2PreBossWeavesSpawned = true
      enemies.spawnEnemy(0.25f * w, -0.05f * h, 0f, WEAVE_VY * 1.5f, TYPE_DRONE, PATTERN_WEAVE)
      enemies.spawnEnemy(0.75f * w, -0.05f * h, 0f, WEAVE_VY * 1.5f, TYPE_DRONE, PATTERN_WEAVE)
    }
    if (!s2CenterInterceptSpawned && elapsedTime >= S2_CENTER_INTERCEPT_AT) {
      s2CenterInterceptSpawned = true
      enemies.spawnEnemy(
        0.50f * w, -0.08f * h, 0f, INTERCEPT_VY, TYPE_INTERCEPTOR, PATTERN_V_HOLD, INTERCEPT_HP,
      )
    }
  }

  private fun spawnS2Pincer(enemies: EnemyPoolManager, w: Float, h: Float, fromLeft: Boolean) {
    val vx = SWEEP_VX * 1.3f
    val vy = SWEEP_VY * 1.1f
    if (fromLeft) {
      enemies.spawnEnemy(-0.05f * w, -0.05f * h, vx, vy, TYPE_DRONE)
    } else {
      enemies.spawnEnemy(1.05f * w, -0.05f * h, -vx, vy, TYPE_DRONE)
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
    const val S2_PINCER_AT = 0.5f
    const val S2_PINCER_END = 3.5f
    const val S2_PINCER_PAIRS = 4
    const val S2_PINCER_PAIR_GAP = 0.6f
    const val S2_PINCER_STAGGER = 0.3f
    const val S2_HEAVIES_AT = 6.5f
    const val S2_WEAVE_AT = 8.0f
    const val S2_WEAVE_GAP = 1.0f
    const val S2_WEAVE_COUNT = 3
    const val S2_TURRETS_AT = 13.0f
    const val S2_KAMI_LEADER_AT = 14.0f
    const val S2_KAMI_WINGS_AT = 14.5f
    const val S2_KAMI_TAIL_AT = 15.0f
    const val S2_LEFT_WALL_AT = 20.0f
    const val S2_RIGHT_WALL_AT = 22.0f
    const val S2_WALL_STAGGER = 0.3f
    const val S2_PRE_BOSS_AT = 24.5f
    const val S2_CENTER_INTERCEPT_AT = 25.5f
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
}
