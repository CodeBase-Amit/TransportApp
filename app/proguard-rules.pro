# S26 — R8 keep rules for TransportApp2's reflective surfaces.
# Everything else is shrunk aggressively; these are the libraries and generated
# marshals that reflection reaches.

# ── Room generated implementations (entity/DAO marshals are reflective) ──
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keep @androidx.room.Entity class * { *; }
-keep class * implements androidx.room.RoomDatabase$Callback

# ── Hilt / Dagger generated components ──
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper

# ── kotlinx-serialization (doc-engine templates + network DTOs by name) ──
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.example.transportapp.**$$serializer { *; }
-keepclassmembers class com.example.transportapp.** { *** Companion; }
-keepclasseswithmembers class com.example.transportapp.** { kotlinx.serialization.KSerializer serializer(...); }

# ── OkHttp / Conscrypt platform probes ──
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# ── The PDF WebView bridge (pdf-android reflectively calls JS) ──
-keepclassmembers class com.example.transportapp.pdf.** { *; }

# ── Google Fonts provider (certificate array is referenced by resource id) ──
-keep class com.example.transportapp.core.designsystem.R$* { *; }
