package com.one.button.jam.desktop;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.utils.ScreenUtils;
import com.one.button.jam.MyGdxGame;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import screen.GameScreen;
import utils.DebugConfig;
import utils.Variables;

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

        new Lwjgl3Application(new DesktopGame(), config);
    }

    private static void configureDebug() {
        DebugConfig.transitionLogs = Boolean.getBoolean("ball.debug");
        DebugConfig.autoAdvanceLevels = Boolean.getBoolean("ball.autoAdvance");
        DebugConfig.showRestartOverlay = Boolean.getBoolean("ball.debugRestartOverlay");
        DebugConfig.springLightProbe = Boolean.getBoolean("ball.debugSpringLight");
        DebugConfig.startLevel = Integer.getInteger("ball.startLevel", 1);
        DebugConfig.resetProgress = Boolean.getBoolean("ball.resetProgress");
        DebugConfig.desktopBenchmark = Boolean.getBoolean("ball.benchmark");
        DebugConfig.fixedStep = Boolean.getBoolean("ball.fixedStep");
        DebugConfig.autoAdvanceDelay = Float.parseFloat(System.getProperty("ball.autoAdvanceDelay", "0.35"));
    }

    private static class DesktopGame extends MyGdxGame {
        private final String screenshotPath = System.getProperty("ball.screenshot");
        private final int screenshotFrame = Integer.getInteger("ball.screenshot.frame", 240);
        private final boolean benchmark = Boolean.getBoolean("ball.benchmark");
        private final float benchmarkSeconds = parseFloatProperty("ball.benchmarkSeconds", 8f);
        private final String benchmarkOutput = System.getProperty("ball.benchmarkOutput",
                defaultReportPath("desktop-benchmark", "benchmark.json"));
        private final long startNanos = System.nanoTime();
        private final long startUsedMemory = usedMemory();
        private final ArrayList<Float> frameTimesMs = new ArrayList<Float>();
        private int renderedFrames;
        private long firstFrameNanos;
        private long firstPlayableFrameNanos;
        private long firstPlayableUsedMemory;
        private String lastSceneName = "startup";
        private boolean benchmarkWritten;
        private boolean screenshotWritten;

        @Override
        public void render() {
            long frameStart = System.nanoTime();
            super.render();
            long frameEnd = System.nanoTime();
            renderedFrames++;
            captureScreenshotIfNeeded();
            updateBenchmark(frameStart, frameEnd);
        }

        private void captureScreenshotIfNeeded(){
            if(screenshotPath == null || screenshotWritten || renderedFrames < screenshotFrame)
                return;
            Pixmap pixmap = ScreenUtils.getFrameBufferPixmap(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            Pixmap flipped = new Pixmap(pixmap.getWidth(), pixmap.getHeight(), pixmap.getFormat());
            for(int y = 0; y < pixmap.getHeight(); y++)
                flipped.drawPixmap(pixmap, 0, y, 0, pixmap.getHeight() - y - 1, pixmap.getWidth(), 1);
            File output = new File(screenshotPath);
            File parent = output.getParentFile();
            if(parent != null)
                parent.mkdirs();
            PixmapIO.writePNG(Gdx.files.absolute(output.getAbsolutePath()), flipped);
            flipped.dispose();
            pixmap.dispose();
            screenshotWritten = true;
            if(!benchmark)
                Gdx.app.exit();
        }

        private void updateBenchmark(long frameStart, long frameEnd){
            if(!benchmark || benchmarkWritten)
                return;
            if(firstFrameNanos == 0)
                firstFrameNanos = frameEnd;
            if(getScreen() != null)
                lastSceneName = getScreen().getClass().getSimpleName();
            if(firstPlayableFrameNanos == 0 && getScreen() instanceof GameScreen){
                firstPlayableFrameNanos = frameEnd;
                firstPlayableUsedMemory = usedMemory();
            }
            frameTimesMs.add(Gdx.graphics.getDeltaTime() * 1000f);
            long elapsedNanos = frameEnd - startNanos;
            if(elapsedNanos >= (long)(Math.max(0.25f, benchmarkSeconds) * 1_000_000_000L)){
                writeBenchmark(frameEnd);
                Gdx.app.exit();
            }
        }

        private void writeBenchmark(long endNanos){
            benchmarkWritten = true;
            File output = new File(benchmarkOutput);
            File parent = output.getParentFile();
            if(parent != null)
                parent.mkdirs();
            String json = "{\n"
                    + "  \"branch\": \"" + escape(System.getProperty("ball.gitBranch", "")) + "\",\n"
                    + "  \"commit\": \"" + escape(System.getProperty("ball.gitCommit", "")) + "\",\n"
                    + "  \"scene\": \"" + escape(lastSceneName) + "\",\n"
                    + "  \"level\": " + Variables.niveauSelectione + ",\n"
                    + "  \"frames\": " + frameTimesMs.size() + ",\n"
                    + "  \"durationMs\": " + number((endNanos - startNanos) / 1_000_000f) + ",\n"
                    + "  \"startupMs\": " + number(msSinceStart(firstFrameNanos)) + ",\n"
                    + "  \"firstPlayableFrameMs\": " + number(msSinceStart(firstPlayableFrameNanos)) + ",\n"
                    + "  \"averageFrameMs\": " + number(average(frameTimesMs)) + ",\n"
                    + "  \"p95FrameMs\": " + number(percentile(frameTimesMs, 0.95f)) + ",\n"
                    + "  \"worstFrameMs\": " + number(max(frameTimesMs)) + ",\n"
                    + "  \"memoryBeforeBytes\": " + startUsedMemory + ",\n"
                    + "  \"memoryAtFirstPlayableBytes\": " + firstPlayableUsedMemory + ",\n"
                    + "  \"memoryAfterBytes\": " + usedMemory() + ",\n"
                    + "  \"screenshotPath\": \"" + escape(screenshotWritten ? new File(screenshotPath).getAbsolutePath() : "") + "\"\n"
                    + "}\n";
            try{
                java.nio.file.Files.write(output.toPath(), json.getBytes(StandardCharsets.UTF_8));
                System.out.println("Desktop benchmark: " + output.getAbsolutePath());
            }
            catch(IOException exception){
                throw new RuntimeException("Unable to write desktop benchmark: " + output.getAbsolutePath(), exception);
            }
        }

        private float msSinceStart(long nanos){
            if(nanos == 0)
                return -1f;
            return (nanos - startNanos) / 1_000_000f;
        }
    }

    private static float parseFloatProperty(String name, float fallback){
        try{
            return Float.parseFloat(System.getProperty(name, String.valueOf(fallback)));
        }
        catch(NumberFormatException exception){
            return fallback;
        }
    }

    private static long usedMemory(){
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }

    private static float average(ArrayList<Float> values){
        if(values.isEmpty())
            return 0f;
        float total = 0f;
        for(Float value : values)
            total += value;
        return total / values.size();
    }

    private static float percentile(ArrayList<Float> values, float percentile){
        if(values.isEmpty())
            return 0f;
        ArrayList<Float> sorted = new ArrayList<Float>(values);
        Collections.sort(sorted);
        int index = Math.min(sorted.size() - 1, Math.max(0, Math.round((sorted.size() - 1) * percentile)));
        return sorted.get(index);
    }

    private static float max(ArrayList<Float> values){
        float max = 0f;
        for(Float value : values)
            max = Math.max(max, value);
        return max;
    }

    private static String number(float value){
        if(Float.isNaN(value) || Float.isInfinite(value))
            return "0";
        return String.format(java.util.Locale.ROOT, "%.3f", value);
    }

    private static String escape(String value){
        if(value == null)
            return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String defaultReportPath(String directory, String fileName){
        Path workingDir = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
        Path rootDir = workingDir.endsWith(Paths.get("android", "assets")) ? workingDir.getParent().getParent() : workingDir;
        return rootDir.resolve("build").resolve("reports").resolve(directory).resolve(fileName).toString();
    }
}
