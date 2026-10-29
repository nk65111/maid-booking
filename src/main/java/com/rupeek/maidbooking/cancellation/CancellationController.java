package com.rupeek.maidbooking.cancellation;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rupeek.maidbooking.booking.Booking;

@RestController
@RequestMapping("/bookings/{bookingId}")
public class CancellationController {

    private final CancellationService cancellationService;

    public CancellationController(CancellationService cancellationService) {
        this.cancellationService = cancellationService;
    }

    @PostMapping("/cancel")
    public Booking cancelBooking(@PathVariable String bookingId) {
        return cancellationService.cancelBooking(bookingId);
    }

    @PostMapping("/occurrences/{occurrenceId}/cancel")
    public Booking cancelOccurrence(@PathVariable String bookingId, @PathVariable String occurrenceId) {
        return cancellationService.cancelOccurrence(bookingId, occurrenceId);
    }
}
