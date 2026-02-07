package ceui.lisa.jcstaff.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ceui.lisa.jcstaff.network.AmWork
import ceui.lisa.jcstaff.network.ArticleContent
import ceui.lisa.jcstaff.network.SpotlightArticle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

data class SpotlightDetailState(
    val article: SpotlightArticle? = null,
    val description: String = "",
    val amWorks: List<AmWork> = emptyList(),
    val contentBlocks: List<ArticleContent> = emptyList(),
    val relatedArticles: List<ArticleContent.RelatedArticle> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * ViewModel for Spotlight article detail page
 * Parses HTML content from pixivision.net to extract artwork list
 */
class SpotlightDetailViewModel(
    private val article: SpotlightArticle
) : ViewModel() {

    private val _state = MutableStateFlow(SpotlightDetailState(article = article))
    val state: StateFlow<SpotlightDetailState> = _state.asStateFlow()

    init {
        fetch()
    }

    fun fetch() {
        val url = article.article_url ?: return

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            try {
                val result = withContext(Dispatchers.IO) {
                    parseArticle(url)
                }
                _state.value = _state.value.copy(
                    description = result.description,
                    amWorks = result.contentBlocks.filterIsInstance<ArticleContent.Work>()
                        .map { it.amWork },
                    contentBlocks = result.contentBlocks,
                    relatedArticles = result.relatedArticles,
                    isLoading = false
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load article"
                )
            }
        }
    }

    private data class ParseResult(
        val description: String,
        val contentBlocks: List<ArticleContent>,
        val relatedArticles: List<ArticleContent.RelatedArticle>
    )

    private fun parseArticle(url: String): ParseResult {
        val document = Jsoup.connect(url)
            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .referrer("https://www.pixivision.net/")
            .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8,ja;q=0.7")
            .get()

        val contentBlocks = mutableListOf<ArticleContent>()
        var description = ""

        // Find article body
        val articleElement = document.getElementsByTag("article").firstOrNull()
            ?: return ParseResult("", emptyList(), emptyList())
        val amBody = articleElement.getElementsByClass("am__body").firstOrNull()
            ?: return ParseResult("", emptyList(), emptyList())

        val nodes = amBody.children()
        if (nodes.isEmpty()) return ParseResult("", emptyList(), emptyList())

        // Check if it's a feature article
        val firstNode = nodes.first()
        val workNodes: List<Element>
        if (firstNode?.classNames()?.any { it.contains("_feature") } == true) {
            workNodes = firstNode.children()
        } else {
            // Extract description from header
            val header = articleElement.getElementsByTag("header").firstOrNull()
            description = header?.getElementsByTag("p")
                ?.joinToString("\n") { it.text() }
                ?: ""
            workNodes = nodes
        }

        // Parse each node in order to build mixed content blocks
        for (node in workNodes) {
            try {
                val classAttr = node.classNames().joinToString(" ")

                // Skip eyecatch, clearfix, thumbnail-only nodes
                if (classAttr.contains("eyecatch") ||
                    classAttr.contains("clearfix") ||
                    classAttr.contains("ad-") ||
                    classAttr.contains("_ad")) continue

                // Paragraph nodes
                if (classAttr.contains("_paragraph") || classAttr.contains("am__description")) {
                    val paragraphTexts = node.getElementsByTag("p")
                        .map { it.text().trim() }
                        .filter { it.isNotBlank() }
                    for (text in paragraphTexts) {
                        contentBlocks.add(ArticleContent.Paragraph(text))
                    }
                    continue
                }

                // Artwork/illust nodes
                if (classAttr.contains("illust")) {
                    val amWork = parseWorkNode(node)
                    if (amWork != null) {
                        contentBlocks.add(ArticleContent.Work(amWork))
                    }
                    continue
                }
            } catch (e: Exception) {
                // Skip malformed entries
                continue
            }
        }

        // Parse related articles
        val relatedArticles = parseRelatedArticles(document)

        return ParseResult(description, contentBlocks, relatedArticles)
    }

    private fun parseWorkNode(node: Element): AmWork? {
        var artworkLink: String? = null
        var showImage: String? = null
        var title: String? = null
        var userLink: String? = null
        var user: String? = null
        var userImage: String? = null
        var pageCount = 1

        // Extract page count from mic__label (e.g., "2P", "3P")
        val micLabel = node.getElementsByClass("mic__label").firstOrNull()
        if (micLabel != null) {
            val labelText = micLabel.text().trim()
            val match = Regex("(\\d+)").find(labelText)
            if (match != null) {
                pageCount = match.groupValues[1].toIntOrNull() ?: 1
            }
        }

        // Find all links in this node
        for (a in node.getElementsByTag("a")) {
            val href = a.attr("href")
            if (href.isBlank()) continue

            when {
                // Artwork link
                href.contains("/artworks/") -> {
                    artworkLink = href
                    // Get artwork image (usually the second img in the element)
                    val imgs = node.getElementsByTag("img")
                    if (imgs.size > 1) {
                        showImage = imgs[1].attr("src").ifBlank { imgs[1].attr("data-src") }
                    }
                    // Get title from h3
                    title = node.getElementsByTag("h3").firstOrNull()?.text()
                }
                // User link
                href.contains("/users/") -> {
                    userLink = href
                    user = node.getElementsByTag("p").firstOrNull()?.text()
                    // User avatar is usually the first img
                    val imgs = node.getElementsByTag("img")
                    if (imgs.isNotEmpty()) {
                        userImage = imgs.first()?.attr("src")
                            ?.ifBlank { imgs.first()?.attr("data-src") }
                    }
                }
            }
        }

        // Only return if we have both artwork and user info
        return if (artworkLink != null && userLink != null &&
            showImage != null && userImage != null &&
            title != null && user != null) {
            AmWork(
                title = title,
                user = user,
                artworkLink = artworkLink,
                userLink = userLink,
                userImage = userImage,
                showImage = showImage,
                pageCount = pageCount
            )
        } else {
            null
        }
    }

    private fun parseRelatedArticles(document: org.jsoup.nodes.Document): List<ArticleContent.RelatedArticle> {
        val related = mutableListOf<ArticleContent.RelatedArticle>()

        // Look for related articles section
        val relatedSection = document.getElementsByClass("_related-articles").firstOrNull()
            ?: document.select("[class*=related-article]").firstOrNull()
            ?: return emptyList()

        // Find article links within the related section
        val articleItems = relatedSection.select("a[href*=pixivision]")
        if (articleItems.isEmpty()) {
            // Try broader selector
            for (item in relatedSection.children()) {
                val link = item.getElementsByTag("a").firstOrNull() ?: continue
                val href = link.attr("href")
                if (href.isBlank()) continue

                val titleEl = item.select(".ascr__title, .arsc__title, h3, h4").firstOrNull()
                    ?: link
                val title = titleEl.text().trim()
                if (title.isBlank()) continue

                val thumbnailImg = item.getElementsByTag("img").firstOrNull()
                val thumbnailUrl = thumbnailImg?.attr("src")?.ifBlank {
                    thumbnailImg.attr("data-src")
                }

                related.add(ArticleContent.RelatedArticle(
                    title = title,
                    url = href,
                    thumbnailUrl = thumbnailUrl
                ))
            }
        } else {
            for (link in articleItems) {
                val href = link.attr("href")
                if (href.isBlank()) continue

                // Find title - try dedicated title element first, then link text
                val parent = link.parent() ?: link
                val titleEl = parent.select(".ascr__title, .arsc__title, h3, h4").firstOrNull()
                val title = (titleEl?.text() ?: link.text()).trim()
                if (title.isBlank()) continue

                val thumbnailImg = parent.getElementsByTag("img").firstOrNull()
                    ?: link.getElementsByTag("img").firstOrNull()
                val thumbnailUrl = thumbnailImg?.attr("src")?.ifBlank {
                    thumbnailImg.attr("data-src")
                }

                related.add(ArticleContent.RelatedArticle(
                    title = title,
                    url = href,
                    thumbnailUrl = thumbnailUrl
                ))
            }
        }

        return related.distinctBy { it.url }
    }

    companion object {
        fun factory(article: SpotlightArticle): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SpotlightDetailViewModel(article) as T
                }
            }
        }
    }
}
