package com.trep.passwordmanager.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trep.passwordmanager.data.crypto.TotpGenerator

@Composable
fun TotpIndicator(
    remainingSeconds: Int,
    modifier: Modifier = Modifier
) {
    val progress = remainingSeconds.toFloat() / TotpGenerator.TOTP_STEP.toFloat()
    val isUrgent = remainingSeconds <= 5

    val indicatorColor by animateColorAsState(
        targetValue = if (isUrgent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        label = "totp_indicator_color"
    )

    Surface(
        shape = MaterialTheme.shapes.small,
        color = if (isUrgent) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.size(height = 24.dp, width = 64.dp)
        ) {
            Spacer(modifier = Modifier.width(6.dp))
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.size(12.dp),
                color = indicatorColor,
                strokeWidth = 2.dp,
                trackColor = Color.Transparent
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "${remainingSeconds}s",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                ),
                color = if (isUrgent) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}
