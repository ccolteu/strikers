package com.cc.ww2blitz

import com.cc.ww2blitz.FormationSpawner.MAX_ACTIVE
import com.cc.ww2blitz.FormationSpawner.PATTERN_WEAVE
import com.cc.ww2blitz.FormationSpawner.S6_CRUISER_AT
import com.cc.ww2blitz.FormationSpawner.S6_CRUISER_HP
import com.cc.ww2blitz.FormationSpawner.S6_FLANK_END
import com.cc.ww2blitz.FormationSpawner.S6_FLANK_SPACING
import com.cc.ww2blitz.FormationSpawner.S6_FLANK_START
import com.cc.ww2blitz.FormationSpawner.S6_FLANK_VX
import com.cc.ww2blitz.FormationSpawner.S6_HOLD_V_AT
import com.cc.ww2blitz.FormationSpawner.S6_KAMI_AT
import com.cc.ww2blitz.FormationSpawner.S6_KAMI_VY
import com.cc.ww2blitz.FormationSpawner.S6_WALL_COUNT
import com.cc.ww2blitz.FormationSpawner.S6_WALL_END
import com.cc.ww2blitz.FormationSpawner.S6_WALL_SPACING
import com.cc.ww2blitz.FormationSpawner.S6_WALL_START
import com.cc.ww2blitz.FormationSpawner.S6_WALL_VY
import com.cc.ww2blitz.FormationSpawner.S6_WEAVE_END
import com.cc.ww2blitz.FormationSpawner.S6_WEAVE_PAIRS
import com.cc.ww2blitz.FormationSpawner.S6_WEAVE_SPACING
import com.cc.ww2blitz.FormationSpawner.S6_WEAVE_START
import com.cc.ww2blitz.FormationSpawner.S6_WEAVE_VY
import com.cc.ww2blitz.FormationSpawner.TYPE_DRONE
import com.cc.ww2blitz.FormationSpawner.TYPE_KAMIKAZE

class Stage6Director : StageDirector {
  private var s6FlankTimer = 0f
  private var s6WeaveTimer = 0f
  private var s6WeaveCount = 0
  private var s6KamiSpawned = false
  private var s6WallCount = 0
  private var s6CruiserSpawned = false
  private var s6HoldVSpawned = false

  override fun reset() {
    s6FlankTimer = S6_FLANK_SPACING
    s6WeaveTimer = S6_WEAVE_SPACING
    s6WeaveCount = 0
    s6KamiSpawned = false
    s6WallCount = 0
    s6CruiserSpawned = false
    s6HoldVSpawned = false
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
    if (elapsed >= S6_FLANK_START && elapsed <= S6_FLANK_END) {
      s6FlankTimer += dt
      var safeguard = 0
      while (s6FlankTimer >= S6_FLANK_SPACING && safeguard < 2) {
        if (enemies.countActive() >= MAX_ACTIVE) break
        s6FlankTimer -= S6_FLANK_SPACING
        safeguard++
        val y = h * 0.40f
        enemies.spawnEnemy(-0.08f * w, y, S6_FLANK_VX, 0f, TYPE_DRONE)
        enemies.spawnEnemy(1.08f * w, y, -S6_FLANK_VX, 0f, TYPE_DRONE)
      }
    }
    if (
      elapsed >= S6_WEAVE_START &&
      elapsed <= S6_WEAVE_END &&
      s6WeaveCount < S6_WEAVE_PAIRS
    ) {
      s6WeaveTimer += dt
      var safeguard = 0
      while (s6WeaveCount < S6_WEAVE_PAIRS && s6WeaveTimer >= S6_WEAVE_SPACING && safeguard < 2) {
        if (enemies.countActive() >= MAX_ACTIVE) break
        s6WeaveTimer -= S6_WEAVE_SPACING
        safeguard++
        s6WeaveCount++
        enemies.spawnEnemy(0.12f * w, -0.02f * h, 0f, S6_WEAVE_VY, TYPE_DRONE, PATTERN_WEAVE)
        enemies.spawnEnemy(0.88f * w, -0.02f * h, 0f, S6_WEAVE_VY, TYPE_DRONE, PATTERN_WEAVE)
      }
    }
    if (!s6CruiserSpawned && elapsed >= S6_CRUISER_AT) {
      s6CruiserSpawned = true
      FormationSpawner.spawnMidBoss(
        enemies,
        w,
        h,
        0.50f,
        S6_CRUISER_HP,
        isHelicopter = true,
      )
    }
    if (enemies.hasActiveMidBoss()) return
    if (!s6KamiSpawned && elapsed >= S6_KAMI_AT) {
      s6KamiSpawned = true
      enemies.spawnEnemy(-0.06f * w, 0.22f * h, S6_FLANK_VX * 0.90f, S6_KAMI_VY * 0.70f, TYPE_KAMIKAZE)
      enemies.spawnEnemy(1.06f * w, 0.22f * h, -S6_FLANK_VX * 0.90f, S6_KAMI_VY * 0.70f, TYPE_KAMIKAZE)
    }
    if (elapsed >= S6_WALL_START && elapsed <= S6_WALL_END && s6WallCount < S6_WALL_COUNT) {
      val at = S6_WALL_START + s6WallCount * S6_WALL_SPACING
      if (elapsed >= at) {
        val lane = if (s6WallCount == 0) {
          0.10f
        } else if (s6WallCount == 1) {
          0.22f
        } else if (s6WallCount == 2) {
          0.78f
        } else {
          0.90f
        }
        enemies.spawnEnemy(lane * w, -0.04f * h, 0f, S6_WALL_VY, TYPE_DRONE)
        s6WallCount++
      }
    }
    if (!s6HoldVSpawned && elapsed >= S6_HOLD_V_AT) {
      s6HoldVSpawned = true
      FormationSpawner.spawnVFormation(enemies, w, h)
    }
  }
}
