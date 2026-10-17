package com.joaquin.komix

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ListaEditorialesActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var oFirebaseAnalytics: FirebaseAnalytics

    private lateinit var contenedor: LinearLayout
    private lateinit var lblContadorEditoriales: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_lista_editoriales)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        oFirebaseAnalytics = FirebaseAnalytics.getInstance(this)

        contenedor = findViewById(R.id.contenedorEditoriales)
        lblContadorEditoriales = findViewById(R.id.lblContadorEditoriales)
        val btnAgregar = findViewById<ExtendedFloatingActionButton>(R.id.btnAgregarEditorial)
        val btnVolver = findViewById<ImageButton>(R.id.btnVolver)

        btnVolver.setOnClickListener { finish() }

        btnAgregar.setOnClickListener {
            mostrarDialogoFormulario(null, "", "", "", 0, "Activa")
        }
    }

    override fun onResume() {
        super.onResume()
        cargarDatos()
    }

    private fun cargarDatos() {
        val uidActual = auth.currentUser?.uid ?: ""

        db.collection("Editoriales")
            .whereEqualTo("uid", uidActual)
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful && task.result != null) {
                    contenedor.removeAllViews()
                    val docs = task.result.documents
                    val total = docs.size
                    lblContadorEditoriales.text = if (total == 1) "1 editorial registrada" else "$total editoriales registradas"

                    val inflater = LayoutInflater.from(this)

                    for (doc in docs) {
                        val id = doc.id
                        val nombre = doc.getString("nombre") ?: "Sin nombre"
                        val correo = doc.getString("correoContacto") ?: ""
                        val pais = doc.getString("paisOrigen") ?: ""
                        val anio = doc.getLong("anioFundacion")?.toInt() ?: 0
                        val estado = doc.getString("estado") ?: "Activa"

                        val itemView = inflater.inflate(R.layout.item_editorial, contenedor, false)

                        val txtInicial = itemView.findViewById<TextView>(R.id.txtInicial)
                        val txtNombre = itemView.findViewById<TextView>(R.id.txtNombreEditorial)
                        val txtPais = itemView.findViewById<TextView>(R.id.txtPaisEditorial)
                        val txtContacto = itemView.findViewById<TextView>(R.id.txtContactoEditorial)
                        val btnEditar = itemView.findViewById<ImageButton>(R.id.btnEditar)
                        val btnEliminar = itemView.findViewById<ImageButton>(R.id.btnEliminar)

                        txtInicial.text = nombre.take(1).uppercase()
                        txtNombre.text = nombre

                        val infoPaisAnio = buildString {
                            if (pais.isNotEmpty()) append("📍 $pais")
                            if (anio > 0) append(" · Fundada en $anio")
                        }
                        txtPais.text = infoPaisAnio

                        val estadoIcono = if (estado == "Inactiva") "🔴 Inactiva" else "🟢 Activa"
                        val infoContacto = buildString {
                            if (correo.isNotEmpty()) append("✉️ $correo · ")
                            append(estadoIcono)
                        }
                        txtContacto.text = infoContacto

                        btnEditar.setOnClickListener {
                            mostrarDialogoFormulario(id, nombre, correo, pais, anio, estado)
                        }

                        btnEliminar.setOnClickListener {
                            confirmarEliminar(id, nombre)
                        }

                        contenedor.addView(itemView)
                    }

                    registrarEventoAnalytics("listado_consultado")

                } else {
                    Toast.makeText(this, "Error al cargar editoriales", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun mostrarDialogoFormulario(
        idEditando: String?,
        nombreEdit: String,
        correoEdit: String,
        paisEdit: String,
        anioEdit: Int,
        estadoEdit: String
    ) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_editorial_formulario, null)
        val inputNombre = dialogView.findViewById<EditText>(R.id.inputNombre)
        val inputCorreo = dialogView.findViewById<EditText>(R.id.inputCorreo)
        val inputPais = dialogView.findViewById<EditText>(R.id.inputPais)
        val inputAnio = dialogView.findViewById<EditText>(R.id.inputAnio)
        val spinnerEstado = dialogView.findViewById<AutoCompleteTextView>(R.id.spinnerEstado)

        val opcionesEstado = listOf("Activa", "Inactiva")
        val adapterEstado = ArrayAdapter(this, R.layout.item_dropdown_dialog, opcionesEstado)
        spinnerEstado.setAdapter(adapterEstado)

        if (!idEditando.isNullOrEmpty()) {
            inputNombre.setText(nombreEdit)
            inputCorreo.setText(correoEdit)
            inputPais.setText(paisEdit)
            if (anioEdit > 0) inputAnio.setText(anioEdit.toString())
            spinnerEstado.setText(estadoEdit, false)
        } else {
            spinnerEstado.setText("Activa", false)
        }

        val tituloDialogo = if (idEditando == null) "Agregar Editorial" else "Editar Editorial"

        AlertDialog.Builder(this)
            .setTitle(tituloDialogo)
            .setView(dialogView)
            .setPositiveButton("Guardar") { _, _ ->
                val nombre = inputNombre.text.toString().trim()
                val correo = inputCorreo.text.toString().trim()
                val pais = inputPais.text.toString().trim()
                val anio = inputAnio.text.toString().toIntOrNull() ?: 0
                val estado = spinnerEstado.text.toString().trim().ifEmpty { "Activa" }

                val uidActual = auth.currentUser?.uid ?: ""

                if (nombre.isEmpty() || pais.isEmpty()) {
                    Toast.makeText(this, "Nombre y país son obligatorios", Toast.LENGTH_SHORT).show()
                } else {
                    val datos = hashMapOf<String, Any>(
                        "uid" to uidActual,
                        "nombre" to nombre,
                        "correoContacto" to correo,
                        "paisOrigen" to pais,
                        "anioFundacion" to anio,
                        "estado" to estado
                    )

                    if (idEditando == null) {
                        db.collection("Editoriales").add(datos).addOnSuccessListener {
                            registrarEventoAnalytics("registro_creado")
                            Toast.makeText(this, "Editorial guardada", Toast.LENGTH_SHORT).show()
                            cargarDatos()
                        }
                    } else {
                        db.collection("Editoriales").document(idEditando).update(datos).addOnSuccessListener {
                            registrarEventoAnalytics("registro_actualizado")
                            Toast.makeText(this, "Editorial actualizada", Toast.LENGTH_SHORT).show()
                            cargarDatos()
                        }
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun confirmarEliminar(id: String, nombre: String) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar editorial")
            .setMessage("¿Seguro que quieres eliminar \"$nombre\"?")
            .setPositiveButton("Eliminar") { _, _ ->
                db.collection("Editoriales").document(id).delete().addOnSuccessListener {
                    registrarEventoAnalytics("registro_eliminado")
                    Toast.makeText(this, "Editorial eliminada", Toast.LENGTH_SHORT).show()
                    cargarDatos()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun registrarEventoAnalytics(nombreEvento: String) {
        val bundle = Bundle().apply {
            putString("tipo_entidad", "editorial")
        }
        oFirebaseAnalytics.logEvent(nombreEvento, bundle)
    }
}