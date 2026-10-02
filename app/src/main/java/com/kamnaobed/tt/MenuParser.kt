package com.kamnaobed.tt

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode

/** Jedna reštaurácia – názov záložky a vyčistené HTML jej sekcie. */
data class Restaurant(val name: String, val html: String)

/**
 * Vytiahne zo stránky "Kam na obed" sekcie jednotlivých reštaurácií.
 *
 * Stránka beží na CMS WEBY PORTÁL. Najprv skúsime kotvy z navigácie
 * (#mid_XXXX), ak to nevyjde, rozdelíme obsah podľa nadpisov h2.
 * Za sekciu reštaurácie považujeme len blok, ktorý obsahuje názov dňa.
 */
object MenuParser {

    private val DAY = Regex("pondelok|utorok|streda|štvrtok|piatok", RegexOption.IGNORE_CASE)

    fun parse(html: String, baseUri: String): List<Restaurant> {
        val doc = Jsoup.parse(html, baseUri)
        doc.select("script, style, noscript, iframe, form, a[href^=javascript]").remove()
        // Absolútne odkazy, aby fungovali PDF menu aj mimo pôvodnej stránky.
        doc.select("a[href]").forEach { it.attr("href", it.absUrl("href")) }
        doc.select("img[src]").forEach { it.attr("src", it.absUrl("src")) }

        val byAnchors = parseByAnchors(doc)
        return if (byAnchors.size >= 2) byAnchors else parseByHeadings(doc)
    }

    private fun findTarget(scope: Element, id: String): Element? =
        scope.getElementById(id) ?: scope.selectFirst("a[name=$id]")

    private fun parseByAnchors(doc: Document): List<Restaurant> {
        val links = doc.select("a[href*=#mid_]")
            .map { it.attr("href").substringAfter('#') to it.text().trim() }
            .filter { it.first.isNotBlank() }
            .distinctBy { it.first }

        val targets = links.mapNotNull { (id, label) ->
            findTarget(doc, id)?.let { Triple(id, label, it) }
        }
        val targetIds = targets.map { it.first }.toSet()

        val result = mutableListOf<Pair<Element, Restaurant>>()
        for ((id, label, start) in targets) {
            if (label.isBlank() || label.equals("Prehľad", ignoreCase = true)) continue
            var block: Element? = start
            while (block != null && !DAY.containsMatchIn(block.text())) {
                val parent = block.parent()
                block = if (parent == null || parent.tagName() == "body" ||
                    targetIds.any { it != id && findTarget(parent, it) != null }
                ) null else parent
            }
            if (block != null && result.none { it.first === block }) {
                result += block to Restaurant(shortName(label), block.outerHtml())
            }
        }
        return result.map { it.second }
    }

    private fun parseByHeadings(doc: Document): List<Restaurant> {
        val out = mutableListOf<Restaurant>()
        for (h2 in doc.select("h2")) {
            val name = h2.text().trim()
            if (name.isEmpty()) continue

            // Vystúp čo najvyššie, kým rodič obsahuje len tento jeden h2.
            var block: Element = h2
            while (true) {
                val parent = block.parent() ?: break
                if (parent.tagName() == "body" || parent.select("h2").size > 1) break
                block = parent
            }

            val (html, text) = if (block !== h2 && DAY.containsMatchIn(block.text())) {
                block.outerHtml() to block.text()
            } else {
                collectSiblings(block)
            }
            if (DAY.containsMatchIn(text)) out += Restaurant(shortName(name), html)
        }
        return out
    }

    /** "Reštaurácia Novstav" → "Novstav", aby sa záložky zmestili. */
    private fun shortName(name: String): String =
        name.replace(Regex("^(Reštaurácia|Penzión)\\s+", RegexOption.IGNORE_CASE), "").ifBlank { name }

    /** Plochá štruktúra: h2 + nasledujúci súrodenci až po ďalší h2. */
    private fun collectSiblings(start: Element): Pair<String, String> {
        val html = StringBuilder(start.outerHtml())
        val text = StringBuilder(start.text())
        var node = start.nextSibling()
        while (node != null) {
            if (node is Element) {
                if (node.tagName() == "h2" || node.select("h2").isNotEmpty()) break
                if (node.text().contains("Mobilná aplikácia")) break
                text.append(' ').append(node.text())
            } else if (node is TextNode) {
                text.append(' ').append(node.text())
            }
            html.append(node.outerHtml())
            node = node.nextSibling()
        }
        return html.toString() to text.toString()
    }
}
