package com.joaquin.komix.Repositorios

import com.joaquin.komix.Modelos.Categoria

object CategoriaRepositorio {
    val listaCategorias = mutableListOf(
        Categoria(1, "Acción"),
        Categoria(2, "Manga"),
        Categoria(3, "Superhéroes")
    )

    fun agregar(categoria: Categoria) {
        listaCategorias.add(categoria)
    }

    fun eliminar(categoria: Categoria) {
        listaCategorias.remove(categoria)
    }

    fun actualizar(categoria: Categoria) {
        val index = listaCategorias.indexOfFirst { it.id == categoria.id }
        if (index != -1) {
            listaCategorias[index] = categoria
        }
    }
}