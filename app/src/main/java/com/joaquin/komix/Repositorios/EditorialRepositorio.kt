package com.joaquin.komix.Repositorios

import com.joaquin.komix.Modelos.Categoria
import com.joaquin.komix.Modelos.Editorial

object EditorialRepositorio {

    val listaEditoriales = mutableListOf(
        Editorial(1, "Marvel Comics", "Estados Unidos"),
        Editorial(2, "DC Comics", "Estados Unidos")
    )

    fun agregar(editorial: Editorial) {
        listaEditoriales.add(editorial)
    }

    fun eliminar(editorial: Editorial) {
        listaEditoriales.remove(editorial)
    }

    fun actualizar(editorial: Editorial) {
        val index = listaEditoriales.indexOfFirst { it.id == editorial.id }
        if (index != -1) {
            listaEditoriales[index] = editorial
        }
    }
}