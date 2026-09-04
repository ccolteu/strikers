package com.cc.ww2blitz

/**
 * Shared spawn helpers and map-script constants. No per-call allocation.
 */
object FormationSpawner {
  const val TYPE_DRONE = 0
  const val TYPE_KAMIKAZE = 1
  const val TYPE_INTERCEPTOR = 2
  const val TYPE_HEAVY = 3
  const val PATTERN_WEAVE = 2
  const val PATTERN_V_HOLD = 1
  const val PATTERN_DIAGONAL_SWEEP = 3
  const val FAST_DOWN = 320f
  const val BASE_STAGE2_SPEED = FAST_DOWN
  const val SWEEP_VX = 210f
  const val SWEEP_VY = 240f
  const val KAMI_VX = 340f
  const val KAMI_VY = 560f
  const val KAMI_VX_FAST = 400f
  const val KAMI_VY_FAST = 680f
  const val FLANK_END = 8f
  const val FLANK_SPACING = 1.05f
  const val V_FORM_AT = 12f
  const val WEAVE_AT = 20.5f
  const val WEAVE_END = 23.6f
  const val WALL_AT = 24.8f
  const val S1_CROSS_AT = 16.0f
  const val S1_CROSS_Y = 0.36f
  const val CROSS_VX = 280f
  const val CROSS_VY = 110f
  const val WEAVE_SPACING = 1.35f
  const val WEAVE_VY = 150f
  const val HEAVY_VY = 78f
  const val HEAVY_HP = 10
  const val INTERCEPT_HP = 6
  const val KAMI_HP = 2
  const val INTERCEPT_VY = 210f
  const val MAX_ACTIVE = 10
  const val OPENING_END = 5f
  const val S6_INTRO_SECS = 5f
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
  const val S2_TANKS_AT = 18.5f
  const val S2_TANK_LANE_L = 0.30f
  const val S2_TANK_LANE_R = 0.70f
  const val S2_TANK_HP = 12
  const val S2_LEFT_WALL_AT = 23.0f
  const val S2_RIGHT_WALL_AT = 25.0f
  const val S2_WALL_STAGGER = 0.3f
  const val S2_PRE_BOSS_AT = 27.0f
  const val S2_CENTER_INTERCEPT_AT = 28.0f
  const val S3_SCOUT_START = 1.5f
  const val S3_SCOUT_END = 5.5f
  const val S3_SCOUT_SPACING = 1.65f
  const val S3_CRUISER_AT = 7.5f
  const val S3_CRUISER_HP = 16
  const val S3_DESTROYER_AT = 16.0f
  const val S3_DESTROYER_HP = 12
  const val S3_CROSS_AT = 10.5f
  const val S3_CROSS_Y = 0.38f
  const val S3_FLANK_START = 14.0f
  const val S3_FLANK_END = 19.5f
  const val S3_FLANK_SPACING = 0.95f
  const val S3_BOSS_AT = 25.0f
  const val S6_FLANK_START = 1.0f
  const val S6_FLANK_END = 6.0f
  const val S6_FLANK_SPACING = 1.25f
  const val S6_FLANK_VX = 340f
  const val S6_CRUISER_AT = 22.0f
  const val S6_CRUISER_HP = 20
  const val S6_CRUISER_VY = 80f
  const val S6_WEAVE_START = 15.0f
  const val S6_WEAVE_END = 21.0f
  const val S6_WEAVE_SPACING = 1.5f
  const val S6_WEAVE_PAIRS = 5
  const val S6_WEAVE_VY = 160f
  const val S6_KAMI_AT = 25.0f
  const val S6_KAMI_VY = 680f
  const val S6_WALL_START = 29.5f
  const val S6_WALL_END = 33.5f
  const val S6_WALL_SPACING = 1.0f
  const val S6_WALL_COUNT = 4
  const val S6_WALL_VY = 440f
  const val S6_HOLD_V_AT = 35.0f
  const val S6_BOSS_AT = 45.0f
  const val S4_FLURRY_START = 6.0f
  const val S4_FLURRY_END = 18.0f
  const val S4_FLURRY_SPACING = 1.55f
  const val S4_FLURRY_VY = 140f
  const val S4_HOLD_V_AT = 20.0f
  const val S4_KAMI_AT = 24.0f
  const val S4_KAMI_VY = 620f
  const val S4_CROSS_AT = 28.0f
  const val S4_CROSS_Y = 0.40f
  const val S4_HEAVIES_AT = 32.0f
  const val S4_WALL_AT = 36.0f
  const val S5_REEF_START = 6.0f
  const val S5_REEF_END = 16.5f
  const val S5_REEF_SPACING = 1.40f
  const val S5_REEF_VY = 165f
  const val S5_HOLD_V_AT = 18.5f
  const val S5_KAMI_AT = 23.0f
  const val S5_KAMI_VY = 640f
  const val S5_CROSS_AT = 27.0f
  const val S5_CROSS_Y = 0.42f
  const val S5_HEAVIES_AT = 31.0f
  const val S5_WALL_AT = 35.0f
  const val S7_FLANK_END = 12.0f
  const val S7_FLANK_SPACING = 1.5f
  const val S7_SWEEP_VX = 260f
  const val S7_SWEEP_VY = 280f
  const val S7_KAMI_V_AT = 8.0f
  const val S7_KAMI_VY = 680f
  const val S7_HEAVY_LEFT_AT = 14.0f
  const val S7_HEAVY_RIGHT_AT = 32.0f
  const val S7_HEAVY_HP = 32
  const val S7_WAGONS_AT = 18.5f
  const val S7_WAGON_HP = 14
  const val S7_DRIZZLE_START = 14.0f
  const val S7_DRIZZLE_END = 17.5f
  const val S7_DRIZZLE_SPACING = 2.0f
  const val S7_DRIZZLE_VY = 170f
  const val S7_POWER_WAVE_AT = 34.0f
  const val S7_POWER_VY = 140f
  const val S7_KAMI_WALL_AT = 38.0f
  const val S7_SCROLL_DECAY_AT = 40.0f
  const val S7_SCROLL_DECAY_SPAN = 5.0f
  const val S7_SCROLL_START = 280f
  const val FORM_CLEAR = 2.4f

  fun spawnSideCross(
    enemies: EnemyPoolManager,
    w: Float,
    h: Float,
    yFrac: Float,
    vx: Float,
    vy: Float,
    type: Int,
  ) {
    enemies.spawnEnemy(-0.06f * w, yFrac * h, vx, vy, type, PATTERN_DIAGONAL_SWEEP)
    enemies.spawnEnemy(1.06f * w, yFrac * h, -vx, vy, type, PATTERN_DIAGONAL_SWEEP)
  }

  fun spawnVFormation(enemies: EnemyPoolManager, w: Float, h: Float) {
    val gapX = formGapX(enemies, TYPE_INTERCEPTOR)
    val gapY = formGapY(enemies, TYPE_INTERCEPTOR)
    val cx = 0.50f * w
    val cy = -0.02f * h
    enemies.spawnEnemy(cx, cy, 0f, INTERCEPT_VY, TYPE_INTERCEPTOR, PATTERN_V_HOLD, INTERCEPT_HP)
    enemies.spawnEnemy(cx - gapX, cy - gapY, 0f, INTERCEPT_VY, TYPE_INTERCEPTOR, PATTERN_V_HOLD, INTERCEPT_HP)
    enemies.spawnEnemy(cx + gapX, cy - gapY, 0f, INTERCEPT_VY, TYPE_INTERCEPTOR, PATTERN_V_HOLD, INTERCEPT_HP)
  }

  fun spawnS2Pincer(enemies: EnemyPoolManager, w: Float, h: Float, fromLeft: Boolean) {
    val vx = SWEEP_VX * 1.3f
    val vy = SWEEP_VY * 1.1f
    if (fromLeft) {
      enemies.spawnEnemy(-0.05f * w, -0.05f * h, vx, vy, TYPE_DRONE)
    } else {
      enemies.spawnEnemy(1.05f * w, -0.05f * h, -vx, vy, TYPE_DRONE)
    }
  }

  fun spawnStage3ScoutV(enemies: EnemyPoolManager, w: Float, h: Float) {
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

  fun spawnS5FlankCascade(
    enemies: EnemyPoolManager,
    w: Float,
    h: Float,
    fromLeft: Boolean,
  ) {
    var n = 0
    while (n < 4) {
      val t = n / 3f
      if (fromLeft) {
        val x = (0f + t * 0.20f) * w
        enemies.spawnEnemy(x, -0.05f * h, S7_SWEEP_VX, S7_SWEEP_VY, TYPE_DRONE, PATTERN_DIAGONAL_SWEEP)
      } else {
        val x = (1f - t * 0.20f) * w
        enemies.spawnEnemy(x, -0.05f * h, -S7_SWEEP_VX, S7_SWEEP_VY, TYPE_DRONE, PATTERN_DIAGONAL_SWEEP)
      }
      n++
    }
  }

  fun spawnS5CenterKamiV(enemies: EnemyPoolManager, w: Float, h: Float) {
    val vy = S7_KAMI_VY * 0.70f
    enemies.spawnEnemy(-0.06f * w, 0.10f * h, S7_SWEEP_VX, vy, TYPE_KAMIKAZE)
    enemies.spawnEnemy(-0.06f * w, 0.22f * h, S7_SWEEP_VX * 1.10f, vy, TYPE_KAMIKAZE)
    enemies.spawnEnemy(1.06f * w, 0.10f * h, -S7_SWEEP_VX, vy, TYPE_KAMIKAZE)
    enemies.spawnEnemy(1.06f * w, 0.22f * h, -S7_SWEEP_VX * 1.10f, vy, TYPE_KAMIKAZE)
    enemies.spawnEnemy(-0.06f * w, 0.34f * h, S7_SWEEP_VX * 0.90f, vy, TYPE_KAMIKAZE)
  }

  fun spawnSweepArcSquadron(enemies: EnemyPoolManager, h: Float) {
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

  fun spawnOpeningPowerV(enemies: EnemyPoolManager, w: Float, h: Float) {
    spawnSweepArcSquadron(enemies, h)
  }

  fun spawnResupplyColumn(enemies: EnemyPoolManager, w: Float, h: Float) {
    spawnSweepArcSquadron(enemies, h)
  }

  fun formGapX(enemies: EnemyPoolManager, type: Int): Float =
    enemies.halfWOf(type) * FORM_CLEAR

  fun formGapY(enemies: EnemyPoolManager, type: Int): Float =
    enemies.halfHOf(type) * FORM_CLEAR
}
