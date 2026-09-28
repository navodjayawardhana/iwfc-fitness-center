package com.iwfc.application.notification;

import java.util.List;

/** Short wellness tips added to booking confirmations. The tip is picked by position and wraps around. */
public final class WellnessTips {

    private static final List<String> TIPS = List.of(
            "Drink a glass of water before and after your session.",
            "Warm up for five minutes so your muscles are ready.",
            "Bring a towel and a water bottle to class.",
            "Sleep is when your body recovers, so aim for seven to eight hours.",
            "Eat something light one to two hours before you train.",
            "Listen to your body: stop and tell your instructor if something hurts.",
            "Finish with a short stretch to help your muscles cool down.");

    private WellnessTips() {
    }

    public static int count() {
        return TIPS.size();
    }

    /** Works for any position, including negative ones and hash codes. */
    public static String tip(int position) {
        return TIPS.get(Math.floorMod(position, TIPS.size()));
    }
}
