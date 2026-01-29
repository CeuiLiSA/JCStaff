package ceui.lisa.jcstaff.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ceui.lisa.jcstaff.network.AmWork
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
                    description = result.first,
                    amWorks = result.second,
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

    private fun parseArticle(url: String): Pair<String, List<AmWork>> {
        val document = Jsoup.connect(url)
            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .referrer("https://www.pixivision.net/")
            .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8,ja;q=0.7")
            .get()

        val amWorks = mutableListOf<AmWork>()
        var description = ""

        // Find article body
        val articleElement = document.getElementsByTag("article").firstOrNull() ?: return "" to emptyList()
        val amBody = articleElement.getElementsByClass("am__body").firstOrNull() ?: return "" to emptyList()

        val nodes = amBody.children()
        if (nodes.isEmpty()) return "" to emptyList()

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

        // Parse each artwork
        for (node in workNodes) {
            try {
                val classAttr = node.classNames().joinToString(" ")
                if (!classAttr.contains("illust")) continue

                var artworkLink: String? = null
                var showImage: String? = null
                var title: String? = null
                var userLink: String? = null
                var user: String? = null
                var userImage: String? = null

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

                // Only add if we have both artwork and user info
                if (artworkLink != null && userLink != null &&
                    showImage != null && userImage != null &&
                    title != null && user != null) {
                    amWorks.add(
                        AmWork(
                            title = title,
                            user = user,
                            artworkLink = artworkLink,
                            userLink = userLink,
                            userImage = userImage,
                            showImage = showImage
                        )
                    )
                }
            } catch (e: Exception) {
                // Skip malformed entries
                continue
            }
        }

        return description to amWorks
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
