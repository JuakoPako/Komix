package com.joaquin.komix

import android.content.Context
import android.speech.tts.TextToSpeech
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.core.content.ContextCompat
import java.util.Locale

object Voz {

    private var tts: TextToSpeech? = null
    private var listo = false
    private var pendiente: String? = null

    var activada = false
        private set

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

    fun hablar(texto: String, encolar: Boolean = false) {
        if (!activada || texto.isBlank()) return

        if (!listo) {
            pendiente = texto
            return
        }

        val modo = if (encolar) TextToSpeech.QUEUE_ADD else TextToSpeech.QUEUE_FLUSH
        tts?.speak(texto, modo, null, "komix_voz")
    }


    fun escucharTeclado(campo: EditText) {
        campo.addTextChangedListener(object : TextWatcher {
            private var anterior = ""

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                anterior = s?.toString() ?: ""
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {

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

    fun aviso(contexto: Context, mensaje: String, decir: String = mensaje) {
        Toast.makeText(contexto, mensaje, Toast.LENGTH_SHORT).show()
        hablar(decir)
    }

    fun detener() {
        tts?.stop()
    }


    fun desactivar() {
        activada = false
        pendiente = null
        detener()
    }


    fun configurarBoton(boton: ImageButton) {
        actualizarBoton(boton)

        boton.setOnClickListener {
            if (activada) {
                desactivar()
                Toast.makeText(boton.context, "Voz desactivada", Toast.LENGTH_SHORT).show()
            } else {
                activada = true
                Toast.makeText(boton.context, "Voz activada", Toast.LENGTH_SHORT).show()
                hablar("Voz activada")
            }
            actualizarBoton(boton)
        }
    }

    private fun actualizarBoton(boton: ImageButton) {
        val color = if (activada) R.color.komix_aqua else R.color.komix_gray_text
        boton.setImageResource(if (activada) R.drawable.ic_voz_on else R.drawable.ic_voz_off)
        boton.setColorFilter(ContextCompat.getColor(boton.context, color))
        boton.contentDescription = if (activada) "Desactivar voz" else "Activar voz"
    }
}