package dev.pranav.reef.ui.timer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.pranav.reef.R

@Composable
fun TimerControls(
    modifier: Modifier = Modifier,
    onPauseClicked: () -> Unit = {},
    onStopClicked: () -> Unit = {},
    onResetClicked: () -> Unit = {}
) {
    val colorScheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            onClick = onPauseClicked,
            modifier = Modifier
                .size(64.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = colorScheme.tertiaryContainer, // Using M3 token
                contentColor = colorScheme.onTertiaryContainer // Using M3 token
            )
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = "||",
                    color = colorScheme.onTertiaryContainer, // Aligned with content color
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Button(
            onClick = onStopClicked,
            modifier = Modifier
                .height(64.dp)
                .weight(1f)
                .padding(horizontal = 8.dp),
            shape = RoundedCornerShape(32.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = colorScheme.surfaceContainerLow, // Using M3 token
                contentColor = colorScheme.onSurface // Using M3 token
            )
        ) {
            Text(
                text = stringResource(R.string.stop),
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                style = typography.titleMedium // Using M3 Typography
            )
        }

        // 3. Reset Button (Outline Circle) - Using M3 tokens for outline/icon color
        Button(
            onClick = onResetClicked,
            modifier = Modifier
                .size(64.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent, // Background is transparent
                contentColor = colorScheme.onSurface // Icon color from M3 token
            ),
            border = BorderStroke(2.dp, colorScheme.outline) // Using M3 outline token
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                // Placeholder for Reset Icon (↻)
                Text(
                    text = "↻",
                    color = colorScheme.onSurface, // Aligned with content color
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }
    }
}
