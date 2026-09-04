package com.cc.ww2blitz

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Rect
import android.view.MotionEvent

/**
 * Relative-drag player. Seven same-size bank sprites:
 * 1 = hard left, 4 = idle, 7 = hard right.
 */
class PlayerShip(private val resources: Resources) {

  val coreHitboxRadius = 8f
  val grazeRadius = 24f

  private val frames = arrayOfNulls<Bitmap>(FRAME_COUNT)
  private val dst = Rect()
  private val paint = Paint().apply { isFilterBitmap = true }
  private val outlinePaint = Paint().apply {
    isFilterBitmap = true
    colorFilter = PorterDuffColorFilter(Color.BLACK, PorterDuff.Mode.SRC_IN)
  }
  private val shadowPaint = Paint().apply {
    isFilterBitmap = true
    colorFilter = PorterDuffColorFilter(0xCC000000.toInt(), PorterDuff.Mode.SRC_IN)
  }

  private var screenW = 0
  private var screenH = 0
  private var halfW = 0f
  private var halfH = 0f

  private var x = 0f
  private var y = 0f
  private var lives = START_LIVES
  private var hitsLeft = HITS_PER_LIFE
  private var weaponPowerLevel = 1
  private var isInvulnerable = false
  private var invulnTimer = 0f
  private var respawnTimer = 0f
  private var respawnPowerDropLatched = false
  private var isGameOverFlag = false

  private var pointerId = MotionEvent.INVALID_POINTER_ID
  private var lastTouchX = 0f
  private var lastTouchY = 0f
  private var targetVelocityX = 0f
  private var isDragging = false
  private var isMovingHorizontal = false
  private var autoFire = false
  private var classBaseSpeed = P38_SPEED
  private var responsivenessTether = P38_TETHER
  private var muzzleFrac = P38_MUZZLE_X
  private var fanAngle = 0f
  private var fireInterval = P38_FIRE_INTERVAL
  var chosenFighterIndex = 0 // 0 = P-38 Lightning, 1 = F6F Hellcat

  private var currentFrameIndex = IDLE_FRAME
  private var targetFrameIndex = IDLE_FRAME

  fun onSizeChanged(width: Int, height: Int) {
    if (width <= 0 || height <= 0) return
    val firstLayout = screenW <= 0 || screenH <= 0
    val prevW = screenW
    val prevH = screenH
    screenW = width
    screenH = height
    if (frames[0] == null) loadFrames()
    refreshDrawSize()
    if (firstLayout) {
      x = width * 0.5f
      y = height * 0.78f
      currentFrameIndex = IDLE_FRAME
      targetFrameIndex = IDLE_FRAME
      targetVelocityX = 0f
      isDragging = false
      isMovingHorizontal = false
    } else if (prevW != width || prevH != height) {
      x *= width.toFloat() / prevW
      y *= height.toFloat() / prevH
    }
    clamp()
    writeDst()
  }

  fun onTouch(event: MotionEvent): Boolean {
    if (isGameOverFlag || lives <= 0 || respawnTimer > 0f) {
      releaseSteer()
      return true
    }
    when (event.actionMasked) {
      MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
        if (pointerId == MotionEvent.INVALID_POINTER_ID) {
          val index = event.actionIndex
          val tx = event.getX(index)
          val ty = event.getY(index)
          val gx = tx - x
          val gy = ty - y
          val grab = touchGrabRadius()
          if ((gx * gx + gy * gy) <= grab * grab) {
            pointerId = event.getPointerId(index)
            lastTouchX = tx
            lastTouchY = ty
            isDragging = true
          }
        }
      }
      MotionEvent.ACTION_MOVE -> {
        val index = event.findPointerIndex(pointerId)
        if (index >= 0 && isDragging) {
          val tx = event.getX(index)
          val ty = event.getY(index)
          lastTouchX = tx
          lastTouchY = ty
        }
      }
      MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_OUTSIDE -> {
        releaseSteer()
      }
      MotionEvent.ACTION_POINTER_UP -> {
        if (event.getPointerId(event.actionIndex) == pointerId) {
          releaseSteer()
        }
      }
    }
    return true
  }

  private fun releaseSteer() {
    pointerId = MotionEvent.INVALID_POINTER_ID
    isDragging = false
    isMovingHorizontal = false
    targetVelocityX = 0f
    targetFrameIndex = IDLE_FRAME
  }

  fun update(dt: Float) {
    if (!isMovingHorizontal) {
      targetFrameIndex = IDLE_FRAME
    } else {
      targetFrameIndex = if (targetVelocityX < 0f) 0f else 6f
    }
    val lerp = (FRAME_LERP * (dt * 60f)).coerceIn(0f, 1f)
    currentFrameIndex += (targetFrameIndex - currentFrameIndex) * lerp
    isMovingHorizontal = false
    if (isInvulnerable) {
      invulnTimer -= dt
      if (invulnTimer <= 0f) {
        invulnTimer = 0f
        isInvulnerable = false
      }
    }
    if (respawnTimer > 0f) {
      respawnTimer -= dt
      if (respawnTimer <= 0f) {
        respawnTimer = 0f
        if (screenW > 0 && screenH > 0) {
          x = screenW * 0.5f
          y = screenH * 0.78f
        }
        isInvulnerable = true
        invulnTimer = INVULN_SEC
        respawnPowerDropLatched = true
      }
    }
    clamp()
    writeDst()
  }

  fun getHitboxX(): Float = x

  fun getHitboxY(): Float = y

  fun centerX(): Float = x

  fun centerY(): Float = y

  fun isOnField(): Boolean = !isGameOverFlag && lives > 0 && respawnTimer <= 0f

  fun getHealth(): Int = lives

  fun getHitsLeft(): Int = hitsLeft

  fun getMaxHitsPerLife(): Int = HITS_PER_LIFE

  fun getWeaponPower(): Int = weaponPowerLevel

  fun upgradeWeapon() {
    if (weaponPowerLevel < 3) weaponPowerLevel++
  }

  fun resetWeaponPower() {
    weaponPowerLevel = 1
  }

  /** True once after a life-explode respawn lands. GameView consumes this to drop a P. */
  fun consumeRespawnPowerDrop(): Boolean {
    if (!respawnPowerDropLatched) return false
    respawnPowerDropLatched = false
    return true
  }

  fun restoreHits() {
    hitsLeft = HITS_PER_LIFE
  }

  fun restoreLives() {
    lives = START_LIVES
    hitsLeft = HITS_PER_LIFE
    respawnTimer = 0f
    isGameOverFlag = false
    respawnPowerDropLatched = false
  }

  /** Extra cabinet body on the same map. Power stays 1; GameView drops the catchable P. */
  fun acceptContinueBody() {
    restoreLives()
    resetForStage()
    isInvulnerable = true
    invulnTimer = INVULN_SEC
    respawnPowerDropLatched = true
  }

  /** Extra life from a score extend. No-op if the credit is dead or the HUD cap is full. */
  fun grantExtraLife(): Boolean {
    if (isGameOverFlag) return false
    if (lives >= MAX_LIVES) return false
    lives++
    return true
  }

  fun isGameOver(): Boolean = isGameOverFlag

  fun resetForStage() {
    isInvulnerable = false
    invulnTimer = 0f
    respawnTimer = 0f
    respawnPowerDropLatched = false
    isGameOverFlag = false
    isDragging = false
    isMovingHorizontal = false
    autoFire = false
    pointerId = MotionEvent.INVALID_POINTER_ID
    currentFrameIndex = IDLE_FRAME
    targetFrameIndex = IDLE_FRAME
    targetVelocityX = 0f
    if (screenW > 0 && screenH > 0) {
      x = screenW * 0.5f
      y = screenH * 0.78f
      clamp()
      writeDst()
    }
  }

  /** @return true if this hit destroyed the ship (explosion). */
  fun takeDamage(): Boolean {
    if (isInvulnerable || isGameOverFlag || respawnTimer > 0f) return false
    ScoreManager.instance.markMiss()
    hitsLeft -= 1
    if (hitsLeft > 0) {
      isInvulnerable = true
      invulnTimer = INVULN_SEC
      return false
    }
    lives -= 1
    hitsLeft = HITS_PER_LIFE
    weaponPowerLevel = 1
    StageData.liveInstance?.dumpCombatRankOnDeath()
    releaseSteer()
    if (lives <= 0) {
      lives = 0
      isGameOverFlag = true
      return true
    }
    respawnTimer = RESPAWN_SEC
    return true
  }

  fun isFiringHeld(): Boolean =
    (isDragging || autoFire) && !isGameOverFlag && lives > 0 && respawnTimer <= 0f

  fun setAutoFire(on: Boolean) {
    autoFire = on
  }

  fun followTether(
    fingerX: Float,
    fingerY: Float,
    grabOffsetX: Float,
    grabOffsetY: Float,
    dt: Float,
  ) {
    if (isGameOverFlag || lives <= 0 || respawnTimer > 0f) return
    isDragging = true
    val targetX = fingerX - grabOffsetX
    val targetY = fingerY - grabOffsetY
    var dx = targetX - x
    var dy = targetY - y
    var dist = kotlin.math.sqrt(dx * dx + dy * dy)
    if (dist <= 0.001f) return
    if (dist > TETHER_LIMIT_PX) {
      val tetherScale = TETHER_LIMIT_PX / dist
      dx *= tetherScale
      dy *= tetherScale
      dist = TETHER_LIMIT_PX
    }
    val maxMove = classBaseSpeed * dt
    if (dist > maxMove) {
      val targetScale = maxMove / dist
      x += dx * targetScale * responsivenessTether
      y += dy * targetScale * responsivenessTether
    } else {
      x += dx
      y += dy
    }
    targetVelocityX = dx
    if (kotlin.math.abs(dx) > MOVE_THRESHOLD) {
      isMovingHorizontal = true
    }
    clamp()
    writeDst()
  }

  fun touchGrabRadius(): Float {
    val span = if (halfW > halfH) halfW else halfH
    return span * TOUCH_GRAB_SCALE
  }

  fun steerToward(targetX: Float, targetY: Float, dt: Float) {
    if (isGameOverFlag || lives <= 0 || respawnTimer > 0f) return
    val dx = targetX - x
    val dy = targetY - y
    val maxStep = DEMO_SPEED * dt
    val lenSq = dx * dx + dy * dy
    if (lenSq > maxStep * maxStep && lenSq > 0.0001f) {
      val inv = maxStep / kotlin.math.sqrt(lenSq)
      x += dx * inv
      y += dy * inv
    } else {
      x = targetX
      y = targetY
    }
    targetVelocityX = dx
    isMovingHorizontal = kotlin.math.abs(dx) > MOVE_THRESHOLD
    clamp()
    writeDst()
  }

  fun leftMuzzleX(): Float = x - halfW * muzzleFrac

  fun rightMuzzleX(): Float = x + halfW * muzzleFrac

  fun muzzleXAt(spanFrac: Float): Float = x + halfW * spanFrac

  fun muzzleY(): Float = y - halfH * MUZZLE_Y_FRAC

  fun vulcanInterval(): Float {
    if (weaponPowerLevel == 2) {
      return if (chosenFighterIndex == 1) {
        HELLCAT_FIRE_INTERVAL_P2
      } else {
        P38_FIRE_INTERVAL_P2
      }
    }
    return fireInterval
  }

  fun draw(canvas: Canvas) {
    if (isGameOverFlag || respawnTimer > 0f) return
    if (isInvulnerable && ((invulnTimer * 20f).toInt() and 1) == 0) return
    val frame = kotlin.math.round(currentFrameIndex).toInt().coerceIn(0, FRAME_COUNT - 1)
    val bmp = frames[frame] ?: return
    dst.offset(SHADOW_PX, SHADOW_PX)
    canvas.drawBitmap(bmp, null, dst, shadowPaint)
    dst.offset(-SHADOW_PX, -SHADOW_PX)
    var oy = -OUTLINE_PX
    while (oy <= OUTLINE_PX) {
      var ox = -OUTLINE_PX
      while (ox <= OUTLINE_PX) {
        if (ox != 0 || oy != 0) {
          dst.offset(ox, oy)
          canvas.drawBitmap(bmp, null, dst, outlinePaint)
          dst.offset(-ox, -oy)
        }
        ox += OUTLINE_PX
      }
      oy += OUTLINE_PX
    }
    canvas.drawBitmap(bmp, null, dst, paint)
  }

  fun applyFighterConfiguration(typeIndex: Int) {
    when (typeIndex) {
      1 -> {
        chosenFighterIndex = 1
        classBaseSpeed = HELLCAT_SPEED
        responsivenessTether = HELLCAT_TETHER
        muzzleFrac = HELLCAT_MUZZLE_X
        fanAngle = HELLCAT_FAN_ANGLE
        fireInterval = HELLCAT_FIRE_INTERVAL
      }
      else -> {
        chosenFighterIndex = 0
        classBaseSpeed = P38_SPEED
        responsivenessTether = P38_TETHER
        muzzleFrac = P38_MUZZLE_X
        fanAngle = 0f
        fireInterval = P38_FIRE_INTERVAL
      }
    }
    var i = 0
    while (i < frames.size) {
      val bmp = frames[i]
      if (bmp != null && !bmp.isRecycled) bmp.recycle()
      frames[i] = null
      i++
    }
    loadFrames()
    refreshDrawSize()
  }

  fun release() {
    for (i in frames.indices) {
      val bmp = frames[i]
      if (bmp != null && !bmp.isRecycled) bmp.recycle()
      frames[i] = null
    }
  }

  private fun clamp() {
    val padX = screenW * SHIP_WIDTH_FRAC * 0.5f
    val padY = if (halfH > 1f) halfH else padX
    val minX = padX
    val maxX = screenW - padX
    val minY = padY
    val maxY = screenH - padY
    if (maxX < minX || maxY < minY) return
    if (x < minX) x = minX
    if (x > maxX) x = maxX
    if (y < minY) y = minY
    if (y > maxY) y = maxY
  }

  private fun writeDst() {
    dst.set(
      (x - halfW).toInt(),
      (y - halfH).toInt(),
      (x + halfW).toInt(),
      (y + halfH).toInt(),
    )
  }

  private fun refreshDrawSize() {
    if (screenW <= 0 || screenH <= 0) return
    val idle = frames[IDLE_INDEX] ?: return
    val drawW = (screenW * SHIP_WIDTH_FRAC).toInt().coerceAtLeast(1)
    val srcW = idle.width.toFloat().coerceAtLeast(1f)
    val srcH = idle.height.toFloat().coerceAtLeast(1f)
    val drawH = (drawW * (srcH / srcW)).toInt().coerceAtLeast(1)
    halfW = drawW * 0.5f
    halfH = drawH * 0.5f
    currentFrameIndex = IDLE_FRAME
    targetFrameIndex = IDLE_FRAME
    clamp()
    writeDst()
  }

  private fun loadFrames() {
    val ids = if (chosenFighterIndex == 0) {
      intArrayOf(
        R.drawable.player_ship_1, R.drawable.player_ship_2, R.drawable.player_ship_3,
        R.drawable.player_ship_4, R.drawable.player_ship_5, R.drawable.player_ship_6,
        R.drawable.player_ship_7,
      )
    } else {
      intArrayOf(
        R.drawable.player_b_1, R.drawable.player_b_2, R.drawable.player_b_3,
        R.drawable.player_b_4, R.drawable.player_b_5, R.drawable.player_b_6,
        R.drawable.player_b_7,
      )
    }
    var i = 0
    while (i < FRAME_COUNT) {
      frames[i] = loadKeyed(ids[i])
      i++
    }
  }

  private fun loadKeyed(resId: Int): Bitmap {
    val opts = BitmapFactory.Options().apply {
      inScaled = false
      inPreferredConfig = Bitmap.Config.ARGB_8888
      inMutable = true
    }
    val src = BitmapFactory.decodeResource(resources, resId, opts)
      ?: error("Missing drawable $resId")
    val bmp = if (src.isMutable) src else src.copy(Bitmap.Config.ARGB_8888, true).also { src.recycle() }
    StageBitmaps.keyGreen(bmp)
    return bmp
  }

  private companion object {
    const val FRAME_COUNT = 7
    const val IDLE_INDEX = 3
    const val IDLE_FRAME = 3f

    const val SHIP_WIDTH_FRAC = 0.16f
    const val TOUCH_GRAB_SCALE = 3.5f
    const val P38_SPEED = 1600f
    const val HELLCAT_SPEED = 1150f
    const val P38_TETHER = 1.0f
    const val HELLCAT_TETHER = 0.82f
    const val TETHER_LIMIT_PX = 40f
    const val P38_MUZZLE_X = 0.22f
    const val HELLCAT_MUZZLE_X = 0.58f
    const val HELLCAT_FAN_ANGLE = 0.32f
    const val P38_FIRE_INTERVAL = 0.090f
    const val P38_FIRE_INTERVAL_P2 = 0.075f
    const val HELLCAT_FIRE_INTERVAL = 0.125f
    const val HELLCAT_FIRE_INTERVAL_P2 = 0.105f
    const val BULLET_SPEED = 1600f

    const val MOVE_THRESHOLD = 0.5f
    const val FRAME_LERP = 0.2f
    const val MUZZLE_Y_FRAC = 0.38f
    const val INVULN_SEC = 2.0f
    const val START_LIVES = 3
    const val MAX_LIVES = 6
    const val HITS_PER_LIFE = 3
    const val RESPAWN_SEC = 0.4f
    const val DEMO_SPEED = 920f
    const val SHADOW_PX = 2
    const val OUTLINE_PX = 3
  }
}
