package screen;

import utils.Data;
import utils.DebugConfig;
import utils.LecteurCarte;
import utils.LevelProgression;
import utils.MyCamera;
import utils.OrthogonalTiledMapRendererWithSprites;
import utils.Variables;
import bodies.Eau;
import bodies.Obstacle;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapRenderer;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.Manifold;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.one.button.jam.Couleurs;
import com.one.button.jam.MyGdxGame;

public class GameScreen extends InputAdapter implements Screen{

	private static final float MAX_FRAME_DELTA = 0.25f;
	private static final int MAX_PHYSICS_STEPS = 5;
	private static final float LEVEL_TRANSITION_DURATION = 1.35f;

	final MyGdxGame game;
	private MyCamera camera;
	TiledMap tiledMap;
	TiledMapRenderer tiledMapRenderer;
	private LecteurCarte lecteurCarte;
	private World world;
    private Box2DDebugRenderer debugRenderer;
    private Stage stage;

	private TextureAtlas textureAtlas;

    private int nbTileHorizontal, dimension;
	private float ratio;
	private Couleurs couleurs;

	private LabelStyle labelStyleRestart, labelStyleRestartOmbre;
	private Label labelRestart, labelRestartOmbre;

	private PolygonSpriteBatch polyBatch;

	/***************Sounds****************/
	private Sound soundWin, soundFall, soundWater, soundChock, soundSpring;

	/*******************TEST SHADERS**********************/
    String vertexShader;
    String fragmentShader;
    ShaderProgram shaderProgram;
    //Vignettage
    float posX, posY, outerRadius = 25, innerRadius = 23;

    FrameBuffer fbo;
    TextureRegion fboRegion;
    private int fboWidth, fboHeight;
    private float physicsAccumulator;
    private float transitionElapsed;
    private float transitionStartRadius;
    private boolean transitionInitialized;
    private boolean nextLevelQueued;
    private boolean gameCompleted;
    private boolean disposed;
    private boolean transitionFallbackLogged;
    private boolean debugAutoAdvanceTriggered;
    private float debugAutoAdvanceElapsed;
    private String lastRestartLayoutLog;
    private final Vector2 transitionVelocity = new Vector2();
    private final Vector3 projectedBallPosition = new Vector3();

	public GameScreen(final MyGdxGame gam){
		game = gam;

		Variables.levelComplete = false;
		DebugConfig.log("GameScreen construct begin level=" + Variables.niveauSelectione
				+ " graphics=" + Gdx.graphics.getWidth() + "x" + Gdx.graphics.getHeight()
				+ " backBuffer=" + Gdx.graphics.getBackBufferWidth() + "x" + Gdx.graphics.getBackBufferHeight());

		soundChock = game.assets.get("Sounds/Chock.wav", Sound.class);
		soundFall = game.assets.get("Sounds/Fall.wav", Sound.class);
		soundWin = game.assets.get("Sounds/FinishLevel.wav", Sound.class);
		soundSpring = game.assets.get("Sounds/Spring.wav", Sound.class);
		soundWater = game.assets.get("Sounds/Water.wav", Sound.class);

		nbTileHorizontal = 50;
		dimension = nbTileHorizontal * Variables.PPT;
		ratio = (float)Gdx.graphics.getHeight()/(float)Gdx.graphics.getWidth();

		camera = new MyCamera();
		camera.setToOrtho(false, dimension * Variables.WORLD_TO_BOX, dimension * Variables.WORLD_TO_BOX * ratio);
        camera.update();

        polyBatch = new PolygonSpriteBatch();
        polyBatch.setProjectionMatrix(camera.combined);

        textureAtlas = game.assets.get("Images/Images.pack", TextureAtlas.class);

		couleurs = new Couleurs(4);

        world = new World(new Vector2(0, Variables.GRAVITE), true);
		World.setVelocityThreshold(10.0f);	//La valeur par défaut est 1.0

		debugRenderer = new Box2DDebugRenderer();

        DebugConfig.log("loading tmx level=" + Variables.niveauSelectione);
        tiledMap = new TmxMapLoader().load("Levels/Level "+ Variables.niveauSelectione + ".tmx");
        DebugConfig.log("loaded tmx level=" + Variables.niveauSelectione
				+ " map=" + tiledMap.getProperties().get("width", Integer.class)
				+ "x" + tiledMap.getProperties().get("height", Integer.class));
        //tiledMap = new TmxMapLoader().load("Levels/Level 5.tmx");
        tiledMapRenderer = new OrthogonalTiledMapRendererWithSprites(tiledMap,Variables.WORLD_TO_BOX, game.batch);

        lecteurCarte = new LecteurCarte(gam, tiledMap, world, camera, couleurs);
        DebugConfig.log("level objects level=" + Variables.niveauSelectione
				+ " obstacles=" + lecteurCarte.obstacles.size
				+ " drawObstacles=" + lecteurCarte.obstaclesOrganises.size
				+ " polygons=" + lecteurCarte.polygones.size
				+ " platforms=" + lecteurCarte.plateformes.size
				+ " springs=" + lecteurCarte.springs.size
				+ " water=" + lecteurCarte.waters.size);

        /*
         * Label restart
         */
        stage = new Stage(new ScreenViewport());
        labelStyleRestart = new LabelStyle();
		labelStyleRestart.fontColor = new Color(237/256f, 246/256f, 47/256f,1);
		labelStyleRestart.font = game.assets.get("fontRestart.ttf", BitmapFont.class);

		labelStyleRestartOmbre = new LabelStyle();
		labelStyleRestartOmbre.fontColor = new Color(81/256f, 166/256f, 220/256f,1);
		labelStyleRestartOmbre.font = game.assets.get("fontRestart.ttf", BitmapFont.class);

		labelRestart = new Label("Restart in", labelStyleRestart);
		labelRestart.setAlignment(Align.center);
		labelRestart.setX(0.5f * Gdx.graphics.getWidth() - labelRestart.getWidth()/2);
		labelRestart.setY(0.5f * Gdx.graphics.getHeight() - labelRestart.getHeight()/2);

		labelRestartOmbre = new Label("Restart in", labelStyleRestartOmbre);
		labelRestartOmbre.setAlignment(Align.center);
		labelRestartOmbre.setX(labelRestart.getX() + Gdx.graphics.getWidth()/380);
		labelRestartOmbre.setY(labelRestart.getY() - Gdx.graphics.getWidth()/380);

		stage.addActor(labelRestartOmbre);
		stage.addActor(labelRestart);

        /*******************TEST SHADERS**********************/
		ShaderProgram.pedantic = false;	//Important pour pouvoir modifier les variables uniformes
	vertexShader = Gdx.files.internal("Shaders/VignetteV.glsl").readString();
		if(Gdx.app.getType() == ApplicationType.Desktop)
		fragmentShader = Gdx.files.internal("Shaders/VignetteF.glsl").readString();
		else
		fragmentShader = Gdx.files.internal("Shaders/VignetteFAndroid.glsl").readString();
	shaderProgram = new ShaderProgram(vertexShader,fragmentShader);
	//game.batch.setShader(shaderProgram);

	if(shaderProgram.isCompiled()){
		shaderProgram.begin();
		shaderProgram.setUniformf("u_resolution", Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
		shaderProgram.setUniformf("u_center", posX, posY);
		shaderProgram.end();
	}
	else
		DebugConfig.log("shader compile failed level=" + Variables.niveauSelectione + " log=" + shaderProgram.getLog());

	resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
	DebugConfig.log("GameScreen construct end level=" + Variables.niveauSelectione);
	}

	@Override
	public void render(float delta) {
		float frameDelta = Math.min(delta, MAX_FRAME_DELTA);
		Gdx.gl.glClearColor(couleurs.getCouleurFond().r,couleurs.getCouleurFond().g,couleurs.getCouleurFond().b,1);
		//Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		lecteurCarte.balle.updateInput();
		updateDebugAutoAdvance(frameDelta);

		if(!Variables.levelComplete)
			stepPhysics(frameDelta);
		else
			physicsAccumulator = 0;

        camera.mouvement(lecteurCarte.balle, tiledMap, frameDelta);
        camera.update();
        polyBatch.setProjectionMatrix(camera.combined);

		if(Variables.levelComplete)
			levelComplete(frameDelta);
		else
			drawGameplay();

		//Level lost
        if(!Variables.levelComplete && lecteurCarte.balle.getY() < -5){
	if(Variables.fallRestartDelay == 2.136f)
	soundFall.play();

	Variables.fallRestartDelay -= frameDelta;

	if(Variables.fallRestartDelay <= 0)
		Variables.restart = true;
        }
        if(Variables.restart){
	Variables.restart = false;
	lecteurCarte.restart();
        }
		if(lecteurCarte.balle.restart){
	levelRestart();
        }
        if(DebugConfig.showRestartOverlay && !Variables.levelComplete && !gameCompleted)
			debugRestartOverlay();
	}

	@Override
	public void show() {
		Gdx.input.setInputProcessor(this);
		world.setContactListener(new ContactListener(){
			@Override
			public void beginContact(Contact contact) {
				Fixture fixtureA = contact.getFixtureA();
				Fixture fixtureB = contact.getFixtureB();

				if(fixtureA.getUserData() != null && fixtureB.getUserData() != null) {
				//Finish the level
				if(fixtureA.getUserData().equals("Ball") && fixtureB.getUserData().equals("Exit")){
					startLevelComplete();
				}
				else if(fixtureB.getUserData().equals("Ball") && fixtureA.getUserData().equals("Exit")){
					startLevelComplete();
				}

				    //Spring
				if((fixtureA.getUserData().equals("Ball") || fixtureA.getUserData().equals("Light")) && fixtureB.getUserData().equals("Spring")){
					for(Obstacle spring : lecteurCarte.springs){
						if(spring.body == fixtureB.getBody()){
							spring.actif();
							soundSpring.play();
						}
					}
				}
				else if((fixtureB.getUserData().equals("Ball") || fixtureB.getUserData().equals("Light")) && fixtureA.getUserData().equals("Spring")){
					for(Obstacle spring : lecteurCarte.springs){
						if(spring.body == fixtureA.getBody()){
							spring.actif();
							soundSpring.play();
						}
					}
				}
				}

			    //EAU
			    if ((fixtureA.getUserData() != null && fixtureA.getUserData().equals("Water")) && fixtureB.getBody().getType() == BodyType.DynamicBody) {
				for(Obstacle obstacle : lecteurCarte.waters){
					if(obstacle.body.getFixtureList().get(0) == fixtureA){
						Eau eau = (Eau) obstacle;
						eau.buoyancyController.addBody(fixtureB);

						if(fixtureB.getUserData().equals("Ball")){
							System.out.println("Balle à l'eau !");
							lecteurCarte.balle.restart = true;
						}

						soundWater.play();
					}
				}
				}
			    else if ((fixtureB.getUserData() != null && fixtureB.getUserData().equals("Water")) && fixtureA.getBody().getType() == BodyType.DynamicBody) {
					for(Obstacle obstacle : lecteurCarte.waters){
					if(obstacle.body.getFixtureList().get(0) == fixtureB){
						Eau eau = (Eau) obstacle;
						eau.buoyancyController.addBody(fixtureA);

						if(fixtureA.getUserData().equals("Ball")){
							System.out.println("Balle à l'eau !");
							lecteurCarte.balle.restart = true;
						}

						soundWater.play();
					}
				}
				}

			}

			@Override
			public void endContact(Contact contact) {
				Fixture FixtureA = contact.getFixtureA();
				Fixture FixtureB = contact.getFixtureB();

			    //EAU
			    if ((FixtureA.getUserData() != null && FixtureA.getUserData().equals("Water")) && FixtureB.getBody().getType() == BodyType.DynamicBody) {
				for(Obstacle obstacle : lecteurCarte.waters){
					if(obstacle.body.getFixtureList().get(0) == FixtureA){
						Eau eau = (Eau) obstacle;
						eau.buoyancyController.removeBody(FixtureB);
					}
				}
				}
			    else if ((FixtureB.getUserData() != null && FixtureB.getUserData().equals("Water")) && FixtureA.getBody().getType() == BodyType.DynamicBody) {
					for(Obstacle obstacle : lecteurCarte.waters){
					if(obstacle.body.getFixtureList().get(0) == FixtureB){
						Eau eau = (Eau) obstacle;
						eau.buoyancyController.removeBody(FixtureA);
					}
				}
				}
			}

			@Override
			public void preSolve(Contact contact, Manifold oldManifold) {
				Body a = contact.getFixtureA().getBody();
			    Body b = contact.getFixtureB().getBody();

			    if((a.getUserData() != null && a.getUserData().equals("Objet")) && (b.getUserData() != null && b.getUserData().equals("Objet"))) {
				contact.setEnabled(false);
				}
			}

			@Override
			public void postSolve(Contact contact, ContactImpulse impulse) {
				Body bodyA = contact.getFixtureA().getBody();
			    Body bodyB = contact.getFixtureB().getBody();

				//Chock sound
			for(int i = 0; i < impulse.getNormalImpulses().length; i++){
				if(impulse.getNormalImpulses()[i] > 1){
					System.out.println("Impulse = " + impulse.getNormalImpulses()[i]);
					soundChock.play();
				}
			}

			}
		});
	}

	@Override
	public void resize(int width, int height) {
		width = Math.max(1, width);
		height = Math.max(1, height);
		ratio = (float)height/(float)width;
		camera.setToOrtho(false, dimension * Variables.WORLD_TO_BOX, dimension * Variables.WORLD_TO_BOX * ratio);
		if(lecteurCarte != null && tiledMap != null)
			camera.mouvement(lecteurCarte.balle, tiledMap, 0);
        camera.update();
        polyBatch.setProjectionMatrix(camera.combined);

        Variables.updateGraphicsMetrics();
        if(stage != null){
			stage.getViewport().update(width, height, true);
			layoutRestartLabels();
        }
        resizeFrameBuffer(Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
        if(transitionInitialized){
			transitionStartRadius = getBackBufferDiagonal();
			updateVignetteState();
        }
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
		if(disposed)
			return;
		disposed = true;

		if(game.batch != null)
			game.batch.setShader(null);
		if(lecteurCarte != null)
			lecteurCarte.disposeResources();
		if(tiledMap != null)
			tiledMap.dispose();
		if(world != null)
			world.dispose();
		if(debugRenderer != null)
			debugRenderer.dispose();
		if(stage != null)
			stage.dispose();
		if(polyBatch != null)
			polyBatch.dispose();
		if(shaderProgram != null)
			shaderProgram.dispose();
		if(fbo != null)
			fbo.dispose();
	}

	private void stepPhysics(float frameDelta){
		physicsAccumulator += frameDelta;
		int steps = 0;
		while(physicsAccumulator >= Variables.BOX_STEP && steps < MAX_PHYSICS_STEPS){
			lecteurCarte.fixedStep();
			world.step(Variables.BOX_STEP, Variables.BOX_VELOCITY_ITERATIONS, Variables.BOX_POSITION_ITERATIONS);
			physicsAccumulator -= Variables.BOX_STEP;
			steps++;
		}
		if(steps == MAX_PHYSICS_STEPS && physicsAccumulator >= Variables.BOX_STEP)
			physicsAccumulator = 0;
		lecteurCarte.updateTimers(frameDelta);
	}

	private void drawGameplay(){
        tiledMapRenderer.setView(camera);
        game.batch.setProjectionMatrix(camera.combined);
        game.batch.begin();
		lecteurCarte.draw(game.batch, textureAtlas/*, couleurs*/);
		game.batch.end();

		polyBatch.setProjectionMatrix(camera.combined);
		polyBatch.begin();
		lecteurCarte.drawPolygone(polyBatch, camera);
		polyBatch.end();
	}

	private void startLevelComplete(){
		if(Variables.levelComplete || nextLevelQueued || gameCompleted)
			return;
		DebugConfig.log("level complete start level=" + Variables.niveauSelectione
				+ " ball=" + lecteurCarte.balle.body.getPosition()
				+ " exit=" + lecteurCarte.exit.body.getPosition());
		Variables.levelComplete = true;
		soundWin.play();
	}

	public void levelComplete(float delta){
		if(gameCompleted){
			drawGameCompleted();
			return;
		}

		initializeLevelTransition();

		transitionElapsed += delta;
		updateVignetteState();
		renderTransitionFrame();

		if(LevelProgression.transitionComplete(transitionElapsed, LEVEL_TRANSITION_DURATION)){
	if(LevelProgression.hasNextLevel(Variables.niveauSelectione, Variables.nombreNiveaux))
		queueNextLevel();
	else{
		gameCompleted = true;
		DebugConfig.log("game complete level=" + Variables.niveauSelectione);
		drawGameCompleted();
	}
        }
	}

	public void levelRestart(){
	labelRestart.setText("Restart in\n" + ((int)lecteurCarte.balle.restartDelay + 1));
	labelRestartOmbre.setText("Restart in\n" + ((int)lecteurCarte.balle.restartDelay + 1));
	layoutRestartLabels();

	stage.act();
	stage.draw();
	}

	private void debugRestartOverlay(){
		labelRestart.setText("Restart in\n3");
		labelRestartOmbre.setText("Restart in\n3");
		layoutRestartLabels();
		stage.act();
		stage.draw();
	}

	private void initializeLevelTransition(){
		if(transitionInitialized)
			return;
		transitionInitialized = true;
		physicsAccumulator = 0;
		transitionElapsed = 0;
		transitionStartRadius = getBackBufferDiagonal();
		outerRadius = transitionStartRadius;
		innerRadius = Math.max(0, outerRadius - getVignetteFeather());
		DebugConfig.log("transition init level=" + Variables.niveauSelectione
				+ " radius=" + outerRadius
				+ " feather=" + getVignetteFeather()
				+ " camera=" + camera.position
				+ " viewport=" + camera.viewportWidth + "x" + camera.viewportHeight
				+ " graphics=" + Gdx.graphics.getWidth() + "x" + Gdx.graphics.getHeight()
				+ " backBuffer=" + getBackBufferWidth() + "x" + getBackBufferHeight()
				+ " shaderCompiled=" + (shaderProgram != null && shaderProgram.isCompiled())
				+ " fbo=" + fboWidth + "x" + fboHeight);
		lecteurCarte.balle.body.getFixtureList().get(0).setSensor(true);
		transitionVelocity.set(lecteurCarte.exit.body.getPosition()).sub(lecteurCarte.balle.body.getPosition());
		lecteurCarte.balle.body.setLinearVelocity(transitionVelocity);
		lecteurCarte.balle.body.setAngularVelocity(1f);
	}

	private void updateVignetteState(){
		float progress = LevelProgression.transitionProgress(transitionElapsed, LEVEL_TRANSITION_DURATION);
		outerRadius = Math.max(1f, transitionStartRadius * (1f - progress));
		innerRadius = Math.max(0, outerRadius - getVignetteFeather());

		if(shaderProgram == null || !shaderProgram.isCompiled())
			return;

		int resolutionX = getBackBufferWidth();
		int resolutionY = getBackBufferHeight();
		camera.project(projectedBallPosition.set(lecteurCarte.balle.getX(),lecteurCarte.balle.getY(),0),
				0, 0, resolutionX, resolutionY);
		posX = projectedBallPosition.x;
		posY = projectedBallPosition.y;

		shaderProgram.begin();
		shaderProgram.setUniformf("u_resolution", resolutionX, resolutionY);
		shaderProgram.setUniformf("u_center", posX, posY);
		shaderProgram.setUniformf("outerRadius", outerRadius);
		shaderProgram.setUniformf("innerRadius", innerRadius);
		shaderProgram.end();
	}

	private void renderTransitionFrame(){
		if(fbo == null || fboRegion == null || shaderProgram == null || !shaderProgram.isCompiled()){
			if(!transitionFallbackLogged){
				transitionFallbackLogged = true;
				DebugConfig.log("transition fallback draw level=" + Variables.niveauSelectione
						+ " fbo=" + (fbo != null)
						+ " fboRegion=" + (fboRegion != null)
						+ " shader=" + (shaderProgram != null)
						+ " shaderCompiled=" + (shaderProgram != null && shaderProgram.isCompiled()));
			}
			drawGameplay();
			return;
		}

		fbo.begin();
		Gdx.graphics.getGL20().glClearColor(couleurs.getCouleurFond().r,couleurs.getCouleurFond().g,couleurs.getCouleurFond().b,1);
		Gdx.graphics.getGL20().glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
		drawGameplay();
		fbo.end();

		game.batch.setProjectionMatrix(camera.combined);
		game.batch.begin();
		game.batch.setColor(1, 1, 1, 1);
		game.batch.setShader(shaderProgram);
		game.batch.draw(fboRegion, camera.position.x - camera.viewportWidth/2, camera.position.y - camera.viewportHeight/2, camera.viewportWidth, camera.viewportHeight);
		game.batch.setShader(null);
		game.batch.end();
	}

	private void queueNextLevel(){
		if(nextLevelQueued)
			return;
		nextLevelQueued = true;
		int nextLevel = LevelProgression.nextLevel(Variables.niveauSelectione, Variables.nombreNiveaux);
		DebugConfig.log("queue next level from=" + Variables.niveauSelectione + " to=" + nextLevel
				+ " elapsed=" + transitionElapsed);
		Gdx.app.postRunnable(new Runnable() {
			@Override
			public void run() {
				if(game.getScreen() != GameScreen.this)
					return;
				try{
					Variables.niveauSelectione = nextLevel;
					DebugConfig.log("constructing queued level=" + Variables.niveauSelectione);
					GameScreen nextScreen = new GameScreen(game);
					game.setScreen(nextScreen);
					DebugConfig.log("queued level active level=" + Variables.niveauSelectione);
					GameScreen.this.dispose();
				}
				catch(RuntimeException exception){
					DebugConfig.log("queued level failed level=" + Variables.niveauSelectione + " error=" + exception);
					throw exception;
				}
				catch(Error error){
					DebugConfig.log("queued level failed level=" + Variables.niveauSelectione + " error=" + error);
					throw error;
				}
			}
		});
	}

	private void updateDebugAutoAdvance(float delta){
		if(!DebugConfig.autoAdvanceLevels || Variables.levelComplete || gameCompleted || debugAutoAdvanceTriggered)
			return;
		debugAutoAdvanceElapsed += delta;
		if(debugAutoAdvanceElapsed >= DebugConfig.autoAdvanceDelay){
			debugAutoAdvanceTriggered = true;
			DebugConfig.log("debug auto advance trigger level=" + Variables.niveauSelectione
					+ " after=" + debugAutoAdvanceElapsed);
			startLevelComplete();
		}
	}

	private void drawGameCompleted(){
		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		labelRestart.setText("Game Complete !\nThanks for playing !");
		labelRestartOmbre.setText("Game Complete !\nThanks for playing !");
		layoutRestartLabels();
		stage.act();
		stage.draw();
	}

	@Override
	public boolean keyDown(int keycode) {
		if(gameCompleted && keycode == Keys.SPACE){
			returnToMainMenu();
			return true;
		}
		return false;
	}

	@Override
	public boolean touchDown(int screenX, int screenY, int pointer, int button) {
		if(gameCompleted){
			returnToMainMenu();
			return true;
		}
		return false;
	}

	private void returnToMainMenu(){
		DebugConfig.log("return to main menu after game complete");
		DebugConfig.autoAdvanceLevels = false;
		Variables.niveauSelectione = 1;
		Variables.levelComplete = false;
		Variables.restart = false;
		Variables.fallRestartDelay = 2.136f;
		game.setScreen(new MainMenuScreen(game));
		dispose();
	}

	private void layoutRestartLabels(){
		if(labelRestart == null || labelRestartOmbre == null)
			return;
		float width = stage != null ? stage.getViewport().getWorldWidth() : Gdx.graphics.getWidth();
		float height = stage != null ? stage.getViewport().getWorldHeight() : Gdx.graphics.getHeight();
		width = Math.max(1f, width);
		height = Math.max(1f, height);

		labelRestart.setFontScale(1f);
		labelRestartOmbre.setFontScale(1f);
		labelRestart.pack();
		labelRestartOmbre.pack();
		float scale = Math.min(1f, Math.min((width * 0.88f) / labelRestart.getWidth(),
				(height * 0.42f) / labelRestart.getHeight()));
		labelRestart.setFontScale(scale);
		labelRestartOmbre.setFontScale(scale);
		labelRestart.pack();
		labelRestartOmbre.pack();
		labelRestart.setAlignment(Align.center);
		labelRestartOmbre.setAlignment(Align.center);
		labelRestart.setPosition(0.5f * width - labelRestart.getWidth()/2,
				0.5f * height - labelRestart.getHeight()/2);
		labelRestartOmbre.setPosition(labelRestart.getX() + width/380f,
				labelRestart.getY() - width/380f);
		String text = labelRestart.getText().toString().replace('\n', '|');
		String layoutLog = "restart label layout screen=" + width + "x" + height
				+ " text=" + text
				+ " bounds=" + labelRestart.getX() + "," + labelRestart.getY()
				+ "," + labelRestart.getWidth() + "," + labelRestart.getHeight();
		if(!layoutLog.equals(lastRestartLayoutLog)){
			lastRestartLayoutLog = layoutLog;
			DebugConfig.log(layoutLog);
		}
	}

	private int getBackBufferWidth(){
		return Math.max(1, Gdx.graphics.getBackBufferWidth());
	}

	private int getBackBufferHeight(){
		return Math.max(1, Gdx.graphics.getBackBufferHeight());
	}

	private float getBackBufferDiagonal(){
		float width = getBackBufferWidth();
		float height = getBackBufferHeight();
		return (float)Math.sqrt(width * width + height * height);
	}

	private float getVignetteFeather(){
		return Math.max(48f, Math.min(getBackBufferWidth(), getBackBufferHeight()) * 0.08f);
	}

	private void resizeFrameBuffer(int width, int height){
		width = Math.max(1, width);
		height = Math.max(1, height);
		if(fbo != null && fboWidth == width && fboHeight == height){
			updateShaderResolution();
			return;
		}
		if(fbo != null)
			fbo.dispose();
		fbo = null;
		fboRegion = null;
		fboWidth = 0;
		fboHeight = 0;

		try{
			fbo = new FrameBuffer(Format.RGB565, width, height, false);
			fboRegion = new TextureRegion(fbo.getColorBufferTexture());
			fboRegion.flip(false, true);
			fboWidth = width;
			fboHeight = height;
		}
		catch(RuntimeException ignored){
			fbo = null;
			fboRegion = null;
		}
		updateShaderResolution();
	}

	private void updateShaderResolution(){
		if(shaderProgram == null || !shaderProgram.isCompiled())
			return;
		float resolutionX = fboWidth > 0 ? fboWidth : getBackBufferWidth();
		float resolutionY = fboHeight > 0 ? fboHeight : getBackBufferHeight();
		shaderProgram.begin();
		shaderProgram.setUniformf("u_resolution", resolutionX, resolutionY);
		shaderProgram.end();
	}

}
