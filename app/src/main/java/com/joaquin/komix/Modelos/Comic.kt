package com.joaquin.komix.Modelos

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.google.firebase.firestore.Exclude
import java.io.ByteArrayOutputStream

data class Comic(
    var id: String = "",
    var uid: String = "",
    var titulo: String = "",
    var autor: String = "",
    var anioPublicacion: Int = 0,
    var precio: Double = 0.0,
    var idCategoria: String = "",
    var idEditorial: String = "",
    var portadaBase64: String? = null
) {
    @get:Exclude
    var portadaBitmap: Bitmap?
        get() {
            if (portadaBase64.isNullOrEmpty()) return null
            return try {
                val decodedBytes = Base64.decode(portadaBase64, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
            } catch (e: Exception) {
                null
            }
        }
        set(value) {
            if (value == null) {
                portadaBase64 = null
            } else {
                val outputStream = ByteArrayOutputStream()
                value.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
                val byteArray = outputStream.toByteArray()
                portadaBase64 = Base64.encodeToString(byteArray, Base64.DEFAULT)
            }
        }
}