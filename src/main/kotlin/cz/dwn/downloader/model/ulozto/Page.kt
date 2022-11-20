package cz.dwn.downloader.model.ulozto

data class Page(var url: String) {
    lateinit var body: String
    lateinit var baseUrl: String
    lateinit var slug: String
    lateinit var pageName: String
    lateinit var fileName: String

    var cookies: List<String> = ArrayList()
    var slowDownloadURL: String? = null
    var quickDownloadURL: String? = null
    var captchaURL: String? = null
    var isDirectDownload: Boolean = false
    var numTorLinks: Int? = null
    var alreadyDownloaded: Int = 0
}
