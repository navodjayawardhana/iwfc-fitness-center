package com.iwfc.infrastructure.web;

import com.iwfc.application.facade.IwfcFacade;
import com.iwfc.infrastructure.IwfcBootstrap;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The REST adapter is thin: these tests check HTTP shape and the mapping of custom exceptions to status codes. */
class ApiControllersTest {

    private static final String NEXT_MONDAY =
            LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY)).toString();

    private final IwfcFacade system = IwfcBootstrap.seeded();
    private final MockMvc mvc = MockMvcBuilders
            .standaloneSetup(new AccountController(system), new EquipmentController(system),
                    new SessionController(system), new MaintenanceController(system))
            .setControllerAdvice(new ApiExceptionHandler())
            .build();

    private final Map<String, String> tokens = new HashMap<>();

    private String token(String userId) {
        return tokens.computeIfAbsent(userId, id -> system.signIn(id, IwfcBootstrap.DEMO_PASSWORD).token());
    }

    /** Sends the request as a signed-in user, using a real bearer token from the facade. */
    private MockHttpServletRequestBuilder as(String userId, MockHttpServletRequestBuilder request) {
        return request.header("Authorization", "Bearer " + token(userId)).contentType(MediaType.APPLICATION_JSON);
    }

    private static String login(String userId, String password) {
        return "{\"userId\":\"" + userId + "\",\"password\":\"" + password + "\"}";
    }

    // ---- accounts and authentication ------------------------------------------------------------

    @Test
    void should_sign_in_with_the_right_password_and_return_a_token_and_the_user() throws Exception {
        mvc.perform(post("/api/login").contentType(MediaType.APPLICATION_JSON)
                        .content(login("M-1", IwfcBootstrap.DEMO_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.expiresAt").isNotEmpty())
                .andExpect(jsonPath("$.user.id").value("M-1"))
                .andExpect(jsonPath("$.user.role").value("Member"));
    }

    @Test
    void should_answer_401_with_the_same_message_for_a_wrong_password_and_an_unknown_user() throws Exception {
        mvc.perform(post("/api/login").contentType(MediaType.APPLICATION_JSON).content(login("M-1", "wrong-password")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("Invalid user id or password"));
        mvc.perform(post("/api/login").contentType(MediaType.APPLICATION_JSON).content(login("nobody", "whatever-1")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid user id or password"));
    }

    @Test
    void should_answer_401_when_the_authorization_header_is_missing() throws Exception {
        mvc.perform(get("/api/equipment")).andExpect(status().isUnauthorized());
    }

    @Test
    void should_answer_401_when_the_token_is_not_a_real_one() throws Exception {
        mvc.perform(get("/api/equipment").header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("INVALID_CREDENTIALS"));
        mvc.perform(get("/api/equipment").header("Authorization", "Basic abc"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void should_tell_who_is_signed_in() throws Exception {
        mvc.perform(as("I-1", get("/api/me")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("I-1"))
                .andExpect(jsonPath("$.role").value("Instructor"));
    }

    @Test
    void should_stop_accepting_the_token_after_sign_out() throws Exception {
        mvc.perform(as("M-1", post("/api/logout"))).andExpect(status().isNoContent());

        mvc.perform(as("M-1", get("/api/me"))).andExpect(status().isUnauthorized());
    }

    @Test
    void should_list_users_for_an_administrator_and_answer_403_for_a_member() throws Exception {
        mvc.perform(as("A-1", get("/api/users"))).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(5)))
                .andExpect(jsonPath("$[0].active").value(true));
        mvc.perform(as("M-1", get("/api/users")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED_ACCESS"));
    }

    @Test
    void should_let_an_administrator_register_a_user_who_can_then_sign_in() throws Exception {
        mvc.perform(as("A-1", post("/api/users"))
                        .content("{\"id\":\"M-9\",\"name\":\"Mia Perera\",\"role\":\"MEMBER\",\"password\":\"mia-secret-1\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("M-9"))
                .andExpect(jsonPath("$.role").value("Member"));

        mvc.perform(post("/api/login").contentType(MediaType.APPLICATION_JSON).content(login("M-9", "mia-secret-1")))
                .andExpect(status().isOk());
    }

    @Test
    void should_answer_409_when_the_user_id_already_exists() throws Exception {
        mvc.perform(as("A-1", post("/api/users"))
                        .content("{\"id\":\"M-1\",\"name\":\"Copy\",\"role\":\"MEMBER\",\"password\":\"copy-secret-1\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("DUPLICATE"));
    }

    @Test
    void should_answer_400_for_a_weak_password_or_an_unknown_role() throws Exception {
        mvc.perform(as("A-1", post("/api/users"))
                        .content("{\"id\":\"M-9\",\"name\":\"Mia\",\"role\":\"MEMBER\",\"password\":\"short\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(as("A-1", post("/api/users"))
                        .content("{\"id\":\"M-9\",\"name\":\"Mia\",\"role\":\"OWNER\",\"password\":\"mia-secret-1\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_answer_403_when_a_member_tries_to_register_a_user() throws Exception {
        mvc.perform(as("M-1", post("/api/users"))
                        .content("{\"id\":\"M-9\",\"name\":\"Mia\",\"role\":\"MEMBER\",\"password\":\"mia-secret-1\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void should_deactivate_a_user_who_can_then_no_longer_sign_in() throws Exception {
        mvc.perform(as("A-1", post("/api/users/M-2/deactivate")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mvc.perform(post("/api/login").contentType(MediaType.APPLICATION_JSON)
                        .content(login("M-2", IwfcBootstrap.DEMO_PASSWORD)))
                .andExpect(status().isUnauthorized());
    }

    // ---- equipment ------------------------------------------------------------------------------

    @Test
    void should_list_equipment_with_status_and_usage() throws Exception {
        mvc.perform(as("M-1", get("/api/equipment")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("TM-01"))
                .andExpect(jsonPath("$[0].status").value("OPERATIONAL"))
                .andExpect(jsonPath("$[0].totalUsageHours").value(0.0));
    }

    @Test
    void should_create_equipment_when_an_administrator_posts_it() throws Exception {
        mvc.perform(as("A-1", post("/api/equipment"))
                        .content("{\"type\":\"TREADMILL\",\"id\":\"TM-09\",\"name\":\"Treadmill 09\",\"location\":\"Cardio Zone\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("TM-09"));
    }

    @Test
    void should_answer_409_when_the_equipment_id_already_exists() throws Exception {
        mvc.perform(as("A-1", post("/api/equipment"))
                        .content("{\"type\":\"TREADMILL\",\"id\":\"TM-01\",\"name\":\"Copy\",\"location\":\"Cardio Zone\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("DUPLICATE"))
                .andExpect(jsonPath("$.message").value(containsString("TM-01")));
    }

    @Test
    void should_answer_403_when_a_member_tries_to_add_equipment() throws Exception {
        mvc.perform(as("M-1", post("/api/equipment"))
                        .content("{\"type\":\"TREADMILL\",\"id\":\"TM-10\",\"name\":\"X\",\"location\":\"Cardio Zone\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void should_answer_400_when_the_equipment_type_is_unknown() throws Exception {
        mvc.perform(as("A-1", post("/api/equipment"))
                        .content("{\"type\":\"JETPACK\",\"id\":\"JP-1\",\"name\":\"X\",\"location\":\"Cardio Zone\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_edit_and_deactivate_equipment() throws Exception {
        mvc.perform(as("A-1", put("/api/equipment/TM-01")).content("{\"name\":\"Treadmill Pro\",\"location\":\"Studio A\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Treadmill Pro"))
                .andExpect(jsonPath("$.location").value("Studio A"));
        mvc.perform(as("A-1", post("/api/equipment/TM-01/deactivate")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void should_log_usage_and_flag_equipment_that_needs_maintenance() throws Exception {
        mvc.perform(as("I-1", post("/api/equipment/TM-01/usage")).content("{\"hours\":100}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.needsMaintenance").value(true));
    }

    // ---- sessions -------------------------------------------------------------------------------

    @Test
    void should_list_sessions_with_free_spots() throws Exception {
        mvc.perform(as("M-1", get("/api/sessions")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("S-1"))
                .andExpect(jsonPath("$[0].availableSpots").value(15));
    }

    @Test
    void should_book_a_session_and_answer_409_when_booking_it_twice() throws Exception {
        mvc.perform(as("M-1", post("/api/sessions/S-1/bookings")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.booked").value(1));
        mvc.perform(as("M-1", post("/api/sessions/S-1/bookings")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("INVALID_BOOKING"));
    }

    @Test
    void should_cancel_a_booking() throws Exception {
        mvc.perform(as("M-1", post("/api/sessions/S-1/bookings"))).andExpect(status().isOk());

        mvc.perform(as("M-1", delete("/api/sessions/S-1/bookings")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.booked").value(0));
    }

    @Test
    void should_schedule_a_session_and_a_weekly_series() throws Exception {
        mvc.perform(as("I-1", post("/api/sessions"))
                        .content(session("N-1", "Core", "Studio B", "14:00", "15:00", 1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(as("I-1", post("/api/sessions"))
                        .content(session("PIL", "Pilates", "Studio B", "07:00", "08:00", 4)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", hasSize(4)));
    }

    @Test
    void should_answer_409_when_the_studio_is_already_booked() throws Exception {
        mvc.perform(as("I-2", post("/api/sessions"))
                        .content(session("N-2", "Clash", "Studio A", "09:00", "10:00", 1)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("INVALID_BOOKING"));
    }

    @Test
    void should_answer_400_when_the_date_is_not_a_date() throws Exception {
        mvc.perform(as("I-1", post("/api/sessions"))
                        .content(session("N-3", "Bad", "Studio B", "14:00", "15:00", 1).replace(NEXT_MONDAY, "someday")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
    }

    @Test
    void should_let_an_instructor_cancel_a_session_and_stop_listing_it() throws Exception {
        mvc.perform(as("I-2", delete("/api/sessions/S-2"))).andExpect(status().isNoContent());

        mvc.perform(as("M-1", get("/api/sessions?all=true")))
                .andExpect(jsonPath("$[?(@.id=='S-2')]").isEmpty());
    }

    @Test
    void should_answer_403_when_a_member_cancels_a_session() throws Exception {
        mvc.perform(as("M-1", delete("/api/sessions/S-1"))).andExpect(status().isForbidden());
    }

    @Test
    void should_log_the_equipment_usage_when_an_instructor_completes_a_session() throws Exception {
        mvc.perform(as("I-1", post("/api/sessions/S-3/complete"))).andExpect(status().isOk());

        mvc.perform(as("A-1", get("/api/equipment")))
                .andExpect(jsonPath("$[?(@.id=='SB-01')].totalUsageHours").value(org.hamcrest.Matchers.contains(1.0)));
    }

    @Test
    void should_answer_404_for_an_unknown_session() throws Exception {
        mvc.perform(as("M-1", post("/api/sessions/nope/bookings"))).andExpect(status().isNotFound());
    }

    @Test
    void should_let_staff_send_reminders_and_answer_403_for_a_member() throws Exception {
        mvc.perform(as("A-1", post("/api/reminders")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sent").isNumber());
        mvc.perform(as("I-1", post("/api/reminders"))).andExpect(status().isOk());
        mvc.perform(as("M-1", post("/api/reminders"))).andExpect(status().isForbidden());
    }

    // ---- maintenance ----------------------------------------------------------------------------

    @Test
    void should_carry_a_fault_through_the_workflow_over_http() throws Exception {
        mvc.perform(as("I-1", post("/api/maintenance"))
                        .content("{\"equipmentId\":\"SB-04\",\"description\":\"Resistance failure\",\"urgency\":\"HIGH\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("MR-001"))
                .andExpect(jsonPath("$.status").value("PENDING"));
        mvc.perform(as("A-1", post("/api/maintenance/MR-001/assign")).content("{\"technician\":\"Kamal\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ASSIGNED"))
                .andExpect(jsonPath("$.assignedTo").value("Kamal"));
        mvc.perform(as("A-1", post("/api/maintenance/MR-001/progress")).content("{\"note\":\"Part fitted\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.progressNotes[0]").value("Part fitted"));
        mvc.perform(as("A-1", post("/api/maintenance/MR-001/complete")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
        mvc.perform(as("I-1", get("/api/notifications")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));
    }

    @Test
    void should_answer_403_when_a_member_reads_the_maintenance_log() throws Exception {
        mvc.perform(as("M-1", get("/api/maintenance"))).andExpect(status().isForbidden());
        mvc.perform(as("M-1", get("/api/maintenance/activity-log"))).andExpect(status().isForbidden());
    }

    @Test
    void should_let_an_instructor_see_only_their_own_requests() throws Exception {
        mvc.perform(as("I-1", post("/api/maintenance"))
                .content("{\"equipmentId\":\"TM-02\",\"description\":\"Belt\",\"urgency\":\"LOW\"}"));

        mvc.perform(as("I-1", get("/api/maintenance?mine=true"))).andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(as("I-2", get("/api/maintenance?mine=true"))).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void should_answer_409_when_a_pending_request_is_completed_directly() throws Exception {
        mvc.perform(as("I-1", post("/api/maintenance"))
                .content("{\"equipmentId\":\"TM-02\",\"description\":\"Belt\",\"urgency\":\"LOW\"}"));

        mvc.perform(as("A-1", post("/api/maintenance/MR-001/complete")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("INVALID_STATUS_TRANSITION"));
    }

    @Test
    void should_show_the_activity_log_to_administrators() throws Exception {
        mvc.perform(as("I-1", post("/api/maintenance"))
                .content("{\"equipmentId\":\"TM-02\",\"description\":\"Belt\",\"urgency\":\"LOW\"}"));

        mvc.perform(as("A-1", get("/api/maintenance/activity-log")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    private static String session(String id, String title, String studio, String start, String end, int weeks) {
        return "{\"id\":\"" + id + "\",\"title\":\"" + title + "\",\"studio\":\"" + studio + "\",\"date\":\""
                + NEXT_MONDAY + "\",\"start\":\"" + start + "\",\"end\":\"" + end
                + "\",\"capacity\":10,\"equipmentIds\":[],\"weeks\":" + weeks + "}";
    }
}
