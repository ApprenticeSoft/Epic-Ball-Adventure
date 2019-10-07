package screen;

import utils.Variables;

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
import com.one.button.jam.MyGdxGame;

public class MainMenuScreen extends InputAdapter implements Screen{

	final MyGdxGame game;
	private OrthographicCamera camera;
	private Stage stage;
	private Skin skin;
	private TextureAtlas textureAtlas;
	private LabelStyle labelStyleTitre, labelStyleOmbre, labelStyleStart;
	private Label labelTitre, labelTitreOmbre, labelStart;
	private Image transitionImage;
	private float startAlpha = 0;
	
	public MainMenuScreen(final MyGdxGame gam){
		game = gam;
		
		camera = new OrthographicCamera();
		camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		
		stage = new Stage();
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
		labelTitre.setX(0.5f * Gdx.graphics.getWidth() - labelTitre.getWidth()/2);
		labelTitre.setY(0.5f * Gdx.graphics.getHeight() - labelTitre.getHeight()/2);
		
		labelTitreOmbre = new Label(Variables.gameTitle, labelStyleOmbre);
		labelTitreOmbre.setX(labelTitre.getX() + Gdx.graphics.getWidth()/380);
		labelTitreOmbre.setY(labelTitre.getY() - Gdx.graphics.getWidth()/380);

		if(Gdx.app.getType() == ApplicationType.Desktop)
			labelStart = new Label("Press F to Start", labelStyleStart);
		else if(Gdx.app.getType() == ApplicationType.Android)
			labelStart = new Label("Touch The Screen to Start", labelStyleStart);
		labelStart.setX(0.5f * Gdx.graphics.getWidth() - labelStart.getWidth()/2);
		labelStart.setY(0.22f * Gdx.graphics.getWidth() - labelStart.getHeight()/2);
		
		transitionImage = new Image(skin.getDrawable("WhiteSquare"));
		transitionImage.setWidth(Gdx.graphics.getWidth());
		transitionImage.setHeight(Gdx.graphics.getHeight());
		transitionImage.setColor(new Color(237/256f, 27/256f, 81/256f,1));
		transitionImage.setX(-Gdx.graphics.getWidth());
		transitionImage.setY(0);
		transitionImage.addAction(Actions.alpha(0));
		
		stage.addActor(labelTitreOmbre);
		stage.addActor(labelTitre);
		stage.addActor(transitionImage);
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
		
		if(Gdx.app.getType() == ApplicationType.Desktop){
			if(Gdx.input.isKeyPressed(Keys.F))
				game.setScreen(new GameScreen(game));
		}
		else if(Gdx.app.getType() == ApplicationType.Android){
			if(Gdx.input.isTouched())
				game.setScreen(new GameScreen(game));
		}
		
	}

	@Override
	public void show() {
		Gdx.input.setInputProcessor(stage);
	}

	@Override
	public void resize(int width, int height) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void pause() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void resume() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void hide() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub
		
	}

}
