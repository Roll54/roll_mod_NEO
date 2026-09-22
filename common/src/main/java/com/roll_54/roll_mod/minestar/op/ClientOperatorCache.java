package com.roll_54.roll_mod.minestar.op;

import java.util.List;

/**
 * The operator roster this client was last sent.
 *
 * <p>Empty for everyone but an administrator, which is what keeps the panel out of other people's
 * way without the tab having to be built differently for them.
 */
public final class ClientOperatorCache {

    public static volatile List<OperatorEntry> ENTRIES = List.of();

    private ClientOperatorCache() {}
}
