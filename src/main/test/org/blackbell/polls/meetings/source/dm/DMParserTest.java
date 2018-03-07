package org.blackbell.polls.meetings.source.dm;

import org.blackbell.polls.common.PollDateUtils;
import org.blackbell.polls.meetings.model.Institution;
import org.blackbell.polls.meetings.model.Season;
import org.blackbell.polls.meetings.model.Town;
import org.blackbell.polls.meetings.source.dm.api.response.DMMeetingsResponse;
import org.blackbell.polls.meetings.source.dm.dto.MeetingDTO;
import org.blackbell.polls.meetings.source.dm.dto.SeasonDTO;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jano on 7. 3. 2018.
 */
public class DMParserTest {

    private static Town presov;
    private static DMMeetingsResponse meetingsResponse;

    @BeforeClass
    public static void init() {
        meetingsResponse = new DMMeetingsResponse();

        List<SeasonDTO> seasonsDTO = new ArrayList<>();
        SeasonDTO season0610DTO = new SeasonDTO();
        season0610DTO.setName("2006-2010");
        List<MeetingDTO> meetings0610DTO = new ArrayList<>();

        MeetingDTO meeting1DTO = new MeetingDTO();
        meeting1DTO.setName("Meeting 1");
        meeting1DTO.setDate("2004-05-03");
        meeting1DTO.setId("1");
        meetings0610DTO.add(meeting1DTO);

        MeetingDTO meeting2DTO = new MeetingDTO();
        meeting2DTO.setName("Meeting 2");
        meeting2DTO.setDate("2005-02-27");
        meeting2DTO.setId("2");
        meetings0610DTO.add(meeting2DTO);

        season0610DTO.setMeetingDTOs(meetings0610DTO);
        seasonsDTO.add(season0610DTO);

        SeasonDTO season1014DTO = new SeasonDTO();
        season1014DTO.setName("2010-2014");
        List<MeetingDTO> meetings1014DTO = new ArrayList<>();

        MeetingDTO meeting3DTO = new MeetingDTO();
        meeting3DTO.setName("Meeting 3");
        meeting3DTO.setDate("2012-11-09");
        meeting3DTO.setId("3");
        meetings1014DTO.add(meeting3DTO);

        season1014DTO.setMeetingDTOs(meetings1014DTO);
        seasonsDTO.add(season1014DTO);

        meetingsResponse.setSeasonDTOs(seasonsDTO);

        presov = new Town();
        presov.setName("Prešov");
        presov.setRef("presov");
    }

    @Test
    public void testParseSeasons() throws Exception {
        DMParser.parseSeasons(presov, Institution.ZASTUPITELSTVO, meetingsResponse.getSeasonDTOs());

        List<Season> seasons = presov.getSeasons();
        assert seasons != null;
        assert seasons.size() == 2;

        assert seasons.get(0).getName().equals("2006-2010");
        assert seasons.get(0).getInstitution().equals(Institution.ZASTUPITELSTVO);

        assert seasons.get(0).getMeetings().get(0).getExtId().equals("1");
        assert seasons.get(0).getMeetings().get(0).getName().equals("Meeting 1");
        assert seasons.get(0).getMeetings().get(0).getDate().equals(PollDateUtils.parseSimpleDate("2004-05-03"));

        assert seasons.get(0).getMeetings().get(1).getExtId().equals("2");
        assert seasons.get(0).getMeetings().get(1).getName().equals("Meeting 2");
        assert seasons.get(0).getMeetings().get(1).getDate().equals(PollDateUtils.parseSimpleDate("2005-02-27"));

        assert seasons.get(1).getName().equals("2010-2014");
        assert seasons.get(1).getInstitution().equals(Institution.ZASTUPITELSTVO);

        assert seasons.get(1).getMeetings().get(0).getExtId().equals("3");
        assert seasons.get(1).getMeetings().get(0).getName().equals("Meeting 3");
        assert seasons.get(1).getMeetings().get(0).getDate().equals(PollDateUtils.parseSimpleDate("2012-11-09"));
    }

    @Test
    public void testParseMeetingResponse() throws Exception {

    }

    @Test
    public void testParsePollDetail() throws Exception {

    }
}