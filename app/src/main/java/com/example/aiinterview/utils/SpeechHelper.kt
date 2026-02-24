package com.example.aiinterview.utils

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

/**
 * Compose-friendly wrapper around Android's built-in SpeechRecognizer.
 *
 * Usage:
 *   val (speechState, startListening) = rememberSpeechHelper { text -> onResult(text) }
 *
 * No runtime permission dialog needed — RECORD_AUDIO is satisfied by the
 * system recognizer activity handling the intent.
 */

data class SpeechState(
    val isListening  : Boolean = false,
    val isAvailable  : Boolean = false
)

@Composable
fun rememberSpeechHelper(onResult: (String) -> Unit): Pair<SpeechState, () -> Unit> {
    val context = LocalContext.current
    var state by remember {
        mutableStateOf(SpeechState(isAvailable = SpeechRecognizer.isRecognitionAvailable(context)))
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        state = state.copy(isListening = false)
        if (result.resultCode == Activity.RESULT_OK) {
            result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                ?.let { if (it.isNotBlank()) onResult(it) }
        }
    }

    val startListening: () -> Unit = remember {
        {
            if (state.isAvailable) {
                state = state.copy(isListening = true)
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your interview answer…")
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2500L)
                }
                launcher.launch(intent)
            }
        }
    }

    return state to startListening
}
