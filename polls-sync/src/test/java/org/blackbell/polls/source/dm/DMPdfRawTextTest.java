package org.blackbell.polls.source.dm;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;

class DMPdfRawTextTest {

    @Test
    void dumpFirstPages() throws Exception {
        try (PDDocument doc = Loader.loadPDF(new File("/tmp/test_hlasovanie.pdf"));
             PrintWriter out = new PrintWriter(new FileWriter("/tmp/pdf_raw_text.txt"))) {
            PDFTextStripper stripper = new PDFTextStripper();
            for (int page = 1; page <= Math.min(5, doc.getNumberOfPages()); page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String text = stripper.getText(doc);
                out.println("=== PAGE " + page + " ===");
                for (String line : text.split("\n")) {
                    out.println("|" + line + "|");
                    out.println(" -> [" + line.replace(" ", "\u00b7") + "]");
                }
                out.println();
            }
            out.flush();
        }
    }
}
