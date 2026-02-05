package ceui.lisa.jcstaff.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ceui.lisa.jcstaff.R

@Composable
fun LoadingIndicator(blockHeight: Dp = 160.dp) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(blockHeight),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(28.dp),
                color = Color.White,
                trackColor = Color.White.copy(alpha = 0.3f),
                strokeWidth = 5.dp
            )
            Spacer(Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.loading),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}