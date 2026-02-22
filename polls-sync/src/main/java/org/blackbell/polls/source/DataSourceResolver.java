package org.blackbell.polls.source;

import org.blackbell.polls.domain.DataImport;
import org.blackbell.polls.domain.model.Town;
import org.blackbell.polls.domain.model.enums.DataOperation;
import org.blackbell.polls.domain.model.enums.InstitutionType;
import org.blackbell.polls.domain.model.enums.Source;
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
    private final SyncLogService syncLogService;

    public record SourcedResult<T>(T data, Source source) {}
    public record SourcedItem<T>(T item, Source source) {}

    public DataSourceResolver(DataSourceConfig config, List<DataImport> allImports,
                              SyncLogService syncLogService) {
        this.config = config;
        this.registry = buildRegistry(allImports);
        this.syncLogService = syncLogService;
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
        return allImports.stream()
                .collect(Collectors.toMap(
                        di -> di.getSource(),
                        di -> di,
                        (a, b) -> a
                ));
    }

    // --- Resolver helper methods ---

    @FunctionalInterface
    public interface CheckedFunction<T, R> {
        R apply(T t) throws Exception;
    }

    /**
     * FIRST WINS: skúsi zdroje v poradí, vráti prvý úspešný výsledok s informáciou o zdroji.
     */
    public <T> SourcedResult<T> resolveAndLoad(Town town, String seasonRef,
            InstitutionType inst, DataOperation op,
            CheckedFunction<DataImport, T> loader) {
        List<DataImport> imports = resolve(town, seasonRef, inst, op);
        for (DataImport di : imports) {
            long start = System.currentTimeMillis();
            try {
                T result = loader.apply(di);
                long duration = System.currentTimeMillis() - start;
                if (result != null && !(result instanceof Collection<?> c && c.isEmpty())) {
                    int count = result instanceof Collection<?> col ? col.size() : 1;
                    log.info("Source {} provided data for {}/{}/{}",
                        di.getSource(), town.getRef(), seasonRef, op);
                    syncLogService.logSuccess(town.getRef(), seasonRef, inst, op,
                            di.getSource(), count, duration);
                    return new SourcedResult<>(result, di.getSource());
                }
                syncLogService.logSuccess(town.getRef(), seasonRef, inst, op,
                        di.getSource(), 0, duration);
            } catch (Exception e) {
                long duration = System.currentTimeMillis() - start;
                log.warn("Source {} failed for {}/{}/{}: {}",
                    di.getSource(), town.getRef(), seasonRef, op, e.getMessage());
                syncLogService.logFailure(town.getRef(), seasonRef, inst, op,
                        di.getSource(), e.getMessage(), duration);
            }
        }
        log.error("No data source provided data for {}/{}/{}", town.getRef(), seasonRef, op);
        return null;
    }

    /**
     * AGGREGATE: zozbiera výsledky zo všetkých zdrojov (pre sezóny) s informáciou o zdroji.
     */
    public <T> List<SourcedItem<T>> resolveAndAggregate(Town town, DataOperation op,
            CheckedFunction<DataImport, List<T>> loader) {
        List<SourcedItem<T>> aggregated = new ArrayList<>();
        for (DataImport di : allForTown(town)) {
            long start = System.currentTimeMillis();
            try {
                List<T> result = loader.apply(di);
                long duration = System.currentTimeMillis() - start;
                if (result != null && !result.isEmpty()) {
                    for (T item : result) {
                        aggregated.add(new SourcedItem<>(item, di.getSource()));
                    }
                    syncLogService.logSuccess(town.getRef(), null, null, op,
                            di.getSource(), result.size(), duration);
                } else {
                    syncLogService.logSuccess(town.getRef(), null, null, op,
                            di.getSource(), 0, duration);
                }
            } catch (Exception e) {
                long duration = System.currentTimeMillis() - start;
                log.warn("Source {} failed for {}/{}: {}",
                    di.getSource(), town.getRef(), op, e.getMessage());
                syncLogService.logFailure(town.getRef(), null, null, op,
                        di.getSource(), e.getMessage(), duration);
            }
        }
        if (aggregated.isEmpty()) {
            log.error("No source provided {} for town {}", op, town.getRef());
        }
        return aggregated;
    }
}
