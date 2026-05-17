# Testy Funkcjonalne — MeshTracker v1

**Wersja dokumentu:** 1.1  
**Data:** 2026-05-15  
**Aplikacja:** MeshTracker v1 (Android)  
**Sprzęt testowy:**
- 1× T-Beam (węzeł główny, połączony przez Bluetooth z telefonem testowym)
- 2× SenseCAP T1000-E (węzły śledzące — jeden stacjonarny, jeden montowany na dronie)

---

## Spis treści

1. [Konfiguracja środowiska testowego](#1-konfiguracja-środowiska-testowego)
2. [TC-CON — Połączenie z serwisem Meshtastic](#2-tc-con--połączenie-z-serwisem-meshtastic)
3. [TC-STS — Pasek statusu połączenia](#3-tc-sts--pasek-statusu-połączenia)
4. [TC-MAP — Mapa i markery węzłów](#4-tc-map--mapa-i-markery-węzłów)
5. [TC-NOD — Lista węzłów](#5-tc-nod--lista-węzłów)
6. [TC-NAV — Nawigacja między ekranami](#6-tc-nav--nawigacja-między-ekranami)
7. [TC-GPS — Aktualizacja pozycji GPS](#7-tc-gps--aktualizacja-pozycji-gps)
8. [TC-DRN — Testy z dronem (ruchomy węzeł)](#8-tc-drn--testy-z-dronem-ruchomy-węzeł)
9. [TC-EDG — Przypadki brzegowe i obsługa błędów](#9-tc-edg--przypadki-brzegowe-i-obsługa-błędów)
10. [TC-MON — Testy skuteczności monitoringu stref](#10-tc-mon--testy-skuteczności-monitoringu-stref)
11. [Matryca pokrycia testami](#11-matryca-pokrycia-testami)

---

## 1. Konfiguracja środowiska testowego

### 1.1 Wymagane oprogramowanie i sprzęt

| Element | Wymaganie |
|---|---|
| Telefon testowy | Android 7.0+ (API 24), Bluetooth 4.0+ |
| Aplikacja Meshtastic | Zainstalowana i uruchomiona (geeksville.mesh) |
| Google Maps API Key | Skonfigurowany w AndroidManifest.xml |
| T-Beam | Firmware Meshtastic, sparowany przez BT z telefonem |
| SenseCAP T1000-E #1 | Firmware Meshtastic, ten sam kanał co T-Beam |
| SenseCAP T1000-E #2 | Firmware Meshtastic, ten sam kanał co T-Beam, **przeznaczony do montażu na dronie** |
| Dron | Zdolny do uniesienia ~60g ładunku, zasięg min. 300 m |

### 1.2 Konfiguracja węzłów Meshtastic przed testami

**Kanał sieciowy:** Wszystkie trzy urządzenia muszą mieć ten sam kanał i klucz szyfrowania.

**Zalecane ustawienia dla T1000-E na dronie (TC-DRN):**
- GPS update interval: 10s (dla płynnej trasy na mapie)
- Position precision: wysoka
- Transmission interval: 15s
- Role: TRACKER

**Zalecane ustawienia dla T1000-E stacjonarnego:**
- GPS update interval: 30s
- Role: CLIENT_MUTE (aby nie retransmitował niepotrzebnie)

**Zalecane ustawienia dla T-Beam (węzeł główny):**
- Role: CLIENT
- BT pairing: ON, sparowany z telefonem testowym

### 1.3 Oznaczenia w scenariuszach

- **PRECONDITION** — warunki, które muszą być spełnione przed testem
- **KROKI** — kolejne czynności do wykonania
- **EXPECTED** — oczekiwany rezultat
- **ACTUAL** — pole do wypełnienia podczas testu
- ✅ PASS / ❌ FAIL / ⏭️ SKIP

---

## 2. TC-CON — Połączenie z serwisem Meshtastic

### TC-CON-001 — Połączenie z uruchomioną aplikacją Meshtastic

**Cel:** Weryfikacja nawiązania połączenia z IMeshService gdy Meshtastic jest aktywny.

**PRECONDITION:**
- Aplikacja Meshtastic jest zainstalowana i uruchomiona w tle
- T-Beam jest sparowany z telefonem przez Bluetooth i widoczny w Meshtastic
- MeshTracker **nie** jest uruchomiony

**KROKI:**
1. Uruchom aplikację MeshTracker
2. Odczekaj max. 5 sekund
3. Obserwuj pasek statusu (górna belka)
4. Sprawdź logi przez `adb logcat -s MeshServiceManager MapViewModel`

**EXPECTED:**
- Pasek statusu przechodzi: `Disconnected` → `Connecting` → `Connected`
- Wskaźnik połączenia zmienia kolor na zielony
- Na liście węzłów pojawia się minimum 1 węzeł (T-Beam)
- Log zawiera: `"Service connected"`, `"Radio connection state: CONNECTED"`

**ACTUAL:** _______________  
**WYNIK:** ✅ / ❌  **Data:** ________

---

### TC-CON-002 — Próba połączenia gdy Meshtastic nie jest uruchomiony

**Cel:** Weryfikacja zachowania aplikacji gdy serwis Meshtastic niedostępny.

**PRECONDITION:**
- Aplikacja Meshtastic jest **wyłączona** (force stop)
- MeshTracker nie jest uruchomiony

**KROKI:**
1. Uruchom aplikację MeshTracker
2. Odczekaj 10 sekund
3. Obserwuj pasek statusu

**EXPECTED:**
- Pasek statusu pokazuje `Disconnected` (czerwony/szary)
- Aplikacja nie crashuje
- Log zawiera: `"Failed to bind to service"` lub `"Failed to start connection"`
- Brak węzłów na mapie i liście

**ACTUAL:** _______________  
**WYNIK:** ✅ / ❌  **Data:** ________

---

### TC-CON-003 — Reconnect po wyłączeniu i włączeniu Meshtastic

**Cel:** Weryfikacja automatycznego reconnectu po przywróceniu serwisu.

**PRECONDITION:**
- MeshTracker jest uruchomiony i połączony (stan CONNECTED)

**KROKI:**
1. Wymuś zatrzymanie aplikacji Meshtastic (Settings → Apps → Force Stop)
2. Obserwuj pasek statusu w MeshTracker — powinien pokazać Disconnected
3. Uruchom ponownie aplikację Meshtastic
4. Odczekaj max. 10 sekund
5. Wróć do MeshTracker

**EXPECTED:**
- Po Force Stop: pasek statusu → `Disconnected`
- Po ponownym uruchomieniu Meshtastic: pasek statusu → `Connecting` → `Connected`
- Węzły pojawiają się na mapie ponownie
- Aplikacja nie wymaga ręcznego restartu

**ACTUAL:** _______________  
**WYNIK:** ✅ / ❌  **Data:** ________

---

### TC-CON-004 — Połączenie gdy radio (T-Beam) nie jest podłączone przez BT

**Cel:** Weryfikacja stanu CONNECTING gdy serwis Meshtastic działa, ale radio niepołączone.

**PRECONDITION:**
- Meshtastic jest uruchomiony
- T-Beam jest **wyłączony** lub **rozparowany z BT**

**KROKI:**
1. Uruchom MeshTracker
2. Obserwuj pasek statusu i logi przez 15 sekund
3. Włącz T-Beam i poczekaj na sparowanie przez BT (max. 30s)
4. Obserwuj zmianę stanu

**EXPECTED:**
- Krok 2: Status → `Connecting` (serwis połączony, radio niepołączone)
- Krok 4: Status → `Connected` po wykryciu radia
- Log: `"Radio connected detected via polling"`

**ACTUAL:** _______________  
**WYNIK:** ✅ / ❌  **Data:** ________

---

### TC-CON-005 — Stabilność połączenia po 30 minutach działania

**Cel:** Weryfikacja braku memory leaków i stabilności długotrwałego połączenia.

**PRECONDITION:**
- Wszystkie 3 węzły aktywne, MeshTracker połączony

**KROKI:**
1. Uruchom MeshTracker w stanie CONNECTED
2. Pozostaw aplikację na pierwszym planie przez 30 minut
3. Co 5 minut zanotuj: stan połączenia, liczbę węzłów, zużycie RAM (Android Developer Options → Memory)
4. Po 30 minutach sprawdź stan

**EXPECTED:**
- Stan: `Connected` przez cały czas
- Liczba węzłów: stabilna (nie rośnie nieskończenie)
- Zużycie RAM: stabilne (bez ciągłego wzrostu > 5 MB/5min)
- Periodic refresh wykonuje się co ~5 sekund (widoczne w logach)

**ACTUAL:** _______________  
**WYNIK:** ✅ / ❌  **Data:** ________

---

## 3. TC-STS — Pasek statusu połączenia

### TC-STS-001 — Wyświetlanie stanu CONNECTED

**PRECONDITION:** MeshTracker uruchomiony i połączony z T-Beam.

**KROKI:**
1. Obserwuj górną belkę aplikacji (ConnectionStatusBar)

**EXPECTED:**
- Kolor: zielony
- Tekst: "Connected" lub odpowiednik
- Wyświetlana liczba węzłów (np. "3 nodes")

**ACTUAL:** _______________  
**WYNIK:** ✅ / ❌

---

### TC-STS-002 — Wyświetlanie stanu DISCONNECTED

**PRECONDITION:** T-Beam wyłączony lub Meshtastic force-stopped.

**KROKI:**
1. Doprowadź aplikację do stanu Disconnected
2. Obserwuj pasek statusu

**EXPECTED:**
- Kolor: czerwony lub szary
- Tekst: "Disconnected"
- Liczba węzłów: 0

**ACTUAL:** _______________  
**WYNIK:** ✅ / ❌

---

### TC-STS-003 — Wyświetlanie stanu CONNECTING

**PRECONDITION:** Meshtastic uruchomiony, T-Beam rozłączony (jak w TC-CON-004).

**KROKI:**
1. Doprowadź aplikację do stanu Connecting
2. Obserwuj pasek statusu

**EXPECTED:**
- Kolor: żółty/pomarańczowy lub animacja
- Tekst: "Connecting"
- CircularProgressIndicator widoczny na mapie (górna część)

**ACTUAL:** _______________  
**WYNIK:** ✅ / ❌

---

### TC-STS-004 — Aktualizacja licznika węzłów

**PRECONDITION:** MeshTracker połączony, widoczny tylko T-Beam (1 węzeł).

**KROKI:**
1. Zanotuj aktualną liczbę węzłów na pasku statusu
2. Włącz SenseCAP T1000-E #1 (stacjonarny)
3. Odczekaj max. 60 sekund (czas na dołączenie do sieci mesh)
4. Włącz SenseCAP T1000-E #2
5. Odczekaj max. 60 sekund

**EXPECTED:**
- Po kroku 3: licznik wzrasta o 1 (np. "2 nodes")
- Po kroku 5: licznik wzrasta o 1 (np. "3 nodes")
- Aktualizacja następuje automatycznie bez restartu aplikacji

**ACTUAL:** _______________  
**WYNIK:** ✅ / ❌

---

## 4. TC-MAP — Mapa i markery węzłów

### TC-MAP-001 — Wyświetlenie markera węzła z pozycją GPS

**PRECONDITION:**
- MeshTracker połączony
- T-Beam z aktywnym GPS na zewnątrz budynku (fix GPS)
- T-Beam ma ważną pozycję (lat/lng różne od 0,0)

**KROKI:**
1. Przejdź do ekranu mapy
2. Odczekaj 10 sekund na pobranie węzłów
3. Obserwuj mapę

**EXPECTED:**
- Na mapie widoczny jest marker w lokalizacji T-Beam
- Kliknięcie na marker otwiera info window z nazwą węzła
- Info window zawiera: status online/offline, poziom baterii (jeśli dostępny), SNR

**ACTUAL:** _______________  
**WYNIK:** ✅ / ❌

---

### TC-MAP-002 — Brak markera dla węzła bez pozycji GPS

**PRECONDITION:**
- MeshTracker połączony
- T1000-E włączony wewnątrz budynku (brak GPS fix, lat=0 lng=0)

**KROKI:**
1. Przejdź do ekranu mapy
2. Sprawdź czy węzeł bez GPS jest widoczny na mapie
3. Przejdź do listy węzłów — sprawdź czy węzeł jest widoczny tam

**EXPECTED:**
- Na mapie: **brak markera** dla węzła z pozycją (0,0) lub bez fixa GPS
- Na liście węzłów: węzeł **jest** widoczny (z informacją o braku pozycji)
- Log: `"hasPosition: false"` dla tego węzła

**ACTUAL:** _______________  
**WYNIK:** ✅ / ❌

---

### TC-MAP-003 — Kliknięcie markera i info window

**PRECONDITION:** Minimum 1 węzeł z pozycją widoczny na mapie.

**KROKI:**
1. Kliknij na marker węzła
2. Obserwuj info window
3. Kliknij w pustym miejscu mapy

**EXPECTED:**
- Krok 2: Info window pojawia się z tytułem (nazwa węzła) i snippetem (Online/Offline • Battery: X% • SNR: X.X dB)
- Krok 3: Info window znika, `selectedNodeId` ustawiony na null

**ACTUAL:** _______________  
**WYNIK:** ✅ / ❌

---

### TC-MAP-004 — Automatyczne centrowanie kamery na wybranym węźle

**PRECONDITION:** MeshTracker połączony, 2+ węzły z pozycją na mapie w różnych lokalizacjach.

**KROKI:**
1. Przesuń mapę ręcznie tak, żeby żaden marker nie był widoczny
2. Przejdź do listy węzłów
3. Kliknij węzeł z pozycją GPS
4. Obserwuj mapę

**EXPECTED:**
- Mapa automatycznie centruje się na wybranym węźle (animacja kamery)
- Zoom: 15 (uliczny)
- Wybrany marker jest zaznaczony lub wyróżniony

**ACTUAL:** _______________  
**WYNIK:** ✅ / ❌

---

### TC-MAP-005 — Domyślna pozycja kamery (centrum Polski)

**PRECONDITION:** Żaden węzeł nie ma ważnej pozycji GPS (wszystkie wewnątrz, brak fixa).

**KROKI:**
1. Uruchom MeshTracker, przejdź do mapy

**EXPECTED:**
- Mapa wyśrodkowana na centrum Polski (52.0°N, 19.0°E)
- Zoom: 6 (widok całego kraju)

**ACTUAL:** _______________  
**WYNIK:** ✅ / ❌

---

### TC-MAP-006 — Kolor markera według roli węzła

**PRECONDITION:** Węzły z różnymi rolami (CLIENT, TRACKER) widoczne na mapie.

**KROKI:**
1. Sprawdź kolory markerów na mapie dla różnych ról
2. Porównaj z kodem w `getMarkerHue()`

> **Uwaga:** W aktualnym kodzie `getMarkerHue()` ma niekompletny `when` — wszystkie węzły używają `HUE_AZURE`. Ten test wykryje regresję jeśli rola stanie się pusta (null).

**EXPECTED:**
- Węzły bez przypisanej roli: kolor niebieski (Azure)
- Log: `"Node X has no user or role, using default color"` dla węzłów bez roli
- Aplikacja nie crashuje nawet gdy rola jest null

**ACTUAL:** _______________  
**WYNIK:** ✅ / ❌ / ⏭️ SKIP (brak różnych ról)

---

### TC-MAP-007 — Wyświetlenie komunikatu gdy brak węzłów z pozycją

**PRECONDITION:** MeshTracker połączony, żaden węzeł nie ma ważnej pozycji.

**KROKI:**
1. Wejdź na ekran mapy
2. Obserwuj środek ekranu

**EXPECTED:**
- Widoczny komunikat tekstowy: "No nodes with position found"
- Jeśli są węzły bez pozycji: dodatkowo "Total nodes: X" i "Check the Nodes tab"
- Jeśli brak węzłów w ogóle: "No nodes found"

**ACTUAL:** _______________  
**WYNIK:** ✅ / ❌

---

## 5. TC-NOD — Lista węzłów

### TC-NOD-001 — Wyświetlenie wszystkich węzłów (z i bez pozycji)

**PRECONDITION:** Wszystkie 3 węzły aktywne, min. jeden wewnątrz budynku (brak GPS).

**KROKI:**
1. Przejdź do ekranu listy węzłów (zakładka "Nodes")
2. Sprawdź zawartość listy

**EXPECTED:**
- Wszystkie 3 węzły widoczne na liście (niezależnie od posiadania pozycji GPS)
- Dla każdego węzła widoczna nazwa (longName lub shortName)
- Węzły online wyświetlane przed offline (sortowanie)

**ACTUAL:** _______________  
**WYNIK:** ✅ / ❌

---

### TC-NOD-002 — Wyświetlenie danych węzła (NodeItem)

**PRECONDITION:** T-Beam online, z baterią (zewnętrzna), z GPS, ze SNR i RSSI.

**KROKI:**
1. Otwórz listę węzłów
2. Znajdź pozycję T-Beam
3. Sprawdź wyświetlane informacje

**EXPECTED:**
Dla węzła T-Beam pozycja zawiera:
- ✅ Nazwa węzła (z Meshtastic)
- ✅ Wskaźnik Online (zielony) — jeśli lastHeard < 5 min
- ✅ Czas "ostatnio słyszany" (np. "2 min ago")
- ✅ Poziom baterii w % (jeśli DeviceMetrics dostępne)
- ✅ SNR w dB (jeśli dostępne)
- ✅ RSSI w dBm (jeśli dostępne)

**ACTUAL:** _______________  
**WYNIK:** ✅ / ❌

---

### TC-NOD-003 — Status Online / Offline węzła

**PRECONDITION:** T1000-E #1 włączony i online.

**KROKI:**
1. Zanotuj status T1000-E #1 na liście (Online)
2. Wyłącz T1000-E #1
3. Odczekaj 5 minut (próg isOnline = 300 sekund)
4. Odśwież listę (lub odczekaj na auto-refresh)
5. Sprawdź status T1000-E #1

**EXPECTED:**
- Krok 2: Węzeł nadal widoczny jako Online (ostatnie słyszenie < 5 min)
- Krok 5: Węzeł zmienia status na Offline
- Węzły offline przesuwają się na dół listy

**ACTUAL:** _______________  
**WYNIK:** ✅ / ❌

---

### TC-NOD-004 — Kliknięcie węzła z listy — nawigacja do mapy

**PRECONDITION:** T-Beam z ważną pozycją GPS widoczny na liście.

**KROKI:**
1. Przejdź do listy węzłów
2. Kliknij T-Beam (węzeł z pozycją GPS)
3. Obserwuj przejście

**EXPECTED:**
- Aplikacja automatycznie przełącza się na ekran mapy
- Mapa centruje się na pozycji T-Beam (zoom 15)
- T-Beam jest wybranym węzłem (selectedNodeId ustawiony)

**ACTUAL:** _______________  
**WYNIK:** ✅ / ❌

---

### TC-NOD-005 — Kliknięcie węzła bez pozycji GPS

**PRECONDITION:** T1000-E #1 wewnątrz budynku (brak GPS fix).

**KROKI:**
1. Kliknij T1000-E #1 na liście węzłów

**EXPECTED:**
- Aplikacja przełącza się na mapę (lub pozostaje w miejscu)
- Kamera mapy **nie** wykonuje gwałtownego skoku
- Brak crashu — aplikacja obsługuje null pozycji

**ACTUAL:** _______________  
**WYNIK:** ✅ / ❌

---

### TC-NOD-006 — Auto-sortowanie listy: Online → Offline → Alfabetycznie

**PRECONDITION:** 2 węzły Online, 1 Offline.

**KROKI:**
1. Otwórz listę węzłów
2. Sprawdź kolejność węzłów

**EXPECTED:**
- Węzły Online na górze listy
- Węzły Offline na dole
- W obrębie tej samej grupy: sortowanie alfabetyczne według nazwy

**ACTUAL:** _______________  
**WYNIK:** ✅ / ❌

---

## 6. TC-NAV — Nawigacja między ekranami

### TC-NAV-001 — Przełączanie między mapą a listą przez Bottom Navigation

**PRECONDITION:** MeshTracker uruchomiony.

**KROKI:**
1. Sprawdź że domyślny ekran to Mapa
2. Kliknij zakładkę "Nodes" w dolnym pasku
3. Kliknij zakładkę "Map" w dolnym pasku

**EXPECTED:**
- Krok 1: Widoczna mapa
- Krok 2: Widoczna lista węzłów, bez restartu danych
- Krok 3: Powrót do mapy, zachowany stan kamery

**ACTUAL:** _______________  
**WYNIK:** ✅ / ❌

---

### TC-NAV-002 — Zachowanie stanu selectedNode podczas nawigacji

**PRECONDITION:** T-Beam wybrany na mapie (marker kliknięty).

**KROKI:**
1. Kliknij marker T-Beam na mapie — info window otwarta
2. Przejdź do listy węzłów
3. Wróć do mapy

**EXPECTED:**
- Info window zamknięta po powrocie (lub otwarta — zależy od implementacji)
- Mapa nie resetuje pozycji kamery
- selectedNodeId zachowany w ViewModel

**ACTUAL:** _______________  
**WYNIK:** ✅ / ❌

---

### TC-NAV-003 — Pasek statusu widoczny na obu ekranach

**PRECONDITION:** MeshTracker połączony.

**KROKI:**
1. Sprawdź ConnectionStatusBar na ekranie mapy
2. Przejdź do listy węzłów
3. Sprawdź ConnectionStatusBar

**EXPECTED:**
- Pasek statusu widoczny na **obu** ekranach (MainScreen renderuje go w TopBar)
- Ten sam stan połączenia na obu ekranach

**ACTUAL:** _______________  
**WYNIK:** ✅ / ❌

---

## 7. TC-GPS — Aktualizacja pozycji GPS

### TC-GPS-001 — Aktualizacja pozycji węzła przez BroadcastReceiver

**PRECONDITION:**
- T-Beam na zewnątrz, GPS fix, stacjonarny
- MeshTracker połączony i marker T-Beam widoczny na mapie

**KROKI:**
1. Zanotuj aktualną pozycję markera T-Beam na mapie
2. Przejdź z T-Beam w nowe miejsce (min. 50 m)
3. Odczekaj max. 2 minuty (czas na GPS update + broadcast)
4. Obserwuj marker na mapie

**EXPECTED:**
- Marker T-Beam przesuwa się na nową pozycję
- Log: `"Node X position changed"` z podaniem różnicy w metrach
- Pozycja aktualizuje się bez restartu aplikacji

**ACTUAL:** _______________  
**WYNIK:** ✅ / ❌

---

### TC-GPS-002 — Brak duplikacji węzłów po aktualizacji pozycji

**PRECONDITION:** T-Beam widoczny na mapie jako 1 marker.

**KROKI:**
1. Przesuń T-Beam, poczekaj na update pozycji
2. Sprawdź liczbę markerów T-Beam na mapie
3. Poczekaj jeszcze 2 minuty, sprawdź ponownie

**EXPECTED:**
- Na mapie zawsze **dokładnie 1 marker** dla T-Beam, niezależnie od liczby aktualizacji
- Marker aktualizuje swoją pozycję (nie tworzy nowych)

**ACTUAL:** _______________  
**WYNIK:** ✅ / ❌

---

### TC-GPS-003 — Periodic refresh (co 5 sekund)

**PRECONDITION:** MeshTracker w stanie CONNECTED.

**KROKI:**
1. Monitoruj logi: `adb logcat -s MapViewModel`
2. Obserwuj przez 30 sekund

**EXPECTED:**
- Log `"Refreshing nodes..."` pojawia się co ~5 sekund
- Każdy refresh pobiera i aktualizuje węzły przez `getNodes()`
- Brak nadmiarowych refreshy (nie częściej niż co 3 sekundy)

**ACTUAL:** _______________  
**WYNIK:** ✅ / ❌

---

### TC-GPS-004 — Zachowanie pozycji przy węźle offline (cache)

**PRECONDITION:** T1000-E #1 miał aktywną pozycję GPS, teraz jest offline.

**KROKI:**
1. Zanotuj ostatnią pozycję T1000-E #1 na mapie
2. Wyłącz T1000-E #1
3. Odczekaj 10 minut
4. Sprawdź czy marker jest nadal na mapie

**EXPECTED:**
- Marker T1000-E #1 **nadal widoczny** na mapie (ostatnia znana pozycja)
- Status węzła na mapie: Offline
- Pozycja zaktualizuje się gdy węzeł wróci online

**ACTUAL:** _______________  
**WYNIK:** ✅ / ❌

---

## 8. TC-DRN — Testy z dronem (ruchomy węzeł)

> **Sprzęt:** SenseCAP T1000-E #2 zamontowany na dronie.  
> **Uwaga bezpieczeństwa:** Wszystkie loty wykonuj zgodnie z lokalnymi przepisami o dronach. Upewnij się że masz pozwolenie na lot w danym obszarze. SenseCAP T1000-E waży ~45g — sprawdź dopuszczalne obciążenie drona.

### TC-DRN-000 — Przygotowanie do testów z dronem

**KROKI:**
1. Zamontuj T1000-E #2 na dronie (bezpieczne mocowanie, nie blokuj anteny)
2. Upewnij się że T1000-E #2 ma fix GPS na ziemi (przed startem)
3. Skonfiguruj GPS update interval: 10s, Position broadcast: 15s
4. Sprawdź że T1000-E #2 jest widoczny na liście węzłów w MeshTracker
5. Sprawdź że T1000-E #2 ma marker na mapie (na ziemi, przy startowni)

**EXPECTED:**
- T1000-E #2 widoczny z poprawną pozycją w MeshTracker przed startem
- Log: `"Node T1000-E-2 new position: lat=X.XXXXX, lng=Y.YYYYY"`

**ACTUAL:** _______________  
**WYNIK:** ✅ / ❌ (warunek konieczny do kontynuowania TC-DRN)

---

### TC-DRN-001 — Śledzenie lotu drona w czasie rzeczywistym (trasa prosta)

**Cel:** Weryfikacja aktualizacji pozycji ruchomego węzła w czasie lotu.

**PRECONDITION:**
- TC-DRN-000 zaliczony
- Obszar testowy: otwarty teren, zasięg LoRa gwarantowany (max. 300m od T-Beam)
- Telefon z MeshTracker w ręku operatora drona

**KROKI:**
1. Uruchom dron, wznieś na 20m wysokości i utrzymaj pozycję hover przez 30s
2. Obserwuj mapę w MeshTracker — zanotuj pozycję markera
3. Przesuń dron poziomo 100m na wschód, ze stałą prędkością ~2m/s
4. Obserwuj ruch markera na mapie przez 60s
5. Przesuń dron 100m na zachód (powrót do punktu startowego)
6. Odczekaj 30s w hover
7. Wyląduj

**EXPECTED:**
- Krok 1: Marker T1000-E #2 na mapie w pozycji startowej (ziemia)
- Krok 4: Marker przesuwa się na wschód w krokach ~10-15s, zgodnie z ruchem drona
- Krok 5: Marker wraca do pozycji startowej
- Log: `"Position changed by: lat=X, lng=Y, distance≈Zm"` z rosnącą odległością
- Brak duplikacji markerów
- Przerwy w aktualizacji: max. 30s (2× broadcast interval)

**ACTUAL:** _______________  
**WYNIK:** ✅ / ❌  **Data:** ________

---

### TC-DRN-002 — Śledzenie trasy złożonej (kwadrat/okrąg)

**Cel:** Weryfikacja ciągłości śledzenia przy złożonej trasie lotu.

**PRECONDITION:** TC-DRN-000 zaliczony, obszar min. 200×200m.

**KROKI:**
1. Poleć dron trasą kwadratową: 100m N → 100m E → 100m S → 100m W (powrót)
2. Prędkość: 3 m/s
3. Obserwuj mapę w MeshTracker podczas całego lotu

**EXPECTED:**
- Marker śledzi kolejne wierzchołki kwadratu (z opóźnieniem 15-30s)
- Po powrocie do punktu startowego: marker wraca do pierwotnej pozycji
- Łącznie na mapie widoczna jest ~1 aktualizacja co 15s (16 aktualizacji na okrążenie ~4 min)
- Żadna aktualizacja nie powoduje crashu aplikacji

**ACTUAL:** _______________  
**WYNIK:** ✅ / ❌

---

### TC-DRN-003 — Zachowanie aplikacji przy utracie sygnału LoRa (poza zasięgiem)

**Cel:** Weryfikacja zachowania gdy węzeł-dron wykroczy poza zasięg sieci.

**PRECONDITION:** TC-DRN-000 zaliczony.

**KROKI:**
1. Poleć dron poza zasięg LoRa (~500m+ w terenie otwartym, zależy od konfiguracji)
2. Odczekaj 3 minuty (brak nowych broadcastów)
3. Obserwuj marker T1000-E #2 na mapie
4. Wróć dronem do zasięgu
5. Odczekaj 60s

**EXPECTED:**
- Krok 3: Marker **nadal widoczny** na ostatniej znanych pozycji (cache)
- Krok 3: Status węzła: Offline (po 5 min od lastHeard)
- Krok 5: Marker aktualizuje się do nowej pozycji po wejściu w zasięg
- Aplikacja nie crashuje przez cały czas testu

**ACTUAL:** _______________  
**WYNIK:** ✅ / ❌

---

### TC-DRN-004 — Porównanie pozycji GPS drona vs. MeshTracker

**Cel:** Weryfikacja dokładności wyświetlanej pozycji.

**PRECONDITION:** Dron z własnym GPS (kontroler z mapą) + T1000-E na dronie.

**KROKI:**
1. Poleć dron na 5 znanych punktów kontrolnych (np. rogi boiska, ławki)
2. Na każdym punkcie: zatrzymaj dron na 30s
3. Zanotuj pozycję z kontrolera drona (lat/lng) i z MeshTracker
4. Porównaj wartości

**EXPECTED:**
- Różnica pozycji GPS drona vs. MeshTracker: max. 20m (typowo <10m)
- Pozycja nie wykazuje dużych skoków między aktualizacjami (< 50m)
- `precisionBits` w logu wskazuje poprawną precyzję GPS

**ACTUAL:** _______________  
**WYNIK:** ✅ / ❌

---

### TC-DRN-005 — Symultaniczne śledzenie wszystkich węzłów podczas lotu drona

**Cel:** Weryfikacja że aktualizacje drona nie zaburzają wyświetlania innych węzłów.

**PRECONDITION:** Wszystkie 3 węzły aktywne (T-Beam stacjonarny, T1000-E #1 stacjonarny, T1000-E #2 na dronie).

**KROKI:**
1. Potwierdź że wszystkie 3 markery widoczne na mapie
2. Uruchom dron z T1000-E #2, poleć 100m
3. Obserwuj mapę — sprawdź czy markery T-Beam i T1000-E #1 pozostają stabilne
4. Sprawdź listę węzłów — czy wszystkie 3 są widoczne

**EXPECTED:**
- Marker T-Beam: bez zmian (stacjonarny)
- Marker T1000-E #1: bez zmian (stacjonarny)
- Marker T1000-E #2: przesuwa się zgodnie z lotem
- Lista węzłów: 3 węzły, poprawne statusy
- Brak "migania" lub znikania stabilnych markerów

**ACTUAL:** _______________  
**WYNIK:** ✅ / ❌

---

### TC-DRN-006 — Aktualizacja pozycji po lądowaniu drona

**Cel:** Weryfikacja że pozycja lądowania jest prawidłowo wyświetlona.

**PRECONDITION:** T1000-E #2 w powietrzu, widoczny na mapie.

**KROKI:**
1. Wyląduj dron w miejscu innym niż punkt startowy (min. 30m od startu)
2. Odczekaj 60s po lądowaniu
3. Sprawdź pozycję markera T1000-E #2

**EXPECTED:**
- Marker T1000-E #2 pokazuje miejsce lądowania (nie punkt startowy)
- Pozycja stabilna po lądowaniu (brak losowych skoków)
- Log: `"satellites="` wskazuje min. 4 satelity

**ACTUAL:** _______________  
**WYNIK:** ✅ / ❌

---

## 9. TC-EDG — Przypadki brzegowe i obsługa błędów

### TC-EDG-001 — Węzeł z pozycją (0, 0) — filtrowanie

**Cel:** Weryfikacja że węzeł z niedostępnym GPS (0,0) nie jest wyświetlany na mapie.

**PRECONDITION:** T1000-E #1 świeżo włączony, wewnątrz budynku (brak GPS fix).

**KROKI:**
1. Sprawdź logi dla T1000-E #1: `adb logcat -s MeshNodeInfo`
2. Sprawdź mapę

**EXPECTED:**
- Log: `"Position valid: false"` lub `"inRange: false"`
- Mapa: brak markera dla T1000-E #1
- Log `MapScreen`: `"hasPosition: false"` dla tego węzła

**ACTUAL:** _______________  
**WYNIK:** ✅ / ❌

---

### TC-EDG-002 — Obsługa braku uprawnienia do lokalizacji

**PRECONDITION:** MeshTracker zainstalowany świeżo, uprawnienia lokalizacji **nie** przyznane.

**KROKI:**
1. Uruchom MeshTracker bez przyznania uprawnień ACCESS_FINE_LOCATION
2. Przejdź do ekranu mapy

**EXPECTED:**
- Aplikacja nie crashuje
- Mapa wyświetla się (bez "niebieskiej kropki" własnej lokalizacji)
- Znacznik `hasLocationPermission` jest false (widoczny w logach/MapScreen)
- Google Maps może wyświetlić informację o brakującym uprawnieniu

**ACTUAL:** _______________  
**WYNIK:** ✅ / ❌

---

### TC-EDG-003 — Brak Google Maps API Key

**PRECONDITION:** API Key **nie** jest skonfigurowany w AndroidManifest.xml.

**KROKI:**
1. Zainstaluj APK bez klucza API
2. Uruchom aplikację, przejdź do mapy

**EXPECTED:**
- Mapa nie ładuje się / pokazuje błąd Google Maps
- Aplikacja nie crashuje całkowicie
- Lista węzłów nadal działa (połączenie z Meshtastic niezależne od map)

**ACTUAL:** _______________  
**WYNIK:** ✅ / ❌

---

### TC-EDG-004 — Obsługa nieprawidłowego NodeInfo przez reflection

**Cel:** Weryfikacja odporności parsera reflection gdy Meshtastic zwraca nieoczekiwany obiekt.

**PRECONDITION:** Dostępna starszą wersja aplikacji Meshtastic (inna wersja API).

**KROKI:**
1. Zainstaluj starszą wersję Meshtastic (jeśli dostępna)
2. Uruchom MeshTracker i połącz z siecią
3. Obserwuj logi dla `MeshNodeInfo`

**EXPECTED:**
- Logi: `"Could not get node num"` lub `"Error getting user"` — bez crashu
- `fromMeshtasticNodeInfo()` zwraca null zamiast rzucać wyjątek
- Na UI: węzły z błędem parsowania pomijane, pozostałe wyświetlane poprawnie

**ACTUAL:** _______________  
**WYNIK:** ✅ / ❌ / ⏭️ SKIP

---

### TC-EDG-005 — Jednoczesne połączenie wielu telefonów z tą samą siecią mesh

**Cel:** Weryfikacja że każdy telefon widzi spójny stan sieci.

**PRECONDITION:** 2 telefony z MeshTracker, oba połączone ze swoimi węzłami Meshtastic, ta sama sieć mesh.

**KROKI:**
1. Uruchom MeshTracker na telefonie A (połączony z T-Beam)
2. Uruchom MeshTracker na telefonie B (jeśli dostępny drugi telefon + węzeł)
3. Porównaj listy węzłów na obu telefonach

**EXPECTED:**
- Oba telefony widzą te same węzły (z możliwym opóźnieniem mesh)
- Liczba węzłów identyczna lub różniąca się max. o 1 (propagacja mesh)

**ACTUAL:** _______________  
**WYNIK:** ✅ / ❌ / ⏭️ SKIP

---

### TC-EDG-006 — Przejście aplikacji do tła i powrót

**PRECONDITION:** MeshTracker połączony, węzły widoczne.

**KROKI:**
1. Naciśnij Home (aplikacja idzie w tło)
2. Odczekaj 5 minut
3. Wróć do MeshTracker przez Recent Apps

**EXPECTED:**
- Połączenie nadal aktywne (ViewModel przeżywa w tle)
- Węzły widoczne bez potrzeby reconnectu
- Periodic refresh kontynuował działanie (widoczne w logach)

**ACTUAL:** _______________  
**WYNIK:** ✅ / ❌

---

### TC-EDG-007 — Rotacja ekranu (landscape/portrait)

**PRECONDITION:** MeshTracker połączony, markery na mapie.

**KROKI:**
1. Obróć telefon do landscape
2. Obserwuj mapę i listę węzłów
3. Obróć z powrotem do portrait

**EXPECTED:**
- Aplikacja nie crashuje podczas rotacji
- Połączenie z Meshtastic zachowane
- Dane węzłów nie tracone (ViewModel przeżywa rotację)
- Mapa wyświetla się poprawnie w obu orientacjach

**ACTUAL:** _______________  
**WYNIK:** ✅ / ❌

---

### TC-EDG-008 — Duża liczba węzłów (stress test)

**Cel:** Weryfikacja wydajności przy dużej sieci mesh.

**PRECONDITION:** Dostęp do środowiska z 10+ węzłami Meshtastic (np. event, duże wdrożenie).

**KROKI:**
1. Połącz MeshTracker z siecią 10+ węzłów
2. Sprawdź płynność mapy (scrollowanie, zoom)
3. Sprawdź płynność listy węzłów (scrollowanie)
4. Sprawdź czas odświeżania (5s periodic refresh z dużą listą)

**EXPECTED:**
- Mapa płynna (60fps lub brak odczuwalnych lagów)
- Lista węzłów scrolluje się płynnie
- Czas periodic refresh: <1s na 10 węzłów
- Brak ANR (Application Not Responding)

**ACTUAL:** _______________  
**WYNIK:** ✅ / ❌ / ⏭️ SKIP

---

## 10. TC-MON — Testy skuteczności monitoringu stref

> **Cel kategorii:** Zmierzyć w sposób ilościowy, jak liczba i rozmieszczenie węzłów retransmitujących wpływa na skuteczność ciągłego śledzenia pozycji — tak aby ocenić, czy system nadaje się do monitoringu zwierząt w wyznaczonych strefach terenowych.

### Kluczowe metryki — definicje

| Metryka | Symbol | Definicja | Jednostka |
|---|---|---|---|
| Packet Delivery Ratio | **PDR** | Liczba odebranych broadcastów GPS / liczba wysłanych × 100% | % |
| Opóźnienie aktualizacji | **Δt** | Czas od nadania broadcastu przez tracker do pojawienia się na mapie | sekundy |
| Ciągłość śledzenia | **CT** | % czasu, w którym pozycja trackera jest znana i aktualna (Δt < 2× broadcast interval) | % |
| Czas wykrycia przekroczenia strefy | **Tz** | Czas od fizycznego przekroczenia granicy strefy do rejestracji zdarzenia w aplikacji | sekundy |
| Pokrycie obszaru na węzeł | **A₁** | Efektywny obszar pewnego odbioru LoRa dla jednego węzła retransmitującego | km² |

### Konfiguracje sieci do porównania

Każdy test TC-MON wykonywany jest dla co najmniej dwóch konfiguracji topologicznych, a wyniki porównywane:

| Konfiguracja | Opis | Węzły |
|---|---|---|
| **K1 — Minimalny** | 1 gateway (T-Beam przy telefonie), brak przekaźników | T-Beam × 1 |
| **K2 — Jeden przekaźnik** | T-Beam jako gateway + T1000-E #1 jako stały przekaźnik | T-Beam + T1000-E #1 |
| **K3 — Dwa przekaźniki** | T-Beam + 2 węzły stacjonarne jako przekaźniki, tracker (dron) jako ruchomy | T-Beam + T1000-E #1 + T1000-E #2 |

> **Uwaga:** Dla K2 i K3 T1000-E pełniący rolę przekaźnika powinien mieć ustawioną rolę `ROUTER` lub `ROUTER_CLIENT` i być ustawiony na stałe w terenie (np. na trójnogu lub maszcie).

---

### TC-MON-001 — Pomiar PDR w funkcji odległości (1 węzeł, konfiguracja K1)

**Cel:** Wyznaczenie zasięgu niezawodnej komunikacji LoRa dla jednego T-Beam w terenie otwartym — punkt odniesienia dla pozostałych konfiguracji.

**PRECONDITION:**
- Teren otwarty (pole, łąka), brak przeszkód terenowych
- T-Beam ustawiony w centrum strefy testowej, podłączony do telefonu z MeshTracker
- T1000-E #2 jako tracker (na dronie lub niesiony ręcznie)
- Broadcast interval trackera: 30s
- Czas trwania pomiaru w każdym punkcie: 10 minut (= 20 oczekiwanych broadcastów)

**Arkusz pomiarowy — wypełnić dla każdego punktu pomiarowego:**

| Punkt | Odległość od T-Beam [m] | Kierunek | Odebrane pkty | Oczekiwane pkty | PDR [%] | Avg Δt [s] | SNR [dB] | RSSI [dBm] |
|---|---|---|---|---|---|---|---|---|
| P1 | 50 | N | | 20 | | | | |
| P2 | 100 | N | | 20 | | | | |
| P3 | 200 | N | | 20 | | | | |
| P4 | 300 | N | | 20 | | | | |
| P5 | 500 | N | | 20 | | | | |
| P6 | 750 | N | | 20 | | | | |
| P7 | 1000 | N | | 20 | | | | |
| P8 | 200 | E | | 20 | | | | |
| P9 | 200 | S | | 20 | | | | |
| P10 | 200 | W | | 20 | | | | |

**KROKI:**
1. Ustaw T-Beam w centrum strefy, uruchom MeshTracker
2. Idź z trackerem do punktu P1 (50m N) i stój przez 10 minut, licząc aktualizacje na mapie
3. Powtórz dla każdego punktu pomiarowego
4. Wartości SNR i RSSI odczytaj z listy węzłów w MeshTracker
5. Oblicz PDR = (odebrane / oczekiwane) × 100%
6. Oblicz Avg Δt = średni czas między kolejnymi aktualizacjami na mapie

**EXPECTED (kryteria zaliczenia):**
- PDR ≥ 95% dla odległości ≤ 200m → sieć **nadaje się** do monitoringu małych stref (< 100m od węzła)
- PDR ≥ 80% dla odległości ≤ 500m → sieć **nadaje się** do monitoringu dużych stref (< 250m od węzła)
- PDR < 60% → dana odległość **poza zasięgiem pewnej komunikacji**

**ACTUAL / WYNIKI:**  
Zasięg 100% PDR: ______ m  
Zasięg 80% PDR: ______ m  
Zasięg 50% PDR: ______ m  

**WYNIK:** ✅ PASS (>95% @ 200m) / ❌ FAIL / ⏭️ SKIP  **Data:** ________

---

### TC-MON-002 — Wpływ liczby przeskoków mesh na opóźnienie aktualizacji (Δt)

**Cel:** Zmierzyć jak każdy dodatkowy hop retransmisji zwiększa opóźnienie dotarcia pozycji do aplikacji.

**PRECONDITION:**
- Konfiguracja K2: T-Beam (gateway) + T1000-E #1 (przekaźnik) + T1000-E #2 (tracker)
- T-Beam w punkcie A, T1000-E #1 (przekaźnik) w punkcie B (200m od A), tracker w różnych punktach
- MeshTracker uruchomiony, logi dostępne przez `adb logcat -s MapViewModel MeshtasticReceiver`
- Broadcast interval trackera: 15s
- Czas pomiaru w każdej pozycji: 15 minut (60 broadcastów)

**Schemat rozmieszczenia:**
```
[Gateway T-Beam] ←200m→ [Przekaźnik T1000-E #1] ←200m→ [Tracker T1000-E #2]
     Punkt A                    Punkt B                       Punkt C (2 hopy)

[Gateway T-Beam] ←100m→ [Tracker T1000-E #2]
     Punkt A                  Punkt D (1 hop / bezpośrednio)
```

**Arkusz pomiarowy:**

| Pozycja trackera | Hopy do gateway | Odebrane / Oczekiwane | PDR [%] | Min Δt [s] | Max Δt [s] | Avg Δt [s] | Odchylenie std [s] |
|---|---|---|---|---|---|---|---|
| Punkt D (100m od A, brak przekaźnika) | 1 | / 60 | | | | | |
| Punkt C (przez przekaźnik B) | 2 | / 60 | | | | | |
| Punkt E (400m od A, przez B) | 2 | / 60 | | | | | |

**KROKI:**
1. Ustaw węzły w punktach A i B
2. Umieść tracker w D (bezpośredni zasięg A, 1 hop) — mierz 15 minut
3. Umieść tracker w C (przez przekaźnik B, 2 hopy) — mierz 15 minut
4. Dla każdego broadcastu zmierz Δt: zaznacz czas nadania (log Meshtastic) i czas pojawienia się na mapie (log MapViewModel)
5. Oblicz statystyki

**EXPECTED:**
- 1 hop: Avg Δt < 5s, Odchylenie std < 3s
- 2 hopy: Avg Δt < 15s, Odchylenie std < 8s
- PDR: bez istotnej różnicy między 1 a 2 hopami (< 5 p.p. różnicy) gdy oba linki >80% PDR

**ACTUAL:**  
Δt dla 1 hopu: avg = ____s, max = ____s  
Δt dla 2 hopów: avg = ____s, max = ____s  
Wzrost Δt na hop: ____s  

**WYNIK:** ✅ / ❌  **Data:** ________

---

### TC-MON-003 — Ciągłość śledzenia CT podczas ruchu ciągłego (symulacja zwierzęcia)

**Cel:** Zmierzyć procent czasu, w którym system zna aktualną pozycję ruchomego trackera — kluczowy wskaźnik dla monitoringu zwierząt.

**PRECONDITION:**
- Konfiguracja K1 i K2 testowane osobno (porównanie)
- Trasa testowa: okrąg o promieniu 150m wokół punktu centralnego (obwód ~940m)
- Tracker niesiony przez osobę chodzącą ze stałą prędkością ~4 km/h (typowa prędkość zwierzęcia pasącego się)
- Broadcast interval: 30s
- CT uznajemy za przerwane gdy przerwa między aktualizacjami > 90s (3× broadcast interval)
- Czas trwania testu: 3 pełne okrążenia (~42 min)

**Definicja CT dla tego testu:**
```
CT = (czas z aktualną pozycją / całkowity czas testu) × 100%
"Aktualna pozycja" = ostatnia aktualizacja nastąpiła < 90 sekund temu
```

**Arkusz pomiarowy — dla każdej konfiguracji:**

| Konfiguracja | Czas testu [min] | Liczba przerw CT (>90s) | Łączny czas przerw [min] | CT [%] | Avg Δt [s] | PDR [%] |
|---|---|---|---|---|---|---|
| K1 (sam gateway) | 42 | | | | | |
| K2 (gateway + 1 przekaźnik) | 42 | | | | | |

**KROKI:**
1. Przygotuj stoper i arkusz do notowania przerw w aktualizacjach
2. Uruchom test K1: wyjdź z trackerem na trasę okrężną i chodź przez 3 okrążenia
3. Za każdym razem gdy mapa nie zaktualizowała się przez > 90s, zanotuj czas początku i końca przerwy
4. Po teście oblicz CT
5. Powtórz identyczną trasę z konfiguracją K2

**EXPECTED:**
- K1: CT ≥ 85% → akceptowalne dla monitoringu o niskich wymaganiach
- K2: CT ≥ 95% → akceptowalne dla monitoringu ciągłego
- Różnica K2 vs K1: wyraźna poprawa CT przy dodaniu przekaźnika, szczególnie po stronie odległej od gateway

**ACTUAL:**  
CT dla K1: ______%  
CT dla K2: ______%  
Poprawa CT po dodaniu przekaźnika: ______ p.p.  

**WYNIK:** ✅ (CT K2 ≥ 95%) / ❌ / ⏭️ SKIP  **Data:** ________

---

### TC-MON-004 — Czas wykrycia przekroczenia granicy strefy (Tz)

**Cel:** Zmierzyć opóźnienie między fizycznym wyjściem zwierzęcia ze strefy a wykryciem tego faktu w aplikacji.

> **Kontekst:** MeshTracker nie ma obecnie wbudowanego geofencingu — "wykrycie" oznacza moment, w którym marker trackera na mapie pojawi się za granicą strefy i operator może to zaobserwować. Test mierzy to opóźnienie i określa minimalny broadcast interval potrzebny do wykrycia w wymaganym czasie.

**PRECONDITION:**
- Konfiguracja K2
- Strefa testowa: kwadrat 200×200m, granica wyraźnie oznaczona w terenie (taśma, pachołki)
- T-Beam (gateway) i przekaźnik wewnątrz strefy
- Broadcast interval trackera: testowany w wersjach 15s, 30s, 60s
- Kamera mapy ustawiona tak, aby cała strefa była widoczna na ekranie

**Procedura dla każdego broadcast interval:**
1. Tracker znajduje się 20m wewnątrz granicy strefy, stoi nieruchomo przez 2 minuty
2. Na sygnał — tracker **szybko** (w 10s) przechodzi granicę i zatrzymuje się 20m na zewnątrz
3. Mierz czas od fizycznego przekroczenia granicy do momentu, gdy marker na mapie pojawi się po zewnętrznej stronie
4. Powtórz 5 razy dla każdego ustawienia i uśrednij

**Arkusz pomiarowy:**

| Broadcast interval | Próba 1 [s] | Próba 2 [s] | Próba 3 [s] | Próba 4 [s] | Próba 5 [s] | Avg Tz [s] | Max Tz [s] |
|---|---|---|---|---|---|---|---|
| 15s | | | | | | | |
| 30s | | | | | | | |
| 60s | | | | | | | |

**EXPECTED:**
- Avg Tz ≤ 2× broadcast interval (typowe opóźnienie = czas do następnego broadcastu + Δt sieci)
- Broadcast interval 30s: Avg Tz ≤ 60s → akceptowalne dla pasących się zwierząt
- Broadcast interval 15s: Avg Tz ≤ 30s → akceptowalne dla monitoringu aktywnych zwierząt
- Max Tz: nigdy nie przekracza 3× broadcast interval

**ACTUAL:**  
Zalecany broadcast interval dla tej strefy: ______s  
Najkrótsze wykryte przekroczenie: ______s  
Najdłuższe wykryte przekroczenie: ______s  

**WYNIK:** ✅ / ❌  **Data:** ________

---

### TC-MON-005 — Skuteczność pokrycia strefy w funkcji liczby węzłów (test z dronem)

**Cel:** Wyznaczyć mapę "dziur" pokrycia w strefie testowej dla K1 vs K2 vs K3. Dron pozwala systematycznie przeczesać obszar po siatce i zidentyfikować miejsca z PDR < 80%.

**PRECONDITION:**
- Teren płaski lub lekko falisty, min. 400×400m
- Dron z T1000-E #2, broadcast interval: 15s
- Siatka pomiarowa: punkty co 50m (siatka 9×9 = 81 punktów dla 400×400m)
- W każdym punkcie siatki: hover 2 minuty (8 oczekiwanych broadcastów)
- Konfiguracje: K1, K2, K3 — każda osobnego dnia lub po przesunięciu węzłów

**Arkusz pomiarowy — siatka 5×5 (uproszczona wersja dla 200×200m):**

Kolumny: W+100, W+50, Centrum, E+50, E+100  
Wiersze: N+100, N+50, Centrum, S+50, S+100

Dla każdego punktu zapisz PDR [%] (K1 / K2 / K3):

|  | W+100 | W+50 | Centrum | E+50 | E+100 |
|---|---|---|---|---|---|
| **N+100** | / / | / / | / / | / / | / / |
| **N+50** | / / | / / | / / | / / | / / |
| **Centrum** | / / | / / | / / | / / | / / |
| **S+50** | / / | / / | / / | / / | / / |
| **S+100** | / / | / / | / / | / / | / / |

**KROKI:**
1. Ustaw węzły zgodnie z konfiguracją K1, uruchom MeshTracker
2. Poleć dronem po siatce — w każdym punkcie hover 2 min, zliczyj aktualizacje na mapie
3. Zanotuj PDR dla każdego punktu siatki w kolumnie K1
4. Przesuń węzły do K2, powtórz siatkę
5. Przesuń do K3, powtórz siatkę
6. Narysuj mapę pokrycia (zamaluj na czerwono PDR < 80%, na żółto 80-95%, na zielono ≥ 95%)

**EXPECTED:**
- K1: "dziury" (PDR < 80%) na peryferiach strefy przy odległości > zasięg z TC-MON-001
- K2: wyraźna redukcja obszaru z PDR < 80% po dodaniu przekaźnika
- K3: PDR ≥ 80% dla ≥ 90% punktów siatki → sieć **nadaje się** do pokrycia strefy

**ACTUAL:**  
Powierzchnia PDR ≥ 95% dla K1: ______%  
Powierzchnia PDR ≥ 95% dla K2: ______%  
Powierzchnia PDR ≥ 95% dla K3: ______%  
Optymalna pozycja przekaźnika (wnioski): _________________________________

**WYNIK:** ✅ (K3 ≥ 90% siatki z PDR≥80%) / ❌  **Data:** ________

---

### TC-MON-006 — Śledzenie wielu trackerów jednocześnie (skalowalność)

**Cel:** Sprawdzić czy sieć obsługuje śledzenie wielu zwierząt jednocześnie bez wzajemnych zakłóceń i kolizji pakietów.

**PRECONDITION:**
- Konfiguracja K2
- Oba T1000-E działają jako trackery (ruchome), T-Beam jako gateway
- Broadcast interval obu trackerów: 30s (ale z losowym offsetem — Meshtastic robi to automatycznie)
- Obaj "trackerzy" poruszają się niezależnie po strefie testowej przez 30 minut

**Arkusz pomiarowy:**

| Tracker | Oczekiwane pkty (30min / 30s) | Odebrane | PDR [%] | Avg Δt [s] | Kolizje (podwójny broadcast w < 5s) |
|---|---|---|---|---|---|
| T1000-E #1 | 60 | | | | |
| T1000-E #2 | 60 | | | | |
| **Razem sieć** | 120 | | | | |

**KROKI:**
1. Uruchom oba trackery, sprawdź że oba widoczne na mapie
2. Dwie osoby wyruszają w różnych kierunkach z trackerami, poruszając się swobodnie po strefie
3. Po 30 minutach wracają, zliczane są aktualizacje każdego trackera na mapie
4. Sprawdź w logach czy nastąpiły kolizje broadcastów (dwa odebrane w < 5s)

**EXPECTED:**
- PDR każdego trackera ≥ 85% (kanał bardziej obciążony niż przy jednym trackerze)
- Brak "zagłuszania" jednego trackera przez drugi (PDR obu trackerów porównywalne, różnica < 15 p.p.)
- Oba markery widoczne jednocześnie na mapie przez > 90% czasu testu
- Aplikacja nie myli pozycji trackerów (marker A nie "skacze" na pozycję trackera B)

**ACTUAL:**  
PDR T1000-E #1: ______%  
PDR T1000-E #2: ______%  
Różnica PDR: ______ p.p.  

**WYNIK:** ✅ / ❌  **Data:** ________

---

### TC-MON-007 — Odporność monitoringu na awarię węzła pośredniego

**Cel:** Sprawdzić jak wyłączenie węzła przekaźnikowego w trakcie monitoringu wpływa na ciągłość śledzenia.

**PRECONDITION:**
- Konfiguracja K2 (gateway + 1 przekaźnik + tracker)
- Tracker w strefie, w miejscu które wymaga przekaźnika (2 hopy do gateway)
- CT mierzone jak w TC-MON-003

**KROKI:**
1. Uruchom monitoring w K2, potwierdź CT ≥ 95% przez pierwsze 5 minut
2. Wyłącz węzeł przekaźnikowy (T1000-E #1)
3. Mierz CT przez kolejne 10 minut
4. Włącz węzeł przekaźnikowy z powrotem
5. Mierz CT przez kolejne 5 minut (recovery)

**Arkusz pomiarowy:**

| Faza | Czas [min] | Konfiguracja faktyczna | Przerwy CT (>90s) | CT [%] |
|---|---|---|---|---|
| Przed awarią | 5 | K2 (pełna) | | |
| Po awarii przekaźnika | 10 | K1 (efektywna) | | |
| Po przywróceniu | 5 | K2 (pełna) | | |

**EXPECTED:**
- Faza 2 (awaria): CT spada — oczekiwane; pytanie o ile i jak szybko wykrywamy awarię
- Czas do wykrycia utraty śledzenia po awarii: < 3 minuty
- Faza 3 (recovery): CT wraca do wartości z fazy 1 w ciągu max. 2 minut od przywrócenia węzła
- Aplikacja **nie crashuje** przez cały czas testu

**ACTUAL:**  
Spadek CT podczas awarii: ______%  
Czas do recovery: ______s  

**WYNIK:** ✅ / ❌  **Data:** ________

---

### TC-MON-008 — Ocena przydatności systemu — arkusz podsumowujący

> Ten test nie jest wykonywany w terenie — to synteza wyników TC-MON-001 do TC-MON-007 w formie oceny przydatności systemu do monitoringu zwierząt w strefach.

**Macierz oceny przydatności:**

| Kryterium | Wymaganie minimalne | Wynik z testów | Spełnione? |
|---|---|---|---|
| Zasięg pewny (PDR ≥ 95%) | ≥ 150m od węzła | ______m | ✅ / ❌ |
| Opóźnienie aktualizacji (Avg Δt) | ≤ 60s (broadcast 30s) | ______s | ✅ / ❌ |
| Ciągłość śledzenia CT | ≥ 90% przy K2 | ______% | ✅ / ❌ |
| Czas wykrycia przekroczenia strefy | ≤ 2× broadcast interval | ______s | ✅ / ❌ |
| Pokrycie strefy 200×200m | ≥ 85% siatki z PDR ≥ 80% | ______% | ✅ / ❌ |
| Śledzenie 2 zwierząt jednocześnie | PDR każdego ≥ 80% | ✅ / ❌ | ✅ / ❌ |
| Odporność na awarię przekaźnika | Recovery < 2 min | ______min | ✅ / ❌ |

**Wnioski końcowe (wypełnić po testach):**

Minimalna zalecana konfiguracja dla strefy ______ ha:  
- Liczba węzłów gateway: ______  
- Liczba węzłów przekaźnikowych: ______  
- Broadcast interval trackerów: ______s  
- Maksymalna liczba jednoczesnych trackerów: ______  

**Ocena ogólna:**  
☐ System nadaje się do monitoringu ciągłego (CT ≥ 95%, wszystkie kryteria spełnione)  
☐ System nadaje się do monitoringu z ograniczeniami (CT 85-95%, wskazać ograniczenia)  
☐ System wymaga rozbudowy sieci przed wdrożeniem (CT < 85%)  

**Ograniczenia zidentyfikowane podczas testów:**  
_________________________________  

---

## 11. Matryca pokrycia testami

| Obszar | TC ID | Liczba testów | Priorytet |
|---|---|---|---|
| Połączenie z serwisem | TC-CON-001 do 005 | 5 | 🔴 Krytyczny |
| Pasek statusu | TC-STS-001 do 004 | 4 | 🟠 Wysoki |
| Mapa i markery | TC-MAP-001 do 007 | 7 | 🔴 Krytyczny |
| Lista węzłów | TC-NOD-001 do 006 | 6 | 🟠 Wysoki |
| Nawigacja | TC-NAV-001 do 003 | 3 | 🟡 Średni |
| Aktualizacja GPS | TC-GPS-001 do 004 | 4 | 🔴 Krytyczny |
| Testy z dronem | TC-DRN-000 do 006 | 7 | 🟠 Wysoki |
| Przypadki brzegowe | TC-EDG-001 do 008 | 8 | 🟡 Średni |
| Skuteczność monitoringu stref | TC-MON-001 do 008 | 8 | 🔴 Krytyczny |
| **RAZEM** | | **52** | |

---

## Znane ograniczenia i otwarte pytania

### Zidentyfikowane problemy w kodzie (do weryfikacji podczas testów)

1. **`getMarkerHue()` — niekompletny `when`:** Funkcja ma pusty blok `when(role)` — wszystkie węzły dostają `HUE_AZURE`. TC-MAP-006 to potwierdzi.

2. **Reflection zamiast bezpośredniego API:** Cała komunikacja z Meshtastic przez Java reflection — podatne na zmiany wersji Meshtastic. TC-EDG-004 testuje odporność.

3. **Brak obsługi uprawnień runtime:** Aplikacja sprawdza uprawnienie lokalizacji, ale nie ma kodu do jego żądania. TC-EDG-002 to weryfikuje.

4. **Parcelable przez ClassLoader Meshtastic:** `MeshtasticBroadcastReceiver` ładuje ClassLoader z Meshtastic przez `createPackageContext` — może być problematyczne na Android 13+.

### Rekomendowane kolejność wykonania testów

**Sprint 1 (pre-lot drona):**
TC-CON-001 → TC-STS-001 → TC-MAP-001 → TC-NOD-001 → TC-GPS-001 → TC-GPS-002 → TC-GPS-003

**Sprint 2 (testy z dronem):**
TC-DRN-000 → TC-DRN-001 → TC-DRN-005 → TC-DRN-002 → TC-DRN-003

**Sprint 3 (edge cases):**
TC-EDG-001 → TC-EDG-006 → TC-EDG-007 → pozostałe TC-EDG

**Sprint 4 (testy monitoringu stref — wymagają stabilnej aplikacji z S1):**
TC-MON-001 (wyznacz zasięg PDR) → TC-MON-002 (opóźnienie vs hopy) → TC-MON-003 (ciągłość CT) → TC-MON-004 (Tz przekroczenia strefy) → TC-MON-005 (mapa pokrycia z dronem) → TC-MON-006 (dwa trackery) → TC-MON-007 (awaria węzła) → TC-MON-008 (synteza)

---

*Dokument wygenerowany dla projektu MeshTracker v1 | Sprzęt: T-Beam + 2× SenseCAP T1000-E*
