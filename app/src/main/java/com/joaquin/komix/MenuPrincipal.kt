package com.joaquin.komix

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MenuPrincipal : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_menu_principal)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val cardComics = findViewById<androidx.cardview.widget.CardView>(R.id.cardComics)
        val cardCategorias = findViewById<androidx.cardview.widget.CardView>(R.id.cardCategorias)
        val cardEditoriales = findViewById<androidx.cardview.widget.CardView>(R.id.cardEditoriales)
        val cardCerrarSesion = findViewById<androidx.cardview.widget.CardView>(R.id.cardCerrarSesion)

        cardComics.setOnClickListener {
            startActivity(Intent(this, ListaComicsActivity::class.java))
        }

        cardCategorias.setOnClickListener {
            startActivity(Intent(this, ListaCategoriasActivity::class.java))
        }

        cardEditoriales.setOnClickListener {
            startActivity(Intent(this, ListaEditorialesActivity::class.java))
        }

        cardCerrarSesion.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}