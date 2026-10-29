package com.rupeek.maidbooking;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * End-to-end through HTTP: onboard -> search -> book -> pay -> cancel, plus error mapping.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class MaidBookingApiTest {

    private static final String MAID_JSON = """
            {
              "name": "Asha",
              "phone": "9876543210",
              "locality": "HSR Layout",
              "gender": "FEMALE",
              "hourlyRates": {"CLEANING": 200, "COOKING": 300},
              "availability": [
                {"day": "MONDAY", "from": "07:00", "to": "20:00"},
                {"day": "TUESDAY", "from": "07:00", "to": "20:00"},
                {"day": "WEDNESDAY", "from": "07:00", "to": "20:00"},
                {"day": "THURSDAY", "from": "07:00", "to": "20:00"},
                {"day": "FRIDAY", "from": "07:00", "to": "20:00"},
                {"day": "SATURDAY", "from": "07:00", "to": "20:00"},
                {"day": "SUNDAY", "from": "07:00", "to": "20:00"}
              ],
              "rating": 4.6
            }
            """;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    private final LocalDateTime start = LocalDate.now(Fixture.ZONE).plusDays(3).atTime(10, 0);

    @Test
    void fullBookingLifecycle() throws Exception {
        String maidId = onboard().get("id").asText();

        mvc.perform(get("/maids/search")
                        .param("locality", "hsr layout")
                        .param("serviceType", "COOKING")
                        .param("start", start.toString())
                        .param("durationMinutes", "120"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        String bookingId = book(maidId).get("id").asText();

        mvc.perform(get("/maids/search").param("start", start.toString()))
                .andExpect(jsonPath("$", hasSize(0)));

        mvc.perform(post("/bookings/{id}/payments", bookingId)
                        .header("Idempotency-Key", "pay-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"method\": \"UPI\", \"instrument\": \"asha@okicici\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.amount").value(600.00));

        mvc.perform(get("/bookings/{id}", bookingId))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        mvc.perform(post("/bookings/{id}/cancel", bookingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.occurrences[0].refundedAmount").value(600.00));

        mvc.perform(get("/maids/search").param("start", start.toString()))
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void doubleBookingIsAConflict() throws Exception {
        String maidId = onboard().get("id").asText();
        book(maidId);

        mvc.perform(post("/bookings").contentType(MediaType.APPLICATION_JSON).content(bookingJson(maidId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("not available")));
    }

    @Test
    void badInputIsMappedToClearErrors() throws Exception {
        mvc.perform(post("/maids").contentType(MediaType.APPLICATION_JSON)
                        .content(MAID_JSON.replace("\"07:00\", \"to\": \"20:00\"}", "\"21:00\", \"to\": \"20:00\"}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("must end after it starts")));

        mvc.perform(post("/maids").contentType(MediaType.APPLICATION_JSON)
                        .content(MAID_JSON.replace("FEMALE", "ALIEN")))
                .andExpect(status().isBadRequest());

        mvc.perform(get("/maids/search").param("serviceType", "GARDENING"))
                .andExpect(status().isBadRequest());

        mvc.perform(get("/bookings/does-not-exist"))
                .andExpect(status().isNotFound());

        String bookingId = book(onboard().get("id").asText()).get("id").asText();
        mvc.perform(post("/bookings/{id}/payments", bookingId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"method\": \"CARD\", \"instrument\": \"4111111111111111\"}"))
                .andExpect(status().isBadRequest());
    }

    private JsonNode onboard() throws Exception {
        String body = mvc.perform(post("/maids").contentType(MediaType.APPLICATION_JSON).content(MAID_JSON))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return mapper.readTree(body);
    }

    private JsonNode book(String maidId) throws Exception {
        String body = mvc.perform(post("/bookings").contentType(MediaType.APPLICATION_JSON).content(bookingJson(maidId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"))
                .andReturn().getResponse().getContentAsString();
        return mapper.readTree(body);
    }

    private String bookingJson(String maidId) {
        return """
                {"customerId": "cust-1", "maidId": "%s", "serviceType": "COOKING",
                 "type": "SCHEDULED", "durationMinutes": 120, "startAt": "%s"}
                """.formatted(maidId, start);
    }
}
