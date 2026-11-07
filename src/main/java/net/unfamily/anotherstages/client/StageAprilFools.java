package net.unfamily.anotherstages.client;

import java.time.LocalDate;

/** Shared April Fools (1 April) easter-egg switch for Unfamiliar Fish textures/names. */
public final class StageAprilFools {
    private StageAprilFools() {}

    public static boolean isAprilFools() {
        LocalDate today = LocalDate.now();
        return today.getMonthValue() == 4 && today.getDayOfMonth() == 1;
    }
}
