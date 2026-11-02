package com.joaquin.komix

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import java.util.Locale


object Voz {

    private var tts: TextToSpeech? = null
    private var listo = false
    private var pendiente: String? = null

    fun iniciar(contexto: Context) {
        if (tts != null) return

        tts = TextToSpeech(contexto.applicationContext) { estado ->
            if (estado != TextToSpeech.SUCCESS) {
                Log.e("Voz", "No se pudo iniciar Text-to-Speech")
                return@TextToSpeech
            }

            val motor = tts ?: return@TextToSpeech


            val idiomas = listOf(Locale("es", "CL"), Locale("es", "ES"), Locale("es"))
            val elegido = idiomas.firstOrNull { motor.isLanguageAvailable(it) >= TextToSpeech.LANG_AVAILABLE }

            if (elegido != null) {
                motor.language = elegido
            } else {
                Log.w("Voz", "No hay voz en español instalada, se usa la del sistema")
            }

            listo = true


            pendiente?.let { hablar(it) }
            pendiente = null
        }
    }


    fun hablar(texto: String) {
        if (texto.isBlank()) return

        if (!listo) {
            pendiente = texto
            return
        }

        tts?.speak(texto, TextToSpeech.QUEUE_FLUSH, null, "komix_voz")
    }


    fun aviso(contexto: Context, mensaje: String, decir: String = mensaje) {
        Toast.makeText(contexto, mensaje, Toast.LENGTH_SHORT).show()
        hablar(decir)
    }

    fun detener() {
        tts?.stop()
    }
}