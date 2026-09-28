package com.iwfc.infrastructure.web;

import com.iwfc.application.facade.IwfcFacade;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** The automatic trigger: every fifteen minutes (configurable) members with a session soon get their reminder. */
@Component
public class ReminderScheduler {

    private final IwfcFacade system;

    public ReminderScheduler(IwfcFacade system) {
        this.system = system;
    }

    @Scheduled(fixedRateString = "${fitpulse.reminders.interval-ms:900000}", initialDelayString = "${fitpulse.reminders.initial-delay-ms:60000}")
    public void sendReminders() {
        system.sendDueReminders();
    }
}
