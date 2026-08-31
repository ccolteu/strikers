package com.cc.ww2blitz

class EnemyBullet {
  var x = 0f
  var y = 0f
  var vx = 0f
  var vy = 0f
  var isActive = false
  var flags = 0

  companion object {
    const val FLAG_GRAZED = 1
    const val FLAG_PINK = 2
    const val FLAG_LASER = 4
    const val FLAG_CYAN = 8
  }
}
