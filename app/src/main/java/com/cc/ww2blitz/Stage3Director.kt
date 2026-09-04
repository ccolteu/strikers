package com.cc.ww2blitz

import com.cc.ww2blitz.FormationSpawner.CROSS_VX
import com.cc.ww2blitz.FormationSpawner.CROSS_VY
import com.cc.ww2blitz.FormationSpawner.FAST_DOWN
import com.cc.ww2blitz.FormationSpawner.INTERCEPT_HP
import com.cc.ww2blitz.FormationSpawner.MAX_ACTIVE
import com.cc.ww2blitz.FormationSpawner.S3_CROSS_AT
import com.cc.ww2blitz.FormationSpawner.S3_CROSS_Y
import com.cc.ww2blitz.FormationSpawner.S3_FLANK_END
import com.cc.ww2blitz.FormationSpawner.S3_FLANK_SPACING
import com.cc.ww2blitz.FormationSpawner.S3_FLANK_START
import com.cc.ww2blitz.FormationSpawner.S3_MID_AT
import com.cc.ww2blitz.FormationSpawner.S3_MID_HP
import com.cc.ww2blitz.FormationSpawner.S3_RECOVERY_AT
import com.cc.ww2blitz.FormationSpawner.S3_SCOUT_END
import com.cc.ww2blitz.FormationSpawner.S3_SCOUT_SPACING
import com.cc.ww2blitz.FormationSpawner.S3_SCOUT_START
import com.cc.ww2blitz.FormationSpawner.SWEEP_VX
import com.cc.ww2blitz.FormationSpawner.TYPE_INTERCEPTOR

class Stage3Director : StageDirector {
  private var s3ScoutGap = 0f
  private var s3FlankGap = 0f
  private var s3MidSpawned = false
  private var s3RecoverySpawned = false
  private var s3MidCrossSpawned = false
  private var s3FlankFromLeft = true

  override fun reset() {
    s3ScoutGap = S3_SCOUT_SPACING
    s3FlankGap = S3_FLANK_SPACING
    s3MidSpawned = false
    s3RecoverySpawned = false
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
    if (!s3MidCrossSpawned && elapsed >= S3_CROSS_AT) {
      if (enemies.countActive() < MAX_ACTIVE) {
        s3MidCrossSpawned = true
        FormationSpawner.spawnSideCross(enemies, w, h, S3_CROSS_Y, CROSS_VX, CROSS_VY, FormationSpawner.TYPE_DRONE)
      }
    }
    if (!s3MidSpawned && elapsed >= S3_MID_AT) {
      s3MidSpawned = true
      FormationSpawner.spawnMidBoss(enemies, w, h, 0.50f, S3_MID_HP, isDestroyer = true)
    }
    if (enemies.hasActiveMidBoss()) return
    if (!s3RecoverySpawned && elapsed >= S3_RECOVERY_AT) {
      s3RecoverySpawned = true
      enemies.spawnEnemy(0.14f * w, -0.06f * h, 0f, FAST_DOWN, FormationSpawner.TYPE_DRONE)
      enemies.spawnEnemy(0.38f * w, -0.10f * h, 0f, FAST_DOWN, FormationSpawner.TYPE_DRONE)
      enemies.spawnEnemy(0.62f * w, -0.10f * h, 0f, FAST_DOWN, FormationSpawner.TYPE_DRONE)
      enemies.spawnEnemy(0.86f * w, -0.06f * h, 0f, FAST_DOWN, FormationSpawner.TYPE_DRONE)
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
