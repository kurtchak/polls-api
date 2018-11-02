package org.blackbell.polls.controllers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class InfoController {

    @Value("${info.build.version}")
    private String version;

    @RequestMapping({"version"})
    public String version() {
        return version;
    }

}
