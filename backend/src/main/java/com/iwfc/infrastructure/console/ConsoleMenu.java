package com.iwfc.infrastructure.console;

import com.iwfc.application.facade.IwfcFacade;
import com.iwfc.application.security.AuthSession;
import com.iwfc.domain.exception.DuplicateEquipmentException;
import com.iwfc.domain.exception.DuplicateUserException;
import com.iwfc.domain.exception.InvalidCredentialsException;
import com.iwfc.domain.exception.InvalidBookingException;
import com.iwfc.domain.exception.ResourceNotFoundException;
import com.iwfc.domain.exception.UnauthorizedAccessException;
import com.iwfc.domain.model.Equipment;
import com.iwfc.domain.model.EquipmentType;
import com.iwfc.domain.model.Location;
import com.iwfc.domain.model.Role;
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
 * Console adapter: thin. It reads input, calls the {@link IwfcFacade} and hands results to {@link ConsoleView}.
 * Business rules stay in the domain; here the custom exceptions are turned into plain messages.
 */
public class ConsoleMenu {

    private final IwfcFacade system;
    private final BufferedReader in;
    private final PrintStream out;
    private final ConsoleView view;
    private final boolean maskPasswords;
    private User current;
    private String token;

    public ConsoleMenu(IwfcFacade system, InputStream in, PrintStream out) {
        this(system, in, out, false);
    }

    public ConsoleMenu(IwfcFacade system, InputStream in, PrintStream out, boolean colour) {
        this(system, in, out, colour, false);
    }

    /** With maskPasswords set, typing is hidden on a real terminal; tests and piped input read a plain line. */
    public ConsoleMenu(IwfcFacade system, InputStream in, PrintStream out, boolean colour, boolean maskPasswords) {
        this.system = system;
        this.maskPasswords = maskPasswords;
        this.in = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        this.out = out;
        this.view = new ConsoleView(out, colour);
    }

    public void run() {
        view.banner();
        try {
            login();
            view.menu(current);
            boolean running = true;
            while (running) {
                running = handle(prompt("Choose an option (m = menu, 0 = exit)"));
            }
        } catch (EndOfInput endOfInput) {
            out.println();
        }
        signOut();
        view.info("Goodbye.");
    }

    private void signOut() {
        if (token != null) {
            system.signOut(token);
            token = null;
        }
        current = null;
    }

    private void login() {
        view.info("Demo users: A-1 Administrator, I-1 / I-2 Instructor, M-1 / M-2 Member");
        while (current == null) {
            String userId = prompt("User id");
            String password = promptSecret("Password");
            try {
                AuthSession session = system.signIn(userId, password);
                current = session.user();
                token = session.token();
                view.welcome(current);
            } catch (InvalidCredentialsException failed) {
                view.failure("Sign-in failed", failed.getMessage());
            }
        }
    }

    /** Returns false when the user chose to exit. */
    private boolean handle(String choice) {
        if (choice.equals("0")) {
            return false;
        }
        try {
            switch (choice) {
                case "1" -> showEquipment();
                case "2" -> addEquipment();
                case "3" -> editEquipment();
                case "4" -> {
                    system.deactivateEquipment(current, prompt("Equipment id"));
                    view.success("Equipment deactivated.");
                }
                case "5" -> logUsage();
                case "6" -> showSessions();
                case "7" -> scheduleSession(false);
                case "8" -> scheduleSession(true);
                case "9" -> {
                    system.bookSession(prompt("Session id"), current);
                    view.success("Booked.");
                }
                case "10" -> {
                    system.cancelBooking(prompt("Session id"), current);
                    view.success("Booking cancelled.");
                }
                case "11" -> {
                    system.cancelSession(current, prompt("Session id"));
                    view.success("Session cancelled.");
                }
                case "12" -> {
                    system.completeSession(current, prompt("Session id"));
                    view.success("Session completed. Usage logged for its equipment.");
                }
                case "13" -> reportFault();
                case "14" -> showRequests();
                case "15" -> {
                    system.assignMaintenance(current, prompt("Request id"), prompt("Technician"));
                    view.success("Request assigned.");
                }
                case "16" -> {
                    system.updateMaintenanceProgress(current, prompt("Request id"), prompt("Progress note"));
                    view.success("Progress recorded.");
                }
                case "17" -> {
                    system.completeMaintenance(current, prompt("Request id"));
                    view.success("Request completed.");
                }
                case "18" -> {
                    view.heading("Maintenance activity log");
                    view.numbered(system.maintenanceActivityLog(current), "Nothing logged yet.");
                }
                case "19" -> {
                    view.heading("My notifications");
                    view.numbered(system.inbox(current), "No notifications yet.");
                }
                case "20" -> {
                    signOut();
                    login();
                    view.menu(current);
                }
                case "21" -> {
                    view.heading("User accounts");
                    view.userTable(system.listUsers(current));
                }
                case "22" -> registerUser();
                case "23" -> {
                    system.deactivateUser(current, prompt("User id to deactivate"));
                    view.success("User deactivated. They can no longer sign in.");
                }
                case "m", "M", "menu", "?" -> view.menu(current);
                default -> view.failure("Unknown option", choice);
            }
        } catch (EndOfInput endOfInput) {
            throw endOfInput;
        } catch (UnauthorizedAccessException error) {
            view.failure("Access denied", error.getMessage());
        } catch (InvalidBookingException error) {
            view.failure("Invalid booking", error.getMessage());
        } catch (DuplicateEquipmentException | DuplicateUserException error) {
            view.failure("Duplicate", error.getMessage());
        } catch (ResourceNotFoundException error) {
            view.failure("Not found", error.getMessage());
        } catch (java.time.format.DateTimeParseException | NumberFormatException error) {
            view.failure("Invalid input", "Could not understand that value: " + error.getMessage());
        } catch (IllegalArgumentException | IllegalStateException error) {
            view.failure("Error", error.getMessage());
        } catch (RuntimeException error) {
            view.failure("Rejected", error.getMessage());
        }
        return true;
    }

    private void showEquipment() {
        view.heading("Equipment");
        view.equipmentTable(system.listEquipment());
    }

    private void showSessions() {
        view.heading("Sessions with free spots");
        view.sessionTable(system.availableSessions());
    }

    /** Administrators see the whole log; an instructor sees only the faults they reported. */
    private void showRequests() {
        boolean ownOnly = current.canReportFaults() && !current.canViewMaintenanceLog();
        view.heading(ownOnly ? "My maintenance requests" : "Maintenance requests");
        view.requestTable(ownOnly ? system.myMaintenanceRequests(current) : system.maintenanceRequests(current));
    }

    private void addEquipment() {
        String types = Arrays.toString(EquipmentType.values());
        EquipmentType type = EquipmentType.valueOf(prompt("Type " + types).toUpperCase());
        String id = prompt("Equipment id");
        String name = prompt("Name");
        Location location = new Location(prompt("Location"));
        Equipment added = system.addEquipment(current, type, id, name, location);
        view.success("Added " + added);
    }

    private void registerUser() {
        String id = prompt("New user id");
        String name = prompt("Full name");
        Role role = Role.parse(prompt("Role " + Arrays.toString(Role.values())));
        String password = promptSecret("Password (at least 8 characters)");
        User created = system.registerUser(current, role, id, name, password);
        view.success("Registered " + created);
    }

    private void editEquipment() {
        String id = prompt("Equipment id");
        String name = prompt("New name");
        Location location = new Location(prompt("New location"));
        system.editEquipment(current, id, name, location);
        view.success("Equipment updated.");
    }

    private void logUsage() {
        String id = prompt("Equipment id");
        double hours = Double.parseDouble(prompt("Hours used"));
        system.logEquipmentUsage(current, id, hours);
        view.success("Usage logged.");
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
            int created = system.scheduleWeeklySession(current, id, title, studio, slot, capacity, equipmentIds, weeks).size();
            view.success("Scheduled " + created + " weekly sessions of " + title);
        } else {
            view.success("Scheduled " + system.scheduleSession(current, id, title, studio, slot, capacity, equipmentIds));
        }
    }

    private void reportFault() {
        String equipmentId = prompt("Equipment id");
        String description = prompt("Description");
        Urgency urgency = Urgency.valueOf(prompt("Urgency " + Arrays.toString(Urgency.values())).toUpperCase());
        view.success("Reported " + system.reportFault(current, equipmentId, description, urgency));
    }

    private String prompt(String label) {
        out.print(" > " + label + ": ");
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

    /** Reads a password without echoing it when a real terminal is attached; otherwise reads a plain line. */
    private String promptSecret(String label) {
        java.io.Console console = System.console();
        if (maskPasswords && console != null) {
            char[] typed = console.readPassword(" > %s: ", label);
            if (typed == null) {
                throw new EndOfInput();
            }
            String password = new String(typed);
            Arrays.fill(typed, ' ');
            return password;
        }
        return prompt(label);
    }

    /** Signals that there is no more input, so the menu can stop cleanly. */
    private static final class EndOfInput extends RuntimeException {
        EndOfInput() {
            super(null, null, false, false);
        }
    }
}
