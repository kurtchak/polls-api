package org.blackbell.polls.config;

import org.blackbell.polls.common.Constants;
import org.blackbell.polls.domain.model.Institution;
import org.blackbell.polls.domain.model.Town;
import org.blackbell.polls.domain.model.enums.InstitutionType;
import org.blackbell.polls.domain.repositories.InstitutionRepository;
import org.blackbell.polls.domain.repositories.TownRepository;
import org.blackbell.polls.domain.model.enums.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds the database with initial data for development/testing.
 */
@Component
@Profile({"dev", "railway"})
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final TownRepository townRepository;
    private final InstitutionRepository institutionRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public DataSeeder(TownRepository townRepository, InstitutionRepository institutionRepository) {
        this.townRepository = townRepository;
        this.institutionRepository = institutionRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Seeding database with initial data...");

        fixSourceCheckConstraints();
        backfillDataSources();
        seedTowns();
        seedInstitutions();
        backfillTownSeasons();

        log.info("Database seeding completed.");
    }

    private void fixSourceCheckConstraints() {
        try {
            // --- Town source constraint ---
            entityManager.createNativeQuery(
                    "ALTER TABLE town DROP CONSTRAINT IF EXISTS town_source_check").executeUpdate();
            // Migrate legacy BA_OPENDATA → BA_ARCGIS
            int migrated = entityManager.createNativeQuery(
                    "UPDATE town SET source = 'BA_ARCGIS' WHERE source = 'BA_OPENDATA'").executeUpdate();
            if (migrated > 0) {
                log.info("Migrated {} town(s) from BA_OPENDATA to BA_ARCGIS", migrated);
            }
            entityManager.createNativeQuery(
                    "ALTER TABLE town ADD CONSTRAINT town_source_check CHECK (source IN ('DM', 'DM_PDF', 'BA_ARCGIS', 'BA_WEB', 'PRESOV_WEB', 'POPRAD_WEB', 'TRNAVA_WEB', 'KOSICE_WEB', 'NITRA_WEB', 'BB_WEB', 'TRENCIN_WEB', 'MANUAL', 'OTHER'))").executeUpdate();

            // --- Poll data_source: migrate old DataSourceType values to Source values ---
            entityManager.createNativeQuery(
                    "ALTER TABLE poll DROP CONSTRAINT IF EXISTS poll_data_source_check").executeUpdate();
            int dmMigrated = entityManager.createNativeQuery(
                    "UPDATE poll SET data_source = 'DM' WHERE data_source = 'DM_API'").executeUpdate();
            if (dmMigrated > 0) {
                log.info("Migrated {} poll(s) from DM_API to DM", dmMigrated);
            }
            int baMigrated = entityManager.createNativeQuery(
                    "UPDATE poll SET data_source = 'BA_ARCGIS' WHERE data_source = 'BA_API'").executeUpdate();
            if (baMigrated > 0) {
                log.info("Migrated {} poll(s) from BA_API to BA_ARCGIS", baMigrated);
            }
            entityManager.createNativeQuery(
                    "ALTER TABLE poll ADD CONSTRAINT poll_data_source_check CHECK (data_source IN ('DM', 'DM_PDF', 'BA_ARCGIS', 'BA_WEB', 'PRESOV_WEB', 'POPRAD_WEB', 'TRNAVA_WEB', 'KOSICE_WEB', 'NITRA_WEB', 'BB_WEB', 'TRENCIN_WEB', 'MANUAL', 'OTHER'))").executeUpdate();

            // --- All other tables with Source enum columns (Hibernate-generated check constraints) ---
            String sourceValues = "'DM', 'DM_PDF', 'BA_ARCGIS', 'BA_WEB', 'PRESOV_WEB', 'POPRAD_WEB', 'TRNAVA_WEB', 'KOSICE_WEB', 'NITRA_WEB', 'BB_WEB', 'TRENCIN_WEB', 'MANUAL', 'OTHER'";
            for (String table : new String[]{"sync_log", "council_member", "meeting", "season"}) {
                String col = table.equals("sync_log") ? "source" : "data_source";
                String constraintName = table + "_" + col + "_check";
                entityManager.createNativeQuery(
                        "ALTER TABLE " + table + " DROP CONSTRAINT IF EXISTS " + constraintName).executeUpdate();
                entityManager.createNativeQuery(
                        "ALTER TABLE " + table + " ADD CONSTRAINT " + constraintName +
                        " CHECK (" + col + " IN (" + sourceValues + "))").executeUpdate();
            }

            log.info("Updated source check constraints for unified Source enum values");
        } catch (Exception e) {
            log.warn("Could not update source check constraints: {}", e.getMessage());
        }
    }

    /**
     * Backfill data_source na existujúcich záznamoch kde je NULL.
     * Mapovanie vychádza z DataSourceConfig pravidiel:
     *   - Bratislava 2022-2026 → BA_WEB, staršie sezóny → BA_ARCGIS
     *   - Prešov members → PRESOV_WEB, ostatné → DM
     *   - Poprad 2022-2026 members → POPRAD_WEB, ostatné → DM
     *   - Košice → removed (no data source)
     */
    private void backfillDataSources() {
        try {
            int total = 0;

            // --- Season (shared across towns, no town FK → use DM as safe default) ---
            int seasons = entityManager.createNativeQuery(
                    "UPDATE season SET data_source = 'DM' WHERE data_source IS NULL").executeUpdate();
            total += seasons;

            // --- Meeting: Bratislava 2022-2026 → BA_WEB ---
            int baMeetingsWeb = entityManager.createNativeQuery(
                    "UPDATE meeting SET data_source = 'BA_WEB' WHERE data_source IS NULL" +
                    " AND town_id = (SELECT id FROM town WHERE ref = 'bratislava')" +
                    " AND season_id IN (SELECT id FROM season WHERE ref = '2022-2026')").executeUpdate();
            total += baMeetingsWeb;

            // --- Meeting: Bratislava 2014-2018, 2018-2022 → BA_ARCGIS ---
            int baMeetingsArcgis = entityManager.createNativeQuery(
                    "UPDATE meeting SET data_source = 'BA_ARCGIS' WHERE data_source IS NULL" +
                    " AND town_id = (SELECT id FROM town WHERE ref = 'bratislava')" +
                    " AND season_id IN (SELECT id FROM season WHERE ref IN ('2014-2018', '2018-2022'))").executeUpdate();
            total += baMeetingsArcgis;

            // --- Meeting: all remaining → DM ---
            int dmMeetings = entityManager.createNativeQuery(
                    "UPDATE meeting SET data_source = 'DM' WHERE data_source IS NULL").executeUpdate();
            total += dmMeetings;

            // --- CouncilMember: Bratislava all seasons → BA_WEB (enrichment source) ---
            int baMembers = entityManager.createNativeQuery(
                    "UPDATE council_member SET data_source = 'BA_WEB' WHERE data_source IS NULL" +
                    " AND town_id = (SELECT id FROM town WHERE ref = 'bratislava')").executeUpdate();
            total += baMembers;

            // --- CouncilMember: Prešov → PRESOV_WEB ---
            int presovMembers = entityManager.createNativeQuery(
                    "UPDATE council_member SET data_source = 'PRESOV_WEB' WHERE data_source IS NULL" +
                    " AND town_id = (SELECT id FROM town WHERE ref = 'presov')").executeUpdate();
            total += presovMembers;

            // --- CouncilMember: Poprad 2022-2026 → POPRAD_WEB ---
            int popradMembers = entityManager.createNativeQuery(
                    "UPDATE council_member SET data_source = 'POPRAD_WEB' WHERE data_source IS NULL" +
                    " AND town_id = (SELECT id FROM town WHERE ref = 'poprad')" +
                    " AND season_id IN (SELECT id FROM season WHERE ref = '2022-2026')").executeUpdate();
            total += popradMembers;

            // --- CouncilMember: all remaining → DM ---
            int dmMembers = entityManager.createNativeQuery(
                    "UPDATE council_member SET data_source = 'DM' WHERE data_source IS NULL").executeUpdate();
            total += dmMembers;

            if (total > 0) {
                log.info("Backfilled data_source on {} records (seasons: {}, meetings: BA_WEB={} BA_ARCGIS={} DM={}, members: BA_WEB={} PRESOV_WEB={} POPRAD_WEB={} DM={})",
                        total, seasons, baMeetingsWeb, baMeetingsArcgis, dmMeetings,
                        baMembers, presovMembers, popradMembers, dmMembers);
            }
        } catch (Exception e) {
            log.warn("Could not backfill data_source: {}", e.getMessage());
        }
    }

    private void seedTowns() {
        if (townRepository.findByRef("presov") == null) {
            Town presov = new Town();
            presov.setRef("presov");
            presov.setName("Prešov");
            presov.setSource(Source.DM);
            townRepository.save(presov);
            log.info("Created town: {}", presov);
        }

        // Košice — now has web scraper
        Town kosice = townRepository.findByRef("kosice");
        if (kosice == null) {
            kosice = new Town();
            kosice.setRef("kosice");
            kosice.setName("Košice");
            kosice.setSource(Source.KOSICE_WEB);
            townRepository.save(kosice);
            log.info("Created town: {}", kosice);
        } else if (kosice.getSource() != Source.KOSICE_WEB) {
            kosice.setSource(Source.KOSICE_WEB);
            townRepository.save(kosice);
            log.info("Updated town source: {}", kosice);
        }

        Town bratislava = townRepository.findByRef("bratislava");
        if (bratislava == null) {
            bratislava = new Town();
            bratislava.setRef("bratislava");
            bratislava.setName("Bratislava");
            bratislava.setSource(Source.BA_ARCGIS);
            townRepository.save(bratislava);
            log.info("Created town: {}", bratislava);
        } else if (bratislava.getSource() != Source.BA_ARCGIS) {
            bratislava.setSource(Source.BA_ARCGIS);
            townRepository.save(bratislava);
            log.info("Updated town source: {}", bratislava);
        }

        if (townRepository.findByRef("poprad") == null) {
            Town poprad = new Town();
            poprad.setRef("poprad");
            poprad.setName("Poprad");
            poprad.setSource(Source.DM);
            townRepository.save(poprad);
            log.info("Created town: {}", poprad);
        }

        // Trnava
        if (townRepository.findByRef("trnava") == null) {
            Town trnava = new Town();
            trnava.setRef("trnava");
            trnava.setName("Trnava");
            trnava.setSource(Source.TRNAVA_WEB);
            townRepository.save(trnava);
            log.info("Created town: {}", trnava);
        }

        // Nitra
        if (townRepository.findByRef("nitra") == null) {
            Town nitra = new Town();
            nitra.setRef("nitra");
            nitra.setName("Nitra");
            nitra.setSource(Source.NITRA_WEB);
            townRepository.save(nitra);
            log.info("Created town: {}", nitra);
        }

        // Banská Bystrica
        if (townRepository.findByRef("banska-bystrica") == null) {
            Town bb = new Town();
            bb.setRef("banska-bystrica");
            bb.setName("Banská Bystrica");
            bb.setSource(Source.BB_WEB);
            townRepository.save(bb);
            log.info("Created town: {}", bb);
        }

        // Trenčín
        if (townRepository.findByRef("trencin") == null) {
            Town trencin = new Town();
            trencin.setRef("trencin");
            trencin.setName("Trenčín");
            trencin.setSource(Source.TRENCIN_WEB);
            townRepository.save(trencin);
            log.info("Created town: {}", trencin);
        }
    }

    /**
     * Backfill town_season join table from existing meetings.
     * Links each town to the seasons for which it has meetings,
     * plus ensures the current season (2022-2026) is linked for all towns.
     */
    private void backfillTownSeasons() {
        try {
            // Link towns to seasons based on existing meetings
            int fromMeetings = entityManager.createNativeQuery(
                    "INSERT INTO town_season (town_id, season_id) " +
                    "SELECT DISTINCT m.town_id, m.season_id FROM meeting m " +
                    "WHERE m.town_id IS NOT NULL AND m.season_id IS NOT NULL " +
                    "AND NOT EXISTS (SELECT 1 FROM town_season ts " +
                    "WHERE ts.town_id = m.town_id AND ts.season_id = m.season_id)"
            ).executeUpdate();

            // Ensure current season is linked for all towns
            int currentSeason = entityManager.createNativeQuery(
                    "INSERT INTO town_season (town_id, season_id) " +
                    "SELECT t.id, s.id FROM town t, season s " +
                    "WHERE s.ref = '2022-2026' " +
                    "AND NOT EXISTS (SELECT 1 FROM town_season ts " +
                    "WHERE ts.town_id = t.id AND ts.season_id = s.id)"
            ).executeUpdate();

            if (fromMeetings > 0 || currentSeason > 0) {
                log.info("Backfilled town_season links: {} from meetings, {} current season", fromMeetings, currentSeason);
            }
        } catch (Exception e) {
            log.warn("Could not backfill town_season: {}", e.getMessage());
        }
    }

    private void seedInstitutions() {
        if (institutionRepository.findByType(InstitutionType.ZASTUPITELSTVO) == null) {
            Institution zastupitelstvo = new Institution();
            zastupitelstvo.setRef(Constants.ZASTUPITELSTVO);
            zastupitelstvo.setName("Mestské zastupiteľstvo");
            zastupitelstvo.setType(InstitutionType.ZASTUPITELSTVO);
            zastupitelstvo.setDescription("Zastupiteľstvo mesta");
            institutionRepository.save(zastupitelstvo);
            log.info("Created institution: {}", zastupitelstvo);
        }

        if (institutionRepository.findByType(InstitutionType.RADA) == null) {
            Institution rada = new Institution();
            rada.setRef(Constants.RADA);
            rada.setName("Mestská rada");
            rada.setType(InstitutionType.RADA);
            rada.setDescription("Rada mesta");
            institutionRepository.save(rada);
            log.info("Created institution: {}", rada);
        }

        if (institutionRepository.findByType(InstitutionType.KOMISIA) == null) {
            Institution komisia = new Institution();
            komisia.setRef(Constants.KOMISIA);
            komisia.setName("Komisia");
            komisia.setType(InstitutionType.KOMISIA);
            komisia.setDescription("Komisie mestského zastupiteľstva");
            institutionRepository.save(komisia);
            log.info("Created institution: {}", komisia);
        }
    }
}