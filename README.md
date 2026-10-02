# Kam na obed – Turčianske Teplice (Android)

Neoficiálna Android appka, ktorá zobrazuje denné menu reštaurácií zo stránky
https://www.turciansketeplice.sk/kam-na-obed-1.html

## Čo vie
- záložka pre každú reštauráciu (Bistro, Mimosa, Panda, Novstav, Anesis… podľa toho, čo je na webe)
- automaticky skočí na **dnešný deň** a označí ho štítkom DNES
- upozorní, ak reštaurácia ešte nezverejnila menu na aktuálny týždeň
- klikateľné telefónne čísla na objednávku, PDF menu sa otvorí v prehliadači
- **dáta sa berú priamo z podstránky mesta** – pri každom spustení appky a ručne potiahnutím nadol
- posledné stiahnuté menu ostáva uložené aj offline
- tmavý režim, zapamätá si poslednú vybranú reštauráciu

Dáta sa čítajú priamo zo stránky mesta (Jsoup) – nič netreba udržiavať na serveri.
Ak mesto zmení štruktúru stránky a parser nič nenájde, appka zobrazí celú pôvodnú stránku.

## Ako získať APK

### A) Cez GitHub (bez inštalácie čohokoľvek)
1. Vytvorte nový repozitár na GitHube a nahrajte doň obsah tohto priečinka
   (`git init && git add . && git commit -m init && git push`).
2. V záložke **Actions** sa spustí „Build APK“ (cca 3–5 min).
3. Po dobehnutí stiahnite artefakt `kam-na-obed-apk`, rozbaľte a `app-debug.apk`
   nainštalujte do telefónu (povoľte inštaláciu z neznámych zdrojov).

### B) V Android Studiu
File → Open → vyberte tento priečinok → počkajte na Gradle sync → ▶ Run.
Alebo z príkazového riadku (s nainštalovaným Android SDK): `./gradlew assembleDebug`.

## Štruktúra
- `MenuRepository.kt` – stiahnutie stránky + offline cache
- `MenuParser.kt` – rozdelenie stránky na sekcie reštaurácií
- `HtmlTemplate.kt` – vzhľad menu, zvýraznenie dnešného dňa, odkazy na telefóny
- `MainActivity.kt` – záložky, obnovovanie, otváranie odkazov

Požiadavky: minSdk 26 (Android 8.0+), targetSdk 35, AGP 8.9, Kotlin 2.1, Gradle 8.14.
