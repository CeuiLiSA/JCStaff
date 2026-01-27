package ceui.lisa.jcstaff.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ceui.lisa.jcstaff.components.AnimatedShaderBackground
import ceui.lisa.jcstaff.core.AppLanguage
import ceui.lisa.jcstaff.core.LanguageManager
import kotlinx.coroutines.launch

@Composable
fun LanguageSelectionScreen() {
    val coroutineScope = rememberCoroutineScope()
    val systemDefault = remember { AppLanguage.fromSystemLocale() }
    var selected by remember { mutableStateOf(systemDefault) }

    AnimatedShaderBackground(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icon
            Icon(
                imageVector = Icons.Outlined.Language,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = Color.White
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Primary title
            Text(
                text = "Select Language",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Secondary title
            Text(
                text = "选择语言",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Language list card — glassmorphism style
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.10f)
                ),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
            ) {
                val languages = AppLanguage.entries
                val selectedIndex = languages.indexOf(selected)
                val density = LocalDensity.current
                var itemHeightPx by remember { mutableIntStateOf(0) }

                val animatedOffset by animateDpAsState(
                    targetValue = with(density) { (itemHeightPx * selectedIndex).toDp() },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    label = "highlightOffset"
                )

                Box(modifier = Modifier.padding(vertical = 4.dp)) {
                    // Sliding highlight indicator
                    if (itemHeightPx > 0) {
                        Box(
                            modifier = Modifier
                                .offset(y = animatedOffset)
                                .fillMaxWidth()
                                .height(with(density) { itemHeightPx.toDp() })
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.18f))
                        )
                    }

                    // Language items
                    Column {
                        languages.forEach { language ->
                            LanguageItem(
                                language = language,
                                isSelected = language == selected,
                                onClick = { selected = language },
                                onMeasured = { height ->
                                    if (itemHeightPx == 0) itemHeightPx = height
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Confirm button
            Button(
                onClick = {
                    coroutineScope.launch {
                        LanguageManager.setLanguage(selected)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.15f),
                    contentColor = Color.White
                ),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
            ) {
                Text(
                    text = "OK",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun LanguageItem(
    language: AppLanguage,
    isSelected: Boolean,
    onClick: () -> Unit,
    onMeasured: (Int) -> Unit = {}
) {
    val contentColor by animateColorAsState(
        targetValue = if (isSelected)
            Color.White
        else
            Color.White.copy(alpha = 0.8f),
        label = "itemContent"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .onSizeChanged { onMeasured(it.height) }
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = language.displayName,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
            color = contentColor
        )

        if (isSelected) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = Color.White
            )
        } else {
            // Reserve space so layout doesn't shift
            Spacer(modifier = Modifier.size(20.dp))
        }
    }
}
