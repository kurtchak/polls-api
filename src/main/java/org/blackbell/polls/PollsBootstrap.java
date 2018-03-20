package org.blackbell.polls;

/**
 * Created by Ján Korčák on 20.3.2018.
 * email: korcak@esten.sk
 */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

@Component
public class PollsBootstrap implements ApplicationListener<ContextRefreshedEvent> {

    private static final Logger log = LoggerFactory.getLogger(PollsBootstrap.class);

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        printApplicationLabel();
    }

    public void printApplicationLabel() {
        log.info("\n" +
                "\n" +
                "______ _            _    _          _ _  ______     _ _     \n" +
                "| ___ \\ |          | |  | |        | | | | ___ \\   | | |    \n" +
                "| |_/ / | __ _  ___| | _| |__   ___| | | | |_/ /__ | | |___ \n" +
                "| ___ \\ |/ _` |/ __| |/ / '_ \\ / _ \\ | | |  __/ _ \\| | / __|\n" +
                "| |_/ / | (_| | (__|   <| |_) |  __/ | | | | | (_) | | \\__ \\\n" +
                "\\____/|_|\\__,_|\\___|_|\\_\\_.__/ \\___|_|_| \\_|  \\___/|_|_|___/\n" +
                "                                                            \n" +
                "                                                            \n" +
                "\n");

    }

}