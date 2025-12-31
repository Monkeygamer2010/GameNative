package app.gamenative.data

/**
 * Represents a GOG cloud save location
 * @param name The name/identifier of the save location (e.g., "__default", "saves", "configs")
 * @param location The absolute path to the save directory on the device
 */
data class GOGCloudSavesLocation(
    val name: String,
    val location: String
)

/**
 * Response from GOG's remote config API
 * Structure: content.Windows.cloudStorage.locations[]
 * (Android runs games through Wine, so always uses Windows platform)
 */
data class GOGRemoteConfigResponse(
    val content: Map<String, GOGPlatformContent>
)

/**
 * Platform-specific content from remote config
 */
data class GOGPlatformContent(
    val cloudStorage: GOGCloudStorageInfo?
)

/**
 * Cloud storage configuration
 */
data class GOGCloudStorageInfo(
    val enabled: Boolean,
    val locations: List<GOGCloudSavesLocationTemplate>
)

/**
 * Save location template from API (before path resolution)
 * @param name The name/identifier of the location
 * @param location The path template with GOG variables (e.g., "<?INSTALL?>/saves")
 */
data class GOGCloudSavesLocationTemplate(
    val name: String,
    val location: String
)

