package com.example.strikers

class PowerUpItem {
    var x = 0f
    var y = 0f
    var vx = 0f
    var vy = 0f
    var isActive = false

    fun spawn(startX: Float, startY: Float) {
        x = startX
        y = startY
        // Slowly drift downward and bounce gently off the horizontal walls
        vx = if (Math.random() > 0.5) 120f else -120f
        vy = 90f
        isActive = true
    }

    fun update(dt: Float, screenW: Int) {
        if (!isActive) return
        x += vx * dt
        y += vy * dt

        // Bounce gently off the left and right screen borders
        if (x <= 30f) { x = 30f; vx = -vx }
        if (x >= screenW - 30f) { x = screenW - 30f; vx = -vx }

        // Deactivate if it falls completely past the bottom boundary edge
        if (y > 2500f) isActive = false
    }
}
