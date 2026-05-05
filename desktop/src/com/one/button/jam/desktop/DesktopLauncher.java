package com.one.button.jam.desktop;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.utils.ScreenUtils;
import com.one.button.jam.MyGdxGame;
import utils.DebugConfig;

public class DesktopLauncher {
    public static void main(String[] arg) {
        configureDebug();
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Epic Ball Adventure");
        config.setWindowedMode(1024, 720);
        config.useVsync(true);
        config.setForegroundFPS(60);
        config.setBackBufferConfig(8, 8, 8, 8, 16, 0, 4);
        config.setWindowIcon(Files.FileType.Internal, "Images/Logo.png");

        new Lwjgl3Application(new MyGdxGame() {
            private final String screenshotPath = System.getProperty("ball.screenshot");
            private final int screenshotFrame = Integer.getInteger("ball.screenshot.frame", 240);
            private int renderedFrames;

            @Override
            public void render() {
                super.render();
                if (screenshotPath != null && ++renderedFrames == screenshotFrame) {
                    Pixmap pixmap = ScreenUtils.getFrameBufferPixmap(0, 0, Gdx.graphics.getWidth(),
                            Gdx.graphics.getHeight());
                    Pixmap flipped = new Pixmap(pixmap.getWidth(), pixmap.getHeight(), pixmap.getFormat());
                    for (int y = 0; y < pixmap.getHeight(); y++) {
                        flipped.drawPixmap(pixmap, 0, y, 0, pixmap.getHeight() - y - 1, pixmap.getWidth(), 1);
                    }
                    PixmapIO.writePNG(Gdx.files.absolute(screenshotPath), flipped);
                    flipped.dispose();
                    pixmap.dispose();
                    Gdx.app.exit();
                }
            }
        }, config);
    }

    private static void configureDebug() {
        DebugConfig.transitionLogs = Boolean.getBoolean("ball.debug");
        DebugConfig.autoAdvanceLevels = Boolean.getBoolean("ball.autoAdvance");
        DebugConfig.startLevel = Integer.getInteger("ball.startLevel", 1);
        DebugConfig.autoAdvanceDelay = Float.parseFloat(System.getProperty("ball.autoAdvanceDelay", "0.35"));
    }
}
