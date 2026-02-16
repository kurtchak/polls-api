package org.blackbell.polls.config;

import org.blackbell.polls.common.Constants;
import org.blackbell.polls.domain.model.Institution;
import org.blackbell.polls.domain.model.Town;
import org.blackbell.polls.domain.model.enums.InstitutionType;
import org.blackbell.polls.domain.repositories.InstitutionRepository;
import org.blackbell.polls.domain.repositories.TownRepository;
import org.blackbell.polls.source.Source;
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
        seedTowns();
        seedInstitutions();

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
                    "ALTER TABLE town ADD CONSTRAINT town_source_check CHECK (source IN ('DM', 'BA_ARCGIS', 'BA_WEB', 'PRESOV_WEB', 'DM_PDF', 'OTHER'))").executeUpdate();

            // --- Poll data_source constraint ---
            entityManager.createNativeQuery(
                    "ALTER TABLE poll DROP CONSTRAINT IF EXISTS poll_data_source_check").executeUpdate();
            entityManager.createNativeQuery(
                    "ALTER TABLE poll ADD CONSTRAINT poll_data_source_check CHECK (data_source IN ('DM_API', 'DM_PDF', 'BA_API', 'BA_WEB', 'MANUAL'))").executeUpdate();

            log.info("Updated source check constraints for new enum values");
        } catch (Exception e) {
            log.warn("Could not update source check constraints: {}", e.getMessage());
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

        if (townRepository.findByRef("kosice") == null) {
            Town kosice = new Town();
            kosice.setRef("kosice");
            kosice.setName("Košice");
            kosice.setSource(Source.DM);
            townRepository.save(kosice);
            log.info("Created town: {}", kosice);
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