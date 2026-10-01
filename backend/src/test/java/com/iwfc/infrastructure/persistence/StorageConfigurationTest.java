package com.iwfc.infrastructure.persistence;

import com.iwfc.application.facade.IwfcFacade;
import com.iwfc.infrastructure.IwfcBootstrap;
import com.iwfc.infrastructure.persistence.jdbc.DataSources;
import com.iwfc.infrastructure.persistence.jdbc.DataSources.DbSettings;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Storage is chosen by environment variables, and credentials never live in the code or in git. */
class StorageConfigurationTest {

    // Z
    @Test
    void should_use_memory_when_nothing_is_configured() {
        IwfcFacade system = IwfcBootstrap.configured(Map.of());

        assertEquals(15, system.listEquipment().size());
    }

    // O
    @Test
    void should_use_memory_when_asked_for_it_by_name() {
        assertEquals(15, IwfcBootstrap.configured(Map.of("FITPULSE_STORAGE", "memory")).listEquipment().size());
        assertEquals(15, IwfcBootstrap.configured(Map.of("FITPULSE_STORAGE", " MEMORY ")).listEquipment().size());
    }

    // M - defaults and overrides for the database settings
    @Test
    void should_default_to_a_local_mysql_database_called_fitpulse() {
        DbSettings settings = DataSources.settings(Map.of("FITPULSE_DB_PASSWORD", "secret"));

        assertTrue(settings.url().startsWith("jdbc:mysql://localhost:3306/fitpulse"));
        assertEquals("fitpulse", settings.user());
        assertEquals("secret", settings.password());
    }

    @Test
    void should_take_the_url_and_user_from_the_environment_when_given() {
        DbSettings settings = DataSources.settings(Map.of("FITPULSE_DB_URL", "jdbc:mysql://db.example:3307/gym",
                "FITPULSE_DB_USER", "gymuser", "FITPULSE_DB_PASSWORD", "another-secret"));

        assertEquals("jdbc:mysql://db.example:3307/gym", settings.url());
        assertEquals("gymuser", settings.user());
    }

    // I - a local development MySQL (such as WAMP) often has root with no password
    @Test
    void should_allow_an_empty_password_only_when_it_is_switched_on_explicitly() {
        DbSettings settings = DataSources.settings(Map.of("FITPULSE_DB_USER", "root", "FITPULSE_DB_NO_PASSWORD", "true"));

        assertEquals("root", settings.user());
        assertEquals("", settings.password());
    }

    @Test
    void should_ignore_the_no_password_switch_unless_it_is_exactly_true() {
        for (String value : new String[] {"false", "yes", "1", ""}) {
            assertThrows(IllegalStateException.class,
                    () -> DataSources.settings(Map.of("FITPULSE_DB_NO_PASSWORD", value)), value);
        }
    }

    @Test
    void should_prefer_a_real_password_when_both_are_given() {
        DbSettings settings = DataSources.settings(Map.of("FITPULSE_DB_PASSWORD", "secret", "FITPULSE_DB_NO_PASSWORD", "true"));

        assertEquals("secret", settings.password());
    }

    @Test
    void should_create_the_database_on_first_connect_so_no_manual_setup_is_needed() {
        DbSettings settings = DataSources.settings(Map.of("FITPULSE_DB_PASSWORD", "secret"));

        assertTrue(settings.url().contains("createDatabaseIfNotExist=true"));
    }

    @Test
    void should_mention_the_no_password_switch_in_the_error_for_a_missing_password() {
        IllegalStateException error = assertThrows(IllegalStateException.class, () -> DataSources.settings(Map.of()));

        assertTrue(error.getMessage().contains("FITPULSE_DB_NO_PASSWORD"));
    }

    // B
    @Test
    void should_not_reveal_the_password_when_settings_are_printed() {
        DbSettings settings = DataSources.settings(Map.of("FITPULSE_DB_PASSWORD", "very-secret-value"));

        assertFalse(settings.toString().contains("very-secret-value"));
    }

    // E
    @Test
    void should_explain_which_variable_is_missing_when_mysql_is_chosen_without_a_password() {
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> IwfcBootstrap.configured(Map.of("FITPULSE_STORAGE", "mysql")));

        assertTrue(error.getMessage().contains("FITPULSE_DB_PASSWORD"));
    }

    @Test
    void should_reject_a_blank_password() {
        assertThrows(IllegalStateException.class, () -> DataSources.settings(Map.of("FITPULSE_DB_PASSWORD", "  ")));
    }

    @Test
    void should_reject_an_unknown_storage_name() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> IwfcBootstrap.configured(Map.of("FITPULSE_STORAGE", "oracle")));

        assertTrue(error.getMessage().contains("oracle"));
    }

    // S
    @Test
    void should_offer_a_working_facade_that_signs_in_a_seeded_user() {
        IwfcFacade system = IwfcBootstrap.configured(Map.of());

        assertEquals("Member", system.signIn("M-1", IwfcBootstrap.DEMO_PASSWORD).user().roleName());
    }
}
