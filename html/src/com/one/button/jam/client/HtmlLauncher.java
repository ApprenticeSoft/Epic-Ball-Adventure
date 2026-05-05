package com.one.button.jam.client;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.backends.gwt.GwtApplication;
import com.badlogic.gdx.backends.gwt.GwtApplicationConfiguration;
import com.one.button.jam.MyGdxGame;

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
        return new MyGdxGame();
    }
}
