package com.joaquin.komix

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.joaquin.komix.Modelos.Comic
import com.joaquin.komix.Repositorios.CategoriaRepositorio
import com.joaquin.komix.Repositorios.ComicRepositorio
import com.joaquin.komix.Repositorios.EditorialRepositorio

class FormularioComicActivity : AppCompatActivity() {

    private var imagenSeleccionada: Bitmap? = null
    private var comicEditando: Comic? = null

    private lateinit var imgPreview: ImageView
    private lateinit var txtTitulo: EditText
    private lateinit var txtAutor: EditText
    private lateinit var txtAnio: EditText
    private lateinit var spinnerCategoria: AutoCompleteTextView
    private lateinit var spinnerEditorial: AutoCompleteTextView

    // Como AutoCompleteTextView no tiene selectedItemPosition, guardamos la posición elegida a mano
    private var categoriaSeleccionadaPos: Int = -1
    private var editorialSeleccionadaPos: Int = -1

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

        imgPreview = findViewById(R.id.imgPreview)
        txtTitulo = findViewById(R.id.txtTitulo)
        txtAutor = findViewById(R.id.txtAutor)
        txtAnio = findViewById(R.id.txtAnio)
        spinnerCategoria = findViewById(R.id.spinnerCategoria)
        spinnerEditorial = findViewById(R.id.spinnerEditorial)
        val btnSeleccionarImagen = findViewById<Button>(R.id.btnSeleccionarImagen)
        val btnGuardar = findViewById<Button>(R.id.btnGuardar)

        val nombresCategorias = mutableListOf<String>()
        for (categoria in CategoriaRepositorio.listaCategorias) {
            nombresCategorias.add(categoria.nombre)
        }
        val adapterCategoria = ArrayAdapter(this, R.layout.item_dropdown, nombresCategorias)
        spinnerCategoria.setAdapter(adapterCategoria)
        spinnerCategoria.onItemClickListener = object : AdapterView.OnItemClickListener {
            override fun onItemClick(adapterView: AdapterView<*>?, vista: android.view.View?, posicion: Int, id: Long) {
                categoriaSeleccionadaPos = posicion
            }
        }

        val nombresEditoriales = mutableListOf<String>()
        for (editorial in EditorialRepositorio.listaEditoriales) {
            nombresEditoriales.add(editorial.nombre)
        }
        val adapterEditorial = ArrayAdapter(this, R.layout.item_dropdown, nombresEditoriales)
        spinnerEditorial.setAdapter(adapterEditorial)
        spinnerEditorial.onItemClickListener = object : AdapterView.OnItemClickListener {
            override fun onItemClick(adapterView: AdapterView<*>?, vista: android.view.View?, posicion: Int, id: Long) {
                editorialSeleccionadaPos = posicion
            }
        }

        val idComic = intent.getIntExtra("id_comic", -1)
        if (idComic != -1) {
            var comicEncontrado: Comic? = null
            for (c in ComicRepositorio.listaComics) {
                if (c.id == idComic) {
                    comicEncontrado = c
                }
            }
            comicEditando = comicEncontrado

            if (comicEditando != null) {
                val comic = comicEditando!!

                txtTitulo.setText(comic.titulo)
                txtAutor.setText(comic.autor)
                txtAnio.setText(comic.anioPublicacion.toString())

                var posCategoria = -1
                for (i in CategoriaRepositorio.listaCategorias.indices) {
                    if (CategoriaRepositorio.listaCategorias[i].id == comic.idCategoria) {
                        posCategoria = i
                    }
                }
                if (posCategoria != -1) {
                    spinnerCategoria.setText(nombresCategorias[posCategoria], false)
                    categoriaSeleccionadaPos = posCategoria
                }

                var posEditorial = -1
                for (i in EditorialRepositorio.listaEditoriales.indices) {
                    if (EditorialRepositorio.listaEditoriales[i].id == comic.idEditorial) {
                        posEditorial = i
                    }
                }
                if (posEditorial != -1) {
                    spinnerEditorial.setText(nombresEditoriales[posEditorial], false)
                    editorialSeleccionadaPos = posEditorial
                }

                if (comic.portadaBitmap != null) {
                    imagenSeleccionada = comic.portadaBitmap
                    imgPreview.setImageBitmap(comic.portadaBitmap)
                }
            }
        }

        btnSeleccionarImagen.setOnClickListener {
            selectorImagen.launch("image/*")
        }

        btnGuardar.setOnClickListener {
            guardarComic()
        }
    }

    private fun guardarComic() {
        val titulo = txtTitulo.text.toString().trim()
        val autor = txtAutor.text.toString().trim()
        val anioTexto = txtAnio.text.toString().trim()

        if (titulo.isEmpty() || autor.isEmpty() || anioTexto.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        if (categoriaSeleccionadaPos == -1 || editorialSeleccionadaPos == -1) {
            Toast.makeText(this, "Selecciona categoría y editorial", Toast.LENGTH_SHORT).show()
            return
        }

        val anio = anioTexto.toIntOrNull()
        if (anio == null) {
            Toast.makeText(this, "El año debe ser un número", Toast.LENGTH_SHORT).show()
            return
        }

        val categoriaSeleccionada = CategoriaRepositorio.listaCategorias[categoriaSeleccionadaPos]
        val editorialSeleccionada = EditorialRepositorio.listaEditoriales[editorialSeleccionadaPos]

        if (comicEditando == null) {
            var idMasAlto = 0
            for (c in ComicRepositorio.listaComics) {
                if (c.id > idMasAlto) {
                    idMasAlto = c.id
                }
            }
            val nuevoId = idMasAlto + 1

            val nuevoComic = Comic(
                nuevoId, titulo, autor, anio,
                categoriaSeleccionada.id, editorialSeleccionada.id,
                imagenSeleccionada
            )
            ComicRepositorio.agregar(nuevoComic)
        } else {
            val comicActualizado = Comic(
                comicEditando!!.id, titulo, autor, anio,
                categoriaSeleccionada.id, editorialSeleccionada.id,
                imagenSeleccionada
            )
            ComicRepositorio.actualizar(comicActualizado)
        }

        finish()
    }
}