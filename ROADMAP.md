# Roadmap - Komunálne hlasovania

## 1. Analýza hlasovaní

Hlbšia analytika nad hlasovacími dátami:

- **Súlad s klubom/stranou** — ako často poslanec hlasuje v zhode so svojím klubom, resp. stranou za ktorú kandidoval
- **Celkový súlad v hlasovaniach** — matice zhody medzi poslancami (kto s kým najčastejšie hlasuje rovnako)
- **Rozporuplné rozhodnutia** — hlasovania kde poslanec hlasoval inak ako väčšina jeho klubu/strany
- **Ďalšie metriky** — účasť na hlasovaniach, pomer Za/Proti/Zdržal sa, aktivita v čase

Toto budeme postupne rozpracovávať.

## 2. Profily poslancov a politická história

Nahradíme hanlivý pojem "prezliekači" neutrálnou analýzou politickej kariéry:

- **Vizualizácia politického postupu** — timeline zmien strán, klubov a funkcií naprieč obdobiami
- **Minulé obdobia v profile** — v detaile poslanca zobraziť aj predchádzajúce volebné obdobia, v ktorých pôsobil
- **Počet rokov zastupovania** — celková doba pôsobenia v zastupiteľstve
- **Relevantné informácie z internetu** — automaticky dohľadať a zobraziť verejne dostupné informácie o poslancovi (profesia, verejné funkcie, mediálne výstupy) pre ucelený profil
- **Rozšírená analýza zmien** — kontext zmien strán/klubov (rozpad strany, zlúčenie, vlastné rozhodnutie)

## 3. Hodnotová zhoda s poslancom

Personalizovaná funkcionalita pre občanov:

- **Kvíz pre používateľa** — séria otázok/hlasovaní kde používateľ vyjadrí svoj postoj k reálnym témam zo zastupiteľstva
- **Výpočet zhody** — algoritmus porovnávajúci odpovede používateľa s reálnymi hlasovaniami poslancov
- **Výsledky** — zoradenie poslancov podľa miery zhody s používateľom, s vysvetlením prečo (v ktorých témach sa zhodujú/rozchádzajú)
- **Personalizácia** — ukladanie preferencií používateľa, sledovanie "svojich" poslancov

Toto je väčšia funkcionalita vyžadujúca:
- Výber reprezentatívnych hlasovaní pre kvíz
- UX pre kvíz (zrozumiteľné formulácie, nie príliš dlhý)
- Backend pre ukladanie odpovedí a výpočet zhody
- Autentifikácia / anonymné profily