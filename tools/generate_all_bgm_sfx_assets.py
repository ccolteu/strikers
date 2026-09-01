#!/usr/bin/env python3
"""
Strikers 1945–inspired 16-bit arcade SFX + BGM bank.

Synthesizes 12 mono 44.1 kHz 16-bit PCM WAVs with numpy oscillators/noise
and scipy.io.wavfile. No sampled audio.

  pip install numpy scipy
  python3 tools/generate_all_bgm_sfx_assets.py
"""

from __future__ import annotations

import argparse
import math
import os

import numpy as np
from scipy.io import wavfile

SR = 44100


def n_samples(seconds: float) -> int:
    return max(1, int(round(seconds * SR)))


def t_axis(n: int) -> np.ndarray:
    return np.arange(n, dtype=np.float64) / SR


def adsr(n: int, attack: float, decay: float, sustain: float, release: float, sustain_lvl: float = 0.7) -> np.ndarray:
    """Linear ADSR in seconds. Zero-length stages are skipped. Ends at 0 to avoid clicks."""
    a = n_samples(attack) if attack > 0 else 0
    d = n_samples(decay) if decay > 0 else 0
    r = n_samples(release) if release > 0 else 0
    s = max(0, n - a - d - r)
    env = np.zeros(n, dtype=np.float64)
    i = 0
    if a:
        env[i : i + a] = np.linspace(0.0, 1.0, a, endpoint=False)
        i += a
    peak = 1.0
    if d:
        env[i : i + d] = np.linspace(peak, sustain_lvl, d, endpoint=False)
        i += d
        peak = sustain_lvl
    elif a == 0:
        peak = sustain_lvl if s else 1.0
    if s:
        env[i : i + s] = peak
        i += s
    if r and i < n:
        env[i:] = np.linspace(peak if i else 1.0, 0.0, n - i)
    elif i < n:
        env[i:] = 0.0
    if n >= 2:
        env[0] = 0.0
        env[-1] = 0.0
    return env


def env_exp(n: int, attack: float, decay: float) -> np.ndarray:
    t = t_axis(n)
    y = np.exp(-t / max(decay, 1e-5))
    if attack > 0:
        a = np.clip(t / attack, 0.0, 1.0)
        y *= a
    y[0] = 0.0
    y[-1] = 0.0
    return y


def phase_from_freq(freq: np.ndarray | float, n: int) -> np.ndarray:
    f = np.broadcast_to(np.asarray(freq, dtype=np.float64), (n,))
    return np.cumsum(f / SR, dtype=np.float64)


def osc_sine(freq: np.ndarray | float, n: int) -> np.ndarray:
    return np.sin(2.0 * math.pi * phase_from_freq(freq, n))


def osc_square(freq: np.ndarray | float, n: int, pw: float = 0.5) -> np.ndarray:
    p = np.mod(phase_from_freq(freq, n), 1.0)
    return np.where(p < pw, 1.0, -1.0).astype(np.float64)


def osc_saw(freq: np.ndarray | float, n: int) -> np.ndarray:
    p = np.mod(phase_from_freq(freq, n), 1.0)
    return p * 2.0 - 1.0


def white(n: int, rng: np.random.Generator) -> np.ndarray:
    return rng.uniform(-1.0, 1.0, size=n)


def one_pole_lp(x: np.ndarray, cutoff_hz: np.ndarray | float) -> np.ndarray:
    n = x.size
    c = np.broadcast_to(np.asarray(cutoff_hz, dtype=np.float64), (n,))
    y = np.empty(n, dtype=np.float64)
    z = 0.0
    two_pi = 2.0 * math.pi
    for i in range(n):
        a = math.exp(-two_pi * float(c[i]) / SR)
        a = 0.0 if a < 0.0 else (0.9995 if a > 0.9995 else a)
        z = (1.0 - a) * x[i] + a * z
        y[i] = z
    return y


def bitcrush(x: np.ndarray, bits: int, hold: int) -> np.ndarray:
    levels = float((1 << (bits - 1)) - 1)
    idx = (np.arange(x.size) // hold) * hold
    sampled = x[idx]
    return np.round(np.clip(sampled, -1.0, 1.0) * levels) / levels


def saturate(x: np.ndarray, drive: float) -> np.ndarray:
    return np.clip(np.tanh(x * drive) * 1.12, -1.0, 1.0)


def normalize(x: np.ndarray, peak: float = 0.89) -> np.ndarray:
    m = float(np.max(np.abs(x)))
    if m < 1e-12:
        return x
    return x * (peak / m)


def loop_crossfade(x: np.ndarray, fade: int = 256) -> np.ndarray:
    """Make a tileable loop: overlap the tail with the head."""
    if x.size <= fade * 2:
        return x
    out = x.copy()
    w = np.linspace(0.0, 1.0, fade, dtype=np.float64)
    out[-fade:] = out[-fade:] * (1.0 - w) + x[:fade] * w
    return out


def midi_hz(midi: float) -> float:
    return 440.0 * (2.0 ** ((midi - 69.0) / 12.0))


def write16(path: str, x: np.ndarray) -> None:
    pcm = np.clip(x, -1.0, 1.0)
    wavfile.write(path, SR, (pcm * 32767.0).astype(np.int16))


# --- SFX -----------------------------------------------------------------

def sfx_vulcan(rng: np.random.Generator) -> np.ndarray:
    n = n_samples(0.068)
    t = t_axis(n)
    click_n = n_samples(0.0035)
    click = np.zeros(n)
    click[:click_n] = white(click_n, rng) * np.linspace(1.0, 0.0, click_n)
    hz = 2100.0 * np.exp(-t * 28.0) + 340.0
    body = osc_square(hz, n, 0.22) * 0.55 + osc_saw(hz * 2.01, n) * 0.18
    hiss = one_pole_lp(white(n, rng), 7000.0 * np.exp(-t * 50.0) + 1200.0)
    x = click * 0.9 + body * env_exp(n, 0.0008, 0.024) + hiss * env_exp(n, 0.0, 0.014) * 0.28
    return normalize(bitcrush(saturate(x, 2.6), 6, 3), 0.92)


def sfx_laser(rng: np.random.Generator) -> np.ndarray:
    n = n_samples(0.20)
    t = t_axis(n)
    hz = 190.0 + 3100.0 * np.exp(-t * 24.0)
    beam = osc_square(hz, n, 0.4) * 0.7 + osc_square(hz * 1.007, n, 0.36) * 0.2 + osc_sine(hz * 0.5, n) * 0.12
    air = one_pole_lp(white(n, rng), hz * 1.6) * 0.1
    x = (beam + air) * env_exp(n, 0.002, 0.10)
    return normalize(bitcrush(saturate(x, 2.0), 7, 2), 0.88)


def sfx_small_explosion(rng: np.random.Generator) -> np.ndarray:
    n = n_samples(0.28)
    t = t_axis(n)
    cutoff = 3800.0 * np.exp(-t * 14.0) + 220.0
    crunch = one_pole_lp(white(n, rng), cutoff)
    thump = osc_sine(180.0 * np.exp(-t * 20.0) + 60.0, n) * env_exp(n, 0.001, 0.08)
    x = crunch * env_exp(n, 0.001, 0.10) + thump * 0.45
    return normalize(bitcrush(saturate(x, 2.7), 5, 4), 0.90)


def sfx_heavy_explosion(rng: np.random.Generator) -> np.ndarray:
    n = n_samples(0.82)
    t = t_axis(n)
    rumble = osc_sine(46.0 + 20.0 * np.exp(-t * 5.5), n) + osc_sine(72.0, n) * 0.5 + osc_square(36.0, n) * 0.18
    grit = one_pole_lp(white(n, rng), 850.0 * np.exp(-t * 4.0) + 80.0)
    body = rumble * env_exp(n, 0.004, 0.34) * 0.9 + grit * env_exp(n, 0.001, 0.30) * 0.8
    x = np.clip(body * 3.1, -0.7, 0.7)
    return normalize(bitcrush(saturate(x, 3.5), 6, 5), 0.95)


def sfx_alarm(rng: np.random.Generator) -> np.ndarray:
    del rng
    half = 4410  # 100 ms — integer cycles at 930 / 700 Hz
    hi = osc_square(93 * SR / half, half, 0.5)
    lo = osc_square(70 * SR / half, half, 0.5)
    x = np.tile(np.concatenate([hi, lo]), 4)
    x *= adsr(x.size, 0.002, 0.0, 0.0, 0.002, 1.0)
    return normalize(bitcrush(saturate(x * 0.75, 1.35), 8, 2), 0.78)


def sfx_pickup(rng: np.random.Generator) -> np.ndarray:
    del rng
    notes = [60, 64, 67, 72, 76]
    note_n = n_samples(0.040)
    parts = []
    for m in notes:
        hz = midi_hz(m)
        tone = osc_square(hz, note_n, 0.5) * 0.7 + osc_square(hz * 2.0, note_n, 0.25) * 0.16
        parts.append(tone * adsr(note_n, 0.002, 0.012, 0.012, 0.012, 0.45))
    x = np.concatenate(parts)
    return normalize(bitcrush(saturate(x, 1.55), 7, 2), 0.82)


def sfx_bomb(rng: np.random.Generator) -> np.ndarray:
    n = n_samples(1.5)
    t = t_axis(n)
    blast = one_pole_lp(white(n, rng), 5000.0 * np.exp(-t * 2.3) + 110.0)
    sub = osc_sine(40.0, n) * env_exp(n, 0.002, 0.55)
    x = blast * env_exp(n, 0.001, 0.62) + sub * 0.55
    return normalize(bitcrush(saturate(x, 2.5), 6, 3), 0.93)


# --- BGM (4 s loops except victory fanfare) ------------------------------

def _note_seq(n: int, bpm: float, midis: list[float], wave: str, duty: float = 0.5) -> np.ndarray:
    """Evenly spaced notes filling n samples. Last sample of each note goes to 0."""
    steps = len(midis)
    step_n = n // steps
    out = np.zeros(n, dtype=np.float64)
    for i, m in enumerate(midis):
        start = i * step_n
        end = n if i == steps - 1 else start + step_n
        ln = end - start
        hz = midi_hz(m)
        if wave == "square":
            osc = osc_square(hz, ln, duty)
        elif wave == "saw":
            osc = osc_saw(hz, ln)
        else:
            osc = osc_sine(hz, ln)
        env = adsr(ln, 0.004, 0.03, max(0.0, ln / SR - 0.05), 0.018, 0.65)
        out[start:end] = osc * env
    return out


def fm_bass(n: int, bpm: float, midis: list[float], ratio: float = 2.0, index: float = 2.2) -> np.ndarray:
    steps = len(midis)
    step_n = n // steps
    out = np.zeros(n, dtype=np.float64)
    for i, m in enumerate(midis):
        start = i * step_n
        end = n if i == steps - 1 else start + step_n
        ln = end - start
        car = midi_hz(m)
        mod = osc_sine(car * ratio, ln)
        sig = np.sin(2.0 * math.pi * phase_from_freq(car, ln) + index * mod)
        out[start:end] = sig * adsr(ln, 0.006, 0.04, max(0.0, ln / SR - 0.06), 0.02, 0.7)
    return out


def hats(n: int, bpm: float, rng: np.random.Generator, dens: int = 8) -> np.ndarray:
    beat = 60.0 / bpm
    step = beat / dens
    y = np.zeros(n, dtype=np.float64)
    t = 0.0
    while True:
        i0 = int(t * SR)
        if i0 >= n:
            break
        ln = n_samples(0.035)
        sl = slice(i0, min(n, i0 + ln))
        k = sl.stop - sl.start
        noise = one_pole_lp(white(k, rng), 9000.0) * env_exp(k, 0.0005, 0.018)
        y[sl] += noise * 0.22
        t += step
    return y


def kick(n: int, bpm: float) -> np.ndarray:
    beat = 60.0 / bpm
    y = np.zeros(n, dtype=np.float64)
    t = 0.0
    while True:
        i0 = int(t * SR)
        if i0 >= n:
            break
        ln = n_samples(0.12)
        sl = slice(i0, min(n, i0 + ln))
        k = sl.stop - sl.start
        tt = t_axis(k)
        hz = 90.0 * np.exp(-tt * 28.0) + 38.0
        y[sl] += osc_sine(hz, k) * env_exp(k, 0.001, 0.07) * 0.7
        t += beat
    return y


def bgm_title(rng: np.random.Generator) -> np.ndarray:
    n = n_samples(4.0)
    bass = fm_bass(n, 120.0, [36, 36, 43, 36, 38, 38, 43, 31, 36, 36, 43, 36, 41, 38, 36, 31])
    lead = _note_seq(n, 120.0, [72, 76, 79, 76, 72, 67, 69, 71, 72, 76, 79, 84, 79, 76, 72, 67], "square", 0.45)
    x = bass * 0.7 + lead * 0.32 + hats(n, 120.0, rng, 8) * 0.45 + kick(n, 120.0) * 0.35
    return loop_crossfade(normalize(bitcrush(saturate(x, 1.35), 8, 2), 0.72))


def bgm_stage1(rng: np.random.Generator) -> np.ndarray:
    n = n_samples(4.0)
    bass = _note_seq(n, 140.0, [31, 31, 38, 31, 34, 31, 38, 43, 31, 31, 38, 31, 29, 31, 34, 38], "saw")
    fifth = _note_seq(n, 140.0, [43, 43, 50, 43, 46, 43, 50, 55, 43, 43, 50, 43, 41, 43, 46, 50], "square", 0.4)
    x = bass * 0.55 + fifth * 0.28 + hats(n, 140.0, rng, 8) + kick(n, 140.0) * 0.4
    return loop_crossfade(normalize(bitcrush(saturate(x, 1.5), 7, 3), 0.74))


def bgm_stage2(rng: np.random.Generator) -> np.ndarray:
    n = n_samples(4.0)
    bass = fm_bass(n, 128.0, [33, 33, 40, 33, 36, 31, 33, 28, 33, 33, 40, 36, 31, 28, 31, 33], ratio=1.5, index=3.0)
    lead = _note_seq(n, 128.0, [64, 63, 64, 67, 60, 59, 60, 55, 64, 67, 70, 67, 64, 60, 58, 55], "square", 0.3)
    pulse = osc_square(8.0, n, 0.12) * 0.04
    x = bass * 0.72 + lead * 0.26 + pulse + hats(n, 128.0, rng, 6) * 0.7 + kick(n, 128.0) * 0.28
    return loop_crossfade(normalize(bitcrush(saturate(x, 1.4), 7, 3), 0.70))


def bgm_boss(rng: np.random.Generator) -> np.ndarray:
    n = n_samples(4.0)
    bass = _note_seq(n, 168.0, [28, 29, 28, 31, 28, 34, 28, 27, 28, 29, 31, 34, 31, 29, 27, 25], "saw")
    stab = _note_seq(n, 168.0, [52, 53, 52, 55, 52, 58, 52, 51, 52, 53, 55, 58, 55, 53, 51, 49], "square", 0.25)
    x = bass * 0.6 + stab * 0.34 + hats(n, 168.0, rng, 8) * 1.1 + kick(n, 168.0) * 0.45
    return loop_crossfade(normalize(bitcrush(saturate(x, 1.85), 6, 3), 0.76))


def bgm_victory(rng: np.random.Generator) -> np.ndarray:
    del rng
    n = n_samples(3.2)
    fanfare = _note_seq(
        n,
        110.0,
        [67, 71, 74, 79, 74, 71, 67, 62, 67, 71, 74, 79, 83, 79, 74, 71],
        "square",
        0.5,
    )
    bass = fm_bass(n, 110.0, [43, 43, 47, 43, 38, 38, 43, 36, 43, 47, 50, 55, 50, 47, 43, 38], index=1.6)
    x = fanfare * 0.5 + bass * 0.45
    x *= adsr(n, 0.01, 0.05, 2.7, 0.4, 0.75)
    return normalize(bitcrush(saturate(x, 1.3), 8, 2), 0.80)


ASSETS = (
    ("sfx_vulcan.wav", sfx_vulcan),
    ("sfx_laser.wav", sfx_laser),
    ("sfx_small_explosion.wav", sfx_small_explosion),
    ("sfx_heavy_explosion.wav", sfx_heavy_explosion),
    ("sfx_alarm.wav", sfx_alarm),
    ("sfx_pickup.wav", sfx_pickup),
    ("sfx_bomb.wav", sfx_bomb),
    ("bgm_title.wav", bgm_title),
    ("bgm_stage1.wav", bgm_stage1),
    ("bgm_stage2.wav", bgm_stage2),
    ("bgm_boss.wav", bgm_boss),
    ("bgm_victory.wav", bgm_victory),
)


def main() -> None:
    here = os.path.dirname(os.path.abspath(__file__))
    default_out = os.path.normpath(os.path.join(here, "..", "app", "src", "main", "res", "raw"))
    parser = argparse.ArgumentParser(description="Generate 12 Strikers arcade audio assets.")
    parser.add_argument("--out", default=default_out, help="Output folder")
    parser.add_argument("--seed", type=int, default=1945)
    args = parser.parse_args()
    rng = np.random.default_rng(args.seed)
    os.makedirs(args.out, exist_ok=True)
    for name, fn in ASSETS:
        audio = fn(rng)
        path = os.path.join(args.out, name)
        write16(path, audio)
        print(f"{name:28s} {audio.size / SR:5.3f}s  peak={float(np.max(np.abs(audio))):.3f}  {path}")


if __name__ == "__main__":
    main()
