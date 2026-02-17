package org.blackbell.polls.source.crawler;

import org.blackbell.polls.domain.model.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.blackbell.polls.common.PollsUtils.*;

/**
 * Scraper for Poprad council members from poprad.sk/kontakty page.
 * Parses: name (with titles), phone, email, electoral district.
 * No photos or clubs available on this page.
 */
public class PopradCouncilMemberScraper {
    private static final Logger log = LoggerFactory.getLogger(PopradCouncilMemberScraper.class);

    private final String membersUrl;
    private final int timeoutMs;

    public PopradCouncilMemberScraper(String membersUrl, int timeoutMs) {
        this.membersUrl = membersUrl;
        this.timeoutMs = timeoutMs;
    }

    public List<CouncilMember> scrapeMembers(Town town, Season season, Institution institution) {
        List<CouncilMember> members = new ArrayList<>();

        try {
            log.info("Fetching Poprad council members from: {}", membersUrl);
            Document document = Jsoup.connect(membersUrl)
                    .userAgent("Mozilla/5.0")
                    .timeout(timeoutMs)
                    .get();

            Elements contactBoxes = document.select("div.contact-box");
            log.info("Found {} contact boxes", contactBoxes.size());

            for (Element box : contactBoxes) {
                try {
                    CouncilMember member = parseContactBox(box, town, season, institution);
                    if (member != null) {
                        members.add(member);
                    }
                } catch (Exception e) {
                    log.error("Error parsing contact box: {}", e.getMessage(), e);
                }
            }

            log.info("Parsed {} Poprad council members", members.size());

        } catch (IOException e) {
            log.error("Failed to fetch Poprad council members from {}: {}", membersUrl, e.getMessage());
        }

        return members;
    }

    /**
     * Parse a single contact-box div into a CouncilMember.
     *
     * HTML structure:
     * <pre>
     * &lt;div class="contact-box"&gt;
     *   &lt;p class="g-line-height-1_2"&gt;
     *     &lt;strong&gt;Ing. arch. Martin Baloga PhD.&lt;/strong&gt;
     *     &lt;span class="position"&gt;Volebný obvod č. 1&lt;/span&gt;
     *   &lt;/p&gt;
     *   &lt;p&gt;
     *     &lt;a href="tel:+421..."&gt;phone&lt;/a&gt;
     *     &lt;a href="mailto:..."&gt;email&lt;/a&gt;
     *   &lt;/p&gt;
     * &lt;/div&gt;
     * </pre>
     */
    private CouncilMember parseContactBox(Element box, Town town, Season season, Institution institution) {
        // Extract full name from <strong> element
        Element strongEl = box.selectFirst("strong");
        if (strongEl == null) {
            return null;
        }
        String fullName = strongEl.text().trim();
        if (fullName.isEmpty()) {
            return null;
        }

        // Extract phone from tel: link
        Element phoneLink = box.selectFirst("a[href^=tel:]");
        String phone = phoneLink != null ? phoneLink.text().trim() : null;

        // Extract email from mailto: link
        Element emailLink = box.selectFirst("a[href^=mailto:]");
        String email = emailLink != null ? emailLink.text().trim() : null;

        // Extract electoral district from span.position
        Element positionEl = box.selectFirst("span.position");
        String district = positionEl != null ? positionEl.text().trim() : null;

        // Create Politician
        Politician politician = new Politician();
        politician.setRef(generateUniqueKeyReference());
        politician.setName(toSimpleName(fullName));
        politician.setTitles(getTitles(fullName));
        politician.setPhone(phone);
        politician.setEmail(email);

        // Create CouncilMember
        CouncilMember member = new CouncilMember();
        member.setRef(generateUniqueKeyReference());
        member.setPolitician(politician);
        member.setSeason(season);
        member.setTown(town);
        member.setInstitution(institution);
        if (district != null) {
            member.setDescription(district);
        }

        log.info("Parsed Poprad member: {} (email: {}, phone: {}, district: {})",
                deAccent(politician.getName()), email, phone, district);

        return member;
    }
}
