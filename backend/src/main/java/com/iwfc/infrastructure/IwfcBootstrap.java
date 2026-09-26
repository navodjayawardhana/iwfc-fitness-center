package com.iwfc.infrastructure;

import com.iwfc.application.facade.IwfcFacade;
import com.iwfc.application.notification.AdminMaintenanceLog;
import com.iwfc.application.notification.NotificationService;
import com.iwfc.application.notification.ReporterNotifier;
import com.iwfc.application.usecase.BookSessionUseCase;
import com.iwfc.application.usecase.EquipmentInventoryUseCase;
import com.iwfc.application.usecase.MaintenanceUseCase;
import com.iwfc.domain.model.Administrator;
import com.iwfc.domain.model.Equipment;
import com.iwfc.domain.model.EquipmentFactory;
import com.iwfc.domain.model.EquipmentType;
import com.iwfc.domain.model.Instructor;
import com.iwfc.domain.model.Location;
import com.iwfc.domain.model.MaintenanceRequest;
import com.iwfc.domain.model.Member;
import com.iwfc.domain.model.SessionSchedule;
import com.iwfc.domain.model.TimeSlot;
import com.iwfc.domain.model.User;
import com.iwfc.domain.repository.Repository;
import com.iwfc.infrastructure.persistence.InMemoryRepository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

/**
 * Composition root: the only place that knows every concrete class and wires them together.
 * Data is hard-coded and kept in memory, as allowed by the brief.
 */
public final class IwfcBootstrap {

    public static final LocalTime OPENS_AT = LocalTime.of(6, 0);
    public static final LocalTime CLOSES_AT = LocalTime.of(22, 0);

    private IwfcBootstrap() {
    }

    /** A running system with no equipment, sessions or users. */
    public static IwfcFacade empty() {
        return wire(new InMemoryRepository<>(User::id));
    }

    /** A running system with sample users, equipment and sessions for the coming Monday. */
    public static IwfcFacade seeded() {
        Repository<User, String> users = new InMemoryRepository<>(User::id);
        Administrator admin = new Administrator("A-1", "Amal Perera");
        Instructor nimali = new Instructor("I-1", "Nimali Silva");
        Instructor kasun = new Instructor("I-2", "Kasun Fernando");
        List.of(admin, nimali, kasun, new Member("M-1", "Dilani Jayasinghe"), new Member("M-2", "Ruwan Bandara"))
                .forEach(users::save);
        IwfcFacade facade = wire(users);

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

    private static IwfcFacade wire(Repository<User, String> users) {
        Repository<Equipment, String> equipment = new InMemoryRepository<>(Equipment::id);
        Repository<MaintenanceRequest, String> requests = new InMemoryRepository<>(MaintenanceRequest::id);

        NotificationService notifications = new NotificationService();
        AdminMaintenanceLog activityLog = new AdminMaintenanceLog();
        notifications.subscribe(new ReporterNotifier(notifications));
        notifications.subscribe(activityLog);

        EquipmentInventoryUseCase inventory =
                new EquipmentInventoryUseCase(equipment, new EquipmentFactory(), notifications);
        BookSessionUseCase sessions = new BookSessionUseCase(
                new SessionSchedule(OPENS_AT, CLOSES_AT), equipment, inventory, notifications);
        MaintenanceUseCase maintenance = new MaintenanceUseCase(requests, equipment, notifications);
        return new IwfcFacade(inventory, sessions, maintenance, notifications, activityLog, users);
    }
}
