# MeshTracker — dokumentacja inżynierska projektu

**Przeznaczenie dokumentu**

Materiał stanowi spójną, opartą na kodzie podstawę do napisania pracy inżynierskiej dotyczącej systemu MeshTracker. Treść porządkuje informacje rozproszone w katalogu `docs/` w pojedynczy logiczny ciąg, zgodny z wytycznymi zawartymi w opracowaniu A. Kraśniewskiego *Jak pisać pracę dyplomową?* (WETI PW 2020). Każda sekcja odpowiada przewidywanemu rozdziałowi pracy i może zostać zaadaptowana do tekstu po niewielkich korektach redakcyjnych. Tam, gdzie to istotne, akapity opisują nie tylko wynik końcowy, ale również proces dochodzenia do rozwiązania — zgodnie z zasadą T. Stareckiego, przywołaną w wytycznych: praca dyplomowa ma stanowić prezentację logicznego ciągu zdarzeń, przemyśleń i wyborów.

Dokument jest świadomie zwięzły w warstwach powszechnie znanych (np. opis Kotlina czy ogólny opis architektury MVVM), a rozbudowany tam, gdzie pojawia się oryginalny wkład inżynierski (integracja z aplikacją Meshtastic poprzez reflection, geofencing offline, mechanizm rozwiązywania problemu ClassLoadera przy deserializacji `Parcelable`).

---

## 1. Wprowadzenie

### 1.1 Tło i obszar tematyczny

Praca dotyczy bezprzewodowych sieci radiowych pracujących w pasmach ISM oraz ich praktycznego zastosowania do śledzenia obiektów ruchomych w obszarach pozbawionych infrastruktury telekomunikacyjnej. Centralnym pojęciem jest sieć mesh oparta o modulację LoRa, w której każde urządzenie pełni jednocześnie rolę nadajnika, odbiornika i przekaźnika. Konkretną realizacją takiej sieci wykorzystaną w projekcie jest otwartoźródłowa platforma Meshtastic.

Drugim obszarem tematycznym jest tworzenie aplikacji mobilnych na platformę Android, w szczególności integracja aplikacji własnej z zewnętrzną aplikacją systemową poprzez mechanizmy międzyprocesowe Androida (bound services, AIDL, broadcasty). Trzeci obszar to geofencing offline, czyli wykrywanie wejścia i wyjścia obiektu z wirtualnej strefy bez korzystania z usług lokalizacyjnych operatora ani interfejsów chmurowych.

### 1.2 Motywacja

Autor projektu corocznie spędza znaczną część okresu letniego na rodzinnej działce na Mazurach Zachodnich, zabierając ze sobą psa myśliwskiego rasy Mały Gończy Gaskoński. Wrodzony instynkt łowczy psa, połączony z otwartą przestrzenią graniczącą z terenami leśnymi bogatymi w dziką zwierzynę, prowadzi do regularnych, niekontrolowanych pościgów za zwierzętami. Klasyczne metody przeciwdziałania — ogrodzenie posesji albo trening odwołania — z różnych przyczyn nie są wystarczające: ogrodzenie nie wchodzi w grę z powodów krajobrazowych i prawnych, a trening odwołania zawodzi w momencie zrywu psa za zwierzyną.

Rynek oferuje gotowe rozwiązania (Dogtra Pathfinder 2, Dogtrace DOG GPS X25), są one jednak kosztowne, zamknięte i niemożliwe do dostosowania ani rozszerzenia. Powstała różnica między dostępną technologią a problemem osobistym stworzyła motywację do zaprojektowania własnego, prostszego i otwartego systemu, opartego na ogólnodostępnych modułach LoRa i bezpłatnym oprogramowaniu Meshtastic.

### 1.3 Cel pracy

**Celem pracy inżynierskiej jest zaprojektowanie i implementacja systemu monitorowania położenia zwierząt domowych w czasie rzeczywistym w obszarach pozbawionych pokrycia sieci komórkowej, wraz z mechanizmem alarmowania o przekroczeniu wyznaczonej strefy bezpieczeństwa.** System składa się z dwóch komponentów:

1. **Sieć radiowa Meshtastic** zbudowana co najmniej z dwóch węzłów: węzła „dom" (stacja bazowa) i węzła „pies" (umieszczony na obroży), wymieniających dane o pozycji GPS.
2. **Aplikacja mobilna MeshTracker** na system Android, która łączy się przez Bluetooth ze stacją bazową, wizualizuje pozycje wszystkich węzłów na mapie, umożliwia definiowanie stref geofencingu w postaci wielokątów oraz powiadamia użytkownika o ich naruszeniu — również w przypadku, gdy aplikacja działa w tle.

Praca obejmuje dobór i konfigurację sprzętu, integrację z systemem Meshtastic, projekt i implementację aplikacji, testy funkcjonalne oraz weryfikację w warunkach polowych.

### 1.4 Ograniczenia zakresu

Wprost poza zakresem pracy znajdują się:

- konstrukcja własnego urządzenia LoRa (wykorzystywane są gotowe platformy: LILYGO T-Beam i Seeed Studio T-1000-E);
- modyfikacje firmware'u Meshtastic (komunikacja odbywa się przez standardowe API aplikacji `com.geeksville.mesh`);
- aplikacja iOS (system zaprojektowano wyłącznie pod Androida);
- przesyłanie wiadomości tekstowych przez sieć Meshtastic (aplikacja korzysta wyłącznie z mechanizmu wymiany pozycji);
- integracja z systemem ATAK (referencyjna implementacja znajduje się w `docs/PRZYKLADOWY_KOD.java`, ale wykracza poza ramy v1).

### 1.5 Układ dokumentu

Rozdział 2 omawia istniejące produkty z dziedziny śledzenia psów oraz alternatywne technologie radiowe i platformy programistyczne, prowadząc do uzasadnienia wyborów dokonanych w projekcie. Rozdział 3 formułuje problem inżynierski i wymagania funkcjonalne oraz parametryczne. Rozdział 4 przedstawia podstawy teoretyczne niezbędne do zrozumienia dalszej części pracy. Rozdział 5 opisuje architekturę systemu, dekompozycję na moduły oraz kluczowe decyzje projektowe wraz z rozważanymi alternatywami. Rozdział 6 przedstawia ciąg logiczny implementacji — od pustego projektu Android Studio po działającą aplikację z geofencingiem. Rozdział 7 omawia szczegółowo poszczególne warstwy aplikacji. Rozdział 8 zawiera ocenę rozwiązania oraz weryfikację wymagań. Rozdział 9 zamyka pracę krytyczną refleksją i wskazaniem kierunków dalszego rozwoju.

---

## 2. Przegląd istniejących rozwiązań i wybór technologii

### 2.1 Komercyjne systemy śledzenia psów

**Dogtra Pathfinder 2** — zestaw składa się z odbiornika łączącego się ze smartfonem przez Bluetooth oraz nadajników (do 21 sztuk) umieszczonych na elektronicznych obrożach. Producent deklaruje zasięg do 14,5 km i częstotliwość aktualizacji pozycji co 2 s. Aplikacja Pathfinder 2 udostępnia funkcję E-ogrodzenia w trzech wariantach (Mobile-Fence przemieszczające się z telefonem, statyczne Geo-Fence powiadamiające właściciela oraz E-Fence stosujące korektę elektryczną wobec psa). System jest zamknięty komercyjnie, a sam zestaw kosztuje kilka tysięcy złotych.

**Dogtrace DOG GPS X25** — alternatywne rozwiązanie czeskiej firmy, eliminujące konieczność użycia smartfona. Odbiornik wielkości małego telefonu wyświetla kierunek i odległość do nadajnika na wbudowanym ekranie LCD. Producent deklaruje obsługę do 19 psów, ponad 40 godzin pracy na baterii Li-Pol 1900 mAh oraz zasięg 20 km w warunkach optymalnych. System wspiera wielosystemowe pozycjonowanie GNSS (GPS + GLONASS + Galileo).

Oba systemy działają na zasadzie analogicznej do Meshtastic (LoRa, peer-to-peer, brak infrastruktury), są jednak zamknięte, kosztowne i niemożliwe do rozszerzenia o własne funkcje. Ich istnienie potwierdza praktyczną wykonalność systemu opartego na LoRa, jednocześnie uzasadnia sens budowy rozwiązania otwartego.

### 2.2 Technologie komunikacji bezprzewodowej

Spośród rozważanych technologii (Bluetooth, Wi-Fi, sieci komórkowe, radioamatorstwo, LoRa, LoRaWAN) jedynie **LoRa** spełnia jednocześnie kryteria zasięgu rzędu kilkunastu kilometrów, niskiego zużycia energii oraz niezależności od infrastruktury zewnętrznej. Bluetooth ograniczony jest do dziesiątek metrów; Wi-Fi wymaga punktu dostępowego; sieci komórkowe wymagają pokrycia operatora, którego na obszarze działki brak; radioamatorstwo wymaga licencji. LoRaWAN, choć opiera się na LoRa, wprowadza scentralizowaną architekturę gwiazdy z bramkami, niepotrzebną w zastosowaniu peer-to-peer.

Wśród protokołów warstwy aplikacyjnej zbudowanych na LoRa rozważono Meshtastic, MeshCore oraz Reticulum. **Meshtastic** wybrano ze względu na: dojrzałą i aktywną społeczność, dostępne aplikacje mobilne dla Androida i iOS (zwalniające z konieczności implementacji własnego klienta od podstaw), wbudowaną obsługę telemetrii GPS oraz publicznie udokumentowany interfejs między aplikacją mobilną a urządzeniem.

### 2.3 Wybór platformy sprzętowej

Spośród zestawionych platform (LILYGO T-Beam, Seeed Studio T-1000-E, Heltec ESP32 LoRa, Arduino MKR WAN 1300, Raspberry Pi z modułem SX1268) wybrano T-Beam jako węzeł stacji bazowej („dom") oraz T-1000-E jako węzeł mobilny („pies"). Kryteria wyboru:

| Cecha | T-Beam | T-1000-E | Raspberry Pi + LoRa HAT | Arduino MKR WAN |
|---|---|---|---|---|
| Zużycie energii | Niskie | Bardzo niskie | Wysokie (1–3 W) | Niskie |
| Rozmiar | Średni | Bardzo mały | Duży | Średni |
| Wbudowany GPS | Tak | Tak | Nie | Nie |
| Wodoszczelność | Nie | IP67 | Nie | Nie |
| Gotowe oprogramowanie | Meshtastic | Meshtastic | brak | brak |
| Akumulator | Wymienny 18650 | Wbudowany | Wymaga zewnętrznego | Wymaga zewnętrznego |
| Koszt jednostkowy | Niski | Średni | Wysoki | Niski |

T-1000-E spełnia wszystkie wymagania węzła montowanego na obroży: wodoszczelność IP67, mały rozmiar, wbudowany GPS, czas pracy powyżej 10 godzin. T-Beam jako stacja bazowa nie musi być wodoszczelny ani miniaturowy — kluczowa jest możliwość zasilania ciągłego z sieci lub większego akumulatora oraz lepszy zasięg dzięki większej antenie.

### 2.4 Wybór platformy mobilnej

Aplikację zaprojektowano w technologii natywnej Android z użyciem języka Kotlin i frameworka Jetpack Compose. Decyzja ta wynika z trzech przesłanek:

1. **Dostępność aplikacji Meshtastic dla Androida** — komunikacja z urządzeniem Meshtastic z poziomu telefonu wymaga sparowania go z oficjalną aplikacją `com.geeksville.mesh`. Aplikacja ta eksportuje interfejs AIDL (Android Interface Definition Language) oraz broadcasty systemowe — oba mechanizmy dostępne wyłącznie z aplikacji natywnych.
2. **Brak realnej alternatywy multiplatformowej** — Flutter ani React Native nie oferują wsparcia dla AIDL bez napisania mostu natywnego; w praktyce sprowadzałoby to projekt do równoległej implementacji warstwy natywnej dla Androida, plus dodatkowej warstwy mostka. Multiplatformowość nie była celem pracy.
3. **Jetpack Compose jako współczesny standard** — deklaratywny model UI, integracja ze `StateFlow` z Kotlin Coroutines i wsparcie Material Design 3 znacząco ograniczają objętość kodu interfejsu w porównaniu z klasycznym podejściem opartym o XML i View.

---

## 3. Sformułowanie problemu i wymagania

### 3.1 Przeznaczenie systemu

System MeshTracker służy do śledzenia położenia oznakowanych obiektów (w docelowym zastosowaniu — psa myśliwskiego) w obszarze pozbawionym infrastruktury sieci komórkowej, z aktywnym alarmowaniem właściciela w przypadku przekroczenia przez obiekt zdefiniowanej strefy bezpieczeństwa. System ma działać poprawnie również w sytuacji, gdy użytkownik telefonu nie ma aktywnie otwartej aplikacji, ale telefon znajduje się w trybie czuwania.

### 3.2 Wymagania funkcjonalne

Wymagania funkcjonalne zostały podzielone na trzy poziomy zgodnie z fazami projektu:

**Faza 1 — wizualizacja sieci Meshtastic**

- F1.1. Aplikacja musi automatycznie wykrywać i łączyć się z aplikacją Meshtastic zainstalowaną na tym samym urządzeniu (przez bound service).
- F1.2. Aplikacja musi wyświetlać pozycje wszystkich węzłów z prawidłowymi danymi GPS na mapie.
- F1.3. Aplikacja musi prezentować listę wszystkich wykrytych węzłów wraz ze statusem online/offline, parametrami sygnału (SNR, RSSI), poziomem baterii oraz czasem ostatniego kontaktu.
- F1.4. Aplikacja musi reagować na zmiany pozycji w czasie rzeczywistym (≤ kilka sekund od odebrania broadcastu).
- F1.5. Aplikacja musi czytelnie sygnalizować stan połączenia ze stacją bazową (połączono, rozłączono, łączenie, brak aplikacji Meshtastic).

**Faza 2 — diagnostyka i historia**

- F2.1. Aplikacja musi zapisywać ostatnie pozycje każdego węzła (do skonfigurowanej liczby punktów) i prezentować je jako trasę na mapie.
- F2.2. Aplikacja musi obliczać statystyki pakietów (liczba odebranych, średni odstęp czasowy między odbiorami, PDR — Packet Delivery Ratio).
- F2.3. Aplikacja musi umożliwiać eksport zebranych danych do pliku CSV w katalogu Downloads.
- F2.4. Aplikacja musi pozwalać konfigurować interwał odświeżania danych z serwisu Meshtastic, próg „online", typ mapy oraz filtry domyślne.

**Faza 3 — geofencing**

- F3.1. Użytkownik musi mieć możliwość tworzenia stref geofencingu w postaci wielokątów rysowanych bezpośrednio na mapie.
- F3.2. Strefy mają być zapisywane trwale (zachowanie po restarcie aplikacji i telefonu).
- F3.3. Do każdej strefy musi być możliwe przypisanie listy monitorowanych węzłów.
- F3.4. System musi wykrywać zdarzenia wejścia (ENTER) i wyjścia (EXIT) każdego monitorowanego węzła z każdej aktywnej strefy.
- F3.5. Każde zdarzenie musi wywoływać powiadomienie systemowe z priorytetem HIGH i być zapisane w trwałym dzienniku zdarzeń.
- F3.6. Monitoring musi działać niezależnie od aktywności UI aplikacji (foreground service).

### 3.3 Wymagania niefunkcjonalne i ograniczenia

- N1. Aplikacja musi działać na Androidzie 7.0 (API 24) i nowszym; targetSDK = 36.
- N2. Aplikacja nie może wymagać stałego dostępu do Internetu (z wyjątkiem pobierania kafelków mapy Google Maps; dane Meshtastic są wymieniane lokalnie).
- N3. Algorytm detekcji strefy musi być deterministyczny i przewidywalny — zalecane jest użycie standardowego algorytmu testowanego w literaturze (ray-casting).
- N4. Aplikacja musi być odporna na zmianę API aplikacji Meshtastic (różne wersje firmware'u i aplikacji `com.geeksville.mesh` używają różnych nazw metod i pól; opis problemu w sekcji 5.4).
- N5. Aplikacja musi być testowalna — kluczowe komponenty muszą być wstrzykiwane przez kontener DI, a logika biznesowa (geofencing) musi być pozbawiona zależności Androida, aby umożliwić testy JVM bez emulatora.

### 3.4 Założenia

Z założeniami pracy związane są decyzje o tym, czego system nie musi rozwiązywać:

- Pies posiada węzeł Meshtastic skonfigurowany jako tracker (rola `TRACKER`) z włączoną wymianą pozycji co skonfigurowany interwał (ok. 30 s). Konfiguracja firmware'u nie jest częścią aplikacji.
- Telefon użytkownika znajduje się w zasięgu radia stacji bazowej, do której podłączony jest przez Bluetooth.
- Użytkownik wyraża zgodę na powiadomienia oraz lokalizację (uprawnienia POST_NOTIFICATIONS od Android 13+, ACCESS_FINE_LOCATION).

---

## 4. Podstawy teoretyczne

Sekcja ogranicza się do pojęć, których zrozumienie jest niezbędne dla dalszej części pracy. Powszechnie znane mechanizmy Androida (Activity, Service, BroadcastReceiver) zakłada się jako znane czytelnikowi z literatury podstawowej.

### 4.1 Modulacja LoRa i sieć mesh Meshtastic

LoRa (Long Range) jest zastrzeżoną modulacją radiową firmy Semtech, wykorzystującą technikę chirp spread spectrum w paśmie ISM (868 MHz w Europie). Konfigurowalne parametry — spreading factor (SF) od 7 do 12 i szerokość pasma (BW) od 125 do 500 kHz — pozwalają wymieniać zasięg na przepustowość. Typowe ustawienia używane w Meshtastic dla preset *Long Slow* (SF11, BW125) zapewniają zasięg kilkunastu kilometrów w terenie otwartym kosztem przepustowości około 200 b/s.

Meshtastic to oprogramowanie firmware oraz aplikacje mobilne tworzące zdecentralizowaną sieć mesh ponad LoRa. Każdy węzeł retransmituje odebrane pakiety z mechanizmem TTL (Time to Live) eliminującym pętle. Pakiety telemetrii pozycyjnej (`POSITION_APP`) zawierają współrzędne, wysokość, prędkość naziemną, kurs naziemny oraz znacznik czasu. Format danych definiowany jest w plikach Protocol Buffers, których aktualna wersja znajduje się w repozytorium `github.com/meshtastic/protobufs`.

W aplikacji Android Meshtastic (`com.geeksville.mesh`) wewnętrzny stan sieci utrzymywany jest w klasie `org.meshtastic.core.model.NodeInfo`, dostępnej z zewnątrz wyłącznie poprzez interfejs AIDL `org.meshtastic.core.service.IMeshService` oraz broadcasty `com.geeksville.mesh.NODE_CHANGE`, `MESH_CONNECTED`, `MESH_DISCONNECTED`.

### 4.2 Komunikacja międzyprocesowa w Androidzie: AIDL i Parcelable

AIDL (Android Interface Definition Language) jest deklaratywnym językiem opisu zdalnego interfejsu, kompilowanym do klas Java pełniących rolę proxy między procesem klienta a procesem usługi. Klient otrzymuje obiekt typu `IBinder`, który po wywołaniu `IInterface.Stub.asInterface(binder)` daje typowy uchwyt do zdalnego API. Parametry i wartości zwracane wymagają serializacji do/z `Parcel` — obiekty implementujące `android.os.Parcelable` muszą znać swoją klasę po stronie odbiorcy, co przekłada się na wymóg dostępu do `ClassLoader` aplikacji-właściciela klasy. Brak tego ClassLoadera prowadzi do błędu `ClassNotFoundException` przy próbie deserializacji. Mechanizm rozwiązania tego problemu zastosowany w MeshTrackerze opisuje sekcja 5.4.

### 4.3 Geofencing oparty o algorytm ray-casting

Geofencing polega na detekcji przynależności punktu (lat, lon) do obszaru wielokątnego zdefiniowanego przez sekwencję wierzchołków. Algorytm użyty w pracy — *ray-casting* (zwany też testem parzystości lub algorytmem Jordana) — działa następująco: z badanego punktu prowadzony jest poziomy promień w prawo (w kierunku rosnących długości geograficznych). Liczone są przecięcia tego promienia z krawędziami wielokąta. Nieparzysta liczba przecięć oznacza punkt wewnątrz wielokąta, parzysta — na zewnątrz. Złożoność: O(n) względem liczby wierzchołków. Algorytm nie wymaga aproksymacji ani konwersji do układu współrzędnych prostokątnych przy umiarkowanych rozmiarach strefy (rzędu kilkuset metrów), ponieważ błąd związany z krzywizną Ziemi jest nieistotny.

### 4.4 Reaktywne zarządzanie stanem: StateFlow i Jetpack Compose

`StateFlow` z biblioteki Kotlin Coroutines jest gorącym strumieniem zawierającym zawsze dokładnie jedną aktualną wartość; każdy abonent otrzymuje wartość bieżącą i wszystkie kolejne. Jetpack Compose integruje się z `StateFlow` poprzez funkcję rozszerzającą `collectAsState()`, która rejestruje kolektora w pamięci kompozycji i wywołuje rekompozycję przy każdej emisji nowej wartości. Pozwala to wyeliminować ręczne zarządzanie odświeżaniem UI: zmiana mapy węzłów w ViewModelu automatycznie wywołuje przeliczenie odpowiednich kompozytów na ekranie.

### 4.5 Hilt — wstrzykiwanie zależności

Hilt jest opracowaną przez Google nakładką na Daggera, dostosowaną do specyfiki Androida. Konfiguruje on graf zależności w punktach wejścia oznaczonych adnotacjami (`@HiltAndroidApp`, `@AndroidEntryPoint`, `@HiltViewModel`) oraz dostarcza obiekty zgodnie z modułami (`@Module` + `@Provides`/`@Binds`). W pracy Hilt rozwiązuje problem testowalności: produkcyjny `MeshServiceRepository` jest wpięty jako implementacja interfejsu `MeshRepository`, a w testach instrumentacyjnych można podmienić go na atrapę bez modyfikacji warstwy ViewModel.

---

## 5. Architektura systemu

### 5.1 Diagram kontekstu

```
+---------------------+           radio LoRa            +----------------------+
|  Węzeł "pies"       |  <---------------------------> |  Węzeł "dom"          |
|  T-1000-E           |  POSITION_APP (~co 30 s)        |  T-Beam              |
|  rola: TRACKER      |                                 |  rola: CLIENT/ROUTER |
+---------------------+                                 +----------+-----------+
                                                                  |
                                                          Bluetooth/USB
                                                                  |
                                                                  v
                                      +------------------------------------------+
                                      |  Telefon Android                          |
                                      |  +-----------------------------------+    |
                                      |  | Aplikacja Meshtastic              |    |
                                      |  | com.geeksville.mesh               |    |
                                      |  |  - MeshService (AIDL)             |    |
                                      |  |  - broadcasty NODE_CHANGE...      |    |
                                      |  +----------+------------------------+    |
                                      |             |                              |
                                      |             | IPC                          |
                                      |             v                              |
                                      |  +-----------------------------------+    |
                                      |  | Aplikacja MeshTracker             |    |
                                      |  | (przedmiot pracy)                 |    |
                                      |  +-----------------------------------+    |
                                      +------------------------------------------+
```

System składa się z trzech komponentów: dwóch urządzeń sprzętowych komunikujących się przez LoRa oraz aplikacji mobilnej. Aplikacja Meshtastic firmy Geeksville jest pośrednikiem między radiem a aplikacją MeshTracker. MeshTracker nie komunikuje się bezpośrednio z radiem — wszystkie operacje przechodzą przez stabilne API aplikacji Meshtastic.

### 5.2 Dekompozycja warstwowa aplikacji

Aplikacja realizuje wzorzec MVVM (Model-View-ViewModel) wzbogacony o warstwę repozytoryjną, kontener DI (Hilt) oraz oddzielony foreground service. Pakiety w przestrzeni `com.example.meshtracker_v1`:

```
+--------------------------------------------------------------+
|  ui                  (View — Jetpack Compose)                |
|    MainScreen, MapScreen, NodeListScreen, NodeDetailScreen,  |
|    SettingsScreen, ZoneDetailScreen, ZoneBottomSheet,        |
|    ZoneConfirmDialog, ConnectionStatusBar                    |
+------------------------------+-------------------------------+
                               |
+------------------------------v-------------------------------+
|  ui (ViewModel)              MapViewModel, ZoneViewModel,    |
|                              SettingsViewModel               |
+------------------------------+-------------------------------+
                               |
+------------------------------v-------------------------------+
|  repository  (warstwa abstrakcji danych)                     |
|    MeshRepository (interface)                                |
|    MeshServiceRepository (implementacja produkcyjna)         |
|    ZoneRepository, PositionHistoryRepository,                |
|    PacketStatsRepository                                     |
+----+--------+----------+-------------+-----------------------+
     |        |          |             |
     v        v          v             v
+--------+ +-------+ +-------+ +-----------------+
| service| |receive| | data  | | logic           |
| Mesh-  | | Mesh- | | Room  | | GeofenceChecker |
| Service| | tastic | | Zone- | | (czysty Kotlin) |
| Manager| | Broad- | | Dao/  | +-----------------+
|        | | cast-  | | DB    |
|        | | Receiv | | App-  |
|        | | er     | | Prefs |
+--------+ +-------+ +-------+
     |        |          
     v        v          
+--------------------------+
|  model                   |
|    MeshNodeInfo,         |
|    MeshPosition,         |
|    MeshUserInfo,         |
|    Zone, ZoneEvent,      |
|    ZoneVertex,           |
|    TimedPosition,        |
|    PacketStats           |
+--------------------------+
                               
   +------------------------------+
   |  service.ZoneMonitorService  |
   |  (Foreground Service +       |
   |   MeshtasticBroadcastReceiver)|
   +------------------------------+
                               
   +------------------------------+
   |  di  (Hilt)                  |
   |    AppModule, DatabaseModule |
   +------------------------------+
```

Każda warstwa zależy wyłącznie od warstw niższych i od abstrakcji. ViewModel nie zna implementacji `MeshServiceManager`, lecz interfejsu `MeshRepository`; warstwa logiki geofencingu (`GeofenceChecker`) nie zna Androida i może być testowana czysto w JVM.

### 5.3 Decyzje projektowe — alternatywy i uzasadnienie

**Decyzja D1: Reflection zamiast skompilowanego interfejsu AIDL**

Aplikacja Meshtastic dystrybuuje interfejs `IMeshService.aidl` w swoim repozytorium publicznym, jednak nie udostępnia ich w postaci biblioteki Maven. Rozważono trzy warianty integracji:

| Wariant | Zalety | Wady |
|---|---|---|
| (a) Skopiowanie plików .aidl do projektu | Sprawdzanie typów w czasie kompilacji, autouzupełnianie w IDE | Kopia musi być aktualizowana przy każdej zmianie API; ryzyko cichego rozjazdu między wersjami |
| (b) Dołączenie aplikacji Meshtastic jako lokalnej AAR | Pełny dostęp do klas | Konieczność utrzymywania kopii AAR, problemy licencyjne przy dystrybucji |
| (c) Java Reflection na obiekcie zwróconym przez bindService() | Brak twardej zależności kompilacyjnej, możliwość obsługi różnych wersji nazw metod | Błędy wykrywane w runtime; konieczność pisania wielu prób z różnymi nazwami |

Wybrano wariant (c) — reflection. Decyzja motywowana faktem, że firmware Meshtastic i aplikacja `com.geeksville.mesh` przechodzą gruntowne zmiany w nazwach pól (np. `snr` vs `rxSnr`, `getMyId` vs `getMyNodeId`), a wariant (a) wymagałby ciągłej obsługi tych zmian na poziomie kompilacji. Kod implementujący reflection jest skupiony w jednym miejscu (`MeshServiceManager`, `MeshNodeInfo.fromMeshtasticNodeInfo`, `MeshPosition.fromMeshtasticPosition`, `MeshUserInfo.fromMeshtasticUser`) i zawiera serię prób fallback, dzięki czemu obsługuje co najmniej 3 historyczne nazewnictwa równolegle.

**Decyzja D2: Wielokąty zamiast okręgów dla stref**

Pierwsza wersja planu zakładała strefy kołowe (decyzja udokumentowana w `docs/ARCHITEKTURA.md`). W ostatecznej implementacji zmieniono je na wielokąty. Powody:

- Kształt rzeczywistej działki rzadko jest okrągły — w terenie wiejskim częściej granicę wyznaczają miedze, ścieżki, linia lasu.
- Algorytm ray-casting jest niewiele bardziej złożony od testu odległości euklidesowej dla okręgu.
- Wielokąt jest naturalnym uogólnieniem okręgu (zbiór wielokątów obejmuje okręgi przybliżone z dowolną precyzją).

**Decyzja D3: Foreground Service zamiast WorkManager**

Detekcja naruszenia strefy musi działać w sytuacji, gdy aplikacja jest w tle. Rozważono dwa warianty:

| Wariant | Zalety | Wady |
|---|---|---|
| WorkManager | Energooszczędność, wbudowane ponowne próby | Brak gwarancji czasu wykonania (≥ 15 min), nieadekwatny do reaktywnego nasłuchiwania broadcastów |
| Foreground Service | Działa natychmiast po broadcaście, widoczne dla użytkownika, niezabijane przez system | Wymaga trwałego powiadomienia, wyższe zużycie energii |

Wybrano Foreground Service (`ZoneMonitorService`), ponieważ wymaganie F3.4 zakłada reakcję w czasie rzędu sekund, nie kwadransów. Powiadomienie trwałe stanowi jednocześnie czytelny sygnał dla użytkownika, że monitoring jest aktywny.

**Decyzja D4: Room + DataStore zamiast SharedPreferences i własnej serializacji**

Dane stref wymagają trwałości, zapytań i obserwacji w formie `Flow`. SharedPreferences nie nadaje się do strukturalnych danych. Wybrano Room (relacyjna baza SQLite z mapowaniem ORM), uzupełniony przez DataStore (preferences) dla prostych ustawień skalarnych. Encje `Zone` i `ZoneEvent` połączone są kluczem obcym z `ON DELETE CASCADE` — usunięcie strefy automatycznie usuwa jej historię zdarzeń.

**Decyzja D5: Hilt jako kontener DI**

Hilt zapewnia jeden, ustandaryzowany kontener DI z gotowymi adnotacjami pod Activity, ViewModel i Service. Alternatywą byłby surowy Dagger 2 (więcej kodu konfiguracyjnego) lub Koin (czysto kotlinowy, ale bez weryfikacji w czasie kompilacji). Wybrano Hilt, ponieważ jest oficjalnie wspierany przez Google i daje gwarancje wykrywania błędów na etapie kompilacji.

**Decyzja D6: Współdzielony `MapViewModel` dla mapy i listy**

Mapa i lista węzłów operują na tym samym stanie (`nodes`, `connectionState`). Wydzielenie osobnych ViewModeli zmuszałoby do synchronizacji. Decyzja ta przeniosła wszystkie subskrypcje stanu Meshtastic do jednej klasy.

### 5.4 Mechanizm dynamicznego wczytywania ClassLoadera

Najtrudniejszym pojedynczym problemem inżynierskim w projekcie była deserializacja obiektu `org.meshtastic.core.model.NodeInfo` z broadcastu wysyłanego przez aplikację Meshtastic. Klasa ta jest typu `Parcelable`, ale nie znajduje się w ścieżce klas aplikacji MeshTracker. Standardowe wywołanie `intent.getParcelableExtra(...)` kończy się `ClassNotFoundException`.

Zastosowane rozwiązanie składa się z czterech kroków, zaimplementowanych w `MeshtasticBroadcastReceiver.handleNodeChange()`:

1. Utworzenie kontekstu pakietu Meshtastic z uprawnieniem do wykonywania kodu:
   ```kotlin
   val meshtasticContext = context.createPackageContext(
       Constants.MESHTASTIC_PACKAGE,
       Context.CONTEXT_INCLUDE_CODE or Context.CONTEXT_IGNORE_SECURITY
   )
   val meshtasticClassLoader = meshtasticContext.classLoader
   ```
2. Tymczasowe podmienianie Thread context ClassLoadera:
   ```kotlin
   val originalClassLoader = Thread.currentThread().contextClassLoader
   Thread.currentThread().contextClassLoader = meshtasticClassLoader
   ```
3. Ustawienie ClassLoadera na `Bundle.extras` przed wywołaniem `getParcelable`:
   ```kotlin
   val bundle = intent.extras ?: return
   bundle.classLoader = meshtasticClassLoader
   val nodeInfoClass = meshtasticClassLoader.loadClass("org.meshtastic.core.model.NodeInfo")
   val nodeInfoObj = bundle.getParcelable(Constants.EXTRA_NODE_INFO, nodeInfoClass)
   ```
4. Przywrócenie oryginalnego ClassLoadera w bloku `finally`.

Powstały obiekt `Any?` jest następnie przekazywany do statycznej metody `MeshNodeInfo.fromMeshtasticNodeInfo()`, która przez kolejne wywołania reflection wydobywa wartości potrzebnych pól. Kod jest celowo bogato logowany (`Log.d` dla każdej próby) — pozwala to zdiagnozować zmianę API Meshtastic bez doczepiania debuggera.

### 5.5 Maszyna stanów połączenia

`MapViewModel.ConnectionState` realizuje pięciostanową maszynę stanów:

```
                       MeshtasticNotInstalled <----- nie znaleziono com.geeksville.mesh
                                                    
                       Disconnected               <----- start aplikacji / utracone połączenie serwisowe
                            |
                            | retryConnect() lub init()
                            v
                       Connecting                 <----- bindService rozpoczęty, AIDL gotowe, czekamy na radio
                            |
                            | connectionState() == "CONNECTED"
                            | lub onNodeChanged() (broadcast)
                            v
                       Connected                  <----- aktywne odpytywanie + nasłuch broadcastów
                            |
                            | onMeshDisconnected (broadcast)
                            v
                       Reconnecting(retryInSec)  <----- exponential / okresowe ponawianie co 30 s
                                                    |
                              (po retryInSec)       |
                                                    v
                                              ponownie Connecting albo Disconnected
```

Każdy stan wpływa na sygnalizację UI (`ConnectionStatusBar`) i decyzje o uruchamianiu zadań okresowych (`startPeriodicRefresh`, `startConnectionCheck`, `startReconnecting`). Stan `Reconnecting` zawiera pozostały czas oczekiwania, wyświetlany użytkownikowi.

---

## 6. Ciąg logiczny implementacji

Sekcja przedstawia kolejność, w jakiej powstawały kolejne warstwy aplikacji. Dla każdego kroku opisano motywację, wybory implementacyjne, ewentualne błędy i sposób ich pokonania. Cel: pokazać, że projekt nie powstał monolitycznie, lecz drogą iteracyjną — od minimalnego prototypu po pełną funkcjonalność.

### Krok 1. Pusty projekt i wybór stosu technologicznego

Punktem wyjścia był projekt utworzony w Android Studio z domyślną aktywnością opartą na Jetpack Compose. Skonfigurowano `compileSdk = 36`, `minSdk = 24`, Kotlin 2.0.21 oraz wtyczki Compose i KSP. Plik `gradle/libs.versions.toml` posłużył jako jedyne źródło wersji bibliotek (single source of truth).

### Krok 2. Konfiguracja manifestu i widoczności pakietu Meshtastic

Android 11 wprowadził mechanizm Package Visibility — aplikacja domyślnie nie widzi innych aplikacji zainstalowanych na urządzeniu. Aby umożliwić binding do aplikacji Meshtastic, dodano w `AndroidManifest.xml` blok:

```xml
<queries>
    <package android:name="com.geeksville.mesh" />
</queries>
```

Zadeklarowano też uprawnienia: `INTERNET` (dla kafelków Google Maps), `ACCESS_FINE_LOCATION` i `ACCESS_COARSE_LOCATION` (dla pokazywania własnej pozycji), `FOREGROUND_SERVICE` i `FOREGROUND_SERVICE_DATA_SYNC` (dla `ZoneMonitorService`), `POST_NOTIFICATIONS` (Android 13+) oraz `WRITE_EXTERNAL_STORAGE` (tylko do API 28, dla eksportu CSV).

### Krok 3. Stałe komunikacji z Meshtastic

W `util/Constants.kt` zdefiniowano w jednym miejscu wszystkie magiczne stringi:

```kotlin
object Constants {
    const val ACTION_NODE_CHANGE       = "com.geeksville.mesh.NODE_CHANGE"
    const val ACTION_MESH_CONNECTED    = "com.geeksville.mesh.MESH_CONNECTED"
    const val ACTION_MESH_DISCONNECTED = "com.geeksville.mesh.MESH_DISCONNECTED"
    const val EXTRA_NODE_INFO          = "com.geeksville.mesh.NodeInfo"
    const val EXTRA_CONNECTED          = "com.geeksville.mesh.Connected"
    const val STATE_CONNECTED          = "CONNECTED"
    const val STATE_DISCONNECTED       = "DISCONNECTED"
    const val MESHTASTIC_PACKAGE       = "com.geeksville.mesh"
    const val MESH_SERVICE_ACTION      = "com.geeksville.mesh.service.MeshService"
}
```

Wyizolowanie tych stałych ułatwia ich późniejszą aktualizację przy zmianie wersji aplikacji Meshtastic.

### Krok 4. Bindowanie do `MeshService` — `MeshServiceManager`

Pierwszy poważny komponent. Singleton (`@Volatile + synchronized` w `getInstance`) zarządzający dokładnie jednym połączeniem z serwisem Meshtastic. Implementacja `ServiceConnection.onServiceConnected()` zawiera kluczowy fragment reflection:

```kotlin
val stubClass = Class.forName("org.meshtastic.core.service.IMeshService\$Stub")
val asInterfaceMethod = stubClass.getMethod("asInterface", IBinder::class.java)
meshService = asInterfaceMethod.invoke(null, service)
```

Zaprojektowano interfejs `ConnectionListener` z metodami `onServiceConnected()` i `onServiceDisconnected()`. Dodano tryb *broadcast-only* — gdy klasa `IMeshService$Stub` nie jest dostępna, manager zachowuje binding ale ustawia `meshService = null`, polegając wyłącznie na broadcastach.

Wykryto problemy:
- Aplikacja Meshtastic mogła być niezainstalowana — dodano `isMeshtasticInstalled()` sprawdzające pakiet przez `PackageManager`.
- Wywoływanie `unbindService()` na nigdy niezbindowanym połączeniu rzuca wyjątek — dodano flagę `isBound`.
- Rozłączenie zainicjowane przez aplikację (`disconnect()`) nie powinno wywoływać callbacku `onServiceDisconnected` — dodano flagę `isIntentionalDisconnect`.

### Krok 5. Modele domenowe — `MeshNodeInfo`, `MeshPosition`, `MeshUserInfo`

Modele domenowe pełnią funkcję anty-corruption layer. Każdy z nich:
- Jest immutable `data class` zawierającym typy Kotlinowe (`Int`, `Double`, `String`).
- Posiada metodę statyczną `fromMeshtastic...()` przyjmującą `Any?` (obiekt z reflection).
- Próbuje kolejno różnych nazw metod i pól, korzystając z `try-catch` zagnieżdżonych dla każdej wersji API.

W `MeshPosition` znalazła się dodatkowo logika korekty formatu — niektóre wersje Meshtastic dostarczają współrzędne jako Int w formacie E7 (stopnie × 10⁷), inne jako Double. Detekcja heurystyczna (`Math.abs(value) > 1000.0` → format E7) wraz z dzieleniem przez `1e7` obsługuje oba przypadki. Analogicznie `groundTrack` w niektórych wersjach jest podany w setnych tysięcznych stopnia (× 100000) — dzielony przez `100000`, gdy wartość > 360.

`MeshNodeInfo` wprowadza metody pomocnicze stanowiące część logiki biznesowej:
- `hasValidPosition()` — pozycja istnieje i jest w zakresach geograficznych.
- `isOnline(thresholdSeconds: Int = 300)` — `lastHeard` w ciągu ostatnich N sekund.
- `getDisplayName()` i `getId()` z fallbackami dla brakujących danych.

### Krok 6. Odbiór broadcastów — `MeshtasticBroadcastReceiver`

Implementacja receivera reaguje na trzy akcje. Najtrudniejsza była `ACTION_NODE_CHANGE`, której obsługa wymagała mechanizmu ClassLoadera opisanego w sekcji 5.4. Po wyodrębnieniu `nodeInfoObj` z `Bundle`, jest on przekazywany do `MeshNodeInfo.fromMeshtasticNodeInfo()`. Receiver eksponuje interfejs `MeshtasticReceiverListener` z metodami `onNodeChanged`, `onMeshConnected`, `onMeshDisconnected`.

### Krok 7. Warstwa repozytorium — abstrakcja danych

Aby umożliwić testowanie ViewModeli bez prawdziwego serwisu Meshtastic, wprowadzono interfejs `MeshRepository`:

```kotlin
interface MeshRepository {
    fun isMeshtasticInstalled(): Boolean
    fun connect(): Boolean
    fun disconnect()
    fun isConnected(): Boolean
    fun getNodes(): List<MeshNodeInfo>?
    fun getMyNodeId(): String?
    fun getConnectionState(): String?
    fun addListener(listener: MeshEventListener)
    fun removeListener(listener: MeshEventListener)
    interface MeshEventListener {
        fun onServiceConnected(); fun onServiceDisconnected()
        fun onNodeChanged(nodeInfo: MeshNodeInfo)
        fun onMeshConnected(); fun onMeshDisconnected()
    }
}
```

Implementacja produkcyjna `MeshServiceRepository` agreguje `MeshServiceManager` i `MeshtasticBroadcastReceiver` w jedną fasadę. Rejestruje receivera przy `connect()`, wyrejestrowuje przy `disconnect()`. Broadcasty pochodzące spoza własnego pakietu wymagają flagi `Context.RECEIVER_EXPORTED` (Android 13+).

### Krok 8. Konfiguracja Hilta i podział na moduły

Punktem wejścia jest klasa `MeshTrackerApplication` adnotowana `@HiltAndroidApp`. Moduł `AppModule` zawiera:
- `@Binds` wiążący `MeshRepository` → `MeshServiceRepository`.
- `@Provides` dostarczający `MeshServiceManager` jako singleton.

Moduł `DatabaseModule` dostarcza `ZoneDatabase` (Room) i `ZoneDao`. ViewModele otrzymują wszystkie zależności w konstruktorze; w UI Compose są pozyskiwane przez `hiltViewModel()`.

### Krok 9. `MapViewModel` — centralny stan i orkiestracja

Najobszerniejsza klasa. Realizuje:
- Subskrypcję zdarzeń z `MeshRepository` przez interfejs `MeshEventListener`.
- Reaktywne `StateFlow`: `nodes`, `selectedNodeId`, `myNodeId`, `connectionState`, `filterState`.
- Reaktywne `StateFlow` mapowane z preferencji: `onlineThresholdSeconds`, `mapType`, `refreshIntervalSeconds`, `historyMaxPoints`, `historyMinDistanceM`, `showAllTracks`, `expectedBroadcastInterval`.
- Zadania okresowe: `periodicRefreshJob` odświeża listę węzłów co interwał z preferencji (domyślnie 5 s), `connectionCheckJob` poll'uje stan radia w fazie Connecting co 2 s, `reconnectJob` realizuje 30 s odliczanie z aktualizacją stanu `Reconnecting(retryInSeconds)`.
- Inteligentne wykrywanie połączenia z radiem — gdy odebrany zostanie `NODE_CHANGE` mimo stanu Connecting/Disconnected, ViewModel zakłada że radio jest aktywne i przechodzi do Connected (naprawia race condition obserwowany w testach).
- Wykrywanie ruchu — gdy nowa pozycja różni się od poprzedniej, obliczana jest przybliżona odległość (aproksymacja euklidesowa z poprawką na cosinus szerokości geograficznej) i logowana.
- Zapis pozycji do `PositionHistoryRepository` z filtrowaniem (`minDistanceM`).
- Aktualizacja `PacketStatsRepository` dla każdego odebranego pakietu.
- `filteredNodes` — wynik combinacji trzech `StateFlow` (`_nodes`, `_filterState`, `onlineThresholdSeconds`).
- `nodesInZones` — reaktywne przeliczanie rozmieszczenia węzłów w strefach (kombinacja `_nodes × activeZones` przez `GeofenceChecker.computeNodeZoneMap`).

### Krok 10. Warstwa UI — Jetpack Compose

`MainScreen` jest nawigacyjnym kontenerem opartym na klasie zapieczętowanej `Screen`. Pięć ekranów: `Map`, `List`, `Settings`, `NodeDetail(nodeId)`, `ZoneDetail(zoneId)`. Dwa ostatnie są ekranami szczegółów wyłączającymi dolny pasek i pasek statusu.

`MapScreen` integruje bibliotekę `maps-compose`. Wyświetla węzły jako markery (różne kolory dla ról TRACKER, CLIENT, ROUTER), strefy jako wielokąty (`Polygon`), trasy jako linie (`Polyline`), oraz nakładkę trybu rysowania. Long-press na mapie w trybie rysowania dodaje wierzchołek (`ZoneViewModel.addVertex`).

`NodeListScreen` używa `LazyColumn` z `NodeItem` jako elementem; pasek filtrów pozwala przełączać widok między „tylko online" i „tylko z GPS". `NodeDetailScreen` pokazuje szczegóły wybranego węzła (statystyki pakietów, mini-mapa, historia pozycji, panel eksportu CSV). `SettingsScreen` operuje na `SettingsViewModel`, który zapisuje zmiany do DataStore.

### Krok 11. Persystencja — Room dla stref

Encja `Zone` ma id typu UUID, nazwę, kolor, flagę aktywności, listę wierzchołków (`verticesJson`) i listę monitorowanych węzłów (`watchedNodeIdsJson`). Listy serializowane są ręcznie do JSON-a (bez `TypeConverter`) — uproszczenie wynikające z faktu, że oba pola to po prostu listy obiektów prostych.

Encja `ZoneEvent` rejestruje historię ENTER/EXIT z migawkami nazwy węzła i znacznikiem czasu. Klucz obcy `zoneId` → `zones(id)` z `ON DELETE CASCADE` zapewnia, że usunięcie strefy oczyszcza też log.

`ZoneDao` definiuje operacje CRUD oraz reaktywne zapytania zwracające `Flow<List<Zone>>` i `Flow<List<ZoneEvent>>`. `ZoneRepository` opakuje DAO i dodaje metody pomocnicze (`setActive`, `updateWatchedNodes`, `recordEvent`).

### Krok 12. Algorytm geofencingu

`GeofenceChecker` jest pojedynczym `object`em z czystym Kotlinem (zero zależności Androida). Funkcja `contains(pointLat, pointLon, polygon)` realizuje klasyczny algorytm ray-casting. Kluczowa pętla:

```kotlin
for (i in polygon.indices) {
    val vi = polygon[i]; val vj = polygon[j]
    val crossesRay = (vi.lat <= pointLat && vj.lat > pointLat) ||
                     (vj.lat <= pointLat && vi.lat > pointLat)
    if (crossesRay) {
        val intersectLon = vi.lon + (pointLat - vi.lat) / (vj.lat - vi.lat) * (vj.lon - vi.lon)
        if (pointLon < intersectLon) inside = !inside
    }
    j = i
}
```

Niezależność od frameworka pozwala testować algorytm jednostkowo w JVM (test `GeofenceCheckerTest` może uruchomić się bez emulatora). Metoda `computeNodeZoneMap()` agreguje wyniki dla wszystkich kombinacji węzłów i stref, używana zarówno przez `ZoneMonitorService` (detekcja zmian stanu), jak i przez `MapViewModel` (kolorowanie markerów).

### Krok 13. `ZoneMonitorService` — monitorowanie w tle

Foreground Service z typem `dataSync` (wymóg API 29+). Cykl życia:

1. `ZoneViewModel.init` obserwuje `zoneRepository.allZones` i wywołuje `start(context)` gdy istnieje ≥ 1 aktywna strefa lub `stop(context)` w przeciwnym razie.
2. `onCreate()` natychmiast wywołuje `startForegroundCompat()` z powiadomieniem (wymagane przez Androida — w przeciwnym razie ANR po 5 s).
3. Serwis rejestruje własny `MeshtasticBroadcastReceiver` z flagą `RECEIVER_EXPORTED`.
4. Koroutyna w `serviceScope` (Dispatchers.IO + SupervisorJob) kolekcjonuje aktywne strefy z Room — Flow gwarantuje aktualność danych.
5. Każde `onNodeChanged` wywołuje `checkNodeAgainstZones(nodeInfo)`:
   - Obliczany jest zbiór `currentZones` zawierających węzeł (poprzez `GeofenceChecker.contains`).
   - Porównywany jest z `previousNodeZoneMap` z poprzedniego sprawdzenia.
   - Różnica `currentZones - previousZones` = ENTER; różnica `previousZones - currentZones` = EXIT.
   - Każde zdarzenie zapisywane jest przez `zoneRepository.recordEvent()` i wywołuje powiadomienie.
6. Self-stop: gdy strumień zwróci pustą listę aktywnych stref, serwis wywołuje `stopSelf()`.

Powiadomienia dzielą się na dwa kanały (`CHANNEL_ZONE_SERVICE` IMPORTANCE_LOW dla cichego statusu, `CHANNEL_ZONE_ALERTS` IMPORTANCE_HIGH dla alarmów ENTER/EXIT). Identyfikator powiadomienia alertu generowany jest jako `(zone.id + nodeName).hashCode()` — gwarantuje, że kolejne zdarzenia dla tego samego węzła w tej samej strefie aktualizują istniejące powiadomienie, zamiast tworzyć stos.

### Krok 14. Eksport danych — `CsvExporter`

Eksport CSV używa MediaStore (API 29+) lub `Environment.getExternalStoragePublicDirectory` (starsze). Format zawiera 16 kolumn z pełną telemetrią (`timestamp_unix, timestamp_readable, node_id, node_name, role, lat, lng, altitude_m, snr_db, rssi_dbm, hops_away, delta_t_s, battery_pct, speed_ms, heading_deg, satellites`). Eksport może być pełny (wszystkie węzły) lub filtrowany do jednego węzła.

### Krok 15. Testy

Konfiguracja `androidTestImplementation` zawiera Hilt Test, Espresso, Compose UI test, Turbine (dla flow). Niestandardowy `HiltTestRunner` umożliwia podmianę modułów na testowe. `GeofenceCheckerTest` weryfikuje algorytm ray-casting (rogi wielokąta, przypadki brzegowe). Wymaganie pełnego pokrycia testowego nie zostało jeszcze zrealizowane — opisane w sekcji 9.2.

---

## 7. Szczegółowy opis modułów

### 7.1 Pakiet `model` — domena

| Klasa | Funkcja |
|---|---|
| `MeshNodeInfo` | Wrapper węzła. Pola: `num, user, position, snr, rssi, lastHeard, batteryLevel, channel, hopsAway`. Metody: `hasValidPosition`, `isOnline`, `getDisplayName`, `getId`. Statyczna fabryka `fromMeshtasticNodeInfo(Any?)`. |
| `MeshPosition` | Pozycja GPS. Pola: `latitude, longitude, altitude, time, satellitesInView, groundSpeed, groundTrack, precisionBits`. Aliasowane przez `heading` i `speed`. Walidacja: `isValid`, `isInRange`, konwersja `toLatLng`. |
| `MeshUserInfo` | Dane użytkownika węzła. Pola: `id, longName, shortName, hwModelString, isLicensed, role`. |
| `Zone` (`@Entity`) | Strefa geofencingu w Room. Pola: `id (UUID), name, colorArgb, isActive, verticesJson, watchedNodeIdsJson`. Helper `Zone.create()` upraszcza tworzenie z listy wierzchołków. |
| `ZoneEvent` (`@Entity`) | Zdarzenie ENTER/EXIT. ForeignKey do `Zone` z CASCADE. |
| `ZoneVertex` | Para `(lat: Double, lon: Double)`. Czysty Kotlin (bez Androida) — testowalny w JVM. |
| `TimedPosition` | Pozycja z znacznikiem czasu — element historii. |
| `PacketStats` | Statystyki pakietów: `receivedCount, lastReceivedSeconds, deltaTHistory, sessionStartSeconds`. Property `avgDeltaT`, `minDeltaT`, `maxDeltaT`, metoda `pdr(expectedIntervalSeconds)` zwracająca Packet Delivery Ratio w %. |

### 7.2 Pakiet `service`

| Klasa | Funkcja |
|---|---|
| `MeshServiceManager` | Singleton bindujący `IMeshService`. Reflection wrap dla metod `getNodes`, `getMyNodeID`, `connectionState`. Tryb broadcast-only przy braku Stub w classpath. |
| `ZoneMonitorService` | Foreground Service typu `dataSync`. Implementuje `MeshtasticReceiverListener`. Detekcja ENTER/EXIT przez porównanie aktualnego i poprzedniego stanu rozmieszczenia w `ConcurrentHashMap<String, Set<String>>`. |

### 7.3 Pakiet `receiver`

`MeshtasticBroadcastReceiver` jako jedyna klasa. Wykorzystywana w dwóch miejscach: w `MeshServiceRepository` (warstwa danych) oraz w `ZoneMonitorService` (działanie w tle).

### 7.4 Pakiet `repository`

| Klasa | Funkcja |
|---|---|
| `MeshRepository` (interface) | Abstrakcja warstwy danych Meshtastic. |
| `MeshServiceRepository` (`@Singleton`) | Produkcyjna implementacja agregująca `MeshServiceManager` i `BroadcastReceiver`. |
| `ZoneRepository` (`@Singleton`) | Fasada przed `ZoneDao`. Eksponuje `allZones: Flow<List<Zone>>`, `getEventsForZone(zoneId)`, operacje CRUD. |
| `PositionHistoryRepository` (`@Singleton`) | Historia pozycji w pamięci (StateFlow). Filtrowanie po minimalnej odległości; ograniczenie do `maxPoints`. |
| `PacketStatsRepository` (`@Singleton`) | Bieżące statystyki pakietów. Akumuluje historię delta-T z odrzucaniem outlierów (`< 5 s` lub `> 600 s`). |

### 7.5 Pakiet `logic`

`GeofenceChecker` — algorytm ray-casting w czystym Kotlinie. Trzy publiczne funkcje: `contains(pointLat, pointLon, polygon)`, `nodeInZone(node, zone)`, `computeNodeZoneMap(nodes, activeZones)`.

### 7.6 Pakiet `data`

| Klasa | Funkcja |
|---|---|
| `ZoneDatabase` | Klasa Room, encje `Zone` i `ZoneEvent`, version 1. |
| `ZoneDao` | Zapytania SQL: pobieranie wszystkich/aktywnych stref, zdarzeń dla strefy, CRUD. |
| `AppPreferences` | Wrapper na DataStore Preferences. 9 kluczy konfiguracyjnych: interwał odświeżania, próg online, domyślny filtr online, domyślny filtr GPS, typ mapy, maksymalna liczba punktów historii, minimalna odległość historii, pokazywanie wszystkich tras, oczekiwany interwał broadcastów. |

### 7.7 Pakiet `util`

| Klasa | Funkcja |
|---|---|
| `Constants` | Stałe broadcastów Meshtastic. |
| `NotificationChannels` | Tworzenie dwóch kanałów powiadomień, idempotentnie. |
| `CsvExporter` | Eksport telemetrii do pliku CSV w katalogu Downloads (MediaStore na API 29+). |

### 7.8 Pakiet `di`

`AppModule` (bind `MeshRepository` → `MeshServiceRepository`, provide `MeshServiceManager`) oraz `DatabaseModule` (provide `ZoneDatabase` i `ZoneDao`).

### 7.9 Pakiet `ui`

Hierarchia ekranów:
- `MainScreen` — kontener nawigacji.
- `MapScreen` (+ `MapViewModel`) — mapa, rysowanie stref, markery, ścieżki.
- `NodeListScreen`, `NodeItem`, `NodeDetailScreen` (+ `NodeFilterState`) — lista i szczegóły węzła.
- `SettingsScreen` (+ `SettingsViewModel`) — preferencje.
- `ZoneDetailScreen`, `ZoneBottomSheet`, `ZoneConfirmDialog` (+ `ZoneViewModel`) — strefy.
- `components/ConnectionStatusBar` — pasek statusu połączenia.
- `theme/*` — Material 3 (kolory, typografia, motyw).

---

## 8. Ocena rozwiązania

### 8.1 Stopień realizacji wymagań

Tabela zestawia wymagania z sekcji 3.2–3.3 z ich realizacją w kodzie:

| Wymaganie | Status | Komponent realizujący |
|---|---|---|
| F1.1. Połączenie z Meshtastic | ✅ | `MeshServiceManager.connect`, `MeshServiceRepository` |
| F1.2. Mapa węzłów | ✅ | `MapScreen` + `NodeToMarkerMapper` |
| F1.3. Lista węzłów | ✅ | `NodeListScreen`, `NodeItem` |
| F1.4. Aktualizacje w czasie rzeczywistym | ✅ | `MeshtasticBroadcastReceiver` + `StateFlow` |
| F1.5. Status połączenia | ✅ | `ConnectionStatusBar` + `MapViewModel.ConnectionState` |
| F2.1. Historia pozycji | ✅ | `PositionHistoryRepository` |
| F2.2. Statystyki pakietów (PDR) | ✅ | `PacketStatsRepository` |
| F2.3. Eksport CSV | ✅ | `CsvExporter` |
| F2.4. Konfiguracja | ✅ | `AppPreferences` + `SettingsScreen` |
| F3.1. Tworzenie stref | ✅ | `ZoneViewModel.DrawingState` + `MapScreen` long-press |
| F3.2. Trwałość stref | ✅ | Room (`ZoneDatabase`) |
| F3.3. Przypisanie węzłów do strefy | ✅ | `Zone.watchedNodeIdsJson` |
| F3.4. Detekcja ENTER/EXIT | ✅ | `ZoneMonitorService.checkNodeAgainstZones` |
| F3.5. Powiadomienia + dziennik | ✅ | `NotificationChannels` + `ZoneEvent` |
| F3.6. Monitoring w tle | ✅ | `ZoneMonitorService` (foreground service) |
| N1. Android 7.0+ | ✅ | `minSdk = 24` |
| N2. Praca bez Internetu | Częściowo | Praca działa bez sieci komórkowej, ale kafelki Google Maps wymagają WiFi/danych |
| N3. Deterministyczny algorytm geofencingu | ✅ | `GeofenceChecker` (ray-casting) |
| N4. Odporność na zmianę API Meshtastic | ✅ | Reflection z wieloma fallbackami |
| N5. Testowalność | Częściowo | `GeofenceChecker` testowalny w JVM; warstwa repozytorium gotowa do podmiany przez Hilt; pełnego pokrycia testowego brak |

### 8.2 Procedura weryfikacji

**Testy funkcjonalne (manualne, opisane w `docs/TESTY_FUNKCJONALNE.md`):**
1. Test połączenia z Meshtastic (T1) — uruchomienie aplikacji bez i z zainstalowaną Meshtastic; oczekiwana sygnalizacja stanu.
2. Test wyświetlania węzłów (T2) — przy podłączonym radiu pojawiają się markery i pozycje na liście.
3. Test odświeżania (T3) — zmiana pozycji w czasie rzeczywistym po przemieszczeniu węzła.
4. Test stref (T4) — utworzenie strefy, przypisanie węzła, weryfikacja ENTER/EXIT.
5. Test trwałości (T5) — restart aplikacji i potwierdzenie zachowania stref.
6. Test pracy w tle (T6) — wyjście z aplikacji i wymuszone naruszenie strefy — oczekiwane powiadomienie.

**Testy jednostkowe:**
- `GeofenceCheckerTest` — przypadki: punkt wewnątrz, punkt na zewnątrz, punkt na krawędzi, wielokąt o niewystarczającej liczbie wierzchołków, wielokąt wklęsły.
- (Planowane) testy `MeshNodeInfo.fromMeshtasticNodeInfo` z mockowanymi obiektami.

**Testy w warunkach polowych:**
Faza 1 — dwa węzły umieszczone w pomieszczeniach o znanej pozycji; weryfikacja, że MeshTracker odbiera pozycje. Faza 2 — pełna konfiguracja terenowa (T-Beam jako dom, T-1000-E zamocowany na obroży), weryfikacja zasięgu i niezawodności w terenie leśno-polnym.

### 8.3 Porównanie z systemami komercyjnymi

| Cecha | Dogtra Pathfinder 2 | Dogtrace DOG GPS X25 | MeshTracker (praca) |
|---|---|---|---|
| Zasięg | ~14,5 km | ~20 km | zależny od konfiguracji LoRa, ok. 5–10 km w terenie |
| Liczba węzłów | Do 21 | Do 19 | Bez ograniczenia (LoRa mesh) |
| Czas pracy nadajnika | ~3 dni | >40 godz. | T-1000-E: 10–24 godz. zależnie od interwału |
| Cena zestawu | ~3500 PLN | ~2500 PLN | Sprzęt: ~300 PLN/węzeł, software: gratis |
| Otwartość | Zamknięte | Zamknięte | Pełna (LoRa + Meshtastic OSS + własna aplikacja) |
| Strefy bezpieczeństwa | Tak (3 rodzaje) | Akustyczna (okrąg) | Tak (wielokąty) |
| Powiadomienia push | Tak | Sygnał dźwiękowy | Tak (Android notifications) |
| Modyfikowalność | Brak | Brak | Pełna — kod źródłowy w pracy |

Pod względem absolutnych parametrów (zasięg, czas pracy) systemy komercyjne wygrywają, jako produkty zoptymalizowane sprzętowo. MeshTracker konkuruje otwartością, niższym kosztem i modyfikowalnością — co odpowiada postawionym wymaganiom.

### 8.4 Trudności napotkane w trakcie realizacji

1. **Deserializacja `Parcelable` z innego pakietu** — początkowo skutkowała `ClassNotFoundException`. Rozwiązanie opisane w sekcji 5.4 — łańcuch ClassLoaderów — kosztowało kilka dni i wymagało zrozumienia mechanizmu klas izolowanych w Androidzie.
2. **Format pozycji E7 vs degrees** — różne wersje Meshtastic dostarczają współrzędne w różnych formatach (Int × 10⁷ lub Double w stopniach). Detekcja heurystyczna (`Math.abs > 1000`) okazała się robustna w testach.
3. **Race condition stanu połączenia** — broadcast `NODE_CHANGE` mógł nadejść zanim ViewModel uzyskał potwierdzenie z `connectionState()`. Rozwiązanie: zakładać że radio jest aktywne, gdy wpłynie pierwszy pakiet w stanie Connecting/Disconnected.
4. **Foreground Service typu dataSync** — wymagania API 29+ co do typu serwisu i powiadomienia wymagały oddzielnej obsługi compat.
5. **Decyzja okrąg → wielokąt** — wczesna decyzja o strefach kołowych okazała się nieoptymalna dla realnego kształtu działki; przepisanie wymagało zmiany schematu Room (`radius` → `verticesJson`) i implementacji ray-casting.

---

## 9. Krytyczna refleksja i kierunki kontynuacji

### 9.1 Co by autor zrobił inaczej

**Wczesne zdefiniowanie warstwy abstrakcji danych.** Pierwsza wersja `MapViewModel` zależała bezpośrednio od `MeshServiceManager` i ręcznie zarządzała rejestracją receivera. Wprowadzenie interfejsu `MeshRepository` przyszło dopiero w drugiej iteracji, wymuszając refaktoryzację. Gdyby `MeshRepository` powstał równolegle z pierwszą wersją, oszczędziłoby to dwóch dni pracy i dało od początku testowalność.

**Wcześniejsze pisanie testów dla parsowania.** Logika `fromMeshtasticNodeInfo` jest najbardziej krucha — opiera się o reflection na klasach zewnętrznej aplikacji. Brak testów jednostkowych z mockami spowodował, że błędy parsowania były wykrywane dopiero w urządzeniu z urządzeniem Meshtastic. Pisanie testów z mockowanymi obiektami (od początku) skróciłoby pętlę feedbackową.

**Wybór wielokątów od początku.** Praca rozpoczęta od stref kołowych musiała być częściowo przepisana. Decyzja podjęta wcześniej zaoszczędziłaby etap migracji schematu Room.

**Bardziej selektywne logowanie.** Modele `MeshNodeInfo`, `MeshPosition` i `MeshUserInfo` zawierają logi `Log.d` dla każdej próby reflection. W trakcie debugowania nieocenione, w wersji produkcyjnej znacząco zwiększają objętość Logcata. Warto wprowadzić poziom logu warunkowany od `BuildConfig.DEBUG`.

### 9.2 Znane ograniczenia

- **Brak testów jednostkowych dla reflection-heavy ścieżek.** Krytyczne klasy parsujące dane z Meshtastic nie mają testów. Plan: napisać testy z fakeami klas `org.meshtastic.core.model.NodeInfo` (przygotowanymi przez `Mockito.mock` lub ręcznie napisanymi w testach).
- **Brak debouncingu broadcastów.** Każda zmiana węzła powoduje pełną aktualizację stanu i potencjalnie nieekonomiczne przeliczanie kombinacji `nodesInZones`. Przy gęstej sieci mesh może to wpływać na zużycie energii i baterii telefonu.
- **Brak migracji Room.** `exportSchema = false` jest akceptowalne we wczesnej fazie, ale przed dystrybucją trzeba wprowadzić strategię migracji (eksport schematu, klasy `Migration`).
- **Pojedynczy `MapViewModel` z dużą liczbą odpowiedzialności.** Refaktoryzacja na osobne `ConnectionViewModel`, `NodeViewModel` i `ExportViewModel` ułatwi testowanie i zmniejszy ryzyko regresji.
- **Brak własnej obsługi map offline.** Mapy Google wymagają dostępu do Internetu na chwilę pobrania kafelków. Alternatywą byłaby integracja `osmdroid` z preloadowanymi kafelkami OpenStreetMap.

### 9.3 Kierunki rozwoju

1. **Migracja na Maps SDK Maps Compose v6** lub na alternatywę OSM dla działania w pełni offline.
2. **Wymuszenie pozycji (`requestPosition`)** — wywołanie metody przez reflection, by w sytuacji podejrzenia naruszenia strefy poprosić węzeł o natychmiastową aktualizację (zaplanowane w `docs/ARCHITEKTURA.md`).
3. **Integracja z ATAK** — przekształcenie aplikacji w plugin systemu Team Awareness Kit, używany m.in. przez służby ratownicze. Referencyjna implementacja CoT (Cursor on Target) Events: `docs/PRZYKLADOWY_KOD.java`.
4. **Przeniesienie detekcji geofencingu na węzeł stacji bazowej** — odciążenie telefonu poprzez wykonywanie testu strefy bezpośrednio na T-Beam (wymaga modyfikacji firmware'u Meshtastic lub odrębnego modułu).
5. **Wsparcie wielu profili (właścicieli/psów)** w jednej aplikacji.
6. **Statystyki i raporty** — agregacje czasowe (czas spędzony poza strefą, dystanse, mapy ciepła ruchu).
7. **Tryb edycji wielokąta** — możliwość edycji wierzchołków istniejącej strefy zamiast tworzenia nowej.

### 9.4 Podsumowanie

Wdrożony system spełnia wszystkie wymagania funkcjonalne pierwszej fazy projektu inżynierskiego i większość drugiej oraz trzeciej. Mocną stroną jest spójna architektura warstwowa, jasno wyodrębniona logika biznesowa (testowalna w JVM) oraz mechanizm reflection odporny na zmiany API Meshtastic. Słabością jest jeszcze niedostateczne pokrycie testowe, brak pełnej obsługi map offline i pojedynczy duży ViewModel.

Projekt pokazuje, że tania, otwarta sieć LoRa z gotową platformą Meshtastic może z powodzeniem zastąpić znacznie droższe systemy komercyjne w zastosowaniu śledzenia obiektów ruchomych, jeśli akceptowalna jest praca przy nieco mniejszym zasięgu i krótszym czasie pracy na baterii. Implementacja dostarcza również szablonu integracji aplikacji Android z zewnętrzną aplikacją systemową poprzez bound service, AIDL i broadcasty — wzorzec, który można przenieść na inne integracje (np. z aplikacjami pomiarowymi czy magazynowymi).

---

## Bibliografia (szkic)

1. Kraśniewski A., *Jak pisać pracę dyplomową?*, prezentacja kursu Techniki Prezentacji, WETI Politechnika Warszawska, 2020.
2. Meshtastic Project, *Documentation*, `https://meshtastic.org/docs/`, dostęp: 2025.
3. Meshtastic Project, *Protocol Buffers definitions*, `https://github.com/meshtastic/protobufs`.
4. Semtech Corporation, *AN1200.22 LoRa Modulation Basics*, 2015.
5. Google, *Android Developers Guide — Bound Services and AIDL*, `https://developer.android.com/guide/components/aidl`.
6. Google, *Jetpack Compose Documentation*, `https://developer.android.com/jetpack/compose`.
7. Google, *Hilt — Dependency Injection for Android*, `https://dagger.dev/hilt/`.
8. Sutherland I. E., Hodgman G. W., *Reentrant Polygon Clipping*, Communications of the ACM, 17(1), 1974 — podstawy algorytmów geometrii obliczeniowej.
9. Hormann K., Agathos A., *The point in polygon problem for arbitrary polygons*, Computational Geometry, 20(3), 2001 — analiza wariantów algorytmu ray-casting.
10. Dogtra Inc., *Pathfinder 2 User Manual*, 2023.
11. Dogtrace s.r.o., *DOG GPS X25 — strona produktu*, `https://www.obroza-elektryczna.pl/dogtrace-dog-gps-x25`.
12. LILYGO, *T-Beam Hardware Documentation*, `https://www.lilygo.cc/`.
13. Seeed Studio, *T-1000-E Hardware Documentation*, `https://www.seeedstudio.com/`.

---

*Wersja dokumentu: 1.0 · Data: 2026 · Bazuje na stanie kodu w gałęzi głównej projektu MeshTracker_v1.*
