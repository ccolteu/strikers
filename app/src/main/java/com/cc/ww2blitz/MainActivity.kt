package com.cc.ww2blitz

import android.app.Activity
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class MainActivity : Activity() {

  private var gameView: GameView? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    super.onCreate(savedInstanceState)
    WindowCompat.setDecorFitsSystemWindows(window, false)
    volumeControlStream = AudioManager.STREAM_MUSIC
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    hideSystemBars()
    val root = FrameLayout(this)
    val still = ImageView(this)
    still.scaleType = ImageView.ScaleType.CENTER_CROP
    still.setImageResource(R.drawable.title_screen_backdrop)
    still.importantForAccessibility = ImageView.IMPORTANT_FOR_ACCESSIBILITY_NO
    val fill = FrameLayout.LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.MATCH_PARENT,
    )
    root.addView(still, fill)
    setContentView(root)
    root.post {
      SoundManager.instance.initialize(this)
      val view = GameView(this)
      gameView = view
      view.onFirstFramePosted = {
        still.visibility = View.GONE
      }
      root.addView(view, 0, fill)
      still.bringToFront()
    }
  }

  override fun onPause() {
    SoundManager.instance.saveAudioSettings()
    SoundManager.instance.pauseAll()
    super.onPause()
  }

  override fun onResume() {
    super.onResume()
    SoundManager.instance.resumeAll()
  }

  override fun onDestroy() {
    // Graceful hardware state destruction
    SoundManager.instance.release()
    super.onDestroy()
  }

  override fun onWindowFocusChanged(hasFocus: Boolean) {
    super.onWindowFocusChanged(hasFocus)
    if (hasFocus) hideSystemBars()
  }

  private fun hideSystemBars() {
    val controller = WindowInsetsControllerCompat(window, window.decorView)
    controller.hide(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
    controller.systemBarsBehavior =
      WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
  }
}
