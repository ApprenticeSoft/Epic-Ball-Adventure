package screen;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGeneratorLoader;
import com.badlogic.gdx.graphics.g2d.freetype.FreetypeFontLoader;
import com.badlogic.gdx.graphics.g2d.freetype.FreetypeFontLoader.FreeTypeFontLoaderParameter;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.one.button.jam.MyGdxGame;

public class LoadingScreen implements Screen{
	
	final MyGdxGame game;
	OrthographicCamera camera;
	private Texture textureLogo;
	private Image imageLogo;
	private Stage stage;

	public LoadingScreen(final MyGdxGame gam){
		game = gam;

		camera = new OrthographicCamera();
		camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		
		textureLogo = new Texture(Gdx.files.internal("Images/Logo.png"), true);
		textureLogo.setFilter(TextureFilter.MipMapLinearNearest, TextureFilter.MipMapLinearNearest);
		imageLogo = new Image(textureLogo);
		imageLogo.setHeight(5 * Gdx.graphics.getHeight()/10);
		imageLogo.setWidth(imageLogo.getHeight() * textureLogo.getWidth() / textureLogo.getHeight());
		imageLogo.setX(Gdx.graphics.getWidth()/2 - imageLogo.getWidth()/2);
		imageLogo.setY(Gdx.graphics.getHeight()/2 - imageLogo.getHeight()/2);
		stage = new Stage();
		
		//Chargement du TextureAtlas
		game.assets.load("Images/Images.pack", TextureAtlas.class);
		
		//Chargement des polices
		FileHandleResolver resolver = new InternalFileHandleResolver();
		game.assets.setLoader(FreeTypeFontGenerator.class, new FreeTypeFontGeneratorLoader(resolver));
		game.assets.setLoader(BitmapFont.class, ".ttf", new FreetypeFontLoader(resolver));
		
		//Loading of the sounds
		game.assets.load("Sounds/Chock.wav", Sound.class);
		game.assets.load("Sounds/Fall.wav", Sound.class);
		game.assets.load("Sounds/FinishLevel.wav", Sound.class);
		game.assets.load("Sounds/Spring.wav", Sound.class);
		game.assets.load("Sounds/Water.wav", Sound.class);
		
		FreeTypeFontLoaderParameter size1Params = new FreeTypeFontLoaderParameter();
		size1Params.fontFileName = "Fonts/calibri.ttf";			
		size1Params.fontParameters.genMipMaps = true;					
		size1Params.fontParameters.minFilter = TextureFilter.Linear;
		size1Params.fontParameters.magFilter = TextureFilter.Linear;						
		size1Params.fontParameters.size = Gdx.graphics.getWidth()/26;
		game.assets.load("font1.ttf", BitmapFont.class, size1Params);
		
		FreeTypeFontLoaderParameter size2Params = new FreeTypeFontLoaderParameter();
		size2Params.fontFileName = "Fonts/HARLOWSI.TTF";			
		size2Params.fontParameters.genMipMaps = true;					
		size2Params.fontParameters.minFilter = TextureFilter.Linear;
		size2Params.fontParameters.magFilter = TextureFilter.Linear;						
		size2Params.fontParameters.size = Gdx.graphics.getWidth()/12;
		game.assets.load("fontTitre.ttf", BitmapFont.class, size2Params);
		
		FreeTypeFontLoaderParameter size3Params = new FreeTypeFontLoaderParameter();
		size3Params.fontFileName = "Fonts/HARLOWSI.TTF";			
		size3Params.fontParameters.genMipMaps = true;					
		size3Params.fontParameters.minFilter = TextureFilter.Linear;
		size3Params.fontParameters.magFilter = TextureFilter.Linear;						
		size3Params.fontParameters.size = Gdx.graphics.getWidth()/10;
		game.assets.load("fontRestart.ttf", BitmapFont.class, size3Params);
		
	}
	
	@Override
	public void render(float delta) {
		Gdx.gl.glClearColor(1, 1, 1, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        
        camera.update();
		game.batch.setProjectionMatrix(camera.combined);

        stage.act();
        stage.draw();
        
        
		if(game.assets.update())
        	((Game)Gdx.app.getApplicationListener()).setScreen(new MainMenuScreen(game));
        	  
	}
	
	@Override
	public void show() {
		stage.addActor(imageLogo);
		
		imageLogo.addAction(Actions.sequence(Actions.alpha(0)
                ,Actions.fadeIn(0.75f),Actions.delay(1.5f)));	
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
