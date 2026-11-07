package com.example.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun CookingTimerDialog(
    initialMinutes: Int,
    timerTitle: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val totalSeconds = remember(initialMinutes) { (initialMinutes * 60).coerceAtLeast(60) }
    var secondsLeft by remember { mutableIntStateOf(totalSeconds) }
    var isRunning by remember { mutableStateOf(true) }
    var isFinished by remember { mutableStateOf(false) }

    LaunchedEffect(isRunning, secondsLeft) {
        if (isRunning && secondsLeft > 0) {
            delay(1000L)
            secondsLeft -= 1
            if (secondsLeft == 0) {
                isRunning = false
                isFinished = true
                triggerVibration(context)
            }
        }
    }

    val minutes = secondsLeft / 60
    val seconds = secondsLeft % 60
    val progress = if (totalSeconds > 0) secondsLeft.toFloat() / totalSeconds.toFloat() else 0f

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    timerTitle,
                    fontFamily = com.example.ui.theme.CaveatFontFamily,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(180.dp)
                ) {
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.size(180.dp),
                        strokeWidth = 10.dp,
                        color = if (isFinished) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = String.format("%02d:%02d", minutes, seconds),
                            fontFamily = com.example.ui.theme.CaveatFontFamily,
                            fontSize = 46.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (isFinished) {
                            Text(
                                "¡Tiempo cumplido!",
                                fontFamily = com.example.ui.theme.CaveatFontFamily,
                                fontSize = 22.sp,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Text(
                                if (isRunning) "Cocinando..." else "En pausa",
                                fontFamily = com.example.ui.theme.PatrickHandFontFamily,
                                fontSize = 17.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (!isFinished) {
                        com.example.ui.components.PostItButton(
                            text = if (isRunning) "Pausar" else "Continuar",
                            icon = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                            onClick = { isRunning = !isRunning },
                            backgroundColor = com.example.ui.theme.PostItYellow,
                            fontSize = 18.sp,
                            modifier = Modifier.testTag("timer_toggle_button")
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                    }

                    com.example.ui.components.PostItButton(
                        text = "Reiniciar",
                        icon = Icons.Default.Refresh,
                        onClick = {
                            secondsLeft = totalSeconds
                            isRunning = false
                            isFinished = false
                        },
                        backgroundColor = com.example.ui.theme.PostItPeach,
                        fontSize = 18.sp,
                        modifier = Modifier.testTag("timer_reset_button")
                    )
                }
            }
        },
        confirmButton = {
            com.example.ui.components.PostItButton(
                text = "Cerrar",
                onClick = onDismiss,
                backgroundColor = com.example.ui.theme.PostItMint,
                fontSize = 18.sp,
                modifier = Modifier.testTag("timer_close_button")
            )
        }
    )
}

private fun triggerVibration(context: Context) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            val vibrator = vibratorManager?.defaultVibrator
            vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 400, 200, 400, 200, 600), -1))
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            vibrator?.vibrate(longArrayOf(0, 400, 200, 400, 200, 600), -1)
        }
    } catch (_: Exception) {}
}
