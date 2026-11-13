package com.roll_54.roll_mod.minestar.fzzyui;

import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Selector;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Slider;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Switch;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextArea;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.gui.ui.utils.UIElementProvider;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import me.fzzyhmstrs.fzzy_config.entry.Entry;
import me.fzzyhmstrs.fzzy_config.screen.context.ContextInput;
import me.fzzyhmstrs.fzzy_config.screen.context.FzzyKeybind;
import me.fzzyhmstrs.fzzy_config.screen.context.FzzyKeybindSimple;
import me.fzzyhmstrs.fzzy_config.screen.context.FzzyKeybindUnbound;
import me.fzzyhmstrs.fzzy_config.util.FcText;
import me.fzzyhmstrs.fzzy_config.util.TriState;
import me.fzzyhmstrs.fzzy_config.validation.ValidatedField;
import me.fzzyhmstrs.fzzy_config.validation.collection.ValidatedIdentifierMap;
import me.fzzyhmstrs.fzzy_config.validation.collection.ValidatedList;
import me.fzzyhmstrs.fzzy_config.validation.collection.ValidatedMap;
import me.fzzyhmstrs.fzzy_config.validation.collection.ValidatedSet;
import me.fzzyhmstrs.fzzy_config.validation.collection.ValidatedStringMap;
import me.fzzyhmstrs.fzzy_config.validation.minecraft.ValidatedIdentifier;
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean;
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedColor;
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedEnum;
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedKeybind;
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedString;
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedTriState;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedNumber;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * The editors of the LdLib config screen, one per Fzzy value type, all built the same way: a
 * {@code template} entry says what kind of value it is (type, bounds, element handlers) and checks
 * a candidate value; a {@link Binding} is where the value is read and written. For a config entry
 * both are the live field, so a write is a recorded change Fzzy can undo and apply. For an element of
 * a list or map the template is the collection's element handler and the binding writes into the
 * collection being edited, which is then handed to the field as a whole.
 *
 * <p>Types the pack does not use, or that have no natural control (objects, pairs, expressions,
 * ingredients...), get a text box holding the value as TOML — exactly what Fzzy writes in the config
 * file — so every entry stays editable.
 */
@OnlyIn(Dist.CLIENT)
final class ConfigEditors {

    /** Where an editor reads and writes its value. {@link #set} refuses values that do not validate. */
    interface Binding {
        Object get();

        boolean set(Object value);
    }

    /** An editor: the control in the row, and for collections a panel shown under the row. */
    record Editor(UIElement inline, @Nullable UIElement panel) {
        static Editor of(UIElement inline) {
            return new Editor(inline, null);
        }
    }

    static final int CONTROL_H = 14;

    /** Lists and maps bigger than this get a filter box in their panel. */
    private static final int FILTER_FROM = 10;

    private ConfigEditors() {}

    /** A config entry: reads and writes the live field, so every write is a recorded change. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    static Binding fieldBinding(ValidatedField<?> field, Runnable onChanged) {
        return new Binding() {
            @Override
            public Object get() {
                return field.get();
            }

            @Override
            public boolean set(Object value) {
                if (value == null || !field.isValidEntry(value)) return false;
                ((ValidatedField) field).accept(value);
                onChanged.run();
                return true;
            }
        };
    }

    /** The editor for a value of {@code template}'s kind, stored through {@code binding}. */
    static Editor create(Entry<?, ?> template, Binding binding, String scope, KeyCapture keys) {
        if (template instanceof ValidatedBoolean) return Editor.of(switchEditor(binding));
        if (template instanceof ValidatedNumber<?> number) return Editor.of(numberEditor(number, binding));
        if (template instanceof ValidatedEnum<?>) return Editor.of(enumEditor(template, binding));
        if (template instanceof ValidatedTriState) {
            return Editor.of(selector(Arrays.asList(TriState.values()), binding, v -> Component.literal(pretty(((Enum<?>) v).name()))));
        }
        if (template instanceof ValidatedString) return Editor.of(textEditor(template, binding, s -> s, String::valueOf));
        if (template instanceof ValidatedIdentifier) {
            return Editor.of(textEditor(template, binding, ResourceLocation::tryParse, String::valueOf));
        }
        if (template instanceof ValidatedColor) return Editor.of(colorEditor(template, binding));
        if (template instanceof ValidatedKeybind) return Editor.of(keybindEditor(binding, keys));
        if (template instanceof ValidatedList<?> || template instanceof ValidatedSet<?>) {
            Entry<?, ?> element = FzzyBridge.elementHandler(template);
            if (element != null) return listEditor(element, binding, template instanceof ValidatedSet<?>, scope, keys);
        }
        if (template instanceof ValidatedStringMap<?> || template instanceof ValidatedIdentifierMap<?>
                || template instanceof ValidatedMap<?, ?>) {
            Entry<?, ?> key = FzzyBridge.keyHandler(template);
            Entry<?, ?> value = FzzyBridge.valueHandler(template);
            if (key != null && value != null) return mapEditor(key, value, binding, scope, keys);
        }
        return Editor.of(tomlEditor(template, binding, scope));
    }

    /* ======================================================= scalars ======================================================= */

    private static UIElement switchEditor(Binding binding) {
        Switch toggle = new Switch();
        toggle.setOn(Boolean.TRUE.equals(binding.get()), false);
        toggle.layout(l -> l.width(28).height(CONTROL_H).flexShrink(0));
        toggle.setOnSwitchChanged(on -> {
            if (!binding.set(on)) toggle.setOn(Boolean.TRUE.equals(binding.get()), false);
        });
        return toggle;
    }

    private static UIElement numberEditor(ValidatedNumber<?> template, Binding binding) {
        Object current = binding.get();
        Function<String, Number> parse = numberParser(current);
        TextField text = new TextField();
        text.setText(formatNumber(current), false);
        text.setTextValidator(s -> {
            Number n = parse.apply(s);
            return n != null && template.isValidEntry(n);
        });
        text.layout(l -> l.height(CONTROL_H).flexGrow(1).minWidth(36));

        Number[] range = FzzyBridge.numberRange(template);
        Number[] typeBounds = FzzyBridge.numberTypeBounds(template);
        boolean bounded = range != null && typeBounds != null
                && range[0].doubleValue() > typeBounds[0].doubleValue()
                && range[1].doubleValue() < typeBounds[1].doubleValue();
        if (FzzyBridge.numberWidget(template) != FzzyBridge.NumberWidget.SLIDER || !bounded) {
            text.setTextResponder(s -> binding.set(parse.apply(s)));
            return text;
        }

        Slider slider = new Slider.Horizontal();
        slider.setRange(range[0].floatValue(), range[1].floatValue());
        slider.setValue(current instanceof Number n ? n.floatValue() : range[0].floatValue(), false);
        slider.layout(l -> l.height(CONTROL_H).flexGrow(2));
        text.layout(l -> l.flexGrow(0).width(46));
        boolean integral = isIntegral(current);
        slider.setOnValueChanged(f -> {
            Number n = convert(current, integral ? Math.round(f) : f);
            if (binding.set(n)) text.setText(formatNumber(binding.get()), false);
        });
        text.setTextResponder(s -> {
            Number n = parse.apply(s);
            if (binding.set(n)) slider.setValue(n.floatValue(), false);
        });
        return row(slider, text);
    }

    private static UIElement enumEditor(Entry<?, ?> template, Binding binding) {
        Object current = binding.get();
        if (!(current instanceof Enum<?> e)) return tomlEditor(template, binding, "");
        List<Object> constants = Arrays.asList(e.getDeclaringClass().getEnumConstants());
        return selector(constants, binding, v -> FcText.INSTANCE.transLit(v, pretty(((Enum<?>) v).name())));
    }

    private static UIElement selector(List<Object> values, Binding binding, Function<Object, Component> label) {
        Selector<Object> selector = new Selector<>();
        selector.setCandidateUIProvider(UIElementProvider.text(v -> v == null ? Component.empty() : label.apply(v)));
        selector.setCandidates(values);
        selector.setSelected(binding.get(), false);
        selector.layout(l -> l.height(CONTROL_H).flexGrow(1));
        selector.setOnValueChanged(v -> {
            if (!binding.set(v)) selector.setSelected(binding.get(), false);
        });
        return selector;
    }

    /** A single-line field whose text parses to the value; red while it does not parse or validate. */
    private static UIElement textEditor(Entry<?, ?> template, Binding binding, Function<String, Object> parse,
                                        Function<Object, String> format) {
        TextField text = new TextField();
        text.setText(format.apply(binding.get()), false);
        text.setTextValidator(s -> {
            Object v = parse.apply(s);
            return v != null && template.isValidEntry(v);
        });
        text.setTextResponder(s -> binding.set(parse.apply(s)));
        text.layout(l -> l.height(CONTROL_H).flexGrow(1));
        return text;
    }

    private static UIElement colorEditor(Entry<?, ?> template, Binding binding) {
        boolean alpha = binding.get() instanceof ValidatedColor.ColorHolder h && h.getAlphaMode();
        UIElement swatch = new UIElement();
        swatch.layout(l -> l.width(CONTROL_H).height(CONTROL_H).flexShrink(0));
        Consumer<Object> paint = v -> {
            if (v instanceof ValidatedColor.ColorHolder h) swatch.style(s -> s.background(new ColorRectTexture(h.argb())));
        };
        paint.accept(binding.get());
        TextField hex = new TextField();
        hex.setText(binding.get() instanceof ValidatedColor.ColorHolder h ? h.toHexString() : "", false);
        hex.setTextValidator(s -> {
            ValidatedColor.ColorHolder v = parseColor(s, alpha);
            return v != null && template.isValidEntry(v);
        });
        hex.setTextResponder(s -> {
            ValidatedColor.ColorHolder v = parseColor(s, alpha);
            if (binding.set(v)) paint.accept(v);
        });
        hex.layout(l -> l.height(CONTROL_H).flexGrow(1));
        return row(swatch, hex);
    }

    @Nullable
    private static ValidatedColor.ColorHolder parseColor(String s, boolean alphaMode) {
        String hex = s.trim().replace("#", "").replace("0x", "");
        if (hex.length() != 6 && hex.length() != 8) return null;
        try {
            long v = Long.parseLong(hex, 16);
            int a = hex.length() == 8 ? (int) (v >> 24 & 0xFF) : 255;
            return new ValidatedColor.ColorHolder((int) (v >> 16 & 0xFF), (int) (v >> 8 & 0xFF), (int) (v & 0xFF), a, alphaMode);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** A button showing the bind; clicking it waits for the next key. Esc clears the bind. */
    private static UIElement keybindEditor(Binding binding, KeyCapture keys) {
        Button button = new Button();
        Runnable show = () -> button.setText(binding.get() instanceof FzzyKeybind k ? k.keybind() : Component.literal("?"));
        show.run();
        button.layout(l -> l.height(CONTROL_H).flexGrow(1));
        button.setOnClick(e -> {
            button.setText(Component.translatable("gui.roll_mod.config.key.press"));
            keys.capture((key, ctrl, shift, alt) -> {
                FzzyKeybind bind = key < 0 ? FzzyKeybindUnbound.INSTANCE : new FzzyKeybindSimple(key, ContextInput.KEYBOARD,
                        ctrl ? TriState.TRUE : TriState.FALSE, shift ? TriState.TRUE : TriState.FALSE,
                        alt ? TriState.TRUE : TriState.FALSE);
                binding.set(bind);
                show.run();
            });
        });
        return button;
    }

    /** Anything: the value as TOML, one line or several. */
    private static UIElement tomlEditor(Entry<?, ?> template, Binding binding, String scope) {
        Entry<?, ?> copy = copyWith(template, binding.get());
        String text = copy == null ? null : FzzyBridge.toText(copy);
        if (text == null) {
            TextField none = new TextField();
            none.setText(String.valueOf(binding.get()), false);
            none.setActive(false);
            none.layout(l -> l.height(CONTROL_H).flexGrow(1));
            return none;
        }
        if (!text.contains("\n")) {
            TextField field = new TextField();
            field.setText(text, false);
            field.setTextValidator(s -> FzzyBridge.fromText(template, s, scope) != null);
            field.setTextResponder(s -> binding.set(FzzyBridge.fromText(template, s, scope)));
            field.layout(l -> l.height(CONTROL_H).flexGrow(1));
            return field;
        }
        TextArea area = new TextArea();
        area.setValue(text.split("\n"), false);
        area.setLinesResponder(lines -> {
            Object v = FzzyBridge.fromText(template, String.join("\n", lines), scope);
            if (v != null) binding.set(v);
        });
        area.layout(l -> l.height(CONTROL_H * 4).flexGrow(1));
        return area;
    }

    /* ====================================================== collections ====================================================== */

    /** A list or set: "N entries" in the row; the elements, each with its own editor, in a panel below. */
    private static Editor listEditor(Entry<?, ?> element, Binding binding, boolean set, String scope, KeyCapture keys) {
        List<Object> items = new ArrayList<>(binding.get() instanceof Collection<?> c ? c : List.of());
        CollectionPanel panel = new CollectionPanel(binding) {
            @Override
            int size() {
                return items.size();
            }

            @Override
            Object commitValue() {
                return set ? new LinkedHashSet<>(items) : new ArrayList<>(items);
            }

            @Override
            boolean matches(int i, String filter) {
                return String.valueOf(items.get(i)).toLowerCase(Locale.ROOT).contains(filter);
            }

            @Override
            UIElement row(int i) {
                Binding b = new Binding() {
                    @Override
                    public Object get() {
                        return items.get(i);
                    }

                    @Override
                    public boolean set(Object value) {
                        if (value == null || !element.isValidEntry(value)) return false;
                        items.set(i, value);
                        return commit();
                    }
                };
                return elementRow(create(element, b, scope, keys), this, items, i);
            }

            @Override
            void add() {
                items.add(freshValue(element));
            }
        };
        return panel.editor();
    }

    /** A map: key and value editors per entry. Duplicate keys are refused until fixed. */
    private static Editor mapEditor(Entry<?, ?> keyTemplate, Entry<?, ?> valueTemplate, Binding binding, String scope,
                                    KeyCapture keys) {
        List<Object[]> pairs = new ArrayList<>();
        if (binding.get() instanceof Map<?, ?> m) m.forEach((k, v) -> pairs.add(new Object[]{k, v}));
        CollectionPanel panel = new CollectionPanel(binding) {
            @Override
            int size() {
                return pairs.size();
            }

            @Nullable
            @Override
            Object commitValue() {
                Map<Object, Object> out = new LinkedHashMap<>();
                for (Object[] p : pairs) {
                    if (out.containsKey(p[0])) return null;
                    out.put(p[0], p[1]);
                }
                return out;
            }

            @Override
            boolean matches(int i, String filter) {
                return (pairs.get(i)[0] + "=" + pairs.get(i)[1]).toLowerCase(Locale.ROOT).contains(filter);
            }

            @Override
            UIElement row(int i) {
                Binding k = slot(pairs, i, 0, keyTemplate, this);
                Binding v = slot(pairs, i, 1, valueTemplate, this);
                UIElement keyEditor = create(keyTemplate, k, scope, keys).inline();
                UIElement valueEditor = create(valueTemplate, v, scope, keys).inline();
                keyEditor.layout(l -> l.flexGrow(1).flexBasis(0));
                valueEditor.layout(l -> l.flexGrow(1).flexBasis(0));
                return elementRow(Editor.of(ConfigEditors.row(keyEditor, valueEditor)), this, pairs, i);
            }

            @Override
            void add() {
                pairs.add(new Object[]{freshValue(keyTemplate), freshValue(valueTemplate)});
            }
        };
        return panel.editor();
    }

    private static Binding slot(List<Object[]> pairs, int i, int side, Entry<?, ?> template, CollectionPanel panel) {
        return new Binding() {
            @Override
            public Object get() {
                return pairs.get(i)[side];
            }

            @Override
            public boolean set(Object value) {
                if (value == null || !template.isValidEntry(value)) return false;
                pairs.get(i)[side] = value;
                return panel.commit();
            }
        };
    }

    /** The shared shape of the list and map editors: header button, filter, rows, add. */
    private abstract static class CollectionPanel {
        final Binding binding;
        final Button header = new Button();
        final UIElement panel = new UIElement();
        final UIElement rows = new UIElement();
        String filter = "";

        CollectionPanel(Binding binding) {
            this.binding = binding;
        }

        abstract int size();

        /** The collection to hand to the field; null while it is not valid as a whole (duplicate key). */
        @Nullable
        abstract Object commitValue();

        abstract boolean matches(int i, String filter);

        abstract UIElement row(int i);

        abstract void add();

        Editor editor() {
            header.layout(l -> l.height(CONTROL_H).flexGrow(1));
            header.setOnClick(e -> {
                panel.setDisplay(!panel.isDisplayed());
                updateHeader();
            });
            panel.layout(l -> l.flexDirection(FlexDirection.COLUMN).widthPercent(100).gapRow(2).paddingLeft(10).paddingTop(2));
            rows.layout(l -> l.flexDirection(FlexDirection.COLUMN).widthPercent(100).gapRow(2));
            if (size() > FILTER_FROM) {
                TextField search = new TextField();
                search.setText("", false);
                search.layout(l -> l.height(CONTROL_H).widthPercent(100));
                search.setTextResponder(s -> {
                    filter = s.trim().toLowerCase(Locale.ROOT);
                    rebuild();
                });
                panel.addChild(search);
            }
            panel.addChild(rows);
            Button add = new Button();
            add.setText(Component.translatable("gui.roll_mod.config.add"));
            add.addPreIcon(Icons.ADD);
            add.layout(l -> l.height(CONTROL_H).width(70));
            add.setOnClick(e -> {
                add();
                commit();
                rebuild();
            });
            panel.addChild(add);
            panel.setDisplay(false);
            rebuild();
            updateHeader();
            return new Editor(header, panel);
        }

        void rebuild() {
            rows.clearAllChildren();
            for (int i = 0; i < size(); i++) {
                if (filter.isEmpty() || matches(i, filter)) rows.addChild(row(i));
            }
        }

        /** Hands the edited collection to the field. */
        boolean commit() {
            Object value = commitValue();
            boolean ok = value != null && binding.set(value);
            updateHeader();
            return ok;
        }

        void updateHeader() {
            header.setText(Component.translatable("gui.roll_mod.config.items", size())
                    .append(panel.isDisplayed() ? " ▴" : " ▾"));
        }
    }

    /** One element: its editor, then up / down / remove. */
    private static UIElement elementRow(Editor editor, CollectionPanel panel, List<?> items, int i) {
        UIElement inline = editor.inline();
        inline.layout(l -> l.flexGrow(1));
        UIElement row = row(inline,
                iconButton(Icons.UP, i > 0, () -> {
                    swap(items, i, i - 1);
                    panel.commit();
                    panel.rebuild();
                }),
                iconButton(Icons.DOWN, i < items.size() - 1, () -> {
                    swap(items, i, i + 1);
                    panel.commit();
                    panel.rebuild();
                }),
                iconButton(Icons.REMOVE, true, () -> {
                    items.remove(i);
                    panel.commit();
                    panel.rebuild();
                }));
        if (editor.panel() == null) return row;
        UIElement column = new UIElement();
        column.layout(l -> l.flexDirection(FlexDirection.COLUMN).widthPercent(100));
        column.addChildren(row, editor.panel());
        return column;
    }

    @SuppressWarnings("unchecked")
    private static <T> void swap(List<T> list, int a, int b) {
        T t = list.get(a);
        list.set(a, list.get(b));
        list.set(b, t);
    }

    /* ======================================================== helpers ======================================================== */

    static UIElement iconButton(IGuiTexture icon, boolean active, Runnable action) {
        Button button = new Button();
        button.noText().addPreIcon(icon);
        button.layout(l -> l.width(CONTROL_H).height(CONTROL_H).flexShrink(0).paddingAll(1));
        button.setActive(active);
        button.setOnClick(e -> action.run());
        return button;
    }

    static UIElement row(UIElement... children) {
        UIElement row = new UIElement();
        row.layout(l -> l.flexDirection(FlexDirection.ROW).alignItems(AlignItems.CENTER).gapColumn(3).flexGrow(1));
        row.addChildren(children);
        return row;
    }

    /** A fresh value of the template's kind, for a newly added element. */
    private static Object freshValue(Entry<?, ?> template) {
        Object v = template.instanceEntry().get();
        return v != null ? v : template.get();
    }

    /** A detached copy of {@code template} holding {@code value}, for serialisation. */
    @Nullable
    @SuppressWarnings({"rawtypes"})
    private static Entry<?, ?> copyWith(Entry<?, ?> template, Object value) {
        try {
            Entry copy = (Entry) template.instanceEntry();
            copy.trySetQuiet(value);
            return copy;
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static boolean isIntegral(Object value) {
        return value instanceof Integer || value instanceof Long || value instanceof Short || value instanceof Byte;
    }

    private static Function<String, Number> numberParser(Object sample) {
        return s -> {
            try {
                String t = s.trim();
                if (sample instanceof Integer) return Integer.parseInt(t);
                if (sample instanceof Long) return Long.parseLong(t);
                if (sample instanceof Short) return Short.parseShort(t);
                if (sample instanceof Byte) return Byte.parseByte(t);
                if (sample instanceof Float) return Float.parseFloat(t);
                return Double.parseDouble(t);
            } catch (NumberFormatException e) {
                return null;
            }
        };
    }

    private static Number convert(Object sample, double v) {
        if (sample instanceof Integer) return (int) v;
        if (sample instanceof Long) return (long) v;
        if (sample instanceof Short) return (short) v;
        if (sample instanceof Byte) return (byte) v;
        if (sample instanceof Float) return (float) v;
        return v;
    }

    private static String formatNumber(Object value) {
        return value == null ? "" : value.toString();
    }

    /** {@code SOME_ENUM_NAME} -> {@code Some enum name}. */
    static String pretty(String name) {
        String s = name.replace('_', ' ').toLowerCase(Locale.ROOT);
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    /** How the keybind editor asks the screen for the next key press. */
    interface KeyCapture {
        /** {@code key} is a GLFW key code, or -1 for "cleared" (Esc). */
        interface Listener {
            void pressed(int key, boolean ctrl, boolean shift, boolean alt);
        }

        void capture(Listener listener);
    }
}
