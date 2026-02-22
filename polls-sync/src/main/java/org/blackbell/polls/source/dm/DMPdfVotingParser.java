package org.blackbell.polls.source.dm;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for PDF voting results from DM API (Format 1 - standard meetings).
 *
 * PDF format: one vote per page, flowing text with names separated by
 * single spaces, titles spanning across line breaks (e.g. "Bandura,\nPhD.").
 * Category headers (Za:, Proti:, Zdržali sa:, Nehlasovali:) appear on
 * their own lines. Categories with 0 votes are omitted entirely.
 */
public class DMPdfVotingParser {

    private static final Logger log = LoggerFactory.getLogger(DMPdfVotingParser.class);

    private static final Pattern VOTE_NUMBER_PATTERN = Pattern.compile(
            "Číslo hlasovania:\\s*(\\d+)", Pattern.UNICODE_CHARACTER_CLASS);
    private static final Pattern AGENDA_ITEM_NUMBER_PATTERN = Pattern.compile(
            "Číslo bodu:\\s*([\\d.]+)", Pattern.UNICODE_CHARACTER_CLASS);
    private static final Pattern SUMMARY_PATTERN = Pattern.compile(
            "Za:\\s*(\\d+)\\s+Proti:\\s*(\\d+)\\s+Zdržali sa:\\s*(\\d+)\\s+Nehlasovali:\\s*(\\d+)\\s+Prítomní:\\s*(\\d+)",
            Pattern.UNICODE_CHARACTER_CLASS);

    // Matches category headers on their own line (after summary block)
    private static final Pattern CATEGORY_HEADER_PATTERN = Pattern.compile(
            "^\\s*(Za|Proti|Zdržali sa|Nehlasovali):\\s*$",
            Pattern.MULTILINE | Pattern.UNICODE_CHARACTER_CLASS);

    // Pre-nominal titles (before first name)
    private static final Set<String> PRE_NOMINAL_TITLES = Set.of(
            "Ing.", "Mgr.", "MUDr.", "JUDr.", "PhDr.", "PaedDr.", "RNDr.",
            "doc.", "prof.", "Bc.", "MVDr.", "ThDr.", "ICDr.");

    // Post-nominal titles (after surname, preceded by comma)
    private static final Set<String> POST_NOMINAL_TITLES = Set.of(
            "PhD.", "CSc.", "DrSc.", "Csc.", "MBA", "MPH", "DBA");

    public List<PdfVoteRecord> parsePdf(InputStream pdfStream) throws IOException {
        List<PdfVoteRecord> records = new ArrayList<>();

        try (PDDocument document = Loader.loadPDF(pdfStream.readAllBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            int totalPages = document.getNumberOfPages();
            log.info("PDF has {} pages", totalPages);

            for (int page = 1; page <= totalPages; page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String pageText = stripper.getText(document);

                PdfVoteRecord record = parsePage(pageText, page);
                if (record != null) {
                    records.add(record);
                } else {
                    log.debug("Page {} did not contain a valid voting record", page);
                }
            }
        }

        log.info("Parsed {} voting records from PDF", records.size());
        return records;
    }

    private PdfVoteRecord parsePage(String pageText, int pageNumber) {
        Matcher voteNumberMatcher = VOTE_NUMBER_PATTERN.matcher(pageText);
        if (!voteNumberMatcher.find()) {
            return null;
        }

        PdfVoteRecord record = new PdfVoteRecord();
        record.voteNumber = Integer.parseInt(voteNumberMatcher.group(1));

        // Agenda item number
        Matcher agendaItemMatcher = AGENDA_ITEM_NUMBER_PATTERN.matcher(pageText);
        if (agendaItemMatcher.find()) {
            record.agendaItemNumber = agendaItemMatcher.group(1);
            if (record.agendaItemNumber.endsWith(".")) {
                record.agendaItemNumber = record.agendaItemNumber.substring(0,
                        record.agendaItemNumber.length() - 1);
            }
        }

        // Description
        record.description = extractDescription(pageText, voteNumberMatcher.end());

        // Summary counts
        Matcher summaryMatcher = SUMMARY_PATTERN.matcher(pageText);
        if (summaryMatcher.find()) {
            record.votedFor = Integer.parseInt(summaryMatcher.group(1));
            record.votedAgainst = Integer.parseInt(summaryMatcher.group(2));
            record.abstained = Integer.parseInt(summaryMatcher.group(3));
            record.notVoted = Integer.parseInt(summaryMatcher.group(4));
            record.present = Integer.parseInt(summaryMatcher.group(5));
        } else {
            log.warn("No summary counts found on page {} (vote #{})", pageNumber, record.voteNumber);
            return null;
        }

        // Extract names from category sections
        String afterSummary = pageText.substring(summaryMatcher.end());
        Map<String, String> categorySections = extractCategorySections(afterSummary);

        record.forNames = splitNames(categorySections.getOrDefault("Za", ""));
        record.againstNames = splitNames(categorySections.getOrDefault("Proti", ""));
        record.abstainNames = splitNames(categorySections.getOrDefault("Zdržali sa", ""));
        record.notVotedNames = splitNames(categorySections.getOrDefault("Nehlasovali", ""));

        log.debug("Page {}: vote #{}, item {}, Za={}/{}, Proti={}/{}, Zdržali={}/{}, Nehlasovali={}/{}",
                pageNumber, record.voteNumber, record.agendaItemNumber,
                record.votedFor, record.forNames.size(),
                record.votedAgainst, record.againstNames.size(),
                record.abstained, record.abstainNames.size(),
                record.notVoted, record.notVotedNames.size());

        return record;
    }

    private String extractDescription(String pageText, int searchFrom) {
        Matcher summaryMatcher = SUMMARY_PATTERN.matcher(pageText);
        if (!summaryMatcher.find(searchFrom)) {
            return null;
        }

        String between = pageText.substring(searchFrom, summaryMatcher.start()).trim();
        between = between.replaceAll("(?m)^Číslo bodu:.*$", "").trim();
        between = between.replaceAll("\\s+", " ").trim();
        return between.isEmpty() ? null : between;
    }

    /**
     * Find category headers in the text after summary and extract the text
     * between each header and the next one (or end of meaningful content).
     * Categories with 0 votes are simply absent from the PDF.
     */
    Map<String, String> extractCategorySections(String text) {
        Map<String, String> sections = new LinkedHashMap<>();
        Matcher m = CATEGORY_HEADER_PATTERN.matcher(text);

        List<String> categoryNames = new ArrayList<>();
        List<Integer> categoryEnds = new ArrayList<>(); // position after the header line

        while (m.find()) {
            categoryNames.add(m.group(1));
            categoryEnds.add(m.end());
        }

        for (int i = 0; i < categoryNames.size(); i++) {
            int textStart = categoryEnds.get(i);
            int textEnd;
            if (i + 1 < categoryNames.size()) {
                // Find the start of the next category header line
                // (search backward from next category's end to find its line start)
                textEnd = text.lastIndexOf(categoryNames.get(i + 1) + ":",
                        categoryEnds.get(i + 1));
                // Go to the beginning of that line
                while (textEnd > 0 && text.charAt(textEnd - 1) != '\n') {
                    textEnd--;
                }
            } else {
                textEnd = text.length();
            }
            sections.put(categoryNames.get(i), text.substring(textStart, textEnd));
        }

        return sections;
    }

    /**
     * Split flowing text of names into individual person names.
     * Uses a state machine that tracks pre-nominal titles, first name,
     * surname, and post-nominal titles to determine boundaries.
     *
     * Examples of names in the text:
     * - "Lukáš Anderko" (no titles)
     * - "MUDr. Peter Bandura, PhD." (pre + post nominal)
     * - "doc. PhDr. Štefánia Andraščíková, PhD., MPH" (multiple pre + post)
     * - "Richard Dubovický, MBA" (post nominal only)
     */
    List<String> splitNames(String text) {
        String joined = text.replaceAll("\\n", " ").replaceAll("\\s+", " ").trim();
        if (joined.isEmpty()) return List.of();

        String[] tokens = joined.split("\\s+");
        List<String> names = new ArrayList<>();
        List<String> currentTokens = new ArrayList<>();
        int nonTitleCount = 0; // count of first name + surname tokens in current name

        for (String token : tokens) {
            String clean = token.replaceAll(",$", "");

            if (isPreNominalTitle(clean)) {
                // Pre-nominal title: if current name already has first+last, emit it
                if (nonTitleCount >= 2) {
                    emitName(names, currentTokens);
                    currentTokens = new ArrayList<>();
                    nonTitleCount = 0;
                }
                currentTokens.add(token);
            } else if (isPostNominalTitle(clean)) {
                // Post-nominal title: always belongs to current person
                currentTokens.add(token);
            } else {
                // Regular word (first name or surname)
                if (nonTitleCount >= 2) {
                    // Current person already has first name + surname,
                    // this new word starts a new person
                    emitName(names, currentTokens);
                    currentTokens = new ArrayList<>();
                    nonTitleCount = 0;
                }
                currentTokens.add(token);
                nonTitleCount++;
            }
        }

        if (!currentTokens.isEmpty()) {
            emitName(names, currentTokens);
        }

        return names;
    }

    private void emitName(List<String> names, List<String> tokens) {
        String name = String.join(" ", tokens).trim();
        // Clean trailing commas from the whole name
        if (name.endsWith(",")) {
            name = name.substring(0, name.length() - 1).trim();
        }
        if (!name.isEmpty()) {
            names.add(name);
        }
    }

    private boolean isPreNominalTitle(String token) {
        return PRE_NOMINAL_TITLES.contains(token);
    }

    private boolean isPostNominalTitle(String token) {
        return POST_NOMINAL_TITLES.contains(token);
    }

    /**
     * Internal DTO for a single voting record parsed from PDF.
     */
    public static class PdfVoteRecord {
        public int voteNumber;
        public String agendaItemNumber;
        public String description;
        public int votedFor;
        public int votedAgainst;
        public int abstained;
        public int notVoted;
        public int present;
        public List<String> forNames = new ArrayList<>();
        public List<String> againstNames = new ArrayList<>();
        public List<String> abstainNames = new ArrayList<>();
        public List<String> notVotedNames = new ArrayList<>();
    }
}
