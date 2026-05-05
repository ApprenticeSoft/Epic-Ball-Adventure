package com.one.button.jam.client;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.gwt.GwtApplication;
import com.badlogic.gdx.backends.gwt.GwtApplicationConfiguration;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.user.client.Window;
import com.one.button.jam.MyGdxGame;

public class HtmlLauncher extends GwtApplication {
    private static final float LANDSCAPE_ASPECT = 1024f / 720f;

    @Override
    public GwtApplicationConfiguration getConfig() {
        int[] size = computeTargetSize();
        GwtApplicationConfiguration config = new GwtApplicationConfiguration(size[0], size[1]);
        config.padHorizontal = 0;
        config.padVertical = 0;
        config.useDebugGL = false;
        return config;
    }

    @Override
    public void onModuleLoad() {
        super.onModuleLoad();
        Window.addResizeHandler(new ResizeHandler() {
            @Override
            public void onResize(ResizeEvent event) {
                applyResponsiveSize();
            }
        });
        applyResponsiveSize();
    }

    private static int[] computeTargetSize() {
        int browserWidth = Math.max(1, Window.getClientWidth());
        int browserHeight = Math.max(1, Window.getClientHeight());
        int targetWidth = browserWidth;
        int targetHeight = Math.round(targetWidth / LANDSCAPE_ASPECT);
        if (targetHeight > browserHeight) {
            targetHeight = browserHeight;
            targetWidth = Math.round(targetHeight * LANDSCAPE_ASPECT);
        }
        return new int[] { Math.max(1, targetWidth), Math.max(1, targetHeight) };
    }

    private void applyResponsiveSize() {
        int[] size = computeTargetSize();
        if (getRootPanel() != null) {
            getRootPanel().setWidth(size[0] + "px");
            getRootPanel().setHeight(size[1] + "px");
        }
        if (Gdx.graphics != null) {
            Gdx.graphics.setWindowedMode(size[0], size[1]);
        }
    }

    @Override
    public ApplicationListener createApplicationListener() {
        return new MyGdxGame();
    }
}
