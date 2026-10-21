package com.joaquin.komix

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.widget.ImageButton
import android.widget.ImageView
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

class ListaComicsActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var oFirebaseAnalytics: FirebaseAnalytics

    private lateinit var contenedor: LinearLayout
    private lateinit var lblContadorComics: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_lista_comics)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        oFirebaseAnalytics = FirebaseAnalytics.getInstance(this)

        contenedor = findViewById(R.id.contenedorComics)
        lblContadorComics = findViewById(R.id.lblContadorComics)
        val btnAgregar = findViewById<ExtendedFloatingActionButton>(R.id.btnAgregarComic)
        val btnVolver = findViewById<ImageButton>(R.id.btnVolver)

        btnVolver.setOnClickListener { finish() }

        btnAgregar.setOnClickListener {
            startActivity(Intent(this, FormularioComicActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        cargarDatos()
    }

    private fun cargarDatos() {
        val uidActual = auth.currentUser?.uid ?: ""

        lblContadorComics.text = "Cargando cómics..."

        db.collection("Categorias").whereEqualTo("uid", uidActual).get().addOnCompleteListener { taskCat ->
            val categoriasMap = HashMap<String, String>()
            if (taskCat.isSuccessful && taskCat.result != null) {
                for (doc in taskCat.result) {
                    categoriasMap[doc.id] = doc.getString("nombre") ?: "Sin categoría"
                }
            }

            db.collection("Editoriales").whereEqualTo("uid", uidActual).get().addOnCompleteListener { taskEd ->
                val editorialesMap = HashMap<String, String>()
                if (taskEd.isSuccessful && taskEd.result != null) {
                    for (doc in taskEd.result) {
                        editorialesMap[doc.id] = doc.getString("nombre") ?: "Sin editorial"
                    }
                }

                db.collection("Comics").whereEqualTo("uid", uidActual).get().addOnCompleteListener { taskComics ->
                    if (taskComics.isSuccessful && taskComics.result != null) {
                        contenedor.removeAllViews()
                        val docs = taskComics.result.documents
                        val total = docs.size
                        lblContadorComics.text = when (total) {
                            0 -> "Aún no hay cómics registrados"
                            1 -> "1 cómic en la colección"
                            else -> "$total cómics en la colección"
                        }

                        val inflater = LayoutInflater.from(this)

                        for (doc in docs) {
                            val id = doc.id
                            val titulo = doc.getString("titulo") ?: "Sin título"
                            val autor = doc.getString("autor") ?: "Sin autor"
                            val anio = doc.getLong("anioPublicacion")?.toInt() ?: 0
                            val precio = doc.getDouble("precio") ?: 0.0
                            val idCat = doc.getString("idCategoria") ?: ""
                            val idEd = doc.getString("idEditorial") ?: ""
                            val portadaB64 = doc.getString("portadaBase64")

                            val itemView = inflater.inflate(R.layout.item_comic, contenedor, false)

                            val imgPortada = itemView.findViewById<ImageView>(R.id.imgPortada)
                            val txtTitulo = itemView.findViewById<TextView>(R.id.txtTitulo)
                            val txtAutor = itemView.findViewById<TextView>(R.id.txtAutor)
                            val txtEditorialTag = itemView.findViewById<TextView>(R.id.txtEditorialTag)
                            val txtCategoriaTag = itemView.findViewById<TextView>(R.id.txtCategoriaTag)
                            val btnEditar = itemView.findViewById<ImageButton>(R.id.btnEditar)
                            val btnEliminar = itemView.findViewById<ImageButton>(R.id.btnEliminar)

                            txtTitulo.text = titulo
                            val infoAutor = buildString {
                                append(autor)
                                if (anio > 0) append(" · $anio")
                                if (precio > 0) append(" · $$precio")
                            }
                            txtAutor.text = infoAutor

                            if (!portadaB64.isNullOrEmpty()) {
                                try {
                                    val decodedBytes = Base64.decode(portadaB64, Base64.DEFAULT)
                                    val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                                    imgPortada.setImageBitmap(bitmap)
                                } catch (e: Exception) {
                                    imgPortada.setImageResource(android.R.drawable.ic_menu_gallery)
                                }
                            } else {
                                imgPortada.setImageResource(android.R.drawable.ic_menu_gallery)
                            }

                            txtCategoriaTag.text = categoriasMap[idCat] ?: "Sin categoría"
                            txtEditorialTag.text = editorialesMap[idEd] ?: "Sin editorial"

                            btnEditar.setOnClickListener {
                                val intent = Intent(this, FormularioComicActivity::class.java)
                                intent.putExtra("id_comic", id)
                                startActivity(intent)
                            }

                            btnEliminar.setOnClickListener {
                                confirmarEliminar(id, titulo)
                            }

                            contenedor.addView(itemView)
                        }

                        registrarEventoAnalytics("listado_consultado")

                    } else {
                        lblContadorComics.text = "No se pudieron cargar los cómics"
                        Toast.makeText(this, "Error al cargar cómics", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun confirmarEliminar(id: String, titulo: String) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar cómic")
            .setMessage("¿Seguro que quieres eliminar \"$titulo\"?")
            .setPositiveButton("Eliminar") { _, _ ->
                db.collection("Comics").document(id).delete().addOnSuccessListener {
                    registrarEventoAnalytics("registro_eliminado")
                    Toast.makeText(this, "Cómic eliminado", Toast.LENGTH_SHORT).show()
                    cargarDatos()
                }.addOnFailureListener { e ->
                    Toast.makeText(this, "Error al eliminar: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun registrarEventoAnalytics(nombreEvento: String) {
        val bundle = Bundle().apply {
            putString("tipo_entidad", "comic")
        }
        oFirebaseAnalytics.logEvent(nombreEvento, bundle)
    }
}