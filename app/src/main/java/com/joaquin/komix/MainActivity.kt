package com.joaquin.komix

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.analytics.FirebaseAnalytics


class MainActivity : AppCompatActivity() {

    private lateinit var firebaseAnalytics: FirebaseAnalytics

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        firebaseAnalytics = FirebaseAnalytics.getInstance(this)

        val usuario = findViewById<EditText>(R.id.txtUsuario)
        val password = findViewById<EditText>(R.id.txtPassword)
        val ingresar = findViewById<Button>(R.id.btnComics)

        val usuarioValido = "admin"
        val contraValida = "123"

        ingresar.setOnClickListener {

            val textoUsuario = usuario.text.toString()
            val textoPassword = password.text.toString()

            if (textoUsuario == usuarioValido && textoPassword == contraValida) {

                val eventoLogin = Bundle()
                eventoLogin.putString(FirebaseAnalytics.Param.METHOD, "local")
                firebaseAnalytics.logEvent(FirebaseAnalytics.Event.LOGIN, eventoLogin)

                val menuPrincipal = Intent(this, MenuPrincipal::class.java)
                startActivity(menuPrincipal)
                finish()
            } else {
                Toast.makeText(this, "Usuario o Contraseña Incorrectos", Toast.LENGTH_LONG).show()
            }
        }
    }
}