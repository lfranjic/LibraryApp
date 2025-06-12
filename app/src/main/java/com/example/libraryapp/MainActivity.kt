package com.example.libraryapp

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Xml
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.libraryapp.adapters.BookAdapter
import com.example.libraryapp.models.Book
import com.example.libraryapp.models.GutenbergBook
import com.example.libraryapp.models.ToastUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: BookAdapter
    private val db = FirebaseFirestore.getInstance()

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val logoutButton = findViewById<Button>(R.id.logoutButton)
        logoutButton.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        val searchEditText = findViewById<EditText>(R.id.searchEditText)
        val searchButton = findViewById<Button>(R.id.searchButton)
        recyclerView = findViewById(R.id.bookRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = BookAdapter(
            books = emptyList(),
            onSaveClick = { book -> saveBookToFirestore(book) },
            onReadClick = { book -> openBookInWebView(book) },
            showSaveButton = true,
            showReadButton = true,
            showDeleteButton = true
        )
        recyclerView.adapter = adapter
        val emptyTextView = findViewById<TextView>(R.id.emptyTextView)

        val viewSavedBooksButton = findViewById<Button>(R.id.savedBooksButton)
        viewSavedBooksButton.setOnClickListener {
            fetchSavedBooks()
        }

        searchButton.setOnClickListener {
            val query = searchEditText.text.toString().trim()
            if (query.isNotEmpty()) {
                lifecycleScope.launch {
                    try {
                        val results = searchGutenbergOPDS(query)
                        Log.d("ResultsDebug", "Results: $results")
                        Log.d("SearchDebug", "Books found: ${results.size}")
                        results.forEach {
                            Log.d("SearchDebug", "Title: ${it.title} Author: ${it.author} AuthorId: ${it.authorId} Link: ${it.link}")
                        }

                        val books = results.map { gb ->
                            async {
                                val bookId = gb.link.substringAfterLast("/ebooks/").substringBefore(".")
                                val authorName = fetchAuthorNameById(bookId)
                                val htmlUrl = "https://www.gutenberg.org/ebooks/$bookId.html.images"

                                Book(
                                    title = gb.title,
                                    author = authorName,
                                    htmlUrl = htmlUrl,
                                    id = bookId
                                )
                            }
                        }.awaitAll()

                        if (books.isEmpty()) {
                            emptyTextView.visibility = View.VISIBLE
                            recyclerView.visibility = View.GONE

                            adapter.updateList(
                                emptyList(),
                                showSaveButton = true,
                                showReadButton = false,
                                onSaveClick = { book -> saveBookToFirestore(book) },
                                onReadClick = { book -> openBookInWebView(book) }
                            )
                        } else {
                            emptyTextView.visibility = View.GONE
                            recyclerView.visibility = View.VISIBLE
                            adapter.updateList(
                                books,
                                showSaveButton = true,
                                showReadButton = false,
                                onSaveClick = { book -> saveBookToFirestore(book) },
                                onReadClick = { book -> openBookInWebView(book) }
                            )
                        }
                    } catch (e: Exception) {
                        Log.e("FetchDebug", "Error fetching books", e)
                        ToastUtils.showCustomToast(this@MainActivity, "Failed to fetch books.")
                    }
                }
            }
        }
    }

    private fun openBookInWebView(book: Book) {
        var url = book.htmlUrl.trim()

        if (url.startsWith("http://")) {
            url = url.replaceFirst("http://", "https://")
        }

        if (url.isNotBlank()) {
            val intent = Intent(this, WebViewActivity::class.java)
            intent.putExtra("url", url)
            startActivity(intent)
        } else {
            ToastUtils.showCustomToast(this, "No readable URL found for this book.")
        }
    }

    private suspend fun searchGutenbergOPDS(query: String): List<GutenbergBook> = withContext(Dispatchers.IO) {
        val url = URL("https://www.gutenberg.org/ebooks/search.opds/?query=${URLEncoder.encode(query, "UTF-8")}")
        val connection = url.openConnection() as HttpURLConnection

        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(connection.inputStream, null)

        val books = mutableListOf<GutenbergBook>()

        var title = ""
        var author = ""
        var authorId: String? = null
        var link = ""
        var insideEntry = false
        var insideAuthor = false

        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "entry" -> {
                            insideEntry = true
                            title = ""
                            author = ""
                            link = ""
                            authorId = null
                        }
                        "title" -> {
                            if (insideEntry){
                                title = parser.nextText().trim()
                            }
                        }
                        "author" -> {
                            if (insideEntry){
                                insideAuthor = true
                            }
                        }
                        "name" -> {
                            if (insideEntry && insideAuthor) {
                                author = parser.nextText().trim()
                            }
                        }
                        "uri" -> {
                            if (insideEntry && insideAuthor) {
                                val uriText = parser.nextText()
                                if (uriText.contains("/ebooks/author/")) {
                                    authorId = uriText.substringAfter("/ebooks/author/").substringBefore("/")
                                    Log.d("ExtractDebug", "Extracted authorId: $authorId")
                                }
                            }
                        }
                        "link" -> {
                            if (insideEntry && link.isBlank()) {
                                val href = parser.getAttributeValue(null, "href")
                                if (!href.isNullOrBlank() && href.contains("/ebooks/")) {
                                    link = href.trim()
                                }
                            }
                        }
                    }
                }

                XmlPullParser.END_TAG -> {
                    when (parser.name) {
                        "entry" -> {
                            insideEntry = false
                            val bookId = link.substringAfterLast("/ebooks/").substringBefore(".opds").toIntOrNull()

                            if (title.isNotBlank() && link.isNotBlank() && bookId != null) {
                                books.add(GutenbergBook(title, author.ifBlank { "Unknown author" }, link, authorId))
                                Log.d("XML_ParseDebug", "Added book: $title by ${author.ifBlank { "Unknown author" }}")
                            } else {
                                Log.d("XML_ParseDebug", "Skipped: title='$title', link='$link', bookId=$bookId")
                            }
                        }
                        "author" -> insideAuthor = false
                    }
                }
            }
            parser.next()
        }

        connection.disconnect()
        Log.d("XML_ParseDebug", "Finished parsing. Found ${books.size} books.")
        return@withContext books
    }

    private suspend fun fetchAuthorNameById(bookId: String): String = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://www.gutenberg.org/ebooks/$bookId")
            val html = url.openConnection().getInputStream().bufferedReader().use { it.readText() }

            val regex = Regex("""<th[^>]*>\s*Author\s*</th>\s*<td[^>]*>\s*<a[^>]*>(.*?)</a>""", RegexOption.IGNORE_CASE)
            regex.find(html)?.groups?.get(1)?.value?.trim() ?: "Unknown author"
        } catch (e: Exception) {
            Log.e("FetchDebug", "Error fetching author", e)
            "Unknown author"
        }
    }

    private fun saveBookToFirestore(book: Book) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        val bookId = book.id ?: return

        val docRef = db.collection("users")
            .document(userId)
            .collection("savedBooks")
            .document(bookId)

        docRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    ToastUtils.showCustomToast(this, "Book already saved")
                } else {
                    docRef.set(book)
                        .addOnSuccessListener {
                            ToastUtils.showCustomToast(this, "Book saved")
                        }
                        .addOnFailureListener {
                            ToastUtils.showCustomToast(this, "Failed to save book")
                        }
                }
            }
            .addOnFailureListener {
                ToastUtils.showCustomToast(this, "Failed to fetch saved books")
            }
    }

    private fun fetchSavedBooks() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        db.collection("users").document(userId)
            .collection("savedBooks")
            .get()
            .addOnSuccessListener { result ->
                val saved = result.toObjects(Book::class.java)

                if (saved.isEmpty()) {
                    ToastUtils.showCustomToast(this, "No saved books found")
                }

                adapter.updateList(
                    saved,
                    showSaveButton = false,
                    showReadButton = true,
                    showDeleteButton = true,
                    onSaveClick = null,
                    onReadClick = { book -> openBookInWebView(book) },
                    onDeleteClick = { book ->
                        deleteBookFromSavedBooks(userId, book.id!!)
                    }
                )
            }
            .addOnFailureListener {
                ToastUtils.showCustomToast(this, "Failed to fetch saved books")
            }
    }


    private fun deleteBookFromSavedBooks(userId: String, bookId: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection("users")
            .document(userId)
            .collection("savedBooks")
            .document(bookId)
            .delete()
            .addOnSuccessListener {
                Log.d("FIRESTORE", "✅ Deleted book: $bookId")

                val updatedList = adapter.books.filter { it.id != bookId }
                adapter.updateList(
                    updatedList,
                    showSaveButton = false,
                    showReadButton = true,
                    showDeleteButton = true,
                    onReadClick = { book -> openBookInWebView(book) },
                    onDeleteClick = { book ->
                        if (book.id != null) deleteBookFromSavedBooks(userId, book.id!!)
                    }
                )
            }
            .addOnFailureListener {
                Log.e("DeleteDebug", "Failed to delete book: $bookId", it)
            }
    }
}