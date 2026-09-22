package com.joaquin.komix

import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.joaquin.komix.Modelos.Categoria
import com.joaquin.komix.Repositorios.CategoriaRepositorio

class ListaCategoriasActivity : AppCompatActivity() {

    private lateinit var contenedor: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_lista_categorias)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        contenedor = findViewById(R.id.contenedorCategorias)
        val btnAgregar = findViewById<Button>(R.id.btnAgregarCategoria)

        dibujarLista()

        btnAgregar.setOnClickListener {
            mostrarDialogoFormulario(null)
        }
    }

    private fun dibujarLista() {
        contenedor.removeAllViews()

        for (categoria in CategoriaRepositorio.listaCategorias) {
            val fila = LinearLayout(this)
            fila.orientation = LinearLayout.HORIZONTAL
            fila.setPadding(24, 16, 24, 16)
            fila.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

            val txtNombre = TextView(this)
            txtNombre.text = categoria.nombre
            txtNombre.textSize = 16f
            txtNombre.setTextColor(android.graphics.Color.WHITE)
            txtNombre.layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            )

            val btnEditar = ImageView(this)
            btnEditar.setImageResource(android.R.drawable.ic_menu_edit)
            val parametrosBtnEditar = LinearLayout.LayoutParams(60, 60)
            parametrosBtnEditar.marginEnd = 24
            btnEditar.layoutParams = parametrosBtnEditar
            btnEditar.setOnClickListener {
                mostrarDialogoFormulario(categoria)
            }

            val btnEliminar = ImageView(this)
            btnEliminar.setImageResource(android.R.drawable.ic_menu_delete)
            btnEliminar.layoutParams = LinearLayout.LayoutParams(60, 60)
            btnEliminar.setOnClickListener {
                confirmarEliminar(categoria)
            }

            fila.addView(txtNombre)
            fila.addView(btnEditar)
            fila.addView(btnEliminar)

            contenedor.addView(fila)
        }
    }

    private fun mostrarDialogoFormulario(categoria: Categoria?) {
        val input = EditText(this)
        input.hint = "Nombre de la categoría"
        if (categoria != null) {
            input.setText(categoria.nombre)
        }

        var tituloDialogo = "Agregar Categoría"
        if (categoria != null) {
            tituloDialogo = "Editar Categoría"
        }

        val clickGuardar = object : DialogInterface.OnClickListener {
            override fun onClick(dialogo: DialogInterface, numeroBoton: Int) {
                val texto = input.text.toString().trim()
                if (texto.isEmpty()) {
                    Toast.makeText(this@ListaCategoriasActivity, "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show()
                } else {
                    if (categoria == null) {
                        var idMasAlto = 0
                        for (c in CategoriaRepositorio.listaCategorias) {
                            if (c.id > idMasAlto) {
                                idMasAlto = c.id
                            }
                        }
                        val nuevoId = idMasAlto + 1
                        CategoriaRepositorio.agregar(Categoria(nuevoId, texto))
                    } else {
                        CategoriaRepositorio.actualizar(Categoria(categoria.id, texto))
                    }
                    dibujarLista()
                }
            }
        }

        AlertDialog.Builder(this)
            .setTitle(tituloDialogo)
            .setView(input)
            .setPositiveButton("Guardar", clickGuardar)
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun confirmarEliminar(categoria: Categoria) {
        val clickEliminarConfirmado = object : DialogInterface.OnClickListener {
            override fun onClick(dialogo: DialogInterface, numeroBoton: Int) {
                CategoriaRepositorio.eliminar(categoria)
                dibujarLista()
            }
        }

        AlertDialog.Builder(this)
            .setTitle("Eliminar categoría")
            .setMessage("¿Seguro que quieres eliminar \"" + categoria.nombre + "\"?")
            .setPositiveButton("Eliminar", clickEliminarConfirmado)
            .setNegativeButton("Cancelar", null)
            .show()
    }
}