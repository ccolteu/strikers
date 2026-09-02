package com.cc.ww2blitz

import com.cc.ww2blitz.FormationSpawner.CROSS_VX
import com.cc.ww2blitz.FormationSpawner.CROSS_VY
import com.cc.ww2blitz.FormationSpawner.HEAVY_HP
import com.cc.ww2blitz.FormationSpawner.HEAVY_VY
import com.cc.ww2blitz.FormationSpawner.INTERCEPT_HP
import com.cc.ww2blitz.FormationSpawner.INTERCEPT_VY
import com.cc.ww2blitz.FormationSpawner.KAMI_HP
import com.cc.ww2blitz.FormationSpawner.KAMI_VX
import com.cc.ww2blitz.FormationSpawner.KAMI_VY
import com.cc.ww2blitz.FormationSpawner.KAMI_VY_FAST
import com.cc.ww2blitz.FormationSpawner.PATTERN_DIAGONAL_SWEEP
import com.cc.ww2blitz.FormationSpawner.PATTERN_V_HOLD
import com.cc.ww2blitz.FormationSpawner.PATTERN_WEAVE
import com.cc.ww2blitz.FormationSpawner.S2_CENTER_INTERCEPT_AT
import com.cc.ww2blitz.FormationSpawner.S2_HEAVIES_AT
import com.cc.ww2blitz.FormationSpawner.S2_KAMI_LEADER_AT
import com.cc.ww2blitz.FormationSpawner.S2_KAMI_TAIL_AT
import com.cc.ww2blitz.FormationSpawner.S2_KAMI_WINGS_AT
import com.cc.ww2blitz.FormationSpawner.S2_LEFT_WALL_AT
import com.cc.ww2blitz.FormationSpawner.S2_PINCER_AT
import com.cc.ww2blitz.FormationSpawner.S2_PINCER_END
import com.cc.ww2blitz.FormationSpawner.S2_PINCER_PAIRS
import com.cc.ww2blitz.FormationSpawner.S2_PINCER_PAIR_GAP
import com.cc.ww2blitz.FormationSpawner.S2_PINCER_STAGGER
import com.cc.ww2blitz.FormationSpawner.S2_PRE_BOSS_AT
import com.cc.ww2blitz.FormationSpawner.S2_RIGHT_WALL_AT
import com.cc.ww2blitz.FormationSpawner.S2_TANKS_AT
import com.cc.ww2blitz.FormationSpawner.S2_TANK_HP
import com.cc.ww2blitz.FormationSpawner.S2_TANK_LANE_L
import com.cc.ww2blitz.FormationSpawner.S2_TANK_LANE_R
import com.cc.ww2blitz.FormationSpawner.S2_TURRETS_AT
import com.cc.ww2blitz.FormationSpawner.S2_WALL_STAGGER
import com.cc.ww2blitz.FormationSpawner.S2_WEAVE_AT
import com.cc.ww2blitz.FormationSpawner.S2_WEAVE_COUNT
import com.cc.ww2blitz.FormationSpawner.S2_WEAVE_GAP
import com.cc.ww2blitz.FormationSpawner.TYPE_DRONE
import com.cc.ww2blitz.FormationSpawner.TYPE_HEAVY
import com.cc.ww2blitz.FormationSpawner.TYPE_INTERCEPTOR
import com.cc.ww2blitz.FormationSpawner.TYPE_KAMIKAZE
import com.cc.ww2blitz.FormationSpawner.WEAVE_VY

class Stage2Director : StageDirector {
  private var s2FingerPincerCount = 0
  private var s2FingerPincerTimer = -1f
  private var s2FlankHeaviesSpawned = false
  private var s2CenterWeaveCount = 0
  private var s2CenterWeaveTimer = 0f
  private var s2TurretGuardsSpawned = false
  private var s2KamiDiamondSpawned = false
  private var s2KamiDiamondStep = 0
  private var s2TanksSpawned = false
  private var s2LeftWallSpawned = false
  private var s2LeftWallCount = 0
  private var s2RightWallSpawned = false
  private var s2RightWallCount = 0
  private var s2PreBossWeavesSpawned = false
  private var s2CenterInterceptSpawned = false

  override fun reset() {
    s2FingerPincerCount = 0
    s2FingerPincerTimer = -1f
    s2FlankHeaviesSpawned = false
    s2CenterWeaveCount = 0
    s2CenterWeaveTimer = 0f
    s2TurretGuardsSpawned = false
    s2KamiDiamondSpawned = false
    s2KamiDiamondStep = 0
    s2TanksSpawned = false
    s2LeftWallSpawned = false
    s2LeftWallCount = 0
    s2RightWallSpawned = false
    s2RightWallCount = 0
    s2PreBossWeavesSpawned = false
    s2CenterInterceptSpawned = false
  }

  override fun tick(
    dt: Float,
    elapsed: Float,
    enemies: EnemyPoolManager,
    w: Float,
    h: Float,
    boss: BossController,
    allowBoss: Boolean,
    stageData: StageData,
    cue: DirectorCue,
  ) {
    if (elapsed >= S2_PINCER_AT && elapsed <= S2_PINCER_END && s2FingerPincerCount < S2_PINCER_PAIRS) {
      var safeguard = 0
      while (s2FingerPincerCount < S2_PINCER_PAIRS && safeguard < 4) {
        val pairAt = S2_PINCER_AT + s2FingerPincerCount * S2_PINCER_PAIR_GAP
        if (elapsed < pairAt) break
        if (s2FingerPincerTimer < 0f) {
          FormationSpawner.spawnS2Pincer(enemies, w, h, true)
          s2FingerPincerTimer = 0f
        }
        if (elapsed < pairAt + S2_PINCER_STAGGER) break
        FormationSpawner.spawnS2Pincer(enemies, w, h, false)
        s2FingerPincerCount++
        s2FingerPincerTimer = -1f
        safeguard++
      }
    }
    if (!s2FlankHeaviesSpawned && elapsed >= S2_HEAVIES_AT) {
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
    if (elapsed >= S2_WEAVE_AT && s2CenterWeaveCount < S2_WEAVE_COUNT) {
      s2CenterWeaveTimer += dt
      var safeguard = 0
      while (s2CenterWeaveCount < S2_WEAVE_COUNT && safeguard < 3) {
        val at = S2_WEAVE_AT + s2CenterWeaveCount * S2_WEAVE_GAP
        if (elapsed < at) break
        val xFrac = if ((s2CenterWeaveCount and 1) == 0) 0.12f else 0.88f
        enemies.spawnEnemy(xFrac * w, -0.05f * h, 0f, WEAVE_VY * 1.4f, TYPE_DRONE, PATTERN_WEAVE)
        s2CenterWeaveCount++
        s2CenterWeaveTimer = 0f
        safeguard++
      }
    }
    if (!s2TurretGuardsSpawned && elapsed >= S2_TURRETS_AT) {
      s2TurretGuardsSpawned = true
      enemies.spawnEnemy(
        0.15f * w, -0.08f * h, 0f, INTERCEPT_VY, TYPE_INTERCEPTOR, PATTERN_V_HOLD, INTERCEPT_HP,
      )
      enemies.spawnEnemy(
        0.85f * w, -0.08f * h, 0f, INTERCEPT_VY, TYPE_INTERCEPTOR, PATTERN_V_HOLD, INTERCEPT_HP,
      )
    }
    if (!s2KamiDiamondSpawned) {
      if (s2KamiDiamondStep == 0 && elapsed >= S2_KAMI_LEADER_AT) {
        enemies.spawnEnemy(
          0.50f * w, -0.05f * h, 0f, KAMI_VY_FAST, TYPE_KAMIKAZE, 0, KAMI_HP,
          spawnCue = SpawnEvent.CUE_DIAMOND_LEADER,
        )
        s2KamiDiamondStep = 1
      }
      if (s2KamiDiamondStep == 1 && elapsed >= S2_KAMI_WINGS_AT) {
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
      if (s2KamiDiamondStep == 2 && elapsed >= S2_KAMI_TAIL_AT) {
        enemies.spawnEnemy(0.50f * w, -0.05f * h, 0f, KAMI_VY_FAST, TYPE_KAMIKAZE, 0, KAMI_HP)
        s2KamiDiamondStep = 3
        s2KamiDiamondSpawned = true
      }
    }
    if (!s2TanksSpawned && elapsed >= S2_TANKS_AT) {
      s2TanksSpawned = true
      val y = -0.06f * h
      val vy = HEAVY_VY * 2.2f
      enemies.spawnEnemy(
        S2_TANK_LANE_L * w,
        y,
        0f,
        vy,
        TYPE_HEAVY,
        PATTERN_V_HOLD,
        S2_TANK_HP,
        isLandVehicle = true,
      )
      enemies.spawnEnemy(
        S2_TANK_LANE_R * w,
        y,
        0f,
        vy,
        TYPE_HEAVY,
        PATTERN_V_HOLD,
        S2_TANK_HP,
        isLandVehicle = true,
      )
    }
    if (!s2LeftWallSpawned && elapsed >= S2_LEFT_WALL_AT) {
      var safeguard = 0
      while (s2LeftWallCount < 3 && safeguard < 3) {
        val at = S2_LEFT_WALL_AT + s2LeftWallCount * S2_WALL_STAGGER
        if (elapsed < at) break
        if (s2LeftWallCount == 0) {
          enemies.spawnEnemy(-0.06f * w, 0.18f * h, CROSS_VX, CROSS_VY, TYPE_DRONE, PATTERN_DIAGONAL_SWEEP)
        } else if (s2LeftWallCount == 1) {
          enemies.spawnEnemy(-0.06f * w, 0.32f * h, CROSS_VX, CROSS_VY * 0.85f, TYPE_DRONE, PATTERN_DIAGONAL_SWEEP)
        } else {
          enemies.spawnEnemy(-0.06f * w, 0.46f * h, CROSS_VX, CROSS_VY * 0.70f, TYPE_DRONE, PATTERN_DIAGONAL_SWEEP)
        }
        s2LeftWallCount++
        safeguard++
      }
      if (s2LeftWallCount >= 3) s2LeftWallSpawned = true
    }
    if (!s2RightWallSpawned && elapsed >= S2_RIGHT_WALL_AT) {
      var safeguard = 0
      while (s2RightWallCount < 3 && safeguard < 3) {
        val at = S2_RIGHT_WALL_AT + s2RightWallCount * S2_WALL_STAGGER
        if (elapsed < at) break
        if (s2RightWallCount == 0) {
          enemies.spawnEnemy(1.06f * w, 0.18f * h, -CROSS_VX, CROSS_VY, TYPE_DRONE, PATTERN_DIAGONAL_SWEEP)
        } else if (s2RightWallCount == 1) {
          enemies.spawnEnemy(1.06f * w, 0.32f * h, -CROSS_VX, CROSS_VY * 0.85f, TYPE_DRONE, PATTERN_DIAGONAL_SWEEP)
        } else {
          enemies.spawnEnemy(1.06f * w, 0.46f * h, -CROSS_VX, CROSS_VY * 0.70f, TYPE_DRONE, PATTERN_DIAGONAL_SWEEP)
        }
        s2RightWallCount++
        safeguard++
      }
      if (s2RightWallCount >= 3) s2RightWallSpawned = true
    }
    if (!s2PreBossWeavesSpawned && elapsed >= S2_PRE_BOSS_AT) {
      s2PreBossWeavesSpawned = true
      enemies.spawnEnemy(0.12f * w, -0.05f * h, 0f, WEAVE_VY * 1.5f, TYPE_DRONE, PATTERN_WEAVE)
      enemies.spawnEnemy(0.88f * w, -0.05f * h, 0f, WEAVE_VY * 1.5f, TYPE_DRONE, PATTERN_WEAVE)
    }
    if (!s2CenterInterceptSpawned && elapsed >= S2_CENTER_INTERCEPT_AT) {
      s2CenterInterceptSpawned = true
      enemies.spawnEnemy(
        0.12f * w, -0.08f * h, 0f, INTERCEPT_VY, TYPE_INTERCEPTOR, PATTERN_V_HOLD, INTERCEPT_HP,
      )
    }
  }
}
