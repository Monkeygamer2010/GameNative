package app.gamenative.utils

import android.content.Context
import com.winlator.contents.AdrenotoolsManager
import com.winlator.contents.ContentProfile
import com.winlator.contents.ContentsManager
import com.winlator.core.StringUtils
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

    fun filterManifestByVariant(entries: List<ManifestEntry>, variant: String?): List<ManifestEntry> {
        if (variant.isNullOrBlank()) return entries
        return entries.filter { entry ->
            val entryVariant = entry.variant?.lowercase(Locale.ENGLISH)
            entryVariant == variant.lowercase(Locale.ENGLISH) || entryVariant.isNullOrEmpty()
        }
    }

    suspend fun loadInstalledContentLists(
        context: Context,
    ): InstalledContentListsAndDrivers = withContext(Dispatchers.IO) {
        val installedDrivers = try {
            AdrenotoolsManager(context).enumarateInstalledDrivers()
        } catch (_: Exception) {
            emptyList()
        }

        val installedContent = try {
            val mgr = ContentsManager(context)
            mgr.syncContents()

            fun profilesToDisplay(
                list: List<ContentProfile>?,
            ): List<String> {
                if (list == null) return emptyList()
                return list.filter { profile -> profile.remoteUrl == null }.map { profile ->
                    val entry = ContentsManager.getEntryName(profile)
                    val firstDash = entry.indexOf('-')
                    if (firstDash >= 0 && firstDash + 1 < entry.length) entry.substring(firstDash + 1) else entry
                }
            }

            fun profilesToDisplayDedup(
                list: List<ContentProfile>?,
                seen: MutableSet<Pair<ContentProfile.ContentType, String>>,
            ): List<String> {
                if (list == null) return emptyList()
                return list.filter { profile ->
                    profile.remoteUrl == null && seen.add(Pair(profile.type, profile.verName))
                }.map { profile ->
                    val entry = ContentsManager.getEntryName(profile)
                    val firstDash = entry.indexOf('-')
                    if (firstDash >= 0 && firstDash + 1 < entry.length) entry.substring(firstDash + 1) else entry
                }
            }

            val wineProtonSeen = mutableSetOf<Pair<ContentProfile.ContentType, String>>()
            InstalledContentLists(
                dxvk = profilesToDisplay(
                    mgr.getProfiles(ContentProfile.ContentType.CONTENT_TYPE_DXVK),
                ),
                vkd3d = profilesToDisplay(
                    mgr.getProfiles(ContentProfile.ContentType.CONTENT_TYPE_VKD3D),
                ),
                box64 = profilesToDisplay(
                    mgr.getProfiles(ContentProfile.ContentType.CONTENT_TYPE_BOX64),
                ),
                wowBox64 = profilesToDisplay(
                    mgr.getProfiles(ContentProfile.ContentType.CONTENT_TYPE_WOWBOX64),
                ),
                fexcore = profilesToDisplay(
                    mgr.getProfiles(ContentProfile.ContentType.CONTENT_TYPE_FEXCORE),
                ),
                wine = profilesToDisplayDedup(
                    mgr.getProfiles(ContentProfile.ContentType.CONTENT_TYPE_WINE),
                    wineProtonSeen,
                ),
                proton = profilesToDisplayDedup(
                    mgr.getProfiles(ContentProfile.ContentType.CONTENT_TYPE_PROTON),
                    wineProtonSeen,
                ),
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

    suspend fun loadComponentAvailability(context: Context): ComponentAvailability = withContext(Dispatchers.IO) {
        val installed = loadInstalledContentLists(context)
        val manifest = ManifestRepository.loadManifest(context)
        ComponentAvailability(
            manifest = manifest,
            installed = installed.installed,
            installedDrivers = installed.installedDrivers,
        )
    }

    fun buildAvailableVersions(
        base: List<String>,
        installed: List<String>,
        manifest: List<ManifestEntry>,
    ): List<String> {
        val manifestIds = manifest.map { it.id }
        val manifestNames = manifest.map { it.name }
        return (base + installed + manifestIds + manifestNames).distinct()
    }

    fun versionExists(version: String, available: List<String>): Boolean {
        if (version.isEmpty()) return false
        val normalizedVersion = version.trim()
        val normalizedId = StringUtils.parseIdentifier(normalizedVersion)
        return available.any {
            val extracted = extractVersion(it).trim()
            val extractedId = StringUtils.parseIdentifier(extracted)
            extracted.equals(normalizedVersion, ignoreCase = true) ||
                extractedId.equals(normalizedId, ignoreCase = true)
        }
    }

    fun findManifestEntryForVersion(
        version: String,
        entries: List<ManifestEntry>,
    ): ManifestEntry? {
        val normalized = version.trim()
        if (normalized.isEmpty()) return null
        val normalizedId = StringUtils.parseIdentifier(normalized)
        return entries.firstOrNull { entry ->
            val id = entry.id
            val nameVersion = extractVersion(entry.name)
            val entryIdNorm = StringUtils.parseIdentifier(id)
            val entryNameNorm = StringUtils.parseIdentifier(nameVersion)

            // Strict-ish: exact match on version string or parsed identifier
            normalized.equals(id, ignoreCase = true) ||
                normalized.equals(nameVersion, ignoreCase = true) ||
                normalizedId.equals(entryIdNorm, ignoreCase = true) ||
                normalizedId.equals(entryNameNorm, ignoreCase = true)
        }
    }

    private fun extractVersion(display: String): String = display.split(" ").first().trim()
}
