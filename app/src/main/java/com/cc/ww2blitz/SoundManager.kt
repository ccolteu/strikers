package com.cc.ww2blitz

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Build
import android.os.Handler
import android.os.Looper

/**
 * Low-latency SFX via [SoundPool]. Gapless BGM via dual [MediaPlayer]
 * chaining ([MediaPlayer.setNextMediaPlayer]). [playSFX] allocates nothing;
 * [switchBGM] / loop-complete rebuffer only run off the frame path.
 */
class SoundManager private constructor() : AudioManager.OnAudioFocusChangeListener {

  private val lock = Any()
  private val loadedIds = IntArray(SFX_COUNT)
  private val sfxReady = BooleanArray(SFX_COUNT)
  private var appContext: Context? = null
  private var audioManager: AudioManager? = null
  private var soundPool: SoundPool? = null
  private var activeBgmPlayer: MediaPlayer? = null
  private var nextBgmPlayer: MediaPlayer? = null
  private var bgmAttrs: AudioAttributes? = null
  private var focusRequest: AudioFocusRequest? = null
  private var alarmStreamId = 0
  private var currentBgmRes = 0
  private var bgmWasPlaying = false
  private var paused = false
  private var muted = false
  private var ducking = false
  private var bgmVolumeScale = 0.50f
  private var sfxVolumeScale = 0.65f
  private var initialized = false
  private var bgmCompleting = false
  private var pendingChainRes = 0
  private val bgmHandler = Handler(Looper.getMainLooper())
  private val chainNextLoopRunnable = Runnable { runChainNextLoop() }

  private val onBgmComplete = MediaPlayer.OnCompletionListener { mp ->
    synchronized(lock) {
      onBgmCompletedLocked(mp)
    }
  }

  fun initialize(context: Context) {
    synchronized(lock) {
      if (initialized) return
      val app = context.applicationContext
      appContext = app
      audioManager = app.getSystemService(Context.AUDIO_SERVICE) as AudioManager
      val attrs = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_GAME)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()
      soundPool = SoundPool.Builder()
        .setMaxStreams(MAX_STREAMS)
        .setAudioAttributes(attrs)
        .build()
      val pool = soundPool ?: return
      pool.setOnLoadCompleteListener { _, soundId, status ->
        if (status != 0) return@setOnLoadCompleteListener
        synchronized(lock) {
          var i = 0
          while (i < SFX_COUNT) {
            if (loadedIds[i] == soundId) {
              sfxReady[i] = true
              break
            }
            i++
          }
        }
      }
      var i = 0
      while (i < SFX_COUNT) {
        sfxReady[i] = false
        loadedIds[i] = pool.load(app, SFX_RAW[i], 1)
        i++
      }
      val musicAttrs = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_GAME)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .build()
      bgmAttrs = musicAttrs
      activeBgmPlayer = createBgmPlayerLocked(musicAttrs)
      nextBgmPlayer = createBgmPlayerLocked(musicAttrs)
      requestFocusLocked()
      loadAudioSettings()
      applyBgmVolumeLocked()
      initialized = true
    }
  }

  fun playSFX(id: Int) {
    synchronized(lock) {
      if (!initialized || paused || muted) return
      if (id < 0 || id >= SFX_COUNT) return
      val pool = soundPool ?: return
      val sid = loadedIds[id]
      if (sid == 0 || !sfxReady[id]) return

      val vol = if (ducking) DUCK_VOLUME * sfxVolumeScale else sfxVolumeScale

      // Determine the structural stream priority value based on arcade gameplay weights
      val priority = when (id) {
        SFX_BOMB, SFX_ALARM, SFX_BOSS_WARNING -> 3
        SFX_VULCAN, SFX_LASER -> 2       // HIGH: Instant player weapon audio feedback
        SFX_PICKUP -> 1                  // NORMAL: Scorecard items and mechanical adjustments
        SFX_SMALL_EXPLOSION, SFX_HEAVY_EXPLOSION -> 0 // LOW: Ambient environment destruction noise
        else -> 0
      }

      if (id == SFX_ALARM) {
        if (alarmStreamId != 0) return
        // Pass the fixed priority (3) for the looping alarm sequence
        alarmStreamId = pool.play(sid, vol, vol, priority, -1, 1f)
      } else {
        // Pass the dynamically assigned arcade priority level
        pool.play(sid, vol, vol, priority, 0, 1f)
      }
    }
  }

  fun stopAlarm() {
    synchronized(lock) {
      val pool = soundPool ?: return
      if (alarmStreamId != 0) {
        pool.stop(alarmStreamId)
        alarmStreamId = 0
      }
    }
  }

  fun stopBGM() {
    synchronized(lock) {
      if (!initialized) return
      cancelChainNextLoopLocked()
      bgmWasPlaying = false
      currentBgmRes = 0
      val active = activeBgmPlayer
      val next = nextBgmPlayer
      if (active != null) {
        resetPlayerLocked(active)
      }
      if (next != null) {
        resetPlayerLocked(next)
      }
    }
  }

  fun switchBGM(resId: Int): Boolean {
    synchronized(lock) {
      if (!initialized || resId == 0) return false
      val active = activeBgmPlayer ?: return false
      val next = nextBgmPlayer ?: return false
      if (resId == currentBgmRes && active.isPlaying) return true
      bgmWasPlaying = false
      cancelChainNextLoopLocked()
      try {
        resetPlayerLocked(active)
        resetPlayerLocked(next)
        if (!loadBgmSourceLocked(active, resId)) {
          currentBgmRes = 0
          return false
        }
        active.prepare()
        currentBgmRes = resId
        applyBgmVolumeLocked()
        val chained = prepareNextPlayerLocked(resId)
        if (!chained) {
          try {
            active.setNextMediaPlayer(null)
            active.isLooping = true
          } catch (_: Exception) {
          }
        }
        if (!paused && !muted) {
          active.start()
          bgmWasPlaying = true
        }
        return true
      } catch (_: Exception) {
        currentBgmRes = 0
        return false
      }
    }
  }

  fun isBgmPlaying(resId: Int): Boolean {
    synchronized(lock) {
      if (resId == 0 || resId != currentBgmRes) return false
      val active = activeBgmPlayer ?: return false
      return try {
        active.isPlaying
      } catch (_: Exception) {
        false
      }
    }
  }

  fun setBgmVolumeScale(volume: Float) {
    synchronized(lock) {
      bgmVolumeScale = volume.coerceIn(0f, 1f)
      applyBgmVolumeLocked()
      saveAudioSettings()
    }
  }

  fun setSfxVolumeScale(volume: Float) {
    synchronized(lock) {
      sfxVolumeScale = volume.coerceIn(0f, 1f)
      saveAudioSettings()
    }
  }

  fun getBgmVolumeScale(): Float {
    synchronized(lock) {
      return bgmVolumeScale
    }
  }

  fun getSfxVolumeScale(): Float {
    synchronized(lock) {
      return sfxVolumeScale
    }
  }

  fun saveAudioSettings() {
    synchronized(lock) {
      val ctx = appContext ?: return
      ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putFloat(KEY_BGM_VOLUME, bgmVolumeScale)
        .putFloat(KEY_SFX_VOLUME, sfxVolumeScale)
        .apply()
    }
  }

  private fun loadAudioSettings() {
    val ctx = appContext ?: return
    val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    bgmVolumeScale = prefs.getFloat(KEY_BGM_VOLUME, 0.50f)
    sfxVolumeScale = prefs.getFloat(KEY_SFX_VOLUME, 0.65f)
  }

  fun setMuted(mute: Boolean) {
    synchronized(lock) {
      muted = mute
      applyBgmVolumeLocked()
      if (mute) {
        stopAlarmLocked()
        pauseActiveBgmLocked(remember = true)
      } else if (!paused) {
        resumeBgmLocked()
      }
    }
  }

  fun pauseAll() {
    synchronized(lock) {
      paused = true
      stopAlarmLocked()
      soundPool?.autoPause()
      pauseActiveBgmLocked(remember = true)
    }
  }

  fun resumeAll() {
    synchronized(lock) {
      paused = false
      if (muted) return
      requestFocusLocked()
      soundPool?.autoResume()
      resumeBgmLocked()
    }
  }

  fun release() {
    synchronized(lock) {
      stopAlarmLocked()
      abandonFocusLocked()
      soundPool?.release()
      soundPool = null
      var i = 0
      while (i < SFX_COUNT) {
        loadedIds[i] = 0
        sfxReady[i] = false
        i++
      }
      releasePlayerLocked(activeBgmPlayer)
      releasePlayerLocked(nextBgmPlayer)
      activeBgmPlayer = null
      nextBgmPlayer = null
      bgmAttrs = null
      currentBgmRes = 0
      bgmWasPlaying = false
      cancelChainNextLoopLocked()
      paused = false
      ducking = false
      initialized = false
      appContext = null
      audioManager = null
      focusRequest = null
    }
  }

  override fun onAudioFocusChange(focusChange: Int) {
    synchronized(lock) {
      when (focusChange) {
        AudioManager.AUDIOFOCUS_GAIN -> {
          ducking = false
          applyBgmVolumeLocked()
          if (!paused && !muted) resumeBgmLocked()
        }
        AudioManager.AUDIOFOCUS_LOSS,
        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
        -> {
          ducking = false
          pauseActiveBgmLocked(remember = true)
          stopAlarmLocked()
        }
        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
          ducking = true
          applyBgmVolumeLocked()
        }
      }
    }
  }

  private fun createBgmPlayerLocked(musicAttrs: AudioAttributes): MediaPlayer {
    val player = MediaPlayer()
    player.setAudioAttributes(musicAttrs)
    player.isLooping = false
    applyVolumeToPlayerLocked(player)
    player.setOnCompletionListener(onBgmComplete)
    player.setOnErrorListener { _, _, _ ->
      synchronized(lock) { currentBgmRes = 0 }
      true
    }
    return player
  }

  private fun onBgmCompletedLocked(mp: MediaPlayer) {
    if (!initialized) return
    if (bgmCompleting) return
    if (mp !== activeBgmPlayer) return
    val resId = currentBgmRes
    if (resId == 0) return
    bgmCompleting = true
    pendingChainRes = resId
    val finished = activeBgmPlayer
    val incoming = nextBgmPlayer
    activeBgmPlayer = incoming
    nextBgmPlayer = finished
    applyBgmVolumeLocked()
    if (!paused && !muted) {
      bgmWasPlaying = true
    }
    bgmHandler.removeCallbacks(chainNextLoopRunnable)
    bgmHandler.postDelayed(chainNextLoopRunnable, CHAIN_PREPARE_DELAY_MS)
  }

  private fun runChainNextLoop() {
    synchronized(lock) {
      try {
        if (!initialized || !bgmCompleting) return
        val resId = pendingChainRes
        if (resId == 0 || resId != currentBgmRes) return
        prepareNextPlayerLocked(resId)
      } catch (_: Exception) {
      } finally {
        bgmCompleting = false
        pendingChainRes = 0
      }
    }
  }

  private fun cancelChainNextLoopLocked() {
    bgmHandler.removeCallbacks(chainNextLoopRunnable)
    bgmCompleting = false
    pendingChainRes = 0
  }

  /**
   * Reset [nextBgmPlayer], load [resId], prepare, and chain it as the gapless
   * successor of [activeBgmPlayer]. Call only while holding [lock].
   */
  private fun prepareNextPlayerLocked(resId: Int): Boolean {
    val active = activeBgmPlayer ?: return false
    val next = nextBgmPlayer ?: return false
    if (resId == 0) return false
    try {
      resetPlayerLocked(next)
      if (!loadBgmSourceLocked(next, resId)) return false
      next.prepare()
      applyVolumeToPlayerLocked(next)
      try {
        active.setNextMediaPlayer(next)
      } catch (_: IllegalArgumentException) {
        active.setNextMediaPlayer(null)
        active.isLooping = true
      } catch (_: IllegalStateException) {
        active.setNextMediaPlayer(null)
        active.isLooping = true
      }
      return true
    } catch (_: Exception) {
      return false
    }
  }

  private fun loadBgmSourceLocked(player: MediaPlayer, resId: Int): Boolean {
    val ctx = appContext ?: return false
    val fd = ctx.resources.openRawResourceFd(resId) ?: return false
    try {
      player.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
    } finally {
      fd.close()
    }
    return true
  }

  private fun resetPlayerLocked(player: MediaPlayer) {
    try {
      player.setNextMediaPlayer(null)
    } catch (_: Exception) {
    }
    try {
      if (player.isPlaying) player.stop()
    } catch (_: Exception) {
    }
    player.reset()
    val attrs = bgmAttrs
    if (attrs != null) player.setAudioAttributes(attrs)
    player.isLooping = false
    player.setOnCompletionListener(onBgmComplete)
    applyVolumeToPlayerLocked(player)
  }

  private fun pauseActiveBgmLocked(remember: Boolean) {
    val active = activeBgmPlayer ?: return
    try {
      if (active.isPlaying) {
        if (remember) bgmWasPlaying = true
        active.pause()
      }
    } catch (_: Exception) {
    }
  }

  private fun resumeBgmLocked() {
    val active = activeBgmPlayer ?: return
    if (muted || paused || currentBgmRes == 0) return
    if (bgmWasPlaying || !active.isPlaying) {
      try {
        applyBgmVolumeLocked()
        active.start()
        bgmWasPlaying = true
      } catch (_: Exception) {
      }
    }
  }

  private fun applyBgmVolumeLocked() {
    applyVolumeToPlayerLocked(activeBgmPlayer)
    applyVolumeToPlayerLocked(nextBgmPlayer)
  }

  private fun applyVolumeToPlayerLocked(player: MediaPlayer?) {
    if (player == null) return
    val baseVol = bgmVolumeScale
    val v = if (muted) 0f else if (ducking) DUCK_VOLUME * baseVol else baseVol
    try {
      player.setVolume(v, v)
    } catch (_: Exception) {
    }
  }

  private fun releasePlayerLocked(player: MediaPlayer?) {
    if (player == null) return
    try {
      player.setOnCompletionListener(null)
      player.setOnErrorListener(null)
      player.setNextMediaPlayer(null)
    } catch (_: Exception) {
    }
    try {
      player.reset()
    } catch (_: Exception) {
    }
    try {
      player.release()
    } catch (_: Exception) {
    }
  }

  private fun stopAlarmLocked() {
    val pool = soundPool
    if (pool != null && alarmStreamId != 0) {
      pool.stop(alarmStreamId)
    }
    alarmStreamId = 0
  }

  private fun requestFocusLocked() {
    val am = audioManager ?: return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val req = focusRequest ?: AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
        .setAudioAttributes(
          AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build(),
        )
        .setOnAudioFocusChangeListener(this)
        .build()
        .also { focusRequest = it }
      am.requestAudioFocus(req)
    } else {
      @Suppress("DEPRECATION")
      am.requestAudioFocus(
        this,
        AudioManager.STREAM_MUSIC,
        AudioManager.AUDIOFOCUS_GAIN,
      )
    }
  }

  private fun abandonFocusLocked() {
    val am = audioManager ?: return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val req = focusRequest
      if (req != null) am.abandonAudioFocusRequest(req)
    } else {
      @Suppress("DEPRECATION")
      am.abandonAudioFocus(this)
    }
  }

  companion object {
    const val SFX_VULCAN = 0
    const val SFX_LASER = 1
    const val SFX_SMALL_EXPLOSION = 2
    const val SFX_HEAVY_EXPLOSION = 3
    const val SFX_ALARM = 4
    const val SFX_PICKUP = 5
    const val SFX_BOMB = 6
    const val SFX_BOSS_WARNING = 7

    @JvmField val BGM_STAGE1 = R.raw.bgm_stage1
    @JvmField val BGM_STAGE2 = R.raw.bgm_stage2
    @JvmField val BGM_STAGE3 = R.raw.bgm_stage3
    @JvmField val BGM_STAGE4 = R.raw.bgm_stage4
    @JvmField val BGM_STAGE5 = R.raw.bgm_stage5
    @JvmField val BGM_STAGE6 = R.raw.bgm_stage6
    @JvmField val BGM_STAGE7 = R.raw.bgm_stage7
    @JvmField val BGM_STAGE8 = R.raw.bgm_stage8
    @JvmField val BGM_TITLE = R.raw.bgm_title
    @JvmField val BGM_BOSS = R.raw.bgm_boss
    @JvmField val BGM_BOSS2 = R.raw.bgm_boss2
    @JvmField val BGM_VICTORY = R.raw.bgm_victory

    private const val SFX_COUNT = 8
    private const val MAX_STREAMS = 16
    private const val DUCK_VOLUME = 0.35f
    private const val CHAIN_PREPARE_DELAY_MS = 16L
    private const val PREFS_NAME = "StrikersAudioPrefs"
    private const val KEY_BGM_VOLUME = "KEY_BGM_VOLUME"
    private const val KEY_SFX_VOLUME = "KEY_SFX_VOLUME"

    private val SFX_RAW = intArrayOf(
      R.raw.sfx_vulcan,
      R.raw.sfx_laser,
      R.raw.sfx_small_explosion,
      R.raw.sfx_heavy_explosion,
      R.raw.sfx_alarm,
      R.raw.sfx_pickup,
      R.raw.sfx_bomb,
      R.raw.sfx_boss_warning,
    )

    val instance: SoundManager by lazy { SoundManager() }
  }
}
