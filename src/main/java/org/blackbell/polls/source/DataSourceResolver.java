package org.blackbell.polls.source;

import org.blackbell.polls.domain.model.Town;
import org.blackbell.polls.domain.model.enums.InstitutionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
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

    // --- Resolver helper methods ---

    @FunctionalInterface
    public interface CheckedFunction<T, R> {
        R apply(T t) throws Exception;
    }

    /**
     * FIRST WINS: skúsi zdroje v poradí, vráti prvý úspešný výsledok.
     */
    public <T> T resolveAndLoad(Town town, String seasonRef,
            InstitutionType inst, DataOperation op,
            CheckedFunction<DataImport, T> loader) {
        List<DataImport> imports = resolve(town, seasonRef, inst, op);
        for (DataImport di : imports) {
            try {
                T result = loader.apply(di);
                if (result != null && !(result instanceof Collection<?> c && c.isEmpty())) {
                    log.info("Source {} provided data for {}/{}/{}",
                        di.getClass().getSimpleName(), town.getRef(), seasonRef, op);
                    return result;
                }
            } catch (Exception e) {
                log.warn("Source {} failed for {}/{}/{}: {}",
                    di.getClass().getSimpleName(), town.getRef(), seasonRef, op, e.getMessage());
            }
        }
        log.error("No data source provided data for {}/{}/{}", town.getRef(), seasonRef, op);
        return null;
    }

    /**
     * AGGREGATE: zozbiera výsledky zo všetkých zdrojov (pre sezóny).
     */
    public <T> List<T> resolveAndAggregate(Town town, DataOperation op,
            CheckedFunction<DataImport, List<T>> loader) {
        List<T> aggregated = new ArrayList<>();
        for (DataImport di : allForTown(town)) {
            try {
                List<T> result = loader.apply(di);
                if (result != null) aggregated.addAll(result);
            } catch (Exception e) {
                log.warn("Source {} failed for {}/{}: {}",
                    di.getClass().getSimpleName(), town.getRef(), op, e.getMessage());
            }
        }
        if (aggregated.isEmpty()) {
            log.error("No source provided {} for town {}", op, town.getRef());
        }
        return aggregated;
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
