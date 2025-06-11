package com.example.libraryapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.libraryapp.R
import com.example.libraryapp.models.Book

class BookAdapter(
    var books: List<Book>,
    private var onSaveClick: ((Book) -> Unit)? = null,
    private var onReadClick: ((Book) -> Unit)? = null,
    private var onDeleteClick: ((Book) -> Unit)? = null,
    private var showSaveButton: Boolean = false,
    private var showReadButton: Boolean = false,
    private var showDeleteButton: Boolean = false
) : RecyclerView.Adapter<BookAdapter.BookViewHolder>() {

    class BookViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleText: TextView = itemView.findViewById(R.id.bookTitle)
        val authorText: TextView = itemView.findViewById(R.id.bookAuthor)
        val saveButton: Button = itemView.findViewById(R.id.saveButton)
        val readButton: Button = itemView.findViewById(R.id.readButton)
        val deleteButton: Button = itemView.findViewById(R.id.deleteButton)
    }

    fun currentBooks(): List<Book> = books

    fun updateList(
        newBooks: List<Book>,
        showSaveButton: Boolean = false,
        showReadButton: Boolean = false,
        showDeleteButton: Boolean = false,
        onSaveClick: ((Book) -> Unit)? = null,
        onReadClick: ((Book) -> Unit)? = null,
        onDeleteClick: ((Book) -> Unit)? = null
    ) {
        this.books = newBooks
        this.showSaveButton = showSaveButton
        this.showDeleteButton = showDeleteButton
        this.showReadButton = showReadButton
        this.onSaveClick = onSaveClick
        this.onReadClick = onReadClick
        this.onDeleteClick = onDeleteClick
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.book_item, parent, false)
        return BookViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        val book = books[position]
        holder.titleText.text = book.title
        holder.authorText.text = if (book.author.isNotBlank()) "by ${book.author}" else ""

        holder.saveButton.setOnClickListener(null)
        holder.readButton.setOnClickListener(null)
        holder.deleteButton.setOnClickListener(null)

        if (showSaveButton && onSaveClick != null) {
            holder.saveButton.visibility = View.VISIBLE
            holder.saveButton.setOnClickListener { onSaveClick?.invoke(book) }
        } else {
            holder.saveButton.visibility = View.GONE
        }

        if (showReadButton && onReadClick != null && book.htmlUrl.isNotBlank()) {
            holder.readButton.visibility = View.VISIBLE
            holder.readButton.setOnClickListener { onReadClick?.invoke(book) }
        } else {
            holder.readButton.visibility = View.GONE
        }

        if (showDeleteButton && onDeleteClick != null) {
            holder.deleteButton.visibility = View.VISIBLE
            holder.deleteButton.setOnClickListener { onDeleteClick?.invoke(book) }
        } else {
            holder.deleteButton.visibility = View.GONE
            holder.deleteButton.setOnClickListener(null)
        }
    }

    override fun getItemCount() = books.size
}
