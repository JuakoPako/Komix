package com.joaquin.komix.Modelos

data class Editorial(
    var id: String = "",
    var uid: String = "",
    var nombre: String = "",
    var correoContacto: String = "",
    var paisOrigen: String = "",
    var anioFundacion: Int = 0,
    var estado: String = "Activa"
)