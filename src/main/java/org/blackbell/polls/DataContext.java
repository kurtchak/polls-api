package org.blackbell.polls;

import org.blackbell.polls.meetings.model.Town;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by kurtcha on 25.2.2018.
 */
public class DataContext {


    private static Map<String, Town> townsMap;

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

    public static List<Town> getTowns() {
        return townsMap != null ? (List) townsMap.values() : null;
    }

    public static void addTowns(List<Town> towns) {
        if (townsMap == null) {
            townsMap = new HashMap<>();
        }
        for (Town town : towns) {
            townsMap.put(town.getName(), town);
        }

    }
}
