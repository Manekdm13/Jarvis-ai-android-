package com.manekdm13.jarvis

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Build
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.RecognitionListener
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.PasswordVisualTransformation
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class PendingPhoneAction(
    val title: String,
    val explanation: String,
    val requestedCommand: String,
    val intent: Intent
)

class MainActivity : ComponentActivity() {
    private var tts: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var voiceSessionActive = false
    private var voiceTranscript: ((String) -> Unit)? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) = Unit
                    override fun onDone(utteranceId: String?) {
                        mainHandler.post { if (voiceSessionActive) beginVoiceListening() }
                    }
                    override fun onError(utteranceId: String?) {
                        mainHandler.post { if (voiceSessionActive) beginVoiceListening() }
                    }
                })
            }
        }
        setContent {
            JarvisApp(
                onSpeak = { speak(it) },
                onStartVoice = { callback -> startVoiceSession(callback) },
                onStopVoice = { stopVoiceSession() }
            )
        }
    }

    private fun speak(message: String) {
        tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "jarvis_reply")
    }

    private fun startVoiceSession(callback: (String) -> Unit) {
        voiceTranscript = callback
        voiceSessionActive = true
        beginVoiceListening()
    }

    private fun beginVoiceListening() {
        if (!voiceSessionActive) return
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            voiceSessionActive = false
            return
        }
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            voiceSessionActive = false
            Toast.makeText(this, "Speech recognition is unavailable on this device.", Toast.LENGTH_LONG).show()
            return
        }
        if (speechRecognizer == null) {
            speechRecognizer = if (Build.VERSION.SDK_INT >= 31 &&
                SpeechRecognizer.isOnDeviceRecognitionAvailable(this)
            ) SpeechRecognizer.createOnDeviceSpeechRecognizer(this)
            else SpeechRecognizer.createSpeechRecognizer(this)
            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) = Unit
                override fun onBeginningOfSpeech() = Unit
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onEndOfSpeech() = Unit
                override fun onEvent(eventType: Int, params: Bundle?) = Unit
                override fun onPartialResults(partialResults: Bundle?) = Unit
                override fun onResults(results: Bundle?) {
                    val spoken = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                    if (!spoken.isNullOrBlank()) voiceTranscript?.invoke(spoken)
                    else mainHandler.postDelayed({ beginVoiceListening() }, 350)
                }
                override fun onError(error: Int) {
                    if (voiceSessionActive && error != SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                        mainHandler.postDelayed({ beginVoiceListening() }, 700)
                    } else {
                        voiceSessionActive = false
                    }
                }
            })
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        }
        runCatching { speechRecognizer?.startListening(intent) }
            .onFailure { mainHandler.postDelayed({ beginVoiceListening() }, 900) }
    }

    private fun stopVoiceSession() {
        voiceSessionActive = false
        voiceTranscript = null
        runCatching { speechRecognizer?.stopListening() }
        speechRecognizer?.destroy()
        speechRecognizer = null
        tts?.stop()
    }

    override fun onDestroy() {
        stopVoiceSession()
        tts?.stop()
        tts?.shutdown()
        tts = null
        super.onDestroy()
    }
}

@Composable
private fun JarvisApp(
    onSpeak: (String) -> Unit,
    onStartVoice: ((String) -> Unit) -> Unit,
    onStopVoice: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val memory = remember(context) { ConversationMemory(context.applicationContext) }
    val profileMemory = remember(context) { UserProfileMemory(context.applicationContext) }
    var userProfile by remember(profileMemory) { mutableStateOf(profileMemory.load()) }
    var activeUserProfile by remember(profileMemory) { mutableStateOf(profileMemory.loadSaved()) }
    var profileStatus by remember { mutableStateOf("") }
    var input by remember { mutableStateOf("") }
    val settings = remember(context) {
        context.getSharedPreferences("jarvis_settings", Context.MODE_PRIVATE)
    }
    var gatewayUrl by remember {
        mutableStateOf(
            settings.getString("ai_gateway_url", null)?.takeIf { it.isNotBlank() }
                ?: "https://jarvis-ai-gateway.juttfaizan794.workers.dev"
        )
    }
    var gatewayToken by remember { mutableStateOf(SecureTokenStore.load(context)) }
    var gatewayTestStatus by remember { mutableStateOf("") }
    var isThinking by remember { mutableStateOf(false) }
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var updateStatus by remember { mutableStateOf("") }
    var showWebAgentDialog by remember { mutableStateOf(false) }
    var webAgentUrl by remember { mutableStateOf("https://") }
    var webAgentGoal by remember { mutableStateOf("") }
    var webAgentStatus by remember { mutableStateOf("") }
    var isRunningWebAgent by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf("home") }
    var activeSectionId by remember { mutableStateOf<String?>(null) }
    val taskEngine = remember(context) { JarvisTaskEngine(context.applicationContext) }
    var agentTasks by remember(taskEngine) { mutableStateOf(taskEngine.load()) }
    var newTaskTitle by remember { mutableStateOf("") }
    var showRemember by remember { mutableStateOf(false) }
    var rememberSearch by remember { mutableStateOf("") }
    var memoryEntries by remember(memory) { mutableStateOf(memory.load()) }
    val scope = rememberCoroutineScope()
    var pendingAction by remember { mutableStateOf<PendingPhoneAction?>(null) }
    var pendingTapTarget by remember { mutableStateOf<String?>(null) }
    var voiceSessionActive by remember { mutableStateOf(false) }
    var voiceTranscriptHandler by remember { mutableStateOf<(String) -> Unit>({}) }
    var galleryAccessGranted by remember { mutableStateOf(false) }
    var libraryStatus by remember { mutableStateOf("") }
    var messages by remember(memory) {
        val restored = memory.load()
        mutableStateOf(restored.ifEmpty {
            listOf("JARVIS: Online. Phone actions require your approval before they run.")
        })
    }

    fun record(question: String, reply: String) {
        val updated = (messages + "YOU: $question" + "JARVIS: $reply").takeLast(100)
        messages = updated
        memory.save(updated)
        memoryEntries = updated
        onSpeak(reply)
    }

    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    val voiceLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val spoken = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
        if (!spoken.isNullOrBlank()) input = spoken
    }

    val microphonePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            voiceSessionActive = true
            onStartVoice(voiceTranscriptHandler)
        } else {
            record("voice call", "Microphone permission was not granted.")
        }
    }

    val galleryPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        galleryAccessGranted = grants.values.any { it }
        libraryStatus = if (galleryAccessGranted) "Gallery permission granted. Ask JARVIS to scan gallery." else "Gallery permission was not granted."
    }

    val folderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            runCatching { JarvisPersonalLibrary.saveFolder(context, uri) }
                .onSuccess { libraryStatus = "Folder shared with JARVIS. You can now ask it to scan files." }
                .onFailure { libraryStatus = "Couldn't save folder access: ${it.message ?: "unknown error"}" }
        }
    }

    fun requestGalleryAccess() {
        val permissions = when {
            Build.VERSION.SDK_INT >= 34 -> arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
            )
            Build.VERSION.SDK_INT >= 33 -> arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO
            )
            else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        galleryPermissionLauncher.launch(permissions)
    }

    fun sendMessage() {
        val question = input.trim()
        if (question.isEmpty()) return
        input = ""
        val normalizedCommand = question.lowercase(Locale.ROOT)
        when {
            normalizedCommand in setOf("scan gallery", "search gallery", "index gallery") -> {
                val granted = if (Build.VERSION.SDK_INT >= 34) {
                    context.checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED ||
                        context.checkSelfPermission(Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED ||
                        context.checkSelfPermission(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) == PackageManager.PERMISSION_GRANTED
                } else if (Build.VERSION.SDK_INT >= 33) {
                    context.checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED ||
                        context.checkSelfPermission(Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
                } else {
                    context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                }
                if (granted) record(question, JarvisPersonalLibrary.scanGallery(context))
                else {
                    requestGalleryAccess()
                    record(question, "Android needs your permission to index gallery images and videos. Review the permission prompt; you can grant selected media only.")
                }
                return
            }
            normalizedCommand in setOf("scan files", "index files", "search files") -> {
                record(question, JarvisPersonalLibrary.scanSavedFolder(context))
                return
            }
            normalizedCommand.startsWith("read file ") -> {
                record(question, JarvisPersonalLibrary.readTextFile(context, question.substringAfter("read file ").trim()))
                return
            }
            normalizedCommand.startsWith("open file ") -> {
                record(question, JarvisPersonalLibrary.openFile(context, question.substringAfter("open file ").trim()))
                return
            }
            normalizedCommand in setOf("read screen", "what is on my screen", "what's on my screen", "inspect screen") -> {
                record(question, JarvisAccessibilityService.readScreen())
                return
            }
            normalizedCommand in setOf("check notifications", "read notifications", "show notifications", "summarize notifications") -> {
                record(question, JarvisNotificationListener.digest())
                return
            }
            normalizedCommand == "scroll down" -> {
                record(question, if (JarvisAccessibilityService.scroll(true)) "Scrolled the current screen down." else "I couldn't scroll. Enable JARVIS Accessibility and open a scrollable screen.")
                return
            }
            normalizedCommand == "scroll up" -> {
                record(question, if (JarvisAccessibilityService.scroll(false)) "Scrolled the current screen up." else "I couldn't scroll. Enable JARVIS Accessibility and open a scrollable screen.")
                return
            }
            normalizedCommand in setOf("go back", "press back") -> {
                record(question, if (JarvisAccessibilityService.globalBack()) "Went back." else "Accessibility access is not enabled.")
                return
            }
            normalizedCommand in setOf("go home", "press home") -> {
                record(question, if (JarvisAccessibilityService.globalHome()) "Returned to the home screen." else "Accessibility access is not enabled.")
                return
            }
            normalizedCommand.startsWith("tap ") -> {
                pendingTapTarget = question.substringAfter("tap ", "").trim().takeIf { it.isNotBlank() }
                record(question, "Review the requested screen tap in the approval prompt. JARVIS will only tap the matching visible control if you approve.")
                return
            }
        }
        val prepared = resolvePhoneAction(context, question)
        when {
            prepared?.first != null -> {
                pendingAction = prepared.first
                record(question, "I have prepared that phone action. Please review and approve it before I proceed.")
            }
            prepared != null -> record(question, prepared.second)
            else -> {
                if (gatewayUrl.isBlank()) {
                    record(question, "Online AI is not configured yet. Paste your Cloudflare Worker URL into the AI gateway URL field above.")
                } else {
                    isThinking = true
                    val history = buildString {
                        if (activeUserProfile.isNotBlank()) {
                            append("USER PROFILE (saved by the user):\n")
                            append(activeUserProfile.trim())
                            append("\n\n")
                        }
                        activeSectionId?.let { sectionId -> JarvisSections.find(sectionId)?.let { section -> append("ACTIVE SPECIALIST SECTION: " + section.name + ". " + section.description + " Use available tools, shared context, and state limitations honestly.\n\n") } }
                        append("RECENT CONVERSATION:\n")
                        append(messages.takeLast(20).joinToString("\n"))
                    }
                    scope.launch {
                        val answer = withContext(Dispatchers.IO) {
                            runCatching { AiGatewayClient.reply(gatewayUrl, gatewayToken, question, history) }
                        }
                        isThinking = false
                        record(question, answer.getOrElse { error ->
                            "I couldn't reach the AI gateway: ${error.message ?: "unknown network error"}"
                        })
                    }
                }
            }
        }
    }

    voiceTranscriptHandler = { spoken -> selectedTab = "chat"; input = spoken; sendMessage() }

    val jarvisColors = darkColorScheme(
        primary = Color(0xFF65E6F2),
        onPrimary = Color(0xFF04151B),
        secondary = Color(0xFF9AABFF),
        background = Color(0xFF070B16),
        surface = Color(0xFF101827),
        surfaceVariant = Color(0xFF18263A),
        onSurface = Color(0xFFEAF4FF),
        onSurfaceVariant = Color(0xFF9DB0C7)
    )

    MaterialTheme(colorScheme = jarvisColors) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF0B1728), Color(0xFF070B16), Color(0xFF090F1D))
                    )
                )
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 14.dp).then(if (selectedTab == "home" || selectedTab == "settings" || selectedTab == "library" || selectedTab == "modules" || selectedTab == "agent") Modifier.verticalScroll(rememberScrollState()) else Modifier),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            "J.A.R.V.I.S",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color(0xFFEAF7FF)
                        )
                        Text(
                            "PERSONAL INTELLIGENCE SYSTEM",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF80A5C4)
                        )
                    }
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF123A3B),
                        contentColor = Color(0xFF78F0D0)
                    ) {
                        Text(
                            "  ● ONLINE  ",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }

                if (selectedTab == "home") {
                    Card(
                        shape = RoundedCornerShape(30.dp),
                        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = Color(0xFF101D30)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("YOUR PERSONAL AI", color = Color(0xFF8EA9C4), style = MaterialTheme.typography.labelSmall)
                                    Text("What can I do for you?", color = Color(0xFFF2F7FF), style = MaterialTheme.typography.headlineSmall)
                                }
                                Surface(shape = CircleShape, color = if (isThinking) Color(0xFF493719) else Color(0xFF123D39)) {
                                    Text(if (isThinking) "WORKING" else "READY", Modifier.padding(horizontal = 12.dp, vertical = 8.dp), color = if (isThinking) Color(0xFFFFD28B) else Color(0xFF7DE6C1), style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            JarvisCore(isThinking = isThinking)
                            Text("One assistant for conversation, files, research and phone actions.", color = Color(0xFF9DB0C7), style = MaterialTheme.typography.bodyMedium)
                            Button(onClick = { selectedTab = "chat" }, Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(18.dp)) {
                                Text("Open conversation  →", style = MaterialTheme.typography.titleMedium)
                            }
                            OutlinedButton(
                                onClick = {
                                    selectedTab = "chat"
                                    if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                        voiceSessionActive = true; onStartVoice(voiceTranscriptHandler)
                                    } else microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                },
                                Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(18.dp)
                            ) { Text(if (voiceSessionActive) "Voice session active" else "Start voice call  •  🎙") }
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Card(Modifier.weight(1f), shape = RoundedCornerShape(22.dp), colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = Color(0xFF15263B))) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text("PERSONAL LIBRARY", color = Color(0xFF78DDE8), style = MaterialTheme.typography.labelSmall)
                                Text("Files & gallery", color = Color(0xFFF0F5FC), style = MaterialTheme.typography.titleMedium)
                                Text("Find your content", color = Color(0xFF9DB0C7), style = MaterialTheme.typography.bodySmall)
                                TextButton(onClick = { selectedTab = "library" }) { Text("Open →") }
                            }
                        }
                        Card(Modifier.weight(1f), shape = RoundedCornerShape(22.dp), colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = Color(0xFF15263B))) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text("WORKSPACE", color = Color(0xFFB4A8FF), style = MaterialTheme.typography.labelSmall)
                                Text("Activity & memory", color = Color(0xFFF0F5FC), style = MaterialTheme.typography.titleMedium)
                                Text("Your recent conversations", color = Color(0xFF9DB0C7), style = MaterialTheme.typography.bodySmall)
                                TextButton(onClick = { selectedTab = "tasks"; showRemember = true; memoryEntries = memory.load() }) { Text("View →") }
                            }
                        }
                    }
                    Card(shape = RoundedCornerShape(22.dp), colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = Color(0xFF111F32)), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("QUICK ACTIONS", color = Color(0xFF78DDE8), style = MaterialTheme.typography.labelMedium)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { selectedTab = "chat"; input = "Scan gallery" }, Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) { Text("Scan gallery") }
                                OutlinedButton(onClick = { selectedTab = "chat"; input = "Check notifications" }, Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) { Text("Notifications") }
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { selectedTab = "chat"; input = "Read screen" }, Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) { Text("Read screen") }
                                OutlinedButton(onClick = { selectedTab = "chat"; input = "Scan files" }, Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) { Text("Scan files") }
                            }
                        }
                    }
                }
                if (selectedTab == "agent") {
                    Text("AUTONOMOUS WORKSPACE", color = Color(0xFF78DDE8), style = MaterialTheme.typography.titleMedium)
                    Text("Durable task tracking is active. Tasks survive app restarts; execution and external actions still require connected tools and your approval where appropriate.", color = Color(0xFF9DB0C7), style = MaterialTheme.typography.bodySmall)
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = Color(0xFF111F32))) {
                        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(value = newTaskTitle, onValueChange = { newTaskTitle = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Add a task for JARVIS to track") }, placeholder = { Text("e.g. Prepare my scholarship shortlist") }, minLines = 2, maxLines = 3)
                            Button(enabled = newTaskTitle.isNotBlank(), onClick = {
                                runCatching { taskEngine.create(newTaskTitle) }.onSuccess {
                                    newTaskTitle = ""
                                    agentTasks = taskEngine.load()
                                }
                            }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) { Text("Save task") }
                        }
                    }
                    if (agentTasks.isEmpty()) {
                        Text("No tracked tasks yet. Add one above; JARVIS will keep its status on this device.", color = Color(0xFF9DB0C7), style = MaterialTheme.typography.bodyMedium)
                    } else {
                        agentTasks.sortedByDescending { it.updatedAt }.forEach { task ->
                            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = Color(0xFF111F32))) {
                                Column(Modifier.fillMaxWidth().padding(13.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(task.title, color = Color(0xFFEAF4FF), style = MaterialTheme.typography.titleSmall)
                                    Text(task.status.name.replace('_', ' '), color = if (task.status == JarvisTaskStatus.COMPLETED) Color(0xFF78E0C2) else Color(0xFFFFD28B), style = MaterialTheme.typography.labelSmall)
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        if (task.status != JarvisTaskStatus.IN_PROGRESS && task.status != JarvisTaskStatus.COMPLETED) TextButton(onClick = { taskEngine.updateStatus(task.id, JarvisTaskStatus.IN_PROGRESS); agentTasks = taskEngine.load() }) { Text("Start") }
                                        if (task.status != JarvisTaskStatus.WAITING_FOR_APPROVAL && task.status != JarvisTaskStatus.COMPLETED) TextButton(onClick = { taskEngine.updateStatus(task.id, JarvisTaskStatus.WAITING_FOR_APPROVAL); agentTasks = taskEngine.load() }) { Text("Needs approval") }
                                        if (task.status != JarvisTaskStatus.COMPLETED) TextButton(onClick = { taskEngine.updateStatus(task.id, JarvisTaskStatus.COMPLETED); agentTasks = taskEngine.load() }) { Text("Complete") }
                                        TextButton(onClick = { taskEngine.delete(task.id); agentTasks = taskEngine.load() }) { Text("Delete") }
                                    }
                                }
                            }
                        }
                    }
                }

                if (selectedTab == "modules") { Text("JARVIS 3.0 • 21 SYSTEM SECTIONS", color = Color(0xFF78DDE8), style = MaterialTheme.typography.titleMedium); Text("One central AI brain. Shared memory. Permission-gated actions. Planned sections are not yet fully connected.", color = Color(0xFF9DB0C7), style = MaterialTheme.typography.bodySmall); JarvisSections.all.forEach { section -> Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = Color(0xFF111F32))) { Row(modifier = Modifier.fillMaxWidth().padding(13.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) { Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) { Text(section.name, color = Color(0xFFEAF4FF), style = MaterialTheme.typography.titleSmall); Text(section.description, color = Color(0xFF9DB0C7), style = MaterialTheme.typography.bodySmall); Text(if (section.status == SectionStatus.FOUNDATION) "FOUNDATION AVAILABLE" else "PLANNED • CENTRAL BRAIN ROUTING", color = if (section.status == SectionStatus.FOUNDATION) Color(0xFF78E0C2) else Color(0xFFFFD28B), style = MaterialTheme.typography.labelSmall) }; TextButton(onClick = { activeSectionId = section.id; selectedTab = "chat"; input = "Open the " + section.name + " section. " + section.description + " Tell me what you can do now and what still needs implementation." }) { Text("Open") } } } } }

                Surface(shape = RoundedCornerShape(20.dp), color = Color(0xFF101D2F), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 3.dp, vertical = 4.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        listOf("home" to "Home", "chat" to "Chat", "modules" to "21 Sections", "agent" to "Agent", "library" to "Library", "tasks" to "Memory", "settings" to "Settings").forEach { (tab, label) ->
                            TextButton(onClick = {
                                selectedTab = tab
                                showSettings = tab == "settings"
                                showRemember = tab == "tasks"
                                if (tab == "tasks") memoryEntries = memory.load()
                                if (tab == "agent") agentTasks = taskEngine.load()
                            }) {
                                Text(label, color = if (selectedTab == tab) Color(0xFF77E5EE) else Color(0xFF8FA5BF), style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }

                if (showSettings) {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = Color(0xFF101C2D))
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = gatewayUrl,
                                onValueChange = {
                                    gatewayUrl = it
                                    settings.edit().putString("ai_gateway_url", it.trim()).apply()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("AI gateway URL") },
                                placeholder = { Text("https://your-worker.workers.dev") },
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = gatewayToken,
                                onValueChange = { value -> gatewayToken = value; SecureTokenStore.save(context, value) },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Private gateway token") },
                                placeholder = { Text("Your JARVIS access token") },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation()
                            )
                            Button(
                                onClick = {
                                    gatewayTestStatus = "Testing connection from the JARVIS app…"
                                    scope.launch {
                                        val result = withContext(Dispatchers.IO) {
                                            runCatching { AiGatewayClient.checkHealth(gatewayUrl, gatewayToken) }
                                        }
                                        gatewayTestStatus = result.fold(
                                            onSuccess = { it },
                                            onFailure = { error ->
                                                "App-side connection failed: ${error.javaClass.simpleName}: ${error.message ?: "no details"}"
                                            }
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp)
                            ) { Text("Test gateway connection") }
                            if (gatewayTestStatus.isNotBlank()) {
                                Text(
                                    gatewayTestStatus,
                                    color = Color(0xFF9FC7D8),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            androidx.compose.material3.HorizontalDivider(color = Color(0xFF29435C))
                            Text("DEVICE ACCESS", color = Color(0xFF70DCEB), style = MaterialTheme.typography.labelMedium)
                            Text(
                                "These are real Android services, not simulated toggles. Enable each service in the system screen. Screen text and notification contents are read only when you explicitly ask JARVIS to inspect them.",
                                color = Color(0xFF9DB5CD),
                                style = MaterialTheme.typography.bodySmall
                            )
                            OutlinedButton(
                                onClick = { runCatching { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) } },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) { Text(if (JarvisAccessibilityService.isRunning()) "Accessibility service connected ✓" else "Enable screen control & reading") }
                            OutlinedButton(
                                onClick = { runCatching { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) } },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) { Text("Enable notification access") }
                            Text(
                                "After enabling, return to JARVIS and try: “read screen”, “check notifications”, “scroll down”, “go back”, or “tap Search”. Taps always require a separate approval.",
                                color = Color(0xFF8EAAC6),
                                style = MaterialTheme.typography.bodySmall
                            )

                            androidx.compose.material3.HorizontalDivider(color = Color(0xFF29435C))
                            Text("PERSONAL FILES & GALLERY", color = Color(0xFF70DCEB), style = MaterialTheme.typography.labelMedium)
                            Text(
                                "Gallery indexing reads file names, sizes and dates only. Shared-folder access lets JARVIS list files and read supported plain-text documents. You choose the folder once; Android still protects private app data.",
                                color = Color(0xFF9DB5CD),
                                style = MaterialTheme.typography.bodySmall
                            )
                            OutlinedButton(
                                onClick = { requestGalleryAccess() },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) { Text("Grant gallery access") }
                            OutlinedButton(
                                onClick = { folderPicker.launch(null) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) { Text(if (JarvisPersonalLibrary.savedFolder(context) != null) "Change shared folder" else "Choose a folder for JARVIS") }
                            if (libraryStatus.isNotBlank()) {
                                Text(libraryStatus, color = Color(0xFF9FC7D8), style = MaterialTheme.typography.bodySmall)
                            }
                            Text(
                                "Commands: “scan gallery”, “scan files”, “read file report.txt”. PDF and Office document extraction, photo OCR and continuous voice calling are still under development.",
                                color = Color(0xFF8EAAC6),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                }

                if (updateStatus.isNotBlank()) {
                    Text(updateStatus, color = Color(0xFF9FC7D8), style = MaterialTheme.typography.bodySmall)
                }

                if (selectedTab == "tasks" || showRemember) {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = Color(0xCC101B2B)),
                        modifier = Modifier.weight(1f).fillMaxWidth()
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                                    Text("PERSONAL PROFILE", color = Color(0xFF70DCEB), style = MaterialTheme.typography.labelMedium)
                                    Text("Review and edit what JARVIS remembers about you. Nothing is sent to online AI until you save. The saved profile is encrypted on this device and included with online chat requests.", color = Color(0xFF9DB5CD), style = MaterialTheme.typography.bodySmall)
                                    OutlinedTextField(
                                        value = userProfile,
                                        onValueChange = { userProfile = it; profileStatus = "" },
                                        modifier = Modifier.fillMaxWidth(),
                                        label = { Text("Your information and goals") },
                                        minLines = 5,
                                        maxLines = 10
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(onClick = {
                                            val profileToSave = userProfile.trim()
                                            runCatching { profileMemory.save(profileToSave) }
                                                .onSuccess {
                                                    userProfile = profileToSave
                                                    activeUserProfile = profileToSave
                                                    profileStatus = "Personal profile saved securely and enabled for online chat."
                                                }
                                                .onFailure { error -> profileStatus = error.message ?: "Could not save profile." }
                                        }, shape = RoundedCornerShape(12.dp)) { Text("Save profile") }
                                        OutlinedButton(onClick = {
                                            profileMemory.clear()
                                            userProfile = ""
                                            activeUserProfile = ""
                                            profileStatus = "Saved profile cleared from this device and removed from online context."
                                        }, shape = RoundedCornerShape(12.dp)) { Text("Clear") }
                                    }
                                    if (profileStatus.isNotBlank()) Text(profileStatus, color = Color(0xFF9FC7D8), style = MaterialTheme.typography.bodySmall)
                                    Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFF142A3B), modifier = Modifier.fillMaxWidth()) {
                                        Text(if (activeUserProfile.isBlank()) "Profile status: not saved" else "Profile status: saved and active for online chat", modifier = Modifier.padding(12.dp), color = Color(0xFF9DB5CD), style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("CONVERSATION MEMORY", color = Color(0xFF70DCEB), style = MaterialTheme.typography.labelMedium)
                                    Text("${memoryEntries.size} encrypted messages stored on this device", color = Color(0xFF9DB5CD), style = MaterialTheme.typography.bodySmall)
                                    OutlinedTextField(value = rememberSearch, onValueChange = { rememberSearch = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text("Search remembered conversations…") }, label = { Text("Search memory") })
                                }
                            }
                            val filteredMemory = memoryEntries.asReversed().filter { it.contains(rememberSearch.trim(), ignoreCase = true) }
                            if (filteredMemory.isEmpty()) {
                                item { Text(if (rememberSearch.isBlank()) "No saved conversations yet. Your chats will appear here." else "No memories match your search.", color = Color(0xFF9DB5CD), style = MaterialTheme.typography.bodyMedium) }
                            } else {
                                items(filteredMemory) { entry ->
                                    val isJarvisEntry = entry.startsWith("JARVIS:")
                                    Surface(shape = RoundedCornerShape(16.dp), color = if (isJarvisEntry) Color(0xFF172B40) else Color(0xFF123344), modifier = Modifier.fillMaxWidth()) {
                                        Column(modifier = Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                            Text(if (isJarvisEntry) "JARVIS RESPONSE" else "YOUR MESSAGE", color = if (isJarvisEntry) Color(0xFF70DCEB) else Color(0xFF9DBDFF), style = MaterialTheme.typography.labelSmall)
                                            Text(entry.substringAfter(":"), color = Color(0xFFEAF4FF), style = MaterialTheme.typography.bodyMedium)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else if (selectedTab == "library") {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = Color(0xFF111F32)),
                        modifier = Modifier.weight(1f).fillMaxWidth()
                    ) {
                        Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Text("PERSONAL LIBRARY", color = Color(0xFF78DDE8), style = MaterialTheme.typography.labelMedium)
                            Text("Your files, within your control.", color = Color(0xFFF0F5FC), style = MaterialTheme.typography.headlineSmall)
                            Text("Connect your gallery or share a folder once. JARVIS can index available gallery metadata, list shared files, read supported text files, and open matching files.", color = Color(0xFF9DB0C7), style = MaterialTheme.typography.bodyMedium)
                            Button(onClick = { requestGalleryAccess(); libraryStatus = "Review Android's media permission prompt." }, Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) { Text("Connect gallery") }
                            OutlinedButton(onClick = { folderPicker.launch(null) }, Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) { Text(if (JarvisPersonalLibrary.savedFolder(context) != null) "Change shared folder" else "Choose folder") }
                            OutlinedButton(onClick = { input = "Scan gallery"; selectedTab = "chat" }, Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) { Text("Index gallery") }
                            OutlinedButton(onClick = { input = "Scan files"; selectedTab = "chat" }, Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) { Text("Index shared files") }
                            if (libraryStatus.isNotBlank()) Text(libraryStatus, color = Color(0xFF9FC7D8), style = MaterialTheme.typography.bodySmall)
                            Text("Private app data remains protected by Android. JARVIS only reads media and folders you authorize.", color = Color(0xFF8EAAC6), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                } else if (selectedTab == "chat") {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = Color(0xCC101B2B)),
                    modifier = Modifier.weight(1f).fillMaxWidth()
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize().padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(messages) { line ->
                            val isJarvis = line.startsWith("JARVIS:")
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = if (isJarvis) Arrangement.Start else Arrangement.End
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(
                                        topStart = 18.dp,
                                        topEnd = 18.dp,
                                        bottomEnd = if (isJarvis) 18.dp else 5.dp,
                                        bottomStart = if (isJarvis) 5.dp else 18.dp
                                    ),
                                    color = if (isJarvis) Color(0xFF1A2B40) else Color(0xFF123D50),
                                    modifier = Modifier.fillMaxWidth(0.92f)
                                ) {
                                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp)) {
                                        Text(
                                            if (isJarvis) "JARVIS" else "YOU",
                                            color = if (isJarvis) Color(0xFF70DCEB) else Color(0xFF9DBDFF),
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            if (isJarvis) line.removePrefix("JARVIS:").trim()
                                            else line.removePrefix("YOU:").trim(),
                                            color = Color(0xFFEAF4FF),
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        if (isJarvis) {
                                            TextButton(
                                                onClick = {
                                                    clipboardManager.setText(
                                                        AnnotatedString(line.removePrefix("JARVIS:").trim())
                                                    )
                                                    Toast.makeText(context, "JARVIS response copied", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.align(androidx.compose.ui.Alignment.End)
                                            ) {
                                                Text("Copy reply")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if (isThinking) {
                            item {
                                Text(
                                    "JARVIS is thinking…",
                                    color = Color(0xFF70DCEB),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
                }

                if (selectedTab == "chat") Card(
                    shape = RoundedCornerShape(26.dp),
                    colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = Color(0xFF111E30))
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (voiceSessionActive) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color(0xFF123C43),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Text("LIVE VOICE SESSION", color = Color(0xFF8CF3E7), style = MaterialTheme.typography.labelMedium)
                                        Text("JARVIS is listening for your next instruction", color = Color(0xFFE7F7F5), style = MaterialTheme.typography.bodySmall)
                                    }
                                    TextButton(onClick = { onStopVoice(); voiceSessionActive = false }) { Text("End call") }
                                }
                            }
                        }
                        OutlinedTextField(
                            value = input,
                            onValueChange = { input = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Message your assistant…") },
                            maxLines = 3
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = {
                                    if (voiceSessionActive) {
                                        onStopVoice()
                                        voiceSessionActive = false
                                    } else if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                        voiceSessionActive = true
                                        onStartVoice(voiceTranscriptHandler)
                                    } else {
                                        microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp)
                            ) { Text(if (voiceSessionActive) "End voice call" else "🎙  Start voice call") }
                            Button(
                                onClick = { sendMessage() },
                                modifier = Modifier.weight(1f),
                                enabled = !isThinking,
                                shape = RoundedCornerShape(14.dp)
                            ) { Text(if (isThinking) "Thinking…" else "Send  ➤") }
                        }
                        OutlinedButton(
                            onClick = { webAgentStatus = ""; showWebAgentDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isThinking && !isRunningWebAgent,
                            shape = RoundedCornerShape(14.dp)
                        ) { Text("🌐  TinyFish Web Agent") }
                    }
                }
            }
        }
    }

    if (showWebAgentDialog) {
        AlertDialog(
            onDismissRequest = { if (!isRunningWebAgent) showWebAgentDialog = false },
            title = { Text("TinyFish Web Agent") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Give JARVIS a website and a specific browser task. Review the task before running it; some browser actions may submit forms or change website data.")
                    OutlinedTextField(
                        value = webAgentUrl,
                        onValueChange = { webAgentUrl = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Starting website URL") },
                        placeholder = { Text("https://example.com") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = webAgentGoal,
                        onValueChange = { webAgentGoal = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("What should TinyFish do?") },
                        placeholder = { Text("Find the public pricing plans and summarize them. Do not purchase anything.") },
                        minLines = 3,
                        maxLines = 5
                    )
                    if (webAgentStatus.isNotBlank()) {
                        Text(webAgentStatus, color = Color(0xFF9FC7D8), style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = !isRunningWebAgent && webAgentUrl.startsWith("http") && webAgentGoal.isNotBlank(),
                    onClick = {
                        isRunningWebAgent = true
                        webAgentStatus = "Connecting to TinyFish…"
                        scope.launch {
                            val result = withContext(Dispatchers.IO) {
                                runCatching {
                                    AiGatewayClient.runWebAgent(gatewayUrl, gatewayToken, webAgentUrl, webAgentGoal)
                                }
                            }
                            isRunningWebAgent = false
                            result.fold(
                                onSuccess = { output ->
                                    showWebAgentDialog = false
                                    record("TinyFish browser task: ${webAgentGoal.trim()}", output)
                                },
                                onFailure = { error ->
                                    webAgentStatus = "Task failed: ${error.message ?: "Unknown error"}"
                                }
                            )
                        }
                    }
                ) { Text(if (isRunningWebAgent) "Running…" else "Run task") }
            },
            dismissButton = {
                OutlinedButton(
                    enabled = !isRunningWebAgent,
                    onClick = { showWebAgentDialog = false }
                ) { Text("Cancel") }
            }
        )
    }

    pendingTapTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingTapTarget = null },
            title = { Text("Approve screen interaction") },
            text = {
                Text("JARVIS will look for a visible control containing “$target” and tap it. Review the current screen first. Do not approve if this could send a message, publish content, delete data, make a purchase, or change security settings.")
            },
            confirmButton = {
                Button(onClick = {
                    pendingTapTarget = null
                    val success = JarvisAccessibilityService.tapText(target)
                    record("tap $target", if (success) "Tapped the matching visible control." else "I couldn't find a matching clickable control. Check that Accessibility is enabled and the target is visible.")
                }) { Text("Approve this tap") }
            },
            dismissButton = {
                OutlinedButton(onClick = { pendingTapTarget = null; record("tap $target", "Denied. No screen interaction was performed.") }) { Text("Deny") }
            }
        )
    }

    pendingAction?.let { action ->
        AlertDialog(
            onDismissRequest = { pendingAction = null },
            title = { Text("JARVIS needs your approval") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(action.title, style = MaterialTheme.typography.titleMedium)
                    Text(action.explanation)
                    Text("Command: ${action.requestedCommand}")
                    Text("This approval applies to this action only.")
                }
            },
            confirmButton = {
                Button(onClick = {
                    pendingAction = null
                    try {
                        context.startActivity(
                            action.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                        record(action.requestedCommand, "Approved. I opened the requested Android screen or app.")
                    } catch (_: Exception) {
                        record(action.requestedCommand, "Android could not open that action on this device.")
                    }
                }) { Text("Approve once") }
            },
            dismissButton = {
                OutlinedButton(onClick = {
                    pendingAction = null
                    record(action.requestedCommand, "Denied. I cancelled that action and did not run it.")
                }) { Text("Deny") }
            }
        )
    }
}

@Composable
private fun JarvisCore(isThinking: Boolean) {
    val transition = rememberInfiniteTransition(label = "jarvis-core")
    val pulse by transition.animateFloat(
        initialValue = 0.94f,
        targetValue = if (isThinking) 1.09f else 1.035f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isThinking) 650 else 1800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "core-pulse"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(132.dp)
                .scale(pulse)
                .background(
                    Brush.radialGradient(
                        listOf(
                            Color(0x553FEAF4),
                            Color(0x223B9FCB),
                            Color.Transparent
                        )
                    ),
                    CircleShape
                )
                .padding(8.dp)
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFF1C536A), Color(0xFF0B1A2D), Color(0xFF07101E))
                    ),
                    CircleShape
                ),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(5.dp),
                shape = CircleShape,
                color = Color.Transparent,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    Brush.sweepGradient(
                        listOf(
                            Color(0xFF8AF8FF),
                            Color(0xFF327AAB),
                            Color(0xFF9AABFF),
                            Color(0xFF8AF8FF)
                        )
                    )
                )
            ) {
                Box(contentAlignment = androidx.compose.ui.Alignment.Center) {
                    Surface(
                        modifier = Modifier.size(78.dp),
                        shape = CircleShape,
                        color = Color(0xFF102C42),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF65E6F2))
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                "J",
                                color = Color(0xFFB8FAFF),
                                style = MaterialTheme.typography.headlineLarge
                            )
                            Text(
                                "CORE",
                                color = Color(0xFF65E6F2),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        }
        Text(
            if (isThinking) "PROCESSING REQUEST" else "NEURAL SYSTEM STANDBY",
            color = if (isThinking) Color(0xFF9AABFF) else Color(0xFF65C9D8),
            style = MaterialTheme.typography.labelSmall
        )
        Text(
            "SECURE  •  PERSONAL  •  INTELLIGENT",
            color = Color(0xFF66839F),
            style = MaterialTheme.typography.labelSmall
        )
    }
}

/** Prepares an action only. It never launches an Intent. */
private fun resolvePhoneAction(
    context: Context,
    rawCommand: String
): Pair<PendingPhoneAction?, String>? {
    val command = rawCommand.trim().lowercase(Locale.ROOT)
    val launchers = mapOf(
        "youtube" to "com.google.android.youtube",
        "whatsapp" to "com.whatsapp",
        "chrome" to "com.android.chrome",
        "google maps" to "com.google.android.apps.maps",
        "maps" to "com.google.android.apps.maps",
        "gmail" to "com.google.android.gm"
    )

    fun approval(title: String, explanation: String, intent: Intent) =
        PendingPhoneAction(title, explanation, rawCommand, intent) to ""

    if (command == "time" || command.contains("what time")) {
        return null to "It's ${SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())}."
    }
    if (command == "date" || command.contains("today's date") || command.contains("what is the date")) {
        return null to "Today is ${SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(Date())}."
    }
    if (command == "settings" || command == "open settings") {
        return approval("Open Android Settings", "JARVIS will open the system Settings screen.", Intent(Settings.ACTION_SETTINGS))
    }
    if (command.contains("wifi") || command.contains("wi-fi")) {
        return approval("Open Wi-Fi settings", "You remain in control of network changes.", Intent(Settings.ACTION_WIFI_SETTINGS))
    }
    if (command.contains("bluetooth")) {
        return approval("Open Bluetooth settings", "You remain in control of pairing and changes.", Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
    }
    if (command == "camera" || command == "open camera") {
        return approval("Open camera", "JARVIS will open the camera. It will not take a photo automatically.", Intent(MediaStore.ACTION_IMAGE_CAPTURE))
    }
    if (command == "call" || command.contains("open phone") || command.contains("dialer")) {
        return approval("Open phone dialer", "JARVIS will open the dialer, not place a call.", Intent(Intent.ACTION_DIAL))
    }
    if (command.startsWith("set alarm") || command.startsWith("alarm")) {
        return approval("Open alarms", "Review and confirm the alarm in the Android alarm screen.", Intent(AlarmClock.ACTION_SHOW_ALARMS))
    }
    if (command.startsWith("search ") || command.startsWith("google ")) {
        val query = rawCommand.substringAfter(' ').trim()
        return approval(
            "Search the web",
            "Open a browser search for: $query",
            Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=" + Uri.encode(query)))
        )
    }
    if (command.startsWith("navigate to ") || command.startsWith("map ")) {
        val destination = rawCommand.substringAfter(" to ", rawCommand.substringAfter("map ", "")).trim()
        return approval(
            "Open Maps",
            "Open Maps for: $destination",
            Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(destination)))
        )
    }
    if (command.startsWith("open ")) {
        val requested = command.removePrefix("open ").trim()
        val packageName = launchers[requested]
        if (packageName != null) {
            val appIntent = context.packageManager.getLaunchIntentForPackage(packageName)
                ?: return null to "I couldn't find $requested installed on this device."
            return approval(
                "Open $requested",
                "Launch $requested. No other app data will be accessed.",
                appIntent
            )
        }
    }
    return null
}
