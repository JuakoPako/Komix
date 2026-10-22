package com.joaquin.komix

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.messaging.FirebaseMessaging
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions

class MenuPrincipal : AppCompatActivity() {

    private val solicitarPermisoNotificacion = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { esConcedido ->
        if (esConcedido) {
            Log.d("FCM", "Permiso aceptado")
        } else {
            Log.w("FCM", "Permiso denegado")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_menu_principal)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val lblBienvenida = findViewById<TextView>(R.id.lblBienvenida)
        val lblSubtitulo = findViewById<TextView>(R.id.lblSubtitulo)

        val usuario = FirebaseAuth.getInstance().currentUser

        if (usuario != null) {
            val nombre = usuario.displayName ?: usuario.email?.substringBefore("@") ?: "Usuario"
            val correo = usuario.email ?: "Sesión activa"

            lblBienvenida.text = "Hola, $nombre"
            lblSubtitulo.text = correo

        } else {
            lblBienvenida.text = "Hola, Usuario"
            lblSubtitulo.text = "Catálogo Komix"
        }

        val cardComics = findViewById<CardView>(R.id.cardComics)
        val cardCategorias = findViewById<CardView>(R.id.cardCategorias)
        val cardEditoriales = findViewById<CardView>(R.id.cardEditoriales)
        val cardCerrarSesion = findViewById<CardView>(R.id.cardCerrarSesion)

        val btnCrashTest = findViewById<Button>(R.id.btnCrashTest)
        val btnNonFatalTest = findViewById<Button>(R.id.btnNonFatalTest)

        cardComics.setOnClickListener {
            startActivity(Intent(this, ListaComicsActivity::class.java))
        }

        cardCategorias.setOnClickListener {
            startActivity(Intent(this, ListaCategoriasActivity::class.java))
        }

        cardEditoriales.setOnClickListener {
            startActivity(Intent(this, ListaEditorialesActivity::class.java))
        }

        cardCerrarSesion.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
            GoogleSignIn.getClient(this, gso).signOut().addOnCompleteListener {
                Toast.makeText(this,"Sesión Cerrada", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        }

        btnCrashTest.setOnClickListener {
            Toast.makeText(this, "Lanzando crash de prueba...", Toast.LENGTH_SHORT).show()
            throw RuntimeException("Test Crash")
        }

        btnNonFatalTest.setOnClickListener {
            try {
                throw Exception("Prueba de error capturado")
            } catch (e: Exception) {
                FirebaseCrashlytics.getInstance().recordException(e)
                Toast.makeText(this, "Error enviado a Crashlytics", Toast.LENGTH_SHORT).show()
            }
        }

        pedirPermisoNotificaciones()
        obtenerTokenFcm()
    }

    private fun pedirPermisoNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            solicitarPermisoNotificacion.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun obtenerTokenFcm() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                Log.d("FCM_TOKEN", "Token: $token")
            }
        }
    }
}