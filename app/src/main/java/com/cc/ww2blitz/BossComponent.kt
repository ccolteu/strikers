package com.cc.ww2blitz

class BossComponent {
  var relOffsetX = 0f
  var relOffsetY = 0f
  var x = 0f
  var y = 0f
  var halfW = 0f
  var halfH = 0f
  var health = 0
  var maxHealth = 0
  var isDestroyed = false
  var componentType = 0
  var shudderTimer = 0f

  fun triggerMicroShudder() {
    shudderTimer = SHUDDER_DURATION
  }

  companion object {
    const val SHUDDER_DURATION = 0.08f
    const val SHUDDER_AMPLITUDE = 2.0f
  }
}
