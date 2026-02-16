package org.blackbell.polls.source;

/**
 * Types of data operations that data sources can provide.
 */
public enum DataOperation {
    SEASONS,          // Zoznam sezón (volebných období)
    MEETINGS,         // Zoznam zasadnutí
    MEETING_DETAILS,  // Detail zasadnutia (body programu + polls)
    POLL_DETAILS,     // Menovité hlasovania
    MEMBERS           // Zoznam poslancov
}
