package org.blackbell.polls.source.crawler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * Created by korcak@esten.sk on 8. 9. 2019.
 */
public class PresovCouncilMemberMatcher {
    private static final Logger log = LoggerFactory.getLogger(PresovCouncilMemberMatcher.class);

    public static String loadValue(Matcher matcher, String name) {
        String result = "";
        switch(name) {
            case "email":
                for (String group : new String[] {"email", "email2"}) {
                    if (!isNullOrEmpty(matcher.group(group))) {
                        result += readGroup(matcher, group) + ", ";
                    }
                }
                result = result.substring(0, result.length() > 1 ? result.length()-2 : result.length());
                break;
            case "candidateparties":
                if (matcher.group("candidateparties") != null && !matcher.group("candidateparties").isEmpty()) {
                    result = readGroup(matcher, "candidateparties");
//                } else if (matcher.group("candidateparty") != null && !matcher.group("candidateparty").isEmpty()) {
//                    result = readGroup(matcher, "candidateparty");
//                } else {
//                    log.info("CANDIDATEPARTIES NOT FOUND IN: " + matcher.group());
                }
                break;
            default:
                result = readGroup(matcher, name);
        }
        log.info("LOAD VALUE: {} -> {}", name, result);
        return result;
    }

    private static String readGroup(Matcher matcher, String name) {
        if (matcher != null && !isNullOrEmpty(matcher.group(name))) {
            return matcher.group(name);
        }
        return null;
    }

}
