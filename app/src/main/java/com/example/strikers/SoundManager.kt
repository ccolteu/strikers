package com.example.strikers

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Build

/**
 * Low-latency SFX via [SoundPool] plus one reused [MediaPlayer] for looping BGM.
 * [playSFX] / [switchBGM] do not allocate on the hot path.
 */
class SoundManager private constructor() : AudioManager.OnAudioFocusChangeListener {

  private val lock = Any()
  private val loadedIds = IntArray(SFX_COUNT)
  private val sfxReady = BooleanArray(SFX_COUNT)
  private var appContext: Context? = null
  private var audioManager: AudioManager? = null
  private var soundPool: SoundPool? = null
  private var bgmPlayer: MediaPlayer? = null
  private var focusRequest: AudioFocusRequest? = null
  private var alarmStreamId = 0
  private var currentBgmRes = 0
  private var bgmWasPlaying = false
  private var paused = false
  private var muted = false
  private var ducking = false
  private var initialized = false

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
      bgmPlayer = MediaPlayer().apply {
        setAudioAttributes(musicAttrs)
        isLooping = true
        setVolume(BGM_VOLUME, BGM_VOLUME)
      }
      requestFocusLocked()
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
      val vol = if (ducking) DUCK_VOLUME else 1f
      if (id == SFX_ALARM) {
        if (alarmStreamId != 0) return
        alarmStreamId = pool.play(sid, vol, vol, 1, -1, 1f)
      } else {
        pool.play(sid, vol, vol, 1, 0, 1f)
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

  fun switchBGM(resId: Int): Boolean {
    synchronized(lock) {
      if (!initialized || resId == 0) return false
      if (resId == currentBgmRes && bgmPlayer?.isPlaying == true) return true
      val ctx = appContext ?: return false
      val player = bgmPlayer ?: return false
      bgmWasPlaying = false
      try {
        player.reset()
        player.isLooping = true
        val fd = ctx.resources.openRawResourceFd(resId) ?: return false
        try {
          player.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
        } finally {
          fd.close()
        }
        player.prepare()
        currentBgmRes = resId
        applyBgmVolumeLocked()
        if (!paused && !muted) {
          player.start()
          bgmWasPlaying = true
        }
        return true
      } catch (_: Exception) {
        currentBgmRes = 0
        return false
      }
    }
  }

  fun setMuted(mute: Boolean) {
    synchronized(lock) {
      muted = mute
      if (mute) {
        stopAlarmLocked()
        val player = bgmPlayer
        if (player != null && player.isPlaying) {
          player.pause()
          bgmWasPlaying = true
        }
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
      val player = bgmPlayer
      if (player != null && player.isPlaying) {
        bgmWasPlaying = true
        player.pause()
      }
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
      val player = bgmPlayer
      if (player != null) {
        try {
          player.reset()
        } catch (_: Exception) {
        }
        player.release()
      }
      bgmPlayer = null
      currentBgmRes = 0
      bgmWasPlaying = false
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
          val player = bgmPlayer
          if (player != null && player.isPlaying) {
            bgmWasPlaying = true
            player.pause()
          }
          stopAlarmLocked()
        }
        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
          ducking = true
          applyBgmVolumeLocked()
        }
      }
    }
  }

  private fun stopAlarmLocked() {
    val pool = soundPool
    if (pool != null && alarmStreamId != 0) {
      pool.stop(alarmStreamId)
    }
    alarmStreamId = 0
  }

  private fun resumeBgmLocked() {
    val player = bgmPlayer ?: return
    if (muted || paused || currentBgmRes == 0) return
    if (bgmWasPlaying || !player.isPlaying) {
      try {
        applyBgmVolumeLocked()
        player.start()
        bgmWasPlaying = true
      } catch (_: Exception) {
      }
    }
  }

  private fun applyBgmVolumeLocked() {
    val v = if (muted) 0f else if (ducking) DUCK_VOLUME * BGM_VOLUME else BGM_VOLUME
    try {
      bgmPlayer?.setVolume(v, v)
    } catch (_: Exception) {
    }
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

    @JvmField val BGM_STAGE1 = R.raw.bgm_stage1
    @JvmField val BGM_STAGE2 = R.raw.bgm_stage2
    @JvmField val BGM_TITLE = R.raw.bgm_title
    @JvmField val BGM_BOSS = R.raw.bgm_boss
    @JvmField val BGM_VICTORY = R.raw.bgm_victory

    private const val SFX_COUNT = 7
    private const val MAX_STREAMS = 16
    private const val BGM_VOLUME = 0.55f
    private const val DUCK_VOLUME = 0.35f

    private val SFX_RAW = intArrayOf(
      R.raw.sfx_vulcan,
      R.raw.sfx_laser,
      R.raw.sfx_small_explosion,
      R.raw.sfx_heavy_explosion,
      R.raw.sfx_alarm,
      R.raw.sfx_pickup,
      R.raw.sfx_bomb,
    )

    val instance: SoundManager by lazy { SoundManager() }
  }
}
