package com.joaquin.komix

import android.content.Context
import android.speech.tts.TextToSpeech
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import java.util.Locale

/**
 * Voz de la aplicación (Text-to-Speech).
 * Es un objeto único para toda la app: se inicia una sola vez y sigue hablando
 * aunque la pantalla cambie o se cierre.
 */
object Voz {

    private var tts: TextToSpeech? = null
    private var listo = false
    private var pendiente: String? = null

    // Se llama en el onCreate de cada pantalla; solo inicia el motor la primera vez
    fun iniciar(contexto: Context) {
        if (tts != null) return

        tts = TextToSpeech(contexto.applicationContext) { estado ->
            if (estado != TextToSpeech.SUCCESS) {
                Log.e("Voz", "No se pudo iniciar Text-to-Speech")
                return@TextToSpeech
            }

            val motor = tts ?: return@TextToSpeech

            // Primero español de Chile, luego España, luego español genérico
            val idiomas = listOf(Locale("es", "CL"), Locale("es", "ES"), Locale("es"))
            val elegido = idiomas.firstOrNull { motor.isLanguageAvailable(it) >= TextToSpeech.LANG_AVAILABLE }

            if (elegido != null) {
                motor.language = elegido
            } else {
                Log.w("Voz", "No hay voz en español instalada, se usa la del sistema")
            }

            listo = true

            // Si pidieron hablar antes de que el motor estuviera listo
            pendiente?.let { hablar(it) }
            pendiente = null
        }
    }

    // Dice el texto en voz alta.
    // Normal: corta lo que estaba diciendo. Con encolar = true: espera a que termine lo anterior.
    fun hablar(texto: String, encolar: Boolean = false) {
        if (texto.isBlank()) return

        if (!listo) {
            pendiente = texto
            return
        }

        val modo = if (encolar) TextToSpeech.QUEUE_ADD else TextToSpeech.QUEUE_FLUSH
        tts?.speak(texto, modo, null, "komix_voz")
    }

    // Dice cada letra que se escribe en el campo (y "borrar" cuando se borra una).
    // No usar en campos de contraseña.
    fun escucharTeclado(campo: EditText) {
        campo.addTextChangedListener(object : TextWatcher {
            private var anterior = ""

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                anterior = s?.toString() ?: ""
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                // Si el texto lo puso la app (por ejemplo al editar un registro), no habla
                if (!campo.hasFocus()) return
                val nuevo = s?.toString() ?: return

                when {
                    nuevo.length == anterior.length + 1 -> {
                        var i = 0
                        while (i < anterior.length && anterior[i] == nuevo[i]) i++
                        hablar(nombreCaracter(nuevo[i]))
                    }
                    nuevo.length == anterior.length - 1 -> hablar("borrar")
                }
            }
        })
    }

    // Nombre hablado de los símbolos que el motor suele leer mal
    private fun nombreCaracter(c: Char): String = when (c) {
        ' ' -> "espacio"
        '@' -> "arroba"
        '.' -> "punto"
        ',' -> "coma"
        '-' -> "guion"
        '_' -> "guion bajo"
        '+' -> "más"
        '/' -> "barra"
        '#' -> "numeral"
        '$' -> "dólar"
        else -> c.toString()
    }

    // Muestra el mensaje en pantalla (Toast) y además lo dice.
    // Si el mensaje trae un error técnico, "decir" permite hablar solo la parte clara.
    fun aviso(contexto: Context, mensaje: String, decir: String = mensaje) {
        Toast.makeText(contexto, mensaje, Toast.LENGTH_SHORT).show()
        hablar(decir)
    }

    fun detener() {
        tts?.stop()
    }
}