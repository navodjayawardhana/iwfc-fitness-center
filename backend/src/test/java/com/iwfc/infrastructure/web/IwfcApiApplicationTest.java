package com.iwfc.infrastructure.web;

import com.iwfc.application.facade.IwfcFacade;
import com.iwfc.infrastructure.IwfcBootstrap;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Starts the real Spring context. The properties beat any FITPULSE_* environment variables on the developer machine,
 * so this test never touches a database, and the reminder timer is pushed an hour away.
 */
@SpringBootTest(classes = IwfcApiApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {"FITPULSE_STORAGE=memory", "fitpulse.reminders.initial-delay-ms=3600000"})
class IwfcApiApplicationTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void should_start_with_the_facade_the_controllers_and_the_reminder_timer() {
        assertNotNull(context.getBean(IwfcFacade.class));
        assertNotNull(context.getBean(AccountController.class));
        assertNotNull(context.getBean(EquipmentController.class));
        assertNotNull(context.getBean(SessionController.class));
        assertNotNull(context.getBean(MaintenanceController.class));
        assertNotNull(context.getBean(ReminderScheduler.class));
    }

    @Test
    void should_wire_a_facade_that_signs_in_a_seeded_user_on_memory_storage() {
        IwfcFacade system = context.getBean(IwfcFacade.class);

        assertEquals("Administrator", system.signIn("A-1", IwfcBootstrap.DEMO_PASSWORD).user().roleName());
        assertEquals(8, system.listEquipment().size());
    }

    @Test
    void should_be_named_fitpulse() {
        assertEquals("fitpulse", context.getEnvironment().getProperty("spring.application.name"));
    }
}
