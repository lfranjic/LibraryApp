package com.example.libraryapp.models

data class GutenbergBook(
    val title: String,
    val author: String,
    val link: String,
    val authorId: String? = null
){
    val bookId: String
        get() = link.substringAfterLast("/").substringBefore(".opds")
}