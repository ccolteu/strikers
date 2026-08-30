package com.example.strikers

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
  private val halfW = FloatArray(TYPE_COUNT)
  private val halfH = FloatArray(TYPE_COUNT)
  private var screenW = 0f
  private var screenH = 0f
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
  }

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
        e.isActive = true
        return
      }
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
        when (e.pattern) {
          PATTERN_V_HOLD -> updateInterceptorHold(e, dt, h)
          PATTERN_WEAVE -> {
            e.weaveT += dt
            e.x = e.homeX + kotlin.math.sin(e.weaveT * WEAVE_RATE) * (w * WEAVE_AMP_FRAC)
            e.y += e.vy * dt
          }
          else -> {
            e.x += e.vx * dt
            e.y += e.vy * dt
          }
        }
        val eh = halfHOf(e.type)
        val ew = halfWOf(e.type)
        if (e.y - eh > h || e.x - ew > w || (e.x + ew < 0f && e.vx <= 0f) || (e.y + eh < 0f && e.vy <= 0f)) {
          e.isActive = false
          continue
        }
        if (e.type == TYPE_KAMIKAZE) continue
        updateEnemyFire(e, dt, playerX, playerY, weapons, speed)
      }
    }
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
        val sheet = sheetFor(e.type) ?: continue
        canvas.save()
        canvas.translate(e.x, e.y)
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
  }

  private fun typeIndex(type: Int): Int =
    if (type in 0 until TYPE_COUNT) type else TYPE_DRONE

  private fun sheetFor(type: Int): Bitmap? = sheets[typeIndex(type)] ?: sheets[TYPE_DRONE]

  private fun ensureSheetsLoaded() {
    if (sheets[TYPE_DRONE] != null) return
    sheets[TYPE_DRONE] = loadKeyed(R.drawable.enemy_drone)
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
    const val SHADOW_PX = 2
    const val OUTLINE_PX = 3
  }
}
