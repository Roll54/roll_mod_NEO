package com.roll_54.roll_mod.data.datagen.ore;

import java.util.HashMap;
import java.util.Map;

/**
 * Ukrainian forms of each ore's name that {@link OreLangProvider} needs to build natural item names instead of
 * "Назва (Тип)": the genitive ("пил гематиту"), the gender (to agree "необроблений гематит / необроблена сірка"),
 * and, for the few materials Ukrainian has a set adjective for, that adjective ("сірчаний пил").
 *
 * <p>Rule: a dust is "пил + genitive" ("Пил гематиту"); where the language has an established adjective it is
 * "adjective + пил" instead ("Сірчаний пил", "Золотий пил"). Every other item is "adjective + руда/блок +
 * genitive" or an adjective agreed with the name itself.
 */
final class UkOreGrammar {

    enum Gender { M, F, N, PL }

    /** @param genitive  the name in the genitive, lower-cased as it stands mid-phrase
     *  @param dustAdj   the dust's adjective in the masculine ("сірчаний"), or {@code null} for "пил + genitive" */
    record Forms(String genitive, Gender gender, String dustAdj) {}

    private static final Map<String, Forms> FORMS = new HashMap<>();

    private static void m(String id, String genitive) { FORMS.put(id, new Forms(genitive, Gender.M, null)); }
    private static void f(String id, String genitive) { FORMS.put(id, new Forms(genitive, Gender.F, null)); }
    private static void n(String id, String genitive) { FORMS.put(id, new Forms(genitive, Gender.N, null)); }
    private static void adj(String id, String genitive, Gender gender, String dustAdj) {
        FORMS.put(id, new Forms(genitive, gender, dustAdj));
    }

    static {
        f("mica", "слюди");
        m("hematite", "гематиту");
        m("yellow_limonite", "жовтого лімоніту");
        m("biotite", "біотиту");
        m("magnetite", "магнетиту");
        m("garnierite", "гарнієриту");
        m("pentlandite", "пентландиту");
        m("chalcopyrite", "халькопіриту");
        m("certus_quartz", "істинного кварцу");
        m("pyrochlore", "пірохлору");
        m("pyrope", "піропу");
        m("apatite", "апатиту");
        m("galena", "галеніту");
        m("pyrolusite", "піролюзиту");
        m("chromite", "хроміту");
        m("malachite", "малахіту");
        m("fluorite", "флюориту");
        m("pyrite", "піриту");
        f("cinnabar", "кіноварі");
        m("peridot", "перидоту");
        m("sodalite", "содаліту");
        m("lazurite", "лазуриту");
        f("lapis_lazuli", "ляпіс-лазурі");
        m("tetrahedrite", "тетраедриту");
        m("stibnite", "стибніту");
        m("ilmenite", "ільменіту");
        m("blue_topaz", "блакитного топазу");
        m("topaz", "топазу");
        m("chalcocite", "халькозину");
        m("bornite", "борніту");
        adj("sulfur", "сірки", Gender.F, "сірчаний");
        m("sphalerite", "сфалериту");
        m("monazite", "монациту");
        m("ruby", "рубіну");
        m("redstone", "редстоуну");
        f("saltpeter", "селітри");
        m("emerald", "смарагду");
        m("beryllium", "берилію");
        m("tantalite", "танталіту");
        m("molybdenite", "молібденіту");
        m("wulfenite", "вульфеніту");
        m("powellite", "повеліту");
        m("molybdenum", "молібдену");
        adj("gold", "золота", Gender.N, "золотий");
        m("goethite", "гетиту");
        m("vanadium_magnetite", "ванадієвого магнетиту");
        m("rutile", "рутилу");
        adj("silver", "срібла", Gender.N, "срібний");
        m("bauxite", "бокситу");
        f("salt", "солі");
        f("rock_salt", "кам'яної солі");
        m("lepidolite", "лепідоліту");
        m("tungstate", "вольфрамату");
        m("scheelite", "шеєліту");
        m("lithium", "літію");
        m("sheldonite", "купериту");
        m("uraninite", "уранініту");
        m("thorium", "торію");
        m("pitchblende", "пітчбленду");
        m("cassiterite", "каситериту");
        m("rhodochrosite", "родохрозиту");
        adj("lead", "свинцю", Gender.M, "свинцевий");
        m("olivine", "олівіну");
        f("trona", "трони");
        m("bismuth", "бісмуту");
        adj("quartz", "кварцу", Gender.M, "кварцовий");
        adj("diamond", "алмазу", Gender.M, "алмазний");
        m("bort", "борту");
        m("cassiterite_sand", "каситеритового піску");
        m("iridium", "іридію");
        f("gold_amalgam", "золотої амальгами");
        f("silver_amalgam", "срібної амальгами");
        adj("coal", "вугілля", Gender.N, "вугільний");
        n("lignite_coal", "бурого вугілля");
        n("azure_silver", "лазурного срібла");
        m("stannite", "станніту");
        n("crimson_iron", "багряного заліза");
        m("nickel", "нікелю");
        f("borax", "бури");
        m("sunnarium_orthosilicate", "ортосилікату санарію");
        m("sunnarium_sulfophosphoorthosilicate", "сульфофосфоортосилікату санарію");
        m("fluosunnarite", "флюосанариту");
        m("rheniite", "реніїту");
        m("antimony", "самородного стибію");
        m("livingstonite", "лівінгстоніту");
    }

    private UkOreGrammar() {}

    /** The forms for {@code id}; an ore missing here fails datagen, so a new ore cannot slip in unnamed. */
    static Forms of(String id) {
        Forms forms = FORMS.get(id);
        if (forms == null) throw new IllegalStateException("No Ukrainian name forms for ore '" + id + "' in UkOreGrammar");
        return forms;
    }

    /** {@code masculine} ("необроблений") agreed with {@code gender}: -ий → -а/-е/-і. */
    static String agree(String masculine, Gender gender) {
        String stem = masculine.substring(0, masculine.length() - 2);
        return switch (gender) {
            case M -> masculine;
            case F -> stem + "а";
            case N -> stem + "е";
            case PL -> stem + "і";
        };
    }

    /** The genitive of {@code masculine} agreed with {@code gender}: -ого / -ої / -их. */
    static String agreeGenitive(String masculine, Gender gender) {
        String stem = masculine.substring(0, masculine.length() - 2);
        return switch (gender) {
            case M, N -> stem + "ого";
            case F -> stem + "ої";
            case PL -> stem + "их";
        };
    }

    static String capitalize(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
