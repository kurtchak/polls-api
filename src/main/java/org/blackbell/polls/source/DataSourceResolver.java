package org.blackbell.polls.source;

import org.blackbell.polls.domain.model.Town;
import org.blackbell.polls.domain.model.enums.InstitutionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Resolves DataImport implementations for a given town/season/institution/operation.
 * Uses DataSourceConfig for routing rules and a registry of Source → DataImport mappings.
 */
@Component
public class DataSourceResolver {

    private static final Logger log = LoggerFactory.getLogger(DataSourceResolver.class);

    private final DataSourceConfig config;
    private final Map<Source, DataImport> registry;

    public DataSourceResolver(DataSourceConfig config, List<DataImport> allImports) {
        this.config = config;
        this.registry = buildRegistry(allImports);
        log.info("DataSourceResolver initialized with {} source mappings: {}",
                registry.size(), registry.keySet());
    }

    /**
     * Vráti zoradený zoznam DataImport implementácií na vyskúšanie.
     */
    public List<DataImport> resolve(Town town, String seasonRef,
                                     InstitutionType institution, DataOperation operation) {
        List<Source> sources = config.resolve(town.getRef(), seasonRef, institution, operation);
        List<DataImport> imports = sources.stream()
                .map(registry::get)
                .filter(Objects::nonNull)
                .toList();
        if (imports.isEmpty()) {
            log.warn("No DataImport found for {}/{}/{}/{}", town.getRef(), seasonRef, institution, operation);
        }
        return imports;
    }

    /**
     * Pre agregáciu sezón: všetky zdroje pre dané mesto.
     */
    public List<DataImport> allForTown(Town town) {
        List<Source> sources = config.allSourcesForTown(town.getRef());
        if (sources.isEmpty()) {
            // Fallback: DM
            sources = List.of(Source.DM);
        }
        return sources.stream()
                .map(registry::get)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private Map<Source, DataImport> buildRegistry(List<DataImport> allImports) {
        // Map each DataImport implementation to its Source
        return allImports.stream()
                .collect(Collectors.toMap(
                        this::sourceFor,
                        di -> di,
                        (a, b) -> a // in case of duplicates, keep first
                ));
    }

    private Source sourceFor(DataImport di) {
        String className = di.getClass().getSimpleName();
        return switch (className) {
            case "DMImport" -> Source.DM;
            case "BratislavaArcGisImport" -> Source.BA_ARCGIS;
            case "BratislavaWebImport" -> Source.BA_WEB;
            case "PresovMemberImport" -> Source.PRESOV_WEB;
            default -> {
                log.warn("Unknown DataImport class: {}, mapping to OTHER", className);
                yield Source.OTHER;
            }
        };
    }
}
