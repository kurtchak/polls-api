package org.blackbell.polls.common;

import org.junit.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by  on 9. 9. 2019.
 */
public class PollsUtilsTest {

    @Test
    public void splitCleanAndTrim() {
        List<String> parties1 = PollsUtils.splitCleanAndTrim("SMER - SD, SNS, MOST - HÍD");
        assertThat(parties1).containsExactly("SMER-SD", "SNS", "MOST-HÍD");
        List<String> parties2 = PollsUtils.splitCleanAndTrim("SaS, SME RODINA - Boris Kollár, ŠANCA, DS");
        assertThat(parties2).containsExactly("SaS", "SME RODINA-Boris Kollár", "ŠANCA", "DS");
        List<String> parties3 = PollsUtils.splitCleanAndTrim("SPOLU, Progresívne Slovensko, OKS");
        assertThat(parties3).containsExactly("SPOLU", "Progresívne Slovensko", "OKS");
        List<String> parties4 = PollsUtils.splitCleanAndTrim("nezávislí kandidáti, SPOLU - občianska demokracia, ŠANCA");
        assertThat(parties4).containsExactly("nezávislí kandidáti", "SPOLU-občianska demokracia", "ŠANCA");
    }

    @Test
    public void cleanAndTrim() {
    }
}
