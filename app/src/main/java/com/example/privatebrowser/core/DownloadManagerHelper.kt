package com.example.privatebrowser.core

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.widget.Toast
import com.example.privatebrowser.R

object DownloadManagerHelper {

    fun downloadFile(
        context: Context,
        url: String,
        userAgent: String?,
        contentDisposition: String?,
        mimetype: String?
    ) {
        try {
            val request = DownloadManager.Request(Uri.parse(url))
            val filename = URLUtil.guessFileName(url, contentDisposition, mimetype)

            // Include session cookies if present
            val cookies = CookieManager.getInstance().getCookie(url)
            if (!cookies.isNullOrEmpty()) {
                request.addRequestHeader("cookie", cookies)
            }
            if (!userAgent.isNullOrEmpty()) {
                request.addRequestHeader("User-Agent", userAgent)
            }

            request.setDescription("Downloading file from Private Browser")
            request.setTitle(filename)
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)

            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
            dm?.enqueue(request)

            Toast.makeText(context, "${context.getString(R.string.download_started)} $filename", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Download failed: ${e.localizedMessage ?: e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
