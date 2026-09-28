package com.iwfc.infrastructure;

import com.iwfc.application.facade.IwfcFacade;
import com.iwfc.application.notification.AdminAlertNotifier;
import com.iwfc.application.notification.AdminMaintenanceLog;
import com.iwfc.application.notification.NotificationService;
import com.iwfc.application.notification.ReporterNotifier;
import com.iwfc.application.security.PasswordHasher;
import com.iwfc.application.usecase.AuthenticationUseCase;
import com.iwfc.application.usecase.BookSessionUseCase;
import com.iwfc.application.usecase.EquipmentInventoryUseCase;
import com.iwfc.application.usecase.MaintenanceUseCase;
import com.iwfc.application.usecase.ReminderUseCase;
import com.iwfc.application.usecase.UserAccountUseCase;
import com.iwfc.domain.model.Administrator;
import com.iwfc.domain.model.Credential;
import com.iwfc.domain.model.EquipmentFactory;
import com.iwfc.domain.model.EquipmentType;
import com.iwfc.domain.model.Instructor;
import com.iwfc.domain.model.Location;
import com.iwfc.domain.model.Member;
import com.iwfc.domain.model.SessionSchedule;
import com.iwfc.domain.model.TimeSlot;
import com.iwfc.infrastructure.persistence.Storage;
import com.iwfc.infrastructure.persistence.jdbc.DataSources;
import com.iwfc.infrastructure.persistence.jdbc.JdbcStorage;
import com.iwfc.infrastructure.security.Pbkdf2PasswordHasher;

import javax.sql.DataSource;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Composition root: the only place that knows every concrete class and wires them together.
 * The same wiring runs on in-memory storage (the default) or on MySQL, chosen by environment variables.
 */
public final class IwfcBootstrap {

    public static final LocalTime OPENS_AT = LocalTime.of(6, 0);
    public static final LocalTime CLOSES_AT = LocalTime.of(22, 0);

    /** Password of every seeded demo account. For local demos only; real accounts choose their own. */
    public static final String DEMO_PASSWORD = "fitpulse-demo";

    private static final Duration TOKEN_LIFETIME = Duration.ofHours(8);
    private static final int FAST_ITERATIONS = 1_000;

    private IwfcBootstrap() {
    }

    /**
     * The system the real console and REST API run. {@code FITPULSE_STORAGE} is {@code memory} (default) or
     * {@code mysql}; see {@link DataSources#settings} for the MySQL variables.
     */
    public static IwfcFacade configured(Map<String, String> environment) {
        String storage = environment.getOrDefault("FITPULSE_STORAGE", "memory").trim().toLowerCase(Locale.ROOT);
        return switch (storage) {
            case "", "memory" -> seededSecure();
            case "mysql" -> database(DataSources.mysql(DataSources.settings(environment)), Pbkdf2PasswordHasher.strong());
            default -> throw new IllegalArgumentException(
                    "Unknown FITPULSE_STORAGE value: " + storage + " (use memory or mysql)");
        };
    }

    /** A running system with no equipment, sessions or users. */
    public static IwfcFacade empty() {
        return wire(Storage.inMemory(), new Pbkdf2PasswordHasher(FAST_ITERATIONS));
    }

    /** Sample data with a fast password hash, used by the unit tests. */
    public static IwfcFacade seeded() {
        return seeded(new Pbkdf2PasswordHasher(FAST_ITERATIONS));
    }

    /** Sample data with the strong password hash, kept in memory. */
    public static IwfcFacade seededSecure() {
        return seeded(Pbkdf2PasswordHasher.strong());
    }

    public static IwfcFacade seeded(PasswordHasher hasher) {
        return seed(Storage.inMemory(), hasher);
    }

    /**
     * The system on a database. Tables are created if missing; sample data is added only when the database has
     * no users yet, so a restart finds everything exactly as it was left.
     */
    public static IwfcFacade database(DataSource dataSource, PasswordHasher hasher) {
        Storage storage = JdbcStorage.create(dataSource);
        return storage.users().count() == 0 ? seed(storage, hasher) : wire(storage, hasher);
    }

    /** Adds sample users, equipment and sessions for the coming Monday, then returns the running system. */
    private static IwfcFacade seed(Storage storage, PasswordHasher hasher) {
        Administrator admin = new Administrator("A-1", "Amal Perera");
        Instructor nimali = new Instructor("I-1", "Nimali Silva");
        Instructor kasun = new Instructor("I-2", "Kasun Fernando");
        String demoHash = hasher.hash(DEMO_PASSWORD);
        List.of(admin, nimali, kasun, new Member("M-1", "Dilani Jayasinghe"), new Member("M-2", "Ruwan Bandara"))
                .forEach(user -> {
                    storage.users().save(user);
                    storage.credentials().save(new Credential(user.id(), demoHash));
                });
        IwfcFacade facade = wire(storage, hasher);

        Location cardio = new Location("Cardio Zone");
        Location spinStudio = new Location("Spin Studio");
        facade.addEquipment(admin, EquipmentType.TREADMILL, "TM-01", "Treadmill 01", cardio);
        facade.addEquipment(admin, EquipmentType.TREADMILL, "TM-02", "Treadmill 02", cardio);
        for (int number = 1; number <= 4; number++) {
            facade.addEquipment(admin, EquipmentType.SPIN_BIKE, "SB-0" + number, "Spin Bike 0" + number, spinStudio);
        }
        facade.addEquipment(admin, EquipmentType.ROWING_MACHINE, "RM-01", "Rowing Machine 01", cardio);
        facade.addEquipment(admin, EquipmentType.HEART_RATE_MONITOR, "HR-01", "Heart Rate Monitor 01", cardio);

        LocalDate monday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        facade.scheduleSession(nimali, "S-1", "Morning Yoga", new Location("Studio A"),
                new TimeSlot(monday.atTime(9, 0), monday.atTime(10, 0)), 15, List.of());
        facade.scheduleSession(kasun, "S-2", "HIIT Blast", new Location("Studio B"),
                new TimeSlot(monday.atTime(11, 0), monday.atTime(12, 0)), 12, List.of());
        facade.scheduleSession(nimali, "S-3", "Spin Class", spinStudio,
                new TimeSlot(monday.atTime(18, 0), monday.atTime(19, 0)), 8, List.of("SB-01", "SB-02"));
        return facade;
    }

    private static IwfcFacade wire(Storage storage, PasswordHasher hasher) {
        NotificationService notifications = new NotificationService(storage.notifications());
        AdminMaintenanceLog activityLog = new AdminMaintenanceLog(storage.activityLog());
        notifications.subscribe(new ReporterNotifier(notifications));
        notifications.subscribe(activityLog);
        notifications.subscribe(new AdminAlertNotifier(notifications, storage.users()));

        EquipmentInventoryUseCase inventory =
                new EquipmentInventoryUseCase(storage.equipment(), new EquipmentFactory(), notifications);
        SessionSchedule schedule = new SessionSchedule(OPENS_AT, CLOSES_AT, storage.sessions());
        BookSessionUseCase sessions = new BookSessionUseCase(schedule, storage.equipment(), inventory, notifications);
        ReminderUseCase reminders = new ReminderUseCase(schedule, notifications, Clock.systemDefaultZone());
        MaintenanceUseCase maintenance =
                new MaintenanceUseCase(storage.requests(), storage.equipment(), notifications);
        AuthenticationUseCase authentication = new AuthenticationUseCase(storage.users(), storage.credentials(),
                hasher, Clock.systemUTC(), TOKEN_LIFETIME);
        UserAccountUseCase accounts = new UserAccountUseCase(storage.users(), storage.credentials(), hasher);
        return new IwfcFacade(inventory, sessions, maintenance, notifications, activityLog, storage.users(),
                authentication, accounts, reminders);
    }
}
