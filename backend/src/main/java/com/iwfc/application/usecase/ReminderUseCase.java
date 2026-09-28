package com.iwfc.application.usecase;

import com.iwfc.application.notification.NotificationService;
import com.iwfc.domain.model.FitnessSession;
import com.iwfc.domain.model.SessionSchedule;
import com.iwfc.domain.model.User;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Automated schedule notifications: reminds each booked member shortly before a session starts.
 * A member is reminded once per session, so the round can run on a timer as often as needed.
 */
public class ReminderUseCase {

    private static final Duration DAY_AHEAD = Duration.ofHours(24);
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EEE dd MMM", Locale.ENGLISH);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH);

    private final SessionSchedule schedule;
    private final NotificationService notifications;
    private final Clock clock;
    private final Set<String> alreadyReminded = ConcurrentHashMap.newKeySet();

    public ReminderUseCase(SessionSchedule schedule, NotificationService notifications, Clock clock) {
        this.schedule = schedule;
        this.notifications = notifications;
        this.clock = clock;
    }

    public int sendDueReminders() {
        return sendDueReminders(DAY_AHEAD);
    }

    /** Returns how many reminders were sent in this round. */
    public int sendDueReminders(Duration window) {
        if (window == null || window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException("The reminder window must be longer than zero");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime limit = now.plus(window);
        int sent = 0;
        for (FitnessSession session : schedule.allSessions()) {
            LocalDateTime start = session.slot().start();
            if (!start.isAfter(now) || start.isAfter(limit)) {
                continue;
            }
            for (User member : session.bookedMembers()) {
                if (alreadyReminded.add(session.id() + "|" + member.id())) {
                    notifications.send(member.id(), message(session));
                    sent++;
                }
            }
        }
        return sent;
    }

    private static String message(FitnessSession session) {
        return "Reminder: " + session.title() + " starts at " + session.slot().start().format(TIME) + " on "
                + session.slot().start().format(DAY) + " in " + session.studio().name() + ".";
    }
}
