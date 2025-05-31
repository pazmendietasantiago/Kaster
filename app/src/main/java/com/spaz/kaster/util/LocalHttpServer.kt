package com.spaz.kaster.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Uri
import android.widget.Toast
import fi.iki.elonen.NanoHTTPD

class LocalHttpServer(
    private val context: Context,
    private val port: Int = 8080
) : NanoHTTPD(port) {

    private var currentUri: Uri? = null
    private var currentFileName: String? = null

    fun serveFile(uri: Uri, fileName: String) {
        currentUri = uri
        currentFileName = fileName
    }

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri
        if (uri == "/video") {
            val fileUri = currentUri ?: return newFixedLengthResponse(
                Response.Status.NOT_FOUND,
                MIME_PLAINTEXT,
                "No file"
            )
            val assetFd = try {
                context.contentResolver.openAssetFileDescriptor(fileUri, "r")
            } catch (e: Exception) {
                Toast.makeText(
                    this.context,
                    "Error en server local: ${e.message}",
                    Toast.LENGTH_LONG
                )
                    .show()

                null
            }
            if (assetFd != null) {
                val fileLength = assetFd.length
                val inputStream = assetFd.createInputStream()
                val range = session.headers["range"]
                if (range != null && range.startsWith("bytes=")) {
                    val parts = range.removePrefix("bytes=").split("-")
                    val start = parts[0].toLongOrNull() ?: 0L
                    val end =
                        if (parts.size > 1 && parts[1].isNotEmpty()) parts[1].toLongOrNull() else fileLength - 1
                    val contentLength = (end ?: (fileLength - 1)) - start + 1
                    inputStream.skip(start)
                    val response = newFixedLengthResponse(
                        Response.Status.PARTIAL_CONTENT,
                        "video/mp4",
                        inputStream,
                        contentLength
                    )
                    response.addHeader("Accept-Ranges", "bytes")
                    response.addHeader(
                        "Content-Range",
                        "bytes $start-${end ?: (fileLength - 1)}/$fileLength"
                    )
                    response.addHeader("Content-Length", contentLength.toString())
                    return response
                } else {
                    val response = newFixedLengthResponse(
                        Response.Status.OK,
                        "video/mp4",
                        inputStream,
                        fileLength
                    )
                    response.addHeader("Accept-Ranges", "bytes")
                    response.addHeader("Content-Length", fileLength.toString())
                    return response
                }
            }
            return newFixedLengthResponse(
                Response.Status.NOT_FOUND,
                MIME_PLAINTEXT,
                "File not found"
            )
        }
        return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not found")
    }

    fun getVideoUrl(): String {
        // Devuelve la URL local para el video
        val ip = getLocalIpAddress()
        return "http://$ip:$port/video"
    }

    private fun getLocalIpAddress(): String {
        val connectivityManager =
            context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val linkProperties =
            connectivityManager.getLinkProperties(connectivityManager.activeNetwork)
        linkProperties?.linkAddresses?.forEach { linkAddress ->
            if (linkAddress.address is java.net.Inet4Address) {
                return linkAddress.address.hostAddress ?: "127.0.0.1"
            }
        }
        return "127.0.0.1" // Fallback a localhost si no se encuentra IP
    }
} 