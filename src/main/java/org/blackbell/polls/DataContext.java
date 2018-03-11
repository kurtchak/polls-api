package org.blackbell.polls;

import org.blackbell.polls.meetings.model.CouncilMember;
import org.blackbell.polls.meetings.model.Season;
import org.blackbell.polls.meetings.model.Town;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.ApplicationScope;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by kurtcha on 25.2.2018.
 */
@ApplicationScope
@Component
public class DataContext {

    private static Map<String, Town> townsMap;
    private static Map<Season, Map<String, CouncilMember>> allMembersMap;

    // TOWNS
    public static void addTown(Town town) {
        if (townsMap == null) {
            townsMap = new HashMap<>();
        }
        townsMap.put(town.getName(), town);
    }

    public static Town getTown(String name) {
        if (townsMap == null) {
            return null;
        }
        return townsMap.get(name);
    }

    public static boolean hasTown(String name) {
        return townsMap != null && townsMap.containsKey(name);
    }

    public static Collection<Town> getTowns() {
        return townsMap != null ? townsMap.values() : null;
    }

    public static void addTowns(List<Town> towns) {
        if (townsMap == null) {
            townsMap = new HashMap<>();
        }
        for (Town town : towns) {
            townsMap.put(town.getName(), town);
        }
    }

    // MEMBERS
    public static void addMember(Season season, CouncilMember member) {
        if (allMembersMap == null) {
            allMembersMap = new HashMap<>();
        }
        if (!allMembersMap.containsKey(season)) {
            allMembersMap.put(season, new HashMap<>());
        }
        allMembersMap.get(season).put(member.getName(), member);
    }

    public static CouncilMember getMember(Season season, String name) {
        if (allMembersMap == null || !allMembersMap.containsKey(season)) {
            return null;
        }
        return allMembersMap.get(season).get(name);
    }

    public static Collection<CouncilMember> getMembers(Season season) {
        return allMembersMap != null && allMembersMap.containsKey(season) ? allMembersMap.get(season).values() : null;
    }

    public static void addMembers(Season season, List<CouncilMember> members) {
        if (allMembersMap == null) {
            allMembersMap = new HashMap<>();
        }
        if (!allMembersMap.containsKey(season)) {
            allMembersMap.put(season, new HashMap<>());
        }
        for (CouncilMember member : members) {
            allMembersMap.get(season).put(member.getName(), member);
        }
    }

    public static Map<Season, Map<String, CouncilMember>> getAllMembersMap() {
        return allMembersMap;
    }

    public static Map<String, CouncilMember> getMembersMap(Season season) {
        return allMembersMap != null ? allMembersMap.get(season) : null;
    }


}
