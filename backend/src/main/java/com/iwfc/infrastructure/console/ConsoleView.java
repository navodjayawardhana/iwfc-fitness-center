package com.iwfc.infrastructure.console;

import com.iwfc.domain.model.Equipment;
import com.iwfc.domain.model.FitnessSession;
import com.iwfc.domain.model.MaintenanceRequest;
import com.iwfc.domain.model.User;

import java.io.PrintStream;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

/**
 * Presentation only: banner, sectioned menu, aligned tables and coloured messages.
 * It never reads input and never decides business rules; {@link ConsoleMenu} drives it.
 */
public class ConsoleView {

    private static final String RESET = "\u001B[0m";
    private static final String BOLD = "\u001B[1m";
    private static final String DIM = "\u001B[2m";
    private static final String RED = "\u001B[31m";
    private static final String GREEN = "\u001B[32m";
    private static final String CYAN = "\u001B[36m";
    private static final String YELLOW = "\u001B[33m";

    private static final int WIDTH = 62;
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EEE dd MMM HH:mm", Locale.ENGLISH);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH);

    /** One menu entry: its number, section, label, who may use it (as text) and the permission that decides it. */
    private record Option(int number, String section, String label, String audience, Predicate<User> allowed) {
    }

    private static final List<Option> OPTIONS = List.of(
            new Option(1, "EQUIPMENT", "List equipment", "", user -> true),
            new Option(2, "EQUIPMENT", "Add equipment", "Administrator only", User::canManageEquipment),
            new Option(3, "EQUIPMENT", "Edit equipment", "Administrator only", User::canManageEquipment),
            new Option(4, "EQUIPMENT", "Deactivate equipment", "Administrator only", User::canManageEquipment),
            new Option(5, "EQUIPMENT", "Log equipment usage", "Instructor only", User::canLogEquipmentUsage),
            new Option(6, "SESSIONS", "View available sessions", "", user -> true),
            new Option(7, "SESSIONS", "Schedule a session", "Instructor only", User::canScheduleSessions),
            new Option(8, "SESSIONS", "Schedule a weekly class", "Instructor only", User::canScheduleSessions),
            new Option(9, "SESSIONS", "Book a session", "Member only", User::canBookSessions),
            new Option(10, "SESSIONS", "Cancel my booking", "Member only", User::canBookSessions),
            new Option(11, "SESSIONS", "Cancel a session", "Instructor only", User::canScheduleSessions),
            new Option(12, "SESSIONS", "Complete a session", "Instructor only", User::canLogEquipmentUsage),
            new Option(13, "MAINTENANCE", "Report a fault", "Instructor only", User::canReportFaults),
            new Option(14, "MAINTENANCE", "View maintenance requests", "Administrator or Instructor",
                    user -> user.canViewMaintenanceLog() || user.canReportFaults()),
            new Option(15, "MAINTENANCE", "Assign a maintenance request", "Administrator only", User::canManageMaintenance),
            new Option(16, "MAINTENANCE", "Update maintenance progress", "Administrator only", User::canManageMaintenance),
            new Option(17, "MAINTENANCE", "Complete a maintenance request", "Administrator only", User::canManageMaintenance),
            new Option(18, "MAINTENANCE", "View maintenance activity log", "Administrator only", User::canViewMaintenanceLog),
            new Option(19, "ACCOUNT", "My notifications", "", user -> true),
            new Option(20, "ACCOUNT", "Switch user", "", user -> true));

    private final PrintStream out;
    private final boolean colour;

    public ConsoleView(PrintStream out, boolean colour) {
        this.out = out;
        this.colour = colour;
    }

    // ---- frame ----------------------------------------------------------------------------------

    public void banner() {
        String title = "FitPulse  |  Intelligent Wellness and Fitness Center";
        String border = "+" + "-".repeat(WIDTH - 2) + "+";
        int left = (WIDTH - 2 - title.length()) / 2;
        int right = WIDTH - 2 - title.length() - left;
        out.println(paint(CYAN + BOLD, border));
        out.println(paint(CYAN + BOLD, "|" + " ".repeat(left) + title + " ".repeat(right) + "|"));
        out.println(paint(CYAN + BOLD, border));
    }

    public void welcome(User user) {
        out.println(paint(GREEN, "Welcome, " + user.name() + " (" + user.roleName() + ")"));
    }

    public void menu(User user) {
        out.println();
        out.println(paint(BOLD, " Signed in as " + user.name() + " (" + user.roleName() + ")"));
        String section = "";
        for (Option option : OPTIONS) {
            if (!option.section().equals(section)) {
                section = option.section();
                out.println();
                out.println(paint(CYAN + BOLD, " " + section));
            }
            boolean allowed = option.allowed().test(user);
            String hint = allowed || option.audience().isEmpty() ? "" : option.audience();
            String line = String.format("  %3d  %-32s%s", option.number(), option.label(), hint);
            out.println(allowed ? line : paint(DIM, line));
        }
        out.println();
        out.println(String.format("  %3d  %s", 0, "Exit"));
    }

    // ---- messages -------------------------------------------------------------------------------

    public void success(String message) {
        out.println(paint(GREEN, "[OK] " + message));
    }

    public void failure(String kind, String message) {
        out.println(paint(RED, "[" + kind + "] " + message));
    }

    public void info(String message) {
        out.println(paint(DIM, message));
    }

    public void heading(String text) {
        out.println();
        out.println(paint(YELLOW + BOLD, "== " + text + " =="));
    }

    // ---- data -----------------------------------------------------------------------------------

    public void numbered(List<String> lines, String whenEmpty) {
        if (lines.isEmpty()) {
            out.println(whenEmpty);
            return;
        }
        for (int index = 0; index < lines.size(); index++) {
            out.println("  " + (index + 1) + ". " + lines.get(index));
        }
    }

    public void equipmentTable(List<Equipment> items) {
        List<List<String>> rows = new ArrayList<>();
        for (Equipment item : items) {
            String status = item.isActive() ? item.status().name() : "DEACTIVATED";
            if (item.isActive() && item.needsMaintenance()) {
                status += " (maintenance due)";
            }
            rows.add(List.of(item.id(), item.name(), item.location().name(), status,
                    String.format(Locale.ROOT, "%.1f / %s", item.hoursSinceMaintenance(),
                            plain(item.maintenanceThresholdHours()))));
        }
        table(List.of("ID", "Name", "Location", "Status", "Hours since maintenance"), rows);
    }

    public void sessionTable(List<FitnessSession> sessions) {
        List<List<String>> rows = new ArrayList<>();
        for (FitnessSession session : sessions) {
            rows.add(List.of(session.id(), session.title(),
                    session.slot().start().format(DAY) + "-" + session.slot().end().format(TIME),
                    session.studio().name(), session.instructor().name(),
                    session.bookedCount() + "/" + session.capacity()));
        }
        table(List.of("ID", "Title", "When", "Studio", "Instructor", "Booked"), rows);
    }

    public void requestTable(List<MaintenanceRequest> requests) {
        List<List<String>> rows = new ArrayList<>();
        for (MaintenanceRequest request : requests) {
            rows.add(List.of(request.id(), request.equipmentId(), request.urgency().name(), request.status().name(),
                    request.assignedTo().orElse("-"), request.description()));
        }
        table(List.of("ID", "Equipment", "Urgency", "Status", "Technician", "Description"), rows);
    }

    /** Draws a bordered table with every column as wide as its longest cell. */
    public void table(List<String> headers, List<List<String>> rows) {
        if (rows.isEmpty()) {
            out.println("(none)");
            return;
        }
        int[] widths = new int[headers.size()];
        for (int column = 0; column < widths.length; column++) {
            widths[column] = headers.get(column).length();
            for (List<String> row : rows) {
                widths[column] = Math.max(widths[column], row.get(column).length());
            }
        }
        String border = border(widths);
        out.println(border);
        out.println(paint(BOLD, line(headers, widths)));
        out.println(border);
        rows.forEach(row -> out.println(line(row, widths)));
        out.println(border);
    }

    // ---- helpers --------------------------------------------------------------------------------

    private static String border(int[] widths) {
        StringBuilder border = new StringBuilder("+");
        for (int width : widths) {
            border.append("-".repeat(width + 2)).append('+');
        }
        return border.toString();
    }

    private static String line(List<String> cells, int[] widths) {
        StringBuilder line = new StringBuilder("|");
        for (int column = 0; column < widths.length; column++) {
            line.append(' ').append(String.format("%-" + widths[column] + "s", cells.get(column))).append(" |");
        }
        return line.toString();
    }

    private static String plain(double number) {
        return number == Math.rint(number) ? String.valueOf((long) number) : String.valueOf(number);
    }

    private String paint(String code, String text) {
        return colour ? code + text + RESET : text;
    }
}
