package com.manekdm13.jarvis

import android.content.Intent
import android.os.Bundle
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
import androidx.compose.ui.unit.dp
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
    var input by remember { mutableStateOf("") }
    var messages by remember {
        mutableStateOf(listOf("JARVIS: Online. Voice input and spoken replies are ready."))
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
        val reply = when {
            question.contains("hello", ignoreCase = true) ||
                question.contains("hi", ignoreCase = true) ->
                "Hello. I'm JARVIS. What can I help you with?"
            question.contains("time", ignoreCase = true) ->
                "I can add live device information in the next development stage."
            else ->
                "I received: $question. My AI brain is not connected yet; that is the next major integration."
        }
        messages = messages + "YOU: $question" + "JARVIS: $reply"
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
                Text("Personal Android assistant • Prototype 0.1")
                Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.weight(1f).fillMaxWidth()) {
                    LazyColumn(
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
