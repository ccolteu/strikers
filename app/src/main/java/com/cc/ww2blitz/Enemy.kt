package com.cc.ww2blitz

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class Enemy {
  var x = 0f
  var y = 0f
  var vx = 0f
  var vy = 0f
  var type = 0
  var pattern = 0
  var flightProfile = 0
  var flightTime = 0f
  var patternDelay = 0f
  var aiPhase = 0
  var holdTimer = 0f
  var weaveT = 0f
  var homeX = 0f
  var health = 1
  var isActive = false
  var isRedShipAnchor = false
  var isDestroyer = false
  var isLandVehicle = false
  var isWagon = false
  fun isGroundHeavy(): Boolean = isDestroyer || isLandVehicle || isWagon
  var fireTimer = 0f
  var burstLeft = 0
  var burstWait = 0f
  var aimVx = 0f
  var aimVy = 0f
  var deathClearBullets = false
  var diamondLeader = false
  var diamondWingSign = 0f
  var splinterVeer = false
  var shudderTimer = 0f

  fun triggerMicroShudder() {
    shudderTimer = SHUDDER_DURATION
  }

  /**
   * Writes [aimVx]/[aimVy] toward the player. Optional first-order lead and
   * signed angular slop; all locals are primitives (no heap).
   */
  fun writeAimedShot(
    targetX: Float,
    targetY: Float,
    playerVelX: Float,
    playerVelY: Float,
    shotSpeed: Float,
    applyLead: Boolean,
    slopRad: Float,
  ): Boolean {
    var tx = targetX
    var ty = targetY
    if (applyLead) {
      val odx = targetX - x
      val ody = targetY - y
      val dist = sqrt(odx * odx + ody * ody)
      val eta = if (shotSpeed > 1f) dist / shotSpeed else 0f
      tx += playerVelX * eta
      ty += playerVelY * eta
    }
    val dx = tx - x
    val dy = ty - y
    val lenSq = dx * dx + dy * dy
    if (lenSq <= 0.0001f) return false
    val ang = atan2(dy, dx) + slopRad
    aimVx = cos(ang) * shotSpeed
    aimVy = sin(ang) * shotSpeed
    return true
  }

  /** Hard+ kamikaze: keep speed, rotate heading toward the player each step. */
  fun steerToward(targetX: Float, targetY: Float, dt: Float, turnRate: Float) {
    val spdSq = vx * vx + vy * vy
    val spd = if (spdSq > 0.0001f) sqrt(spdSq) else 220f
    val desired = atan2(targetY - y, targetX - x)
    val current = atan2(vy, vx)
    var delta = desired - current
    if (delta > PI) delta -= TWO_PI
    if (delta < -PI) delta += TWO_PI
    val maxTurn = turnRate * dt
    val turn = if (delta > maxTurn) maxTurn else if (delta < -maxTurn) -maxTurn else delta
    val ang = current + turn
    vx = cos(ang) * spd
    vy = sin(ang) * spd
  }

  companion object {
    const val FLIGHT_PROFILE_SWEEP_ARC = 101
    const val SHUDDER_DURATION = 0.08f
    const val SHUDDER_AMPLITUDE = 2.0f
    const val AIM_SLOP_RAD = 0.15f
    const val KAMI_TURN_RATE = 4.8f
    const val PI = 3.1415927f
    const val TWO_PI = 6.2831855f
  }
}
