package ee.tallinntastebuds.content

/**
 * Every word of interface text, read from the website's `data/ui.json`.
 *
 * The app ships no `strings.xml` translations on purpose. Add a language to
 * `ui.json` on the site and it appears in the app's language picker on the next
 * refresh; fix a typo there and the app stops showing it, with no release in
 * between.
 *
 * A value type, rebuilt from the store's table and the reader's language each
 * time it is read. It holds a map reference and nothing else, so that costs
 * nothing and there is no second copy of the language to keep in step.
 */
data class Strings(
    /** language code -> key -> text */
    val table: Map<String, Map<String, String>> = emptyMap(),
    val lang: String = FALLBACK_LANG,
) {
    /**
     * The languages the site offers, English first because it is the fallback
     * everything else leans on, then by name.
     */
    val languages: List<Language> by lazy {
        table.keys
            .map { Language(it, table[it]?.get("langName") ?: it.uppercase()) }
            .sortedWith(
                compareBy<Language> { it.code != FALLBACK_LANG }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
            )
    }

    val hasContent: Boolean get() = table.isNotEmpty()

    fun has(code: String): Boolean = table.containsKey(code)

    /**
     * Look up [key] in the current language, falling back to English and then to
     * the key itself — a missing string shows as its key rather than as a blank,
     * which is the difference between a bug you can see and one you cannot.
     */
    operator fun invoke(key: String): String =
        table[lang]?.get(key) ?: table[FALLBACK_LANG]?.get(key) ?: key

    /** The site's placeholder convention: `{n} places`, `Photo {n} of {total}`. */
    operator fun invoke(key: String, replacements: Map<String, String>): String =
        replacements.entries.fold(invoke(key)) { text, (name, value) ->
            text.replace("{$name}", value)
        }

    /**
     * "3 places" / "1 place": the site keeps a separate key for the singular
     * because not every one of its languages forms it the same way.
     */
    fun count(n: Int): String =
        if (n == 1) invoke("listCountOne") else invoke("listCount", mapOf("n" to n.toString()))

    /** `YYYY-MM` rendered with the month names from `ui.json`. */
    fun monthYear(value: String): String {
        val parts = value.split("-")
        val month = parts.getOrNull(1)?.toIntOrNull() ?: return value
        if (month !in 1..12) return value
        val names = invoke("months").split("|")
        if (names.size != 12) return value
        return invoke("monthYear", mapOf("month" to names[month - 1], "year" to parts[0]))
    }

    companion object {
        const val FALLBACK_LANG = "en"
    }
}

data class Language(val code: String, val name: String)
