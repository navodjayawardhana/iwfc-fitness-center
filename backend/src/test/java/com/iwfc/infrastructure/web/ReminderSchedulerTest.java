package com.iwfc.infrastructure.web;

import com.iwfc.application.facade.IwfcFacade;
import com.iwfc.infrastructure.IwfcBootstrap;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The automatic part: a timer calls the facade so members are reminded without anyone pressing a button. */
class ReminderSchedulerTest {

    @Test
    void should_run_the_reminder_round_through_the_facade_without_failing() {
        IwfcFacade system = IwfcBootstrap.seeded();

        assertDoesNotThrow(() -> new ReminderScheduler(system).sendReminders());
    }

    @Test
    void should_be_safe_to_run_again_and_again() {
        ReminderScheduler scheduler = new ReminderScheduler(IwfcBootstrap.seeded());

        assertDoesNotThrow(() -> {
            scheduler.sendReminders();
            scheduler.sendReminders();
        });
    }
}
