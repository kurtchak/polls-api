package org.blackbell.polls.source.crawler;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.blackbell.polls.source.crawler.PresovCouncilMemberCrawler.MEMBER_DETAIL_RE;
import static org.junit.Assert.assertTrue;

/**
 * Created by korcak@esten.sk on 8. 9. 2019.
 */
public class PresovCouncilMemberCrawlerTest {

    @Test
    public void test() {
        File workingDir = new File(new File(".").getAbsolutePath());
        try (Stream<Path> walk = Files.walk(Paths.get(workingDir.getCanonicalPath(), "src/test/resources/presov"))) {
            List<String> contents = walk.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().contains("presov_msz_2018-2022_member_"))
                    .filter(path -> path.getFileName().toString().endsWith("detail.html"))
                    .map(Path::toString).collect(Collectors.toList());
            contents.forEach(path -> {
                System.out.println(path);
                FileInputStream fis = null;
                try {
                    fis = new FileInputStream(path);
                    String data = IOUtils.toString(fis, "UTF-8");
                    assertTrue(data.matches(MEMBER_DETAIL_RE));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
