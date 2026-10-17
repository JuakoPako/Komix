package com.joaquin.komix

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.ByteArrayOutputStream

class FormularioComicActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var oFirebaseAnalytics: FirebaseAnalytics

    private var imagenSeleccionada: Bitmap? = null
    private var idComicEditando: String? = null

    private lateinit var imgPreview: ImageView
    private lateinit var txtTitulo: EditText
    private lateinit var txtAutor: EditText
    private lateinit var txtAnio: EditText
    private lateinit var txtPrecio: EditText
    private lateinit var spinnerCategoria: AutoCompleteTextView
    private lateinit var spinnerEditorial: AutoCompleteTextView

    private val categoriasListaIds = ArrayList<String>()
    private val categoriasListaNombres = ArrayList<String>()

    private val editorialesListaIds = ArrayList<String>()
    private val editorialesListaNombres = ArrayList<String>()

    private var idCatSeleccionada: String = ""
    private var idEdSeleccionada: String = ""

    private val selectorImagen = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
            imagenSeleccionada = bitmap
            imgPreview.setImageBitmap(bitmap)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_formulario_comic)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        oFirebaseAnalytics = FirebaseAnalytics.getInstance(this)

        imgPreview = findViewById(R.id.imgPreview)
        txtTitulo = findViewById(R.id.txtTitulo)
        txtAutor = findViewById(R.id.txtAutor)
        txtAnio = findViewById(R.id.txtAnio)
        txtPrecio = findViewById(R.id.txtPrecio)
        spinnerCategoria = findViewById(R.id.spinnerCategoria)
        spinnerEditorial = findViewById(R.id.spinnerEditorial)

        val btnSeleccionarImagen = findViewById<Button>(R.id.btnSeleccionarImagen)
        val btnGuardar = findViewById<Button>(R.id.btnGuardar)
        val btnVolver = findViewById<ImageButton>(R.id.btnVolver)

        btnVolver.setOnClickListener { finish() }

        cargarSpinners {
            idComicEditando = intent.getStringExtra("id_comic")
            if (!idComicEditando.isNullOrEmpty()) {
                cargarDatosComic(idComicEditando!!)
            }
        }

        btnSeleccionarImagen.setOnClickListener {
            selectorImagen.launch("image/*")
        }

        btnGuardar.setOnClickListener {
            guardarComic()
        }
    }

    private fun cargarSpinners(onComplete: () -> Unit) {
        val uidActual = auth.currentUser?.uid ?: ""

        db.collection("Categorias")
            .whereEqualTo("uid", uidActual)
            .get()
            .addOnCompleteListener { taskCat ->
                if (taskCat.isSuccessful && taskCat.result != null) {
                    categoriasListaIds.clear()
                    categoriasListaNombres.clear()

                    for (doc in taskCat.result) {
                        categoriasListaIds.add(doc.id)
                        categoriasListaNombres.add(doc.getString("nombre") ?: "Sin nombre")
                    }

                    val adapterCat = ArrayAdapter(this, R.layout.item_dropdown, categoriasListaNombres)
                    spinnerCategoria.setAdapter(adapterCat)
                    spinnerCategoria.setOnItemClickListener { _, _, pos, _ ->
                        idCatSeleccionada = categoriasListaIds[pos]
                    }
                }

                db.collection("Editoriales")
                    .whereEqualTo("uid", uidActual)
                    .get()
                    .addOnCompleteListener { taskEd ->
                        if (taskEd.isSuccessful && taskEd.result != null) {
                            editorialesListaIds.clear()
                            editorialesListaNombres.clear()

                            for (doc in taskEd.result) {
                                editorialesListaIds.add(doc.id)
                                editorialesListaNombres.add(doc.getString("nombre") ?: "Sin nombre")
                            }

                            val adapterEd = ArrayAdapter(this, R.layout.item_dropdown, editorialesListaNombres)
                            spinnerEditorial.setAdapter(adapterEd)
                            spinnerEditorial.setOnItemClickListener { _, _, pos, _ ->
                                idEdSeleccionada = editorialesListaIds[pos]
                            }
                        }

                        onComplete()
                    }
            }
    }

    private fun cargarDatosComic(docId: String) {
        db.collection("Comics").document(docId).get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                txtTitulo.setText(doc.getString("titulo") ?: "")
                txtAutor.setText(doc.getString("autor") ?: "")

                val anio = doc.getLong("anioPublicacion")?.toInt() ?: 0
                if (anio > 0) txtAnio.setText(anio.toString())

                val precio = doc.getDouble("precio") ?: 0.0
                if (precio > 0) txtPrecio.setText(precio.toString())

                idCatSeleccionada = doc.getString("idCategoria") ?: ""
                val posCat = categoriasListaIds.indexOf(idCatSeleccionada)
                if (posCat != -1) {
                    spinnerCategoria.setText(categoriasListaNombres[posCat], false)
                }

                idEdSeleccionada = doc.getString("idEditorial") ?: ""
                val posEd = editorialesListaIds.indexOf(idEdSeleccionada)
                if (posEd != -1) {
                    spinnerEditorial.setText(editorialesListaNombres[posEd], false)
                }

                val portadaB64 = doc.getString("portadaBase64")
                if (!portadaB64.isNullOrEmpty()) {
                    try {
                        val decodedBytes = Base64.decode(portadaB64, Base64.DEFAULT)
                        imagenSeleccionada = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                        imgPreview.setImageBitmap(imagenSeleccionada)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    private fun guardarComic() {
        val titulo = txtTitulo.text.toString().trim()
        val autor = txtAutor.text.toString().trim()
        val anioStr = txtAnio.text.toString().trim()
        val precioStr = txtPrecio.text.toString().trim()
        val uidActual = auth.currentUser?.uid ?: ""

        if (titulo.isEmpty() || autor.isEmpty() || anioStr.isEmpty()) {
            Toast.makeText(this, "Título, autor y año son obligatorios", Toast.LENGTH_SHORT).show()
            return
        }

        val anio = anioStr.toIntOrNull() ?: 0
        if (anio < 1900 || anio > 2027) {
            Toast.makeText(this, "El año debe estar entre 1900 y 2027", Toast.LENGTH_SHORT).show()
            return
        }

        val precio = precioStr.toDoubleOrNull() ?: 0.0

        var portadaBase64 = ""
        if (imagenSeleccionada != null) {
            val outputStream = ByteArrayOutputStream()
            imagenSeleccionada!!.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
            portadaBase64 = Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
        }

        val datos = hashMapOf<String, Any>(
            "uid" to uidActual,
            "titulo" to titulo,
            "autor" to autor,
            "anioPublicacion" to anio,
            "precio" to precio,
            "idCategoria" to idCatSeleccionada,
            "idEditorial" to idEdSeleccionada,
            "portadaBase64" to portadaBase64
        )

        if (idComicEditando == null) {
            db.collection("Comics").add(datos).addOnSuccessListener {
                registrarEventoAnalytics("registro_creado")
                Toast.makeText(this, "Cómic guardado con éxito", Toast.LENGTH_SHORT).show()
                finish()
            }.addOnFailureListener { e ->
                Toast.makeText(this, "Error al guardar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            db.collection("Comics").document(idComicEditando!!).update(datos).addOnSuccessListener {
                registrarEventoAnalytics("registro_actualizado")
                Toast.makeText(this, "Cómic actualizado con éxito", Toast.LENGTH_SHORT).show()
                finish()
            }.addOnFailureListener { e ->
                Toast.makeText(this, "Error al actualizar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun registrarEventoAnalytics(nombreEvento: String) {
        val bundle = Bundle().apply {
            putString("tipo_entidad", "comic")
        }
        oFirebaseAnalytics.logEvent(nombreEvento, bundle)
    }
}