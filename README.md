# WW2 Blitz

WW2 Blitz is a **portrait shoot-’em-up** for Android. You fly one fighter up a scrolling WWII landscape, shoot waves of planes, peel a multi-part boss, and carry your score through **six** stages.

If you have played *1942*, *Strikers 1945*, or a Psikyo cabinet: that is the genre. The phone is held upright. Enemies come from the top. You stay near the bottom.

The software is a small Kotlin engine (`com.cc.ww2blitz`). It does **not** use Jetpack Compose for gameplay. One `SurfaceView` draws every pixel. A vsync callback (`Choreographer`) is the clock. The combat loop is written so it **does not allocate memory every frame**, which keeps Android’s garbage collector from hitching the action.

---

## What you play

### The fantasy, in one paragraph

You are a lone interceptor. The ground scrolls toward you like a map on a conveyor. Flights of enemy aircraft enter in recognizable **formations**—Vs, side sweeps, weaving pairs, walls of heavies. You hold the glass to fire, drag to dodge green bullets, grab a **P** to make the gun wider, and double-tap a **bomb** when a wall is about to crush you. After a few dozen seconds a **boss** fills the top of the screen. You shoot off its guns first; only then will the core take damage. Survive **six** of those maps and the war is over. Before each flight (including the first tap from the title), a **briefing card** fills the screen for three seconds. The title itself is a full-bleed 1080×2400 attraction still (`title_screen_backdrop`), **center-cropped** to the phone so it never letterboxes. Stage 5 is an industrial facility: a **floor** scrolls under the fight, a **canopy** of girders and fans slides over enemy craft, and you fly *above* that roof. Stage 6 is the orbital climb: cloud floor, then space, then an orbit overlay after 35 seconds, and a dreadnought that arrives after a 5-second intro with **no grunt waves**.

### Why it feels like an arcade, not a “level select” app

A real cabinet in 1996 did three things while nobody was playing: show the logo, play a **demo** with the CPU flying, then show the **high score table**. WW2 Blitz does the same. You do not pick a stage from a menu. You either tap **1P START** and play the campaign, or you watch the machine sell itself.

### Controls (what your thumb does)

**Relative drag** means the ship moves *by how much your finger moved*, not “the ship jumps under your finger.” If you put your thumb on the right side of the screen and slide left a centimeter, the plane moves left a centimeter. That keeps the plane readable and keeps your thumb off the sprite.

**Hold to fire** means you do not tap for every bullet. While the finger is down, a vulcan fires on a timer. Stronger **weapon power** adds extra streams, then **homing missiles**.

**Graze** is a Psikyo-cabinet trick: the plane’s *picture* is large so you can see it, but you only die if a green shot hits a **tiny core**. Sliding a bullet through the halo around that core is a graze: you get a spark and **100** points, and the shot keeps going.

**Double-tap** is a separate gesture (short taps close together, little movement). That spends one bomb. A bomb is a short, huge blast used as a panic button, not as your main gun.

**Audio settings** on the title are real sliders for BGM and SFX. They do not start a game.

You start with **3 lives**, **3 hits per life** (a small bar), and **3 bombs**. A chip on the hit bar does not cost a life. Emptying the bar **explodes the plane** and spends one life; you respawn with a short invincibility window. **GAME OVER** only happens when that last life is gone—not when the sprite blows up with lives still on the HUD.

Heavies and boss turrets **shudder** a couple of pixels when they take a hit, so you can see the round connect without a full-body white flash.

### Power-ups and score

Drops are items that fall after certain kills:

- **P (power)** — your shot pattern gets denser. When power is already maxed, extra P becomes a medal (score only).
- **B (bomb)** — another bomb, up to 3. Extra B becomes a medal.
- **Shield** — restores the hit bar on the current life (`restoreHits()`). Stage 5’s right flank can drop this instead of a bomb when you are already stocked.
- **Medal** — score. Face-up is worth more (2000) than edge-on (200). Bullets cancelled in the Stage 5 core-kill freeze become **stationary +100** medals.
- **Graze** — 100 points the first time each enemy bullet threads the halo (not the core). Attract demo still sparks; it does **not** add to the campaign total. The **count** of grazes this stage is stored for the recap bonus.

Your running total is owned by `ScoreManager` (`campaignScore` in `GameView` is a getter/setter over it), capped at 99,999,999. When a stage is cleared, leftover planes, bullets, and pickups are **swept off** so the recap is readable. The ticker is a **four-phase state machine** (lives → bombs → graze → total):

- **LIVES BONUS** — 50,000 × lives left, rolled in over ~1 s with a vulcan click every 5 frames.
- **BOMBS BONUS** — 20,000 × bombs left, same roll.
- **GRAZE BONUS** — 500 × grazes this stage, same roll.
- **STAGE CLEAR TOTAL** — sum of those three bonuses, then a 2 Hz **PRESS FIRE TO CONTINUE**. `ACTION_UP` advances; weapon power **carries** into the next map. The campaign ends only when `advanceToNextStage()` walks **past** the last playlist slot (`campaignFinishedLatch`). Duplicate ids in a test playlist do not look “finished” just because you are on the last *index* of a repeated id.

HUD recap lines are drawn from `UIController` with a pre-allocated `CharArray` (no `String.format` on the frame).

### Bosses

A boss is one big illustration, but the engine treats it as **several hitboxes**: left gun, right gun, core, and so on. If you could dump all damage into the core immediately, the fight would be a sponge. Instead you **peel** the modules. Destroyed modules show wreck art. Each hit **shudders** the vehicle 2 px left/right for a few frames. On stages 1–4 the core only becomes fully open when every other part is gone. When the last gun comes off, the screen **flashes** and **shakes**; when the core dies, a heavier flash/shake and a short explode overlay, then stage clear on a dimmed empty map. A bomb also kicks the camera.

**Stage 5 (Vulcan Engine)** is three world-space modules: `TYPE_LEFT_FLANK` (150 HP), `TYPE_RIGHT_FLANK` (150 HP), `TYPE_CORE` (350 HP). While **both** flanks live, core hits bank at **half** damage. Breaking a flank is **+25,000** and a guaranteed drop (left always **P**; right is **P** if weapon power &lt; 3, else bomb or shield). The core kill is **+100,000** and starts a **4.5 s** victory script (freeze + bullet-to-medal, cascade explosions, white shatter flash, wreck-center art), then the recap.

**Stage 6 (orbit dreadnought)** peels left/right rails then the core lens. While both rails live: aimed **cyan** triples (XOR with the rail-charge timer) and a staggered **pink** column from the lens. One rail left: cyan downward stream + pink ring. Both rails gone: pink helix. Combat lives in `EnemyWeaponSystem.updateStage6Boss`.

| Stage | Operation (briefing) | Theater | How fast the ground moves | When the boss is cued |
| --- | --- | --- | --- | --- |
| 1 | CLOUD FORTRESS | Canyon / airfield | slower | ~38 seconds |
| 2 | IRON TREADS | Super-tank country | faster | ~30 seconds |
| 3 | STEEL ATLANTIC | Ocean / carrier | medium | ~25 seconds |
| 4 | JUNGLE RUINS | Jungle air fortress | fastest | ~45 seconds |
| 5 | ASCENT CANOPY | Industrial facility | fast (280 px/s, ramps to 0 at the gate) | ~45 seconds |
| 6 | ORBIT THRESHOLD | Clouds → space → orbit overlay | 180 px/s, speed envelope then brake | **5 seconds** (no grunt script) |

The four **grunt types** you see before the boss: **drone** (fodder), **kamikaze** (dives, little shooting), **interceptor** (holds and aims), **heavy** (tanky, slow fire). How they are *arranged* is a design topic of its own—see [Enemy formation design](#enemy-formation-design) below. Stage 6 skips that vocabulary: the briefing ends, then the dreadnought.

### Title card and briefing cards

The attract title is **not** the scrolling stage parallax. `doFrame` blits `title_screen_backdrop` with a **center-crop fit**: `fillScale = max(screenW / bmpW, screenH / bmpH)`, dest `RectF` reused (`titleDstRect`). Uniform scale fills the glass; overflow is cropped; aspect is not stretched. Logo, **1P START**, **CREDIT 00**, and **[ AUDIO SETTINGS ]** draw afterward in `drawArcadeUI` / `drawTitleScreen` (screen-relative layout, independent of the crop rect).

After **1P START**, and after every recap that is **not** the end of the playlist, the loop enters `STATE_INTERSTITIAL` for **3 seconds**. Touch is swallowed. Combat does not tick. Six native-aspect PNGs (`interstitial_stage1.png` … `stage6.png`) decode once. Draw **width-fits** and **centers vertically**. Gold type: `OPERATION:` then the mission name, offset from the card top (`textSize * 4.2f`) so it clears a camera notch. Fade-in uses remaining `interstitialTimer` (first 0.5 s). When the timer hits 0 → `STATE_PLAYING`.

### Attract mode (the machine playing itself)

Leave the title untouched:

1. **Title (~4 s)** — logo and blinking **1P START**.
2. **CPU demo (~30 s)** — an AI flies a **random stage 1–5**, never the same map twice in a row (`% 5 + 1`). Stage 6 is not in that attract set. That demo score is **not** a high score.
3. **Top 10 (~4 s)** — rank, 8-digit score, three letters, highest stage reached.

Touch during demo or ranking returns you to the interactive title so you can start.

### Game over, campaign complete, names

If you lose all lives: **GAME OVER** for about 9 seconds (tap to skip). Spending a life with 2 remaining is a respawn, not game over. If your score beats 10th place, **REGISTRATION**: tap the left half of the letter row to go backward through A–Z, right half to go forward, bottom prompt to lock a letter. Three letters, then the ranking list. If you do not beat 10th, you skip naming and see the list.

If you **clear the last map in `STAGE_SEQUENCE`** (default `1,2,3,4,5,6`), recap still runs. `ACTION_UP` calls `advanceToNextStage()`. If that increment walks off the end, `campaignFinishedLatch` is set and you enter **`STATE_CAMPAIGN_COMPLETE`**: scrolling credits (`UIController.drawCampaignCompleteCredits`, word wrap at 85% width). After **22 s**, a dimmer gold **TOUCH SCREEN TO REGISTER SCORE** blinks, then the same “do I qualify?” path. Finishing is the **latch**, not “the id is 6.”

The table is stored on the phone (`SharedPreferences` name `arcade_leaderboard`): ten rows of score, initials, and max stage.

---

## Design choices

Each choice is stated first as a player-facing idea, then as an engineering rule.

### 1. Arcade cabinet, not a mobile “session”

**Plain:** A good shmup in an arcade *looks busy while it waits*. People walking by see a demo, then names of people who were good. You should not be dumped into a stage-select grid.

**Engine:** Title, demo, and ranking are a timer cycle (`ATTRACT_TITLE` → `ATTRACT_CPU_DEMO` → `ATTRACT_HIGH_SCORE`). Demo uses the *same* spawn and combat code as a real game, with auto-fire and a cheap steering AI. Inserting into the leaderboard is forbidden on that path.

### 2. Zero allocation on the frame

**Plain:** If the phone pauses for garbage collection in the middle of a bullet-hell, it feels like the game lagged. Players blame “bad performance.” We blame `new` on the hot path.

**Engine:** `doFrame` must not allocate. No `ArrayList`, no `for (x in list)`, no `"score_$i"` string building, no boxing. Combat objects live in **fixed arrays** created at startup. HUD text is one reused `StringBuilder`. Loops are `while (i < n) { ...; i++ }`.

### 3. Time-scripted stages, not a level-editor graph

**Plain:** Classic cabinets did not load a JSON graph of 200 enemy nodes. A designer said “at 8 seconds, a V of interceptors; at 12 seconds, weavers from 30% and 70% width.” Density is a function of **clock time**.

**Engine:** `SpawnTimeline` holds `elapsedTime` and booleans like `vFormSpawned`. When the clock crosses a constant, it spawns once. Stages **3, 4, and 5** **stop incrementing the clock** after the boss is cued (`locksElapsedAtBoss`; Stage 5 also ramps `scrollSpeedY` 280→0 between 40–45 s). Stage **6** uses its **own** clock: increment until `S6_INTRO_SECS` (5 s), cue the boss, latch, then **return**. Routing is by `StageData` flags (`isStage1Script` … `isStage6Backdrop`), not playlist index.

### 4. Multi-hitbox bosses, peel then core

**Plain:** Shooting a giant until a single HP bar dies feels like a damage sponge. Shooting *guns off a plane* feels like a machine coming apart.

**Engine:** `BossComponent` slots with their own HP. `isCoreVulnerable()` is derived: all non-core parts destroyed. Wreck bitmaps overlay dead modules. Per-stage fire is more timers (chin, sponson, flak, gatling, desperation rings)—still no per-bullet objects beyond the shared enemy-bullet pool.

### 5. Relative drag + auto-fire + discrete bomb

**Plain:** Putting the plane under your thumb hides the plane. Tapping for every shot is exhausting. Using the same tap for bombs would spend bombs by accident.

**Engine:** `PlayerShip` tracks `pointerId` and last XY. `BulletManager` fires on a cooldown while `isFiringHeld()`. Bombs require a double-tap window (`DOUBLE_TAP_MS`, slop in pixels) in `GameView.onTouchEvent`.

### 6. Green chroma at load, not authored alpha

**Plain:** Pixel artists often paint on screaming green. The engine should punch that green out once when the bitmap loads, not require a Photoshop export dance for every ship.

**Engine:** Mutable `ARGB_8888` decode, then pixels with `g > 160 && g > r + 40 && g > b + 40` become 0. Same helper on player, enemies, bosses, missiles, explosions.

### 7. Cover-scaled parallax, not stretched 9:16

**Plain:** Background paintings are closer to 2:3. Stretching them to a tall phone makes hangars look like they melted. Cropping the sides (cover) keeps proportions.

**Engine:** `ParallaxBackground` cover-scales three layers. Mid/high clouds sit on black and composite with `PorterDuff.Mode.SCREEN`. Stages 2–4 swap only the **ground** bitmap; cloud layers stay. Overlay clouds (`hasOverlayClouds`) are Stage **1** identity. Stage 5 is a **two-bitmap** theater: facility floor at `scrollSpeedY`, neon-green-keyed canopy at **1.5×**. Stage 6 loads an unscaled cloud floor and a space floor; at **30 s** (`S6_SPACE_SWAP_AT`) the cloud bitmap is recycled and space becomes the floor. From **35 s** (`S6_CANOPY_AT`) an orbit overlay draws like Stage 5’s canopy. Scroll speed is modulated by an ascent envelope in `updateStage6`. The **title** does not use this system; it center-crops `title_screen_backdrop`.

### 8. Hardware canvas, vsync clock

**Plain:** The game should tick with the screen refresh, not with Compose “recompose this tree.” One canvas, one frame.

**Engine:** `Choreographer.FrameCallback`. `dt` is nanoseconds converted to seconds, clamped to 50 ms so a debugger pause does not teleport every bullet. API 26+ uses `lockHardwareCanvas()`.

### 9. Audio off the frame

**Plain:** Gunshots must be immediate. Music must loop without a gap. Neither should create objects while you are dodging.

**Engine:** `SoundPool` for SFX (`playSFX` is lock + play). Dual `MediaPlayer` for BGM with `setNextMediaPlayer`. Track changes happen on the main looper. Volumes live in prefs.

### 10. Persistence is primitives

**Plain:** The high score table is ten rows you can draw with a `while` loop. It should survive app restart. It should not parse JSON during attract.

**Engine:** `IntArray` / `CharArray`, in-place insertion, `SharedPreferences.edit().apply()`. Load once (`hydrated`).

### 11. Sprite is not the hurtbox; graze is a ring

**Plain:** If the whole fighter sprite were solid, weaving between bullets would be impossible. Psikyo cabinets draw a generous plane and hurt you on a **dot**. Passing a shot through the gap between the dot and a larger ring is a skill check with a score drip, not a second life.

**Engine:** `PlayerShip` keeps `coreHitboxRadius = 6f` and `grazeRadius = 24f` on the same center as the sprite (`x`/`y`). `BulletManager.resolveEnemyBulletsVsPlayer` uses Euclidean distance from each `EnemyBullet` center to that point—no `RectF.intersects` on the physical path. Core overlap calls `takeDamage()` and consumes the shot. Else if inside the graze ring, `EnemyBullet.flags` bit `FLAG_GRAZED` latches once per bullet. `ScoreManager.instance.addGrazeScore(100)` and a pooled `ParticleManager.triggerSpark` at the bullet. Demo sets `awardScore = false`. Rams and pickups still use the larger `PLAYER_HIT_RADIUS`.

### 12. Camera shake and screen flash as duration, not objects

**Plain:** When a fortress loses its last turret, the cabinet should *kick*. When the core blows, it should kick harder and bleach the glass for a beat. HUD numbers should stay readable.

**Engine:** `shakeDuration` / `shakeIntensity` / `flashDuration` on `GameView`. `triggerScreenShake` writes localized floats. World draw (background through particles) sits inside `canvas.save()` + `translate(dx, dy)` + `restore()`; `dx`/`dy` are signed unit noise × intensity from an LCG (same mapping as `(random * 2 - 1) * intensity`, no `Math.random()` on the frame). Duration decays by `dt`. Flash is a reused full-screen quad, 40% white, prebuilt PorterDuff `SRC_OVER`, then HUD. `BossController` latches `FX_PHASE` when the core first becomes vulnerable and `FX_DEATH` when explode starts; `GameView` consumes those flags after collisions. Bombs call `addScreenShake`, which forwards into `triggerScreenShake`.

### 13. Hit confirm is a 2 px shudder, not a white flash

**Plain:** A tanky gunship should *jolt* when a round lands so you know the shot counted. Painting the whole sprite white fights the outline/sticker look. A tiny left-right rattle is the cabinet language for “armor took it.”

**Engine:** `Enemy` (heavies only) and `BossComponent` hold `shudderTimer`. `triggerMicroShudder()` sets it to `SHUDDER_DURATION` (0.08 s). Update subtracts `dt`. Draw wraps that unit’s blit in `save` / `translate(±SHUDDER_AMPLITUDE, 0)` / `restore` with ping-pong `(shudderTimer * 100).toInt() % 2`. The boss is one welded sheet, so a part hit shudders the whole vehicle blit, then the matrix pops. Drones, kamikazes, and interceptors do not shudder.

### 14. Stage clear is a swept, dimmed map

**Plain:** Bonus text on top of a frozen bullet soup is unreadable. The map can stay so you know which theater you beat; the war toys go away.

**Engine:** On the playing → `STATE_CLEAR` edge, `sweepPlayfieldForClear()` deactivates enemies, both bullet pools, missiles, pickups, bombs, score popups, and the boss, and zeros shake/flash. World sprites are not drawn in `STATE_CLEAR`. A 40% black `drawColor` wash sits under the scorecard HUD.

### 15. Title is a still, center-cropped; briefing is a state

**Plain:** The cabinet should sell the next map with one painting and two gold lines—not a scrolling theater behind the logo.

**Engine:** Title skips `tickParallax`. `titleBackdropBmp` loads via `decodeKeyed(R.drawable.title_screen_backdrop)`. Draw uses `max(scaleX, scaleY)` into `titleDstRect` (no `new Rect`/`Matrix` on the frame). `STATE_INTERSTITIAL` (7): `interstitialTimer` counts down; at 0 → `STATE_PLAYING`. Title start and recap-continue (when not finished) both assign `INTERSTITIAL_SECS`. `UIController` decodes six native-aspect cards; dest is width-fit + vertical center. Two `drawCentered` calls on `OP_PREFIX` and `OP_STAGE1`…`6`.

---

## Enemy formation design

This is the part that makes the game *read* as a shmup instead of “random sprites from the top.”

### What a formation is, plainly

A **formation** is a group of ships that enter with a **shared shape and a shared job**. Players learn the shape: “that V will stop and shoot,” “those two lanes will snake,” “that wall is for the bomb.” If every enemy were an independent random spawn, the screen would be noisy but not readable. Psikyo and Toaplan games teach you *patterns*, then mix them.

WW2 Blitz does **not** spawn a “Formation” object. It spawns several `Enemy` slots at related positions and gives them the same **pattern id** (or a special cue like diamond leader/wings). The *look* of a formation is emergent from those slots.

### Vocabulary of jobs

| Job | What the player should feel | How we approximate it |
| --- | --- | --- |
| **Teach the gun** | First seconds must feed a **P** so you are not stuck at power 1 | Opening **sweep-arc squadron** (a small V that flies a curve) |
| **Flank pressure** | Do not camp a corner | Drones from **off the left and right edges**, crossing inward |
| **Readable hold** | A V that **stops mid-screen** and aims is a shooting gallery | Interceptors + `PATTERN_V_HOLD` |
| **Lane dodge** | Two columns that **weave** force you to pick a gap | Drones + `PATTERN_WEAVE` at 30%/70% (or 25%/75%) width |
| **Bomb bait** | Two chunky ships that refuse to die quickly | Twin **heavies** as a “wall” |
| **Pincer** | Threat from both sides with a stagger so it is fair | Stage 2 pairs: left then right a beat later |
| **Diamond dive** | A kamikaze pack with a leader | Cues `DIAMOND_LEADER` / `WING_L` / `WING_R` |
| **Charge wall** | A row of bodies, not a row of bullets | Stage 4 drones in lanes, faster vy |
| **Cruiser** | One fat mid-boss grunt before the real boss | High-HP interceptor/heavy with hold |

Types are few (**four**) so silhouettes stay readable at phone size. Complexity lives in **when** they appear and **which pattern** they run, not in a dozen enemy classes.

### Why time, not “when the last wave dies”

If the next wave waited until the screen was empty, a skilled player would skip content and a struggling player would stall forever. A **clock** guarantees the stage has a rhythm: pressure, pause, V, weaves, wall, boss. `MAX_ACTIVE` is a safety valve so a slow device or a huge overlap cannot exceed the pool.

One-shot flags (`vFormSpawned`, `wallSpawned`) mean a wave cannot double-fire if a hitch makes `elapsedTime` jump across the threshold twice in spirit—the flag is the latch.

### Stage 1 — teach, then stack

1. **Opening sweep V** — small, curved entry, power-up duty.  
2. **Flank drones** — left/right crossers on a spacing timer (`FLANK_SPACING`). Teaches you the whole width of the screen.  
3. **Interceptor V-hold** — three ships in a V that descend and **hold**. This is the first “aim at me” exam.  
4. **Weave pairs** — two drones on sine paths. Teaches dodging without filling the sky.  
5. **Twin heavies** — slow, high HP. Teaches bombs and focus fire before the B-17-style boss.

Scroll is the **slowest** of the six so new players can read the canyon and the shapes.

### Stage 2 — pincer and geometry

Stage 2 is **faster scroll** and meaner geometry:

- **Finger pincers** — pairs from left then right so you cannot hug one edge.  
- **Flank heavies** with a **death-clear** cue: killing them eats nearby enemy bullets (a small “cancel” reward).  
- **Center weaves**, **turret-guard interceptors** on V-hold at the sides.  
- **Kamikaze diamond** — leader plus wings, then a fast solo kami.  
- **Left/right walls** of drones at slightly different speeds so the wall is not a flat line (easier to thread, still threatening).  
- Pre-boss weaves and a center interceptor as a “last quiz” before the tank.

The design thesis: **deny camping**, then **test tracking** (diamond), then **test bombing** (walls).

### Stage 3 — ocean scouts and a cruiser

Shorter clock to boss (~25 s). **Scout Vs** of weaving drones keep the water busy without heavies every second. **Alternating flanks** (`s3FlankFromLeft` toggles) so the threat side swaps. One **cruiser** (fat interceptor, V-hold, high HP) is a mid-stage “mini-peel” before the carrier boss. Timeline **freezes** once the boss is cued so the carrier does not share the screen with an infinite scout pump.

### Stage 4 — width and panic

Fastest scroll. **Horizontal flankers** (vx from the sides, not just vy from the top) use the whole width. A **cruiser**, **weaves**, **paired kamikazes**, then **charge walls** (drones in lanes). The jungle boss is long (~45 s gate) and dense; freezing the timeline at the gate is mandatory or the fortress would drown in leftover walls.

### Stage 5 — facility approach, then a three-module engine

Scroll starts at 280 px/s. **No opening power-V** (you should already be armed). 0–12 s: diagonal drone sweeps plus a kamikaze V. 14–28 s: two 25 HP heavies on V-hold and interceptors in the center band. 32 s: red-anchor drones. 36 s: kamikaze wall. 40–45 s: **no spawns**, scroll ramps to 0. At 45 s the Vulcan Engine enters. Timeline freeze matches stages 3–4 so the factory does not keep pouring fodder behind the boss.

### Stage 6 — empty sky, then the orbit boss

Stage 6 is **not** another formation stack. `SpawnTimeline.update` sees `isStage6Backdrop`, runs a 5-second intro clock, calls `beginEntranceForStage(6)`, latches, and **returns**. Attract demo never picks this map.

### What we deliberately did *not* do

- **No steering groups.** There is no “formation leader” object that children follow, except diamond cues that set flags on the `Enemy` slot. Follow-the-leader graphs allocate and desync.  
- **No 12 enemy classes.** Four types × two main patterns (hold, weave) plus kamikaze dive is enough contrast.  
- **No random within a wave’s shape.** Random is for *which attract stage* to demo, not for scattering a V. Random scatter looks like bugs.  
- **No spawn when pool is full.** `countActive() >= MAX_ACTIVE` skips; better to drop a ship than to overwrite a live one.

Those choices map directly to `SpawnTimeline` + `Enemy.pattern` + a 48-slot pool.

---

## From design to components

**Plain:** Each product rule above has a “home” class so the rule cannot dissolve into `GameView` spaghetti. `GameView` still **calls** everyone, but it does not *simulate* bullets itself.

| Design rule | Home in code |
| --- | --- |
| Cabinet attract | `GameView` attract timers, `STATE_DEMO`, `demoPilot()`, ranking draw |
| Title still | `titleBackdropBmp`; center-crop in `doFrame`; HUD in `drawTitleScreen` |
| No GC in combat | Pools: `Enemy`, `PlayerBullet`, `EnemyBullet`, missiles, explosions, graze sparks, pickups |
| Scripted waves | `SpawnTimeline` + flags; `StageData` identity flags for which director runs |
| Formation shapes | `spawnVFormation`, weaves, pincers, walls, diamond cues in `SpawnTimeline`; motion in `EnemyPoolManager` |
| Peel bosses | `BossController` + `BossComponent[14]`; wreck sheets; `FX_PHASE` / `FX_DEATH` / Stage 5 `FX_VICTORY_*` |
| Stage 1–4 / 6 guns | Isolated `EnemyWeaponSystem.updateStage1Boss` … `updateStage6Boss` |
| Tiny core + graze ring | `PlayerShip` radii; `BulletManager.resolveEnemyBulletsVsPlayer`; `EnemyBullet.FLAG_GRAZED` |
| Running score | `ScoreManager`; graze 100; flank 25k / core 100k; recap 50k/20k/500 |
| Camera kick / bleach | `triggerScreenShake` / `triggerScreenFlash` / `triggerWhiteFlash`; world `translate`; 40% white quad |
| Hit shudder | `triggerMicroShudder`; 2 px ping-pong `translate` on heavies and boss blit |
| Stage clear card | `sweepPlayfieldForClear`; skip world draw; 40% dim; `ScoreManager` recap + `UIController` |
| Next-map briefing | `STATE_INTERSTITIAL`; `UIController.loadInterstitials` / `drawStageInterstitial` |
| Relative flight | `PlayerShip` pointer tracking; frames 1–7 |
| Auto-fire / bombs | `BulletManager` cooldown; `PanicBomb`; double-tap in `GameView` |
| Green key | loadKeyed / `keyGreen` at bitmap load; Stage 5 canopy punches `#00FF00` |
| Parallax | `ParallaxBackground` + ground override; Stage 5 dual Y; Stage 6 floor swap + orbit overlay |
| Vsync + GPU blit | `GameView.doFrame` → `lockHardwareCanvas` |
| SFX/BGM | `SoundManager`; `syncBgm()`; interstitial uses stage BGM; `stopBGM()` during Stage 5 victory |
| High scores | `HighScoreManager`; `STATE_REGISTRATION` |
| Campaign end | `STATE_CAMPAIGN_COMPLETE` when `isCampaignFinished` after `advanceToNextStage()` |
| Score ticker | `ScoreManager.recapPhase` + `UIController` char buffers |
| End credits | `drawCampaignCompleteCredits` + `Paint.breakText` wrap |

---

## Architecture (high level)

**Plain:** Think of the app as a **theater**. `MainActivity` is the building (window, audio life). `GameView` is the stage manager: it decides which *scene* is on (title, play, game over). Every vsync it tells each **department** (enemies, bullets, boss, sound) to take one small step, then paints the frame from back to front.

Departments do not talk to the title screen. They only expose “turn a slot on,” “step time `dt`,” “draw on this canvas,” “turn everything off.”

```mermaid
flowchart TB
  MA[MainActivity]
  SM[SoundManager]
  GV[GameView]
  CH[Choreographer]

  MA --> SM
  MA --> GV
  GV --> CH

  subgraph frame["Each vsync: doFrame"]
    ST[gameState / attractCycle]
    SD[StageData]
    TL[SpawnTimeline]
    EN[EnemyPoolManager]
    EW[EnemyWeaponSystem]
    BS[BossController]
    PL[PlayerShip]
    BL[BulletManager]
    HM[HomingMissileManager]
    PB[PanicBomb]
    PU[PowerUpItem / PowerUpManager]
    PA[ParallaxBackground]
    PT[ParticleManager]
    UI[UIController]
    SCR[ScoreManager]
    HS[HighScoreManager]
  end

  GV --> ST
  ST --> SD
  ST --> TL
  TL --> EN
  TL --> BS
  EN --> EW
  BS --> EW
  PL --> BL
  BL --> HM
  BL --> SCR
  GV --> PB
  GV --> PU
  GV --> PA
  GV --> PT
  GV --> UI
  GV --> SCR
  GV --> HS
```

**Boot:** hide system bars → init `SoundManager` → construct `GameView` → load high scores once → when the surface exists, post a Choreographer callback.

**One frame:** measure `dt` → update only what the current scene needs → optionally `save`/`translate` the world → draw sky/ground and sprites → `restore` → optional full-screen flash → draw HUD → unlock the canvas → ask for the next vsync.

**A real play second, in order:**

1. Title tap (or recap continue) may enter **briefing** (`STATE_INTERSTITIAL`): timer only, then `STATE_PLAYING`.  
2. Timeline may activate enemy slots or start a boss entrance (Stage 6: intro clock only).  
3. Enemies and boss may call `EnemyWeaponSystem.fireBullet`.  
4. Your drag moves `PlayerShip`; hold feeds `BulletManager` (and missiles at high power).  
5. `resolveCollisions` in `GameView` compares pools: **6 px core** vs enemy shots (graze ring 24 px, once per bullet), body vs rams, padded boxes vs boss parts. Score, particles, peel damage, heavy/boss shudder. After collisions the boss may pulse shake/flash. Core hit that spends a life respawns; `enterGameOver` only if `player.isGameOver()`.  
6. Core dead: stages 1–4 play the short explode overlay then sweep → recap → `STATE_CLEAR`. Stage 5 runs the 4.5 s victory script first, then the same recap. Recap continue either briefs the **next** id or, if the latch is set, credits.  
7. Lives gone → game over → maybe registration.

---

## Component chapters

Each chapter starts with the idea, then the mechanism.

### `MainActivity`

**Idea:** The OS needs one screen. That screen should be full-bleed, stay awake, and own music volume keys. Gameplay is not a stack of XML buttons.

**Mechanism:** Immersive `Activity`, `FLAG_KEEP_SCREEN_ON`, `STREAM_MUSIC`. `onCreate` initializes audio then `setContentView(GameView)`. Pause saves volumes; destroy releases the pool and players. No Compose, no fragment.

### `GameView`

**Idea:** One object is allowed to know *what screen we are on* and *in what order to tick the world*. If every class knew about “title vs play,” you could not reuse the demo path.

**Mechanism:** `SurfaceHolder.Callback` + `Choreographer.FrameCallback`. State is an `Int`, not an enum (no extra objects, easy `when`).

| Constant | Scene |
| --- | --- |
| `STATE_TITLE` | Attraction still + logo / settings / ranking overlay |
| `STATE_PLAYING` | Your campaign |
| `STATE_CLEAR` | Bonus ticker after a boss |
| `STATE_GAMEOVER` | Death pause |
| `STATE_DEMO` | CPU attract |
| `STATE_REGISTRATION` | Three initials |
| `STATE_CAMPAIGN_COMPLETE` | Credits after the playlist latch |
| `STATE_INTERSTITIAL` | Full-screen briefing card (3 s) |

Attract uses a second int (`ATTRACT_*`) and one float timer so title/demo/ranking can share `STATE_TITLE` / `STATE_DEMO` without a third activity.

**Play/demo tick order:** parallax → optional `demoPilot` → player → player bullets/missiles → pickups → floating scores → timeline → enemies → boss → enemy shots → bomb → particles → collisions → boss visual flags → clear/demo-timeout. **`STATE_TITLE`** does not tick parallax. **`STATE_INTERSTITIAL`** only decrements `interstitialTimer`.

**Paint order:** optional world `translate` → background (title: center-crop still; interstitial: `drawStageInterstitial`; else parallax) → world sprites → `restore` → flash → HUD. Title HUD is `drawArcadeUI` after the still. Interstitial skips combat HUD.

**Stage 5 tactical z:** floor → enemies/boss/enemy bullets → canopy → player, pickups, player shots, particles, bomb → HUD. The roof occludes hostile craft; you stay readable on top.

**Stage 6 tactical z:** after `S6_CANOPY_AT` (35 s), same sandwich using the orbit overlay; before that, the active floor (clouds, then space at 30 s).

**Why collisions live here:** they need *every* pool. Splitting enemy-shot vs player *math* still lives in `BulletManager.resolveEnemyBulletsVsPlayer` so the Euclidean core/graze test is one loop; `GameView` owns game-over and only enters it when `isGameOver()` is true (`takeDamage()` returning true can mean “this life exploded,” not “credit over”). Rams use a **fraction of body size**. Boss shots use padded AABBs. Bomb damage is **DPS with a per-frame cap** so a 0.5 s animation neither deletes the core through armor nor does nothing.

**HUD:** one `StringBuilder`, shared paints. Hit testing is `RectF.contains`, not Android widgets.

**Demo AI:** aim at the lowest on-screen enemy or boss part, sidestep nearby downward bullets, sine-wander if idle. `lastDemoStage` + LCG avoids repeating the showcase map.

### `StageData`

**Idea:** “Which war theater is this?” is an **identity**, not a playlist index. Scroll, music, overlay clouds, and which spawn director runs must survive a shuffled or duplicated `STAGE_SEQUENCE`.

**Mechanism:** Playlist default `intArrayOf(1, 2, 3, 4, 5, 6)`. Flags: `hasOverlayClouds`, `isStage1Script`…`isStage4Script`, `isStage5Backdrop` / `isStage6Backdrop`, `locksElapsedAtBoss`, `usesOpeningPowerV`, `usesSharedBossEntranceCue`. `isCampaignFinished` is **only** `campaignFinishedLatch`, set when `advanceToNextStage()` increments past `STAGE_SEQUENCE.size` (index clamped, no wrap). `isLastInSequence()` is “current index is last”; do **not** use it as campaign-complete. `setCurrentStage` scans with a `while`. `resetToStart()` clears the latch.

### `SpawnTimeline`

**Idea:** A stage is a **clock with latch flags**, not a list of entity instances in memory.

**Mechanism:** `elapsedTime += dt` unless `locksElapsedAtBoss && bossCueFired`. `update()` reads `stageData` flags (no `currentStage` argument). Opening power-V only if `usesOpeningPowerV`. Shared boss-at-elapsed cue only if `usesSharedBossEntranceCue`. Stage 5 owns its `stageTimer` and gate. Stage 6 **early-returns** after the 5 s intro cue. `reset()` zeros wave/pincer/drizzle/opening-V/`stageTimer` latches so a **duplicate** id in the playlist can fire again.

See [Enemy formation design](#enemy-formation-design) for *why* each wave exists.

### `Enemy` and `EnemyPoolManager`

**Idea:** An enemy is a **row in a table**, not a Java object graph. Forty-eight rows is enough for the densest wall. When a ship dies, its row is marked inactive and can be reused.

**Mechanism:** `Enemy` holds x/y/vx/vy, type, pattern, HP, fire timers, weave phase, diamond flags, `shudderTimer`, etc. `EnemyPoolManager` owns the array, four keyed sheets, sizes, and a sine LUT for sweep-arc flight (`FLIGHT_PROFILE_SWEEP_ARC`). `spawnEnemy` finds `!isActive`. `update` integrates, runs hold/weave/kamikaze logic, may fire, and ticks shudder. Draw is shadow, black outline, body. **Heavies** add a local ±2 px `translate` while `shudderTimer > 0` after `triggerMicroShudder()` from a player round.

### `EnemyBullet` and `EnemyWeaponSystem`

**Idea:** Every green shot is the same physical thing: a point with a velocity. Bosses and grunts share one pool so a dense boss pattern cannot allocate.

**Mechanism:** First-free `fireBullet`. `flags` is a bitmask; `FLAG_GRAZED` is set once when the shot threads the player halo. `beginDeathClear` records a short-lived circle that deactivates nearby enemy shots. `convertActiveToScoreItems()` (Stage 5 core death) deactivates every live shot and spawns a zero-velocity +100 medal. Draw: green oval + white core. Isolated `dt` gates: Stage **1** nose + wing 3-way; **2** dual down + sponson fans; **3** flak + 7-bullet wall; **4** mortars + gatling; **5** volley/ring / fans / dual spiral; **6** cyan/pink as above.

### `BossComponent` and `BossController`

**Idea:** A boss is a **constellation of parts** welded to a core position. Art is one sheet; gameplay is many HP bars.

**Mechanism:** Up to 14 `BossComponent`s. Entrance moves the constellation on; then hover/sweep. Stage-specific timers fire into `EnemyWeaponSystem`. `isCoreVulnerable()` walks non-core parts. Wreck overlays for left/right/center. Stages 1–4: explode frames on core death. `refreshPhaseFlags` / `consumeVisualFlags` expose peel-complete (`FX_PHASE`) and core death (`FX_DEATH`) as primitive bits; Stage 5 also pulses `FX_VICTORY_CASCADE` / `FX_VICTORY_SHATTER`. A damaged part calls `triggerMicroShudder()`; draw `save`/`translate`/`restore`s the whole body blit ±2 px.

**Stage 5 hitboxes** are preallocated `RectF`s (`leftFlankBounds`, `rightFlankBounds`, `coreBounds`) rebuilt from core XY each frame—no new rects. Edge flags (`hasDroppedLeftReward` / `Right` / `Core`) grant score and `PowerUpManager` drops **once**. `victorySequenceTimer` drives the 4.5 s freeze (halt, `stopBGM`, `SFX_HEAVY_EXPLOSION`), 0.15 s cascade `ParticleManager.instance.spawnExplosion()` inside those bounds, 3.0 s wreck-center + 8-wide cluster + `SFX_BOMB` + white flash, then deactivate so `GameView` can recap. Bitmaps: `boss_stage5_full` plus wreck left/right/center.

### `PlayerShip`

**Idea:** The player is the only thing that should feel *sticky* and *readable*. Bank art (seven frames) telegraphs horizontal speed. The **drawn** ship is large; the **core** is a 6 px circle so weaving is possible. A 24 px graze ring scores near-misses.

**Mechanism:** Relative drag, clamp, lives/hits, invuln/respawn timers, `weaponPowerLevel`, `autoFire` for demo. `centerX`/`centerY` are the sprite center. `takeDamage()` returns true when the **plane explodes** (life spent); `isGameOver()` is true only when lives hit 0. `steerToward` is a simple arrive used by `demoPilot`. Same chroma-keyed blit stack (`dst` is visual only).

### `PlayerBullet` and `BulletManager`

**Idea:** Your gun is a **metronome**, not a spray of new objects. Power only changes *how many* slots you activate per tick. The same class also runs the **enemy-bullet vs core/graze** test so that loop stays Euclidean and allocation-free.

**Mechanism:** Pool of 100. ~0.12 s vulcan cooldown. Separate missile cooldown at power ≥ 3. Yellow `RectF`s for *your* shots (draw only). `spawnWeaponStream` fans extra bullets. `resolveEnemyBulletsVsPlayer` walks the enemy pool: core hit → `takeDamage`; else graze latch + score + spark.

### `ScoreManager`

**Idea:** One running total the HUD, graze, medals, and registration all read. No second score living only in `GameView`.

**Mechanism:** Singleton like `SoundManager`. Clamp 0…99,999,999. `addGrazeScore` increments **grazeCount** and score. Flank/core/bullet-cancel bonuses queue a popup drained into `GameView`’s floating-text pool. `beginRecap(lives, bombs)` snapshots recap targets; `updateRecap(dt)` ticks `recapPhase` / `recapTimer` and adds score over 1.0 s per row. `resetStageCounters()` zeros graze and recap flags when a map starts or you tap through the total.

### `HomingMissileManager`

**Idea:** Missiles are a *reward* for powering up, not a second stick. They should look like sprites, not yellow dots, and they should prefer a real target.

**Mechanism:** Small pool, keyed bitmap, seek nearest living enemy or vulnerable part. Spawned from `BulletManager`.

### `PanicBomb`

**Idea:** One bomb at a time, big and readable, ~half a second. It is a resource, not a laser you hold.

**Mechanism:** Single instance, 6 frames at ~12 fps, scale-up. `GameView` applies DPS with banks and a frame cap so peel rules still apply (core stays safe until modules are gone, unless already vulnerable).

### `PowerUpItem` / `PowerUpSlot`

**Idea:** Drops are few, must never allocate, medals should flicker frames like real arcade items.

**Mechanism:** Slot pool with type enum (power / bomb / medal / shield). `spawnSway` uses a sine-wave X drift for Stage 5 guaranteed drops. `GameView` converts extras to medals and applies upgrades. `PowerUpManager` is the singleton that owns the pool: `spawnGuaranteedDrop`, `spawnRightFlankDrop`, `spawnBulletCancelDrop`.

### `ParticleManager` and `ActiveExplosion`

**Idea:** Explosions are decoration with a cap. If ten ships die in one frame, we still only have N explosion slots.

**Mechanism:** Sheet sliced into frames, pool of `ActiveExplosion` (28 slots), optional SFX. `spawnExplosion` is the Stage 5 victory alias for `triggerExplosion` (no extra SFX). `GameView` binds the instance at construct. Separate pooled `GrazeSpark` dots (`triggerSpark`) with velocity and a short life; draw even if the explosion sheet is missing.

### `ParallaxBackground`

**Idea:** The world should move at three speeds so your eye gets depth: dirt/water fast, haze slower, high clouds slowest.

**Mechanism:** Three cover-scaled bitmaps, wrapping Y. `GameView` can pass stage 2–4 ground. SCREEN blend on black-authored cloud layers. Registration forces stage 1 ground. Stage 5: dual Y + 1.5× canopy. Stage 6: `updateStage6` applies a time envelope to `scrollSpeedY` and advances `stage6Layer2Y` at 1.5×; `GameView` swaps `activeFloorBmp` cloud → space at 30 s.

### `UIController`

**Idea:** Recap, briefing titles, and credits must not allocate strings while they draw.

**Mechanism:** Pre-cached `CharArray` labels and an 80-char line buffer. `writeInt` parses digits in place. `drawStageClear` centers lines from `ScoreManager`. `PRESS FIRE TO CONTINUE` is 30 frames on / 30 off.

`drawStageInterstitial` width-fits the native card, two gold lines (`OP_PREFIX` + mission), fade from `timer`. `loadInterstitials` / `releaseInterstitials` own the six bitmaps.

`drawCampaignCompleteCredits` scrolls `CREDIT_CHARS` at 75 px/s. `wrapCreditLine` uses `Paint.breakText` into a reused `FloatArray(1)` (85% width).

### `HighScoreManager`

**Idea:** Ten rows you can draw in a `while`. Insert is a shift, like inserting into a sorted array in a CS 101 textbook.

**Mechanism:** Kotlin `object`. Parallel arrays. `checkIfQualifies` vs 10th. `checkAndInsertNewScore` shifts with bounds on the 3-char stride (indices 0–29). `apply()` off the frame. Accessors for draw.

### `SoundManager`

**Idea:** Shots click now; music does not hiccup at loop; nothing in `doFrame` builds a `MediaPlayer`.

**Mechanism:** Singleton, SoundPool, dual MediaPlayer chain, audio focus, prefs for two volume floats. `GameView.syncBgm()` maps state → raw resource. `STATE_PLAYING`, `STATE_DEMO`, and `STATE_INTERSTITIAL` share the **stage** track. During Stage 5 victory `want` is 0 and `stopBGM()` resets both players. Recap / campaign complete still use `bgm_victory`. Title uses `bgm_title`.

### `FloatingScore` (private in `GameView.kt`)

**Idea:** “+2000” should float up and vanish. Twelve is enough.

**Mechanism:** Pool of `{x,y,value,age,active}`. Drawn with the same `StringBuilder`.

---

## Repo layout

```
app/src/main/java/com/cc/ww2blitz/   # engine sources
app/src/main/res/drawable/            # keyed sprites, logos, title_screen_backdrop, stage grounds, Stage 5/6 floors, canopy/orbit, interstitial_stage1–6
app/src/main/res/raw/                 # BGM / SFX
app/src/main/AndroidManifest.xml      # MainActivity, portrait, applicationId com.cc.ww2blitz
```

Application id: **`com.cc.ww2blitz`**. Gradle project name: `WW2Blitz` (no space; Studio run configs break on spaces). Launcher label: **WW2 Blitz**.

Open in Android Studio, sync Gradle, run **app** (minSdk 26).
