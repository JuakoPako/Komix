package com.joaquin.komix.Repositorios

import com.joaquin.komix.Modelos.Comic

object ComicRepositorio {

    val listaComics = mutableListOf(
        Comic(1, "Spider-Man #1", "Stan Lee", 1963, 3, 1, null),
        Comic(2, "Dragon Ball", "Akira Toriyama", 1984, 2, 2, null)
    )

    fun agregar(comic: Comic) {
        listaComics.add(comic)
    }

    fun eliminar(comic: Comic) {
        listaComics.remove(comic)
    }

    fun actualizar(comic: Comic) {
        val index = listaComics.indexOfFirst { it.id == comic.id }
        if (index != -1) {
            listaComics[index] = comic
        }
    }
}