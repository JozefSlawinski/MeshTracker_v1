# Dokumentacja - Integracja z Meshtastic

## Pliki w tym pakiecie

### 1. `DOKUMENTACJA_MESHTASTIC_POZYCJA.md`
**Kompletna dokumentacja techniczna** zawierająca:
- Przegląd architektury systemu
- Szczegółowy opis interfejsu IMeshService
- Struktury danych (NodeInfo, Position, MeshUser)
- Krok po kroku implementacja
- Gotowe fragmenty kodu
- Gotowy prompt do użycia z AI

**Użyj tego pliku gdy:**
- Chcesz zrozumieć jak działa system
- Potrzebujesz szczegółowych informacji technicznych
- Chcesz zobaczyć przykłady kodu z wyjaśnieniami

### 2. `PROMPT_IMPLEMENTACJA.txt`
**Gotowy prompt do skopiowania** - możesz użyć go bezpośrednio z:
- ChatGPT
- Claude
- GitHub Copilot
- Innymi narzędziami AI

**Użyj tego pliku gdy:**
- Chcesz szybko wygenerować kod używając AI
- Potrzebujesz gotowego promptu bez dodatkowych wyjaśnień

### 3. `PRZYKLADOWY_KOD.java`
**Kompletna, gotowa do użycia klasa Java** zawierająca:
- Pełną implementację wyświetlania pozycji węzłów
- Obsługę broadcastów
- Zarządzanie cyklem życia
- Obsługę błędów i logowanie

**Użyj tego pliku gdy:**
- Chcesz skopiować gotowy kod
- Potrzebujesz działającego przykładu
- Chcesz zobaczyć kompletną implementację

## Szybki start

### Opcja 1: Użyj gotowego kodu
1. Otwórz `PRZYKLADOWY_KOD.java`
2. Skopiuj klasę `MeshtasticPositionDisplay`
3. Dodaj do swojego projektu
4. Użyj w komponencie mapy:
   ```java
   MeshtasticPositionDisplay display = new MeshtasticPositionDisplay(context);
   // W onDestroy():
   display.cleanup();
   ```

### Opcja 2: Użyj AI do generacji
1. Otwórz `PROMPT_IMPLEMENTACJA.txt`
2. Skopiuj całą zawartość
3. Wklej do narzędzia AI (ChatGPT, Claude, etc.)
4. AI wygeneruje kod na podstawie promptu

### Opcja 3: Implementuj samodzielnie
1. Otwórz `DOKUMENTACJA_MESHTASTIC_POZYCJA.md`
2. Przeczytaj sekcję "Implementacja - Krok po kroku"
3. Postępuj zgodnie z instrukcjami
4. Użyj gotowych fragmentów kodu jako referencji

## Wymagania

### Zależności
Aplikacja musi mieć dostęp do:
- `org.meshtastic.core.model.*` (NodeInfo, Position, MeshUser)
- `org.meshtastic.core.service.IMeshService` (AIDL)

### AndroidManifest.xml
Dodaj do `<manifest>`:
```xml
<queries>
    <package android:name="com.geeksville.mesh" />
</queries>
```

### Aplikacja Meshtastic
Aplikacja Meshtastic musi być zainstalowana i uruchomiona na urządzeniu.

## Struktura dokumentacji

```
.
├── README_DOKUMENTACJA.md          ← Ten plik
├── DOKUMENTACJA_MESHTASTIC_POZYCJA.md  ← Pełna dokumentacja
├── PROMPT_IMPLEMENTACJA.txt        ← Gotowy prompt dla AI
└── PRZYKLADOWY_KOD.java            ← Gotowy kod
```

## Najczęstsze pytania

**Q: Czy muszę używać wszystkich plików?**  
A: Nie, wybierz to co Ci odpowiada. Jeśli chcesz szybko zacząć, użyj `PRZYKLADOWY_KOD.java`.

**Q: Jak sprawdzić czy działa?**  
A: 
1. Upewnij się że aplikacja Meshtastic jest uruchomiona
2. Połącz urządzenie Meshtastic z aplikacją
3. Sprawdź logi (Log.d z tagiem "MeshtasticPos")
4. Sprawdź czy markery pojawiają się na mapie

**Q: Czy muszę mieć fizyczne urządzenie Meshtastic?**  
A: Tak, aplikacja Meshtastic musi być połączona z urządzeniem radio Meshtastic, aby otrzymywać pozycje węzłów.

## Wsparcie

Jeśli masz pytania lub problemy:
1. Sprawdź logi aplikacji (szukaj tagu "MeshtasticPos")
2. Przeczytaj `DOKUMENTACJA_MESHTASTIC_POZYCJA.md` - sekcja "Dodatkowe informacje"
3. Sprawdź czy aplikacja Meshtastic jest uruchomiona i połączona z radiem

## Licencja

Ta dokumentacja jest częścią projektu MeshTracker.

