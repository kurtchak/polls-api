package org.blackbell.polls.context;

import org.blackbell.polls.meetings.model.Season;
import org.springframework.web.context.annotation.SessionScope;

import java.util.Map;

/**
 * Created by Ján Korčák on 5.3.2017.
 * email: korcak@esten.sk
 */
@SessionScope
public class SessionContext {
    private Map<String, Season> seasonsMap;
//    private Map<String, Meeting> meetingsMap;
}
