# polls-api - Slovenské komunálne hlasovania

REST API pre agregáciu a zobrazenie hlasovaní mestských zastupiteľstiev na Slovensku.

## Tech stack
- **Java 21**, Spring Boot 3.4.1, Gradle 8.12
- **Jakarta Persistence** (migrované z javax.persistence)
- **PostgreSQL** (produkcia), H2 (dev profil)
- **jsoup** pre web scraping

## Zdroje dát

### 1. DM API (Digitálne Mesto)
- Base URL: `https://www.digitalnemesto.sk/DmApi/`
- Endpointy:
  - `GetDZVolebneObdobie/mesto-{city}` - volebné obdobia
  - `GetDZZasadnutie/{mz|mr}/mesto-{city}?VolebneObdobie={season}` - zasadnutia
  - `GetDZZaKoDet/{meetingGuid}` - detail zasadnutia s programom
  - `GetDZHlas/{idBodProgramu}/{route}` - detaily hlasovania s menami
- **Stav:** Funguje pre staršie obdobia (2014-2018). Novšie (2022-2026) majú prázdne sekcie "Hlasovania"
- **Chýba:** Endpoint pre zoznam poslancov

### 2. presov.sk Crawler
- `PresovCouncilMemberCrawlerV2` - nový crawler pre prepracovanú stránku (2024+)
- URL: `https://www.presov.sk/poslanci-msz.html`
- Parsuje: meno, foto, email, telefón, strany, kluby, komisie, volebný obvod
- Starý `PresovCouncilMemberCrawler` je obsolete (JavaScript štruktúra už neexistuje)

## Kľúčové komponenty

### Synchronizácia
- `SyncAgent` - hlavná sync logika, beží ako @Scheduled task
- `DMImport` / `DMParser` - spracovanie DM API odpovedí
- `PresovCouncilMemberCrawlerV2` - scraping poslancov z presov.sk

### Sledovanie politikov ("prezliekači")
- `Politician` entita je zdieľaná naprieč sezónami
- `PoliticianRepository.findPartySwitchers()` - kto menil strany
- `PoliticianRepository.findClubSwitchers()` - kto menil kluby
- API: `GET /politicians/party-switchers`

## Spustenie

```bash
# Dev profil s H2 databázou
./gradlew bootRun --args='--spring.profiles.active=dev'

# Build
./gradlew build
```

## Známe problémy

1. **DM API - prázdne hlasovania** pre sezóny 2022-2026
   - Pravdepodobne ešte nie sú publikované v systéme
   - Staršie sezóny (2014-2018) fungujú

2. **Poll details** - `extAgendaItemId` a `extPollRouteId` sú null pre novšie zasadnutia
   - Súvisí s bodom 1

## Štruktúra projektu

```
src/main/java/org/blackbell/polls/
├── config/          # DataSeeder, konfigurácia
├── controllers/     # REST API endpointy
├── domain/
│   ├── model/       # Entity (Politician, CouncilMember, Club, Party, Vote...)
│   └── repositories/# JPA repositories
└── source/
    ├── crawler/     # PresovCouncilMemberCrawlerV2
    └── dm/          # DM API klient a parser
```

## Posledné zmeny (január 2026)

- Upgrade na Java 21, Spring Boot 3.4.1, Gradle 8.12
- Migrácia javax.persistence → jakarta.persistence
- Nový `PresovCouncilMemberCrawlerV2` pre novú štruktúru presov.sk
- `PoliticianRepository` + `PoliticiansController` pre sledovanie prezliekačov
- Oprava `DMParser` - diacritika "Materiály", robustnejší parsing

## Plánované AI features

### Sprístupnenie hlasovaní bežným občanom
- **Preklad názvov hlasovaní** do zrozumiteľného jazyka (názvy bodov programu sú často úradnícky nečitateľné)
- **Zhrnutie** čoho sa hlasovanie týkalo, pre koho je to relevantné

### Kategorizácia a zhlukovanie
- **Labeling/tagging** hlasovaní podľa tém (doprava, školstvo, rozpočet, územný plán...)
- **Zhlukovanie** podobných hlasovaní naprieč zasadnutiami

### Analýza hlasovania členov
- **Súlad so stranou/klubom** - miera zhody hlasovania člena s jeho klubom
- **Detekcia "rebel" hlasovaní** - kedy člen hlasuje opakovane proti svojmu klubu
- **Skupiny podľa hlasovacích vzorcov** - ktorí členovia hlasujú podobne, aj keď sú z rôznych klubov/strán
- **Cross-club analýza** - či člen hlasuje častejšie v súlade s iným klubom než vlastným