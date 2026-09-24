package com.joaquin.komix

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.joaquin.komix.Modelos.Comic
import com.joaquin.komix.Repositorios.CategoriaRepositorio
import com.joaquin.komix.Repositorios.ComicRepositorio
import com.joaquin.komix.Repositorios.EditorialRepositorio

class ListaComicsActivity : AppCompatActivity() {

    private lateinit var contenedor: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_lista_comics)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        contenedor = findViewById(R.id.contenedorComics)
        val btnAgregar = findViewById<Button>(R.id.btnAgregarComic)

        btnAgregar.setOnClickListener {
            startActivity(Intent(this, FormularioComicActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        dibujarLista()
    }


    //pa q se vea bonito
    private fun dibujarLista() {
        contenedor.removeAllViews()

        for (comic in ComicRepositorio.listaComics) {


            val fila = android.widget.FrameLayout(this)
            val parametrosFila = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 220
            )
            parametrosFila.setMargins(0, 0, 0, 12)
            fila.layoutParams = parametrosFila
            fila.clipToOutline = false


            val imgFondo = ImageView(this)
            imgFondo.scaleType = ImageView.ScaleType.CENTER_CROP
            imgFondo.layoutParams = android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT
            )
            imgFondo.alpha = 0.7f

            if (comic.portadaBitmap != null) {
                imgFondo.setImageBitmap(comic.portadaBitmap)
            } else {
                imgFondo.setImageResource(android.R.drawable.ic_menu_gallery)
            }


            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                imgFondo.setRenderEffect(
                    android.graphics.RenderEffect.createBlurEffect(
                        30f, 30f, android.graphics.Shader.TileMode.CLAMP
                    )
                )
            }


            val overlay = View(this)
            overlay.setBackgroundColor(android.graphics.Color.parseColor("#60000000"))
            overlay.layoutParams = android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT
            )


            val filaContenido = LinearLayout(this)
            filaContenido.orientation = LinearLayout.HORIZONTAL
            filaContenido.gravity = android.view.Gravity.CENTER_VERTICAL
            filaContenido.setPadding(16, 16, 16, 16)
            filaContenido.layoutParams = android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT
            )

            val imgPortada = ImageView(this)
            val parametrosImagen = LinearLayout.LayoutParams(150, 150)
            parametrosImagen.marginEnd = 16
            imgPortada.layoutParams = parametrosImagen
            imgPortada.scaleType = ImageView.ScaleType.CENTER_CROP

            if (comic.portadaBitmap != null) {
                imgPortada.setImageBitmap(comic.portadaBitmap)
            } else {
                imgPortada.setImageResource(android.R.drawable.ic_menu_gallery)
            }

            val infoLayout = LinearLayout(this)
            infoLayout.orientation = LinearLayout.VERTICAL
            infoLayout.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

            val txtTitulo = TextView(this)
            txtTitulo.text = comic.titulo
            txtTitulo.textSize = 16f
            txtTitulo.setTextColor(android.graphics.Color.WHITE)

            var nombreCategoria = ""
            for (categoria in CategoriaRepositorio.listaCategorias) {
                if (categoria.id == comic.idCategoria) {
                    nombreCategoria = categoria.nombre
                }
            }

            var nombreEditorial = ""
            for (editorial in EditorialRepositorio.listaEditoriales) {
                if (editorial.id == comic.idEditorial) {
                    nombreEditorial = editorial.nombre
                }
            }

            val txtSubtitulo = TextView(this)
            txtSubtitulo.text = nombreEditorial + " · " + nombreCategoria
            txtSubtitulo.textSize = 13f
            txtSubtitulo.setTextColor(android.graphics.Color.LTGRAY)

            infoLayout.addView(txtTitulo)
            infoLayout.addView(txtSubtitulo)

            val btnEditar = ImageView(this)
            btnEditar.setImageResource(android.R.drawable.ic_menu_edit)
            val parametrosBtnEditar = LinearLayout.LayoutParams(60, 60)
            parametrosBtnEditar.marginEnd = 16
            btnEditar.layoutParams = parametrosBtnEditar
            btnEditar.setOnClickListener {
                val intent = Intent(this, FormularioComicActivity::class.java)
                intent.putExtra("id_comic", comic.id)
                startActivity(intent)
            }

            val btnEliminar = ImageView(this)
            btnEliminar.setImageResource(android.R.drawable.ic_menu_delete)
            btnEliminar.layoutParams = LinearLayout.LayoutParams(60, 60)
            btnEliminar.setOnClickListener {
                confirmarEliminar(comic)
            }

            filaContenido.addView(imgPortada)
            filaContenido.addView(infoLayout)
            filaContenido.addView(btnEditar)
            filaContenido.addView(btnEliminar)


            fila.addView(imgFondo)
            fila.addView(overlay)
            fila.addView(filaContenido)

            contenedor.addView(fila)
        }
    }

    private fun confirmarEliminar(comic: Comic) {
        val clickEliminarConfirmado = object : DialogInterface.OnClickListener {
            override fun onClick(dialogo: DialogInterface, numeroBoton: Int) {
                ComicRepositorio.eliminar(comic)
                dibujarLista()
            }
        }

        AlertDialog.Builder(this)
            .setTitle("Eliminar cómic")
            .setMessage("¿Seguro que quieres eliminar \"" + comic.titulo + "\"?")
            .setPositiveButton("Eliminar", clickEliminarConfirmado)
            .setNegativeButton("Cancelar", null)
            .show()
    }
}