# Inzynierka

## Wprowadzienie

### Kontakst problemu
Autor pracy w okresie pisanie poniższej pracy inżynierskiej spędzał dużą część lata na rodzinnej działce na Mazurach Zachodnich. Zabierał tam komputer do pracy oraz swojego psa imieniem Reja rasy Mały Gończy Gaskoński. Myśliwski charakter psa wraz z jego aktywnyi łowczym inkstnktem oraz nieograniczona przestrzeń polnej działki w pobliżu terenów bogatych w dziką zwierzynę, stworzyły różnicę potencjałów, która owocowała licznymi ucieczkami Rei, wieloma zjawiskowymi pościgami za sarnami, koziołkami i łosiami. Ogrodzenie działki nie wchodziło w grę, z wielu nieistotnych dla kontekstu przyczyn. Trening odwołania mógł zadziałać, ale nie dawał pewności, bo w momencie zrywu za zwierzyną Reja zapominała o całym świecie i było tylko kwestią czasu kiedy zagalopuje się za daleko, żeby wrócić bezpiecznie do domu. Problem stworzony przez próbę zamieszkania w terenie "łownym" z psem myśliwskim wymagał rozwiązania technicznego. Takie uwarunkowanie dostarczyło założeń pod implementację poniższego projektu.

### Cel projektu

Celem projektu jest opracowanie prostego systemu monitorowania położenia zwierząt w czasie rzeczywistym, zwiększającego ich bezpieczeństwo w otwartych przestrzeniach. System wykorzystuje moduły LoRa i sieć Meshtastic do przesyłania danych na duże odległości bez potrzeby korzystania z sieci GSM. Dane z urządzeń lokalizacyjnych będą odbierane przez stację bazową i prezentowane w aplikacji mobilnej, umożliwiającej wizualizację położenia zwierząt na mapie, definiowanie wirtualnych stref bezpieczeństwa oraz natychmiastowe powiadamianie o ich opuszczeniu. Projekt obejmuje dobór i konfigurację sprzętu, implementację komunikacji w sieci Meshtastic, stworzenie aplikacji do wizualizacji danych oraz testy terenowe systemu.

### Zakres pracy
#### Wymagania
 - Węzeł sieci systemu spełnia następujące wymagania:
    - Możliwość komunikacji przez Bluetooth
    - Wbudowany moduł GPS
    - Wodoszczelność i pyłoszczelność
    - Odporność na uszkodzenia mechaniczne powodowane ciągłym użytkowaniem w środowisku leśnym
    - Rozmiar i kształt odpowiedni do umieszczenia na obroży
    - Akumulator o pojemności wystarczającej na funkcjonowanie min. przez 10 godzin.
    - Możliwość wgrania oprogramowania

- Aplikacja oferuje następujące funkcjonalności:
    - **Połączenie z siecią Meshtastic**: Automatyczne łączenie z aplikacją Meshtastic poprzez interfejs AIDL (Android Interface Definition Language)
    - **Wyświetlanie pozycji węzłów**: Wizualizacja wszystkich węzłów z prawidłową pozycją GPS na mapie Google Maps
    - **Lista węzłów**: Przeglądanie wszystkich wykrytych węzłów wraz z ich statusem, parametrami sygnału i informacjami o baterii
    - **Aktualizacje w czasie rzeczywistym**: Automatyczne odświeżanie pozycji węzłów poprzez nasłuchiwanie broadcastów systemowych
    - **Status połączenia**: Wizualna informacja o stanie połączenia z radiem Meshtastic

### Analiza istniejących rozwiązań
Środowisko myśliwskie zna wiele rozwiązań na utrzymywanie psów w ściśle zdefiniowanych obszarach. Rozważając jedynie elektroniczne rozwiązania przedstawię tutaj jedynie te najlepiej dopracowane. 

1. Dogtra Pathfinder 2
Jest to jeden z najbardziej zaawansowanych produktów z tej dziedziny. Zestaw składa się z odbiornika, który łączy się z telefonem przez bluetooth, oraz jednego lub więcej (maksymalnie 21) nadajników, które są umieszczone na obrożach elektronicznych. Urządzenia łączą się ze sobą po falach radiowych. (Nie udało mi się uzyskać bardziej szczegółowych informacji). Odbiornik zarówno przekazuje dane z nadajników, ale też daje możliwość użycia przycisku. Może on być zaprogarmowany do wzbudzania impulsów elektrycznych w obroży, emitowania światła, dźwięku albo wibracji w nadajnikach. Dodatkowo aplikacja PATHFINDER2 app, która łączy się przez Bluetooth z odbiornikiem służy do wyświetlania dokładnej pozycji wszystkich skonfigurowanych nadajników, podglądania ich stanu ( ikony psów zmieniają się w zaleźności od ich aktualnego zajęcia np. szczekanie, wskazywanie zwierzyny, przycinanie drzew, bieganie, czy nawet osaczanie "wieprza" ), naładowania baterii nadajników, aż do statusu połączenia z GPS. Pozatym aplikacja umożliwia nagrywanie lokalizacji w sesje do późniejszego odtworzenia, nawigację na Mapach Google, pomiary odległości, sterowanie modułami na nadajnikach (wibracja, dźwięk, światło, impul elektryczny). 
Ciekawą funkcją jest możliwość definiowania różnego rodzaju "E-Ogrodzeń". Firma Dogtra opracawało trzy rodzaje ogrodzeń:

   1. Mobile-Fence: Ogrodzenie, które porusza się w oparciu o lokalizację smartfona i wysyła powiadomienia. 
   2. Geo-Fence: Konfiguracja statycznego ogrodzenia, które wysyła powiadomienia. 
   3. E-Fence: Statyczna konfiguracja ogrodzenia, która wysyła psu automatyczną korektę i powiadomienie.
   " chrome-extension://efaidnbmnnnibpcajpcglclefindmkaj/https://i00.eu/file/403/15309-manualplpathfinder2.pdf strona 26.

Producent deklaruje następujące parametry pracy:
- zasięg do 9 mil (14,484 m)
- aktualizajce pozycji co 2 sekundy

- wagę nadajnika 
- użytkowania bez dodatkowych opłat za abonamenty

2. Dogtrace DOG GPS X25
Jedynie nieznacznie różnym podejściem wykazała się firma DOGTRACE, która w swoim rozwiązaniu zdecydowała się zrezygnować z konieczności użycia smartfona. X25 składa się z identycznego funkjonalnie nadajnika oraz znacznie rozbudowanego odbiornika, wielkości mniejszego smartfona, który na wbudowanym wyświetlaczu LCD pokazuje kierunek i odległość do nadajnika. Czeska firma udostępniła lepszej jakości informację o funkcjonowaniu swojego produktu. Juz samej oferty na stronie internetowej dowiadujemy się, że:
- Mamy możlowość śledzenia do 19 psów 
- Bateria nadajnika i odbiornika powinna wystarczyć na ponad 40 godzin pracy (Li-Pol 1900 mAh)
- Pozycjonowanie modułów zapewnia skorzystanie z kilku systemów: GPS, GALILEO, GLONASS, a nadajnik z odbiornikiem łączą sie po LoRa.
- Deklarowany zasięg to 20 km dla komunikacji "w lini wzroku"
- Pełna zanurzalność nadajnika i odbiornika

- Dodatkowo dostępne są funkcje takie jak :
"""
    - KOMPAS - kierunek na północ magnetyczną
    - BEEPER - wykrywanie ruchu lub bezruchu psa
    - FENCE - okrągły płot - akustyczna granica wyznaczająca przestrzeń dla psa
    - WAYPOINT - możliwość zapisania 4 współrzędnych gps odbiornika - nawigacja do tych punktów
    - CAR mode - tryb umożliwiający korzystanie z odbiornika (urządzenia ręcznego) w pojeździe
    - Sygnał dźwiękowy - zastępuje gwizdek, do wyboru 3 różne dźwięki """ cytat z https://www.obroza-elektryczna.pl/dogtrace-dog-gps-x25

### Przegląd dostępnych narzędzi i technologii

Wybór odpowiednich technologii i narzędzi jest kluczowy dla realizacji projektu systemu monitorowania położenia zwierząt. Poniżej przedstawiono analizę dostępnych rozwiązań w poszczególnych obszarach funkcjonalnych systemu.

#### Technologie komunikacji bezprzewodowej

System wymaga komunikacji na duże odległości bez konieczności korzystania z infrastruktury sieci komórkowej. Rozważono następujące opcje:

**LoRa (Long Range)**
LoRa to modulacja radiowa wykorzystująca technikę chirp spread spectrum, opracowana przez firmę Semtech. Działa w pasmach ISM (868 MHz w Europie, 915 MHz w Ameryce Północnej), dostępnych bez licencji. Główne zalety to:
- Długi zasięg: w warunkach optymalnych do kilkudziesięciu kilometrów, rekordowo do 331 km
- Niskie zużycie energii: umożliwia długą pracę na baterii
- Odporność na zakłócenia: dobra propagacja w terenie otwartym i leśnym
- Brak konieczności infrastruktury: komunikacja peer-to-peer

Główne ograniczenia:
- Niska przepustowość: ograniczona do krótkich wiadomości
- Zależność od topografii: zasięg zależy od ukształtowania terenu
- Czas transmisji: wyższe współczynniki rozpraszania zwiększają czas transmisji

**Alternatywne technologie komunikacji**

*Bluetooth* oferuje zasięg zaledwie kilkudziesięciu metrów, co jest niewystarczające dla zastosowań terenowych. *WiFi* wymaga infrastruktury punktów dostępowych i charakteryzuje się wysokim zużyciem energii. *Sieci komórkowe (GSM/LTE)* zapewniają globalny zasięg, ale wymagają infrastruktury operatorów, abonamentów oraz mogą być zawodne w obszarach wiejskich. *Radioamatorstwo* wymaga licencji i specjalnych uprawnień, co ogranicza dostępność.

**Uzasadnienie wyboru:** LoRa została wybrana jako optymalne rozwiązanie, łączące długi zasięg, niskie zużycie energii oraz niezależność od infrastruktury zewnętrznej, co jest kluczowe dla zastosowań terenowych.

#### Platformy sprzętowe i sieć Meshtastic

**Meshtastic** to projekt open source wykorzystujący radia LoRa do tworzenia zdecentralizowanej sieci mesh. Główne zalety:
- Architektura mesh: automatyczna retransmisja wiadomości przez węzły pośrednie
- Brak infrastruktury centralnej: każdy węzeł może pełnić rolę przekaźnika
- Odporność na awarie: awaria pojedynczego węzła nie przerywa komunikacji
- Otwarty kod źródłowy: możliwość modyfikacji i audytu bezpieczeństwa
- Wbudowane szyfrowanie: ochrona prywatności komunikacji

**Dostępne platformy sprzętowe:**

*T-Beam* - popularna platforma oparta na ESP32 z wbudowanym modułem GPS, anteną LoRa oraz akumulatorem. Charakteryzuje się dobrym stosunkiem ceny do wydajności oraz szerokim wsparciem społeczności.

*T-2000-E* - kompaktowe urządzenie zaprojektowane specjalnie dla Meshtastic, oferujące wodoszczelność (IP67), wbudowany GPS oraz zoptymalizowaną żywotność baterii. Wymiary i kształt są odpowiednie do umieszczenia na obroży.

**Uzasadnienie wyboru:** Meshtastic został wybrany jako gotowe rozwiązanie sieciowe, eliminujące konieczność implementacji protokołów komunikacji od podstaw. Platformy T-Beam i T-2000-E oferują kompletne rozwiązanie sprzętowe z wbudowanym GPS, co upraszcza integrację.

**Alternatywy dla Meshtastic:**

Rozważono również inne rozwiązania sieciowe oparte na LoRa:

*MeshCore* - wieloplatformowy system komunikacji tekstowej wykorzystujący LoRa. Główne różnice w stosunku do Meshtastic:
- Nie wysyła telemetrii bez potrzeby, co może zwiększać efektywność wykorzystania kanału radiowego
- Skupia się wyłącznie na komunikacji tekstowej, bez dodatkowych funkcji
- Mniejsza społeczność użytkowników w porównaniu do Meshtastic

*Reticulum* - projekt oferujący komunikację mesh z wykorzystaniem LoRa, skupiający się na prywatności i bezpieczeństwie danych. Charakteryzuje się:
- Zaawansowanymi mechanizmami szyfrowania
- Możliwością pracy w różnych mediach (nie tylko LoRa)
- Bardziej złożoną konfiguracją w porównaniu do Meshtastic

*LoRaWAN* - protokół warstwy sieciowej dla LoRa, wykorzystujący architekturę gwiazdy z bramkami (gateways) zamiast mesh:
- Wymaga infrastruktury bramek LoRaWAN
- Lepsza skalowalność dla dużych sieci IoT
- Wymaga rejestracji w sieci operatora lub własnej infrastruktury
- Nie nadaje się do zastosowań peer-to-peer bez infrastruktury

**Uzasadnienie wyboru Meshtastic:** W porównaniu do alternatyw, Meshtastic oferuje najlepsze połączenie łatwości użycia, wsparcia społeczności, gotowych rozwiązań sprzętowych oraz funkcjonalności potrzebnych dla projektu (lokalizacja GPS, komunikacja mesh bez infrastruktury).

#### Alternatywne platformy sprzętowe

Rozważono możliwość wykorzystania innych platform sprzętowych z modułami LoRa do budowy własnego rozwiązania:

**Raspberry Pi z modułami LoRa**

Raspberry Pi może być wyposażone w nakładki (HAT) z modułami LoRa, takie jak SX1268 LoRa HAT:
- **Zalety:**
  - Wysoka moc obliczeniowa: możliwość uruchomienia pełnego systemu operacyjnego Linux
  - Elastyczność: łatwa integracja z różnymi modułami (GPS, czujniki)
  - Bogate wsparcie: szeroka gama bibliotek i narzędzi
  - Możliwość implementacji własnych protokołów komunikacji
- **Wady:**
  - Wysokie zużycie energii: typowo 1-3W, co wymaga dużych akumulatorów
  - Duże rozmiary: Raspberry Pi jest zbyt duże do umieszczenia na obroży psa
  - Wymaga systemu operacyjnego: większe opóźnienia startu
  - Wyższy koszt: zarówno samego urządzenia, jak i akumulatora o odpowiedniej pojemności
  - Brak wodoszczelności: wymaga dodatkowej obudowy

**Raspberry Pi Zero W** - mniejsza wersja Raspberry Pi:
- Mniejsze zużycie energii (ok. 0.5-1W), ale nadal wysokie dla zastosowań bateryjnych
- Wymiary nadal zbyt duże dla obroży
- Ograniczona moc obliczeniowa w porównaniu do pełnego Raspberry Pi

**Arduino z modułami LoRa**

Platforma Arduino oferuje kilka opcji integracji z LoRa:

*Arduino MKR WAN 1300* - płytka z wbudowanym modułem LoRa:
- **Zalety:**
  - Niskie zużycie energii: typowo 20-50mA w trybie aktywnym
  - Prosta integracja: wbudowany moduł LoRa
  - Niski koszt: tańsze niż Raspberry Pi
  - Łatwe programowanie: środowisko Arduino IDE
- **Wady:**
  - Brak wbudowanego GPS: wymaga dodatkowego modułu
  - Ograniczona moc obliczeniowa: mikrokontroler ARM Cortex-M0+
  - Wymaga implementacji własnych protokołów komunikacji
  - Brak gotowego rozwiązania sieciowego (jak Meshtastic)

*Arduino Uno/Nano z modułem LoRa* (np. SX1278, SX1262):
- **Zalety:**
  - Bardzo niski koszt: najtańsza opcja
  - Prosta integracja: moduły LoRa dostępne jako shieldy
  - Niskie zużycie energii
- **Wady:**
  - Wymaga dodatkowych modułów: GPS, akumulator, obudowa
  - Ograniczona funkcjonalność: brak WiFi/Bluetooth
  - Wymaga kompleksowej implementacji od podstaw

**ESP32 z modułami LoRa**

ESP32 to popularna platforma łącząca mikrokontroler z WiFi i Bluetooth:

*Heltec ESP32 LoRa* - płytki z wbudowanym modułem LoRa i wyświetlaczem OLED:
- **Zalety:**
  - Wbudowany WiFi i Bluetooth: możliwość komunikacji z telefonem
  - Dobra moc obliczeniowa: dual-core procesor
  - Niskie zużycie energii: tryby oszczędzania energii
  - Niski koszt
- **Wady:**
  - Brak wbudowanego GPS: wymaga dodatkowego modułu
  - Wymaga implementacji protokołów komunikacji
  - Ograniczona wodoszczelność: wymaga obudowy

*TTGO T-Beam* - platforma oparta na ESP32 z LoRa i GPS:
- **Zalety:**
  - Kompletne rozwiązanie: ESP32 + LoRa + GPS + akumulator
  - Wsparcie Meshtastic: oficjalnie wspierana platforma
  - Dobry stosunek ceny do funkcjonalności
- **Wady:**
  - Brak wodoszczelności: wymaga dodatkowej obudowy
  - Większe rozmiary niż dedykowane rozwiązania (np. T-2000-E)

**Porównanie platform dla zastosowania na obroży:**

| Platforma | Zużycie energii | Rozmiar | GPS | Wodoszczelność | Gotowe oprogramowanie | Koszt |
|-----------|----------------|---------|-----|----------------|----------------------|-------|
| T-2000-E | Bardzo niskie | Mały | Tak | IP67 | Meshtastic | Średni |
| T-Beam | Niskie | Średni | Tak | Nie | Meshtastic | Niski |
| Raspberry Pi | Wysokie | Duży | Moduł | Nie | Własne | Wysoki |
| Arduino MKR | Niskie | Średni | Moduł | Nie | Własne | Niski |
| ESP32 LoRa | Niskie | Średni | Moduł | Nie | Własne/Meshtastic | Niski |

**Uzasadnienie wyboru T-Beam/T-2000-E:**

Dla zastosowania na obroży psa, kluczowe są następujące wymagania:
1. **Niskie zużycie energii** - konieczne dla długiej pracy na baterii (min. 10 godzin)
2. **Małe rozmiary i waga** - urządzenie musi być wygodne dla psa
3. **Wodoszczelność** - pies będzie przebywał w różnych warunkach atmosferycznych
4. **Gotowe oprogramowanie** - skraca czas implementacji i zapewnia stabilność
5. **Wbudowany GPS** - upraszcza integrację

T-2000-E i T-Beam spełniają wszystkie te wymagania, podczas gdy Raspberry Pi jest zbyt duże i energochłonne, a Arduino/ESP32 wymagają dodatkowych modułów i implementacji protokołów od podstaw. Meshtastic zapewnia gotowe, przetestowane rozwiązanie sieciowe, co znacznie skraca czas rozwoju projektu.

#### Technologie lokalizacji

**GPS (Global Positioning System)** to satelitarny system nawigacyjny zapewniający globalne pokrycie. Współczesne moduły GPS obsługują również:
- **GLONASS** (rosyjski system)
- **Galileo** (europejski system)
- **BeiDou** (chiński system)

Wielosystemowe pozycjonowanie (GNSS - Global Navigation Satellite System) zwiększa dokładność i niezawodność, szczególnie w trudnych warunkach (zarośla, lasy).

**Alternatywne technologie lokalizacji:**

*A-GPS* (Assisted GPS) wymaga połączenia z siecią komórkową, co nie jest dostępne w obszarach wiejskich. *WiFi positioning* i *Bluetooth beacons* wymagają infrastruktury i mają ograniczony zasięg. *Lokalizacja radiowa* (triangulacja sygnału LoRa) może być uzupełnieniem, ale ma niższą dokładność niż GPS.


**Uzasadnienie wyboru:** GPS/GNSS został wybrany jako standardowa technologia lokalizacji, oferująca globalne pokrycie, wysoką dokładność (zwykle 3-5 metrów) oraz niezależność od infrastruktury naziemnej.

#### Platforma mobilna i narzędzia programistyczne

**Android** został wybrany jako platforma docelowa aplikacji mobilnej. Główne zalety:
- Dominujący udział w rynku urządzeń mobilnych
- Otwartość platformy: możliwość dystrybucji poza oficjalnym sklepem
- Bogate API: dostęp do funkcji systemowych, map, Bluetooth
- Integracja z Meshtastic: aplikacja Meshtastic jest dostępna na Android

**Język programowania: Kotlin**

Kotlin został wybrany jako język programowania ze względu na:
- Oficjalne wsparcie Google dla Androida
- Interoperacyjność z Javą: możliwość wykorzystania istniejących bibliotek
- Zwięzła składnia: zwiększa produktywność programistyczną
- Bezpieczeństwo typów: redukuje błędy w czasie kompilacji
- Wsparcie dla programowania asynchronicznego: Coroutines

**Framework UI: Jetpack Compose**

Jetpack Compose to nowoczesny, deklaratywny framework do budowy interfejsu użytkownika:
- Deklaratywny sposób definiowania UI: kod bardziej czytelny i łatwiejszy w utrzymaniu
- Lepsza wydajność: w porównaniu do tradycyjnego XML
- Material Design 3: nowoczesny design system
- Integracja z innymi komponentami Android: ViewModel, LiveData, StateFlow

**Alternatywy:** Tradycyjny XML z Views oferuje większą dojrzałość i wsparcie, ale wymaga więcej kodu boilerplate. React Native czy Flutter umożliwiają multiplatformowość, ale wymagają dodatkowej warstwy abstrakcji i mogą mieć ograniczenia w dostępie do natywnych funkcji Android.

**Uzasadnienie wyboru:** Kotlin z Jetpack Compose został wybrany jako natywne rozwiązanie Android, oferujące najlepszą integrację z systemem, dostęp do wszystkich funkcji platformy oraz optymalną wydajność.

#### Biblioteki i narzędzia pomocnicze

**Google Maps for Android**

Google Maps zostało wybrane do wizualizacji map i markerów ze względu na:
- Wysoką jakość map: szczegółowe dane kartograficzne
- Szerokie wsparcie: dobrze udokumentowane API
- Integrację z Compose: biblioteka maps-compose
- Funkcje offline: możliwość pobrania map do użycia bez internetu
- Aktualizacje: regularne aktualizacje danych mapowych

**Android Architecture Components**

Wykorzystano komponenty architektury Android:
- **ViewModel**: zarządzanie danymi związanymi z UI, przetrwanie zmian konfiguracji
- **Lifecycle**: prawidłowe zarządzanie cyklem życia komponentów
- **StateFlow**: reaktywne strumienie danych dla aktualizacji UI w czasie rzeczywistym

**Kotlin Coroutines**

Coroutines umożliwiają asynchroniczne operacje bez blokowania głównego wątku:
- Asynchroniczne operacje sieciowe: komunikacja z Meshtastic
- Obsługa broadcastów systemowych: nasłuchiwanie aktualizacji z aplikacji Meshtastic
- Zarządzanie wątkami: uproszczona obsługa operacji równoległych

**Integracja z Meshtastic: AIDL**

Android Interface Definition Language (AIDL) został wykorzystany do komunikacji między procesami z aplikacją Meshtastic:
- Binding do serwisu Meshtastic: dostęp do danych węzłów sieci
- Broadcast Receivers: nasłuchiwanie aktualizacji pozycji węzłów
- Niezależność od implementacji: komunikacja przez standardowe interfejsy Android

#### Podsumowanie wyborów technologicznych

Zestaw wybranych technologii tworzy spójne rozwiązanie spełniające wymagania projektu:

1. **Komunikacja:** LoRa + Meshtastic zapewniają długi zasięg bez infrastruktury
2. **Lokalizacja:** GPS/GNSS oferuje globalne pokrycie i wysoką dokładność
3. **Platforma:** Android z Kotlin i Jetpack Compose zapewniają natywną integrację
4. **Wizualizacja:** Google Maps oferuje wysokiej jakości mapy i łatwą integrację
5. **Architektura:** MVVM z komponentami Android zapewnia separację warstw i łatwość utrzymania

Wybór technologii open source (Meshtastic, Android SDK) oraz standardowych rozwiązań (GPS, Google Maps) zapewnia dostępność dokumentacji, wsparcie społeczności oraz możliwość dalszego rozwoju projektu.

### Fazy projektu
Praca została podzielona na etapy, aby ułatwić organizację projektu oraz testów na każdym poziomie.

#### Faza 1
##### Sieć
- Zakup dwóch węzłów Meshtastic:
    - T-2000-E
    - T-Beam
- Koniguracja prostej sieci Meshtastic złożonej z dwóch węzłów
- Ustawienie przekazywania stałej pozycji przez węzły

##### Aplikacja
Wymagania fukcjonalne 
- Połącznie z węzłami sieci Meshtastic przez Bluetooth
- Wyświetlanie pozycji węzłów na mapie Google Maps
- Wyświetlanie listy węzłów wraz ze statusami i pozycją 
- Aktualizacja w czasie rzeczywistym
- Pokazywanie statusu połączenia z siecią Meshtastic

#### Faza 2
Celem jest stworzenie prototypu systemu, który będzie składał się z dwóch węzłów, którę będą miały przypisane role "Domu" i "Psa". Aplikacja łączy się przez Bluetooth z węzłem sieci "Dom", natomiast przez skonfigurowaną sieć Meshtastic między węzłami "Dom" i "Pies" wymieniana jest pozycja GPS z określonym interwałem. Po zakończeniu implementacji można wykonać pierwsze testy terenowe, aby sprawdzić system w warunkach polowych

##### Sieć 
- Konfiguracja ról na węzłach sieci
- Ustawienie przekazywania rzeczywistej pozycji węzłów przez sieć

##### Aplikacja
- Zróżnicowanie wyświetlania węzłów na mapie ze względu na przypisaną im rolę.
- Kominikacja z siecią Meshtastic w celu otrzymania pozycji od "zdalnych" węzłów ( niepołączonych bezpośrednio przez Bluetooth ).

#### Faza 3
Końcowa faza projektu skupia się na ułatwieniu korzystania z aplikacji. Zakłada możliwość dodania bezpiecznych stref na mapie w aplikacji. Jeżeli węzeł w aplikacji znajdzie się poza wyznaczoną bezpieczną strefą użytkownik powinien zostać powiadomiony.

#### Aplikacja
- Możliwość tworzenia, edycji i usuwania stref bezpiecznych na mapie w formie kół.
- Logika sprawdzająca incydenty przekroczenia bezpiecznej strefy.

#### Rozważania nad aplikacjąę
Rozważam przerzucenie odpowiedzialności obliczeniowej na "podręczny" węzeł Meshtastic, żeby ograniczyć zużycie energii w telefonie.
Możliwe jest też dodanie ekranu do tego węzła, żeby wyeliminować konieczność użycia telefonu. Można też użyć ekranu w telafonie w taki sposób, żeby na nim przebiegała konfiguracja, natomiast działanie w czasie rzeczywistym wykonywało się na telefonie.

#### Parsowanie pozycji w Meshtastic
https://github.com/meshtastic/protobufs/blob/master/meshtastic/mesh.proto

#### Poszukiwanie jak można wydobyć informacje o HEADING 
Na T-1000E jest tylko 3-osiowy akcelelometr (nie wykorzystywany). Dlatego nie ma jak osiągnąc z tego kierunku ustawienia karty. 