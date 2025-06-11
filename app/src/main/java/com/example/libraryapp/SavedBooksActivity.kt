package com.example.libraryapp

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.libraryapp.adapters.BookAdapter
import com.example.libraryapp.models.Book
import com.example.libraryapp.models.ToastUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SavedBooksActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: BookAdapter
    private val savedBooks = mutableListOf<Book>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.saved_books)

        recyclerView = findViewById(R.id.savedBooksRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = BookAdapter(savedBooks)
        recyclerView.adapter = adapter

        fetchSavedBooks()
    }

    private fun fetchSavedBooks() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        db.collection("users").document(userId)
            .collection("savedBooks")
            .get()
            .addOnSuccessListener { result ->
                val books = result.toObjects(Book::class.java)
                savedBooks.clear()
                savedBooks.addAll(books)
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                ToastUtils.showCustomToast(this, "Failed to load saved books.")
            }
    }
}
