package com.cc.ww2blitz

import com.cc.ww2blitz.FormationSpawner.CROSS_VX
import com.cc.ww2blitz.FormationSpawner.CROSS_VY
import com.cc.ww2blitz.FormationSpawner.MAX_ACTIVE
import com.cc.ww2blitz.FormationSpawner.PATTERN_WEAVE
import com.cc.ww2blitz.FormationSpawner.S5_CROSS_AT
import com.cc.ww2blitz.FormationSpawner.S5_CROSS_Y
import com.cc.ww2blitz.FormationSpawner.S5_KAMI_AT
import com.cc.ww2blitz.FormationSpawner.S5_KAMI_VY
import com.cc.ww2blitz.FormationSpawner.S5_MID_AT
import com.cc.ww2blitz.FormationSpawner.S5_MID_HP
import com.cc.ww2blitz.FormationSpawner.S5_REEF_END
import com.cc.ww2blitz.FormationSpawner.S5_REEF_SPACING
import com.cc.ww2blitz.FormationSpawner.S5_REEF_START
import com.cc.ww2blitz.FormationSpawner.S5_REEF_VY
import com.cc.ww2blitz.FormationSpawner.S5_WALL_AT
import com.cc.ww2blitz.FormationSpawner.SWEEP_VX
import com.cc.ww2blitz.FormationSpawner.TYPE_DRONE
import com.cc.ww2blitz.FormationSpawner.TYPE_KAMIKAZE

/** Pacific atoll: reef weaves, mid-lane captain, then the helipad. */
class Stage5Director : StageDirector {
  private var reefGap = 0f
  private var midSpawned = false
  private var kamiSpawned = false
  private var crossSpawned = false
  private var wallSpawned = false

  override fun reset() {
    reefGap = S5_REEF_SPACING
    midSpawned = false
    kamiSpawned = false
    crossSpawned = false
    wallSpawned = false
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
    if (elapsed >= S5_REEF_START && elapsed <= S5_REEF_END) {
      reefGap += dt
      var safeguard = 0
      while (reefGap >= S5_REEF_SPACING && safeguard < 2) {
        if (enemies.countActive() >= MAX_ACTIVE) break
        reefGap -= S5_REEF_SPACING
        safeguard++
        enemies.spawnEnemy(0.22f * w, -0.05f * h, 0f, S5_REEF_VY, TYPE_DRONE, PATTERN_WEAVE)
        enemies.spawnEnemy(0.78f * w, -0.05f * h, 0f, S5_REEF_VY * 1.06f, TYPE_DRONE, PATTERN_WEAVE)
      }
    }
    if (!midSpawned && elapsed >= S5_MID_AT) {
      midSpawned = true
      FormationSpawner.spawnMidBoss(enemies, w, h, 0.50f, S5_MID_HP)
    }
    if (enemies.hasActiveMidBoss()) return
    if (!kamiSpawned && elapsed >= S5_KAMI_AT) {
      kamiSpawned = true
      enemies.spawnEnemy(-0.06f * w, 0.32f * h, SWEEP_VX * 0.90f, S5_KAMI_VY, TYPE_KAMIKAZE)
      enemies.spawnEnemy(1.06f * w, 0.32f * h, -SWEEP_VX * 0.90f, S5_KAMI_VY, TYPE_KAMIKAZE)
    }
    if (!crossSpawned && elapsed >= S5_CROSS_AT) {
      if (enemies.countActive() < MAX_ACTIVE) {
        crossSpawned = true
        FormationSpawner.spawnSideCross(enemies, w, h, S5_CROSS_Y, CROSS_VX * 1.05f, CROSS_VY, TYPE_DRONE)
      }
    }
    if (!wallSpawned && elapsed >= S5_WALL_AT) {
      wallSpawned = true
      enemies.spawnEnemy(0.14f * w, -0.06f * h, 0f, S5_REEF_VY * 1.15f, TYPE_DRONE)
      enemies.spawnEnemy(0.38f * w, -0.10f * h, 0f, S5_REEF_VY * 1.15f, TYPE_DRONE)
      enemies.spawnEnemy(0.62f * w, -0.10f * h, 0f, S5_REEF_VY * 1.15f, TYPE_DRONE)
      enemies.spawnEnemy(0.86f * w, -0.06f * h, 0f, S5_REEF_VY * 1.15f, TYPE_DRONE)
    }
  }
}
