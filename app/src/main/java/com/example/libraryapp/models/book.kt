package com.example.libraryapp.models

data class Book(
    val title: String = "",
    val author: String = "",
    val htmlUrl: String = "",
    var id: String? = null
)