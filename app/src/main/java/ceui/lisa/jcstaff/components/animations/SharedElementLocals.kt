@file:OptIn(ExperimentalSharedTransitionApi::class)

package ceui.lisa.jcstaff.components.animations

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.compositionLocalOf

/**
 * CompositionLocals for shared element transitions.
 * Provided by SharedTransitionLayout + AnimatedContent in MainActivity.
 */
val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }
val LocalAnimatedVisibilityScope = compositionLocalOf<AnimatedVisibilityScope?> { null }
