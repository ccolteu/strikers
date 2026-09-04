package com.cc.ww2blitz

import com.cc.ww2blitz.FormationSpawner.CROSS_VX
import com.cc.ww2blitz.FormationSpawner.CROSS_VY
import com.cc.ww2blitz.FormationSpawner.HEAVY_HP
import com.cc.ww2blitz.FormationSpawner.HEAVY_VY
import com.cc.ww2blitz.FormationSpawner.MAX_ACTIVE
import com.cc.ww2blitz.FormationSpawner.PATTERN_V_HOLD
import com.cc.ww2blitz.FormationSpawner.PATTERN_WEAVE
import com.cc.ww2blitz.FormationSpawner.S4_CROSS_AT
import com.cc.ww2blitz.FormationSpawner.S4_CROSS_Y
import com.cc.ww2blitz.FormationSpawner.S4_FLURRY_END
import com.cc.ww2blitz.FormationSpawner.S4_FLURRY_SPACING
import com.cc.ww2blitz.FormationSpawner.S4_FLURRY_START
import com.cc.ww2blitz.FormationSpawner.S4_FLURRY_VY
import com.cc.ww2blitz.FormationSpawner.S4_HEAVIES_AT
import com.cc.ww2blitz.FormationSpawner.S4_HOLD_V_AT
import com.cc.ww2blitz.FormationSpawner.S4_KAMI_AT
import com.cc.ww2blitz.FormationSpawner.S4_KAMI_VY
import com.cc.ww2blitz.FormationSpawner.S4_WALL_AT
import com.cc.ww2blitz.FormationSpawner.SWEEP_VX
import com.cc.ww2blitz.FormationSpawner.TYPE_DRONE
import com.cc.ww2blitz.FormationSpawner.TYPE_HEAVY
import com.cc.ww2blitz.FormationSpawner.TYPE_KAMIKAZE

/** Winter front: slow snow-lane weaves, then a keep. Shared boss cue from the def. */
class Stage4Director : StageDirector {
  private var flurryGap = 0f
  private var holdVSpawned = false
  private var kamiSpawned = false
  private var crossSpawned = false
  private var heaviesSpawned = false
  private var wallSpawned = false

  override fun reset() {
    flurryGap = S4_FLURRY_SPACING
    holdVSpawned = false
    kamiSpawned = false
    crossSpawned = false
    heaviesSpawned = false
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
    if (elapsed >= S4_FLURRY_START && elapsed <= S4_FLURRY_END) {
      flurryGap += dt
      var safeguard = 0
      while (flurryGap >= S4_FLURRY_SPACING && safeguard < 2) {
        if (enemies.countActive() >= MAX_ACTIVE) break
        flurryGap -= S4_FLURRY_SPACING
        safeguard++
        enemies.spawnEnemy(0.18f * w, -0.04f * h, 0f, S4_FLURRY_VY, TYPE_DRONE, PATTERN_WEAVE)
        enemies.spawnEnemy(0.50f * w, -0.08f * h, 0f, S4_FLURRY_VY * 0.92f, TYPE_DRONE, PATTERN_WEAVE)
        enemies.spawnEnemy(0.82f * w, -0.04f * h, 0f, S4_FLURRY_VY, TYPE_DRONE, PATTERN_WEAVE)
      }
    }
    if (!holdVSpawned && elapsed >= S4_HOLD_V_AT) {
      holdVSpawned = true
      FormationSpawner.spawnVFormation(enemies, w, h)
    }
    if (!kamiSpawned && elapsed >= S4_KAMI_AT) {
      kamiSpawned = true
      enemies.spawnEnemy(-0.06f * w, 0.28f * h, SWEEP_VX * 0.85f, S4_KAMI_VY, TYPE_KAMIKAZE)
      enemies.spawnEnemy(1.06f * w, 0.28f * h, -SWEEP_VX * 0.85f, S4_KAMI_VY, TYPE_KAMIKAZE)
    }
    if (!crossSpawned && elapsed >= S4_CROSS_AT) {
      if (enemies.countActive() < MAX_ACTIVE) {
        crossSpawned = true
        FormationSpawner.spawnSideCross(enemies, w, h, S4_CROSS_Y, CROSS_VX, CROSS_VY, TYPE_DRONE)
      }
    }
    if (!heaviesSpawned && elapsed >= S4_HEAVIES_AT) {
      heaviesSpawned = true
      enemies.spawnEnemy(0.28f * w, -0.10f * h, 0f, HEAVY_VY, TYPE_HEAVY, PATTERN_V_HOLD, HEAVY_HP)
      enemies.spawnEnemy(0.72f * w, -0.10f * h, 0f, HEAVY_VY, TYPE_HEAVY, PATTERN_V_HOLD, HEAVY_HP)
    }
    if (!wallSpawned && elapsed >= S4_WALL_AT) {
      wallSpawned = true
      enemies.spawnEnemy(0.12f * w, -0.06f * h, 0f, S4_FLURRY_VY, TYPE_DRONE)
      enemies.spawnEnemy(0.36f * w, -0.10f * h, 0f, S4_FLURRY_VY, TYPE_DRONE)
      enemies.spawnEnemy(0.64f * w, -0.10f * h, 0f, S4_FLURRY_VY, TYPE_DRONE)
      enemies.spawnEnemy(0.88f * w, -0.06f * h, 0f, S4_FLURRY_VY, TYPE_DRONE)
    }
  }
}
