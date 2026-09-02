package com.cc.ww2blitz

import com.cc.ww2blitz.FormationSpawner.CROSS_VX
import com.cc.ww2blitz.FormationSpawner.CROSS_VY
import com.cc.ww2blitz.FormationSpawner.FLANK_END
import com.cc.ww2blitz.FormationSpawner.FLANK_SPACING
import com.cc.ww2blitz.FormationSpawner.HEAVY_HP
import com.cc.ww2blitz.FormationSpawner.HEAVY_VY
import com.cc.ww2blitz.FormationSpawner.MAX_ACTIVE
import com.cc.ww2blitz.FormationSpawner.OPENING_END
import com.cc.ww2blitz.FormationSpawner.PATTERN_WEAVE
import com.cc.ww2blitz.FormationSpawner.S1_CROSS_AT
import com.cc.ww2blitz.FormationSpawner.S1_CROSS_Y
import com.cc.ww2blitz.FormationSpawner.SWEEP_VX
import com.cc.ww2blitz.FormationSpawner.SWEEP_VY
import com.cc.ww2blitz.FormationSpawner.TYPE_DRONE
import com.cc.ww2blitz.FormationSpawner.TYPE_HEAVY
import com.cc.ww2blitz.FormationSpawner.V_FORM_AT
import com.cc.ww2blitz.FormationSpawner.WALL_AT
import com.cc.ww2blitz.FormationSpawner.WEAVE_AT
import com.cc.ww2blitz.FormationSpawner.WEAVE_END
import com.cc.ww2blitz.FormationSpawner.WEAVE_SPACING
import com.cc.ww2blitz.FormationSpawner.WEAVE_VY

class Stage1Director : StageDirector {
  private var flankGap = 0f
  private var weaveGap = 0f
  private var vFormSpawned = false
  private var s1CrossSpawned = false
  private var wallSpawned = false
  private var weaveStarted = false

  override fun reset() {
    flankGap = FLANK_SPACING
    weaveGap = 0f
    vFormSpawned = false
    s1CrossSpawned = false
    wallSpawned = false
    weaveStarted = false
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
    if (elapsed >= OPENING_END && elapsed <= FLANK_END) {
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
    if (!vFormSpawned && elapsed >= V_FORM_AT) {
      vFormSpawned = true
      FormationSpawner.spawnVFormation(enemies, w, h)
    }
    if (!weaveStarted && elapsed >= WEAVE_AT) {
      weaveStarted = true
      weaveGap = WEAVE_SPACING
    }
    if (elapsed >= WEAVE_AT && elapsed <= WEAVE_END) {
      weaveGap += dt
      var safeguard = 0
      while (weaveGap >= WEAVE_SPACING && safeguard < 2) {
        if (enemies.countActive() >= MAX_ACTIVE) break
        weaveGap -= WEAVE_SPACING
        safeguard++
        enemies.spawnEnemy(0.14f * w, -0.02f * h, 0f, WEAVE_VY, TYPE_DRONE, PATTERN_WEAVE)
        enemies.spawnEnemy(0.86f * w, -0.02f * h, 0f, WEAVE_VY, TYPE_DRONE, PATTERN_WEAVE)
      }
    }
    if (!s1CrossSpawned && elapsed >= S1_CROSS_AT) {
      s1CrossSpawned = true
      FormationSpawner.spawnSideCross(enemies, w, h, S1_CROSS_Y, CROSS_VX, CROSS_VY, TYPE_DRONE)
    }
    if (!wallSpawned && elapsed >= WALL_AT) {
      wallSpawned = true
      enemies.spawnEnemy(0.30f * w, -0.10f * h, 0f, HEAVY_VY, TYPE_HEAVY, 0, HEAVY_HP)
      enemies.spawnEnemy(0.70f * w, -0.10f * h, 0f, HEAVY_VY, TYPE_HEAVY, 0, HEAVY_HP)
    }
  }
}
