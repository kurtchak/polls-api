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
        log.info("Polls bootstrap");
    }

}