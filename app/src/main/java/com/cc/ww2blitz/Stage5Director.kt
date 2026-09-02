package com.cc.ww2blitz

import com.cc.ww2blitz.FormationSpawner.HEAVY_VY
import com.cc.ww2blitz.FormationSpawner.PATTERN_V_HOLD
import com.cc.ww2blitz.FormationSpawner.PATTERN_WEAVE
import com.cc.ww2blitz.FormationSpawner.S5_DRIZZLE_END
import com.cc.ww2blitz.FormationSpawner.S5_DRIZZLE_SPACING
import com.cc.ww2blitz.FormationSpawner.S5_DRIZZLE_START
import com.cc.ww2blitz.FormationSpawner.S5_DRIZZLE_VY
import com.cc.ww2blitz.FormationSpawner.S5_FLANK_END
import com.cc.ww2blitz.FormationSpawner.S5_FLANK_SPACING
import com.cc.ww2blitz.FormationSpawner.S5_HEAVY_HP
import com.cc.ww2blitz.FormationSpawner.S5_HEAVY_LEFT_AT
import com.cc.ww2blitz.FormationSpawner.S5_HEAVY_RIGHT_AT
import com.cc.ww2blitz.FormationSpawner.S5_KAMI_V_AT
import com.cc.ww2blitz.FormationSpawner.S5_KAMI_VY
import com.cc.ww2blitz.FormationSpawner.S5_KAMI_WALL_AT
import com.cc.ww2blitz.FormationSpawner.S5_POWER_VY
import com.cc.ww2blitz.FormationSpawner.S5_POWER_WAVE_AT
import com.cc.ww2blitz.FormationSpawner.S5_SCROLL_DECAY_AT
import com.cc.ww2blitz.FormationSpawner.S5_SCROLL_DECAY_SPAN
import com.cc.ww2blitz.FormationSpawner.S5_SCROLL_START
import com.cc.ww2blitz.FormationSpawner.S5_WAGONS_AT
import com.cc.ww2blitz.FormationSpawner.S5_WAGON_HP
import com.cc.ww2blitz.FormationSpawner.TYPE_DRONE
import com.cc.ww2blitz.FormationSpawner.TYPE_HEAVY
import com.cc.ww2blitz.FormationSpawner.TYPE_INTERCEPTOR
import com.cc.ww2blitz.FormationSpawner.TYPE_KAMIKAZE

class Stage5Director : StageDirector {
  private var s5FlankGap = 0f
  private var s5FlankFromLeft = true
  private var s5KamiVSpawned = false
  private var s5HeavyLeftSpawned = false
  private var s5HeavyRightSpawned = false
  private var s5WagonsSpawned = false
  private var s5DrizzleGap = 0f
  private var s5DrizzleSeed = 1L
  private var s5PowerWaveSpawned = false
  private var s5KamiWallSpawned = false

  override fun reset() {
    s5FlankGap = S5_FLANK_SPACING
    s5FlankFromLeft = true
    s5KamiVSpawned = false
    s5HeavyLeftSpawned = false
    s5HeavyRightSpawned = false
    s5WagonsSpawned = false
    s5DrizzleGap = 0f
    s5DrizzleSeed = 1L
    s5PowerWaveSpawned = false
    s5KamiWallSpawned = false
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
    if (cue.bossCueFired) return
    if (elapsed <= S5_FLANK_END) {
      s5FlankGap += dt
      var safeguard = 0
      while (s5FlankGap >= S5_FLANK_SPACING && safeguard < 2) {
        s5FlankGap -= S5_FLANK_SPACING
        safeguard++
        if (s5FlankFromLeft) {
          FormationSpawner.spawnS5FlankCascade(enemies, w, h, true)
        } else {
          FormationSpawner.spawnS5FlankCascade(enemies, w, h, false)
        }
        s5FlankFromLeft = !s5FlankFromLeft
      }
    }
    if (!s5KamiVSpawned && elapsed >= S5_KAMI_V_AT) {
      s5KamiVSpawned = true
      FormationSpawner.spawnS5CenterKamiV(enemies, w, h)
    }
    if (!s5HeavyLeftSpawned && elapsed >= S5_HEAVY_LEFT_AT) {
      s5HeavyLeftSpawned = true
      enemies.spawnEnemy(
        0.20f * w,
        -0.10f * h,
        0f,
        HEAVY_VY,
        TYPE_HEAVY,
        PATTERN_V_HOLD,
        S5_HEAVY_HP,
      )
    }
    if (!s5HeavyRightSpawned && elapsed >= S5_HEAVY_RIGHT_AT) {
      s5HeavyRightSpawned = true
      enemies.spawnEnemy(
        0.80f * w,
        -0.10f * h,
        0f,
        HEAVY_VY,
        TYPE_HEAVY,
        PATTERN_V_HOLD,
        S5_HEAVY_HP,
      )
    }
    if (!s5WagonsSpawned && elapsed >= S5_WAGONS_AT) {
      s5WagonsSpawned = true
      val y = -0.06f * h
      val vy = HEAVY_VY * 2.2f
      enemies.spawnEnemy(
        0.22f * w,
        y,
        0f,
        vy,
        TYPE_HEAVY,
        PATTERN_V_HOLD,
        S5_WAGON_HP,
        isWagon = true,
      )
      enemies.spawnEnemy(
        0.78f * w,
        y,
        0f,
        vy,
        TYPE_HEAVY,
        PATTERN_V_HOLD,
        S5_WAGON_HP,
        isWagon = true,
      )
    }
    if (elapsed >= S5_DRIZZLE_START && elapsed <= S5_DRIZZLE_END) {
      s5DrizzleGap += dt
      var safeguard = 0
      while (s5DrizzleGap >= S5_DRIZZLE_SPACING && safeguard < 2) {
        s5DrizzleGap -= S5_DRIZZLE_SPACING
        safeguard++
        s5DrizzleSeed = s5DrizzleSeed * 1664525L + 1013904223L
        val u = ((s5DrizzleSeed ushr 8) and 0xFFFFFFL).toFloat() / 16777215f
        val xFrac = 0.08f + u * 0.84f
        enemies.spawnEnemy(
          xFrac * w,
          -0.04f * h,
          0f,
          S5_DRIZZLE_VY,
          TYPE_INTERCEPTOR,
          PATTERN_WEAVE,
        )
      }
    }
    if (!s5PowerWaveSpawned && elapsed >= S5_POWER_WAVE_AT) {
      s5PowerWaveSpawned = true
      enemies.spawnEnemy(
        0.25f * w, -0.05f * h, 0f, S5_POWER_VY, TYPE_DRONE,
        isRedShipAnchor = true,
      )
      enemies.spawnEnemy(
        0.50f * w, -0.05f * h, 0f, S5_POWER_VY, TYPE_DRONE,
        isRedShipAnchor = true,
      )
      enemies.spawnEnemy(
        0.75f * w, -0.05f * h, 0f, S5_POWER_VY, TYPE_DRONE,
        isRedShipAnchor = true,
      )
    }
    if (!s5KamiWallSpawned && elapsed >= S5_KAMI_WALL_AT) {
      s5KamiWallSpawned = true
      var n = 0
      while (n < 6) {
        val xFrac = (n + 0.5f) / 6f
        enemies.spawnEnemy(xFrac * w, -0.06f * h, 0f, S5_KAMI_VY, TYPE_KAMIKAZE)
        n++
      }
    }
    if (elapsed >= S5_SCROLL_DECAY_AT) {
      var u = (elapsed - S5_SCROLL_DECAY_AT) / S5_SCROLL_DECAY_SPAN
      if (u < 0f) u = 0f
      if (u > 1f) u = 1f
      stageData.scrollSpeedY = S5_SCROLL_START * (1f - u)
    }
    if (allowBoss && elapsed >= stageData.def.bossAtSeconds) {
      cue.fireBoss(stageData.currentStage, boss)
      stageData.scrollSpeedY = 0f
    }
  }
}
