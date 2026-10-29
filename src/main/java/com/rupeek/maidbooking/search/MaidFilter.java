package com.rupeek.maidbooking.search;

import com.rupeek.maidbooking.maid.Maid;

/**
 * A new search filter is just a new bean implementing this; the search service never changes.
 */
public interface MaidFilter {

    boolean matches(Maid maid, SearchCriteria criteria);
}
