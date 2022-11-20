package cz.dwn.downloader.service.ulozto

import cz.dwn.downloader.model.ulozto.Page
import cz.dwn.downloader.service.TorService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import org.apache.http.NameValuePair
import org.apache.http.client.ClientProtocolException
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.methods.HttpRequestBase
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import org.apache.http.message.BasicNameValuePair
import org.apache.http.util.EntityUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.io.IOException
import java.net.MalformedURLException
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.*


@Service
class UloztoPageService(val torService: TorService) {
    private val log: Logger = LoggerFactory.getLogger("ulozto-page-service")
    private val crawlClient: CloseableHttpClient = HttpClients.createDefault()

    @EventListener(ApplicationReadyEvent::class)
    fun doSomethingAfterStartup() {
        runBlocking {
            //val page = crawl("")
            //println(page)
        }
    }

    suspend fun obtainLink(page: Page): String =
        if (page.quickDownloadURL != null) page.quickDownloadURL!! else obtainSlowDownloadUrl(page)

    suspend fun obtainSlowDownloadUrl(page: Page): String {
        return ""
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

        val request = HttpGet(page.url)
        page.cookies.forEach { request.addHeader(SET_COOKIE, it) }

        val response = makeRequest(request).single()

        response.use {
            if (it.statusLine.statusCode != HttpStatus.OK.value()) {
                if (it.statusLine.statusCode == HttpStatus.UNAVAILABLE_FOR_LEGAL_REASONS.value()) {
                    throw RuntimeException("File is unavailable due to legal reasons.")
                } else {
                    throw RuntimeException("Wrong response code. File probably does not exist.")
                }
            }

            page.slug = parseSingle(parsedUrl.path, "/file/([^\\\\]*)/".toRegex())!!
            page.body = EntityUtils.toString(it.entity, StandardCharsets.UTF_8)

            parse(page)
            return page
        }
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

            val request = HttpGet(page.url)
            page.cookies.forEach { request.addHeader(SET_COOKIE, it) }

            val response = makeRequest(request).single()
            if (response.statusLine.statusCode != HttpStatus.MOVED_PERMANENTLY.value()) {
                throw RuntimeException("Invalid request state, expected redirect with 'location' header.")
            }
            val parsedUrl = parseUrl(response.getFirstHeader("location").value)
            page.url = parsedUrl.toString()
        }
    }

    protected suspend fun handlePornFile(page: Page) {
        if (page.pageName.contains("pornfile.cz")) {
            val request = HttpPost("https://pornfile.cz/porn-disclaimer/")
            request.entity = UrlEncodedFormEntity(PORNFILE_FORMDATA)

            val response = makeRequest(request).single()
            if (response.statusLine.statusCode != HttpStatus.SEE_OTHER.value()) {
                throw RuntimeException("Failed to submit disclaimer.")
            }
            page.cookies = Arrays.stream(response.getHeaders(SET_COOKIE))
                .map { it.value }
                .toList()
        }
    }

    @Throws(IOException::class, ClientProtocolException::class)
    fun makeRequest(request: HttpRequestBase): Flow<CloseableHttpResponse> {
        return flowOf(crawlClient.execute(request))
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

    protected suspend fun stripTrackingInfo(url: String): String =
        if (url.contains(TRACKING_SEPARATOR)) url.split(TRACKING_SEPARATOR)[0] else url

    companion object {
        const val TRACKING_SEPARATOR: String = "#!"
        const val SET_COOKIE = "set-cookie"
        val PORNFILE_FORMDATA = listOf<NameValuePair>(
            BasicNameValuePair("agree", "Souhlasím"),
            BasicNameValuePair("_do", "pornDisclaimer-submit")
        )
    }
}

fun parseSingle(s: String, regex: Regex): String? = regex.find(s)?.groupValues?.getOrNull(1)
