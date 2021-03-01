package ua.gardenapple.itchupdater.client

import android.util.Log
import android.webkit.CookieManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.Request
import org.json.JSONObject
import org.jsoup.Jsoup
import org.ocpsoft.prettytime.PrettyTime
import ua.gardenapple.itchupdater.Mitch
import java.io.IOException
import java.text.SimpleDateFormat

class ItchLibraryParser {
    companion object {
        private const val LOGGING_TAG = "ItchLibraryParser"
        
        private val dateFormat = SimpleDateFormat("yyyy-MM-dd")
        private val thumbnailCssPattern = Regex("""background-image:\s+url\('([^']*)'\)""")

        /**
         * @return null if user is not logged in and has no access, otherwise a list of items (if size == 50, should request next page)
         */
        suspend fun parsePage(page: Int): List<ItchLibraryItem>? = withContext(Dispatchers.IO) {
            val request = Request.Builder().run {
                url("https://itch.io/my-purchases?format=json&page=$page")
                addHeader("Cookie", CookieManager.getInstance().getCookie("https://itch.io"))
                get()
                build()
            }
            val result: String =
                Mitch.httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful)
                        throw IOException("Unexpected code $response")

                    if (response.isRedirect)
                        return@withContext null

                    response.body!!.string()
                }
            val resultJson = JSONObject(result)
            
            val itemCount = resultJson.getInt("num_items")
            val items = ArrayList<ItchLibraryItem>(itemCount)

            val document = Jsoup.parse(resultJson.getString("content"))
            for (gameDiv in document.getElementsByClass("game_cell")) {
                val purchaseDate = gameDiv.getElementsByClass("date_header").firstOrNull()
                    ?.getElementsByTag("span")
                    ?.attr("title")
                    ?.let {
                        if (it.isNotEmpty())
                            dateFormat.parse(it)
                        else
                            null
                    }
                
                val thumbnailLink = gameDiv.getElementsByClass("thumb_link").first()
                val downloadUrl = thumbnailLink.attr("href")
                val thumbnailUrl = 
                    thumbnailCssPattern.find(thumbnailLink.child(0).attr("style"))!!
                        .groupValues[1]
                val title = gameDiv.getElementsByClass("game_title").first().text()
                val description = gameDiv.getElementsByClass("game_text").attr("title")
                val author = gameDiv.getElementsByClass("game_author").first().text()
                val isAndroid = gameDiv.getElementsByClass("icon-android").isNotEmpty()

                items.add(ItchLibraryItem(
                    purchaseDate = purchaseDate,
                    downloadUrl = downloadUrl,
                    thumbnailUrl = thumbnailUrl,
                    title = title,
                    author = author,
                    description = description,
                    isAndroid = isAndroid
                ))
            }

            return@withContext items
        }
    }
}