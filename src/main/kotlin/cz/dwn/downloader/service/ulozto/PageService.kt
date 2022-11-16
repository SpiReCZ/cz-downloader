package cz.dwn.downloader.service.ulozto

import cz.dwn.downloader.model.ulozto.Page
import cz.dwn.downloader.service.TorService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.io.IOException
import java.net.MalformedURLException
import java.net.URI
import java.net.URL
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.http.HttpResponse.BodyHandlers
import java.nio.charset.StandardCharsets


@Service
class UloztoPageService(val torService: TorService) {
    private val log: Logger = LoggerFactory.getLogger("ulozto-page-service")
    private val crawlClient: HttpClient = HttpClient.newBuilder().build()

    @EventListener(ApplicationReadyEvent::class)
    fun doSomethingAfterStartup() {
        runBlocking {
            //val page = crawl("")
            //println(page)
        }
    }

    @Throws(RuntimeException::class)
    suspend fun crawl(url: String): Page {
        var parsedUrl = parseUrl(url)
        val page = Page(parsedUrl.toString())
        page.pageName = parsedUrl.host.lowercase()
        handlePornFile(page)
        handleTrackingLink(page)
        parsedUrl = parseUrl(url)
        page.baseUrl = parsedUrl.toString().replace(parsedUrl.path, "")

        val requestBuilder = HttpRequest.newBuilder()
            .uri(parseUrl(page.url).toURI())
            .GET()
        page.cookies.forEach { requestBuilder.header(SET_COOKIE, it) }

        val response = makeRequest(requestBuilder.build(), BodyHandlers.ofString()).single()

        if (response.statusCode() != HttpStatus.OK.value()) {
            if (response.statusCode() == HttpStatus.UNAVAILABLE_FOR_LEGAL_REASONS.value()) {
                throw RuntimeException("File is unavailable due to legal reasons.")
            } else {
                throw RuntimeException("Wrong response code. File probably does not exist.")
            }
        }

        page.slug = parseSingle(parsedUrl.path, "/file/([^\\\\]*)/".toRegex())!!
        page.body = response.body()

        parse(page)
        return page
    }

    @Throws(RuntimeException::class)
    protected suspend fun parse(page: Page) {
        // Parse filename only to the first | (Uloz.to sometimes add titles like "name | on-line video | Ulož.to" and so on)
        val parsedFilename = parseSingle(page.body, "<title>([^|]*)\\s+\\|.*</title>".toRegex())!!
        // Replace illegal characters in filename https://docs.microsoft.com/en-us/windows/win32/fileio/naming-a-file
        page.fileName = parsedFilename.replace("[<>:,\"/\\\\|?*]".toRegex(), "-")
        var downloadFound = false

        val quickDownloadUrl = parseSingle(page.body, "href=\"(/quickDownload/[^\"]*)".toRegex())
        if (quickDownloadUrl != null) {
            page.quickDownloadURL = page.baseUrl + quickDownloadUrl
            downloadFound = true
        }

        page.isDirectDownload = parseSingle(
            page.body,
            "data-href=\"/download-dialog/free/[^\"]+\" +class=\".+(js-free-download-button-direct).+\"".toRegex()
        )!! == "js-free-download-button-direct"

        // Other files are protected by CAPTCHA challenge
        // <a href="javascript:;" data-href="/download-dialog/free/default?fileSlug=apj0q49iETRR" class="c-button c-button__c-white js-free-download-button-dialog t-free-download-button">
        val captchaUrl = parseSingle(page.body, "data-href=\"(/download-dialog/free/[^\"]*)\"".toRegex())

        if (captchaUrl != null) {
            page.captchaURL = page.baseUrl + captchaUrl
            downloadFound = true
        }

        if (!downloadFound) {
            throw RuntimeException("Unable to parse page to get download information. No direct URL or captcha challenge URL found.")
        }
    }

    @Throws(RuntimeException::class)
    protected suspend fun handleTrackingLink(page: Page) {
        if (page.pageName.contains("/file-tracking/")) {
            val requestBuilder = HttpRequest.newBuilder()
                .uri(parseUrl(page.url).toURI())
                .GET()
            page.cookies.forEach { requestBuilder.header(SET_COOKIE, it) }
            val response = makeRequest(requestBuilder.build(), BodyHandlers.ofString()).single()
            if (response.statusCode() != HttpStatus.MOVED_PERMANENTLY.value()) {
                throw RuntimeException("Invalid request state, expected redirect with 'location' header.")
            }
            val parsedUrl = parseUrl(response.headers().firstValue("location").orElseThrow())
            page.url = parsedUrl.toString()
        }
    }

    protected suspend fun handlePornFile(page: Page) {
        if (page.pageName.contains("pornfile.cz")) {
            val request = HttpRequest.newBuilder()
                .uri(URI.create("https://pornfile.cz/porn-disclaimer/"))
                .POST(HttpRequest.BodyPublishers.ofString(getFormDataAsString(PORNFILE_FORMDATA)))
                .build()
            val response = makeRequest(request, BodyHandlers.ofString()).single()
            if (response.statusCode() != HttpStatus.SEE_OTHER.value()) {
                throw RuntimeException("Failed to submit disclaimer.")
            }
            page.cookies = response.headers().allValues(SET_COOKIE)
        }
    }

    protected fun <T> makeRequest(
        request: HttpRequest,
        bodyHandlers: HttpResponse.BodyHandler<T>
    ): Flow<HttpResponse<T>> {
        return flowOf(crawlClient.send(request, bodyHandlers))
            .flowOn(Dispatchers.IO)
            .retryWhen { cause, attempt ->
                log.warn("Retrying request..")
                delay(1000)
                attempt < 3 || cause is IOException
            }
    }

    @Throws(MalformedURLException::class)
    protected suspend fun parseUrl(url: String): URL {
        return URL(with(url) {
            val cleanUrl = stripTrackingInfo(url)
            if (url.startsWith("http")) cleanUrl else "https://${cleanUrl}"
        })
    }

    protected suspend fun stripTrackingInfo(url: String): String {
        return if (url.contains(TRACKING_SEPARATOR)) url.split(TRACKING_SEPARATOR)[0] else url
    }


    companion object {
        const val TRACKING_SEPARATOR: String = "#!"
        const val SET_COOKIE = "set-cookie"
        val PORNFILE_FORMDATA = mapOf("agree" to "Souhlasím", "_do" to "pornDisclaimer-submit")
    }
}

fun parseSingle(s: String, regex: Regex): String? {
    return regex.find(s)?.groupValues?.getOrNull(1)
}

fun getFormDataAsString(formData: Map<String, String>): String {
    val formBodyBuilder = StringBuilder()
    for ((key, value) in formData.entries) {
        if (formBodyBuilder.isNotEmpty()) {
            formBodyBuilder.append("&")
        }
        formBodyBuilder.append(URLEncoder.encode(key, StandardCharsets.UTF_8))
        formBodyBuilder.append("=")
        formBodyBuilder.append(URLEncoder.encode(value, StandardCharsets.UTF_8))
    }
    return formBodyBuilder.toString()
}