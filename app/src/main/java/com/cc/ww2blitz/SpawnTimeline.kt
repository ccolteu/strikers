package com.cc.ww2blitz

import com.cc.ww2blitz.FormationSpawner.OPENING_END
import com.cc.ww2blitz.FormationSpawner.POWER_WAVE_DELAY

/**
 * Thin elapsed-time clock. Per-stage spawn scripts live on StageDirector implementations.
 */
class SpawnTimeline {

  private var elapsedTime = 0f
  private var activeStage = 0
  private var openingPowerVSpawned = false
  private var powerUpWaveQueued = false
  private var powerUpWaveTimer = 0f
  private val cue = DirectorCue()
  private val directors = StageDirectors.table()

  fun elapsedSeconds(): Float = elapsedTime

  fun forceElapsed(targetSeconds: Float) {
    elapsedTime = targetSeconds
  }

  fun update(
    dt: Float,
    enemyManager: EnemyPoolManager,
    screenWidth: Int,
    screenHeight: Int,
    boss: BossController,
    bossEnterSeconds: Float,
    allowBoss: Boolean,
    playerWeaponPower: Int,
    stageData: StageData,
  ) {
    if (screenWidth <= 0 || screenHeight <= 0) return
    activeStage = stageData.currentStage
    val def = StageCatalog.get(activeStage)
    if (def.introOnly) {
      val w = screenWidth.toFloat()
      val h = screenHeight.toFloat()
      if (!cue.bossCueFired) {
        elapsedTime += dt
        val introSecs = def.introSecs
        if (allowBoss && elapsedTime >= introSecs) {
          cue.fireBoss(def.id, boss)
          elapsedTime = introSecs
        }
      }
      HiddenMedalRoute.bind(activeStage)
      HiddenMedalRoute.tick(elapsedTime, w, h, PowerUpManager.instance.items)
      return
    }
    if (!(def.locksElapsedAtBoss && cue.bossCueFired)) {
      elapsedTime += dt
    }
    val w = screenWidth.toFloat()
    val h = screenHeight.toFloat()
    if (def.usesOpeningPowerV && !openingPowerVSpawned && elapsedTime >= 0f && elapsedTime <= OPENING_END) {
      openingPowerVSpawned = true
      FormationSpawner.spawnOpeningPowerV(enemyManager, w, h)
    }
    updatePowerSafeguard(dt, enemyManager, w, h, playerWeaponPower, boss)
    val dir = if (def.id >= 0 && def.id < directors.size) directors[def.id] else null
    dir?.tick(dt, elapsedTime, enemyManager, w, h, boss, allowBoss, stageData, cue)
    HiddenMedalRoute.bind(activeStage)
    HiddenMedalRoute.tick(elapsedTime, w, h, PowerUpManager.instance.items)
    if (allowBoss && !cue.bossCueFired && def.usesSharedBossEntranceCue &&
      elapsedTime >= def.bossAtSeconds
    ) {
      cue.fireBoss(def.id, boss)
    }
  }

  fun reset() {
    elapsedTime = 0f
    activeStage = 0
    openingPowerVSpawned = false
    powerUpWaveQueued = false
    powerUpWaveTimer = 0f
    cue.bossCueFired = false
    HiddenMedalRoute.reset()
    var i = 0
    while (i < directors.size) {
      directors[i]?.reset()
      i++
    }
  }

  private fun updatePowerSafeguard(
    dt: Float,
    enemies: EnemyPoolManager,
    w: Float,
    h: Float,
    playerWeaponPower: Int,
    boss: BossController,
  ) {
    val bossOnScreen = cue.bossCueFired || boss.isActive() || boss.isExploding()
    val emergencyLive = enemies.hasActiveRedShipAnchor()
    if (playerWeaponPower >= 2 || bossOnScreen) {
      powerUpWaveQueued = false
      powerUpWaveTimer = 0f
      return
    }
    if (!powerUpWaveQueued && !emergencyLive) {
      powerUpWaveQueued = true
      powerUpWaveTimer = 0f
    }
    if (!powerUpWaveQueued) return
    powerUpWaveTimer += dt
    if (powerUpWaveTimer < POWER_WAVE_DELAY) return
    FormationSpawner.spawnResupplyColumn(enemies, w, h)
    powerUpWaveQueued = false
    powerUpWaveTimer = 0f
  }
}
