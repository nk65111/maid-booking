package com.rupeek.maidbooking.maid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/maids")
public class MaidController {

    private final MaidService maidService;

    public MaidController(MaidService maidService) {
        this.maidService = maidService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Maid onboard(@RequestBody OnboardMaidRequest request) {
        return maidService.onboard(request);
    }

    @GetMapping("/{id}")
    public Maid get(@PathVariable String id) {
        return maidService.get(id);
    }
}
