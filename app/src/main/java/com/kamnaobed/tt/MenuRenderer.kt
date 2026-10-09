package com.kamnaobed.tt

import java.time.DayOfWeek
import java.time.LocalDate

/** Jednotné zobrazenie menu: pre každú prevádzku rovnaké karty – deň, dátum, jedlo, cena. */
object MenuRenderer {

    fun render(menu: StructuredMenu, today: LocalDate = LocalDate.now()): String {
        val sb = StringBuilder()
        sb.append("<!doctype html><html lang=\"sk\"><head><meta charset=\"utf-8\">")
            .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">")
            .append("<style>").append(CSS).append("</style></head><body>")

        sb.append("<header><h1>").append(esc(menu.title)).append("</h1>")
        if (menu.phones.isNotEmpty()) {
            sb.append("<div class=\"phones\">")
            menu.phones.forEach { p ->
                sb.append("<a class=\"tel\" href=\"tel:").append(p.filter { it.isDigit() || it == '+' }).append("\">")
                    .append(PHONE_ICON).append(esc(p)).append("</a>")
            }
            sb.append("</div>")
        }
        sb.append("</header>")

        val dated = menu.days.mapNotNull { it.date }
        val isWeekday = today.dayOfWeek != DayOfWeek.SATURDAY && today.dayOfWeek != DayOfWeek.SUNDAY
        if (isWeekday && dated.isNotEmpty() && dated.all { it.isBefore(today) }) {
            sb.append("<div class=\"stale\">Menu na tento týždeň zatiaľ nie je zverejnené – ")
                .append("zobrazujem posledné dostupné.</div>")
        }

        val todayName = DAY_NAMES[today.dayOfWeek.value - 1]
        for (day in menu.days) {
            val isToday = if (day.date != null) day.date == today else day.dayName.equals(todayName, true)
            val isPast = day.date != null && day.date.isBefore(today)
            val cls = buildString {
                append("card")
                if (isToday) append(" today")
                if (isPast) append(" past")
            }
            sb.append("<section class=\"").append(cls).append("\"").append(if (isToday) " id=\"today\"" else "").append(">")
            sb.append("<h2><span class=\"day\">").append(esc(day.dayName)).append("</span>")
            day.date?.let { sb.append("<span class=\"date\">").append(it.dayOfMonth).append(". ").append(it.monthValue).append(".</span>") }
            if (isToday) sb.append("<span class=\"badge\">DNES</span>")
            sb.append("</h2>")
            appendDishes(sb, day.dishes)
            sb.append("</section>")
        }

        for (group in menu.weekly) {
            sb.append("<section class=\"card weekly\"><h2><span class=\"day\">")
                .append(esc(group.title)).append("</span></h2>")
            appendDishes(sb, group.dishes)
            sb.append("</section>")
        }

        sb.append("<footer>Zdroj: turciansketeplice.sk · Zmena jedálneho lístka vyhradená.</footer>")
        sb.append("<script>var t=document.getElementById('today');")
            .append("if(t){setTimeout(function(){window.scrollTo(0,Math.max(0,t.getBoundingClientRect().top+window.pageYOffset-8));},50);}</script>")
        sb.append("</body></html>")
        return sb.toString()
    }

    private fun appendDishes(sb: StringBuilder, dishes: List<Dish>) {
        sb.append("<ul>")
        for (d in dishes) {
            sb.append("<li class=\"").append(if (d.soup) "dish soup" else "dish").append("\">")
            val label = if (d.soup) "Polievka" else d.label
            if (label != null) sb.append("<span class=\"label\">").append(esc(label)).append("</span>")
            sb.append("<span class=\"name\">").append(esc(d.name)).append("</span>")
            if (d.price != null) sb.append("<span class=\"price\">").append(esc(d.price)).append("</span>")
            sb.append("</li>")
        }
        sb.append("</ul>")
    }

    private val DAY_NAMES = listOf("Pondelok", "Utorok", "Streda", "Štvrtok", "Piatok", "Sobota", "Nedeľa")

    private fun esc(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

    private const val PHONE_ICON =
        "<svg viewBox=\"0 0 24 24\" width=\"16\" height=\"16\" aria-hidden=\"true\"><path fill=\"currentColor\" " +
            "d=\"M6.6 10.8a15.1 15.1 0 0 0 6.6 6.6l2.2-2.2a1 1 0 0 1 1-.25 11.4 11.4 0 0 0 3.6.57 1 1 0 0 1 1 1V20a1 1 0 0 1-1 1A17 17 0 0 1 3 4a1 1 0 0 1 1-1h3.5a1 1 0 0 1 1 1c0 1.25.2 2.45.57 3.57a1 1 0 0 1-.25 1z\"/></svg>"

    private const val CSS = """
:root{--bg:#faf8f4;--fg:#1d1b19;--muted:#6f6a63;--card:#ffffff;--line:#ebe6dd;--accent:#b4441c;--accent-soft:#fbeee6;--warn-bg:#fde8c8;--warn-fg:#5b3a00}
@media (prefers-color-scheme: dark){:root{--bg:#141312;--fg:#ecebe7;--muted:#a49f97;--card:#1e1d1b;--line:#33302c;--accent:#ff8b5e;--accent-soft:#3a2519;--warn-bg:#4a3713;--warn-fg:#ffe2ad}}
*{box-sizing:border-box}
html{-webkit-text-size-adjust:100%}
body{margin:0;padding:12px 14px 40px;background:var(--bg);color:var(--fg);font:15px/1.4 system-ui,Roboto,sans-serif}
header{margin:2px 2px 12px}
header h1{font-size:20px;margin:0 0 8px;font-weight:600}
.phones{display:flex;flex-wrap:wrap;gap:8px}
a.tel{display:inline-flex;align-items:center;gap:6px;padding:7px 12px;border-radius:999px;background:var(--accent-soft);color:var(--accent);text-decoration:none;font-weight:600;font-size:14px}
.stale{background:var(--warn-bg);color:var(--warn-fg);padding:10px 12px;border-radius:12px;margin-bottom:12px;font-size:13px}
.card{background:var(--card);border-radius:14px;box-shadow:0 0 0 1px var(--line);margin:0 0 12px;overflow:hidden}
.card.today{box-shadow:0 0 0 2px var(--accent)}
.card.past{opacity:.6}
.card h2{display:flex;align-items:baseline;gap:8px;margin:0;padding:11px 14px 9px;font-size:16px;border-bottom:1px solid var(--line)}
.card.today h2{background:var(--accent-soft)}
.card h2 .day{font-weight:700;color:var(--accent)}
.card h2 .date{color:var(--muted);font-weight:500}
.badge{margin-left:auto;padding:2px 9px;border-radius:999px;background:var(--accent);color:#fff;font-size:11px;font-weight:700;letter-spacing:.05em}
ul{list-style:none;margin:0;padding:0}
li.dish{display:flex;align-items:baseline;gap:10px;padding:10px 14px;border-top:1px solid var(--line)}
li.dish:first-child{border-top:none}
.label{flex:0 0 auto;min-width:26px;padding:1px 6px;border-radius:6px;background:var(--line);color:var(--fg);font-size:12px;font-weight:700;text-align:center}
li.soup .label{background:transparent;color:var(--muted);padding:0;min-width:0;font-weight:600;text-transform:uppercase;letter-spacing:.04em;font-size:11px}
li.soup .name{color:var(--muted)}
.name{flex:1 1 auto;min-width:0}
.price{flex:0 0 auto;font-weight:700;white-space:nowrap;font-variant-numeric:tabular-nums}
footer{color:var(--muted);font-size:12px;text-align:center;margin-top:18px}
"""
}
