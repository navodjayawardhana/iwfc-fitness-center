package com.iwfc.infrastructure.web;

import com.iwfc.application.facade.IwfcFacade;
import com.iwfc.domain.model.FitnessSession;
import com.iwfc.domain.model.Location;
import com.iwfc.domain.model.TimeSlot;
import com.iwfc.domain.model.User;
import com.iwfc.infrastructure.web.ApiDtos.SessionRequest;
import com.iwfc.infrastructure.web.ApiDtos.SessionResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
public class SessionController {

    private final IwfcFacade system;

    public SessionController(IwfcFacade system) {
        this.system = system;
    }

    /** Sessions with free spots by default; {@code ?all=true} includes full ones. */
    @GetMapping("/api/sessions")
    List<SessionResponse> list(@RequestHeader("X-User-Id") String userId,
                               @RequestParam(defaultValue = "false") boolean all) {
        system.findUser(userId);
        List<FitnessSession> sessions = all ? system.allSessions() : system.availableSessions();
        return sessions.stream().map(SessionResponse::from).toList();
    }

    @PostMapping("/api/sessions")
    @ResponseStatus(HttpStatus.CREATED)
    List<SessionResponse> schedule(@RequestHeader("X-User-Id") String userId, @RequestBody SessionRequest request) {
        User instructor = system.findUser(userId);
        LocalDate date = LocalDate.parse(request.date());
        TimeSlot slot = new TimeSlot(date.atTime(LocalTime.parse(request.start())),
                date.atTime(LocalTime.parse(request.end())));
        Location studio = new Location(request.studio());
        List<String> equipmentIds = request.equipmentIds() == null ? List.of() : request.equipmentIds();
        int weeks = request.weeks() == null ? 1 : request.weeks();
        List<FitnessSession> created = weeks > 1
                ? system.scheduleWeeklySession(instructor, request.id(), request.title(), studio, slot,
                        request.capacity(), equipmentIds, weeks)
                : List.of(system.scheduleSession(instructor, request.id(), request.title(), studio, slot,
                        request.capacity(), equipmentIds));
        return created.stream().map(SessionResponse::from).toList();
    }

    @PostMapping("/api/sessions/{id}/bookings")
    SessionResponse book(@RequestHeader("X-User-Id") String userId, @PathVariable String id) {
        system.bookSession(id, system.findUser(userId));
        return SessionResponse.from(system.findSession(id));
    }

    @DeleteMapping("/api/sessions/{id}/bookings")
    SessionResponse cancelBooking(@RequestHeader("X-User-Id") String userId, @PathVariable String id) {
        system.cancelBooking(id, system.findUser(userId));
        return SessionResponse.from(system.findSession(id));
    }

    @DeleteMapping("/api/sessions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void cancelSession(@RequestHeader("X-User-Id") String userId, @PathVariable String id) {
        system.cancelSession(system.findUser(userId), id);
    }

    @PostMapping("/api/sessions/{id}/complete")
    SessionResponse complete(@RequestHeader("X-User-Id") String userId, @PathVariable String id) {
        system.completeSession(system.findUser(userId), id);
        return SessionResponse.from(system.findSession(id));
    }
}
