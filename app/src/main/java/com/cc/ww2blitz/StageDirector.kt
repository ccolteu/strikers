package com.cc.ww2blitz

class DirectorCue {
  var bossCueFired = false

  fun fireBoss(stageId: Int, boss: BossController) {
    if (bossCueFired) return
    boss.beginEntranceForStage(stageId)
    bossCueFired = true
  }
}

interface StageDirector {
  fun reset()
  fun tick(
    dt: Float,
    elapsed: Float,
    enemies: EnemyPoolManager,
    w: Float,
    h: Float,
    boss: BossController,
    allowBoss: Boolean,
    stageData: StageData,
    cue: DirectorCue,
  )
}

/** One director instance per catalog id. Wave list comes from [StageDef.waveScript], not from id. */
object StageDirectors {
  fun create(waveScript: Int): StageDirector {
    return when (waveScript) {
      StageWaveKind.IRON_TREADS -> Stage2Director()
      StageWaveKind.STEEL_ATLANTIC -> Stage3Director()
      StageWaveKind.JUNGLE_RUINS -> Stage4Director()
      StageWaveKind.ASCENT_CANOPY -> Stage5Director()
      StageWaveKind.ORBIT_INTRO -> Stage6Director()
      StageWaveKind.FROZEN_FRONT -> Stage7Director()
      else -> Stage1Director()
    }
  }

  fun table(): Array<StageDirector?> {
    val slots = arrayOfNulls<StageDirector>(StageCatalog.maxId() + 1)
    val catalog = StageCatalog.all
    var i = 0
    while (i < catalog.size) {
      val def = catalog[i]
      if (def.id >= 0 && def.id < slots.size) {
        slots[def.id] = create(def.waveScript)
      }
      i++
    }
    return slots
  }
}
