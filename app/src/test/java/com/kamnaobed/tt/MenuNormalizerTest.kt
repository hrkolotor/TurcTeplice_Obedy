package com.kamnaobed.tt

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** Overenie na skutočnej stránke z týždňa 5.–9. 10. 2026 (src/test/resources). */
class MenuNormalizerTest {

    private val today = LocalDate.of(2026, 10, 9)
    private val menus: Map<String, StructuredMenu> by lazy {
        val html = javaClass.classLoader!!.getResource("kam-na-obed-2026-10-05.html")!!.readText()
        MenuParser.parse(html, "https://www.turciansketeplice.sk/kam-na-obed-1.html")
            .associate { it.name to MenuNormalizer.normalize(it, today)!! }
    }

    private fun day(rest: String, date: LocalDate) = menus.getValue(rest).days.first { it.date == date }

    @Test fun allRestaurantsAreRecognized() {
        assertEquals(listOf("Bistro", "Mimosa", "Panda", "Novstav", "Anesis"), menus.keys.toList())
    }

    @Test fun bistroTableWithCaption() {
        val fri = day("Bistro", today)
        assertEquals("Piatok", fri.dayName)
        assertTrue(fri.dishes.first().soup)
        val d1 = fri.dishes.first { it.label == "1" }
        assertEquals("Pečené bravčové mäso, dusená kapusta, dom. parená knedľa", d1.name)
        assertEquals("6,50 €", d1.price)
        assertEquals("8,50 €", menus.getValue("Bistro").weekly.single().dishes.single().price)
    }

    @Test fun mimosaParagraphListDropsIngredientsAndAllergens() {
        val thu = day("Mimosa", LocalDate.of(2026, 10, 8))
        assertEquals("Kulajda", thu.dishes.first { it.soup }.name)
        val lasagne = thu.dishes.first { it.label == "1" }
        assertEquals("Lasagne bolognese", lasagne.name)
        assertEquals("8,20 €", lasagne.price)
    }

    @Test fun pandaHeaderInsideFirstRow() {
        val fri = day("Panda", today)
        assertEquals("Francúzska s mušličkami", fri.dishes.first { it.soup }.name)
        assertEquals("8,50 €", fri.dishes.first { it.label == "F" }.price)
        assertTrue(fri.dishes.none { it.name.contains("príbor") })
    }

    @Test fun novstavPricesFromPriceListAndWrongDateFixed() {
        val fri = day("Novstav", today)          // na webe chybne "Piatok 7. 10."
        assertEquals("7,00 €", fri.dishes.first { it.label == "1" }.price)
        assertEquals("7,50 €", fri.dishes.first { it.label == "2" }.price)   // cena priamo v texte
        assertEquals("8,50 €", fri.dishes.first { it.label == "XXL" }.price)
        val wed = day("Novstav", LocalDate.of(2026, 10, 7))
        assertEquals("Kulajda", wed.dishes.first { it.soup }.name)
    }

    @Test fun anesisDefaultPrice() {
        val mon = day("Anesis", LocalDate.of(2026, 10, 5))
        val main = mon.dishes.first { !it.soup }
        assertEquals("Segedínsky guláš s parenou knedľou", main.name)
        assertEquals("7,90 €", main.price)
        assertEquals(listOf("0910 115 685"), menus.getValue("Anesis").phones)
    }

    @Test fun unknownFormatFallsBack() {
        val r = Restaurant("X", "<div><h2>X</h2><p>Dnes zatvorené.</p></div>")
        assertEquals(null, MenuNormalizer.normalize(r, today))
        assertNotNull(HtmlTemplate.build(r, null))
    }
}
