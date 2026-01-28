package app.gamenative.utils

import android.content.Context
import android.net.Uri
import app.gamenative.R
import com.winlator.contents.AdrenotoolsManager
import com.winlator.contents.ContentProfile
import com.winlator.contents.ContentsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

data class ManifestInstallResult(
    val success: Boolean,
    val message: String,
)

object ManifestInstaller {
    suspend fun downloadAndInstallDriver(
        context: Context,
        entry: ManifestEntry,
        onProgress: (Float) -> Unit = {},
    ): ManifestInstallResult = withContext(Dispatchers.IO) {
        var destFile: File? = null
        try {
            destFile = ManifestRepository.downloadToCache(context, entry.url, onProgress)
            val uri = Uri.fromFile(destFile)
            val name = AdrenotoolsManager(context).installDriver(uri)
            if (name.isEmpty()) {
                return@withContext ManifestInstallResult(
                    success = false,
                    message = context.getString(R.string.manifest_install_failed, entry.name),
                )
            }
            return@withContext ManifestInstallResult(
                success = true,
                message = context.getString(R.string.manifest_install_success, entry.name),
            )
        } catch (e: Exception) {
            Timber.e(e, "ManifestInstaller: driver install failed")
            return@withContext ManifestInstallResult(
                success = false,
                message = context.getString(R.string.manifest_download_failed, e.message ?: e.javaClass.simpleName),
            )
        } finally {
            destFile?.delete()
        }
    }

    /**
     * Shared helper to install a single manifest entry (driver or content).
     *
     * UI layers should provide [onProgress] to update their own state and then
     * handle the returned [ManifestInstallResult] (e.g. to show a Toast or
     * refresh installed-content lists).
     */
    suspend fun installManifestEntry(
        context: Context,
        entry: ManifestEntry,
        isDriver: Boolean,
        contentType: ContentProfile.ContentType? = null,
        onProgress: (Float) -> Unit = {},
    ): ManifestInstallResult {
        return if (isDriver) {
            downloadAndInstallDriver(context, entry, onProgress)
        } else {
            val type = contentType
                ?: throw IllegalArgumentException("contentType must be provided when installing manifest content")
            downloadAndInstallContent(context, entry, type, onProgress)
        }
    }

    suspend fun downloadAndInstallContent(
        context: Context,
        entry: ManifestEntry,
        expectedType: ContentProfile.ContentType,
        onProgress: (Float) -> Unit = {},
    ): ManifestInstallResult = withContext(Dispatchers.IO) {
        var destFile: File? = null
        try {
            destFile = ManifestRepository.downloadToCache(context, entry.url, onProgress)
            val uri = Uri.fromFile(destFile)
            val mgr = ContentsManager(context)

            val (profile, fail, error) = extractContent(mgr, uri)
            if (profile == null) {
                return@withContext ManifestInstallResult(
                    success = false,
                    message = context.getString(R.string.manifest_install_failed, entry.name),
                )
            }

            if (profile.type != expectedType) {
                ContentsManager.cleanTmpDir(context)
                return@withContext ManifestInstallResult(
                    success = false,
                    message = context.getString(R.string.manifest_type_mismatch),
                )
            }

            if (expectedType == ContentProfile.ContentType.CONTENT_TYPE_WINE ||
                expectedType == ContentProfile.ContentType.CONTENT_TYPE_PROTON
            ) {
                val tmpDir = ContentsManager.getTmpDir(context)
                val variant = detectBinaryVariant(tmpDir)
                if (variant == "glibc") {
                    ContentsManager.cleanTmpDir(context)
                    return@withContext ManifestInstallResult(
                        success = false,
                        message = context.getString(R.string.manifest_glibc_not_supported),
                    )
                }
            }

            val untrusted = mgr.getUnTrustedContentFiles(profile)
            if (untrusted.isNotEmpty()) {
                ContentsManager.cleanTmpDir(context)
                return@withContext ManifestInstallResult(
                    success = false,
                    message = context.getString(R.string.manifest_content_untrusted),
                )
            }

            val installed = finishInstall(mgr, profile)
            if (!installed) {
                return@withContext ManifestInstallResult(
                    success = false,
                    message = context.getString(R.string.manifest_install_failed, entry.name),
                )
            }

            return@withContext ManifestInstallResult(
                success = true,
                message = context.getString(R.string.manifest_install_success, entry.name),
            )
        } catch (e: Exception) {
            Timber.e(e, "ManifestInstaller: content install failed")
            return@withContext ManifestInstallResult(
                success = false,
                message = context.getString(R.string.manifest_download_failed, e.message ?: e.javaClass.simpleName),
            )
        } finally {
            destFile?.delete()
        }
    }

    private suspend fun extractContent(
        mgr: ContentsManager,
        uri: Uri,
    ): Triple<ContentProfile?, ContentsManager.InstallFailedReason?, Exception?> = withContext(Dispatchers.IO) {
        var profile: ContentProfile? = null
        var failReason: ContentsManager.InstallFailedReason? = null
        var err: Exception? = null
        val latch = CountDownLatch(1)
        try {
            mgr.extraContentFile(uri, object : ContentsManager.OnInstallFinishedCallback {
                override fun onFailed(reason: ContentsManager.InstallFailedReason, e: Exception) {
                    failReason = reason
                    err = e
                    latch.countDown()
                }

                override fun onSucceed(profileArg: ContentProfile) {
                    profile = profileArg
                    latch.countDown()
                }
            })
        } catch (e: Exception) {
            err = e
            latch.countDown()
        }
        if (!latch.await(240, TimeUnit.SECONDS)) {
            err = Exception("Installation timed out")
        }
        Triple(profile, failReason, err)
    }

    private suspend fun finishInstall(
        mgr: ContentsManager,
        profile: ContentProfile,
    ): Boolean = withContext(Dispatchers.IO) {
        var success = false
        val latch = CountDownLatch(1)
        try {
            mgr.finishInstallContent(profile, object : ContentsManager.OnInstallFinishedCallback {
                override fun onFailed(reason: ContentsManager.InstallFailedReason, e: Exception) {
                    latch.countDown()
                }

                override fun onSucceed(profileArg: ContentProfile) {
                    success = true
                    latch.countDown()
                }
            })
        } catch (_: Exception) {
            latch.countDown()
        }
        latch.await()
        success
    }

    private fun detectBinaryVariant(installDir: File): String {
        return try {
            val wine64 = File(installDir, "bin/wine64")
            val wine = File(installDir, "bin/wine")
            val binaryFile = when {
                wine64.exists() -> wine64
                wine.exists() -> wine
                else -> return "unknown"
            }

            val bytes = binaryFile.inputStream().use { stream ->
                val buffer = ByteArray(1024)
                val read = stream.read(buffer)
                buffer.copyOf(read)
            }
            val content = String(bytes, Charsets.ISO_8859_1)
            when {
                content.contains("/system/bin/linker") -> "bionic"
                content.contains("/lib64/ld-linux") || content.contains("/lib/ld-linux") -> "glibc"
                else -> "unknown"
            }
        } catch (e: Exception) {
            Timber.e("Error detecting binary variant: $e")
            "unknown"
        }
    }
}
