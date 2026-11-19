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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class PendingPhoneAction(val title: String, val explanation: String, val intent: Intent)

class MainActivity : ComponentActivity() {
    private var tts: TextToSpeech? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this) { status -> if (status == TextToSpeech.SUCCESS) tts?.language = Locale.US }
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
    var pendingAction by remember { mutableStateOf<PendingPhoneAction?>(null) }
    var messages by remember(memory) {
        val restored = memory.load()
        mutableStateOf(restored.ifEmpty { listOf("JARVIS: Online. Phone actions require your approval before they run.") })
    }

    fun saveReply(reply: String) {
        val updated = (messages + "JARVIS: $reply").takeLast(100)
        messages = updated
        memory.save(updated)
        onSpeak(reply)
    }
    fun addExchange(question: String, reply: String) {
        val updated = (messages + "YOU: $question" + "JARVIS: $reply").takeLast(100)
        messages = updated
        memory.save(updated)
        onSpeak(reply)
    }

    val voiceLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (!spoken.isNullOrBlank()) input = spoken
    }

    fun sendMessage() {
        val question = input.trim()
        if (question.isEmpty()) return
        input = ""
        val resolved = resolvePhoneAction(context, question)
        when {
            resolved == null -> addExchange(question, "I received your request. That needs the online AI brain, which is not connected yet.")
            resolved.first == null -> addExchange(question, resolved.second)
            else -> {
                pendingAction = resolved.first
                addExchange(question, "I can prepare this action, but I will not run it until you approve it below.")
            }
        }
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("J.A.R.V.I.S", style = MaterialTheme.typography.headlineLarge)
                Text("Personal Android assistant • You control every phone action")
                Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.weight(1f).fillMaxWidth()) {
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(messages) { line -> Text(line, style = MaterialTheme.typography.bodyLarge) }
                    }
                }
                pendingAction?.let { action ->
                    Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Approval required", style = MaterialTheme.typography.titleMedium)
                            Text(action.title, style = MaterialTheme.typography.titleSmall)
                            Text(action.explanation, style = MaterialTheme.typography.bodyMedium)
                            Text("This approval is for this action only. It does not grant ongoing access.", style = MaterialTheme.typography.bodySmall)
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(onClick = {
                                    val approved = pendingAction ?: return@Button
                                    pendingAction = null
                                    try {
                                        context.startActivity(approved.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                                        saveReply("Approved. I opened the requested Android screen or app.")
                                    } catch (_: Exception) {
                                        saveReply("Android could not open that action on this device.")
                                    }
                                }, modifier = Modifier.weight(1f)) { Text("Approve once") }
                                OutlinedButton(onClick = {
                                    pendingAction = null
                                    saveReply("Denied. I cancelled that action and did not run it.")
                                }, modifier = Modifier.weight(1f)) { Text("Deny") }
                            }
                        }
                    }
                }
                OutlinedTextField(value = input, onValueChange = { input = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Message JARVIS") }, maxLines = 3)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to JARVIS")
                        }
                        runCatching { voiceLauncher.launch(intent) }
                    }, modifier = Modifier.weight(1f)) { Text("Speak") }
                    Button(onClick = { sendMessage() }, modifier = Modifier.weight(1f)) { Text("Send") }
                }
                Spacer(modifier = Modifier.height(2.dp))
            }
        }
    }
}

/** Prepares an action only. This function never launches an Intent. */
private fun resolvePhoneAction(context: Context, rawCommand: String): Pair<PendingPhoneAction?, String>? {
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
        PendingPhoneAction(title, explanation, intent) to ""
    if (command == "time" || command.contains("what time")) {
        return null to "It's ${SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())}."
    }
    if (command == "date" || command.contains("today's date") || command.contains("what is the date")) {
        return null to "Today is ${SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(Date())}."
    }
    if (command == "settings" || command == "open settings") return approval("Open Android Settings", "JARVIS will open the system Settings screen.", Intent(Settings.ACTION_SETTINGS))
    if (command.contains("wifi") || command.contains("wi-fi")) return approval("Open Wi-Fi settings", "You remain in control of network changes.", Intent(Settings.ACTION_WIFI_SETTINGS))
    if (command.contains("bluetooth")) return approval("Open Bluetooth settings", "You remain in control of pairing and changes.", Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
    if (command == "camera" || command == "open camera") return approval("Open camera", "JARVIS will open the camera. It will not take a photo automatically.", Intent(MediaStore.ACTION_IMAGE_CAPTURE))
    if (command == "call" || command.contains("open phone") || command.contains("dialer")) return approval("Open phone dialer", "JARVIS will open the dialer, not place a call.", Intent(Intent.ACTION_DIAL))
    if (command.startsWith("set alarm") || command.startsWith("alarm")) return approval("Open alarms", "Review and confirm the alarm in the Android alarm screen.", Intent(AlarmClock.ACTION_SHOW_ALARMS))
    if (command.startsWith("search ") || command.startsWith("google ")) {
        val query = rawCommand.substringAfter(' ').trim()
        return approval("Search the web", "Open a browser search for: $query", Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=" + Uri.encode(query))))
    }
    if (command.startsWith("navigate to ") || command.startsWith("map ")) {
        val destination = rawCommand.substringAfter(" to ", rawCommand.substringAfter("map ", "")).trim()
        return approval("Open Maps", "Open Maps for: $destination", Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(destination))))
    }
    if (command.startsWith("open ")) {
        val requested = command.removePrefix("open ").trim()
        val packageName = launchers[requested]
        if (packageName != null) {
            val appIntent = context.packageManager.getLaunchIntentForPackage(packageName)
                ?: return null to "I couldn't find $requested installed on this device."
            return approval("Open $requested", "Launch $requested. No other app data will be accessed.", appIntent)
        }
    }
    return null
}
