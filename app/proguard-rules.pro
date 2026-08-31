# =============================================================================
# WW2 BLITZ 2026 - PRODUCTION OPTIMIZATION & OBFUSCATION RULES
# =============================================================================

# Global Optimization Flags
-optimizationpasses 5
-allowaccessmodification
-dontpreverify

# 1. Protect Android System Components
-keep public class * extends android.app.Activity
-keep public class * extends android.view.View
-keep public class * extends android.content.Context

# 2. Protect Custom Engine Views & Surface Components
# This prevents ProGuard from renaming GameView, allowing the layout inflater to bind it safely.
-keep class com.cc.ww2blitz.GameView {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# 3. Protect Zero-Allocation Singletons & Pools
# Your managers use rigid static/lazy instances. Renaming these fields can break reference threads.
-keep class com.cc.ww2blitz.HighScoreManager { *; }
-keep class com.cc.ww2blitz.SoundManager { *; }
-keep class com.cc.ww2blitz.ScoreManager { *; }
-keep class com.cc.ww2blitz.PowerUpManager { *; }
-keep class com.cc.ww2blitz.ParticleManager { *; }

# 4. Protect Native Structural Resource Identifiers
# Your background and sprite loaders use openRawResourceFd and decodeResource.
# We MUST prevent ProGuard from stripping or breaking the generated R class constants fields.
-keepclassmembers class com.cc.ww2blitz.R$* {
    public static <fields>;
}
-keep class com.cc.ww2blitz.R { *; }

# 5. Native Math & Arithmetic Loop Optimizations
# Force ProGuard to keep our high-speed math parameters from micro-precision variations.
-keepclassmembers class * {
    val *Speed*;
    val *Interval*;
    val *Timer*;
    val *Angle*;
}

# 6. Suppress Safe Hardware Warnings
# Your SoundManager checks for explicit SDK versions (Oreo+ paths). Suppress non-critical warnings.
-dontwarn android.media.AudioFocusRequest*
-dontwarn android.media.AudioAttributes*
