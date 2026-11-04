package com.roll_54.roll_mod.items.electricItems.orescanner;

/**
 * The last scan sent to this client, read by {@code OreScannerUI}. {@code volatile} because the network thread
 * writes it and the render thread reads it; the sequence number lets an open screen notice a new scan.
 */
public final class ClientOreScanCache {

    public static volatile OreScanResult LAST;
    public static volatile int SEQUENCE;

    private ClientOreScanCache() {}

    public static void set(OreScanResult result) {
        LAST = result;
        SEQUENCE++;
    }
}
