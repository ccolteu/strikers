package com.example.strikers

class SpawnEvent(
  val timestampSeconds: Float,
  val spawnXFraction: Float,
  val spawnYFraction: Float,
  val velocityX: Float,
  val velocityY: Float,
  val enemyType: Int,
  val pattern: Int = 0,
)
