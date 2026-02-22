package org.blackbell.polls.domain.model.enums;

import java.util.regex.Pattern;

/**
 * Typy väčšín potrebných na prijatie uznesenia v mestskom zastupiteľstve.
 * Podľa zákona č. 369/1990 Zb. o obecnom zriadení.
 */
public enum MajorityType {

    /**
     * Nadpolovičná väčšina prítomných poslancov (§12 ods. 7).
     * Default pre bežné uznesenia.
     */
    SIMPLE_MAJORITY,

    /**
     * Trojpätinová väčšina prítomných poslancov (§12 ods. 7).
     * Pre prijatie VZN (všeobecne záväzného nariadenia).
     */
    THREE_FIFTHS_PRESENT,

    /**
     * Trojpätinová väčšina všetkých poslancov (§13 ods. 8).
     * Pre prelomenie pozastavenia (veta) primátora.
     */
    THREE_FIFTHS_ALL,

    /**
     * Nadpolovičná väčšina všetkých poslancov.
     * Pre voľbu/odvolanie hlavného kontrolóra (§18a ods. 3, 10),
     * zmenu programu zasadnutia (§12 ods. 5),
     * vyhlásenie referenda o odvolaní starostu (§13a ods. 3).
     */
    ABSOLUTE_MAJORITY;

    private static final Pattern VZN_PATTERN = Pattern.compile(
            "všeobecne\\s+záväzn|\\bVZN\\b|\\bnariadeni",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private static final Pattern VETO_OVERRIDE_PATTERN = Pattern.compile(
            "pozastaven.*uzneseni|prelomen.*veta|potvrden.*pozastaven",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private static final Pattern AUDITOR_PATTERN = Pattern.compile(
            "(voľb|odvolan|zvolen).*hlavn.*kontrolór|hlavn.*kontrolór.*(voľb|odvolan|zvolen)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private static final Pattern AGENDA_CHANGE_PATTERN = Pattern.compile(
            "zmena?.*(návrhu?)?\\s*programu\\s*(zasadnutia|rokovania)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private static final Pattern REFERENDUM_PATTERN = Pattern.compile(
            "referend.*odvolan.*(starost|primátor)|odvolan.*(starost|primátor).*referend",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    /**
     * Detekuje typ potrebnej väčšiny z názvu bodu programu.
     */
    public static MajorityType detectFromAgendaItemName(String agendaItemName) {
        if (agendaItemName == null || agendaItemName.isBlank()) {
            return SIMPLE_MAJORITY;
        }

        if (VZN_PATTERN.matcher(agendaItemName).find()) {
            return THREE_FIFTHS_PRESENT;
        }

        if (VETO_OVERRIDE_PATTERN.matcher(agendaItemName).find()) {
            return THREE_FIFTHS_ALL;
        }

        if (AUDITOR_PATTERN.matcher(agendaItemName).find()
                || AGENDA_CHANGE_PATTERN.matcher(agendaItemName).find()
                || REFERENDUM_PATTERN.matcher(agendaItemName).find()) {
            return ABSOLUTE_MAJORITY;
        }

        return SIMPLE_MAJORITY;
    }
}
