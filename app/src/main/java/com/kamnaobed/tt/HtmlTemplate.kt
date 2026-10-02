package com.kamnaobed.tt

import java.util.Calendar

/** Dnešný pracovný deň – na zvýraznenie v menu. Cez víkend null. */
data class Today(val dayName: String, val day: Int, val month: Int) {
    companion object {
        fun now(): Today? {
            val c = Calendar.getInstance()
            val name = when (c.get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY -> "Pondelok"
                Calendar.TUESDAY -> "Utorok"
                Calendar.WEDNESDAY -> "Streda"
                Calendar.THURSDAY -> "Štvrtok"
                Calendar.FRIDAY -> "Piatok"
                else -> return null
            }
            return Today(name, c.get(Calendar.DAY_OF_MONTH), c.get(Calendar.MONTH) + 1)
        }
    }

    fun toJson() = """{"name":"$dayName","d":$day,"m":$month}"""
}

object HtmlTemplate {

    fun build(restaurant: Restaurant, today: Today?): String = StringBuilder()
        .append("<!doctype html><html lang=\"sk\"><head><meta charset=\"utf-8\">")
        .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">")
        .append("<style>").append(CSS).append("</style></head><body>")
        .append(restaurant.html)
        .append("<script>var TODAY = ").append(today?.toJson() ?: "null").append(";")
        .append(JS).append("</script></body></html>")
        .toString()

    private const val CSS = """
:root{--bg:#faf8f4;--fg:#1d1b19;--muted:#6f6a63;--card:#ffffff;--line:#e7e2d9;--accent:#b4441c;--today:#fff1e6;--warn-bg:#fde8c8;--warn-fg:#5b3a00}
@media (prefers-color-scheme: dark){:root{--bg:#141312;--fg:#ecebe7;--muted:#a49f97;--card:#1e1d1b;--line:#35322e;--accent:#ff8b5e;--today:#3a2519;--warn-bg:#4a3713;--warn-fg:#ffe2ad}}
html{-webkit-text-size-adjust:100%}
body{margin:0;padding:12px 14px 48px;background:var(--bg);color:var(--fg);font:15px/1.45 system-ui,Roboto,sans-serif;overflow-wrap:break-word}
h2{display:none}
h3{font-size:21px;margin:4px 0 10px}
h4,h5{font-size:16px;margin:22px 0 6px;color:var(--accent)}
em,i{font-style:normal}
p{margin:6px 0}
a{color:var(--accent)}
img{max-width:100%;height:auto}
table{width:100%!important;border-collapse:collapse;margin:6px 0 14px;background:var(--card);border-radius:12px;overflow:hidden;box-shadow:0 0 0 1px var(--line)}
td,th{width:auto!important;padding:8px;border-top:1px solid var(--line);vertical-align:top;text-align:left;font-weight:400}
tr:first-child>td,tr:first-child>th{border-top:none}
td>br:first-child,th>br:first-child{display:none}
th{font-weight:600;color:var(--accent)}
td:last-child{color:var(--muted);font-size:12px}
td:first-child,td:last-child,td strong,td b{white-space:nowrap}
td:last-child{white-space:normal;min-width:4.5em}
th.dayrow{font-size:16px;padding-top:10px}
table.menu td{white-space:nowrap}
table.menu td.main{white-space:normal;width:100%!important}
table.menu td:last-child{white-space:normal;min-width:0;width:3.2em!important}
td .w{color:var(--muted);font-size:13px;margin-right:4px}
table.menu td{padding:8px 6px}
.today-head{background:var(--today);border-radius:8px;padding:6px 8px;margin-left:-8px;margin-right:-8px}
table.today{box-shadow:0 0 0 2px var(--accent)}
.badge{display:inline-block;white-space:nowrap;margin-left:8px;padding:1px 8px;border-radius:999px;background:var(--accent);color:#fff;font-size:11px;font-weight:700;letter-spacing:.05em;vertical-align:middle}
.stale{background:var(--warn-bg);color:var(--warn-fg);padding:9px 11px;border-radius:10px;margin-bottom:10px;font-size:13px}
details{margin:14px 0;color:var(--muted);font-size:12px}
summary{cursor:pointer;font-weight:600;font-size:13px}
a.tel{font-weight:600;text-decoration:none;border-bottom:1px dashed currentColor}
"""

    private const val JS = """
(function(){
  // 1) Klikateľné telefónne čísla
  var rx = /(?:\+421|0)[\s ]?\d{2,3}(?:[\s ]?\d{2,3}){2,3}/g;
  var walker = document.createTreeWalker(document.body, NodeFilter.SHOW_TEXT, null);
  var nodes = []; while (walker.nextNode()) nodes.push(walker.currentNode);
  nodes.forEach(function(t){
    var parent = t.parentNode;
    if (!parent || (parent.closest && parent.closest('a,script,style'))) return;
    var s = t.nodeValue, last = 0, m, frag = null;
    rx.lastIndex = 0;
    while ((m = rx.exec(s))) {
      if (m[0].replace(/\D/g,'').length < 10) continue;
      frag = frag || document.createDocumentFragment();
      frag.appendChild(document.createTextNode(s.slice(last, m.index)));
      var a = document.createElement('a');
      a.href = 'tel:' + m[0].replace(/[\s ]/g,'');
      a.className = 'tel'; a.textContent = m[0];
      frag.appendChild(a); last = m.index + m[0].length;
    }
    if (!frag) return;
    frag.appendChild(document.createTextNode(s.slice(last)));
    parent.replaceChild(frag, t);
  });

  // 2) Dlhý zoznam alergénov schovať do rozbaľovača
  document.querySelectorAll('p,div').forEach(function(el){
    var tx = (el.textContent||'').trim();
    if (tx.indexOf('Zoznam alergénov') !== 0 || tx.length > 2000 || el.querySelector('table,h3,h4')) return;
    var d = document.createElement('details'), s = document.createElement('summary');
    s.textContent = 'Zoznam alergénov'; d.appendChild(s);
    var body = document.createElement('div');
    body.innerHTML = el.innerHTML.replace(/<strong>\s*Zoznam alergénov:?\s*<\/strong>:?/i,'');
    d.appendChild(body);
    el.parentNode.replaceChild(d, el);
  });

  // 3) Riadok s názvom dňa v tabuľke roztiahnuť cez celú šírku
  var dayRx = /^(pondelok|utorok|streda|štvrtok|piatok)\b/i;
  document.querySelectorAll('tr').forEach(function(tr){
    var cells = tr.children; if (cells.length < 2) return;
    var first = cells[0];
    if (!dayRx.test((first.textContent||'').trim())) return;
    for (var k = 1; k < cells.length; k++) if ((cells[k].textContent||'').trim()) return;
    var n = cells.length;
    while (tr.children.length > 1) tr.removeChild(tr.lastElementChild);
    first.colSpan = n; first.classList.add('dayrow');
  });

  // 4) V tabuľkách dať najviac miesta stĺpcu s popisom jedla
  document.querySelectorAll('table').forEach(function(t){
    var len = [];
    t.querySelectorAll('tr').forEach(function(tr){
      for (var c = 0; c < tr.children.length; c++) {
        if (tr.children[c].colSpan > 1) return;
        len[c] = (len[c]||0) + (tr.children[c].textContent||'').trim().length;
      }
    });
    if (len.length < 3) return;
    var main = 0;
    for (var c = 1; c < len.length; c++) if ((len[c]||0) > (len[main]||0)) main = c;
    // Krátky stĺpec pred popisom (gramáž) pripoj k popisu, nech je viac miesta.
    var merge = false;
    if (main > 1 && len.length >= 5) {
      merge = true;
      t.querySelectorAll('tr').forEach(function(tr){
        var w = tr.children[main-1];
        if (w && (w.textContent||'').trim().length > 10) merge = false;
      });
    }
    t.querySelectorAll('tr').forEach(function(tr){
      var cell = tr.children[main];
      if (!cell || cell.colSpan !== 1) return;
      cell.classList.add('main');
      if (merge) {
        var w = tr.children[main-1], txt = (w.textContent||'').trim();
        if (txt) { var sp = document.createElement('span'); sp.className = 'w'; sp.textContent = txt;
                   var br = cell.querySelector('br');
                   if (br && br === cell.firstChild) br.remove();
                   cell.insertBefore(sp, cell.firstChild); }
        w.remove();
      }
    });
    t.classList.add('menu');
  });

  // 5) Zvýrazniť dnešný deň a posunúť sa naň
  if (!TODAY) return;
  var name = TODAY.name.toLowerCase(), hit = null;
  var cands = document.querySelectorAll('h1,h3,h4,h5,h6,th,td,p,strong,b,em,div');
  for (var i = 0; i < cands.length; i++) {
    var tx = (cands[i].textContent||'').replace(/\s+/g,' ').trim();
    if (tx.length <= 40 && tx.toLowerCase().indexOf(name) === 0) { hit = cands[i]; break; }
  }
  if (!hit) return;
  // <em><h4>Piatok</h4></em> → zvýrazni samotný nadpis, nie obal
  while (hit.children.length === 1 && /^(H[1-6]|P|DIV)$/.test(hit.children[0].tagName) &&
         (hit.children[0].textContent||'').trim() === (hit.textContent||'').trim()) hit = hit.children[0];
  var label = (hit.textContent||'').replace(/\s+/g,' ');
  var dateRx = new RegExp('(^|\\D)' + TODAY.d + '\\.\\s*' + TODAY.m + '(\\D|$)');
  var target = hit;
  var table = hit.closest && hit.closest('table');
  if (table) { table.classList.add('today'); target = table; }
  else { hit.classList.add('today-head'); }
  var badge = document.createElement('span'); badge.className = 'badge'; badge.textContent = 'DNES';
  hit.appendChild(badge);
  if (/\d+\.\s*\d+/.test(label) && !dateRx.test(label)) {
    var w = document.createElement('div'); w.className = 'stale';
    w.textContent = 'Pozor: menu je pravdepodobne z iného týždňa – reštaurácia ho ešte neaktualizovala.';
    target.parentNode.insertBefore(w, target);
    target = w;
  }
  setTimeout(function(){
    var y = target.getBoundingClientRect().top + window.pageYOffset - 10;
    window.scrollTo(0, Math.max(0, y));
  }, 60);
})();
"""
}
