package com.joaquin.komix

import android.content.DialogInterface
import android.os.Bundle
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
import com.joaquin.komix.Modelos.Editorial
import com.joaquin.komix.Repositorios.EditorialRepositorio

class ListaEditorialesActivity : AppCompatActivity() {

    private lateinit var contenedor: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_lista_editoriales)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        contenedor = findViewById(R.id.contenedorEditoriales)
        val btnAgregar = findViewById<Button>(R.id.btnAgregarEditorial)

        dibujarLista()

        btnAgregar.setOnClickListener {
            mostrarDialogoFormulario(null)
        }
    }

    private fun dibujarLista() {
        contenedor.removeAllViews()

        for (editorial in EditorialRepositorio.listaEditoriales) {
            val fila = LinearLayout(this)
            fila.orientation = LinearLayout.HORIZONTAL
            fila.setPadding(24, 16, 24, 16)
            fila.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

            val txtInfo = TextView(this)
            txtInfo.text = editorial.nombre + " · " + editorial.pais
            txtInfo.textSize = 16f
            txtInfo.setTextColor(android.graphics.Color.WHITE)
            txtInfo.layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            )

            val btnEditar = ImageView(this)
            btnEditar.setImageResource(android.R.drawable.ic_menu_edit)
            val parametrosBtnEditar = LinearLayout.LayoutParams(60, 60)
            parametrosBtnEditar.marginEnd = 24
            btnEditar.layoutParams = parametrosBtnEditar
            btnEditar.setOnClickListener {
                mostrarDialogoFormulario(editorial)
            }

            val btnEliminar = ImageView(this)
            btnEliminar.setImageResource(android.R.drawable.ic_menu_delete)
            btnEliminar.layoutParams = LinearLayout.LayoutParams(60, 60)
            btnEliminar.setOnClickListener {
                confirmarEliminar(editorial)
            }

            fila.addView(txtInfo)
            fila.addView(btnEditar)
            fila.addView(btnEliminar)

            contenedor.addView(fila)
        }
    }

    private fun mostrarDialogoFormulario(editorial: Editorial?) {
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(40, 20, 40, 20)

        val inputNombre = EditText(this)
        inputNombre.hint = "Nombre de la editorial"
        if (editorial != null) {
            inputNombre.setText(editorial.nombre)
        }

        val inputPais = EditText(this)
        inputPais.hint = "País"
        if (editorial != null) {
            inputPais.setText(editorial.pais)
        }

        layout.addView(inputNombre)
        layout.addView(inputPais)

        var tituloDialogo = "Agregar Editorial"
        if (editorial != null) {
            tituloDialogo = "Editar Editorial"
        }

        val clickGuardar = object : DialogInterface.OnClickListener {
            override fun onClick(dialogo: DialogInterface, numeroBoton: Int) {
                val nombre = inputNombre.text.toString().trim()
                val pais = inputPais.text.toString().trim()

                if (nombre.isEmpty() || pais.isEmpty()) {
                    Toast.makeText(this@ListaEditorialesActivity, "Completa ambos campos", Toast.LENGTH_SHORT).show()
                } else {
                    if (editorial == null) {
                        var idMasAlto = 0
                        for (e in EditorialRepositorio.listaEditoriales) {
                            if (e.id > idMasAlto) {
                                idMasAlto = e.id
                            }
                        }
                        val nuevoId = idMasAlto + 1
                        EditorialRepositorio.agregar(Editorial(nuevoId, nombre, pais))
                    } else {
                        EditorialRepositorio.actualizar(Editorial(editorial.id, nombre, pais))
                    }
                    dibujarLista()
                }
            }
        }

        AlertDialog.Builder(this)
            .setTitle(tituloDialogo)
            .setView(layout)
            .setPositiveButton("Guardar", clickGuardar)
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun confirmarEliminar(editorial: Editorial) {
        val clickEliminarConfirmado = object : DialogInterface.OnClickListener {
            override fun onClick(dialogo: DialogInterface, numeroBoton: Int) {
                EditorialRepositorio.eliminar(editorial)
                dibujarLista()
            }
        }

        AlertDialog.Builder(this)
            .setTitle("Eliminar editorial")
            .setMessage("¿Seguro que quieres eliminar \"" + editorial.nombre + "\"?")
            .setPositiveButton("Eliminar", clickEliminarConfirmado)
            .setNegativeButton("Cancelar", null)
            .show()
    }
}