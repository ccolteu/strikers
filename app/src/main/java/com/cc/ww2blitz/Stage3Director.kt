package com.cc.ww2blitz

import com.cc.ww2blitz.FormationSpawner.CROSS_VX
import com.cc.ww2blitz.FormationSpawner.CROSS_VY
import com.cc.ww2blitz.FormationSpawner.FAST_DOWN
import com.cc.ww2blitz.FormationSpawner.HEAVY_VY
import com.cc.ww2blitz.FormationSpawner.INTERCEPT_HP
import com.cc.ww2blitz.FormationSpawner.MAX_ACTIVE
import com.cc.ww2blitz.FormationSpawner.PATTERN_V_HOLD
import com.cc.ww2blitz.FormationSpawner.S3_CROSS_AT
import com.cc.ww2blitz.FormationSpawner.S3_CROSS_Y
import com.cc.ww2blitz.FormationSpawner.S3_CRUISER_AT
import com.cc.ww2blitz.FormationSpawner.S3_CRUISER_HP
import com.cc.ww2blitz.FormationSpawner.S3_DESTROYER_AT
import com.cc.ww2blitz.FormationSpawner.S3_DESTROYER_HP
import com.cc.ww2blitz.FormationSpawner.S3_FLANK_END
import com.cc.ww2blitz.FormationSpawner.S3_FLANK_SPACING
import com.cc.ww2blitz.FormationSpawner.S3_FLANK_START
import com.cc.ww2blitz.FormationSpawner.S3_SCOUT_END
import com.cc.ww2blitz.FormationSpawner.S3_SCOUT_SPACING
import com.cc.ww2blitz.FormationSpawner.S3_SCOUT_START
import com.cc.ww2blitz.FormationSpawner.SWEEP_VX
import com.cc.ww2blitz.FormationSpawner.TYPE_DRONE
import com.cc.ww2blitz.FormationSpawner.TYPE_HEAVY
import com.cc.ww2blitz.FormationSpawner.TYPE_INTERCEPTOR

class Stage3Director : StageDirector {
  private var s3ScoutGap = 0f
  private var s3FlankGap = 0f
  private var s3DestroyersSpawned = false
  private var s3CruiserSpawned = false
  private var s3MidCrossSpawned = false
  private var s3FlankFromLeft = true

  override fun reset() {
    s3ScoutGap = S3_SCOUT_SPACING
    s3FlankGap = S3_FLANK_SPACING
    s3DestroyersSpawned = false
    s3CruiserSpawned = false
    s3MidCrossSpawned = false
    s3FlankFromLeft = true
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
    if (elapsed >= S3_SCOUT_START && elapsed <= S3_SCOUT_END) {
      s3ScoutGap += dt
      var safeguard = 0
      while (s3ScoutGap >= S3_SCOUT_SPACING && safeguard < 2) {
        if (enemies.countActive() >= MAX_ACTIVE) break
        s3ScoutGap -= S3_SCOUT_SPACING
        safeguard++
        FormationSpawner.spawnStage3ScoutV(enemies, w, h)
      }
    }
    if (!s3CruiserSpawned && elapsed >= S3_CRUISER_AT) {
      s3CruiserSpawned = true
      val y = -0.10f * h
      val vy = HEAVY_VY
      enemies.spawnEnemy(0.30f * w, y, 0f, vy, TYPE_HEAVY, PATTERN_V_HOLD, S3_CRUISER_HP)
      enemies.spawnEnemy(0.70f * w, y, 0f, vy, TYPE_HEAVY, PATTERN_V_HOLD, S3_CRUISER_HP)
    }
    if (!s3DestroyersSpawned && elapsed >= S3_DESTROYER_AT) {
      s3DestroyersSpawned = true
      val y = -0.06f * h
      val vy = HEAVY_VY * 2.2f
      enemies.spawnEnemy(
        0.28f * w,
        y,
        0f,
        vy,
        TYPE_HEAVY,
        PATTERN_V_HOLD,
        S3_DESTROYER_HP,
        isDestroyer = true,
      )
      enemies.spawnEnemy(
        0.72f * w,
        y,
        0f,
        vy,
        TYPE_HEAVY,
        PATTERN_V_HOLD,
        S3_DESTROYER_HP,
        isDestroyer = true,
      )
    }
    if (!s3MidCrossSpawned && elapsed >= S3_CROSS_AT) {
      if (enemies.countActive() < MAX_ACTIVE) {
        s3MidCrossSpawned = true
        FormationSpawner.spawnSideCross(enemies, w, h, S3_CROSS_Y, CROSS_VX, CROSS_VY, TYPE_DRONE)
      }
    }
    if (elapsed >= S3_FLANK_START && elapsed <= S3_FLANK_END) {
      s3FlankGap += dt
      var safeguard = 0
      while (s3FlankGap >= S3_FLANK_SPACING && safeguard < 2) {
        if (enemies.countActive() >= MAX_ACTIVE) break
        s3FlankGap -= S3_FLANK_SPACING
        safeguard++
        val laneY = if (s3FlankFromLeft) {
          0.10f * h
        } else {
          0.10f * h + FormationSpawner.formGapY(enemies, TYPE_INTERCEPTOR)
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
}
