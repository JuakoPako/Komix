package com.joaquin.komix

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class MainActivity : AppCompatActivity() {

    private lateinit var oFirebaseAnalytics: FirebaseAnalytics
    private lateinit var auth: FirebaseAuth
    private var googleSignInClient: GoogleSignInClient? = null

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)

        try {
            val account: GoogleSignInAccount? = task.getResult(ApiException::class.java)

            if (account != null && account.idToken != null) {
                firebaseAuthWithGoogle(account.idToken!!)
            } else {
                Toast.makeText(this, "No se pudo obtener la cuenta de Google", Toast.LENGTH_SHORT).show()
            }

        } catch (e: ApiException) {
            Log.w("GoogleSignIn", "Error al iniciar con Google: ${e.statusCode}", e)
            Toast.makeText(this, "Error en Google Sign-In", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        oFirebaseAnalytics = FirebaseAnalytics.getInstance(this)
        auth = FirebaseAuth.getInstance()

        try {
            val webClientId = getString(R.string.default_web_client_id)
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(webClientId)
                .requestEmail()
                .build()

            googleSignInClient = GoogleSignIn.getClient(this, gso)

        } catch (e: Exception) {
            Log.w("GoogleSignIn", "Error al configurar Google Client", e)
        }

        val txtUsuario = findViewById<EditText>(R.id.txtUsuario)
        val txtPassword = findViewById<EditText>(R.id.txtPassword)

        val btnIngresar = findViewById<Button>(R.id.btnComics)
        val btnRegistrar = findViewById<Button>(R.id.btnRegistrar)
        val btnGoogle = findViewById<Button>(R.id.btnGoogle)

        btnIngresar.setOnClickListener {
            var email = txtUsuario.text.toString().trim()
            var pass = txtPassword.text.toString().trim()

            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Ingresa tu correo y contraseña", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (email == "admin" && pass == "123") {
                email = "admin@komix.com"
                pass = "123456"
            }

            auth.signInWithEmailAndPassword(email, pass)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        registrarAnalytics("email")
                        irAlMenu()

                    } else {
                        if (email == "admin@komix.com") {
                            crearUsuarioPrueba(email, pass)
                        } else {
                            Toast.makeText(this, "Correo o contraseña incorrectos", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
        }

        btnRegistrar.setOnClickListener {
            val email = txtUsuario.text.toString().trim()
            val pass = txtPassword.text.toString().trim()

            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Ingresa un correo y clave de al menos 6 caracteres", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (pass.length < 6) {
                Toast.makeText(this, "La clave debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email, pass)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Usuario registrado con éxito", Toast.LENGTH_SHORT).show()
                        registrarAnalytics("registro")
                        irAlMenu()

                    } else {
                        Toast.makeText(this, "Error al registrar: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        btnGoogle.setOnClickListener {
            if (googleSignInClient != null) {
                googleSignInClient!!.signOut().addOnCompleteListener {
                    googleSignInLauncher.launch(googleSignInClient!!.signInIntent)
                }
            } else {
                Toast.makeText(this, "Google Sign-In no disponible", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)

        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    registrarAnalytics("google")
                    irAlMenu()

                } else {
                    Toast.makeText(this, "Error de autenticación con Google", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun crearUsuarioPrueba(email: String, pass: String) {
        auth.createUserWithEmailAndPassword(email, pass)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    registrarAnalytics("email")
                    irAlMenu()
                } else {
                    Toast.makeText(this, "Error de acceso", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun registrarAnalytics(metodo: String) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.METHOD, metodo)
        }
        oFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.LOGIN, bundle)
    }

    private fun irAlMenu() {
        val intent = Intent(this, MenuPrincipal::class.java)
        startActivity(intent)
        finish()
    }
}