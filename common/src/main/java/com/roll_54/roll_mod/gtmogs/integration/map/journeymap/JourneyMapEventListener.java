package com.roll_54.roll_mod.gtmogs.integration.map.journeymap;
import com.roll_54.roll_mod.gtmogs.GTMOGS;
import com.roll_54.roll_mod.gtmogs.config.ConfigHolder;
import com.roll_54.roll_mod.gtmogs.integration.map.ButtonState;
import journeymap.api.v2.client.event.FullscreenDisplayEvent;
import journeymap.api.v2.client.fullscreen.IThemeButton;
import journeymap.api.v2.common.event.FullscreenEventRegistry;
import journeymap.client.io.ThemeLoader;

import java.util.ArrayList;
import java.util.Arrays;

public class JourneyMapEventListener {

    public static void init() {
        FullscreenEventRegistry.ADDON_BUTTON_DISPLAY_EVENT.subscribe(GTMOGS.MOD_ID,
                JourneyMapEventListener::onFullscreenAddonButton);
        FullscreenEventRegistry.CUSTOM_TOOLBAR_UPDATE_EVENT.subscribe(GTMOGS.MOD_ID,
                JourneyMapEventListener::onFullscreenToolbarEvent);
    }

    protected static void onFullscreenAddonButton(FullscreenDisplayEvent.AddonButtonDisplayEvent event) {
        if (!ConfigHolder.INSTANCE.compat.minimap.toggle.journeyMapIntegration) {
            return;
        }
        if (ConfigHolder.INSTANCE.compat.minimap.direction !=
                ConfigHolder.CompatibilityConfigs.MinimapCompatConfig.Direction.VERTICAL) {
            return;
        }
        var display = event.getThemeButtonDisplay();
        var buttons = new ArrayList<IThemeButton>(ButtonState.getAllButtons().size());
        for (var state : ButtonState.getAllButtons()) {
            buttons.add(display.addThemeToggleButton("gtmogs.button." + state.name,
                    GTMOGS.id("textures/gtmogs/gui/widget/button_" + state.name + ".png"),
                    state.enabled,
                    b -> {
                        ButtonState.toggleButton(state);
                        buttons.stream().filter(btn -> btn.getToggled() || btn == b).forEach(IThemeButton::toggle);
                    }));
        }
    }

    @SuppressWarnings("deprecation")
    protected static void onFullscreenToolbarEvent(FullscreenDisplayEvent.CustomToolbarEvent event) {
        if (!ConfigHolder.INSTANCE.compat.minimap.toggle.journeyMapIntegration) {
            return;
        }
        if (ConfigHolder.INSTANCE.compat.minimap.direction ==
                ConfigHolder.CompatibilityConfigs.MinimapCompatConfig.Direction.VERTICAL) {
            return;
        }
        var builder = event.getCustomToolBarBuilder();
        var allButtons = ButtonState.getAllButtons();
        var buttons = new IThemeButton[allButtons.size()];
        for (int i = 0; i < allButtons.size(); i++) {
            var state = allButtons.get(i);
            buttons[i] = builder.getThemeToggleButton("gtmogs.button." + state.name,
                    GTMOGS.id("textures/gtmogs/gui/widget/button_" + state.name + ".png"),
                    b -> {
                        ButtonState.toggleButton(state);
                        Arrays.stream(buttons).filter(btn -> btn.getToggled() || btn == b)
                                .forEach(IThemeButton::toggle);
                    });
            buttons[i].setToggled(state.enabled);
        }
        var toolbar = builder.getNewToolbar(buttons);

        var theme = ThemeLoader.getCurrentTheme();
        var screen = event.getFullscreen().getScreen();
        var toolbarTheme = theme.container.toolbar;
        var toolbarSpec = toolbarTheme.horizontal;
        var margin = toolbarSpec.margin;
        var padding = toolbarSpec.padding;
        var height = toolbarSpec.inner.height;
        toolbar.setLayoutHorizontal(margin, screen.height - (height + margin), padding, true);
    }
}