package com.cc.ww2blitz

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RectF

class EnemyPoolManager(private val resources: Resources) {

  private val pool = Array(POOL_SIZE) { Enemy() }
  private val lock = Any()
  private val paint = Paint().apply {
    isFilterBitmap = true
    isAntiAlias = false
  }
  private val outlinePaint = Paint().apply {
    isFilterBitmap = true
    isAntiAlias = false
    colorFilter = PorterDuffColorFilter(Color.BLACK, PorterDuff.Mode.SRC_IN)
  }
  private val shadowPaint = Paint().apply {
    isFilterBitmap = true
    isAntiAlias = false
    colorFilter = PorterDuffColorFilter(0xCC000000.toInt(), PorterDuff.Mode.SRC_IN)
  }
  private val drawRect = RectF()
  private val sheets = arrayOfNulls<Bitmap>(TYPE_COUNT)
  private var droneRedSheet: Bitmap? = null
  private val halfW = FloatArray(TYPE_COUNT)
  private val halfH = FloatArray(TYPE_COUNT)
  private var screenW = 0f
  private var screenH = 0f
  private val sweepArcS = FloatArray(SWEEP_LUT)
  private var sweepArcLen = 1f
  private var sweepTailDelay = 0.75f
  private var rng = 2463534242

  fun onSizeChanged(width: Int, height: Int) {
    screenW = width.toFloat()
    screenH = height.toFloat()
    ensureSheetsLoaded()
    var t = 0
    while (t < TYPE_COUNT) {
      val sheet = sheets[t] ?: sheets[0] ?: return
      val frac = WIDTH_FRAC[t]
      val targetDrawW = (width * frac).toInt().coerceAtLeast(1)
      val aspectRatio = sheet.height.toFloat() / sheet.width.toFloat()
      val targetDrawH = (targetDrawW * aspectRatio).toInt().coerceAtLeast(1)
      halfW[t] = targetDrawW * 0.5f
      halfH[t] = targetDrawH * 0.5f
      t++
    }
    rebuildSweepLut()
  }

  fun sweepArcTailDelay(): Float = sweepTailDelay

  fun getEnemyPool(): Array<Enemy> = pool

  fun getPoolSize(): Int = POOL_SIZE

  fun countActive(): Int {
    synchronized(lock) {
      var n = 0
      var i = 0
      while (i < POOL_SIZE) {
        if (pool[i].isActive) n++
        i++
      }
      return n
    }
  }

  fun getHalfW(): Float = halfW[TYPE_DRONE]

  fun getHalfH(): Float = halfH[TYPE_DRONE]

  fun halfWOf(type: Int): Float = halfW[typeIndex(type)]

  fun halfHOf(type: Int): Float = halfH[typeIndex(type)]

  fun deactivateAll() {
    synchronized(lock) {
      var i = 0
      while (i < POOL_SIZE) {
        pool[i].isActive = false
        pool[i].isRedShipAnchor = false
        pool[i].flightProfile = 0
        pool[i].flightTime = 0f
        pool[i].patternDelay = 0f
        pool[i].deathClearBullets = false
        pool[i].diamondLeader = false
        pool[i].diamondWingSign = 0f
        pool[i].splinterVeer = false
        pool[i].shudderTimer = 0f
        i++
      }
    }
  }

  fun spawnEnemy(
    startX: Float,
    startY: Float,
    velocityX: Float,
    velocityY: Float,
    enemyType: Int,
    pattern: Int = 0,
    health: Int = 1,
    isRedShipAnchor: Boolean = false,
    flightProfile: Int = 0,
    patternDelay: Float = 0f,
    spawnCue: Int = 0,
  ) {
    synchronized(lock) {
      for (i in 0 until POOL_SIZE) {
        val e = pool[i]
        if (e.isActive) continue
        e.x = startX
        e.y = startY
        e.vx = velocityX
        e.vy = velocityY
        e.type = enemyType
        e.pattern = pattern
        e.flightProfile = flightProfile
        e.flightTime = 0f
        e.patternDelay = patternDelay
        e.aiPhase = 0
        e.holdTimer = 0f
        e.weaveT = 0f
        e.homeX = startX
        e.health = if (health < 1) 1 else health
        e.fireTimer = FIRE_DELAY_MIN + nextUnit() * (FIRE_DELAY_MAX - FIRE_DELAY_MIN)
        e.burstLeft = 0
        e.burstWait = 0f
        e.aimVx = 0f
        e.aimVy = 0f
        e.isRedShipAnchor = isRedShipAnchor
        e.deathClearBullets = spawnCue == SpawnEvent.CUE_DEATH_CLEAR
        e.diamondLeader = spawnCue == SpawnEvent.CUE_DIAMOND_LEADER
        e.diamondWingSign = 0f
        if (spawnCue == SpawnEvent.CUE_DIAMOND_WING_L) e.diamondWingSign = -1f
        if (spawnCue == SpawnEvent.CUE_DIAMOND_WING_R) e.diamondWingSign = 1f
        e.splinterVeer = false
        e.shudderTimer = 0f
        e.isActive = true
        return
      }
    }
  }

  fun triggerDiamondSplinter() {
    synchronized(lock) {
      var i = 0
      while (i < POOL_SIZE) {
        val e = pool[i]
        if (e.isActive && e.diamondWingSign != 0f) {
          e.splinterVeer = true
        }
        i++
      }
    }
  }

  fun hasActiveRedShipAnchor(): Boolean {
    synchronized(lock) {
      var i = 0
      while (i < POOL_SIZE) {
        val e = pool[i]
        if (e.isActive && e.isRedShipAnchor) return true
        i++
      }
      return false
    }
  }

  fun update(dt: Float, playerX: Float, playerY: Float, weapons: EnemyWeaponSystem) {
    synchronized(lock) {
      val w = screenW
      val h = screenH
      val speed = AIMED_SHOT_SPEED
      for (i in 0 until POOL_SIZE) {
        val e = pool[i]
        if (!e.isActive) continue
        if (e.shudderTimer > 0f) {
          e.shudderTimer -= dt
        }
        if (e.flightProfile == Enemy.FLIGHT_PROFILE_SWEEP_ARC) {
          updateSweepArc(e, dt, w, h)
          continue
        }
        when (e.pattern) {
          PATTERN_V_HOLD -> updateInterceptorHold(e, dt, h)
          PATTERN_WEAVE -> {
            e.weaveT += dt
            e.x = e.homeX + kotlin.math.sin(e.weaveT * WEAVE_RATE) * (w * WEAVE_AMP_FRAC)
            e.y += e.vy * dt
          }
          else -> {
            if (e.splinterVeer) {
              e.vx += e.diamondWingSign * SPLINTER_ACCEL * dt
            }
            e.x += e.vx * dt
            e.y += e.vy * dt
          }
        }
        val eh = halfHOf(e.type)
        val ew = halfWOf(e.type)
        if (e.y - eh > h || e.x - ew > w || (e.x + ew < 0f && e.vx <= 0f) || (e.y + eh < 0f && e.vy <= 0f)) {
          e.isActive = false
          e.isRedShipAnchor = false
          continue
        }
        if (e.type == TYPE_KAMIKAZE) continue
        updateEnemyFire(e, dt, playerX, playerY, weapons, speed)
      }
    }
  }

  private fun recycleEnemy(e: Enemy) {
    e.isActive = false
    e.isRedShipAnchor = false
    e.flightProfile = 0
    e.flightTime = 0f
    e.patternDelay = 0f
    e.deathClearBullets = false
    e.diamondLeader = false
    e.diamondWingSign = 0f
    e.splinterVeer = false
  }

  private fun updateSweepArc(e: Enemy, dt: Float, screenW: Float, screenH: Float) {
    e.patternDelay -= dt
    if (e.patternDelay > 0f) {
      val ew = halfWOf(e.type)
      e.x = -SWEEP_ARC_MARGIN - ew
      e.y = screenH * SWEEP_ARC_START_Y_FRAC
      return
    }
    e.flightTime += dt
    val t = e.flightTime / SWEEP_ARC_DURATION
    if (t >= 1.0f) {
      recycleEnemy(e)
      return
    }
    val u = sweepUForArcLength(t * sweepArcLen)
    val startLeftX = -SWEEP_ARC_MARGIN
    val targetRightX = screenW + SWEEP_ARC_MARGIN
    val startTopY = screenH * SWEEP_ARC_START_Y_FRAC
    e.x = startLeftX + (targetRightX - startLeftX) * u
    e.y = startTopY + (screenH * SWEEP_ARC_DIP_FRAC) * kotlin.math.sin(u * SWEEP_ARC_PI)
    val eh = halfHOf(e.type)
    val ew = halfWOf(e.type)
    if (e.y - eh > screenH || e.x - ew > screenW) {
      recycleEnemy(e)
    }
  }

  private fun rebuildSweepLut() {
    val n = SWEEP_LUT
    val x0 = -SWEEP_ARC_MARGIN
    val x1 = screenW + SWEEP_ARC_MARGIN
    val y0 = screenH * SWEEP_ARC_START_Y_FRAC
    val dip = screenH * SWEEP_ARC_DIP_FRAC
    val span = x1 - x0
    var prevX = x0
    var prevY = y0
    sweepArcS[0] = 0f
    var i = 1
    while (i < n) {
      val u = i / (n - 1).toFloat()
      val x = x0 + span * u
      val y = y0 + dip * kotlin.math.sin(u * SWEEP_ARC_PI)
      val dx = x - prevX
      val dy = y - prevY
      sweepArcS[i] = sweepArcS[i - 1] + kotlin.math.sqrt(dx * dx + dy * dy)
      prevX = x
      prevY = y
      i++
    }
    sweepArcLen = sweepArcS[n - 1]
    if (sweepArcLen < 1f) sweepArcLen = 1f
    val shipW = halfW[TYPE_DRONE] * 2f
    val shipH = halfH[TYPE_DRONE] * 2f
    val shipSpan = kotlin.math.max(shipW, shipH)
    val gap = if (shipSpan > 1f) shipSpan * SWEEP_ARC_SPACING else sweepArcLen * 0.12f
    sweepTailDelay = (gap / sweepArcLen) * SWEEP_ARC_DURATION
  }

  private fun sweepUForArcLength(s: Float): Float {
    val n = SWEEP_LUT
    val last = n - 1
    if (s <= 0f) return 0f
    if (s >= sweepArcS[last]) return 1f
    var lo = 0
    var hi = last
    while (lo < hi) {
      val mid = (lo + hi) ushr 1
      if (sweepArcS[mid] < s) lo = mid + 1 else hi = mid
    }
    val i = lo
    if (i <= 0) return 0f
    val s0 = sweepArcS[i - 1]
    val s1 = sweepArcS[i]
    val ds = s1 - s0
    val f = if (ds > 0.0001f) (s - s0) / ds else 0f
    val u0 = (i - 1) / last.toFloat()
    val u1 = i / last.toFloat()
    return u0 + (u1 - u0) * f
  }

  private fun updateInterceptorHold(e: Enemy, dt: Float, screenH: Float) {
    val heavyHold = e.type == TYPE_HEAVY
    val holdY = screenH * if (heavyHold) HEAVY_HOLD_Y_FRAC else HOLD_Y_FRAC
    when (e.aiPhase) {
      0 -> {
        e.x += e.vx * dt
        e.y += e.vy * dt
        if (e.y >= holdY) {
          e.y = holdY
          e.vx = 0f
          e.vy = 0f
          e.aiPhase = 1
          e.holdTimer = if (heavyHold) HEAVY_HOLD_SEC else HOLD_SEC
          e.fireTimer = 0f
        }
      }
      1 -> {
        e.holdTimer -= dt
        if (e.holdTimer <= 0f) {
          e.aiPhase = 2
          e.vy = if (heavyHold) HEAVY_RETREAT_VY else DIVE_VY
        }
      }
      else -> {
        if (!heavyHold) {
          e.vy += DIVE_ACCEL * dt
        }
        e.y += e.vy * dt
      }
    }
  }

  private fun updateEnemyFire(
    e: Enemy,
    dt: Float,
    playerX: Float,
    playerY: Float,
    weapons: EnemyWeaponSystem,
    speed: Float,
  ) {
    // --- PATTERN 1: LIGHT DRONES AMED 3-SHOT BURST ---
    // If mid-burst, handle timed delay ticks between bullets
    if (e.burstLeft > 0) {
      e.burstWait -= dt
      if (e.burstWait <= 0f) {
        // Recalculate tracking vector slightly to prevent complete linear avoidance
        val dx = playerX - e.x
        val dy = playerY - e.y
        val lenSq = dx * dx + dy * dy
        if (lenSq > 0.0001f) {
          val inv = speed / kotlin.math.sqrt(lenSq)
          e.aimVx = dx * inv
          e.aimVy = dy * inv
        }
        weapons.fireBullet(e.x, e.y, e.aimVx, e.aimVy)
        SoundManager.instance.playSFX(SoundManager.SFX_LASER) // Play retro laser sound per burst element

        e.burstLeft -= 1
        if (e.burstLeft > 0) {
          e.burstWait = BURST_GAP
        } else {
          e.fireTimer = SCOUT_REFIRE
        }
      }
      return
    }

    e.fireTimer -= dt
    if (e.fireTimer > 0f) return

    when (e.type) {
      TYPE_HEAVY -> {
        // --- PATTERN 3: HEAVY VINYARD GUNSHIPS 12-WAY RADIAL RING ---
        fireHeavyRing(e, weapons, RING_SPEED)
        e.fireTimer = HEAVY_FIRE_GAP
      }
      TYPE_INTERCEPTOR -> {
        // --- PATTERN 2: MEDIUM INTERCEPTORS WIDE 3-WAY SPREAD ---
        fireInterceptorSpread(e, weapons, speed)
        e.fireTimer = if (e.pattern == PATTERN_V_HOLD && e.aiPhase == 1) HOLD_FIRE_GAP else INTERCEPT_REFIRE
      }
      else -> {
        // Initiate the 3-shot burst cycle for standard drones
        val dx = playerX - e.x
        val dy = playerY - e.y
        val lenSq = dx * dx + dy * dy
        if (lenSq > 0.0001f) {
          val inv = speed / kotlin.math.sqrt(lenSq)
          e.aimVx = dx * inv
          e.aimVy = dy * inv

          // Fire initial shot immediately
          weapons.fireBullet(e.x, e.y, e.aimVx, e.aimVy)
          SoundManager.instance.playSFX(SoundManager.SFX_LASER)

          e.burstLeft = 2 // 2 more shots left to complete the 3-shot arcade burst
          e.burstWait = BURST_GAP
        } else {
          e.fireTimer = SCOUT_REFIRE
        }
      }
    }
  }

  private fun fireInterceptorSpread(e: Enemy, weapons: EnemyWeaponSystem, speed: Float) {
    // Fire a classic 3-way branching wall (Center Down, Left-Angled Down, Right-Angled Down)
    var k = -1
    while (k <= 1) {
      // Android Canvas Math Fix: To point straight down by default, your starting base angle must be PI / 2.
      val baseAng = Math.PI / 2.0
      val finalAng = baseAng + (k * SPREAD_RAD)

      // Realignment: X uses cosine for horizontal offset, Y uses sine for downward travel vector
      val vx = speed * kotlin.math.cos(finalAng).toFloat()
      val vy = speed * kotlin.math.sin(finalAng).toFloat()

      weapons.fireBullet(e.x, e.y, vx, vy)
      k++
    }
    SoundManager.instance.playSFX(SoundManager.SFX_LASER)
  }

  private fun fireHeavyRing(e: Enemy, weapons: EnemyWeaponSystem, speed: Float) {
    // Overhaul from 8 bullets to a dense 12-bullet circular ring
    val customRingCount = 12
    val customRingStep = (Math.PI * 2.0) / customRingCount
    var k = 0
    while (k < customRingCount) {
      val ang = k * customRingStep
      val vx = speed * kotlin.math.cos(ang).toFloat()
      val vy = speed * kotlin.math.sin(ang).toFloat()

      weapons.fireBullet(e.x, e.y, vx, vy)
      k++
    }
    SoundManager.instance.playSFX(SoundManager.SFX_ALARM) // Play a quick alert warning overlay for heavy shots
  }

  private fun nextUnit(): Float {
    rng = rng * 1664525 + 1013904223
    return ((rng ushr 8) and 0xFFFFFF) / 16777215f
  }

  fun draw(canvas: Canvas) {
    synchronized(lock) {
      for (i in 0 until POOL_SIZE) {
        val e = pool[i]
        if (!e.isActive) continue
        if (e.flightProfile == Enemy.FLIGHT_PROFILE_SWEEP_ARC && e.patternDelay > 0f) continue
        val base = sheetFor(e.type) ?: continue
        val sheet = if (e.isRedShipAnchor) (droneRedSheet ?: base) else base
        var drawX = e.x
        if (e.type == TYPE_HEAVY && e.shudderTimer > 0f) {
          drawX += if ((e.shudderTimer * 100f).toInt() % 2 == 0) {
            Enemy.SHUDDER_AMPLITUDE
          } else {
            -Enemy.SHUDDER_AMPLITUDE
          }
        }
        canvas.save()
        canvas.translate(drawX, e.y)
        canvas.rotate(180f)
        val ew = halfWOf(e.type)
        val eh = halfHOf(e.type)
        drawRect.set(-ew, -eh, ew, eh)
        // Local +x/+y is screen up-left after 180°, so negate for a screen down-right shadow.
        drawRect.offset(-SHADOW_PX.toFloat(), -SHADOW_PX.toFloat())
        canvas.drawBitmap(sheet, null, drawRect, shadowPaint)
        drawRect.offset(SHADOW_PX.toFloat(), SHADOW_PX.toFloat())
        var oy = -OUTLINE_PX
        while (oy <= OUTLINE_PX) {
          var ox = -OUTLINE_PX
          while (ox <= OUTLINE_PX) {
            if (ox != 0 || oy != 0) {
              drawRect.offset(ox.toFloat(), oy.toFloat())
              canvas.drawBitmap(sheet, null, drawRect, outlinePaint)
              drawRect.offset(-ox.toFloat(), -oy.toFloat())
            }
            ox += OUTLINE_PX
          }
          oy += OUTLINE_PX
        }
        canvas.drawBitmap(sheet, null, drawRect, paint)
        canvas.restore()
      }
    }
  }

  fun release() {
    var t = 0
    while (t < TYPE_COUNT) {
      val sheet = sheets[t]
      if (sheet != null && !sheet.isRecycled) sheet.recycle()
      sheets[t] = null
      t++
    }
    val red = droneRedSheet
    if (red != null && !red.isRecycled) red.recycle()
    droneRedSheet = null
  }

  private fun typeIndex(type: Int): Int =
    if (type in 0 until TYPE_COUNT) type else TYPE_DRONE

  private fun sheetFor(type: Int): Bitmap? = sheets[typeIndex(type)] ?: sheets[TYPE_DRONE]

  private fun ensureSheetsLoaded() {
    if (sheets[TYPE_DRONE] != null) return
    sheets[TYPE_DRONE] = loadKeyed(R.drawable.enemy_drone)
    droneRedSheet = loadKeyed(R.drawable.enemy_drone_red)
    sheets[TYPE_KAMIKAZE] = loadKeyed(R.drawable.enemy_kamikaze)
    sheets[TYPE_INTERCEPTOR] = loadKeyed(R.drawable.enemy_interceptor)
    sheets[TYPE_HEAVY] = loadKeyed(R.drawable.enemy_heavy)
  }

  private fun loadKeyed(drawableId: Int): Bitmap {
    val opts = BitmapFactory.Options().apply {
      inScaled = false
      inPreferredConfig = Bitmap.Config.ARGB_8888
      inMutable = true
    }
    val src = BitmapFactory.decodeResource(resources, drawableId, opts)
      ?: error("Missing drawable $drawableId")
    val bmp = if (src.isMutable) src else src.copy(Bitmap.Config.ARGB_8888, true).also { src.recycle() }
    keyGreen(bmp)
    return bmp
  }

  private fun keyGreen(bmp: Bitmap) {
    val w = bmp.width
    val h = bmp.height
    val row = IntArray(w)
    for (rowY in 0 until h) {
      bmp.getPixels(row, 0, w, 0, rowY, w, 1)
      for (i in 0 until w) {
        val c = row[i]
        val r = (c ushr 16) and 0xFF
        val g = (c ushr 8) and 0xFF
        val b = c and 0xFF
        if (g > 160 && g > r + 40 && g > b + 40) {
          row[i] = 0
        }
      }
      bmp.setPixels(row, 0, w, 0, rowY, w, 1)
    }
  }

  private companion object {
    const val POOL_SIZE = 48
    const val TYPE_COUNT = 4
    const val TYPE_DRONE = 0
    const val TYPE_KAMIKAZE = 1
    const val TYPE_INTERCEPTOR = 2
    const val TYPE_HEAVY = 3
    const val PATTERN_V_HOLD = 1
    const val PATTERN_WEAVE = 2
    const val HOLD_Y_FRAC = 0.30f
    const val HOLD_SEC = 1.15f
    const val HEAVY_HOLD_Y_FRAC = 0.25f
    const val HEAVY_HOLD_SEC = 5f
    const val HEAVY_RETREAT_VY = -160f
    const val HOLD_FIRE_GAP = 0.55f
    const val INTERCEPT_REFIRE = 0.85f
    const val HEAVY_FIRE_GAP = 1.5f
    const val BURST_EXTRA = 2
    const val BURST_GAP = 0.10f
    const val SCOUT_REFIRE = 0.85f
    const val RING_COUNT = 8
    const val RING_STEP = Math.PI * 2.0 / RING_COUNT
    const val RING_SPEED = 340f
    const val SPREAD_RAD = (15.0 * Math.PI / 180.0).toFloat()
    const val DIVE_VY = 280f
    const val DIVE_ACCEL = 520f
    const val WEAVE_RATE = 6.2f
    const val WEAVE_AMP_FRAC = 0.055f
    val WIDTH_FRAC = floatArrayOf(0.18f, 0.14f, 0.22f, 0.28f)
    const val FIRE_DELAY_MIN = 0.5f
    const val FIRE_DELAY_MAX = 1.5f
    const val FIRE_ONCE_LOCK = 999f
    const val AIMED_SHOT_SPEED = 550f
    const val SPLINTER_ACCEL = 180f
    const val SHADOW_PX = 2
    const val OUTLINE_PX = 3
    const val SWEEP_ARC_DURATION = 4.8f
    const val SWEEP_ARC_SPACING = 1.15f
    const val SWEEP_ARC_DIP_FRAC = 0.30f
    const val SWEEP_ARC_START_Y_FRAC = 0.08f
    const val SWEEP_ARC_MARGIN = 64f
    const val SWEEP_ARC_PI = 3.1415927f
    const val SWEEP_LUT = 64
  }
}
