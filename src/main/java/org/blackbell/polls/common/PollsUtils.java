package org.blackbell.polls.common;

import org.blackbell.polls.domain.model.Season;
import org.blackbell.polls.domain.model.Town;
import org.blackbell.polls.domain.model.enums.InstitutionType;
import org.blackbell.polls.source.crawler.PresovCouncilMemberCrawler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.text.Normalizer;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Created by Ján Korčák on 1.4.2017.
 * email: korcak@esten.sk
 */
public class PollsUtils {
    private static final Logger log = LoggerFactory.getLogger(PresovCouncilMemberCrawler.class);

    private static String cutDateStringToTFormat(String dateString) {
        return dateString.substring(0,19);
    }

    public static Date parseDMDate(String dmDate) throws ParseException {
        return Constants.FULLDATE_WITH_T_FORMAT.parse(cutDateStringToTFormat(dmDate));
    }

    public static Date parseSimpleDate(String dmDate) throws ParseException {
        return Constants.DATE_FORMAT.parse(dmDate);
    }

    public static String toFilenameForm(String name) {
        return toSimpleName(name).toLowerCase().replaceAll(" ", "_");
    }

    public static String toSimpleName(String name) {
        String result = name;
        Matcher m = Constants.TITLE_PATTERN.matcher(name);
        while (m.find()) {
            result = result.replace(m.group(), "");
        }
        return result.replaceAll(",", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    public static String toSimpleNameWithoutAccents(String name) {
        return deAccent(toSimpleName(name).replaceAll(",", ""));
    }

    public static String startWithFirstname(String fullname) {
        String[] name = fullname.split("\\s", 2);
        return String.join(" ", Arrays.asList(name[1], name[0]));
    }

    public static String getTitles(String name) {
        String result = "";
        Matcher m = Constants.TITLE_PATTERN.matcher(name);
        while (m.find()) {
            String title = !m.group().endsWith(".") ? m.group() + "." : m.group();
            result += title + ", ";
        }
        return !result.isEmpty() ? result.substring(0, result.length() - 2) : null;
    }

    public static String generateUniqueKeyReference() {
        return "" + System.nanoTime();
    }

    public static List<String> splitCleanAndTrim(String value) {
        return Arrays.stream(value.split("\\s*,\\s*|\\s+a\\s+")).map(PollsUtils::cleanAndTrim).collect(Collectors.toList());
    }

    public static String recognizeClubName(List<String> partyList) {
        return String.join("/", partyList);
    }

    public static String cleanAndTrim(String item) {
        return item.replaceAll("\\s*-\\s*", "-").replaceAll("\\s*–\\s*", "-");
    }

    public static String generateMemberKey(Town town, Season season, InstitutionType type) {
        return town.getRef() + ":" + season.getRef() + ":" + type.name();
    }

    /**
     * Convert accented letters to ascii form.
     *
     * @param str .
     * @return .
     */
    public static String deAccent(String str) {
        String nfdNormalizedString = Normalizer.normalize(str, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(nfdNormalizedString).replaceAll("");
    }

    public static void saveToFile(String filename, String content) {
        try {
            if (Files.notExists(Paths.get("samples"))) {
                Files.createDirectory(Paths.get("samples"));
            }
            Path newFile = Files.createFile(Paths.get("samples/" + filename), null);
            Files.write(newFile, content.getBytes());
            log.info("Saved file {}", filename);
        } catch (IOException e) {
            log.error("Unable to save file {}", filename);
            e.printStackTrace();
        }
    }

    public static boolean isNullOrEmpty(String value) {
        return value == null || value.isEmpty();
    }
}
