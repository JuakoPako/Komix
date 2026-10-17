package com.joaquin.komix

import android.os.Bundle
import android.view.LayoutInflater
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

class ListaCategoriasActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var oFirebaseAnalytics: FirebaseAnalytics

    private lateinit var contenedor: LinearLayout
    private lateinit var lblContadorCategorias: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_lista_categorias)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        oFirebaseAnalytics = FirebaseAnalytics.getInstance(this)

        contenedor = findViewById(R.id.contenedorCategorias)
        lblContadorCategorias = findViewById(R.id.lblContadorCategorias)
        val btnAgregar = findViewById<ExtendedFloatingActionButton>(R.id.btnAgregarCategoria)
        val btnVolver = findViewById<ImageButton>(R.id.btnVolver)

        btnVolver.setOnClickListener { finish() }

        btnAgregar.setOnClickListener {
            mostrarDialogoFormulario(null, "")
        }
    }

    override fun onResume() {
        super.onResume()
        cargarDatos()
    }

    private fun cargarDatos() {
        val uidActual = auth.currentUser?.uid ?: ""

        db.collection("Categorias")
            .whereEqualTo("uid", uidActual)
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful && task.result != null) {
                    contenedor.removeAllViews()
                    val docs = task.result.documents
                    val total = docs.size
                    lblContadorCategorias.text = if (total == 1) "1 categoría registrada" else "$total categorías registradas"

                    val inflater = LayoutInflater.from(this)

                    for (doc in docs) {
                        val id = doc.id
                        val nombre = doc.getString("nombre") ?: "Sin nombre"

                        val itemView = inflater.inflate(R.layout.item_categoria, contenedor, false)

                        val txtNombre = itemView.findViewById<TextView>(R.id.txtNombreCategoria)
                        val btnEditar = itemView.findViewById<ImageButton>(R.id.btnEditar)
                        val btnEliminar = itemView.findViewById<ImageButton>(R.id.btnEliminar)

                        txtNombre.text = nombre

                        btnEditar.setOnClickListener {
                            mostrarDialogoFormulario(id, nombre)
                        }

                        btnEliminar.setOnClickListener {
                            confirmarEliminar(id, nombre)
                        }

                        contenedor.addView(itemView)
                    }

                    registrarEventoAnalytics("listado_consultado")

                } else {
                    Toast.makeText(this, "Error al cargar categorías", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun mostrarDialogoFormulario(idEditando: String?, nombreEdit: String) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_categoria_formulario, null)
        val input = dialogView.findViewById<EditText>(R.id.inputNombreCategoria)

        if (!idEditando.isNullOrEmpty()) {
            input.setText(nombreEdit)
        }

        val tituloDialogo = if (idEditando == null) "Agregar Categoría" else "Editar Categoría"

        AlertDialog.Builder(this)
            .setTitle(tituloDialogo)
            .setView(dialogView)
            .setPositiveButton("Guardar") { _, _ ->
                val texto = input.text.toString().trim()
                val uidActual = auth.currentUser?.uid ?: ""

                if (texto.isEmpty()) {
                    Toast.makeText(this, "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show()
                } else {
                    val datos = hashMapOf<String, Any>(
                        "uid" to uidActual,
                        "nombre" to texto
                    )

                    if (idEditando == null) {
                        db.collection("Categorias").add(datos).addOnSuccessListener {
                            registrarEventoAnalytics("registro_creado")
                            Toast.makeText(this, "Categoría agregada", Toast.LENGTH_SHORT).show()
                            cargarDatos()
                        }
                    } else {
                        db.collection("Categorias").document(idEditando).update(datos).addOnSuccessListener {
                            registrarEventoAnalytics("registro_actualizado")
                            Toast.makeText(this, "Categoría actualizada", Toast.LENGTH_SHORT).show()
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
            .setTitle("Eliminar categoría")
            .setMessage("¿Seguro que quieres eliminar \"$nombre\"?")
            .setPositiveButton("Eliminar") { _, _ ->
                db.collection("Categorias").document(id).delete().addOnSuccessListener {
                    registrarEventoAnalytics("registro_eliminado")
                    Toast.makeText(this, "Categoría eliminada", Toast.LENGTH_SHORT).show()
                    cargarDatos()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun registrarEventoAnalytics(nombreEvento: String) {
        val bundle = Bundle().apply {
            putString("tipo_entidad", "categoria")
        }
        oFirebaseAnalytics.logEvent(nombreEvento, bundle)
    }
}