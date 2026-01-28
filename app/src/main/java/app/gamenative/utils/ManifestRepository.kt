package app.gamenative.utils

import android.content.Context
import app.gamenative.PrefManager
import app.gamenative.service.SteamService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Request
import timber.log.Timber
import java.io.File
import java.io.IOException
import kotlin.math.max

object ManifestRepository {
    private const val ONE_DAY_MS = 24 * 60 * 60 * 1000L
    private const val MANIFEST_URL = "https://raw.githubusercontent.com/utkarshdalal/GameNative/refs/heads/master/manifest.json"
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun loadManifest(context: Context): ManifestData {
        val cachedJson = PrefManager.componentManifestJson
        val cachedManifest = parseManifest(cachedJson) ?: ManifestData.empty()
        val lastFetchedAt = PrefManager.componentManifestFetchedAt
        val now = System.currentTimeMillis()
        val isStale = now - lastFetchedAt >= ONE_DAY_MS

        if (cachedJson.isNotEmpty() && !isStale) {
            return cachedManifest
        }

        val fetched = fetchManifestJson()
        if (fetched != null) {
            PrefManager.componentManifestJson = fetched
            PrefManager.componentManifestFetchedAt = now
            return parseManifest(fetched) ?: cachedManifest
        }

        return cachedManifest
    }

    suspend fun downloadToCache(
        context: Context,
        url: String,
        onProgress: (Float) -> Unit = {},
    ): File = withContext(Dispatchers.IO) {
        val fileName = url.substringAfterLast("/").ifEmpty { "download.zip" }
        val dest = File(context.cacheDir, fileName)
        dest.parentFile?.mkdirs()

        if (url.startsWith("https://downloads.gamenative.app/")) {
            val path = url.removePrefix("https://downloads.gamenative.app/")
            SteamService.fetchFileWithFallback(path, dest, context, onProgress)
            if (!dest.exists() || dest.length() == 0L) {
                throw IOException("Download failed")
            }
            return@withContext dest
        }

        fetchUrlToFile(url, dest, onProgress)
        return@withContext dest
    }

    private suspend fun fetchManifestJson(): String? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(MANIFEST_URL).build()
            Net.http.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Timber.w("ManifestRepository: fetch failed HTTP=%s", response.code)
                    return@withContext null
                }
                return@withContext response.body?.string()
            }
        } catch (e: Exception) {
            Timber.e(e, "ManifestRepository: fetch failed")
            return@withContext null
        }
    }

    private fun fetchUrlToFile(
        url: String,
        dest: File,
        onProgress: (Float) -> Unit,
    ) {
        val tmp = File(dest.parentFile, dest.name + ".part")
        if (tmp.exists()) tmp.delete()
        try {
            val request = Request.Builder().url(url).build()
            Net.http.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("HTTP ${response.code}")
                }
                val body = response.body ?: throw IOException("Empty response body")
                val total = max(body.contentLength(), 0L)
                var readBytes = 0L
                body.byteStream().use { input ->
                    tmp.outputStream().use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        while (true) {
                            val read = input.read(buffer)
                            if (read <= 0) break
                            output.write(buffer, 0, read)
                            readBytes += read
                            if (total > 0L) {
                                onProgress(readBytes.toFloat() / total.toFloat())
                            }
                        }
                    }
                }
                if (total > 0L && tmp.length() != total) {
                    throw IOException("Incomplete download")
                }
                if (!tmp.renameTo(dest)) {
                    tmp.copyTo(dest, overwrite = true)
                    tmp.delete()
                }
            }
        } catch (e: Exception) {
            tmp.delete()
            throw e
        }
    }

    fun parseManifest(jsonString: String?): ManifestData? {
        if (jsonString.isNullOrBlank()) return null
        return try {
            val root = json.parseToJsonElement(jsonString).jsonObject
            val version = root["version"]?.jsonPrimitive?.intOrNull
            val updatedAt = root["updatedAt"]?.jsonPrimitive?.contentOrNull
            val itemsObj = root["items"]?.jsonObject ?: JsonObject(emptyMap())

            val items = itemsObj.entries.associate { (key, value) ->
                key to parseManifestArray(value.jsonArray)
            }
            ManifestData(version, updatedAt, items)
        } catch (e: Exception) {
            Timber.e(e, "ManifestRepository: parse failed")
            null
        }
    }

    private fun parseManifestArray(array: JsonArray): List<ManifestEntry> {
        return array.mapNotNull { element ->
            val obj = element.jsonObject
            val id = obj["id"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
            val name = obj["name"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
            val url = obj["url"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
            if (id.isEmpty() || name.isEmpty() || url.isEmpty()) return@mapNotNull null
            val variant = obj["variant"]?.jsonPrimitive?.contentOrNull?.trim()
            val arch = obj["arch"]?.jsonPrimitive?.contentOrNull?.trim()
            ManifestEntry(id = id, name = name, url = url, variant = variant, arch = arch)
        }
    }
}
