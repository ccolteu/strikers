package com.cc.ww2blitz

import com.cc.ww2blitz.FormationSpawner.HEAVY_VY
import com.cc.ww2blitz.FormationSpawner.PATTERN_V_HOLD
import com.cc.ww2blitz.FormationSpawner.PATTERN_WEAVE
import com.cc.ww2blitz.FormationSpawner.S7_DRIZZLE_END
import com.cc.ww2blitz.FormationSpawner.S7_DRIZZLE_SPACING
import com.cc.ww2blitz.FormationSpawner.S7_DRIZZLE_START
import com.cc.ww2blitz.FormationSpawner.S7_DRIZZLE_VY
import com.cc.ww2blitz.FormationSpawner.S7_FLANK_END
import com.cc.ww2blitz.FormationSpawner.S7_FLANK_SPACING
import com.cc.ww2blitz.FormationSpawner.S7_HEAVY_HP
import com.cc.ww2blitz.FormationSpawner.S7_HEAVY_LEFT_AT
import com.cc.ww2blitz.FormationSpawner.S7_HEAVY_RIGHT_AT
import com.cc.ww2blitz.FormationSpawner.S7_KAMI_V_AT
import com.cc.ww2blitz.FormationSpawner.S7_KAMI_VY
import com.cc.ww2blitz.FormationSpawner.S7_KAMI_WALL_AT
import com.cc.ww2blitz.FormationSpawner.S7_POWER_VY
import com.cc.ww2blitz.FormationSpawner.S7_POWER_WAVE_AT
import com.cc.ww2blitz.FormationSpawner.S7_SCROLL_DECAY_AT
import com.cc.ww2blitz.FormationSpawner.S7_SCROLL_DECAY_SPAN
import com.cc.ww2blitz.FormationSpawner.S7_SCROLL_START
import com.cc.ww2blitz.FormationSpawner.S7_WAGONS_AT
import com.cc.ww2blitz.FormationSpawner.S7_WAGON_HP
import com.cc.ww2blitz.FormationSpawner.TYPE_DRONE
import com.cc.ww2blitz.FormationSpawner.TYPE_HEAVY
import com.cc.ww2blitz.FormationSpawner.TYPE_INTERCEPTOR
import com.cc.ww2blitz.FormationSpawner.TYPE_KAMIKAZE

class Stage7Director : StageDirector {
  private var s7FlankGap = 0f
  private var s7FlankFromLeft = true
  private var s7KamiVSpawned = false
  private var s7HeavyLeftSpawned = false
  private var s7HeavyRightSpawned = false
  private var s7WagonsSpawned = false
  private var s7DrizzleGap = 0f
  private var s7DrizzleSeed = 1L
  private var s7PowerWaveSpawned = false
  private var s7KamiWallSpawned = false

  override fun reset() {
    s7FlankGap = S7_FLANK_SPACING
    s7FlankFromLeft = true
    s7KamiVSpawned = false
    s7HeavyLeftSpawned = false
    s7HeavyRightSpawned = false
    s7WagonsSpawned = false
    s7DrizzleGap = 0f
    s7DrizzleSeed = 1L
    s7PowerWaveSpawned = false
    s7KamiWallSpawned = false
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
    if (elapsed <= S7_FLANK_END) {
      s7FlankGap += dt
      var safeguard = 0
      while (s7FlankGap >= S7_FLANK_SPACING && safeguard < 2) {
        s7FlankGap -= S7_FLANK_SPACING
        safeguard++
        if (s7FlankFromLeft) {
          FormationSpawner.spawnS5FlankCascade(enemies, w, h, true)
        } else {
          FormationSpawner.spawnS5FlankCascade(enemies, w, h, false)
        }
        s7FlankFromLeft = !s7FlankFromLeft
      }
    }
    if (!s7KamiVSpawned && elapsed >= S7_KAMI_V_AT) {
      s7KamiVSpawned = true
      FormationSpawner.spawnS5CenterKamiV(enemies, w, h)
    }
    if (!s7HeavyLeftSpawned && elapsed >= S7_HEAVY_LEFT_AT) {
      s7HeavyLeftSpawned = true
      enemies.spawnEnemy(
        0.20f * w,
        -0.10f * h,
        0f,
        HEAVY_VY,
        TYPE_HEAVY,
        PATTERN_V_HOLD,
        S7_HEAVY_HP,
      )
    }
    if (!s7HeavyRightSpawned && elapsed >= S7_HEAVY_RIGHT_AT) {
      s7HeavyRightSpawned = true
      enemies.spawnEnemy(
        0.80f * w,
        -0.10f * h,
        0f,
        HEAVY_VY,
        TYPE_HEAVY,
        PATTERN_V_HOLD,
        S7_HEAVY_HP,
      )
    }
    if (!s7WagonsSpawned && elapsed >= S7_WAGONS_AT) {
      s7WagonsSpawned = true
      val y = -0.06f * h
      val vy = HEAVY_VY * 2.2f
      enemies.spawnEnemy(
        0.22f * w,
        y,
        0f,
        vy,
        TYPE_HEAVY,
        PATTERN_V_HOLD,
        S7_WAGON_HP,
        isWagon = true,
      )
      enemies.spawnEnemy(
        0.78f * w,
        y,
        0f,
        vy,
        TYPE_HEAVY,
        PATTERN_V_HOLD,
        S7_WAGON_HP,
        isWagon = true,
      )
    }
    if (elapsed >= S7_DRIZZLE_START && elapsed <= S7_DRIZZLE_END) {
      s7DrizzleGap += dt
      var safeguard = 0
      while (s7DrizzleGap >= S7_DRIZZLE_SPACING && safeguard < 2) {
        s7DrizzleGap -= S7_DRIZZLE_SPACING
        safeguard++
        s7DrizzleSeed = s7DrizzleSeed * 1664525L + 1013904223L
        val u = ((s7DrizzleSeed ushr 8) and 0xFFFFFFL).toFloat() / 16777215f
        val xFrac = 0.08f + u * 0.84f
        enemies.spawnEnemy(
          xFrac * w,
          -0.04f * h,
          0f,
          S7_DRIZZLE_VY,
          TYPE_INTERCEPTOR,
          PATTERN_WEAVE,
        )
      }
    }
    if (!s7PowerWaveSpawned && elapsed >= S7_POWER_WAVE_AT) {
      s7PowerWaveSpawned = true
      enemies.spawnEnemy(
        0.25f * w, -0.05f * h, 0f, S7_POWER_VY, TYPE_DRONE,
        isRedShipAnchor = true,
      )
      enemies.spawnEnemy(
        0.50f * w, -0.05f * h, 0f, S7_POWER_VY, TYPE_DRONE,
        isRedShipAnchor = true,
      )
      enemies.spawnEnemy(
        0.75f * w, -0.05f * h, 0f, S7_POWER_VY, TYPE_DRONE,
        isRedShipAnchor = true,
      )
    }
    if (!s7KamiWallSpawned && elapsed >= S7_KAMI_WALL_AT) {
      s7KamiWallSpawned = true
      var n = 0
      while (n < 6) {
        val xFrac = (n + 0.5f) / 6f
        enemies.spawnEnemy(xFrac * w, -0.06f * h, 0f, S7_KAMI_VY, TYPE_KAMIKAZE)
        n++
      }
    }
    if (elapsed >= S7_SCROLL_DECAY_AT) {
      var u = (elapsed - S7_SCROLL_DECAY_AT) / S7_SCROLL_DECAY_SPAN
      if (u < 0f) u = 0f
      if (u > 1f) u = 1f
      stageData.scrollSpeedY = S7_SCROLL_START * (1f - u)
    }
    if (allowBoss && elapsed >= stageData.def.bossAtSeconds) {
      cue.fireBoss(stageData.currentStage, boss)
      stageData.scrollSpeedY = 0f
    }
  }
}
