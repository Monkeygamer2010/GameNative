package app.gamenative.utils

class VdfStringParser {
    fun parse(content: String): Map<String, Any> {
        val lines = content.lines().map { it.trim() }.filter { it.isNotEmpty() }
        return parseSection(lines.iterator())
    }

    private fun parseSection(iterator: Iterator<String>): Map<String, Any> {
        val result = mutableMapOf<String, Any>()

        while (iterator.hasNext()) {
            val line = iterator.next()

            when {
                line == "{" -> continue
                line == "}" -> break
                line.contains("\t\t") -> {
                    // Key-value pair
                    val parts = line.split("\t\t", limit = 2)
                    if (parts.size == 2) {
                        val key = parts[0].trim('"')
                        val value = parts[1].trim('"')
                        result[key] = value
                    }
                }
                else -> {
                    // Section header
                    val sectionName = line.trim('"')
                    if (iterator.hasNext() && iterator.next() == "{") {
                        result[sectionName] = parseSection(iterator)
                    }
                }
            }
        }

        return result
    }
}
