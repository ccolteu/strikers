package com.cc.ww2blitz

import com.cc.ww2blitz.FormationSpawner.MAX_ACTIVE
import com.cc.ww2blitz.FormationSpawner.PATTERN_V_HOLD
import com.cc.ww2blitz.FormationSpawner.PATTERN_WEAVE
import com.cc.ww2blitz.FormationSpawner.S4_CRUISER_AT
import com.cc.ww2blitz.FormationSpawner.S4_CRUISER_HP
import com.cc.ww2blitz.FormationSpawner.S4_CRUISER_VY
import com.cc.ww2blitz.FormationSpawner.S4_FLANK_END
import com.cc.ww2blitz.FormationSpawner.S4_FLANK_SPACING
import com.cc.ww2blitz.FormationSpawner.S4_FLANK_START
import com.cc.ww2blitz.FormationSpawner.S4_FLANK_VX
import com.cc.ww2blitz.FormationSpawner.S4_HOLD_V_AT
import com.cc.ww2blitz.FormationSpawner.S4_KAMI_AT
import com.cc.ww2blitz.FormationSpawner.S4_KAMI_VY
import com.cc.ww2blitz.FormationSpawner.S4_WALL_COUNT
import com.cc.ww2blitz.FormationSpawner.S4_WALL_END
import com.cc.ww2blitz.FormationSpawner.S4_WALL_SPACING
import com.cc.ww2blitz.FormationSpawner.S4_WALL_START
import com.cc.ww2blitz.FormationSpawner.S4_WALL_VY
import com.cc.ww2blitz.FormationSpawner.S4_WEAVE_END
import com.cc.ww2blitz.FormationSpawner.S4_WEAVE_PAIRS
import com.cc.ww2blitz.FormationSpawner.S4_WEAVE_SPACING
import com.cc.ww2blitz.FormationSpawner.S4_WEAVE_START
import com.cc.ww2blitz.FormationSpawner.S4_WEAVE_VY
import com.cc.ww2blitz.FormationSpawner.TYPE_DRONE
import com.cc.ww2blitz.FormationSpawner.TYPE_HEAVY
import com.cc.ww2blitz.FormationSpawner.TYPE_KAMIKAZE

class Stage4Director : StageDirector {
  private var s4FlankTimer = 0f
  private var s4WeaveTimer = 0f
  private var s4WeaveCount = 0
  private var s4KamiSpawned = false
  private var s4WallCount = 0
  private var s4CruiserSpawned = false
  private var s4HoldVSpawned = false

  override fun reset() {
    s4FlankTimer = S4_FLANK_SPACING
    s4WeaveTimer = S4_WEAVE_SPACING
    s4WeaveCount = 0
    s4KamiSpawned = false
    s4WallCount = 0
    s4CruiserSpawned = false
    s4HoldVSpawned = false
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
    if (elapsed >= S4_FLANK_START && elapsed <= S4_FLANK_END) {
      s4FlankTimer += dt
      var safeguard = 0
      while (s4FlankTimer >= S4_FLANK_SPACING && safeguard < 2) {
        if (enemies.countActive() >= MAX_ACTIVE) break
        s4FlankTimer -= S4_FLANK_SPACING
        safeguard++
        val y = h * 0.40f
        enemies.spawnEnemy(-0.08f * w, y, S4_FLANK_VX, 0f, TYPE_DRONE)
        enemies.spawnEnemy(1.08f * w, y, -S4_FLANK_VX, 0f, TYPE_DRONE)
      }
    }
    if (
      elapsed >= S4_WEAVE_START &&
      elapsed <= S4_WEAVE_END &&
      s4WeaveCount < S4_WEAVE_PAIRS
    ) {
      s4WeaveTimer += dt
      var safeguard = 0
      while (s4WeaveCount < S4_WEAVE_PAIRS && s4WeaveTimer >= S4_WEAVE_SPACING && safeguard < 2) {
        if (enemies.countActive() >= MAX_ACTIVE) break
        s4WeaveTimer -= S4_WEAVE_SPACING
        safeguard++
        s4WeaveCount++
        enemies.spawnEnemy(0.12f * w, -0.02f * h, 0f, S4_WEAVE_VY, TYPE_DRONE, PATTERN_WEAVE)
        enemies.spawnEnemy(0.88f * w, -0.02f * h, 0f, S4_WEAVE_VY, TYPE_DRONE, PATTERN_WEAVE)
      }
    }
    if (!s4CruiserSpawned && elapsed >= S4_CRUISER_AT) {
      s4CruiserSpawned = true
      enemies.spawnEnemy(
        0.50f * w,
        -0.10f * h,
        0f,
        S4_CRUISER_VY,
        TYPE_HEAVY,
        PATTERN_V_HOLD,
        S4_CRUISER_HP,
      )
    }
    if (!s4KamiSpawned && elapsed >= S4_KAMI_AT) {
      s4KamiSpawned = true
      enemies.spawnEnemy(-0.06f * w, 0.22f * h, S4_FLANK_VX * 0.90f, S4_KAMI_VY * 0.70f, TYPE_KAMIKAZE)
      enemies.spawnEnemy(1.06f * w, 0.22f * h, -S4_FLANK_VX * 0.90f, S4_KAMI_VY * 0.70f, TYPE_KAMIKAZE)
    }
    if (elapsed >= S4_WALL_START && elapsed <= S4_WALL_END && s4WallCount < S4_WALL_COUNT) {
      val at = S4_WALL_START + s4WallCount * S4_WALL_SPACING
      if (elapsed >= at) {
        val lane = if (s4WallCount == 0) {
          0.10f
        } else if (s4WallCount == 1) {
          0.22f
        } else if (s4WallCount == 2) {
          0.78f
        } else {
          0.90f
        }
        enemies.spawnEnemy(lane * w, -0.04f * h, 0f, S4_WALL_VY, TYPE_DRONE)
        s4WallCount++
      }
    }
    if (!s4HoldVSpawned && elapsed >= S4_HOLD_V_AT) {
      s4HoldVSpawned = true
      FormationSpawner.spawnVFormation(enemies, w, h)
    }
  }
}
