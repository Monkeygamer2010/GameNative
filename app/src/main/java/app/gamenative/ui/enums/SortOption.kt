package app.gamenative.ui.enums

import androidx.annotation.StringRes
import app.gamenative.R

/**
 * Sort options for the library list. Icon mapping is handled separately in the UI layer
 */
enum class SortOption(
    @param:StringRes val displayTextRes: Int,
) {
    INSTALLED_FIRST(displayTextRes = R.string.sort_installed_first),
    NAME_ASC(displayTextRes = R.string.sort_name_asc),
    NAME_DESC(displayTextRes = R.string.sort_name_desc),
    RECENTLY_PLAYED(displayTextRes = R.string.sort_recently_played),
    SIZE_SMALLEST(displayTextRes = R.string.sort_size_smallest),
    SIZE_LARGEST(displayTextRes = R.string.sort_size_largest),
    ;

    companion object {
        fun fromOrdinal(ordinal: Int): SortOption {
            return entries.getOrElse(ordinal) { INSTALLED_FIRST }
        }

        fun next(current: SortOption): SortOption {
            val nextIndex = (current.ordinal + 1) % entries.size
            return entries[nextIndex]
        }

        fun previous(current: SortOption): SortOption {
            val prevIndex = if (current.ordinal == 0) entries.size - 1 else current.ordinal - 1
            return entries[prevIndex]
        }
    }
}
