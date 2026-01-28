package app.gamenative.utils

import android.content.Context
import com.winlator.contents.AdrenotoolsManager
import com.winlator.contents.ContentProfile
import com.winlator.contents.ContentsManager
import com.winlator.core.StringUtils
import com.winlator.core.GPUHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

object ManifestComponentHelper {
    data class InstalledContentLists(
        val dxvk: List<String>,
        val vkd3d: List<String>,
        val box64: List<String>,
        val wowBox64: List<String>,
        val fexcore: List<String>,
        val wine: List<String>,
        val proton: List<String>,
    )

    data class InstalledContentListsAndDrivers(
        val installed: InstalledContentLists,
        val installedDrivers: List<String>,
    )

    data class ComponentAvailability(
        val manifest: ManifestData,
        val installed: InstalledContentLists,
        val installedDrivers: List<String>,
    )

    data class VersionOption(
        val label: String,
        val id: String,
        val isManifest: Boolean,
        val isInstalled: Boolean,
    )

    data class VersionOptionList(
        val labels: List<String>,
        val ids: List<String>,
        val muted: List<Boolean>,
    )

    fun filterManifestByVariant(entries: List<ManifestEntry>, variant: String?): List<ManifestEntry> {
        return entries.filter { entry -> entry.variant?.lowercase(Locale.ENGLISH) == variant.lowercase(Locale.ENGLISH) }
    }

    suspend fun loadInstalledContentLists(
        context: Context,
    ): InstalledContentListsAndDrivers = withContext(Dispatchers.IO) {
        val installedDrivers = AdrenotoolsManager(context).enumarateInstalledDrivers()

        val installedContent = try {
            val mgr = ContentsManager(context)
            mgr.syncContents()

            fun profilesToDisplay(
                list: List<ContentProfile>?,
            ): List<String> {
                if (list == null) return emptyList()
                return list
                    .filter { profile -> profile.remoteUrl == null }
                    .map { profile -> profile.verName.orEmpty() }
            }

            InstalledContentLists(
                dxvk = profilesToDisplay(ContentProfile.ContentType.CONTENT_TYPE_DXVK),
                vkd3d = profilesToDisplay(ContentProfile.ContentType.CONTENT_TYPE_VKD3D),
                box64 = profilesToDisplay(ContentProfile.ContentType.CONTENT_TYPE_BOX64),
                wowBox64 = profilesToDisplay(ContentProfile.ContentType.CONTENT_TYPE_WOWBOX64),
                fexcore = profilesToDisplay(ContentProfile.ContentType.CONTENT_TYPE_FEXCORE),
                wine = profilesToDisplay(ContentProfile.ContentType.CONTENT_TYPE_WINE),
                proton = profilesToDisplay(ContentProfile.ContentType.CONTENT_TYPE_PROTON),
            )
        } catch (_: Exception) {
            InstalledContentLists(
                dxvk = emptyList(),
                vkd3d = emptyList(),
                box64 = emptyList(),
                wowBox64 = emptyList(),
                fexcore = emptyList(),
                wine = emptyList(),
                proton = emptyList(),
            )
        }

        InstalledContentListsAndDrivers(
            installed = installedContent,
            installedDrivers = installedDrivers,
        )
    }

    fun buildAvailableVersions(base: List<String>, installed: List<String>, manifest: List<ManifestEntry>, ): List<String> {
        return (base + installed + manifestIds + manifest.map { it.id }).distinct()
    }

    fun buildVersionOptionList(base: List<String>, installed: List<String>, manifest: List<ManifestEntry>, ): VersionOptionList {
        val options = LinkedHashMap<String, VersionOption>()

        (base + installed).forEach { label ->
            val id = StringUtils.parseIdentifier(label)
            options[id] = VersionOption(label, id, false, true)
        }

        val availableIds = options.keys.toSet()
        manifest.forEach { entry ->
            if (!options.containsKey(entry.id)) {
                val isInstalled = availableIds.contains(entry.id)
                addOption(entry.id, entry.id, isManifest = true, isInstalled = isInstalled)
            }
        }

        val values = options.values.toList()
        return VersionOptionList(labels = values.map { it.label }, ids = values.map { it.id }, muted = values.map { it.isManifest && !it.isInstalled }, )
    }

    fun versionExists(version: String, available: List<String>): Boolean {
        if (version.isEmpty()) return false
        val normalizedVersion = version.trim()
        return available.any { StringUtils.parseIdentifier(it).equals(normalizedVersion, ignoreCase = true) }
    }

    fun findManifestEntryForVersion(
        version: String,
        entries: List<ManifestEntry>,
    ): ManifestEntry? {
        val normalized = version.trim()
        if (normalized.isEmpty()) return null
        return entries.firstOrNull { entry ->
            normalized.equals(entry.id, ignoreCase=true)
        }
    }
}
