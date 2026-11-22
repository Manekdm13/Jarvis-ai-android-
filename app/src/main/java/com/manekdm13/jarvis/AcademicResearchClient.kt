package com.manekdm13.jarvis

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

internal object AcademicResearchClient {
    fun search(baseUrl: String, token: String, query: String, limit: Int = 5): String {
        require(token.isNotBlank()) { "Enter your private gateway access token in JARVIS settings." }
        val cleanBase = baseUrl.trim().trimEnd('/')
        require(cleanBase.startsWith("https://")) { "Enter a valid HTTPS Worker URL in AI gateway URL." }
        val endpoint = "$cleanBase/v1/research/search"
        val payload = JSONObject().put("query", query.take(300)).put("limit", limit.coerceIn(1, 8))
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 35_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Authorization", "Bearer $token")
        }
        try {
            connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            val response = runCatching { JSONObject(body) }.getOrNull()
            if (status !in 200..299) {
                throw IllegalStateException(response?.optString("error")?.takeIf { it.isNotBlank() }
                    ?: "Research service returned HTTP $status.")
            }

            val papers = response?.optJSONArray("papers")
                ?: throw IllegalStateException("Research service returned no paper list.")
            if (papers.length() == 0) return "I couldn't find papers for: $query. Try broader keywords."
            val output = StringBuilder("Research results for: $query\n\n")
            for (index in 0 until minOf(papers.length(), 12)) {
                val paper = papers.optJSONObject(index) ?: continue
                val title = paper.optString("title", "Untitled paper")
                val source = paper.optString("source").takeIf { it.isNotBlank() }
                val year = paper.opt("year")?.takeIf { it !is JSONObject && it != JSONObject.NULL }?.toString()
                val authors = paper.optJSONArray("authors")?.let { array ->
                    (0 until minOf(array.length(), 4)).map { array.optString(it) }.filter { it.isNotBlank() }
                        .joinToString(", ")
                }.orEmpty()
                val abstractText = paper.optString("abstract").takeIf { it.isNotBlank() && it != "null" }
                val venue = paper.optString("venue").takeIf { it.isNotBlank() && it != "null" }
                val doi = paper.optString("doi").takeIf { it.isNotBlank() && it != "null" }
                val citations = paper.opt("citations")?.takeIf { it !is JSONObject && it != JSONObject.NULL }?.toString()
                val link = paper.optString("url").takeIf { it.startsWith("https://") }
                val pdf = paper.optString("openAccessUrl").takeIf { it.startsWith("https://") }
                output.append("${index + 1}. $title")
                if (!year.isNullOrBlank()) output.append(" ($year)")
                output.append("\n")
                if (!source.isNullOrBlank()) output.append("Index: $source\n")
                if (authors.isNotBlank()) output.append("Authors: $authors\n")
                if (!venue.isNullOrBlank()) output.append("Journal/source: $venue\n")
                if (!abstractText.isNullOrBlank()) output.append("Abstract: ${abstractText.take(450)}\n")
                if (!citations.isNullOrBlank()) output.append("Citations: $citations (database count; not a quality score)\n")
                if (!doi.isNullOrBlank()) output.append("DOI: https://doi.org/$doi\n")
                if (!link.isNullOrBlank()) output.append("Paper: $link\n")
                if (!pdf.isNullOrBlank()) output.append("Open-access full text: $pdf\n")
                output.append("\n")
            }
            if (response.optBoolean("partial")) {
                output.append("Some sources were unavailable for this search: ")
                output.append(response.optJSONArray("warnings")?.let { warnings ->
                    (0 until warnings.length()).joinToString("; ") { warnings.optString(it) }
                } ?: "partial results")
                output.append("\n")
            }
            output.append("Sources: Semantic Scholar and OpenAlex. Verify the paper and publication details before citing.")
            return output.toString().trim()
        } finally {
            connection.disconnect()
        }
    }
}
