package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.R
import com.example.ai.ChefChatMessage
import com.example.ai.ChefChatRepository
import com.example.ai.ChefChatRole
import com.example.ui.components.notebookPaperBackground
import com.example.ui.components.sketchedBorder
import com.example.ui.theme.CaveatFontFamily
import com.example.ui.theme.PatrickHandFontFamily
import com.example.ui.theme.PostItMint
import com.example.ui.theme.PostItYellow
import com.example.util.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ChefChatDialog(
    messages: List<ChefChatMessage>,
    selectedRole: ChefChatRole,
    isSending: Boolean,
    pantryCount: Int,
    recipeCount: Int,
    onSelectRole: (ChefChatRole) -> Unit,
    onSendMessage: (prompt: String, attachedImageUri: String) -> Unit,
    onClearChat: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var inputText by remember { mutableStateOf("") }
    var attachedImageUri by remember { mutableStateOf("") }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var cameraError by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size, messages.lastOrNull()?.text?.length) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            cameraError = null
            attachedImageUri = ImageUtils.saveImageLocally(context, uri)
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val targetUri = pendingCameraUri
        if (success && targetUri != null) {
            cameraError = null
            attachedImageUri = ImageUtils.saveImageLocally(context, targetUri)
        }
    }

    fun triggerCamera() {
        try {
            val outUri = ImageUtils.createCameraTempImageUri(context)
            pendingCameraUri = outUri
            cameraError = null
            cameraLauncher.launch(outUri)
        } catch (_: Exception) {
            cameraError = context.getString(R.string.recipe_photo_camera_unavailable)
        }
    }

    fun submitCurrent() {
        if (isSending) return
        val prompt = inputText.trim()
        if (prompt.isBlank() && attachedImageUri.isBlank()) return
        onSendMessage(prompt, attachedImageUri)
        inputText = ""
        attachedImageUri = ""
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .testTag("chef_chat_dialog"),
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = stringResource(R.string.chef_chat_title),
                                fontFamily = CaveatFontFamily,
                                fontSize = 25.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = stringResource(R.string.chef_chat_subtitle),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("chef_chat_close")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.action_close)
                            )
                        }
                    },
                    actions = {
                        if (messages.isNotEmpty()) {
                            TextButton(
                                onClick = onClearChat,
                                modifier = Modifier.testTag("chef_chat_clear")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteSweep,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.chef_chat_clear))
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .navigationBarsPadding()
                    .imePadding()
                    .notebookPaperBackground()
            ) {
                // Role / Gemini Model Selector Row
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(ChefChatRole.entries) { role ->
                        FilterChip(
                            selected = selectedRole == role,
                            onClick = { onSelectRole(role) },
                            enabled = !isSending,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = stringResource(role.labelResId),
                                    fontWeight = if (selectedRole == role) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            modifier = Modifier.testTag("chef_chat_role_${role.name}")
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))

                // Scrollable Multi-turn Conversation Thread
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .testTag("chef_chat_thread"),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .sketchedBorder(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.img_ref_almuerzo_casero),
                                    contentDescription = stringResource(R.string.recipes_hero_photo_cd),
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                )
                                Text(
                                    text = stringResource(R.string.chef_chat_welcome, pantryCount, recipeCount),
                                    fontFamily = PatrickHandFontFamily,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    if (messages.isEmpty()) {
                        item {
                            val starterPrompts = listOf(
                                stringResource(R.string.chef_chat_prompt_1),
                                stringResource(R.string.chef_chat_prompt_2),
                                stringResource(R.string.chef_chat_prompt_3)
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                starterPrompts.forEachIndexed { idx, promptText ->
                                    OutlinedButton(
                                        onClick = { onSendMessage(promptText, attachedImageUri) },
                                        shape = RoundedCornerShape(18.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                        modifier = Modifier.testTag("chef_chat_starter_$idx")
                                    ) {
                                        Text(
                                            text = promptText,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        items(messages, key = { it.id }) { msg ->
                            val isUser = msg.role == "user"
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                            ) {
                                val bubbleColor = when {
                                    msg.isError -> MaterialTheme.colorScheme.errorContainer
                                    isUser -> PostItYellow.copy(alpha = 0.85f)
                                    else -> PostItMint.copy(alpha = 0.7f)
                                }
                                val textColor = when {
                                    msg.isError -> MaterialTheme.colorScheme.onErrorContainer
                                    else -> MaterialTheme.colorScheme.onSurface
                                }

                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = bubbleColor,
                                    modifier = Modifier
                                        .widthIn(max = 310.dp)
                                        .sketchedBorder(
                                            color = if (msg.isError) {
                                                MaterialTheme.colorScheme.error
                                            } else {
                                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                                            },
                                            strokeWidthDp = 1.3f
                                        )
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        if (msg.attachedImageUri.isNotBlank()) {
                                            AsyncImage(
                                                model = ImageUtils.toImageModel(msg.attachedImageUri),
                                                contentDescription = stringResource(R.string.chef_chat_user_photo_badge),
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(140.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                        }

                                        if (!isUser && msg.text.isBlank() && isSending) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                                Text(
                                                    text = stringResource(R.string.chef_chat_thinking),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        } else {
                                            Text(
                                                text = msg.text,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = textColor
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Bottom Input Bar + Photo Attachment Controls
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                        if (cameraError != null) {
                            Text(
                                text = cameraError!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }

                        if (attachedImageUri.isNotBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(6.dp)
                            ) {
                                AsyncImage(
                                    model = ImageUtils.toImageModel(attachedImageUri),
                                    contentDescription = stringResource(R.string.chef_chat_user_photo_badge),
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                )
                                Text(
                                    text = stringResource(R.string.chef_chat_user_photo_badge),
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { attachedImageUri = "" },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = stringResource(R.string.chef_chat_remove_attachment),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(bottom = 6.dp)
                        ) {
                            OutlinedButton(
                                onClick = { triggerCamera() },
                                enabled = !isSending,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("chef_chat_camera_btn")
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.chef_chat_photo_camera), style = MaterialTheme.typography.labelMedium)
                            }
                            OutlinedButton(
                                onClick = {
                                    galleryLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                enabled = !isSending,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("chef_chat_gallery_btn")
                            ) {
                                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.chef_chat_photo_gallery), style = MaterialTheme.typography.labelMedium)
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = inputText,
                                onValueChange = { inputText = it },
                                placeholder = {
                                    Text(
                                        text = stringResource(R.string.chef_chat_input_hint),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                keyboardActions = KeyboardActions(onSend = { submitCurrent() }),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("chef_chat_input")
                            )
                            IconButton(
                                onClick = { submitCurrent() },
                                enabled = !isSending && (inputText.isNotBlank() || attachedImageUri.isNotBlank()),
                                modifier = Modifier.testTag("chef_chat_send")
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Send,
                                        contentDescription = stringResource(R.string.chef_chat_send_cd),
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
