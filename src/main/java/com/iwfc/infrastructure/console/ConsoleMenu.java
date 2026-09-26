package com.iwfc.infrastructure.console;

import com.iwfc.application.facade.IwfcFacade;
import com.iwfc.domain.exception.DuplicateEquipmentException;
import com.iwfc.domain.exception.InvalidBookingException;
import com.iwfc.domain.exception.ResourceNotFoundException;
import com.iwfc.domain.exception.UnauthorizedAccessException;
import com.iwfc.domain.model.EquipmentType;
import com.iwfc.domain.model.Location;
import com.iwfc.domain.model.TimeSlot;
import com.iwfc.domain.model.Urgency;
import com.iwfc.domain.model.User;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

/**
 * Console adapter: thin. It only reads input, calls the {@link IwfcFacade} and prints results.
 * Business rules stay in the domain; here the custom exceptions are turned into plain messages.
 */
public class ConsoleMenu {

    private final IwfcFacade system;
    private final BufferedReader in;
    private final PrintStream out;
    private User current;

    public ConsoleMenu(IwfcFacade system, InputStream in, PrintStream out) {
        this.system = system;
        this.in = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        this.out = out;
    }

    public void run() {
        out.println("=== Intelligent Wellness and Fitness Center ===");
        try {
            login();
            boolean running = true;
            while (running) {
                showMenu();
                running = handle(prompt("Choose an option"));
            }
        } catch (EndOfInput endOfInput) {
            out.println();
        }
        out.println("Goodbye.");
    }

    private void login() {
        while (current == null) {
            try {
                current = system.login(prompt("User id (A-1, I-1, I-2, M-1, M-2)"));
                out.println("Welcome, " + current.name() + " (" + current.roleName() + ")");
            } catch (ResourceNotFoundException notFound) {
                out.println("[Not found] " + notFound.getMessage());
            }
        }
    }

    private void showMenu() {
        out.println();
        out.println("--- " + current.roleName() + ": " + current.name() + " ---");
        out.println(" 1 List equipment");
        out.println(" 2 Add equipment              (Administrator)");
        out.println(" 3 Deactivate equipment       (Administrator)");
        out.println(" 4 Log equipment usage        (Instructor)");
        out.println(" 5 View available sessions");
        out.println(" 6 Schedule a session         (Instructor)");
        out.println(" 7 Schedule a weekly class    (Instructor)");
        out.println(" 8 Book a session             (Member)");
        out.println(" 9 Cancel my booking          (Member)");
        out.println("10 Report a fault             (Instructor)");
        out.println("11 View maintenance requests  (Administrator)");
        out.println("12 Assign a maintenance request (Administrator)");
        out.println("13 Update maintenance progress  (Administrator)");
        out.println("14 Complete a maintenance request (Administrator)");
        out.println("15 View my notifications");
        out.println("16 Switch user");
        out.println(" 0 Exit");
    }

    /** Returns false when the user chose to exit. */
    private boolean handle(String choice) {
        if (choice.equals("0")) {
            return false;
        }
        try {
            switch (choice) {
                case "1" -> system.listEquipment().forEach(out::println);
                case "2" -> addEquipment();
                case "3" -> {
                    system.deactivateEquipment(current, prompt("Equipment id"));
                    out.println("Equipment deactivated.");
                }
                case "4" -> logUsage();
                case "5" -> showSessions();
                case "6" -> scheduleSession(false);
                case "7" -> scheduleSession(true);
                case "8" -> {
                    system.bookSession(prompt("Session id"), current);
                    out.println("Booked.");
                }
                case "9" -> {
                    system.cancelBooking(prompt("Session id"), current);
                    out.println("Booking cancelled.");
                }
                case "10" -> reportFault();
                case "11" -> system.maintenanceRequests(current).forEach(out::println);
                case "12" -> {
                    system.assignMaintenance(current, prompt("Request id"), prompt("Technician"));
                    out.println("Request assigned.");
                }
                case "13" -> {
                    system.updateMaintenanceProgress(current, prompt("Request id"), prompt("Progress note"));
                    out.println("Progress recorded.");
                }
                case "14" -> {
                    system.completeMaintenance(current, prompt("Request id"));
                    out.println("Request completed.");
                }
                case "15" -> showNotifications();
                case "16" -> {
                    current = null;
                    login();
                }
                default -> out.println("Unknown option: " + choice);
            }
        } catch (EndOfInput endOfInput) {
            throw endOfInput;
        } catch (UnauthorizedAccessException error) {
            out.println("[Access denied] " + error.getMessage());
        } catch (InvalidBookingException error) {
            out.println("[Invalid booking] " + error.getMessage());
        } catch (DuplicateEquipmentException error) {
            out.println("[Duplicate] " + error.getMessage());
        } catch (ResourceNotFoundException error) {
            out.println("[Not found] " + error.getMessage());
        } catch (java.time.format.DateTimeParseException | NumberFormatException error) {
            out.println("[Invalid input] Could not understand that value: " + error.getMessage());
        } catch (IllegalArgumentException | IllegalStateException error) {
            out.println("[Error] " + error.getMessage());
        } catch (RuntimeException error) {
            out.println("[Rejected] " + error.getMessage());
        }
        return true;
    }

    private void addEquipment() {
        String types = Arrays.toString(EquipmentType.values());
        EquipmentType type = EquipmentType.valueOf(prompt("Type " + types).toUpperCase());
        String id = prompt("Equipment id");
        String name = prompt("Name");
        Location location = new Location(prompt("Location"));
        out.println("Added " + system.addEquipment(current, type, id, name, location));
    }

    private void logUsage() {
        String id = prompt("Equipment id");
        double hours = Double.parseDouble(prompt("Hours used"));
        system.logEquipmentUsage(current, id, hours);
        out.println("Usage logged.");
    }

    private void showSessions() {
        List<?> available = system.availableSessions();
        if (available.isEmpty()) {
            out.println("No sessions with free spots.");
        }
        available.forEach(out::println);
    }

    private void scheduleSession(boolean weekly) {
        String id = prompt("Session id");
        String title = prompt("Title");
        Location studio = new Location(prompt("Studio"));
        LocalDate date = LocalDate.parse(prompt("Date (yyyy-MM-dd)"));
        LocalTime start = LocalTime.parse(prompt("Start (HH:mm)"));
        LocalTime end = LocalTime.parse(prompt("End (HH:mm)"));
        int capacity = Integer.parseInt(prompt("Capacity"));
        String equipmentText = prompt("Equipment ids, comma separated (blank for none)");
        List<String> equipmentIds = equipmentText.isBlank()
                ? List.of()
                : Arrays.stream(equipmentText.split(",")).map(String::trim).toList();
        TimeSlot slot = new TimeSlot(date.atTime(start), date.atTime(end));
        if (weekly) {
            int weeks = Integer.parseInt(prompt("Number of weeks"));
            out.println("Scheduled " + system.scheduleWeeklySession(current, id, title, studio, slot, capacity,
                    equipmentIds, weeks).size() + " weekly sessions of " + title);
        } else {
            out.println("Scheduled " + system.scheduleSession(current, id, title, studio, slot, capacity, equipmentIds));
        }
    }

    private void reportFault() {
        String equipmentId = prompt("Equipment id");
        String description = prompt("Description");
        Urgency urgency = Urgency.valueOf(prompt("Urgency " + Arrays.toString(Urgency.values())).toUpperCase());
        out.println("Reported " + system.reportFault(current, equipmentId, description, urgency));
    }

    private void showNotifications() {
        List<String> inbox = system.inbox(current);
        if (inbox.isEmpty()) {
            out.println("No notifications.");
        }
        inbox.forEach(out::println);
    }

    private String prompt(String label) {
        out.print(label + ": ");
        out.flush();
        try {
            String line = in.readLine();
            if (line == null) {
                throw new EndOfInput();
            }
            return line.trim();
        } catch (IOException error) {
            throw new UncheckedIOException(error);
        }
    }

    /** Signals that there is no more input, so the menu can stop cleanly. */
    private static final class EndOfInput extends RuntimeException {
        EndOfInput() {
            super(null, null, false, false);
        }
    }
}
