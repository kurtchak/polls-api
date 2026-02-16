package org.blackbell.polls.source.dm;

import org.junit.jupiter.api.Test;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DMPdfVotingParserTest {

    @Test
    void parseSamplePdf() throws Exception {
        DMPdfVotingParser parser = new DMPdfVotingParser();

        try (InputStream is = new FileInputStream("/tmp/test_hlasovanie.pdf");
             PrintWriter out = new PrintWriter(new FileWriter("/tmp/pdf_parse_results.txt"))) {
            List<DMPdfVotingParser.PdfVoteRecord> records = parser.parsePdf(is);

            assertFalse(records.isEmpty(), "Should parse at least one voting record");

            out.println("=== PARSED " + records.size() + " VOTING RECORDS ===\n");

            int totalMismatches = 0;
            for (DMPdfVotingParser.PdfVoteRecord r : records) {
                out.printf("Vote #%d | Bod: %s | Za: %d, Proti: %d, Zdržali: %d, Nehlasovali: %d, Prítomní: %d%n",
                        r.voteNumber, r.agendaItemNumber,
                        r.votedFor, r.votedAgainst, r.abstained, r.notVoted, r.present);
                out.printf("  Popis: %s%n", r.description != null ?
                        r.description.substring(0, Math.min(100, r.description.length())) + "..." : "(none)");
                out.printf("  Za mená (%d): %s%n", r.forNames.size(), r.forNames);
                if (!r.againstNames.isEmpty())
                    out.printf("  Proti mená (%d): %s%n", r.againstNames.size(), r.againstNames);
                if (!r.abstainNames.isEmpty())
                    out.printf("  Zdržali mená (%d): %s%n", r.abstainNames.size(), r.abstainNames);
                if (!r.notVotedNames.isEmpty())
                    out.printf("  Nehlasovali mená (%d): %s%n", r.notVotedNames.size(), r.notVotedNames);

                boolean mismatch = false;
                if (r.votedFor != r.forNames.size()) {
                    out.printf("  *** MISMATCH: Za count=%d but names=%d%n", r.votedFor, r.forNames.size());
                    mismatch = true;
                }
                if (r.votedAgainst != r.againstNames.size()) {
                    out.printf("  *** MISMATCH: Proti count=%d but names=%d%n", r.votedAgainst, r.againstNames.size());
                    mismatch = true;
                }
                if (r.abstained != r.abstainNames.size()) {
                    out.printf("  *** MISMATCH: Zdržali count=%d but names=%d%n", r.abstained, r.abstainNames.size());
                    mismatch = true;
                }
                if (r.notVoted != r.notVotedNames.size()) {
                    out.printf("  *** MISMATCH: Nehlasovali count=%d but names=%d%n", r.notVoted, r.notVotedNames.size());
                    mismatch = true;
                }
                if (mismatch) totalMismatches++;
                out.println();
            }

            out.printf("=== TOTAL: %d records, %d mismatches ===%n", records.size(), totalMismatches);
            out.flush();

            // Basic sanity checks
            DMPdfVotingParser.PdfVoteRecord first = records.get(0);
            assertEquals(1, first.voteNumber, "First vote should be #1");
            assertTrue(first.present > 0, "Should have present count");
        }
    }
}
