package com.example.strikers

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
  var fireTimer = 0f
  var burstLeft = 0
  var burstWait = 0f
  var aimVx = 0f
  var aimVy = 0f
  var deathClearBullets = false
  var diamondLeader = false
  var diamondWingSign = 0f
  var splinterVeer = false

  companion object {
    const val FLIGHT_PROFILE_SWEEP_ARC = 101
  }
}
