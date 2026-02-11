package org.blackbell.polls.domain.model;

import org.blackbell.polls.domain.model.embeddable.VotesCount;
import org.blackbell.polls.domain.model.enums.MajorityType;
import org.blackbell.polls.domain.model.enums.VoteResult;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PollTest {

    private Poll createPoll(int voters, int votedFor, int votedAgainst, int abstain, int notVoted, int absent) {
        Poll poll = new Poll();
        poll.setVoters(voters);
        VotesCount vc = new VotesCount();
        vc.setVotedFor(votedFor);
        vc.setVotedAgainst(votedAgainst);
        vc.setAbstain(abstain);
        vc.setNotVoted(notVoted);
        vc.setAbsent(absent);
        poll.setVotesCount(vc);
        return poll;
    }

    private Poll createPollWithAgendaItem(String agendaItemName, int voters, int votedFor, int votedAgainst, int abstain, int notVoted, int absent) {
        Poll poll = createPoll(voters, votedFor, votedAgainst, abstain, notVoted, absent);
        AgendaItem agendaItem = new AgendaItem();
        agendaItem.setName(agendaItemName);
        Meeting meeting = new Meeting();
        meeting.setName("test meeting");
        agendaItem.setMeeting(meeting);
        poll.setAgendaItem(agendaItem);
        return poll;
    }

    // ========== MajorityType detection ==========

    @Nested
    class MajorityTypeDetection {

        @Test
        void defaultIsSimpleMajority() {
            assertEquals(MajorityType.SIMPLE_MAJORITY,
                    MajorityType.detectFromAgendaItemName("Správa z kontroly plnenia uznesení"));
        }

        @Test
        void nullName_defaultSimpleMajority() {
            assertEquals(MajorityType.SIMPLE_MAJORITY,
                    MajorityType.detectFromAgendaItemName(null));
        }

        @Test
        void blankName_defaultSimpleMajority() {
            assertEquals(MajorityType.SIMPLE_MAJORITY,
                    MajorityType.detectFromAgendaItemName(""));
        }

        @Test
        void vzn_fullName() {
            assertEquals(MajorityType.THREE_FIFTHS_PRESENT,
                    MajorityType.detectFromAgendaItemName(
                            "Návrh Všeobecne záväzného nariadenia mesta Prešov č. .../2015, ktorým sa dopĺňa VZN č. 13/"));
        }

        @Test
        void vzn_abbreviation() {
            assertEquals(MajorityType.THREE_FIFTHS_PRESENT,
                    MajorityType.detectFromAgendaItemName("Návrh VZN mesta Prešov o miestnych daniach"));
        }

        @Test
        void vzn_lowercase() {
            assertEquals(MajorityType.THREE_FIFTHS_PRESENT,
                    MajorityType.detectFromAgendaItemName("Návrh všeobecne záväzného nariadenia č. 5/2023"));
        }

        @Test
        void vetoOverride_pozastavenie() {
            assertEquals(MajorityType.THREE_FIFTHS_ALL,
                    MajorityType.detectFromAgendaItemName(
                            "Potvrdenie pozastaveného uznesenia mestského zastupiteľstva č. 123/2023"));
        }

        @Test
        void vetoOverride_prelomenie() {
            assertEquals(MajorityType.THREE_FIFTHS_ALL,
                    MajorityType.detectFromAgendaItemName("Prelomenie veta primátora k uzneseniu č. 456/2024"));
        }

        @Test
        void auditorElection() {
            assertEquals(MajorityType.ABSOLUTE_MAJORITY,
                    MajorityType.detectFromAgendaItemName("Voľba hlavného kontrolóra mesta Prešov"));
        }

        @Test
        void auditorDismissal() {
            assertEquals(MajorityType.ABSOLUTE_MAJORITY,
                    MajorityType.detectFromAgendaItemName("Odvolanie hlavného kontrolóra mesta Prešov"));
        }

        @Test
        void auditorElection_zvolenie() {
            assertEquals(MajorityType.ABSOLUTE_MAJORITY,
                    MajorityType.detectFromAgendaItemName("Zvolenie hlavného kontrolóra"));
        }

        @Test
        void agendaChange() {
            assertEquals(MajorityType.ABSOLUTE_MAJORITY,
                    MajorityType.detectFromAgendaItemName("Zmena programu zasadnutia mestského zastupiteľstva"));
        }

        @Test
        void agendaChange_navrhu() {
            assertEquals(MajorityType.ABSOLUTE_MAJORITY,
                    MajorityType.detectFromAgendaItemName("Zmena návrhu programu rokovania"));
        }

        @Test
        void referendum() {
            assertEquals(MajorityType.ABSOLUTE_MAJORITY,
                    MajorityType.detectFromAgendaItemName(
                            "Vyhlásenie referenda o odvolaní primátora mesta"));
        }

        @Test
        void referendum_starosta() {
            assertEquals(MajorityType.ABSOLUTE_MAJORITY,
                    MajorityType.detectFromAgendaItemName(
                            "Návrh na vyhlásenie referenda o odvolaní starostu obce"));
        }

        @Test
        void majetkovePrevodySuDefault() {
            assertEquals(MajorityType.SIMPLE_MAJORITY,
                    MajorityType.detectFromAgendaItemName("Návrh na schválenie majetkových prevodov"));
        }

        @Test
        void rozpocetJeDefault() {
            assertEquals(MajorityType.SIMPLE_MAJORITY,
                    MajorityType.detectFromAgendaItemName("Návrh rozpočtového opatrenia č. 12"));
        }

        @Test
        void pollWithAgendaItem_detectsType() {
            Poll poll = createPollWithAgendaItem(
                    "Návrh Všeobecne záväzného nariadenia mesta Prešov",
                    31, 20, 5, 3, 0, 3);
            assertEquals(MajorityType.THREE_FIFTHS_PRESENT, poll.getMajorityType());
        }

        @Test
        void pollWithoutAgendaItem_defaultsToSimple() {
            Poll poll = createPoll(31, 20, 5, 3, 0, 3);
            assertEquals(MajorityType.SIMPLE_MAJORITY, poll.getMajorityType());
        }
    }

    // ========== Simple majority (>50% of present) ==========

    @Nested
    class SimpleMajorityResult {

        @Test
        void clearMajority_passed() {
            // 25 poslancov, 5 neprítomných = 20 prítomných, treba > 10
            Poll poll = createPoll(25, 15, 3, 2, 0, 5);
            assertEquals(VoteResult.PASSED, poll.getResult());
        }

        @Test
        void clearMajority_rejected() {
            Poll poll = createPoll(25, 5, 10, 5, 0, 5);
            assertEquals(VoteResult.REJECTED, poll.getResult());
        }

        @Test
        void moreForThanAgainst_butNotMajority_rejected() {
            // Kľúčový bug fix: za > proti, ale nie nadpolovičná väčšina prítomných
            Poll poll = createPoll(25, 8, 5, 7, 0, 5);
            assertEquals(VoteResult.REJECTED, poll.getResult());
        }

        @Test
        void exactlyHalf_rejected() {
            // 20 prítomných, 10 za = presne 50%, nie nadpolovičná
            Poll poll = createPoll(20, 10, 5, 5, 0, 0);
            assertEquals(VoteResult.REJECTED, poll.getResult());
        }

        @Test
        void oneMoreThanHalf_passed() {
            Poll poll = createPoll(20, 11, 5, 4, 0, 0);
            assertEquals(VoteResult.PASSED, poll.getResult());
        }

        @Test
        void oddNumberPresent_passed() {
            // 21 prítomných, 11 za (> 10.5)
            Poll poll = createPoll(25, 11, 5, 5, 0, 4);
            assertEquals(VoteResult.PASSED, poll.getResult());
        }

        @Test
        void oddNumberPresent_rejected() {
            // 21 prítomných, 10 za (≤ 10.5)
            Poll poll = createPoll(25, 10, 5, 6, 0, 4);
            assertEquals(VoteResult.REJECTED, poll.getResult());
        }

        @Test
        void unanimousPassed() {
            Poll poll = createPoll(20, 20, 0, 0, 0, 0);
            assertEquals(VoteResult.PASSED, poll.getResult());
        }

        @Test
        void unanimousRejected() {
            Poll poll = createPoll(20, 0, 20, 0, 0, 0);
            assertEquals(VoteResult.REJECTED, poll.getResult());
        }

        @Test
        void allAbstained_rejected() {
            Poll poll = createPoll(20, 0, 0, 20, 0, 0);
            assertEquals(VoteResult.REJECTED, poll.getResult());
        }

        @Test
        void nullVotesCount_returnsNull() {
            Poll poll = new Poll();
            poll.setVoters(25);
            assertNull(poll.getResult());
        }

        @Test
        void allAbsent_returnsNull() {
            Poll poll = createPoll(20, 0, 0, 0, 0, 20);
            assertNull(poll.getResult());
        }
    }

    // ========== THREE_FIFTHS_PRESENT (3/5 of present — VZN) ==========

    @Nested
    class ThreeFifthsPresentResult {

        @Test
        void vzn_passed_clearMajority() {
            // 31 poslancov, 2 neprítomní = 29 prítomných, treba > 29*3/5 = > 17.4, teda 18+
            Poll poll = createPollWithAgendaItem(
                    "Návrh Všeobecne záväzného nariadenia mesta Prešov č. 5/2015",
                    31, 29, 0, 0, 0, 2);
            assertEquals(VoteResult.PASSED, poll.getResult());
        }

        @Test
        void vzn_passed_exactThreshold() {
            // 30 prítomných, 3/5 = 18, treba > 18, teda 19+
            Poll poll = createPollWithAgendaItem(
                    "Návrh VZN č. 3/2024",
                    31, 19, 5, 6, 0, 1);
            assertEquals(VoteResult.PASSED, poll.getResult());
        }

        @Test
        void vzn_rejected_exactlyThreeFifths() {
            // 30 prítomných, 3/5 = 18, presne 18 nie je > 18
            Poll poll = createPollWithAgendaItem(
                    "Návrh VZN č. 3/2024",
                    31, 18, 5, 7, 0, 1);
            assertEquals(VoteResult.REJECTED, poll.getResult());
        }

        @Test
        void vzn_rejected_simpleMajorityNotEnough() {
            // 25 prítomných, 3/5 = 15, treba > 15. Má 14 za — aj keď to je > 50% (> 12.5), pre VZN nestačí
            Poll poll = createPollWithAgendaItem(
                    "Návrh Všeobecne záväzného nariadenia",
                    31, 14, 5, 6, 0, 6);
            assertEquals(VoteResult.REJECTED, poll.getResult());
        }

        @Test
        void vzn_passed_oddPresent() {
            // 25 prítomných, 3/5 z 25 = 15, treba > 15, teda 16+
            Poll poll = createPollWithAgendaItem(
                    "Návrh VZN o miestnych daniach",
                    31, 16, 3, 6, 0, 6);
            assertEquals(VoteResult.PASSED, poll.getResult());
        }

        @Test
        void vzn_rejected_oddPresent() {
            // 25 prítomných, 3/5 z 25 = 15, presne 15 nie je > 15
            Poll poll = createPollWithAgendaItem(
                    "Návrh VZN o miestnych daniach",
                    31, 15, 3, 7, 0, 6);
            assertEquals(VoteResult.REJECTED, poll.getResult());
        }
    }

    // ========== THREE_FIFTHS_ALL (3/5 of all — veto override) ==========

    @Nested
    class ThreeFifthsAllResult {

        @Test
        void vetoOverride_passed() {
            // 31 všetkých, 3/5 z 31 = 18.6, treba > 18.6, teda 19+
            Poll poll = createPollWithAgendaItem(
                    "Potvrdenie pozastaveného uznesenia mestského zastupiteľstva č. 123/2023",
                    31, 19, 5, 3, 0, 4);
            assertEquals(VoteResult.PASSED, poll.getResult());
        }

        @Test
        void vetoOverride_rejected_notEnoughForAll() {
            // 31 všetkých, 3/5 z 31 = 18.6, 18 za nestačí
            Poll poll = createPollWithAgendaItem(
                    "Potvrdenie pozastaveného uznesenia č. 456/2024",
                    31, 18, 5, 4, 0, 4);
            assertEquals(VoteResult.REJECTED, poll.getResult());
        }

        @Test
        void vetoOverride_rejected_majorityOfPresentButNotAll() {
            // 31 všetkých, 10 neprítomných = 21 prítomných
            // 15 za je > 50% prítomných aj > 3/5 prítomných (12.6), ale nie > 3/5 všetkých (18.6)
            Poll poll = createPollWithAgendaItem(
                    "Prelomenie veta primátora k uzneseniu č. 789/2024",
                    31, 15, 3, 3, 0, 10);
            assertEquals(VoteResult.REJECTED, poll.getResult());
        }
    }

    // ========== ABSOLUTE_MAJORITY (>50% of all) ==========

    @Nested
    class AbsoluteMajorityResult {

        @Test
        void auditorElection_passed() {
            // 31 všetkých, treba > 15.5, teda 16+
            Poll poll = createPollWithAgendaItem(
                    "Voľba hlavného kontrolóra mesta Prešov",
                    31, 20, 5, 3, 0, 3);
            assertEquals(VoteResult.PASSED, poll.getResult());
        }

        @Test
        void auditorElection_rejected_exactlyHalf() {
            // 30 všetkých, treba > 15, presne 15 nestačí
            Poll poll = createPollWithAgendaItem(
                    "Voľba hlavného kontrolóra mesta Prešov",
                    30, 15, 5, 5, 0, 5);
            assertEquals(VoteResult.REJECTED, poll.getResult());
        }

        @Test
        void auditorElection_rejected_majorityOfPresentButNotAll() {
            // 31 všetkých, 10 neprítomných = 21 prítomných
            // 14 za je > 50% prítomných (> 10.5), ale nie > 50% všetkých (> 15.5)
            Poll poll = createPollWithAgendaItem(
                    "Odvolanie hlavného kontrolóra mesta",
                    31, 14, 3, 4, 0, 10);
            assertEquals(VoteResult.REJECTED, poll.getResult());
        }

        @Test
        void auditorElection_passed_justAbove() {
            // 31 všetkých, treba > 15.5, 16 stačí
            Poll poll = createPollWithAgendaItem(
                    "Zvolenie hlavného kontrolóra",
                    31, 16, 5, 5, 0, 5);
            assertEquals(VoteResult.PASSED, poll.getResult());
        }
    }

    // ========== Real data from DM API ==========

    @Nested
    class RealDataExamples {

        @Test
        void presov_2015_vzn_passed() {
            // Reálne dáta z 12. zasadnutia MsZ Prešov, bod 3
            Poll poll = createPollWithAgendaItem(
                    "Návrh Všeobecne záväzného nariadenia mesta Prešov č. .../2015, ktorým sa dopĺňa Všeobecne záväzné nariadenie mesta Prešov č. 13/",
                    31, 29, 0, 0, 0, 2);
            assertEquals(MajorityType.THREE_FIFTHS_PRESENT, poll.getMajorityType());
            // 29 prítomných, 3/5 = 17.4, 29 za > 17.4
            assertEquals(VoteResult.PASSED, poll.getResult());
        }

        @Test
        void presov_2015_regularVote() {
            // Reálne dáta z 12. zasadnutia MsZ Prešov, bod 4
            Poll poll = createPollWithAgendaItem(
                    "Správa z kontroly plnenia uznesení Mestského zastupiteľstva mesta Prešov za 2. štvrťrok 2015",
                    31, 28, 1, 0, 1, 1);
            assertEquals(MajorityType.SIMPLE_MAJORITY, poll.getMajorityType());
            // 30 prítomných, treba > 15, 28 za
            assertEquals(VoteResult.PASSED, poll.getResult());
        }

        @Test
        void presov_2015_tightVote() {
            // Reálne dáta z bod 16.1 - 13 za, 0 proti, 5 zdržal sa, 7 nehlasoval, 6 neprítomných
            Poll poll = createPollWithAgendaItem(
                    "Informatívna správa o likvidácii obchodnej spoločnosti IPZ Prešov, a.s. v likvidácii",
                    31, 13, 0, 5, 7, 6);
            assertEquals(MajorityType.SIMPLE_MAJORITY, poll.getMajorityType());
            // 25 prítomných, treba > 12.5, 13 za — tesne PASSED
            assertEquals(VoteResult.PASSED, poll.getResult());
        }

        @Test
        void presov_2015_programVote_rejected() {
            // Reálne dáta z bod 1, hlasovanie 1 - schvaľovanie programu
            // 13 za, 0 proti, 16 zdržal sa, 0 nehlasoval, 2 neprítomní
            Poll poll = createPollWithAgendaItem(
                    "Otvorenie, určenie zapisovateľa a overovateľov zápisnice, schválenie návrhu programu.",
                    31, 13, 0, 16, 0, 2);
            // 29 prítomných, treba > 14.5, 13 za
            assertEquals(VoteResult.REJECTED, poll.getResult());
        }
    }
}
