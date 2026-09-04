package com.cc.ww2blitz

import com.cc.ww2blitz.FormationSpawner.CROSS_VX
import com.cc.ww2blitz.FormationSpawner.CROSS_VY
import com.cc.ww2blitz.FormationSpawner.MAX_ACTIVE
import com.cc.ww2blitz.FormationSpawner.PATTERN_WEAVE
import com.cc.ww2blitz.FormationSpawner.S4_CROSS_AT
import com.cc.ww2blitz.FormationSpawner.S4_CROSS_Y
import com.cc.ww2blitz.FormationSpawner.S4_FLURRY_END
import com.cc.ww2blitz.FormationSpawner.S4_FLURRY_SPACING
import com.cc.ww2blitz.FormationSpawner.S4_FLURRY_START
import com.cc.ww2blitz.FormationSpawner.S4_FLURRY_VY
import com.cc.ww2blitz.FormationSpawner.S4_KAMI_AT
import com.cc.ww2blitz.FormationSpawner.S4_KAMI_VY
import com.cc.ww2blitz.FormationSpawner.S4_MID_AT
import com.cc.ww2blitz.FormationSpawner.S4_MID_HP
import com.cc.ww2blitz.FormationSpawner.S4_WALL_AT
import com.cc.ww2blitz.FormationSpawner.SWEEP_VX
import com.cc.ww2blitz.FormationSpawner.TYPE_DRONE
import com.cc.ww2blitz.FormationSpawner.TYPE_KAMIKAZE

/** Winter front: snow-lane weaves, tank captain, then the fortress. */
class Stage4Director : StageDirector {
  private var flurryGap = 0f
  private var midSpawned = false
  private var kamiSpawned = false
  private var crossSpawned = false
  private var wallSpawned = false

  override fun reset() {
    flurryGap = S4_FLURRY_SPACING
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
    if (!midSpawned && elapsed >= S4_MID_AT) {
      midSpawned = true
      FormationSpawner.spawnMidBoss(enemies, w, h, 0.50f, S4_MID_HP, isLandVehicle = true)
    }
    if (enemies.hasActiveMidBoss()) return
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
    if (!wallSpawned && elapsed >= S4_WALL_AT) {
      wallSpawned = true
      enemies.spawnEnemy(0.12f * w, -0.06f * h, 0f, S4_FLURRY_VY, TYPE_DRONE)
      enemies.spawnEnemy(0.36f * w, -0.10f * h, 0f, S4_FLURRY_VY, TYPE_DRONE)
      enemies.spawnEnemy(0.64f * w, -0.10f * h, 0f, S4_FLURRY_VY, TYPE_DRONE)
      enemies.spawnEnemy(0.88f * w, -0.06f * h, 0f, S4_FLURRY_VY, TYPE_DRONE)
    }
  }
}
