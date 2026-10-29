package com.rupeek.maidbooking.search;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RestController;

import com.rupeek.maidbooking.maid.Maid;

@RestController
public class MaidSearchController {

    private final MaidSearchService searchService;

    public MaidSearchController(MaidSearchService searchService) {
        this.searchService = searchService;
    }

    /**
     * e.g. GET /maids/search?locality=Koramangala&serviceType=COOKING&start=2026-11-05T09:00&durationMinutes=120
     */
    @GetMapping("/maids/search")
    public List<Maid> search(@ModelAttribute SearchCriteria criteria) {
        return searchService.search(criteria);
    }
}
