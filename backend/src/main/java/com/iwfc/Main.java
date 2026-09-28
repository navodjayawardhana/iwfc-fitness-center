package com.iwfc;

import com.iwfc.application.facade.IwfcFacade;
import com.iwfc.infrastructure.IwfcBootstrap;
import com.iwfc.infrastructure.console.ConsoleMenu;

import java.io.Console;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Map;

/**
 * Starts the console version of FitPulse. Storage comes from the FITPULSE_* environment variables
 * (memory by default, MySQL when FITPULSE_STORAGE=mysql).
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        run(System.in, System.out, System.getenv(), isTerminal());
    }

    /** The whole start-up, with its inputs passed in so it can be tested without a real terminal. */
    static void run(InputStream in, PrintStream out, Map<String, String> environment, boolean terminal) {
        IwfcFacade system = IwfcBootstrap.configured(environment);
        new ConsoleMenu(system, in, out, useColour(terminal, environment), terminal).run();
    }

    /** Colours only when a real terminal is attached and the user has not opted out (NO_COLOR). */
    static boolean useColour(boolean terminal, Map<String, String> environment) {
        return terminal && !environment.containsKey("NO_COLOR");
    }

    private static boolean isTerminal() {
        Console console = System.console();
        return console != null && console.isTerminal();
    }
}
