package app.gamenative.service.gog

/**
 * Constants for GOG integration
 */
object GOGConstants {
    // GOG API URLs
    const val GOG_BASE_API_URL = "https://api.gog.com"
    const val GOG_AUTH_URL = "https://auth.gog.com"
    const val GOG_EMBED_URL = "https://embed.gog.com"
    const val GOG_GAMESDB_URL = "https://gamesdb.gog.com"

    // GOG Client ID for authentication
    const val GOG_CLIENT_ID = "46899977096215655"

    // GOG uses a standard redirect URI that we can intercept
    const val GOG_REDIRECT_URI = "https://embed.gog.com/on_login_success?origin=client"

    // GOG OAuth authorization URL with redirect
    const val GOG_AUTH_LOGIN_URL = "https://auth.gog.com/auth?client_id=$GOG_CLIENT_ID&redirect_uri=$GOG_REDIRECT_URI&response_type=code&layout=client2"

    // GOG paths
    const val GOG_GAMES_BASE_PATH = "/data/data/app.gamenative/files/gog_games"

    /**
     * Get the install path for a specific GOG game
     */
    fun getGameInstallPath(gameTitle: String): String {
        // Sanitize game title for filesystem
        val sanitizedTitle = gameTitle.replace(Regex("[^a-zA-Z0-9 ]"), "").trim()
        return "$GOG_GAMES_BASE_PATH/$sanitizedTitle"
    }

    /**
     * Get the auth config path
     */
    fun getAuthConfigPath(): String {
        return "/data/data/app.gamenative/files/gog_auth.json"
    }

    /**
     * Get the support directory path (for redistributables)
     */
    fun getSupportPath(): String {
        return "/data/data/app.gamenative/files/gog-support"
    }
}
