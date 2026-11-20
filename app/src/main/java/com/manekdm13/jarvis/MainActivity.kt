package com.manekdm13.jarvis

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) tts?.language = Locale.US
        }
        setContent { JarvisApp(onSpeak = { speak(it) }) }
    }

    private fun speak(message: String) {
        tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "jarvis_reply")
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        super.onDestroy()
    }
}

@Composable
private fun JarvisApp(onSpeak: (String) -> Unit) {
    val context = LocalContext.current
    val memory = remember(context) { ConversationMemory(context.applicationContext) }
    var input by remember { mutableStateOf("") }
    val settings = remember(context) {
        context.getSharedPreferences("jarvis_settings", Context.MODE_PRIVATE)
    }
    var gatewayUrl by remember { mutableStateOf(settings.getString("ai_gateway_url", "") ?: "") }
    var gatewayToken by remember { mutableStateOf(SecureTokenStore.load(context)) }
    var isThinking by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var pendingAction by remember { mutableStateOf<PendingPhoneAction?>(null) }
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

    fun sendMessage() {
        val question = input.trim()
        if (question.isEmpty()) return
        input = ""
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
                    val history = messages.takeLast(20).joinToString("\n")
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

    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("J.A.R.V.I.S", style = MaterialTheme.typography.headlineLarge)
                Text("Personal Android assistant • You control every phone action")
                OutlinedTextField(
                    value = gatewayUrl,
                    onValueChange = {
                        gatewayUrl = it
                        settings.edit().putString("ai_gateway_url", it.trim()).apply()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("AI gateway URL (Cloudflare Worker)") },
                    placeholder = { Text("https://your-worker.workers.dev") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = gatewayToken,
                    onValueChange = { value -> gatewayToken = value; SecureTokenStore.save(context, value) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Private gateway access token") },
                    placeholder = { Text("Paste your JARVIS token here") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation()
                )
                if (isThinking) Text("JARVIS is thinking…", color = MaterialTheme.colorScheme.primary)
                Card(
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.weight(1f).fillMaxWidth()
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize().padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(messages) { line ->
                            Text(line, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Message JARVIS") },
                    maxLines = 3
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(
                                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                                )
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to JARVIS")
                            }
                            runCatching { voiceLauncher.launch(intent) }
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Speak") }
                    Button(onClick = { sendMessage() }, modifier = Modifier.weight(1f), enabled = !isThinking) {
                        Text(if (isThinking) "Thinking…" else "Send")
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
            }
        }
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
