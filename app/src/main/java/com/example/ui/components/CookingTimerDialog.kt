package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.settings.AppSettingsPreferences
import com.example.ui.theme.CaveatFontFamily
import com.example.ui.theme.PatrickHandFontFamily
import com.example.ui.theme.PostItMint
import com.example.ui.theme.PostItPeach
import com.example.ui.theme.PostItYellow
import com.example.util.TimerAlertManager
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun CookingTimerDialog(
    initialMinutes: Int,
    timerTitle: String,
    resumeExisting: Boolean = false,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val persisted by AppSettingsPreferences.observeTimerState(context).collectAsStateWithLifecycle()

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { }

    val requestedDurationSec = remember(initialMinutes) { (initialMinutes * 60).coerceAtLeast(60) }
    var totalSeconds by remember {
        mutableIntStateOf(
            if (resumeExisting && persisted.durationSeconds > 0) persisted.durationSeconds else requestedDurationSec
        )
    }
    var secondsLeft by remember {
        mutableIntStateOf(
            if (resumeExisting && persisted.durationSeconds > 0) {
                if (persisted.isRunning) {
                    ((persisted.endTimeMillis - System.currentTimeMillis()) / 1000L).toInt().coerceAtLeast(0)
                } else {
                    persisted.pausedRemainingSeconds.coerceAtLeast(0)
                }
            } else {
                requestedDurationSec
            }
        )
    }
    var isRunning by remember {
        mutableStateOf(if (resumeExisting && persisted.durationSeconds > 0) persisted.isRunning else true)
    }
    var isFinished by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (!resumeExisting) {
            TimerAlertManager.startOrResumeTimer(
                context = context,
                durationSeconds = requestedDurationSec,
                remainingSeconds = requestedDurationSec,
                label = timerTitle
            )
        }
    }

    LaunchedEffect(isRunning, persisted.endTimeMillis) {
        if (isRunning) {
            while (isRunning) {
                val targetEnd = AppSettingsPreferences.getTimerState(context).endTimeMillis
                val rem = if (targetEnd > 0L) {
                    ((targetEnd - System.currentTimeMillis() + 999L) / 1000L).toInt().coerceAtLeast(0)
                } else {
                    secondsLeft
                }
                secondsLeft = rem
                if (rem <= 0) {
                    isRunning = false
                    isFinished = true
                    TimerAlertManager.triggerVibration(context)
                    TimerAlertManager.postFinishedNotification(context, timerTitle)
                    AppSettingsPreferences.saveTimerState(
                        context,
                        persisted.copy(isRunning = false, pausedRemainingSeconds = 0, endTimeMillis = 0L)
                    )
                    break
                }
                delay(500L)
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
                    text = timerTitle,
                    fontFamily = CaveatFontFamily,
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
                    .verticalScroll(rememberScrollState())
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
                            text = String.format(Locale.US, "%02d:%02d", minutes, seconds),
                            fontFamily = CaveatFontFamily,
                            fontSize = 46.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (isFinished) {
                            Text(
                                text = stringResource(R.string.timer_done),
                                fontFamily = CaveatFontFamily,
                                fontSize = 22.sp,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Text(
                                text = if (isRunning) stringResource(R.string.timer_cooking) else stringResource(R.string.timer_paused),
                                fontFamily = PatrickHandFontFamily,
                                fontSize = 15.sp,
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
                        PostItButton(
                            text = if (isRunning) stringResource(R.string.timer_pause) else stringResource(R.string.timer_resume),
                            icon = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                            onClick = {
                                if (isRunning) {
                                    isRunning = false
                                    TimerAlertManager.pauseTimer(context, secondsLeft)
                                } else {
                                    val resumeFrom = if (secondsLeft > 0) secondsLeft else totalSeconds
                                    isRunning = true
                                    TimerAlertManager.startOrResumeTimer(context, totalSeconds, resumeFrom, timerTitle)
                                }
                            },
                            backgroundColor = PostItYellow,
                            fontSize = 17.sp,
                            modifier = Modifier.testTag("timer_toggle_button")
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                    }

                    PostItButton(
                        text = stringResource(R.string.timer_reset),
                        icon = Icons.Default.Refresh,
                        onClick = {
                            secondsLeft = totalSeconds
                            isRunning = false
                            isFinished = false
                            TimerAlertManager.cancelTimer(context)
                        },
                        backgroundColor = PostItPeach,
                        fontSize = 17.sp,
                        modifier = Modifier.testTag("timer_reset_button")
                    )
                }
            }
        },
        confirmButton = {
            PostItButton(
                text = stringResource(R.string.action_close),
                onClick = onDismiss,
                backgroundColor = PostItMint,
                fontSize = 17.sp,
                modifier = Modifier.testTag("timer_close_button")
            )
        }
    )
}
