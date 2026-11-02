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
    private var guardando = false

    private lateinit var imgPreview: ImageView
    private lateinit var txtTitulo: EditText
    private lateinit var txtAutor: EditText
    private lateinit var txtAnio: EditText
    private lateinit var txtPrecio: EditText
    private lateinit var spinnerCategoria: AutoCompleteTextView
    private lateinit var spinnerEditorial: AutoCompleteTextView
    private lateinit var btnGuardar: Button

    private val categoriasListaIds = ArrayList<String>()
    private val categoriasListaNombres = ArrayList<String>()

    private val editorialesListaIds = ArrayList<String>()
    private val editorialesListaNombres = ArrayList<String>()

    private var idCatSeleccionada: String = ""
    private var idEdSeleccionada: String = ""

    private val selectorImagen = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            try {
                val original = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                val bitmap = reducirBitmap(original)
                imagenSeleccionada = bitmap
                imgPreview.setImageBitmap(bitmap)
            } catch (e: Exception) {
                Voz.aviso(this, "No se pudo cargar la imagen")
            } catch (e: OutOfMemoryError) {
                Voz.aviso(this, "La imagen es demasiado grande")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_formulario_comic)
        Voz.iniciar(this)

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
        btnGuardar = findViewById(R.id.btnGuardar)

        val btnSeleccionarImagen = findViewById<Button>(R.id.btnSeleccionarImagen)
        val btnVolver = findViewById<ImageButton>(R.id.btnVolver)

        btnVolver.setOnClickListener { finish() }

        Voz.hablar(if (intent.getStringExtra("id_comic").isNullOrEmpty()) "Nuevo cómic" else "Editar cómic")

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

    private fun reducirBitmap(origen: Bitmap, maxLado: Int = 800): Bitmap {
        val escala = maxLado.toFloat() / maxOf(origen.width, origen.height)
        if (escala >= 1f) return origen
        val nuevoAncho = (origen.width * escala).toInt().coerceAtLeast(1)
        val nuevoAlto = (origen.height * escala).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(origen, nuevoAncho, nuevoAlto, true)
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
                        } else {
                            Voz.aviso(this, "No se pudieron cargar las editoriales")
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
        }.addOnFailureListener { e ->
            Voz.aviso(this, "Error al cargar el cómic: ${e.message}", "Error al cargar el cómic")
        }
    }

    private fun iniciarGuardado() {
        guardando = true
        btnGuardar.isEnabled = false
        btnGuardar.text = "Guardando..."
    }

    private fun terminarGuardado() {
        guardando = false
        btnGuardar.isEnabled = true
        btnGuardar.text = "Guardar"
    }

    private fun guardarComic() {
        if (guardando) return

        val titulo = txtTitulo.text.toString().trim()
        val autor = txtAutor.text.toString().trim()
        val anioStr = txtAnio.text.toString().trim()
        val precioStr = txtPrecio.text.toString().trim()
        val uidActual = auth.currentUser?.uid ?: ""

        if (titulo.isEmpty() || autor.isEmpty() || anioStr.isEmpty()) {
            Voz.aviso(this, "Título, autor y año son obligatorios")
            return
        }

        val anio = anioStr.toIntOrNull()
        if (anio == null || anio < 1900 || anio > 2027) {
            Voz.aviso(this, "El año debe estar entre 1900 y 2027")
            return
        }

        val precio = if (precioStr.isEmpty()) 0.0 else precioStr.toDoubleOrNull()
        if (precio == null || precio < 0 || precio > 1000000) {
            Voz.aviso(this, "Ingresa un precio válido")
            return
        }

        var portadaBase64 = ""
        if (imagenSeleccionada != null) {
            val outputStream = ByteArrayOutputStream()
            imagenSeleccionada!!.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
            portadaBase64 = Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
        }

        if (portadaBase64.length > 900000) {
            Voz.aviso(this, "La imagen es demasiado pesada, elige otra")
            return
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

        iniciarGuardado()

        if (idComicEditando == null) {
            db.collection("Comics").add(datos).addOnSuccessListener {
                registrarEventoAnalytics("registro_creado")
                Voz.aviso(this, "Cómic guardado con éxito")
                finish()
            }.addOnFailureListener { e ->
                terminarGuardado()
                Voz.aviso(this, "Error al guardar: ${e.message}", "Error al guardar")
            }
        } else {
            db.collection("Comics").document(idComicEditando!!).update(datos).addOnSuccessListener {
                registrarEventoAnalytics("registro_actualizado")
                Voz.aviso(this, "Cómic actualizado con éxito")
                finish()
            }.addOnFailureListener { e ->
                terminarGuardado()
                Voz.aviso(this, "Error al actualizar: ${e.message}", "Error al actualizar")
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