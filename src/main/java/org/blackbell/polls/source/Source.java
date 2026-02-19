package org.blackbell.polls.source;

/**
 * Created by kurtcha on 25.2.2018.
 */
public enum Source {
    DM,           // Digitálne Mesto API
    DM_PDF,       // DM PDF fallback (runtime v MeetingSyncService, nie cez config)
    BA_ARCGIS,    // Bratislava ArcGIS (2014-2022)
    BA_WEB,       // Bratislava web scraping (2022-2026)
    PRESOV_WEB,   // Prešov web scraping (members)
    POPRAD_WEB,   // Poprad web scraping (members)
    TRNAVA_WEB,   // Trnava web scraping (members + meetings + votes)
    KOSICE_WEB,   // Košice web scraping (members + meetings + votes)
    NITRA_WEB,    // Nitra web scraping (members + meetings)
    BB_WEB,       // Banská Bystrica web scraping (members)
    TRENCIN_WEB,  // Trenčín web scraping (members)
    MANUAL,       // Manuálne zadané dáta
    OTHER;
}
