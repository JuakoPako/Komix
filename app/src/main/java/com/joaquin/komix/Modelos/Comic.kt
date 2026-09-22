package com.joaquin.komix.Modelos

import android.graphics.Bitmap

data class Comic(
    val id: Int,
    var titulo: String,
    var autor: String,
    var anioPublicacion: Int,
    var idCategoria: Int,
    var idEditorial: Int,
    var portadaBitmap: Bitmap? = null
)