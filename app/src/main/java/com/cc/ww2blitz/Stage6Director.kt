package com.cc.ww2blitz

/** Stage 6 intro is driven by the SpawnTimeline clock; this director is a no-op. */
class Stage6Director : StageDirector {
  override fun reset() {}

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
  }
}
