package com.iwfc;

import com.iwfc.infrastructure.IwfcBootstrap;
import com.iwfc.infrastructure.console.ConsoleMenu;

/** Starts the console version of the IWFC prototype with sample data. */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        new ConsoleMenu(IwfcBootstrap.seeded(), System.in, System.out).run();
    }
}
