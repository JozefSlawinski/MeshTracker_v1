# Konfiguracja Sieci Meshtastic - Przesyłanie Pozycji Węzłów

## Wprowadzenie

Ten dokument wyjaśnia, jak skonfigurować sieć Meshtastic, aby wszystkie węzły mogły przesyłać między sobą informacje o swoim położeniu GPS. Przesyłanie pozycji jest kluczowe dla aplikacji MeshTracker, która wyświetla lokalizację wszystkich węzłów na mapie.

---

## Jak Działa Przesyłanie Pozycji w Meshtastic

### Podstawy

W sieci Meshtastic, informacje o pozycji GPS są przesyłane jako część **telemetrii** węzła. Telemetria jest automatycznie udostępniana wszystkim węzłom w sieci, które:

1. **Używają tego samego kanału PRIMARY** (lub kanału wtórnego z włączoną pozycją)
2. **Mają zgodne ustawienia LoRa** (częstotliwość, modem preset)
3. **Są w zasięgu** (bezpośrednio lub przez pośrednie węzły)

### Kanał PRIMARY - Domyślna Konfiguracja

**Domyślnie**, wszystkie węzły Meshtastic używają kanału PRIMARY o nazwie:
- **Nazwa**: `LongFast`
- **Klucz szyfrowania**: `AQ==` (Base64, odpowiada Hex 0x01)
- **Modem preset**: `LONG_FAST`

Jeśli **nie zmienisz** tej konfiguracji, wszystkie węzły w zasięgu automatycznie będą widzieć pozycje innych węzłów.

---

## Konfiguracja Podstawowa - Wszystkie Węzły Widzą Wszystkie Pozycje

### Wymagania

Aby wszystkie węzły w sieci mogły przesyłać pozycje między sobą, **wszystkie węzły muszą mieć identyczne ustawienia**:

1. **Kanał PRIMARY**:
   - Ta sama nazwa kanału
   - Ten sam klucz szyfrowania (PSK - Pre-Shared Key)
   - Włączone udostępnianie pozycji

2. **Ustawienia LoRa**:
   - Ten sam **LoRa Frequency Slot** (częstotliwość)
   - Ten sam **Modem Preset** (domyślnie `LONG_FAST`)
   - Ten sam region (np. EU868, US915)

3. **Ustawienia pozycji**:
   - Włączone automatyczne udostępnianie pozycji
   - GPS włączony i działający (lub pozycja z telefonu)

### Kroki Konfiguracji

#### Krok 1: Sprawdź Firmware

Upewnij się, że wszystkie węzły mają aktualne firmware Meshtastic (zalecane 2.5+).

#### Krok 2: Skonfiguruj Kanał PRIMARY (jeśli zmieniasz domyślne ustawienia)

**W aplikacji Meshtastic (Android/iOS):**

1. Otwórz aplikację Meshtastic
2. Przejdź do **Settings** (Ustawienia)
3. Wybierz **Radio Config** → **Channels**
4. Wybierz **Channel 0 (PRIMARY)**
5. Upewnij się, że:
   - **Channel Name**: `LongFast` (lub wybierz wspólną nazwę dla swojej sieci)
   - **PSK (Pre-Shared Key)**: `AQ==` (dla domyślnego) lub ustaw wspólny klucz
   - **Position Sharing**: **WŁĄCZONE** ✅

**Ważne**: Jeśli zmieniasz nazwę lub klucz kanału PRIMARY, **wszystkie węzły w sieci muszą mieć identyczne ustawienia**.

#### Krok 3: Skonfiguruj LoRa Frequency Slot

1. W aplikacji Meshtastic: **Settings** → **Radio Config** → **LoRa Frequency Slot**
2. Upewnij się, że wszystkie węzły używają **tego samego slotu częstotliwości**
3. Dla regionu EU: zazwyczaj slot 0 (868.1 MHz)
4. Dla regionu US: zazwyczaj slot 0 (903.1 MHz)

**Uwaga**: Jeśli zmieniasz częstotliwość, sprawdź lokalne przepisy dotyczące użytkowania pasm ISM.

#### Krok 4: Włącz GPS i Udostępnianie Pozycji

1. W aplikacji Meshtastic: **Settings** → **Module Config** → **Position**
2. Upewnij się, że:
   - **Position Enabled**: **WŁĄCZONE** ✅
   - **Position Broadcast Interval**: Ustaw interwał (np. 15 minut dla oszczędności baterii, lub częściej dla aktualizacji w czasie rzeczywistym)
   - **Position Broadcast Smart**: **WŁĄCZONE** ✅ (automatycznie zwiększa częstotliwość podczas ruchu)

#### Krok 5: Sprawdź Modem Preset

1. W aplikacji Meshtastic: **Settings** → **Radio Config** → **Modem Preset**
2. Upewnij się, że wszystkie węzły używają **tego samego presetu** (domyślnie `LONG_FAST`)

**Dlaczego to ważne**: Różne presety używają różnych parametrów LoRa (spreading factor, bandwidth), co uniemożliwia komunikację między węzłami.

#### Krok 6: Sprawdź Role Węzłów

1. W aplikacji Meshtastic: **Settings** → **Radio Config** → **Role**
2. **Zalecane**: Użyj roli `CLIENT` dla większości węzłów
   - `CLIENT`: Węzeł odbiera, wysyła i inteligentnie retransmituje wiadomości
   - `CLIENT_MUTE`: Węzeł tylko wysyła i odbiera, nie retransmituje (użyj gdy masz wiele węzłów blisko siebie)
   - `CLIENT_BASE`: Węzeł priorytetyzuje retransmisję z własnych ulubionych węzłów

**Unikaj**: `ROUTER` i `REPEATER` chyba że masz konkretny powód - mogą powodować problemy z siecią.

#### Krok 7: Sprawdź Hop Count

1. W aplikacji Meshtastic: **Settings** → **Radio Config** → **Hop Count**
2. **Zalecane**: Pozostaw domyślną wartość **3 hops**
   - Zwiększaj tylko jeśli wiesz, że potrzebujesz więcej skoków
   - Zbyt wysoka wartość może powodować problemy z siecią

---

## Konfiguracja Zaawansowana - Prywatne Kanały (Firmware 2.7.1+)

Jeśli chcesz udostępniać pozycję tylko wybranym węzłom (np. tylko członkom swojej grupy), możesz użyć **prywatnego kanału wtórnego**.

### Scenariusz: Pozycja Tylko dla Grupy

1. **Wyłącz pozycję na kanale PRIMARY**:
   - Settings → Radio Config → Channels → Channel 0 (PRIMARY)
   - Position Sharing: **WYŁĄCZONE** ❌

2. **Utwórz prywatny kanał wtórny**:
   - Settings → Radio Config → Channels → **Add Channel**
   - **Channel Name**: np. `GrupaWyprawa`
   - **PSK**: Ustaw **wspólny klucz szyfrowania** (musi być identyczny dla wszystkich członków grupy)
   - **Position Sharing**: **WŁĄCZONE** ✅
   - **Position Precision**: Wybierz poziom precyzji (np. 32-bit dla dokładnej pozycji)

3. **Dodaj ten sam kanał do wszystkich węzłów w grupie**:
   - Wszystkie węzły muszą mieć **identyczną nazwę kanału** i **identyczny klucz PSK**

4. **Automatyczne aktualizacje pozycji**:
   - Tylko **najniższy indeksowany kanał wtórny z włączoną pozycją** otrzymuje automatyczne aktualizacje
   - Jeśli masz Channel 1 i Channel 2 z pozycją, tylko Channel 1 otrzymuje automatyczne aktualizacje
   - Channel 2 może nadal otrzymywać pozycje przez ręczne żądania

### Przykład Konfiguracji

```
Channel 0 (PRIMARY): "LongFast"
  - Position Sharing: OFF ❌
  - PSK: AQ== (domyślny, publiczny)

Channel 1: "GrupaWyprawa"
  - Position Sharing: ON ✅
  - PSK: [wspólny klucz dla grupy]
  - Position Precision: 32-bit

Channel 2: "Przyjaciele"
  - Position Sharing: ON ✅
  - PSK: [inny wspólny klucz]
  - Position Precision: 32-bit
```

W tym przykładzie:
- **Channel 1** otrzymuje automatyczne aktualizacje pozycji
- **Channel 2** może otrzymywać pozycje przez ręczne żądania
- **Channel 0** nie otrzymuje pozycji

---

## Kontrola Precyzji Pozycji

Meshtastic pozwala kontrolować precyzję pozycji GPS przesyłanej przez kanał. To jest przydatne dla prywatności.

### Ustawienia Precyzji

1. W aplikacji Meshtastic: **Settings** → **Radio Config** → **Channels** → Wybierz kanał
2. Znajdź opcję **Position Precision**
3. Wybierz poziom:
   - **32-bit**: Pełna precyzja (dokładność do metrów)
   - **24-bit**: Precyzja ~10 metrów
   - **20-bit**: Precyzja ~100 metrów
   - **16-bit**: Precyzja ~1 km

**Uwaga**: Niższa precyzja = większa prywatność, ale mniejsza dokładność na mapie.

---

## Weryfikacja Konfiguracji

### Jak Sprawdzić Czy Pozycje Są Przesyłane

1. **W aplikacji Meshtastic**:
   - Otwórz listę węzłów (Nodes)
   - Sprawdź czy węzły mają ikonę pozycji GPS
   - Kliknij węzeł → powinieneś zobaczyć pozycję na mapie

2. **W aplikacji MeshTracker**:
   - Połącz aplikację z Meshtastic
   - Sprawdź ekran mapy - powinny pojawić się markery dla węzłów z pozycją
   - Sprawdź listę węzłów - węzły z pozycją powinny mieć status "Online" z pozycją

### Typowe Problemy

#### Problem: Węzły nie widzą pozycji innych węzłów

**Możliwe przyczyny**:
1. ❌ Różne nazwy kanałów PRIMARY
2. ❌ Różne klucze PSK
3. ❌ Różne częstotliwości LoRa (Frequency Slot)
4. ❌ Różne modem presety
5. ❌ Pozycja wyłączona na kanale PRIMARY
6. ❌ Węzły poza zasięgiem (sprawdź SNR/RSSI)

**Rozwiązanie**:
- Sprawdź ustawienia kanału PRIMARY na wszystkich węzłach
- Upewnij się, że wszystkie mają identyczne ustawienia
- Sprawdź czy pozycja jest włączona

#### Problem: Pozycje są widoczne, ale nieaktualne

**Możliwe przyczyny**:
1. ❌ Zbyt długi interwał aktualizacji pozycji
2. ❌ GPS wyłączony lub słaby sygnał GPS
3. ❌ Węzeł nie porusza się (smart broadcast może nie aktualizować)

**Rozwiązanie**:
- Zmniejsz interwał aktualizacji pozycji (Settings → Module Config → Position → Position Broadcast Interval)
- Sprawdź czy GPS działa (w aplikacji Meshtastic powinna być ikona GPS)
- Włącz "Position Broadcast Smart" dla automatycznych aktualizacji podczas ruchu

#### Problem: Niektóre węzły widzą pozycje, inne nie

**Możliwe przyczyny**:
1. ❌ Węzły używają różnych kanałów
2. ❌ Niektóre węzły mają wyłączoną pozycję
3. ❌ Problemy z zasięgiem (węzły poza zasięgiem)

**Rozwiązanie**:
- Sprawdź konfigurację każdego węzła osobno
- Upewnij się, że wszystkie używają tego samego kanału PRIMARY
- Sprawdź zasięg między węzłami (SNR/RSSI w aplikacji Meshtastic)

---

## Najlepsze Praktyki

### Dla Sieci Publicznej (Wszystkie Węzły Widzą Wszystkie Pozycje)

1. ✅ **Użyj domyślnych ustawień** kanału PRIMARY (`LongFast`, `AQ==`)
2. ✅ **Nie zmieniaj** nazwy ani klucza kanału PRIMARY (chyba że tworzysz prywatną sieć)
3. ✅ **Użyj modem preset `LONG_FAST`** dla najlepszej kompatybilności
4. ✅ **Ustaw interwał pozycji** odpowiednio do potrzeb (15-30 minut dla oszczędności baterii)
5. ✅ **Włącz "Position Broadcast Smart"** dla automatycznych aktualizacji podczas ruchu

### Dla Sieci Prywatnej (Tylko Grupa Widzi Pozycje)

1. ✅ **Wyłącz pozycję na kanale PRIMARY**
2. ✅ **Utwórz prywatny kanał wtórny** z unikalnym kluczem PSK
3. ✅ **Użyj silnego klucza PSK** (nie używaj domyślnego `AQ==`)
4. ✅ **Udostępnij klucz PSK tylko członkom grupy**
5. ✅ **Ustaw odpowiednią precyzję pozycji** (32-bit dla dokładności, niższa dla prywatności)

### Oszczędność Baterii

1. ✅ **Zwiększ interwał aktualizacji pozycji** (30-60 minut gdy nie poruszasz się)
2. ✅ **Użyj "Position Broadcast Smart"** - automatycznie zwiększa częstotliwość podczas ruchu
3. ✅ **Wyłącz GPS gdy nie jest potrzebny** (ale wtedy pozycja nie będzie aktualizowana)

---

## Podsumowanie - Szybki Przewodnik

### Minimalna Konfiguracja (Dla Początkujących)

1. **Zostaw domyślne ustawienia** kanału PRIMARY (`LongFast`, `AQ==`)
2. **Włącz pozycję** w Settings → Module Config → Position
3. **Upewnij się, że wszystkie węzły używają tego samego Frequency Slot**
4. **Gotowe!** Węzły powinny automatycznie przesyłać pozycje między sobą

### Konfiguracja dla Grupy (Zaawansowana)

1. **Wyłącz pozycję na kanale PRIMARY**
2. **Utwórz kanał wtórny** z unikalnym kluczem PSK
3. **Włącz pozycję na kanale wtórnym**
4. **Dodaj ten sam kanał do wszystkich węzłów w grupie**
5. **Gotowe!** Tylko członkowie grupy widzą pozycje

---

## Dodatkowe Zasoby

- [Oficjalna dokumentacja Meshtastic - Configuration Tips](https://meshtastic.org/docs/configuration/tips/)
- [Oficjalna dokumentacja Meshtastic - Remote Admin](https://meshtastic.org/docs/configuration/remote-admin/)
- [Dokumentacja projektu MeshTracker](../DOKUMENTACJA_PROJEKTU.md)

---

**Ostatnia aktualizacja**: 2024  
**Wersja dokumentacji**: 1.0
