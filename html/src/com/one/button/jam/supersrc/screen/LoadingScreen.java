package screen;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.assets.AssetLoaderParameters;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.assets.loaders.SynchronousAssetLoader;
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.one.button.jam.MyGdxGame;
import utils.DebugConfig;

public class LoadingScreen implements Screen {
    final MyGdxGame game;
    OrthographicCamera camera;
    private Texture textureLogo;
    private Image imageLogo;
    private Stage stage;

    private static final class WebFontLoader
            extends SynchronousAssetLoader<BitmapFont, AssetLoaderParameters<BitmapFont>> {
        private static final float FONT1_GENERATED_SIZE = 158f;
        private static final float TITLE_GENERATED_SIZE = 341f;
        private static final float RESTART_GENERATED_SIZE = 410f;

        WebFontLoader(FileHandleResolver resolver) {
            super(resolver);
        }

        @Override
        public BitmapFont load(AssetManager assetManager, String fileName,
                com.badlogic.gdx.files.FileHandle file, AssetLoaderParameters<BitmapFont> parameter) {
            String fontFile;
            float desiredSize;
            float generatedSize;

            if ("fontTitre.ttf".equals(fileName)) {
                fontFile = "Fonts/web_title_hd.fnt";
                desiredSize = Gdx.graphics.getWidth() / 12f;
                generatedSize = TITLE_GENERATED_SIZE;
            } else if ("fontRestart.ttf".equals(fileName)) {
                fontFile = "Fonts/web_restart_hd.fnt";
                desiredSize = Gdx.graphics.getWidth() / 10f;
                generatedSize = RESTART_GENERATED_SIZE;
            } else {
                fontFile = "Fonts/web_font1_hd.fnt";
                desiredSize = Gdx.graphics.getWidth() / 26f;
                generatedSize = FONT1_GENERATED_SIZE;
            }

            BitmapFont font = new BitmapFont(Gdx.files.internal(fontFile), false);
            font.getData().setScale(desiredSize / generatedSize);
            font.setUseIntegerPositions(false);
            for (TextureRegion region : font.getRegions()) {
                region.getTexture().setFilter(TextureFilter.Linear, TextureFilter.Linear);
            }
            return font;
        }

        @Override
        public Array<AssetDescriptor> getDependencies(String fileName, com.badlogic.gdx.files.FileHandle file,
                AssetLoaderParameters<BitmapFont> parameter) {
            return null;
        }
    }

    public LoadingScreen(final MyGdxGame gam) {
        game = gam;

        camera = new OrthographicCamera();
        camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        textureLogo = new Texture(Gdx.files.internal("Images/Logo.png"), false);
        textureLogo.setFilter(TextureFilter.Linear, TextureFilter.Linear);
        imageLogo = new Image(textureLogo);
        stage = new Stage(new ScreenViewport());
        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        game.assets.load("Images/Images.pack", TextureAtlas.class);
        game.assets.load("Sounds/Chock.wav", Sound.class);
        game.assets.load("Sounds/Fall.wav", Sound.class);
        game.assets.load("Sounds/FinishLevel.wav", Sound.class);
        game.assets.load("Sounds/Spring.wav", Sound.class);
        game.assets.load("Sounds/Water.wav", Sound.class);

        FileHandleResolver resolver = new InternalFileHandleResolver();
        game.assets.setLoader(BitmapFont.class, ".ttf", new WebFontLoader(resolver));
        game.assets.load("font1.ttf", BitmapFont.class);
        game.assets.load("fontTitre.ttf", BitmapFont.class);
        game.assets.load("fontRestart.ttf", BitmapFont.class);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(1, 1, 1, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();
        game.batch.setProjectionMatrix(camera.combined);

        stage.act();
        stage.draw();

        if (game.assets.update()) {
            dispose();
            ((Game) Gdx.app.getApplicationListener()).setScreen(new MainMenuScreen(game));
        }
    }

    @Override
    public void show() {
        stage.addActor(imageLogo);
        imageLogo.addAction(Actions.sequence(Actions.alpha(0), Actions.fadeIn(0.75f), Actions.delay(1.5f)));
    }

    @Override
    public void resize(int width, int height) {
        width = Math.max(1, width);
        height = Math.max(1, height);
        camera.setToOrtho(false, width, height);
        stage.getViewport().update(width, height, true);
        float maxWidth = width * 0.82f;
        float maxHeight = height * 0.50f;
        float scale = Math.min(maxWidth / textureLogo.getWidth(), maxHeight / textureLogo.getHeight());
        imageLogo.setWidth(textureLogo.getWidth() * scale);
        imageLogo.setHeight(textureLogo.getHeight() * scale);
        imageLogo.setPosition(width / 2f - imageLogo.getWidth() / 2f,
                height / 2f - imageLogo.getHeight() / 2f);
        DebugConfig.log("loading logo layout screen=" + width + "x" + height
                + " bounds=" + imageLogo.getX() + "," + imageLogo.getY()
                + "," + imageLogo.getWidth() + "," + imageLogo.getHeight());
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
    }

    @Override
    public void dispose() {
        textureLogo.dispose();
        stage.dispose();
    }
}
