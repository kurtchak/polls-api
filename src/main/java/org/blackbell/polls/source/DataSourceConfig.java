package org.blackbell.polls.source;

import org.blackbell.polls.domain.model.enums.InstitutionType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Centrálna konfigurácia zdrojov dát.
 * Mapuje (townRef, seasonRef?, institution?, operation?) → [Source1, Source2, ...]
 */
@Component
public class DataSourceConfig {

    record SourceRule(
        String townRef,              // null = wildcard
        String seasonRef,            // null = wildcard (akákoľvek sezóna)
        InstitutionType institution, // null = wildcard (nateraz vždy null, pripravené pre budúcnosť)
        DataOperation operation,     // null = wildcard (všetky operácie)
        Source source
    ) {
        boolean matches(String town, String season, InstitutionType inst, DataOperation op) {
            return (townRef == null || townRef.equals(town))
                && (seasonRef == null || seasonRef.equals(season))
                && (institution == null || institution == inst)
                && (operation == null || operation == op);
        }
    }

    private final List<SourceRule> rules = buildRules();

    /**
     * Vráti zoradený zoznam zdrojov pre dané mesto/sezónu/inštitúciu/operáciu.
     *
     * Postup:
     * 1. Hľadá pravidlá presne pre (town, season, inst, op)
     * 2. Ak nič nenájde a season != null → skúsi pravidlá s season=null (wildcard sezóna)
     * 3. Ak stále nič → DM ako fallback
     */
    public List<Source> resolve(String townRef, String seasonRef,
                                InstitutionType institution, DataOperation operation) {
        // Presné match (town + season + institution + operation)
        List<Source> result = rules.stream()
                .filter(r -> r.matches(townRef, seasonRef, institution, operation))
                .map(SourceRule::source)
                .distinct()
                .toList();

        if (!result.isEmpty()) return result;

        // Wildcard season match (town + null-season + operation)
        if (seasonRef != null) {
            result = rules.stream()
                    .filter(r -> r.matches(townRef, null, institution, operation))
                    .map(SourceRule::source)
                    .distinct()
                    .toList();
            if (!result.isEmpty()) return result;
        }

        // Fallback: DM
        return List.of(Source.DM);
    }

    /**
     * Vráti VŠETKY zdroje nakonfigurované pre dané mesto (pre agregáciu sezón).
     */
    public List<Source> allSourcesForTown(String townRef) {
        return rules.stream()
                .filter(r -> townRef.equals(r.townRef()))
                .map(SourceRule::source)
                .distinct()
                .toList();
    }

    private List<SourceRule> buildRules() {
        List<SourceRule> r = new ArrayList<>();

        // --- Bratislava ---
        // 2022-2026: web scraping pre všetko
        r.add(new SourceRule("bratislava", "2022-2026", null, null, Source.BA_WEB));
        // 2014-2018, 2018-2022: ArcGIS
        r.add(new SourceRule("bratislava", "2014-2018", null, null, Source.BA_ARCGIS));
        r.add(new SourceRule("bratislava", "2018-2022", null, null, Source.BA_ARCGIS));
        // Default pre Bratislavu (nová sezóna bez pravidla → skúsi BA_WEB)
        r.add(new SourceRule("bratislava", null, null, null, Source.BA_WEB));

        // --- Prešov ---
        // Members cez web scraper
        r.add(new SourceRule("presov", null, null, DataOperation.MEMBERS, Source.PRESOV_WEB));
        // Ostatné cez DM (explicitne, aj keď by fallback fungoval)
        r.add(new SourceRule("presov", null, null, null, Source.DM));

        // --- Košice, Poprad --- (len DM, pokryté fallbackom)
        // Niet explicitných pravidiel → fallback na DM

        return r;
    }
}
