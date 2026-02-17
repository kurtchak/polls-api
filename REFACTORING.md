# Refactoring TODO: polls-api

## Kontext

Aplikácia polls-api prešla 9-ročným vývojom (2017-2026) s 3 fázami dormancie a jednou intenzívnou AI-asistovanou revitalizáciou (jan-feb 2026). Výsledkom je funkčná ale nekonzistentná architektúra. Tento checklist slúži na postupné čistenie pomedzi bežný vývoj.

**Verifikácia:** Po každej zmene spustiť `./verify-api.sh` (viď nižšie).

---

## Fáza 1: Dead code removal & cleanup

- [x] **1.1** Zmazať `source/crawler/PresovCouncilMemberCrawler.java` (obsolete v1 crawler, nepoužívaný od 2024)
- [x] **1.2** Zmazať `PollsUtils.saveToFile()` a `PollsUtils.readFileToString()` (debug, nikdy nevolané)
- [x] **1.3** Zmazať `DataSeeder.resyncDmMeetingsWithoutVotes()` (nikdy nevolaná, one-time fix)
- [x] **1.4** Zmazať `source/base/BaseImport.java` + celý `source/base/` package (všetko vracia null)
- [x] **1.5** V SyncAgent factory: throw `UnsupportedOperationException` namiesto return BaseImport (nahradené DataSourceResolverom)
- [x] **1.6** `DMServiceClient.java`: Nahradiť `System.out.println()` → `log.debug()` / `log.warn()`
- [x] **1.7** `build.gradle.kts`: Odstrániť `mssql-jdbc` runtime dependency
- [ ] **1.8** Prejsť TODO komentáre — zmazať alebo zdokumentovať ako issues:
  - [ ] `DMParser:130` — `//TODO: members...`
  - [ ] `DMParser:232` — `//TODO: use stable reference`
  - [ ] `CouncilMember:39` — `//TODO: get rid of clubMembers list`
  - [ ] `SyncAgent:421` — `//TODO:` komisia sync
  - [ ] `SyncAgent:697` — `//TODO: preco vracia empty set?`

---

## Fáza 2: Externalizácia konfigurácie

- [x] **2.1** Vytvoriť `@ConfigurationProperties` triedu `DmApiProperties` (`dm.api.base-url`)
- [x] **2.2** Vytvoriť `@ConfigurationProperties` triedu `CrawlerProperties` (`presov.crawler.members-url`, `crawler.timeout-ms`)
- [x] **2.3** Presunúť DM API base URL z `Constants.java` → `application.properties`
- [x] **2.4** Presunúť Presov crawler URL z hardcoded konštanty → `application.properties`
- [x] **2.5** Upraviť `DMAPIUtils`, `DMServiceClient` — injektovať `DmApiProperties`
- [x] **2.6** Upraviť `PresovCouncilMemberCrawlerV2` — injektovať `CrawlerProperties`
- [x] **2.7** Externalizovať `IRRELEVANT_AGENDA_PATTERN` z `PollsController` → properties
- [x] **2.8** Externalizovať sync timing (`sync.fixed-rate-ms`, `sync.initial-delay-ms`)

---

## Fáza 3: Drobné vylepšenia

- [x] **3.1** `CouncilMember.java`: null-safe equals/hashCode cez `Objects.equals()` (už bolo implementované)
- [x] **3.2** `PollsUtils.startWithFirstname()`: pridať length check pred array prístupom (už bolo implementované)
- [x] **3.3** Presunúť `isMeetingComplete()` logiku z SyncAgent na `Meeting` entitu — pridané `hasPolls()`, `hasVotes()`, `hasUnmatchedVotes()` metódy
- [x] **3.4** `SyncAgent`: nahradiť `e.printStackTrace()` → `log.error("msg", e)` (už bolo implementované)
- [x] **3.5** `SyncAgent`: logovať plný stack trace pri zlyhaniach syncSingleMeeting()

---

## Fáza 4: Thread safety

- [ ] **4.1** `SyncProgress`: pridať `synchronized` na `startSync()`, `endSync()`, `getStatus()`
- [ ] **4.2** `SyncProgress`: nahradiť `volatile Date` → `AtomicReference<Instant>` (Date je mutable)
- [ ] **4.3** `SyncProgress`: overiť konzistenciu snapshot v `getStatus()`

---

## Fáza 5: Service layer

- [ ] **5.1** Vytvoriť `PollService` — extrahuje business logiku z `PollsController` (filtrovanie, markAsIrrelevant)
- [ ] **5.2** Vytvoriť `MeetingService` — extrahuje z `MeetingsController`
- [ ] **5.3** Vytvoriť `MemberService` — extrahuje z `MembersController`
- [ ] **5.4** Vytvoriť `ClubService` — extrahuje z `ClubsController`
- [ ] **5.5** Vytvoriť `PartyService` — extrahuje z `PartiesController`
- [ ] **5.6** Vytvoriť `PoliticianService` — extrahuje z `PoliticiansController`
- [ ] **5.7** Vytvoriť `SeasonService` — extrahuje z `SeasonsController`
- [ ] **5.8** Zjednodušiť controllery — len request mapping + delegácia na service

---

## Fáza 6: Rozbiť SyncAgent (722 riadkov → ~6 tried)

- [ ] **6.1** Vytvoriť `SyncCacheManager` — extrahuje loadTownsMap, loadSeasonsMap, loadPartiesMap, loadPoliticiansMap
- [ ] **6.2** Vytvoriť `SeasonSyncService` — extrahuje syncSeasons()
- [ ] **6.3** Vytvoriť `CouncilMemberSyncService` — extrahuje syncCouncilMembers() + politician matching
- [ ] **6.4** Vytvoriť `MeetingSyncService` — extrahuje syncSeasonMeetings() + syncSingleMeeting()
- [ ] **6.5** Vytvoriť `PoliticianMatchingService` — extrahuje findPoliticianByName, createMissingMembers
- [ ] **6.6** Premenovať SyncAgent → `SyncOrchestrator` — len scheduling + orchestrácia (~100 riadkov)
- [ ] **6.7** Odstrániť `@Lazy self` self-injection — transakcie na service metódach
- [ ] **6.8** Vytvoriť `DataImportFactory` @Component alebo `Map<Source, DataImport>` beans namiesto `new DMImport()`

---

## Fáza 7: Serialization cleanup

- [ ] **7.1** Zanalyzovať ktoré serializery robia netriviálnu transformáciu vs. len property filtering
- [ ] **7.2** Odstrániť `SeasonPropertySerializer` → nahradiť `@JsonProperty` + `@JsonView`
- [ ] **7.3** Odstrániť `TownPropertySerializer` → nahradiť `@JsonProperty` + `@JsonView`
- [ ] **7.4** Odstrániť `InstitutionPropertySerializer` → nahradiť `@JsonProperty` + `@JsonView`
- [ ] **7.5** Zvážiť DTO vrstvu pre najkomplexnejšie endpointy (polls detail, member detail)

---

## Mimo scope

- Nové features, Flyway/Liquibase, prepísanie testov, frontend, zmena API kontraktu, zvýšenie test coverage