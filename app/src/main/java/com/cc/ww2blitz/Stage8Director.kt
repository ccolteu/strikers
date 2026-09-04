package com.cc.ww2blitz

/** Stage 8 intro is driven by the SpawnTimeline clock; this director is a no-op. */
class Stage8Director : StageDirector {
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
