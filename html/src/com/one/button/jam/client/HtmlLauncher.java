package com.one.button.jam.client;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.backends.gwt.GwtApplication;
import com.badlogic.gdx.backends.gwt.GwtApplicationConfiguration;
import com.google.gwt.user.client.Window;
import com.one.button.jam.MyGdxGame;
import utils.DebugConfig;
import utils.PlatformInfo;

public class HtmlLauncher extends GwtApplication {
    @Override
    public GwtApplicationConfiguration getConfig() {
        GwtApplicationConfiguration config = new GwtApplicationConfiguration(true);
        config.padHorizontal = 0;
        config.padVertical = 0;
        config.useDebugGL = false;
        return config;
    }

    @Override
    public ApplicationListener createApplicationListener() {
        configureDebug();
        return new MyGdxGame();
    }

    private void configureDebug() {
        DebugConfig.transitionLogs = hasFlag("ballDebug");
        DebugConfig.autoAdvanceLevels = hasFlag("ballAutoAdvance");
        DebugConfig.showRestartOverlay = hasFlag("ballDebugRestartOverlay");
        DebugConfig.startLevel = getIntParameter("ballStartLevel", 1);
        DebugConfig.autoAdvanceDelay = getFloatParameter("ballAutoAdvanceDelay", 0.35f);
        PlatformInfo.mobileBrowser = isMobileBrowser();
    }

    private boolean hasFlag(String name) {
        String value = getQueryParameter(name);
        return "1".equals(value) || "true".equalsIgnoreCase(value);
    }

    private int getIntParameter(String name, int defaultValue) {
        try {
            String value = getQueryParameter(name);
            return value == null ? defaultValue : Integer.parseInt(value);
        }
        catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    private float getFloatParameter(String name, float defaultValue) {
        try {
            String value = getQueryParameter(name);
            return value == null ? defaultValue : Float.parseFloat(value);
        }
        catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    private String getQueryParameter(String name) {
        String query = Window.Location.getQueryString();
        if(query == null || query.length() == 0)
            return null;
        if(query.startsWith("?"))
            query = query.substring(1);

        String[] parameters = query.split("&");
        for(String parameter : parameters){
            int separator = parameter.indexOf('=');
            String parameterName = separator >= 0 ? parameter.substring(0, separator) : parameter;
            if(name.equals(parameterName))
                return separator >= 0 ? parameter.substring(separator + 1) : "true";
        }
        return null;
    }

    private static native boolean isMobileBrowser() /*-{
        var userAgent = ($wnd.navigator && $wnd.navigator.userAgent) || "";
        return /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(userAgent);
    }-*/;
}
