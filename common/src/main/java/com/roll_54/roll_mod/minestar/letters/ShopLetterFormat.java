package com.roll_54.roll_mod.minestar.letters;

import com.roll_54.roll_mod.config.MyConfig;
import com.roll_54.roll_mod.util.ServerLang;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * What a Minestar shop letter looks like: the thank-you text, the product list in the body, and one
 * console command reward per product. Shared by the server's {@code ShopLetters}, which delivers
 * real purchases, and {@code /rollmod debug fill}, which writes samples of the same letter.
 *
 * <p>A letter holds {@link Letter#MAX_REWARDS}; past that the products go in a further letter rather
 * than being lost, so one purchase list becomes one or more {@link Draft}s.
 */
public final class ShopLetterFormat {

    public static final String KEY = "letters.roll_mod.shop.";
    /** The starcoin, as the rest of the mod draws money — see {@code VendorUIHelper.STARCOIN}. */
    public static final String ICON = "roll_mod:textures/gui/container/starcoin.png";

    /**
     * One product as the letter sees it.
     *
     * @param commands run from the console on accept, {@code %player%} replaced; one per line
     */
    public record Product(String label, String commands, int quantity) {}

    /** One letter's worth: products {@code [from, to)} of the list it was drafted from. */
    public record Draft(String author, String title, String body, List<LetterReward> rewards, int from, int to) {}

    private ShopLetterFormat() {}

    public static String author() {
        return ServerLang.uk(KEY + "author");
    }

    /** Splits {@code products} into letters for the player called {@code name}. */
    public static List<Draft> drafts(String name, List<Product> products) {
        List<Draft> drafts = new ArrayList<>();
        for (int from = 0; from < products.size(); from += Letter.MAX_REWARDS) {
            int to = Math.min(products.size(), from + Letter.MAX_REWARDS);
            List<LetterReward> rewards = new ArrayList<>(to - from);
            StringBuilder list = new StringBuilder();
            for (Product product : products.subList(from, to)) {
                int quantity = Math.max(1, product.quantity());
                rewards.add(LetterReward.command(product.commands(), product.label(), quantity, true));
                if (!list.isEmpty()) list.append('\n');
                list.append("- &e").append(product.label()).append("&r ×").append(quantity);
            }
            String body = ServerLang.uk(KEY + "body.list", name, list.toString());
            if (body.length() > Letter.MAX_BODY) body = body.substring(0, Letter.MAX_BODY);
            drafts.add(new Draft(author(), ServerLang.uk(KEY + "title"), body, rewards, from, to));
        }
        return drafts;
    }

    /** Delivers {@code draft} to {@code recipient} — silently; the caller says so in its own words. */
    public static Letter send(MinecraftServer server, UUID recipient, Draft draft) {
        return LetterService.sendPersonal(server, recipient, draft.author(), draft.title(), draft.body(),
                ICON, MyConfig.INSTANCE.letters.shopLifetime(), draft.rewards(), false);
    }

    /**
     * {@code draft} as a letter that is not stored anywhere yet, under {@code title}, expiring as a
     * real delivery sent at {@code sentAt} would.
     */
    public static Letter letter(Draft draft, String title, UUID recipient, long sentAt) {
        long lifetime = MyConfig.INSTANCE.letters.shopLifetime();
        return new Letter(UUID.randomUUID(), title, draft.body(), draft.author(), sentAt, sentAt,
                lifetime <= 0L ? 0L : sentAt + lifetime,
                draft.rewards(), ICON, Map.of(), recipient);
    }
}
