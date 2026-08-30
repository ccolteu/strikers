package com.example.strikers

class SpawnEvent(
  val timestampSeconds: Float,
  val spawnXFraction: Float,
  val spawnYFraction: Float,
  val velocityX: Float,
  val velocityY: Float,
  val enemyType: Int,
  val pattern: Int = 0,
  val spawnCue: Int = 0,
) {
  companion object {
    const val CUE_NONE = 0
    const val CUE_DEATH_CLEAR = 1
    const val CUE_DIAMOND_LEADER = 2
    const val CUE_DIAMOND_WING_L = 3
    const val CUE_DIAMOND_WING_R = 4
  }
}
