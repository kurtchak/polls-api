package org.blackbell.polls.controllers;

import org.blackbell.polls.integrity.DataIntegrityReport;
import org.blackbell.polls.integrity.DataIntegrityService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
public class AdminDataIntegrityController {

    private final DataIntegrityService service;

    public AdminDataIntegrityController(DataIntegrityService service) {
        this.service = service;
    }

    @GetMapping("/data-integrity")
    public DataIntegrityReport check(@RequestParam(required = false) String town) {
        return service.runAllChecks(town);
    }
}
