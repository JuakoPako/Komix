package com.joaquin.komix

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
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
import com.google.firebase.installations.FirebaseInstallations

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
                Voz.aviso(this, "No se pudo obtener la cuenta de Google")
            }

        } catch (e: ApiException) {
            Log.w("GoogleSignIn", "Error al iniciar con Google: ${e.statusCode}", e)
            Voz.aviso(this, "Error en Google Sign-In")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        Voz.iniciar(this)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        oFirebaseAnalytics = FirebaseAnalytics.getInstance(this)
        auth = FirebaseAuth.getInstance()

        FirebaseInstallations.getInstance().id.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d("FIREBASE_TEST_ID", "ID de Instalacion: ${task.result}")
            } else {
                Log.e("FIREBASE_TEST_ID", "Error al obtener ID", task.exception)
            }
        }
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

        // Solo el correo: la contraseña no se dice en voz alta
        Voz.escucharTeclado(txtUsuario)

        val btnIngresar = findViewById<Button>(R.id.btnComics)
        val btnRegistrar = findViewById<Button>(R.id.btnRegistrar)
        val btnGoogle = findViewById<Button>(R.id.btnGoogle)

        btnIngresar.setOnClickListener {
            var email = txtUsuario.text.toString().trim()
            var pass = txtPassword.text.toString().trim()

            if (email.isEmpty() || pass.isEmpty()) {
                Voz.aviso(this, "Ingresa tu correo y contraseña")
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
                            Voz.aviso(this, "Correo o contraseña incorrectos")
                        }
                    }
                }
        }

        btnRegistrar.setOnClickListener {
            val email = txtUsuario.text.toString().trim()
            val pass = txtPassword.text.toString().trim()

            if (email.isEmpty() || pass.isEmpty()) {
                Voz.aviso(this, "Ingresa un correo y clave de al menos 6 caracteres")
                return@setOnClickListener
            }

            if (pass.length < 6) {
                Voz.aviso(this, "La clave debe tener al menos 6 caracteres")
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email, pass)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Voz.aviso(this, "Usuario registrado con éxito")
                        registrarAnalytics("registro")
                        irAlMenu()

                    } else {
                        Voz.aviso(this, "Error al registrar: ${task.exception?.message}", "Error al registrar")
                    }
                }
        }

        btnGoogle.setOnClickListener {
            if (googleSignInClient != null) {
                googleSignInClient!!.signOut().addOnCompleteListener {
                    googleSignInLauncher.launch(googleSignInClient!!.signInIntent)
                }
            } else {
                Voz.aviso(this, "Google Sign-In no disponible")
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
                    Voz.aviso(this, "Error de autenticación con Google")
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
                    Voz.aviso(this, "Error de acceso")
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