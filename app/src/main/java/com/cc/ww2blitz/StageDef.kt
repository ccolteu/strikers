package com.cc.ww2blitz

/** Scroll / facility / ascent draw kit. Not a spawn script. */
object StageTheaterKind {
  const val SCROLL = 0
  const val FACILITY = 1
  const val ASCENT = 2
}

/** Peel layout + fire tables in BossController. Reuse a kind to clone an existing fortress. */
object BossCombatKind {
  const val PLANE = 1
  const val TANK = 2
  const val BATTLESHIP = 3
  const val WINTER = 4
  const val ATOLL = 5
  const val JUNGLE = 6
  const val CANOPY = 7
  const val ORBIT = 8
}

/** Elapsed-time wave list. Independent of theater, boss peel, and playlist id. */
object StageWaveKind {
  const val CLOUD_FORTRESS = 1
  const val IRON_TREADS = 2
  const val STEEL_ATLANTIC = 3
  const val FROZEN_FRONT = 4
  const val CORAL_ATOLL = 5
  const val JUNGLE_RUINS = 6
  const val ASCENT_CANOPY = 7
  const val ORBIT_INTRO = 8
}

class BossKit(
  val body: String,
  val wreckLeft: String,
  val wreckRight: String,
  val wreckCenter: String,
  val triPart: Boolean = false,
)

/**
 * One map identity: metrics, theater flags, asset paths, briefing title.
 * Directors and pools are composed beside this, not subclassed from it.
 */
class StageDef(
  val id: Int,
  val operationName: CharArray,
  val scrollSpeedY: Float,
  val bossAtSeconds: Float,
  val stageMusicTrack: Int,
  val theaterKind: Int,
  val hasOverlayClouds: Boolean = false,
  val keyedOverlayLayers: Boolean = false,
  val locksElapsedAtBoss: Boolean = false,
  val usesOpeningPowerV: Boolean = false,
  val airHeavyHighHold: Boolean = false,
  val introOnly: Boolean = false,
  val introSecs: Float = 0f,
  val spaceSwapAt: Float = 0f,
  val canopyAt: Float = 0f,
  val floorFile: String = "floor.png",
  val midFile: String? = null,
  val highFile: String? = null,
  val canopyFile: String? = null,
  val floorAltFile: String? = null,
  val briefingFile: String = "briefing.png",
  val skinTankFile: String? = null,
  val skinDestroyerFile: String? = null,
  val skinWagonFile: String? = null,
  val skinHelicopterFile: String? = null,
  val waveScript: Int,
  val bossCombat: Int,
  val boss: BossKit,
  val artFolder: String? = null,
) {
  val folder: String = artFolder ?: "stages/$id"
  val usesSharedBossEntranceCue: Boolean = usesOpeningPowerV && !introOnly

  fun asset(file: String): String = "$folder/$file"

  fun floorPath(): String = asset(floorFile)
  fun midPath(): String? = midFile?.let { asset(it) }
  fun highPath(): String? = highFile?.let { asset(it) }
  fun canopyPath(): String? = canopyFile?.let { asset(it) }
  fun floorAltPath(): String? = floorAltFile?.let { asset(it) }
  fun briefingPath(): String = asset(briefingFile)
  fun skinTankPath(): String? = skinTankFile?.let { asset(it) }
  fun skinDestroyerPath(): String? = skinDestroyerFile?.let { asset(it) }
  fun skinWagonPath(): String? = skinWagonFile?.let { asset(it) }
  fun skinHelicopterPath(): String? = skinHelicopterFile?.let { asset(it) }
  fun bossBodyPath(): String = asset(boss.body)
  fun wreckLeftPath(): String = asset(boss.wreckLeft)
  fun wreckRightPath(): String = asset(boss.wreckRight)
  fun wreckCenterPath(): String = asset(boss.wreckCenter)
}

object StageCatalog {

  private val wrecks = BossKit("boss.png", "wreck_left.png", "wreck_right.png", "wreck_center.png")
  private val wrecksTri = BossKit(
    "boss.png",
    "wreck_left.png",
    "wreck_right.png",
    "wreck_center.png",
    triPart = true,
  )

  val all: Array<StageDef> = arrayOf(
    StageDef(
      id = 1,
      operationName = charArrayOf('C', 'L', 'O', 'U', 'D', ' ', 'F', 'O', 'R', 'T', 'R', 'E', 'S', 'S'),
      scrollSpeedY = 180f,
      bossAtSeconds = 38f,
      stageMusicTrack = SoundManager.BGM_STAGE1,
      theaterKind = StageTheaterKind.SCROLL,
      hasOverlayClouds = true,
      usesOpeningPowerV = true,
      midFile = "mid.png",
      highFile = "high.png",
      waveScript = StageWaveKind.CLOUD_FORTRESS,
      bossCombat = BossCombatKind.PLANE,
      boss = wrecks,
    ),
    StageDef(
      id = 2,
      operationName = charArrayOf('I', 'R', 'O', 'N', ' ', 'T', 'R', 'E', 'A', 'D', 'S'),
      scrollSpeedY = 260f,
      bossAtSeconds = 30f,
      stageMusicTrack = SoundManager.BGM_STAGE2,
      theaterKind = StageTheaterKind.SCROLL,
      usesOpeningPowerV = true,
      skinTankFile = "skin_tank.png",
      waveScript = StageWaveKind.IRON_TREADS,
      bossCombat = BossCombatKind.TANK,
      boss = wrecks,
    ),
    StageDef(
      id = 3,
      operationName = charArrayOf('S', 'T', 'E', 'E', 'L', ' ', 'A', 'T', 'L', 'A', 'N', 'T', 'I', 'C'),
      scrollSpeedY = 200f,
      bossAtSeconds = 42f,
      stageMusicTrack = SoundManager.BGM_STAGE3,
      theaterKind = StageTheaterKind.SCROLL,
      locksElapsedAtBoss = true,
      usesOpeningPowerV = true,
      airHeavyHighHold = true,
      skinDestroyerFile = "skin_destroyer.png",
      waveScript = StageWaveKind.STEEL_ATLANTIC,
      bossCombat = BossCombatKind.BATTLESHIP,
      boss = wrecks,
    ),
    StageDef(
      id = 4,
      operationName = charArrayOf('F', 'R', 'O', 'Z', 'E', 'N', ' ', 'F', 'R', 'O', 'N', 'T'),
      scrollSpeedY = 240f,
      bossAtSeconds = 42f,
      stageMusicTrack = SoundManager.BGM_STAGE4,
      theaterKind = StageTheaterKind.SCROLL,
      hasOverlayClouds = true,
      keyedOverlayLayers = true,
      locksElapsedAtBoss = true,
      usesOpeningPowerV = true,
      skinTankFile = "skin_tank.png",
      midFile = "mid.png",
      highFile = "high.png",
      waveScript = StageWaveKind.FROZEN_FRONT,
      bossCombat = BossCombatKind.WINTER,
      boss = wrecks,
    ),
    StageDef(
      id = 5,
      operationName = charArrayOf('C', 'O', 'R', 'A', 'L', ' ', 'A', 'T', 'O', 'L', 'L'),
      scrollSpeedY = 220f,
      bossAtSeconds = 40f,
      stageMusicTrack = SoundManager.BGM_STAGE5,
      theaterKind = StageTheaterKind.SCROLL,
      hasOverlayClouds = true,
      keyedOverlayLayers = true,
      locksElapsedAtBoss = true,
      usesOpeningPowerV = true,
      midFile = "mid.png",
      highFile = "high.png",
      waveScript = StageWaveKind.CORAL_ATOLL,
      bossCombat = BossCombatKind.ATOLL,
      boss = wrecks,
    ),
    StageDef(
      id = 6,
      operationName = charArrayOf('J', 'U', 'N', 'G', 'L', 'E', ' ', 'R', 'U', 'I', 'N', 'S'),
      scrollSpeedY = 310f,
      bossAtSeconds = 45f,
      stageMusicTrack = SoundManager.BGM_STAGE6,
      theaterKind = StageTheaterKind.SCROLL,
      locksElapsedAtBoss = true,
      usesOpeningPowerV = true,
      skinHelicopterFile = "skin_hellicopter.png",
      waveScript = StageWaveKind.JUNGLE_RUINS,
      bossCombat = BossCombatKind.JUNGLE,
      boss = wrecks,
    ),
    StageDef(
      id = 7,
      operationName = charArrayOf('A', 'S', 'C', 'E', 'N', 'T', ' ', 'C', 'A', 'N', 'O', 'P', 'Y'),
      scrollSpeedY = 280f,
      bossAtSeconds = 45f,
      stageMusicTrack = SoundManager.BGM_STAGE7,
      theaterKind = StageTheaterKind.FACILITY,
      locksElapsedAtBoss = true,
      canopyFile = "canopy.png",
      skinWagonFile = "skin_wagon.png",
      waveScript = StageWaveKind.ASCENT_CANOPY,
      bossCombat = BossCombatKind.CANOPY,
      boss = wrecksTri,
    ),
    StageDef(
      id = 8,
      operationName = charArrayOf('O', 'R', 'B', 'I', 'T', ' ', 'T', 'H', 'R', 'E', 'S', 'H', 'O', 'L', 'D'),
      scrollSpeedY = 180f,
      bossAtSeconds = 50f,
      stageMusicTrack = SoundManager.BGM_STAGE8,
      theaterKind = StageTheaterKind.ASCENT,
      introOnly = true,
      introSecs = 5f,
      spaceSwapAt = 30f,
      canopyAt = 35f,
      canopyFile = "canopy.png",
      floorAltFile = "floor_alt.png",
      waveScript = StageWaveKind.ORBIT_INTRO,
      bossCombat = BossCombatKind.ORBIT,
      boss = wrecksTri,
    ),
  )

  fun get(id: Int): StageDef {
    var i = 0
    while (i < all.size) {
      if (all[i].id == id) return all[i]
      i++
    }
    return all[0]
  }

  fun count(): Int = all.size

  fun maxId(): Int {
    var m = 0
    var i = 0
    while (i < all.size) {
      val id = all[i].id
      if (id > m) m = id
      i++
    }
    return m
  }
}
