package com.iwfc.application;

import com.iwfc.application.notification.WellnessTips;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Members get a short wellness tip with their booking confirmation. */
class WellnessTipsTest {

    // Z
    @Test
    void should_give_a_tip_for_the_first_position() {
        assertFalse(WellnessTips.tip(0).isBlank());
    }

    // O
    @Test
    void should_give_the_same_tip_for_the_same_position() {
        assertEquals(WellnessTips.tip(3), WellnessTips.tip(3));
    }

    // M
    @Test
    void should_offer_several_different_tips() {
        Set<String> tips = new HashSet<>();
        for (int position = 0; position < WellnessTips.count(); position++) {
            tips.add(WellnessTips.tip(position));
        }

        assertEquals(WellnessTips.count(), tips.size());
        assertTrue(WellnessTips.count() >= 5);
    }

    // B
    @Test
    void should_start_over_after_the_last_tip() {
        assertEquals(WellnessTips.tip(0), WellnessTips.tip(WellnessTips.count()));
        assertNotEquals(WellnessTips.tip(0), WellnessTips.tip(1));
    }

    // E
    @Test
    void should_cope_with_negative_positions_and_hash_codes() {
        assertFalse(WellnessTips.tip(-1).isBlank());
        assertFalse(WellnessTips.tip(Integer.MIN_VALUE).isBlank());
    }

    // S
    @Test
    void should_never_return_an_empty_tip() {
        for (int position = 0; position < 50; position++) {
            assertFalse(WellnessTips.tip(position).isBlank());
        }
    }
}
