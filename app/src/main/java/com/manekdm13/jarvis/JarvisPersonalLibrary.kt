package com.manekdm13.jarvis

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.provider.DocumentsContract
import android.content.Intent
import android.os.Build

object JarvisPersonalLibrary {
    fun scanGallery(context: Context, limit: Int = 250): String {
        val resolver = context.contentResolver
        val rows = mutableListOf<String>()
        val collections = buildList {
            add(MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            add(MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
        }
        for (collection in collections) {
            val projection = arrayOf(
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.SIZE,
                MediaStore.MediaColumns.DATE_ADDED
            )
            runCatching {
                resolver.query(
                    collection, projection, null, null,
                    "${MediaStore.MediaColumns.DATE_ADDED} DESC"
                )?.use { cursor ->
                    val nameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                    val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                    val dateCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
                    while (cursor.moveToNext() && rows.size < limit) {
                        val name = cursor.getString(nameCol).orEmpty()
                        val bytes = cursor.getLong(sizeCol)
                        val date = cursor.getLong(dateCol)
                        val kind = if (collection == MediaStore.Images.Media.EXTERNAL_CONTENT_URI) "Image" else "Video"
                        rows.add("$kind • $name • ${bytes / 1024} KB • added $date")
                    }
                }
            }
        }
        return if (rows.isEmpty()) {
            "I couldn't find accessible gallery items. Check JARVIS photo and video permissions, or grant access to selected media in Android settings."
        } else {
            "Gallery index (metadata only; no photos or videos were uploaded): ${rows.size} items\n" + rows.joinToString("\n")
        }
    }

    fun savedFolder(context: Context): Uri? =
        context.getSharedPreferences("jarvis_settings", Context.MODE_PRIVATE)
            .getString("library_tree_uri", null)?.let(Uri::parse)

    fun saveFolder(context: Context, uri: Uri) {
        context.contentResolver.takePersistableUriPermission(
            uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        context.getSharedPreferences("jarvis_settings", Context.MODE_PRIVATE)
            .edit().putString("library_tree_uri", uri.toString()).apply()
    }

    fun scanSavedFolder(context: Context, limit: Int = 250): String {
        val tree = savedFolder(context)
            ?: return "No folder has been shared with JARVIS yet. In Connection Settings, choose a folder once."
        return runCatching {
            val rootId = DocumentsContract.getTreeDocumentId(tree)
            val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, rootId)
            val projection = arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_SIZE
            )
            val rows = mutableListOf<String>()
            context.contentResolver.query(children, projection, null, null, null)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                val sizeCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_SIZE)
                while (cursor.moveToNext() && rows.size < limit) {
                    val name = cursor.getString(nameCol).orEmpty()
                    val mime = cursor.getString(mimeCol).orEmpty()
                    val size = if (cursor.isNull(sizeCol)) "" else " • ${cursor.getLong(sizeCol) / 1024} KB"
                    rows.add("${if (mime == DocumentsContract.Document.MIME_TYPE_DIR) "Folder" else "File"} • $name$size")
                }
            }
            if (rows.isEmpty()) "The shared folder is empty or contains no items visible at its top level."
            else "Shared folder index (names and sizes only):\n" + rows.joinToString("\n")
        }.getOrElse { "I couldn't scan that folder: ${it.message ?: "access error"}" }
    }

    fun openFile(context: Context, nameQuery: String): String {
        val tree = savedFolder(context) ?: return "Share a folder with JARVIS first."
        return runCatching {
            val rootId = DocumentsContract.getTreeDocumentId(tree)
            val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, rootId)
            val projection = arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE
            )
            var matchId: String? = null
            var mime = ""
            var displayName = ""
            context.contentResolver.query(children, projection, null, null, null)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                while (cursor.moveToNext()) {
                    val name = cursor.getString(nameCol).orEmpty()
                    if (name.contains(nameQuery, true)) {
                        matchId = cursor.getString(idCol)
                        mime = cursor.getString(mimeCol).orEmpty()
                        displayName = name
                        break
                    }
                }
            }
            val id = matchId ?: return "I couldn't find a top-level file matching '$nameQuery' in the shared folder."
            val uri = DocumentsContract.buildDocumentUriUsingTree(tree, id)
            val view = Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, mime.ifBlank { "*/*" })
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(view)
            "Opened $displayName in its associated Android app."
        }.getOrElse { "I couldn't open that file: ${it.message ?: "unknown error"}" }
    }

    fun readTextFile(context: Context, nameQuery: String): String {
        val tree = savedFolder(context)
            ?: return "Share a folder with JARVIS first."
        return runCatching {
            val rootId = DocumentsContract.getTreeDocumentId(tree)
            val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, rootId)
            val projection = arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE
            )
            var matchId: String? = null
            var mime = ""
            context.contentResolver.query(children, projection, null, null, null)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                while (cursor.moveToNext()) {
                    val name = cursor.getString(nameCol).orEmpty()
                    if (name.contains(nameQuery, true)) {
                        matchId = cursor.getString(idCol)
                        mime = cursor.getString(mimeCol).orEmpty()
                        break
                    }
                }
            }
            val id = matchId ?: return "I couldn't find a top-level file matching '$nameQuery' in the shared folder."
            val uri = DocumentsContract.buildDocumentUriUsingTree(tree, id)
            if (mime.startsWith("text/") || mime == "application/json") {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText().take(24000) }
                    ?: "I couldn't read that file."
            } else {
                context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, mime.ifBlank { "*/*" })
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK))
                "I found the file and asked Android to open it in its associated app. I can directly read plain-text files; PDF and office document parsing needs a dedicated document reader."
            }
        }.getOrElse { "File access failed: ${it.message ?: "unknown error"}" }
    }
}
