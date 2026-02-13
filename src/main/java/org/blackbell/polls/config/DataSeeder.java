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

/**
 * Seeds the database with initial data for development/testing.
 */
@Component
@Profile({"dev", "railway"})
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final TownRepository townRepository;
    private final InstitutionRepository institutionRepository;

    public DataSeeder(TownRepository townRepository, InstitutionRepository institutionRepository) {
        this.townRepository = townRepository;
        this.institutionRepository = institutionRepository;
    }

    @Override
    public void run(String... args) {
        log.info("Seeding database with initial data...");

        seedTowns();
        seedInstitutions();

        log.info("Database seeding completed.");
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

        if (townRepository.findByRef("bratislava") == null) {
            Town bratislava = new Town();
            bratislava.setRef("bratislava");
            bratislava.setName("Bratislava");
            bratislava.setSource(Source.BA_OPENDATA);
            townRepository.save(bratislava);
            log.info("Created town: {}", bratislava);
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