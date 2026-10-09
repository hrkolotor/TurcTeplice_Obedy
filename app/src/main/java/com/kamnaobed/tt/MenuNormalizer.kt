package com.kamnaobed.tt

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

// ---------------------------------------------------------------------------
// Jednotný model menu – rovnaký pre všetky prevádzky.
// ---------------------------------------------------------------------------

data class Dish(
    val label: String?,   // "1", "A", "XXL" … (podľa čoho sa objednáva), alebo null
    val name: String,     // vyčistený názov jedla – bez gramáže a alergénov
    val price: String?,   // "7,50 €" alebo null
    val soup: Boolean,
)

data class DayMenu(
    val dayName: String,      // "Pondelok"
    val date: LocalDate?,     // dátum (opravený, ak ho web uvádza zle)
    val dishes: List<Dish>,
)

data class WeeklyGroup(val title: String, val dishes: List<Dish>)

data class StructuredMenu(
    val title: String,
    val days: List<DayMenu>,
    val weekly: List<WeeklyGroup>,
    val phones: List<String>,
)

/**
 * Z HTML sekcie jednej prevádzky vytiahne deň, dátum, jedlo a cenu.
 *
 * Prevádzky píšu menu každá inak (tabuľky s rôznymi stĺpcami, deň v nadpise
 * tabuľky alebo v jej prvom riadku, zoznam v odseku, cena v stĺpci / v texte /
 * len v cenníku hore). Preto sa obsah najprv rozloží na „riadky“ a tie sa potom
 * spracujú jednotnými pravidlami. Ak sa nič rozumné nenájde, vráti null a appka
 * zobrazí pôvodný formát.
 */
object MenuNormalizer {

    // ----- riadky -----

    data class Line(val cells: List<String>, val isRow: Boolean) {
        val text: String get() = cells.filter { it.isNotEmpty() }.joinToString(" ")
    }

    private val BLOCK = setOf(
        "p", "div", "h1", "h2", "h3", "h4", "h5", "h6", "li", "ul", "ol", "section",
        "article", "blockquote", "caption", "header", "footer", "dl", "dt", "dd", "pre",
    )

    fun clean(s: String): String =
        s.replace(' ', ' ').replace(Regex("\\s+"), " ").trim()

    fun extractLines(root: Element): List<Line> {
        val out = mutableListOf<Line>()
        val buf = StringBuilder()
        fun flush() {
            val t = clean(buf.toString())
            if (t.isNotEmpty()) out += Line(listOf(t), isRow = false)
            buf.setLength(0)
        }
        fun walk(node: Node) {
            when (node) {
                is TextNode -> buf.append(node.wholeText)
                is Element -> when (val tag = node.tagName().lowercase()) {
                    "script", "style", "img" -> Unit
                    "br" -> flush()
                    "table" -> {
                        flush()
                        node.selectFirst("caption")?.let { cap ->
                            val t = clean(cap.text())
                            if (t.isNotEmpty()) out += Line(listOf(t), isRow = false)
                        }
                        for (tr in node.select("tr")) {
                            val cells = tr.children()
                                .filter { it.tagName().lowercase() in setOf("td", "th") }
                                .map { clean(it.text()) }
                            if (cells.any { it.isNotEmpty() }) out += Line(cells, isRow = true)
                        }
                    }
                    else -> {
                        val block = tag in BLOCK
                        if (block) flush()
                        node.childNodes().forEach { walk(it) }
                        if (block) flush()
                    }
                }
            }
        }
        walk(root)
        flush()
        return out
    }

    // ----- regulárne výrazy -----

    private val DAY_NAMES = listOf("pondelok", "utorok", "streda", "štvrtok", "piatok", "sobota", "nedeľa")

    private val DAY_RX = Regex(
        "^(pondelok|utorok|streda|štvrtok|piatok|sobota|nedeľa)\\b[\\s,.:\\-–]*" +
            "(?:(\\d{1,2})\\s*\\.\\s*(\\d{1,2})\\.?(?:\\s*(\\d{4}))?)?\\s*\\.?$",
        RegexOption.IGNORE_CASE,
    )
    private val WEEKLY_RX = Regex(
        "^(týždenn\\S*(?:\\s+\\S+)?|\\S+\\s+týždňa|celý týždeň|víkendové menu)\\s*:?$",
        RegexOption.IGNORE_CASE,
    )
    private val STOP_RX = Regex("^(zoznam alergénov|alergény\\s*:)", RegexOption.IGNORE_CASE)

    private val PRICE_RX = Regex("(\\d{1,3})(?:[,.](\\d{2})|,-)\\s*(?:€|eur\\b)", RegexOption.IGNORE_CASE)
    private val PRICE_ONLY_RX = Regex("^\\s*(\\d{1,3})(?:[,.](\\d{2})|,-)\\s*(?:€|eur)\\s*$", RegexOption.IGNORE_CASE)

    // "Denné menu 1,2,3,6: 7,00 €", "Týždenné menu - Guláš … - 8,50€", "Októbrové menu: Kebab … | cena: 10,-€"
    private val MENU_DEF_RX = Regex(
        "^(.{0,30}?\\bmenu)\\s*((?:\\d+|xxl)(?:\\s*,\\s*(?:\\d+|xxl))*)?\\s*[:\\-–]\\s*(.+)$",
        RegexOption.IGNORE_CASE,
    )
    private val DEFAULT_PRICE_RX = Regex(
        "cena[^0-9€]{0,40}?(\\d{1,3}(?:[,.]\\d{2}|,-))\\s*(?:€|eur)",
        RegexOption.IGNORE_CASE,
    )

    private val SOUP_PREFIX_RX = Regex("^polievka\\b[^:]{0,15}:\\s*(.*)$", RegexOption.IGNORE_CASE)
    private val LABEL_PREFIX_RX = Regex(
        "^(?:menu\\s*(?:č\\.?\\s*)?)?(\\d{1,2}|xxl|[a-h])\\s*[.:)]\\s+(.+)$",
        RegexOption.IGNORE_CASE,
    )
    private val LABEL_CELL_RX = Regex(
        "^(?:polievka\\s*:?|menu\\s*(?:č\\.?\\s*)?(?:\\d{1,2}|xxl)\\s*:?|\\d{1,2}\\.?|xxl|kombo|[a-h]\\s*[:.)]?(?:\\s*$QTY)?)$",
        RegexOption.IGNORE_CASE,
    )
    private val NOTE_RX = Regex(
        "(doplatok|možnosť prípravy|objednáv|hmotnosť|zmena jedál|so sebou|\\bobal\\b|zverejnené|" +
            "aktualizované|ďakujeme|alergén|\\(pdf|príbor|rozváž|podávame|v cene je|@|www\\.)",
        RegexOption.IGNORE_CASE,
    )
    private val PHONE_RX = Regex("(?:\\+421|\\b0)\\s?\\d{2,3}(?:\\s?\\d{2,3}){2,3}\\b")

    private val ALLERGEN_PAREN_RX = Regex("\\(\\s*\\d{1,2}(?:\\s*,\\s*\\d{1,2})*\\s*\\)")
    private val ALLERGEN_TAG_RX = Regex("(?:;\\s*)?\\bAl?\\.?\\s*:\\s*\\d{1,2}(?:\\s*,\\s*\\d{1,2})*")
    // zoznam surovín, za ktorým nasledujú alergény: "Lasagne (mäso, omáčka, bešamel) (1,3,7)"
    private val INGREDIENTS_RX = Regex("\\([^()]*\\)(?=\\s*\\(\\s*\\d{1,2}(?:\\s*,\\s*\\d{1,2})*\\s*\\))")
    private const val QTY = "\\d+(?:[,.]\\d+)?\\s*(?:/\\s*\\d+(?:[,.]\\d+)?\\s*)?(?:g|kg|ml|l|ks)\\b"
    private val QTY_PAREN_RX = Regex("\\(\\s*$QTY\\s*\\)", RegexOption.IGNORE_CASE)
    private val QTY_RX = Regex("\\b$QTY", RegexOption.IGNORE_CASE)

    // ----- hlavná funkcia -----

    fun normalize(restaurant: Restaurant, today: LocalDate = LocalDate.now()): StructuredMenu? {
        val root = Jsoup.parseBodyFragment(restaurant.html).body()
        val title = root.selectFirst("h3")?.text()?.let(::clean)?.takeIf { it.isNotEmpty() }
            ?: restaurant.name
        val phones = findPhones(clean(root.text()))
        val lines = extractLines(root)

        val priceMap = mutableMapOf<String, String>()
        var defaultPrice: String? = null
        val days = mutableListOf<RawDay>()
        val weekly = mutableListOf<WeeklyGroup>()
        var current: MutableList<Dish>? = null

        for (line in lines) {
            val text = line.text
            if (STOP_RX.containsMatchIn(text)) break

            val singleCell = !line.isRow || line.cells.count { it.isNotEmpty() } == 1
            if (singleCell) {
                val dayM = DAY_RX.find(text)
                if (dayM != null) {
                    val list = mutableListOf<Dish>()
                    days += RawDay(dayM.groupValues[1].lowercase(), dayM.groupValues[2].toIntOrNull(),
                        dayM.groupValues[3].toIntOrNull(), dayM.groupValues[4].toIntOrNull(), list)
                    current = list
                    continue
                }
                val weekM = WEEKLY_RX.find(text)
                if (weekM != null) {
                    val list = mutableListOf<Dish>()
                    weekly += WeeklyGroup(capitalize(weekM.groupValues[1].lowercase()), list)
                    current = list
                    continue
                }
            }

            val target = current
            if (target == null) {
                // Úvod pred prvým dňom: cenník, týždenné menu, predvolená cena.
                val def = MENU_DEF_RX.find(text)
                if (def != null && !line.isRow) {
                    val head = clean(def.groupValues[1])
                    val nums = def.groupValues[2]
                    val rest = def.groupValues[3]
                    val restName = cleanName(rest)
                    val restPrice = PRICE_RX.findAll(rest).lastOrNull()?.let(::formatPrice)
                    if (letters(restName) >= 6) {
                        weekly += WeeklyGroup(capitalize(head.lowercase()), listOf(Dish(null, restName, restPrice, false)))
                    } else if (restPrice != null) {
                        val labels = Regex("\\d+|xxl", RegexOption.IGNORE_CASE)
                            .findAll(nums + " " + head).map { it.value.uppercase() }.toList()
                        labels.forEach { priceMap[it] = restPrice }
                    }
                    continue
                }
                if (defaultPrice == null) {
                    DEFAULT_PRICE_RX.find(text)?.let { defaultPrice = normalizePriceText(it.groupValues[1]) }
                }
                continue
            }

            parseDish(line, priceMap, defaultPrice)?.let { target += it }
        }

        val fixedDays = fixDates(days, today).filter { it.dishes.isNotEmpty() }
        val nonEmptyWeekly = weekly.filter { it.dishes.isNotEmpty() }
        if (fixedDays.isEmpty() && nonEmptyWeekly.isEmpty()) return null
        return StructuredMenu(title, fixedDays, nonEmptyWeekly, phones)
    }

    // ----- jedlo z riadku -----

    private fun parseDish(line: Line, priceMap: Map<String, String>, defaultPrice: String?): Dish? {
        var label: String? = null
        var desc: String
        var price: String? = null
        var soup = false

        if (line.isRow) {
            val cells = line.cells
            // Popis = bunka s najviac textom; bunky typu "Polievka", "Menu 1", "A: 120g" ním nie sú.
            val descIdx = cells.indices.maxByOrNull { i ->
                if (LABEL_CELL_RX.matches(cells[i])) -1 else letters(cleanName(cells[i]))
            } ?: return null
            desc = cells[descIdx]
            for (i in cells.indices) {
                if (i == descIdx) continue
                PRICE_ONLY_RX.find(cells[i])?.let { if (price == null) price = formatPrice(it) }
            }
            label = (0 until descIdx).map { cells[it] }.firstOrNull { it.isNotEmpty() && PRICE_ONLY_RX.find(it) == null }
        } else {
            desc = line.text
            val soupM = SOUP_PREFIX_RX.find(desc)
            val labelM = LABEL_PREFIX_RX.find(desc)
            when {
                soupM != null -> { soup = true; desc = soupM.groupValues[1] }
                labelM != null -> { label = labelM.groupValues[1]; desc = labelM.groupValues[2] }
                NOTE_RX.containsMatchIn(desc) -> return null
            }
        }

        if (label != null) {
            if (label.contains("polievka", ignoreCase = true)) { soup = true; label = null }
            else label = normalizeLabel(label)
        }
        if (price == null) price = PRICE_RX.findAll(desc).lastOrNull()?.let(::formatPrice)
        val name = cleanName(desc)
        if (letters(name) < 3) return null
        if (price == null && !soup) price = label?.let { priceMap[it] } ?: defaultPrice
        return Dish(label, name, price, soup)
    }

    private fun normalizeLabel(raw: String): String? {
        var s = raw.replace(Regex("(?i)\\bmenu\\b|\\bč\\."), " ")
        s = QTY_RX.replace(s, " ")
        s = s.replace(Regex("[:.)(]"), " ")
        s = clean(s)
        if (s.isEmpty() || s.length > 6 || letters(s) > 3) return null
        return s.uppercase()
    }

    fun cleanName(input: String): String {
        var s = input
        s = PRICE_RX.replace(s, " ")
        s = INGREDIENTS_RX.replace(s, " ")
        s = ALLERGEN_PAREN_RX.replace(s, " ")
        s = ALLERGEN_TAG_RX.replace(s, " ")
        s = QTY_PAREN_RX.replace(s, " ")
        s = QTY_RX.replace(s, " ")
        s = s.replace(Regex("\\(\\s*\\)"), " ")
        s = clean(s)
        s = s.replace(Regex("\\s+([,;.])"), "$1")
        repeat(3) {
            s = s.replace(Regex("(?i)\\|?\\s*cena\\s*:?\\s*$"), "")
            s = s.replace(Regex("([;,])\\s*(?=[;,]|$)"), "")
            s = s.replace(Regex("\\s*/\\s*(?=[;,]|$)"), "")
            s = s.replace(Regex("^[\\s;,:/\\-–|]+|[\\s;,:/\\-–|]+$"), "")
        }
        s = s.replace(Regex(";\\s*"), ", ")
        s = s.replace(Regex("(?<![\\d/])1\\s*/\\s*2(?![\\d/])"), "½")
        return capitalize(clean(s))
    }

    // ----- pomocné -----

    private class RawDay(val name: String, val d: Int?, val m: Int?, val y: Int?, val dishes: List<Dish>)

    /** Doplní rok, a ak dátum nesedí s dňom v týždni (preklep na webe), odvodí ho z ostatných dní. */
    private fun fixDates(days: List<RawDay>, today: LocalDate): List<DayMenu> {
        fun build(r: RawDay): LocalDate? {
            val d = r.d ?: return null
            val m = r.m ?: return null
            if (m !in 1..12 || d !in 1..31) return null
            if (r.y != null) return runCatching { LocalDate.of(r.y, m, d) }.getOrNull()
            return listOf(today.year - 1, today.year, today.year + 1)
                .mapNotNull { y -> runCatching { LocalDate.of(y, m, d) }.getOrNull() }
                .minByOrNull { kotlin.math.abs(ChronoUnit.DAYS.between(today, it)) }
        }
        fun dow(name: String) = DayOfWeek.of(DAY_NAMES.indexOf(name) + 1)

        val parsed = days.map { it to build(it) }
        val monday = parsed
            .mapNotNull { (r, date) -> date?.takeIf { it.dayOfWeek == dow(r.name) }?.minusDays((dow(r.name).value - 1).toLong()) }
            .groupingBy { it }.eachCount().maxByOrNull { it.value }?.key

        return parsed.map { (r, date) ->
            val ok = date != null && date.dayOfWeek == dow(r.name)
            val finalDate = when {
                ok -> date
                monday != null -> monday.plusDays((dow(r.name).value - 1).toLong())
                else -> date
            }
            DayMenu(capitalize(r.name), finalDate, r.dishes)
        }
    }

    private fun findPhones(text: String): List<String> {
        val seen = mutableSetOf<String>()
        val out = mutableListOf<String>()
        for (m in PHONE_RX.findAll(text)) {
            var digits = m.value.filter { it.isDigit() || it == '+' }
            if (digits.startsWith("+421")) digits = "0" + digits.removePrefix("+421")
            if (digits.length != 10 || !digits.startsWith("0")) continue
            if (seen.add(digits)) out += clean(m.value)
        }
        return out
    }

    private fun formatPrice(m: MatchResult): String {
        val euros = m.groupValues[1]
        val cents = m.groupValues[2].ifEmpty { "00" }
        return "$euros,$cents €"
    }

    private fun normalizePriceText(s: String): String {
        val m = Regex("(\\d{1,3})(?:[,.](\\d{2})|,-)").find(s) ?: return s
        return "${m.groupValues[1]},${m.groupValues[2].ifEmpty { "00" }} €"
    }

    private fun letters(s: String) = s.count { it.isLetter() }

    private fun capitalize(s: String) = s.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}
