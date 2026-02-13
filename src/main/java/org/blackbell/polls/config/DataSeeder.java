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

        fixSourceCheckConstraint();
        seedTowns();
        seedInstitutions();
        resyncDmMeetingsWithoutVotes();

        log.info("Database seeding completed.");
    }

    private void fixSourceCheckConstraint() {
        try {
            entityManager.createNativeQuery(
                    "ALTER TABLE town DROP CONSTRAINT IF EXISTS town_source_check").executeUpdate();
            entityManager.createNativeQuery(
                    "ALTER TABLE town ADD CONSTRAINT town_source_check CHECK (source IN ('DM', 'BA_OPENDATA', 'OTHER'))").executeUpdate();
            log.info("Updated town_source_check constraint to include BA_OPENDATA");
        } catch (Exception e) {
            log.warn("Could not update town_source_check constraint: {}", e.getMessage());
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
            bratislava.setSource(Source.BA_OPENDATA);
            townRepository.save(bratislava);
            log.info("Created town: {}", bratislava);
        } else if (bratislava.getSource() != Source.BA_OPENDATA) {
            bratislava.setSource(Source.BA_OPENDATA);
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

    /**
     * One-time fix: delete DM meetings that have polls but no individual votes,
     * so they get re-synced with the fixed DM API URLs (format=json).
     */
    private void resyncDmMeetingsWithoutVotes() {
        try {
            // Find meeting IDs that have polls but no votes
            String meetingIds =
                    "SELECT DISTINCT m.id FROM meeting m" +
                    " JOIN town t ON m.town_id = t.id" +
                    " JOIN agenda_item ai ON ai.meeting_id = m.id" +
                    " JOIN poll p ON p.agenda_item_id = ai.id" +
                    " LEFT JOIN vote v ON v.poll_id = p.id" +
                    " WHERE t.source = 'DM' AND v.id IS NULL";

            // Delete in correct order: polls -> agenda_items -> meetings
            entityManager.createNativeQuery(
                    "DELETE FROM poll WHERE agenda_item_id IN (" +
                    "  SELECT ai.id FROM agenda_item ai WHERE ai.meeting_id IN (" + meetingIds + ")" +
                    ")").executeUpdate();
            entityManager.createNativeQuery(
                    "DELETE FROM agenda_item WHERE meeting_id IN (" + meetingIds + ")").executeUpdate();
            entityManager.createNativeQuery(
                    "DELETE FROM meeting_attachment WHERE meeting_id IN (" + meetingIds + ")").executeUpdate();
            int deleted = entityManager.createNativeQuery(
                    "DELETE FROM meeting WHERE id IN (" + meetingIds + ")").executeUpdate();
            if (deleted > 0) {
                log.info("Deleted {} DM meetings without votes for re-sync", deleted);
            }
        } catch (Exception e) {
            log.warn("Could not clean up DM meetings: {}", e.getMessage());
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