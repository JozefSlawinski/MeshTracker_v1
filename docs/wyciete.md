Rozdzial 3
## 3.6 Wstępna dyskusja możliwości realizacji wymagań

Kluczowym pytaniem na etapie projektowania było, czy limit duty cycle 1% wynikający z regulacji ETSI nie uniemożliwia spełnienia wymagania częstotliwości aktualizacji pozycji.

Dla konfiguracji SF=12, BW=125 kHz typowy pakiet pozycji Meshtastic ma długość około 50 bajtów i zajmuje pasmo przez około 2,5 sekundy. Przy częstotliwości co 5 sekund w trybie ruchu współczynnik wypełnienia wynosi 2,5/5 = 50% - co przekracza limit. Wymaganie W3.1 (adaptacyjna częstotliwość) jest więc nie tylko postulatem wygodowym, ale warunkiem koniecznym zgodności z regulacjami. W trybie spoczynku (co 20 sekund) współczynnik wynosi 2,5/20 = 12,5% - nadal powyżej limitu dla pojedynczego kanału, lecz Meshtastic rozkłada transmisje na kilka kanałów, co w praktyce redukuje efektywny duty cycle poniżej progu regulacyjnego.

Zasięg 2 km w terenie otwartym przy SF=12 i mocy nadawania 100 mW (prawne maksimum w paśmie 868 MHz) jest osiągalny - literatura podaje zasięgi rzędu 5–15 km w warunkach line-of-sight dla tej konfiguracji. Teren leśny wprowadza tłumienie rzędu 20–30 dB, co redukuje zasięg do 1–3 km. Wymaganie minimalne 2 km w terenie otwartym oceniono jako realistyczne.
