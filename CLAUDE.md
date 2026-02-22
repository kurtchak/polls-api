# polls-api - Slovenské komunálne hlasovania

REST API pre agregáciu a zobrazenie hlasovaní mestských zastupiteľstiev na Slovensku.

## Tech stack
- **Java 21**, Spring Boot 3.4.1, Gradle 8.12
- **Jakarta Persistence** (migrované z javax.persistence)
- **PostgreSQL** (produkcia), H2 (dev profil)
- **jsoup** pre web scraping
- **Gradle multi-module** — 4 submoduly, 1 deployovateľný fat JAR

## Štruktúra projektu (multi-module)

```
polls-api/                           (root — conventions only)
├── settings.gradle.kts              (include 4 submoduly)
├── build.gradle.kts                 (plugins apply false, -parameters flag)
│
├── polls-domain/                    (entity, repos, enums, serializers)
│   └── src/main/java/org/blackbell/polls/
│       ├── common/                  (Constants, PollsUtils)
│       └── domain/
│           ├── DataImport.java      (interface pre importéry)
│           ├── api/                 (Views, serializers)
│           ├── model/               (entity, enums, embeddable, relate)
│           └── repositories/        (12 JPA repos)
│
├── polls-sync/                      (scrapery, sync orchestrácia, DM API)
│   └── src/main/java/org/blackbell/polls/
│       ├── source/                  (SyncOrchestrator, importéry, scrapery)
│       │   ├── bratislava/          (ArcGIS + web scraper)
│       │   ├── crawler/             (Košice, Trnava, Nitra, Prešov, Poprad, BB, Trenčín)
│       │   └── dm/                  (DM API klient, parser, PDF importer)
│       ├── sync/                    (SyncEvent, SyncProgress, SSE broadcaster)
│       ├── config/                  (CrawlerProperties, DmApiProperties, SyncProperties)
│       └── controllers/             (SyncController, AdminSyncLogController, AdminSeasonController)
│
├── polls-bff/                       (REST API pre frontend)
│   └── src/main/java/org/blackbell/polls/
│       ├── controllers/             (11 controllerov — Seasons, Meetings, Members, Polls...)
│       ├── service/                 (10 services)
│       └── config/                  (WebConfig)
│
└── polls-app/                       (bootable JAR — spája všetko)
    ├── src/main/java/.../           (Application.java, DataSeeder.java)
    └── src/main/resources/          (application*.properties, banner.txt)
```

### Hranice modulov (vynútené kompilátorom)
- **polls-bff** závisí len na **polls-domain** — nesmie importovať z source/sync
- **polls-sync** závisí len na **polls-domain**
- **polls-app** závisí na všetkých troch, obsahuje `@SpringBootApplication`

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

### 2. Web scrapery (polls-sync modul)
- **Bratislava** — ArcGIS API (2014-2022) + web scraper (2022-2026)
- **Prešov** — `PresovCouncilMemberCrawlerV2` pre presov.sk (2024+)
- **Košice** — `KosiceScraper` pre members, meetings, votes
- **Trnava** — `TrnavaScraper` pre members, meetings, votes
- **Nitra, Poprad, B. Bystrica, Trenčín** — members only

## Kľúčové komponenty

### Synchronizácia (polls-sync)
- `SyncOrchestrator` - hlavná sync logika, beží ako @Scheduled task
- `DataSourceResolver` - routing na správny importér podľa mesta a operácie
- `DMImport` / `DMParser` - spracovanie DM API odpovedí
- `SyncEventBroadcaster` - SSE streaming sync udalostí na frontend

### Sledovanie politikov ("prezliekači")
- `Politician` entita je zdieľaná naprieč sezónami
- `PoliticianRepository.findPartySwitchers()` - kto menil strany
- `PoliticianRepository.findClubSwitchers()` - kto menil kluby
- API: `GET /politicians/party-switchers`, `GET /politicians/club-switchers`

## Spustenie

```bash
# Dev profil s H2 databázou
./gradlew :polls-app:bootRun --args='--spring.profiles.active=dev'

# Build (fat JAR)
./gradlew :polls-app:bootJar

# Kompilacia po moduloch
./gradlew :polls-domain:compileJava
./gradlew :polls-sync:compileJava
./gradlew :polls-bff:compileJava
./gradlew :polls-app:compileJava

# Testy
./gradlew test

# Overenie hraníc — BFF nesmie importovať z source/sync
grep -r "org.blackbell.polls.source" polls-bff/src/ && echo "FAIL" || echo "OK"
grep -r "org.blackbell.polls.sync" polls-bff/src/ && echo "FAIL" || echo "OK"
```

## Známe problémy

1. **DM API - prázdne hlasovania** pre sezóny 2022-2026
   - Pravdepodobne ešte nie sú publikované v systéme
   - Staršie sezóny (2014-2018) fungujú

2. **Poll details** - `extAgendaItemId` a `extPollRouteId` sú null pre novšie zasadnutia
   - Súvisí s bodom 1

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
