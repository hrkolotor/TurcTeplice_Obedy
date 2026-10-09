# Kam na obed – Turčianske Teplice (Android)

Neoficiálna Android appka, ktorá zobrazuje denné menu reštaurácií zo stránky
https://www.turciansketeplice.sk/kam-na-obed-1.html

## Čo vie
- **jednotný prehľad pre všetky prevádzky**: deň, dátum, jedlo a cena – bez gramáží
  a alergénov; ceny sa doplnia aj z cenníka v úvode (napr. Novstav „Denné menu 4,5: 7,50 €“,
  Anesis „Cena obedového menu 7,90 €“); zjavne chybný dátum (napr. „Piatok 7. 10.“) sa opraví
- v menu (⋮) je prepínač **Pôvodný formát** – zobrazí sekciu presne ako na webe;
  ak sa dáta niektorej prevádzky nepodarí rozpoznať, pôvodný formát sa použije automaticky
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
- `MenuNormalizer.kt` – vyťaženie dňa, dátumu, jedla a ceny z každej sekcie
- `MenuRenderer.kt` – jednotné zobrazenie (karty po dňoch)
- `src/test/` – test na skutočnej stránke z 5.–9. 10. 2026 (`./gradlew test`)
- `HtmlTemplate.kt` – vzhľad menu, zvýraznenie dnešného dňa, odkazy na telefóny
- `MainActivity.kt` – záložky, obnovovanie, otváranie odkazov

Požiadavky: minSdk 26 (Android 8.0+), targetSdk 36, AGP 8.13, Kotlin 2.1, Gradle 8.14.

## Dôveryhodnosť pri inštalácii
- APK je **release** build (nie ladiaci), cieli na aktuálny Android (API 36), používa len
  povolenie INTERNET a komunikuje iba cez HTTPS.
- Podpisový kľúč je pevný: `app/kamnaobed.keystore`
  SHA-256 odtlačok certifikátu:
  `72:DC:7C:32:00:A9:FC:DF:A0:89:FE:E5:CD:09:3C:76:19:60:41:67:65:C2:CA:FB:8F:10:A5:CE:AE:34:32:4D`
  (tento odtlačok sa zadáva pri registrácii appky v Android Developer Console).
- Repozitár držte **súkromný** – kto má kľúč, môže podpísať appku za vás.
