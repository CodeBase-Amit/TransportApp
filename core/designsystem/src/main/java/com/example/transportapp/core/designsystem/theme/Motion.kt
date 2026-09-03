package com.example.transportapp.core.designsystem.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * Material 3 Expressive Motion — refined spring physics and easing curves.
 * Two vocabularies:
 *  - **Springs** for finger-driven interactions (buttons, tiles, chips): bouncy for celebratory
 *    moments, snappy for everyday state changes, crisp for press feedback.
 *  - **Easing tweens** for self-playing animations (screen transitions, reveals, count-ins):
 *    emphasized for entering, accelerate for exiting, standard for on-screen movement.
 *
 * Respects prefers-reduced-motion by degrading animations to instant or near-instant.
 */
object HaulMotion {
    // Springs — finger-driven interactions

    /** Celebratory landing — booking saved, payment collected. Medium bounce. */
    val bouncy = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMediumLow,
    )

    /** Everyday state changes — chips, selections, toggles. No bounce. */
    val snappy = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium,
    )

    /** Button press feedback — rapid, crisp scale response. */
    val press = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessHigh,
    )

    /** Smooth drag-to-dismiss and gesture-driven animation. Carries velocity. */
    val gesture = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow,
    )

    // Easing curves — self-playing animations

    /** Emphasized easing — entering the screen or revealing content. Starts fast, decelerates. */
    val emphasizedEasing: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    /** Decelerate easing — entering secondary elements. Softer emphasis. */
    val decelerateEasing: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

    /** Accelerate easing — exiting the screen. Ends fast. */
    val accelerateEasing: Easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

    /** Standard easing — moving on screen, state transitions. Balanced. */
    val standardEasing: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    // Durations — timings for self-playing animations
    const val DURATION_ENTER = 400     // Entering screen
    const val DURATION_EXIT = 200      // Exiting screen
    const val DURATION_UTIL = 300      // Standard on-screen movement
    const val DURATION_SHORT = 150     // Quick transitions (hover, focus)
    const val DURATION_LONG = 500      // Extended reveals or complex motion

    // Animation spec builders
    fun <T> enter(reduced: Boolean = false) =
        if (reduced) tween<T>(50) else tween<T>(DURATION_ENTER, easing = decelerateEasing)

    fun enterFloat(reduced: Boolean = false) = enter<Float>(reduced)

    fun <T> exit(reduced: Boolean = false) =
        if (reduced) tween<T>(50) else tween<T>(DURATION_EXIT, easing = accelerateEasing)

    fun exitFloat(reduced: Boolean = false) = exit<Float>(reduced)

    fun <T> util(reduced: Boolean = false) =
        if (reduced) tween<T>(50) else tween<T>(DURATION_UTIL, easing = emphasizedEasing)

    fun utilFloat(reduced: Boolean = false) = util<Float>(reduced)

    fun <T> short(reduced: Boolean = false) =
        if (reduced) tween<T>(50) else tween<T>(DURATION_SHORT, easing = standardEasing)

    fun shortFloat(reduced: Boolean = false) = short<Float>(reduced)

    fun <T> long(reduced: Boolean = false) =
        if (reduced) tween<T>(100) else tween<T>(DURATION_LONG, easing = decelerateEasing)

    fun longFloat(reduced: Boolean = false) = long<Float>(reduced)
}

/**
 * Whether the user asked for reduced motion. Compose has no first-class read for the
 * developer "remove animations" setting; animations degrade per-effect (skeleton pulse
 * is the one gated hardest). Kept as a function so call sites read the same way and the
 * wiring lands in one place.
 */
@Composable
fun rememberReducedMotion(): Boolean = remember { false }
