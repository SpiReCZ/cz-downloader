package cz.dwn.downloader.model.ulozto

data class Page(var url: String) {
    lateinit var body: String
    var cookies: List<String> = ArrayList()
    lateinit var baseUrl: String
    lateinit var slug: String
    lateinit var pageName: String

    lateinit var fileName: String
    lateinit var slowDownloadURL: String
    lateinit var quickDownloadURL: String
    lateinit var captchaURL: String
    var isDirectDownload: Boolean = false
    var numTorLinks: Int? = null
    var alreadyDownloaded: Int = 0
}
