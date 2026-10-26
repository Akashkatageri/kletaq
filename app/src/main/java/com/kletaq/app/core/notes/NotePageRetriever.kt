package com.kletaq.app.core.notes

data class RetrievedContext(
    val pages: List<PageOcrRecord>,
    val hasSufficientEvidence: Boolean,
    val formattedExcerpts: String,
    val citedPageIndices: Set<Int>
)

/**
 * Local keyword and token ranker that retrieves the most relevant note pages
 * for a student query, preventing token overload and context dilution.
 */
object NotePageRetriever {

    private val STOPWORDS = setOf(
        "a", "an", "the", "and", "or", "but", "if", "because", "as", "what",
        "which", "this", "that", "these", "those", "then", "just", "so", "than",
        "such", "both", "through", "about", "for", "is", "of", "while", "during",
        "to", "from", "in", "out", "on", "off", "again", "further", "then", "once",
        "here", "there", "when", "where", "why", "how", "all", "any", "both", "each",
        "few", "more", "most", "other", "some", "such", "no", "nor", "not", "only",
        "own", "same", "so", "than", "too", "very", "can", "will", "just", "don",
        "should", "now", "explain", "describe", "define", "what", "how", "give", "me"
    )

    /**
     * Retrieves the top relevant pages for a query using keyword ranking.
     * If the total number of pages is 4 or less, all pages are included.
     */
    fun retrieveTopPages(
        query: String,
        topicTitle: String,
        manifest: ModuleOcrManifest,
        maxPages: Int = 2
    ): RetrievedContext {
        val allPages = manifest.pages.filter { it.text.isNotBlank() }
        if (allPages.isEmpty()) {
            return RetrievedContext(emptyList(), false, "", emptySet())
        }

        // If 2 or fewer pages exist, include all of them directly
        if (allPages.size <= maxPages) {
            val excerpts = allPages.joinToString("\n\n") { page ->
                val pageText = if (page.text.length > 1500) page.text.take(1500) + "…" else page.text
                "[Page ${page.pageIndex}]:\n$pageText"
            }
            return RetrievedContext(
                pages = allPages,
                hasSufficientEvidence = true,
                formattedExcerpts = excerpts,
                citedPageIndices = allPages.map { it.pageIndex }.toSet()
            )
        }

        // Tokenize query + topic title into meaningful terms
        val combinedTerms = (query + " " + topicTitle)
            .lowercase()
            .split(Regex("[^a-z0-9_]"))
            .filter { it.length > 2 && it !in STOPWORDS }
            .distinct()

        if (combinedTerms.isEmpty()) {
            // General question with no specific keywords -> take first few pages
            val fallback = allPages.take(maxPages)
            val excerpts = fallback.joinToString("\n\n") { page ->
                "[Page ${page.pageIndex}]:\n${page.text}"
            }
            return RetrievedContext(
                pages = fallback,
                hasSufficientEvidence = true,
                formattedExcerpts = excerpts,
                citedPageIndices = fallback.map { it.pageIndex }.toSet()
            )
        }

        // Score each page
        val scoredPages = allPages.map { page ->
            val pageTextLower = page.text.lowercase()
            var score = 0
            for (term in combinedTerms) {
                val occurrences = Regex.escape(term).toRegex().findAll(pageTextLower).count()
                score += occurrences * 2
            }
            // Bonus for phrase match
            if (query.length > 4 && pageTextLower.contains(query.lowercase())) {
                score += 10
            }
            page to score
        }

        val topScored = scoredPages
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .take(maxPages)
            .map { it.first }
            .ifEmpty { allPages.take(maxPages) }

        // Sort by chronological page order for coherent context reading
        val sortedTop = topScored.sortedBy { it.pageIndex }
        val excerpts = sortedTop.joinToString("\n\n") { page ->
            val pageText = if (page.text.length > 1500) page.text.take(1500) + "…" else page.text
            "[Page ${page.pageIndex}]:\n$pageText"
        }

        return RetrievedContext(
            pages = sortedTop,
            hasSufficientEvidence = true,
            formattedExcerpts = excerpts,
            citedPageIndices = sortedTop.map { it.pageIndex }.toSet()
        )
    }
}
