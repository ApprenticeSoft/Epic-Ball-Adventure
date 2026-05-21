package screen;

import utils.Variables;
import utils.DebugConfig;
import utils.PlatformInfo;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.one.button.jam.MyGdxGame;

public class MainMenuScreen extends InputAdapter implements Screen{
	private static final float MOBILE_START_TEXT_MAX_SCALE = 0.62f;
	private static final int WEBGL_E_KEYCODE = 33;

	final MyGdxGame game;
	private OrthographicCamera camera;
	private Stage stage;
	private Skin skin;
	private TextureAtlas textureAtlas;
	private LabelStyle labelStyleTitre, labelStyleOmbre, labelStyleStart;
	private Label labelTitre, labelTitreOmbre, labelStart;
	private Image transitionImage;
	private float startAlpha = 0;
	private boolean mobileStartText;
	private boolean startRequested;

	public MainMenuScreen(final MyGdxGame gam){
		game = gam;

		camera = new OrthographicCamera();
		camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

		stage = new Stage(new ScreenViewport());
		skin = new Skin();

		textureAtlas = game.assets.get("Images/Images.pack", TextureAtlas.class);
		skin.addRegions(textureAtlas);

		labelStyleTitre = new LabelStyle();
		labelStyleTitre.fontColor = new Color(81/256f, 166/256f, 220/256f,1);
		labelStyleTitre.font = game.assets.get("fontTitre.ttf", BitmapFont.class);

		labelStyleOmbre = new LabelStyle();
		labelStyleOmbre.fontColor = new Color(247/256f, 236/256f, 47/256f,1);
		labelStyleOmbre.font = game.assets.get("fontTitre.ttf", BitmapFont.class);

		labelStyleStart = new LabelStyle();
		labelStyleStart.fontColor = new Color(81/256f, 166/256f, 220/256f,1);
		labelStyleStart.font = game.assets.get("font1.ttf", BitmapFont.class);

		labelTitre = new Label(Variables.gameTitle, labelStyleTitre);

		labelTitreOmbre = new Label(Variables.gameTitle, labelStyleOmbre);

		mobileStartText = Gdx.app.getType() == ApplicationType.Android || PlatformInfo.mobileBrowser;
		if(mobileStartText)
			labelStart = new Label(Variables.niveauSelectione > 1 ? "Touch to Continue" : "Touch to Start", labelStyleStart);
		else
			labelStart = new Label(Variables.niveauSelectione > 1 ? "Press F to continue" : "Press F to start", labelStyleStart);

		transitionImage = new Image(skin.getDrawable("WhiteSquare"));
		transitionImage.setColor(new Color(237/256f, 27/256f, 81/256f,1));
		transitionImage.addAction(Actions.alpha(0));

		stage.addActor(labelTitreOmbre);
		stage.addActor(labelTitre);
		stage.addActor(transitionImage);
		resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		if(DebugConfig.startEditor && editorAvailable())
			Gdx.app.postRunnable(new Runnable() {
				@Override
				public void run() {
					if(game.getScreen() == MainMenuScreen.this) {
						game.setScreen(new LevelEditorScreen(game));
						dispose();
					}
				}
			});
		else if(DebugConfig.autoAdvanceLevels)
			startRequested = true;
	}

	@Override
	public void render(float delta) {
		Gdx.gl.glClearColor(237/256f, 27/256f, 81/256f,1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		game.batch.setProjectionMatrix(camera.combined);

		stage.act();
		stage.draw();

		game.batch.begin();
		labelStart.draw(game.batch, (1 + MathUtils.cos(startAlpha += 7*Gdx.graphics.getDeltaTime()))/2);
		game.batch.end();

		if(startRequested){
			game.setScreen(new GameScreen(game));
			dispose();
		}

	}

	@Override
	public void show() {
		Gdx.input.setInputProcessor(this);
	}

	@Override
	public boolean keyDown(int keycode) {
		if((keycode == Keys.E || keycode == WEBGL_E_KEYCODE) && editorAvailable()) {
			game.setScreen(new LevelEditorScreen(game));
			dispose();
			return true;
		}
		if(keycode == Keys.F || keycode == Keys.SPACE) {
			startRequested = true;
			return true;
		}
		return false;
	}

	private boolean editorAvailable(){
		return Gdx.app.getType() != ApplicationType.Android && !PlatformInfo.mobileBrowser;
	}

	@Override
	public boolean touchDown(int screenX, int screenY, int pointer, int button) {
		if(Gdx.app.getType() == ApplicationType.Android || PlatformInfo.mobileBrowser) {
			startRequested = true;
			return true;
		}
		return false;
	}

	@Override
	public void resize(int width, int height) {
		width = Math.max(1, width);
		height = Math.max(1, height);
		camera.setToOrtho(false, width, height);
		camera.update();
		stage.getViewport().update(width, height, true);

		labelTitre.setFontScale(1f);
		labelTitreOmbre.setFontScale(1f);
		labelStart.setFontScale(1f);
		labelTitre.pack();
		labelTitreOmbre.pack();
		labelStart.pack();
		float titleScale = Math.min(1f, Math.min((width * 0.90f) / labelTitre.getWidth(),
				(height * 0.22f) / labelTitre.getHeight()));
		labelTitre.setFontScale(titleScale);
		labelTitreOmbre.setFontScale(titleScale);
		labelTitre.pack();
		labelTitreOmbre.pack();
		float startScale = Math.min(1f, Math.min((width * 0.82f) / labelStart.getWidth(),
				(height * 0.10f) / labelStart.getHeight()));
		if(mobileStartText)
			startScale = Math.min(startScale, MOBILE_START_TEXT_MAX_SCALE);
		labelStart.setFontScale(startScale);
		labelStart.pack();
		labelTitre.setPosition(0.5f * width - labelTitre.getWidth()/2f,
				0.5f * height - labelTitre.getHeight()/2f);
		labelTitreOmbre.setPosition(labelTitre.getX() + width/380f,
				labelTitre.getY() - width/380f);
		float startCenterY = labelTitre.getY()/2f;
		labelStart.setPosition(0.5f * width - labelStart.getWidth()/2f,
				startCenterY - labelStart.getHeight()/2f);
		transitionImage.setBounds(-width, 0, width, height);
		DebugConfig.log("main menu layout screen=" + width + "x" + height
				+ " startText=" + labelStart.getText()
				+ " titleBounds=" + labelTitre.getX() + "," + labelTitre.getY()
				+ "," + labelTitre.getWidth() + "," + labelTitre.getHeight()
				+ " startBounds=" + labelStart.getX() + "," + labelStart.getY()
				+ "," + labelStart.getWidth() + "," + labelStart.getHeight());

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
		stage.dispose();
		skin.dispose();

	}

}
