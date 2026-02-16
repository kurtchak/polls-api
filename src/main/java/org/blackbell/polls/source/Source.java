package org.blackbell.polls.source;

/**
 * Created by kurtcha on 25.2.2018.
 */
public enum Source {
    DM,           // Digitálne Mesto API
    DM_PDF,       // DM PDF fallback (runtime v SyncAgent, nie cez config)
    BA_ARCGIS,    // Bratislava ArcGIS (2014-2022)
    BA_WEB,       // Bratislava web scraping (2022-2026)
    PRESOV_WEB,   // Prešov web scraping (members)
    OTHER;

    // Backward compat pre Town.source stĺpec
    public static final Source BA_OPENDATA = BA_ARCGIS;
}
