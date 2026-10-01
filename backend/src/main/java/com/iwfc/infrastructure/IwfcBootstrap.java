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
import com.iwfc.domain.model.MaintenanceRequest;
import com.iwfc.domain.model.Member;
import com.iwfc.domain.model.Role;
import com.iwfc.domain.model.SessionSchedule;
import com.iwfc.domain.model.TimeSlot;
import com.iwfc.domain.model.Urgency;
import com.iwfc.domain.model.User;
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

    /** Sample data plus a full demo week, with the strong password hash, kept in memory. */
    public static IwfcFacade seededSecure() {
        return demo(seeded(Pbkdf2PasswordHasher.strong()));
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

    /**
     * Extra content for live demos only, never the unit tests or the database path: more people and
     * equipment, a class on each of the next seven days, a recurring weekly class, bookings, logged
     * usage that crosses a maintenance threshold, and a maintenance request in every workflow state.
     */
    private static IwfcFacade demo(IwfcFacade facade) {
        User admin = facade.findUser("A-1");
        User nimali = facade.findUser("I-1");
        User kasun = facade.findUser("I-2");

        facade.registerUser(admin, Role.INSTRUCTOR, "I-3", "Sachini Weerasinghe", DEMO_PASSWORD);
        facade.registerUser(admin, Role.MEMBER, "M-3", "Tharindu Senanayake", DEMO_PASSWORD);
        facade.registerUser(admin, Role.MEMBER, "M-4", "Ishara Gunawardena", DEMO_PASSWORD);
        facade.registerUser(admin, Role.MEMBER, "M-5", "Chamodi Herath", DEMO_PASSWORD);
        User sachini = facade.findUser("I-3");

        Location cardio = new Location("Cardio Zone");
        Location spin = new Location("Spin Studio");
        Location strength = new Location("Strength Zone");
        facade.addEquipment(admin, EquipmentType.TREADMILL, "TM-03", "Treadmill 03", cardio);
        facade.addEquipment(admin, EquipmentType.TREADMILL, "TM-04", "Treadmill 04", cardio);
        facade.addEquipment(admin, EquipmentType.ROWING_MACHINE, "RM-02", "Rowing Machine 02", strength);
        facade.addEquipment(admin, EquipmentType.ROWING_MACHINE, "RM-03", "Rowing Machine 03", strength);
        facade.addEquipment(admin, EquipmentType.SPIN_BIKE, "SB-05", "Spin Bike 05", spin);
        facade.addEquipment(admin, EquipmentType.SPIN_BIKE, "SB-06", "Spin Bike 06", spin);
        facade.addEquipment(admin, EquipmentType.HEART_RATE_MONITOR, "HR-02", "Heart Rate Monitor 02", strength);

        // One or two classes on each of the next seven days. Times (07-08 and 20-21) and studios are
        // chosen so they can never clash with the Monday classes S-1..S-3 (09-10, 11-12, 18-19).
        LocalDate today = LocalDate.now();
        Location studioC = new Location("Studio C");
        daySession(facade, nimali, "S-10", "Sunrise Yoga", studioC, today.plusDays(1), 7, 15, List.of());
        daySession(facade, sachini, "S-11", "Strength Circuit", strength, today.plusDays(1), 20, 10, List.of("RM-02", "RM-03"));
        daySession(facade, kasun, "S-12", "HIIT Express", studioC, today.plusDays(2), 7, 12, List.of());
        daySession(facade, sachini, "S-13", "Evening Spin", spin, today.plusDays(2), 20, 8, List.of("SB-05", "SB-06"));
        daySession(facade, nimali, "S-14", "Pilates Core", studioC, today.plusDays(3), 7, 12, List.of());
        daySession(facade, kasun, "S-15", "Boxfit", studioC, today.plusDays(4), 20, 14, List.of());
        daySession(facade, sachini, "S-16", "Rowing Intervals", strength, today.plusDays(5), 7, 6, List.of("RM-02"));
        daySession(facade, nimali, "S-17", "Zumba Party", studioC, today.plusDays(6), 20, 20, List.of());
        daySession(facade, kasun, "S-18", "Full Body Burn", studioC, today.plusDays(7), 7, 12, List.of());
        facade.scheduleWeeklySession(sachini, "S-20", "Weekly Pilates", new Location("Studio A"),
                new TimeSlot(today.plusDays(2).atTime(14, 0), today.plusDays(2).atTime(15, 0)), 12, List.of(), 3);

        facade.bookSession("S-1", facade.findUser("M-3"));
        facade.bookSession("S-1", facade.findUser("M-5"));
        facade.bookSession("S-10", facade.findUser("M-1"));
        facade.bookSession("S-10", facade.findUser("M-2"));
        facade.bookSession("S-10", facade.findUser("M-5"));
        facade.bookSession("S-12", facade.findUser("M-3"));
        facade.bookSession("S-13", facade.findUser("M-2"));
        facade.bookSession("S-13", facade.findUser("M-4"));
        facade.bookSession("S-14", facade.findUser("M-1"));
        facade.bookSession("S-16", facade.findUser("M-4"));
        facade.bookSession("S-17", facade.findUser("M-3"));
        facade.bookSession("S-17", facade.findUser("M-5"));

        // TM-01 crosses its 100-hour threshold, so the dashboard shows a preventative maintenance alert.
        facade.logEquipmentUsage(nimali, "TM-01", 60);
        facade.logEquipmentUsage(kasun, "TM-01", 45);
        facade.logEquipmentUsage(kasun, "TM-03", 35);
        facade.logEquipmentUsage(nimali, "SB-01", 80);
        facade.logEquipmentUsage(sachini, "SB-05", 25);
        facade.logEquipmentUsage(sachini, "RM-02", 40);

        // One maintenance request in each workflow state: assigned, completed and pending.
        MaintenanceRequest spinBike = facade.reportFault(nimali, "SB-04", "Resistance fails above level 8", Urgency.HIGH);
        facade.assignMaintenance(admin, spinBike.id(), "Tech Pradeep");
        facade.updateMaintenanceProgress(admin, spinBike.id(), "Replacement magnet unit ordered");
        MaintenanceRequest monitor = facade.reportFault(kasun, "HR-01", "Heart-rate reading drifts after 20 minutes", Urgency.MEDIUM);
        facade.assignMaintenance(admin, monitor.id(), "Tech Shalini");
        facade.completeMaintenance(admin, monitor.id());
        facade.reportFault(sachini, "TM-02", "Belt slips at speeds above 10 km/h", Urgency.LOW);
        return facade;
    }

    private static void daySession(IwfcFacade facade, User instructor, String id, String title, Location studio,
                                   LocalDate day, int startHour, int capacity, List<String> equipmentIds) {
        facade.scheduleSession(instructor, id, title, studio,
                new TimeSlot(day.atTime(startHour, 0), day.atTime(startHour + 1, 0)), capacity, equipmentIds);
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
