package com.roll_54.roll_mod.minestar.letters;

import java.util.List;

/** The letters as this client last received them, newest first. */
public final class ClientLetterCache {

    public static volatile List<LetterView> LETTERS = List.of();

    /** Bumped on every arrival, so tabs can tell new data from the same data cheaply. */
    public static volatile int VERSION;

    private ClientLetterCache() {}

    public static void accept(List<LetterView> letters) {
        LETTERS = letters;
        VERSION++;
    }
}
