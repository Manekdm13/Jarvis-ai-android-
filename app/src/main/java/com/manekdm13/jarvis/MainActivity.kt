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
    var messages by remember(memory) {
        val restored = memory.load()
        mutableStateOf(
            restored.ifEmpty {
                listOf("JARVIS: Online. I can now handle a starter set of phone commands.")
            }
        )
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
        val reply = executePhoneCommand(context, question)
            ?: "I received: $question. That needs the online AI brain, which is not connected yet."
        val updatedMessages = (messages + "YOU: $question" + "JARVIS: $reply").takeLast(100)
        messages = updatedMessages
        memory.save(updatedMessages)
        input = ""
        onSpeak(reply)
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier.fillMaxSize().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("J.A.R.V.I.S", style = MaterialTheme.typography.headlineLarge)
                Text("Personal Android assistant • Phone actions + private local memory")
                Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.weight(1f).fillMaxWidth()) {
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
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to JARVIS")
                            }
                            runCatching { voiceLauncher.launch(intent) }
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Speak") }
                    Button(onClick = { sendMessage() }, modifier = Modifier.weight(1f)) {
                        Text("Send")
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
            }
        }
    }
}

/**
 * Executes a deliberately limited set of local Android actions.
 * Sensitive actions (calls, sending messages, changing protected settings)
 * open Android's own confirmation-capable screen instead of acting silently.
 */
private fun executePhoneCommand(context: Context, rawCommand: String): String? {
    val command = rawCommand.trim().lowercase(Locale.ROOT)
    val launchers = mapOf(
        "youtube" to "com.google.android.youtube",
        "whatsapp" to "com.whatsapp",
        "chrome" to "com.android.chrome",
        "google maps" to "com.google.android.apps.maps",
        "maps" to "com.google.android.apps.maps",
        "gmail" to "com.google.android.gm"
    )

    fun launch(intent: Intent, success: String): String {
        return try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            success
        } catch (_: Exception) {
            "I couldn't open that on this device. The app may not be installed."
        }
    }

    if (command == "time" || command.contains("what time")) {
        val time = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
        return "It's $time."
    }
    if (command == "date" || command.contains("today's date") || command.contains("what is the date")) {
        val date = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(Date())
        return "Today is $date."
    }
    if (command.contains("open settings") || command == "settings") {
        return launch(Intent(Settings.ACTION_SETTINGS), "Opening Android settings.")
    }
    if (command.contains("wifi") || command.contains("wi-fi")) {
        return launch(Intent(Settings.ACTION_WIFI_SETTINGS), "Opening Wi-Fi settings.")
    }
    if (command.contains("bluetooth")) {
        return launch(Intent(Settings.ACTION_BLUETOOTH_SETTINGS), "Opening Bluetooth settings.")
    }
    if (command.contains("open camera") || command == "camera") {
        return launch(Intent(MediaStore.ACTION_IMAGE_CAPTURE), "Opening the camera.")
    }
    if (command.contains("open phone") || command.contains("dialer") || command == "call") {
        return launch(Intent(Intent.ACTION_DIAL), "Opening the phone dialer. You can review the number before calling.")
    }
    if (command.contains("set alarm") || command.startsWith("alarm")) {
        val intent = Intent(AlarmClock.ACTION_SHOW_ALARMS)
        return launch(intent, "Opening your alarms. Set or confirm the alarm there.")
    }
    if (command.startsWith("search ") || command.startsWith("google ")) {
        val query = rawCommand.substringAfter(' ').trim()
        val uri = Uri.parse("https://www.google.com/search?q=" + Uri.encode(query))
        return launch(Intent(Intent.ACTION_VIEW, uri), "Searching the web for $query.")
    }
    if (command.startsWith("navigate to ") || command.startsWith("map ")) {
        val destination = rawCommand.substringAfter(" to ", rawCommand.substringAfter("map ", "")).trim()
        val uri = Uri.parse("geo:0,0?q=" + Uri.encode(destination))
        return launch(Intent(Intent.ACTION_VIEW, uri), "Opening maps for $destination.")
    }
    if (command.startsWith("open ")) {
        val requested = command.removePrefix("open ").trim()
        val packageName = launchers[requested]
        if (packageName != null) {
            val appIntent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (appIntent != null) return launch(appIntent, "Opening $requested.")
            return "I couldn't find $requested installed on this device."
        }
    }
    return null
}
