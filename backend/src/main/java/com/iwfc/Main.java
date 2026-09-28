package com.iwfc;

import com.iwfc.infrastructure.IwfcBootstrap;
import com.iwfc.infrastructure.console.ConsoleMenu;

import java.io.Console;

/** Starts the console version of the prototype with sample data. */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        boolean terminal = isTerminal();
        new ConsoleMenu(IwfcBootstrap.seededSecure(), System.in, System.out, terminal && supportsColour(), terminal).run();
    }

    private static boolean isTerminal() {
        Console console = System.console();
        return console != null && console.isTerminal();
    }

    /** Colours only when a real terminal is attached and the user has not opted out (NO_COLOR). */
    private static boolean supportsColour() {
        return System.getenv("NO_COLOR") == null;
    }
}
