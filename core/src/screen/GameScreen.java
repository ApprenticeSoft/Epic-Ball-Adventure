package screen;

import utils.Data;
import utils.DebugBridge;
import utils.DebugConfig;
import utils.LecteurCarte;
import utils.LevelProgression;
import utils.LightCollisionCategories;
import utils.MyCamera;
import utils.OrthogonalTiledMapRendererWithSprites;
import utils.PlatformInfo;
import utils.SpringLightGeometry;
import utils.SpringLightSystem;
import utils.Variables;
import utils.WaterRefractionRenderer;
import utils.WaterSplashSystem;
import bodies.Eau;
import bodies.Obstacle;
import bodies.Spring;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapRenderer;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.math.MathUtils;
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
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.one.button.jam.Couleurs;
import com.one.button.jam.MyGdxGame;

public class GameScreen extends InputAdapter implements Screen{

	private static final float MAX_FRAME_DELTA = 0.25f;
	private static final int MAX_PHYSICS_STEPS = 5;
	private static final float LEVEL_TRANSITION_DURATION = 1.35f;
	private static final float MOBILE_CAMERA_SHORT_SIDE_WORLD = 48f;
	private static final float WATER_TUNING_FONT_SCALE = 0.16f;
	private static final float WATER_TUNING_BUTTON_HEIGHT = 28f;

	final MyGdxGame game;
	private MyCamera camera;
	TiledMap tiledMap;
	TiledMapRenderer tiledMapRenderer;
	private LecteurCarte lecteurCarte;
	private World world;
    private Box2DDebugRenderer debugRenderer;
    private Stage stage;
	private Stage waterTuningStage;

	private TextureAtlas textureAtlas;
	private Texture waterTuningUiTexture;

    private int nbTileHorizontal, dimension;
	private float ratio;
	private Couleurs couleurs;

	private LabelStyle labelStyleRestart, labelStyleRestartOmbre;
	private Label labelRestart, labelRestartOmbre;
	private LabelStyle waterTuningLabelStyle, waterTuningTitleStyle;
	private TextButton.TextButtonStyle waterTuningButtonStyle;
	private Drawable waterTuningPanelDrawable;
	private Label waterTuningDensityLabel, waterTuningSizeLabel, waterTuningLifetimeLabel, waterTuningFoamLabel;
	private Label waterTuningStatusLabel;
	private TextButton waterTuningAdaptiveButton;

	private PolygonSpriteBatch polyBatch;
	private WaterSplashSystem waterSplashSystem;
	private WaterRefractionRenderer waterRefractionRenderer;
	private SpringLightSystem springLightSystem;

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
    private float debugSpringLightElapsed = SpringLightGeometry.FADE_SECONDS;
    private String lastRestartLayoutLog;
    private String lastCameraLayoutLog;
	private final Vector2 transitionVelocity = new Vector2();
	private final Vector3 projectedBallPosition = new Vector3();
	private final TiledMap editorTestMap;
	private final Screen editorReturnScreen;

	public GameScreen(final MyGdxGame gam){
		this(gam, null, null);
	}

	public GameScreen(final MyGdxGame gam, TiledMap testMap, Screen editorReturnScreen){
		game = gam;
		this.editorTestMap = testMap;
		this.editorReturnScreen = editorReturnScreen;

		Variables.levelComplete = false;
		DebugBridge.setCurrentLevel(Variables.niveauSelectione);
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
		updateGameplayCameraViewport(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.update();

        polyBatch = new PolygonSpriteBatch();
        polyBatch.setProjectionMatrix(camera.combined);

        textureAtlas = game.assets.get("Images/Images.pack", TextureAtlas.class);

		couleurs = new Couleurs(4);

        world = new World(new Vector2(0, Variables.GRAVITE), true);
		World.setVelocityThreshold(10.0f);	//La valeur par défaut est 1.0

		debugRenderer = new Box2DDebugRenderer();

		if(editorTestMap != null){
			tiledMap = editorTestMap;
			DebugConfig.log("loaded editor test map="
					+ tiledMap.getProperties().get("width", Integer.class)
					+ "x" + tiledMap.getProperties().get("height", Integer.class));
		}
		else{
	        DebugConfig.log("loading tmx level=" + Variables.niveauSelectione);
	        tiledMap = new TmxMapLoader().load("Levels/Level "+ Variables.niveauSelectione + ".tmx");
	        DebugConfig.log("loaded tmx level=" + Variables.niveauSelectione
					+ " map=" + tiledMap.getProperties().get("width", Integer.class)
					+ "x" + tiledMap.getProperties().get("height", Integer.class));
		}
        //tiledMap = new TmxMapLoader().load("Levels/Level 5.tmx");
        tiledMapRenderer = new OrthogonalTiledMapRendererWithSprites(tiledMap,Variables.WORLD_TO_BOX, game.batch);

        lecteurCarte = new LecteurCarte(gam, tiledMap, world, camera, couleurs);
        LightCollisionCategories.applyToWorld(world);
        springLightSystem = new SpringLightSystem(world);
        DebugConfig.log("level objects level=" + Variables.niveauSelectione
				+ " obstacles=" + lecteurCarte.obstacles.size
				+ " drawObstacles=" + lecteurCarte.obstaclesOrganises.size
				+ " polygons=" + lecteurCarte.polygones.size
				+ " platforms=" + lecteurCarte.plateformes.size
				+ " springs=" + lecteurCarte.springs.size
				+ " water=" + lecteurCarte.waters.size);
        waterSplashSystem = new WaterSplashSystem(world, lecteurCarte.waters);
		if(DebugConfig.waterBubbleProbe)
			waterSplashSystem.spawnDebugBubbleProbe();

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
		if(DebugConfig.waterTuningOverlay)
			createWaterTuningOverlay();

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

	waterRefractionRenderer = new WaterRefractionRenderer();
	resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
	DebugConfig.log("GameScreen construct end level=" + Variables.niveauSelectione);
	}

	@Override
	public void render(float delta) {
		float frameDelta = DebugConfig.fixedStep ? Variables.BOX_STEP : Math.min(delta, MAX_FRAME_DELTA);
		Gdx.gl.glClearColor(couleurs.getCouleurFond().r,couleurs.getCouleurFond().g,couleurs.getCouleurFond().b,1);
		//Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		lecteurCarte.balle.updateInput();
		updateDebugAutoAdvance(frameDelta);

		if(!Variables.levelComplete)
			stepPhysics(frameDelta);
		else
			physicsAccumulator = 0;
		if(waterSplashSystem != null && !Variables.levelComplete)
			waterSplashSystem.update(frameDelta);
		if(waterRefractionRenderer != null && !Variables.levelComplete)
			waterRefractionRenderer.update(frameDelta);
		if(springLightSystem != null)
			springLightSystem.update(frameDelta);
		updateDebugSpringLightProbe(frameDelta);

        camera.mouvement(lecteurCarte.balle, tiledMap, frameDelta);
        camera.update();
        polyBatch.setProjectionMatrix(camera.combined);

		if(Variables.levelComplete)
			levelComplete(frameDelta);
		else
			drawGameplay(true);
		if(springLightSystem != null && !gameCompleted)
			springLightSystem.render(camera);

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
	if(waterSplashSystem != null)
		waterSplashSystem.clear();
        }
		if(lecteurCarte.balle.restart){
	levelRestart();
        }
        if(DebugConfig.showRestartOverlay && !Variables.levelComplete && !gameCompleted)
			debugRestartOverlay();
		if(waterTuningStage != null && !gameCompleted)
			drawWaterTuningOverlay(frameDelta);
	}

	@Override
	public void show() {
		Gdx.input.setCatchKey(Keys.BACK, true);
		if(waterTuningStage != null)
			Gdx.input.setInputProcessor(new InputMultiplexer(waterTuningStage, this));
		else
			Gdx.input.setInputProcessor(this);
		world.setContactListener(new ContactListener(){
			@Override
			public void beginContact(Contact contact) {
				handleBeginContact(contact);
			}

			@Override
			public void endContact(Contact contact) {
				handleEndContact(contact);
			}

			@Override
			public void preSolve(Contact contact, Manifold oldManifold) {
				if(isObjectBody(contact.getFixtureA().getBody()) && isObjectBody(contact.getFixtureB().getBody()))
					contact.setEnabled(false);
			}

			@Override
			public void postSolve(Contact contact, ContactImpulse impulse) {
				handlePostSolve(impulse);
			}
		});
	}

	private void handleBeginContact(Contact contact){
		Fixture fixtureA = contact.getFixtureA();
		Fixture fixtureB = contact.getFixtureB();
		handleExitContact(fixtureA, fixtureB);
		handleSpringContact(fixtureA, fixtureB);
		handleWaterEnterContact(contact, fixtureA, fixtureB);
	}

	private void handleExitContact(Fixture fixtureA, Fixture fixtureB){
		if((isFixtureUserData(fixtureA, "Ball") && isFixtureUserData(fixtureB, "Exit"))
				|| (isFixtureUserData(fixtureB, "Ball") && isFixtureUserData(fixtureA, "Exit")))
			startLevelComplete();
	}

	private void handleSpringContact(Fixture fixtureA, Fixture fixtureB){
		if(isSpringActivator(fixtureA) && isFixtureUserData(fixtureB, "Spring")){
			activateSpring(fixtureB);
			return;
		}
		if(isSpringActivator(fixtureB) && isFixtureUserData(fixtureA, "Spring"))
			activateSpring(fixtureA);
	}

	private void activateSpring(Fixture springFixture){
		for(Spring spring : lecteurCarte.springs){
			if(spring.body == springFixture.getBody()){
				spring.actif();
				soundSpring.play();
				if(springLightSystem != null)
					springLightSystem.activate(spring);
				return;
			}
		}
	}

	private void handleWaterEnterContact(Contact contact, Fixture fixtureA, Fixture fixtureB){
		if(isFixtureUserData(fixtureA, "Water") && isDynamicFixture(fixtureB)){
			enterWater(waterForFixture(fixtureA), fixtureB, contact);
			return;
		}
		if(isFixtureUserData(fixtureB, "Water") && isDynamicFixture(fixtureA))
			enterWater(waterForFixture(fixtureB), fixtureA, contact);
	}

	private void enterWater(Eau water, Fixture fixture, Contact contact){
		if(water == null)
			return;
		water.buoyancyController.addBody(fixture);
		waterSplashSystem.enterWater(water, fixture, contact);
		if(isFixtureUserData(fixture, "Ball")){
			DebugConfig.log("ball entered water");
			lecteurCarte.balle.restart = true;
		}
		soundWater.play();
	}

	private void handleEndContact(Contact contact){
		Fixture fixtureA = contact.getFixtureA();
		Fixture fixtureB = contact.getFixtureB();
		if(isFixtureUserData(fixtureA, "Water") && isDynamicFixture(fixtureB)){
			exitWater(waterForFixture(fixtureA), fixtureB);
			return;
		}
		if(isFixtureUserData(fixtureB, "Water") && isDynamicFixture(fixtureA))
			exitWater(waterForFixture(fixtureB), fixtureA);
	}

	private void exitWater(Eau water, Fixture fixture){
		if(water == null)
			return;
		water.buoyancyController.removeBody(fixture);
		waterSplashSystem.exitWater(water, fixture);
	}

	private Eau waterForFixture(Fixture waterFixture){
		for(Eau water : lecteurCarte.waters)
			if(water.body.getFixtureList().get(0) == waterFixture)
				return water;
		return null;
	}

	private void handlePostSolve(ContactImpulse impulse){
		for(int i = 0; i < impulse.getNormalImpulses().length; i++){
			float normalImpulse = impulse.getNormalImpulses()[i];
			if(normalImpulse > 1f){
				DebugConfig.log("contact impulse=" + normalImpulse);
				soundChock.play();
			}
		}
	}

	private boolean isSpringActivator(Fixture fixture){
		return isFixtureUserData(fixture, "Ball") || isFixtureUserData(fixture, "Light");
	}

	private boolean isDynamicFixture(Fixture fixture){
		return fixture != null && fixture.getBody() != null && fixture.getBody().getType() == BodyType.DynamicBody;
	}

	private boolean isObjectBody(Body body){
		return body != null && "Objet".equals(body.getUserData());
	}

	private boolean isFixtureUserData(Fixture fixture, String expected){
		return fixture != null && expected.equals(fixture.getUserData());
	}

	@Override
	public void resize(int width, int height) {
		width = Math.max(1, width);
		height = Math.max(1, height);
		ratio = (float)height/(float)width;
		updateGameplayCameraViewport(width, height);
		if(lecteurCarte != null && tiledMap != null)
			camera.mouvement(lecteurCarte.balle, tiledMap, 0);
        camera.update();
        polyBatch.setProjectionMatrix(camera.combined);

        Variables.updateGraphicsMetrics();
        if(stage != null){
			stage.getViewport().update(width, height, true);
			layoutRestartLabels();
        }
        if(waterTuningStage != null)
			waterTuningStage.getViewport().update(width, height, true);
        resizeFrameBuffer(Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
        if(waterRefractionRenderer != null)
			waterRefractionRenderer.resize(Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
        if(springLightSystem != null)
			springLightSystem.resize(Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
        if(transitionInitialized){
			transitionStartRadius = getBackBufferDiagonal();
			updateVignetteState();
        }
	}

	@Override
	public void pause() {
	}

	@Override
	public void resume() {
	}

	@Override
	public void hide() {
		Gdx.input.setCatchKey(Keys.BACK, false);
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
		if(debugRenderer != null)
			debugRenderer.dispose();
		if(stage != null)
			stage.dispose();
		if(waterTuningStage != null)
			waterTuningStage.dispose();
		if(waterTuningUiTexture != null)
			waterTuningUiTexture.dispose();
		if(polyBatch != null)
			polyBatch.dispose();
		if(waterRefractionRenderer != null)
			waterRefractionRenderer.dispose();
		if(springLightSystem != null)
			springLightSystem.dispose();
		if(world != null)
			world.dispose();
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
		drawGameplay(true);
	}

	private void drawGameplay(boolean allowWaterRefraction){
        tiledMapRenderer.setView(camera);
		boolean refractionReady = allowWaterRefraction && captureWaterRefractionScene();

		drawGameplaySceneWithoutWater(!refractionReady);

		if(waterSplashSystem != null){
			polyBatch.setProjectionMatrix(camera.combined);
			if(refractionReady && waterRefractionRenderer.beginWaterPass(polyBatch,
					waterSplashSystem.refractionWaveStrength())){
				lecteurCarte.drawWater(polyBatch, textureAtlas, waterSplashSystem, true);
				waterRefractionRenderer.endWaterPass(polyBatch);
			}
			else{
				polyBatch.begin();
				lecteurCarte.drawWater(polyBatch, textureAtlas, waterSplashSystem);
				polyBatch.end();
			}
		}

		if(waterSplashSystem != null){
			game.batch.setProjectionMatrix(camera.combined);
			game.batch.begin();
			waterSplashSystem.drawBubbles(game.batch, textureAtlas, couleurs.getCouleurEau());
			game.batch.end();

			game.batch.setProjectionMatrix(camera.combined);
			game.batch.begin();
			waterSplashSystem.drawRipples(game.batch, textureAtlas, couleurs.getCouleurEau());
			game.batch.end();

			game.batch.setProjectionMatrix(camera.combined);
			game.batch.begin();
			waterSplashSystem.drawDropletsAndSplats(game.batch, textureAtlas, couleurs.getCouleurEau());
			game.batch.end();
		}
	}

	private void drawGameplaySceneWithoutWater(){
		drawGameplaySceneWithoutWater(false);
	}

	private void drawGameplaySceneWithoutWater(boolean drawBubblesBehindWater){
		game.batch.setProjectionMatrix(camera.combined);
		game.batch.begin();
		lecteurCarte.drawBehindWater(game.batch, textureAtlas);
		if(drawBubblesBehindWater && waterSplashSystem != null)
			waterSplashSystem.drawBubblesBehindWater(game.batch, textureAtlas, couleurs.getCouleurEau());
		lecteurCarte.drawWaterOccluders(game.batch, textureAtlas);
		game.batch.end();

		polyBatch.setProjectionMatrix(camera.combined);
		polyBatch.begin();
		lecteurCarte.drawPolygoneBehindWater(polyBatch, camera);
		lecteurCarte.drawPolygone(polyBatch, camera);
		polyBatch.end();
	}

	private boolean captureWaterRefractionScene(){
		if(waterRefractionRenderer == null || lecteurCarte == null || lecteurCarte.waters.size == 0)
			return false;
		if(!waterRefractionRenderer.beginCapture(couleurs.getCouleurFond()))
			return false;

		drawGameplaySceneWithoutWater(true);

		waterRefractionRenderer.endCapture();
		return true;
	}

	private void updateGameplayCameraViewport(int screenWidth, int screenHeight){
		float viewportWidth;
		float viewportHeight;
		if(usesMobileCameraViewport()){
			float aspect = (float)screenWidth / (float)screenHeight;
			if(screenWidth <= screenHeight){
				viewportWidth = MOBILE_CAMERA_SHORT_SIDE_WORLD;
				viewportHeight = MOBILE_CAMERA_SHORT_SIDE_WORLD / aspect;
			}
			else{
				viewportHeight = MOBILE_CAMERA_SHORT_SIDE_WORLD;
				viewportWidth = MOBILE_CAMERA_SHORT_SIDE_WORLD * aspect;
			}
		}
		else{
			viewportWidth = dimension * Variables.WORLD_TO_BOX;
			viewportHeight = viewportWidth * ((float)screenHeight / (float)screenWidth);
		}

		camera.setToOrtho(false, viewportWidth, viewportHeight);
		logCameraLayout(screenWidth, screenHeight);
	}

	private boolean usesMobileCameraViewport(){
		return Gdx.app.getType() == ApplicationType.Android || PlatformInfo.mobileBrowser;
	}

	private void logCameraLayout(int screenWidth, int screenHeight){
		float pixelsPerWorld = Math.min(screenWidth / camera.viewportWidth, screenHeight / camera.viewportHeight);
		String layoutLog = "game camera layout screen=" + screenWidth + "x" + screenHeight
				+ " viewport=" + camera.viewportWidth + "x" + camera.viewportHeight
				+ " pixelsPerWorld=" + pixelsPerWorld
				+ " mobile=" + usesMobileCameraViewport();
		if(!layoutLog.equals(lastCameraLayoutLog)){
			lastCameraLayoutLog = layoutLog;
			DebugConfig.log(layoutLog);
		}
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
	if(editorReturnScreen != null){
		gameCompleted = true;
		DebugConfig.log("editor test complete");
		Gdx.app.postRunnable(new Runnable() {
			@Override
			public void run() {
				if(game.getScreen() == GameScreen.this)
					returnToEditor();
			}
		});
	}
	else if(LevelProgression.hasNextLevel(Variables.niveauSelectione, Variables.nombreNiveaux))
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

	private void createWaterTuningOverlay(){
		waterTuningStage = new Stage(new ScreenViewport());
		createWaterTuningStyles();
		Table root = new Table();
		root.setFillParent(true);
		root.top().left().pad(8f);
		root.setTouchable(Touchable.childrenOnly);

		Table panel = new Table();
		panel.setTouchable(Touchable.childrenOnly);
		panel.setBackground(waterTuningPanelDrawable);
		panel.pad(8f);
		panel.defaults().pad(2f);

		Label title = waterTuningLabel("Water tuning", waterTuningTitleStyle);
		panel.add(title).colspan(4).left().growX();
		panel.row();

		waterTuningDensityLabel = addWaterTuningControl(panel, "Density", new WaterTuningAdjustment() {
			@Override
			public void adjust(float delta) {
				DebugConfig.setWaterBubbleDensityMultiplier(DebugConfig.waterBubbleDensityScale() + delta * 0.25f);
			}
		});
		waterTuningSizeLabel = addWaterTuningControl(panel, "Size", new WaterTuningAdjustment() {
			@Override
			public void adjust(float delta) {
				DebugConfig.setWaterBubbleSizeMultiplier(DebugConfig.waterBubbleSizeScale() + delta * 0.15f);
			}
		});
		waterTuningLifetimeLabel = addWaterTuningControl(panel, "Lifetime", new WaterTuningAdjustment() {
			@Override
			public void adjust(float delta) {
				DebugConfig.setWaterBubbleLifetimeMultiplier(DebugConfig.waterBubbleLifetimeScale() + delta * 0.15f);
			}
		});
		waterTuningFoamLabel = addWaterTuningControl(panel, "Foam", new WaterTuningAdjustment() {
			@Override
			public void adjust(float delta) {
				DebugConfig.setWaterFoamAmount(DebugConfig.waterFoamAmountScale() + delta * 0.25f);
			}
		});

		Label adaptiveLabel = waterTuningLabel("Adaptive", waterTuningLabelStyle);
		panel.add(adaptiveLabel).left().width(92f);
		waterTuningAdaptiveButton = waterTuningButton("On");
		waterTuningAdaptiveButton.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				DebugConfig.adaptiveBubbleThrottle = !DebugConfig.adaptiveBubbleThrottle;
				refreshWaterTuningOverlay();
			}
		});
		panel.add(waterTuningAdaptiveButton).height(WATER_TUNING_BUTTON_HEIGHT).colspan(3).growX();
		panel.row();

		waterTuningStatusLabel = waterTuningLabel("", waterTuningLabelStyle);
		panel.add(waterTuningStatusLabel).colspan(4).left().growX().padTop(5f);
		root.add(panel).width(292f).top().left();
		waterTuningStage.addActor(root);
		refreshWaterTuningOverlay();
		DebugConfig.log("water tuning overlay ready");
	}

	private void createWaterTuningStyles(){
		Pixmap pixmap = new Pixmap(4, 4, Pixmap.Format.RGBA8888);
		pixmap.setColor(Color.WHITE);
		pixmap.fill();
		waterTuningUiTexture = new Texture(pixmap);
		pixmap.dispose();
		TextureRegionDrawable white = new TextureRegionDrawable(new TextureRegion(waterTuningUiTexture));
		waterTuningPanelDrawable = white.tint(new Color(0.08f, 0.10f, 0.12f, 0.88f));
		Drawable buttonDrawable = white.tint(new Color(0.20f, 0.26f, 0.30f, 0.96f));
		Drawable buttonDownDrawable = white.tint(new Color(0.10f, 0.55f, 0.72f, 0.98f));

		BitmapFont font = game.assets.get("fontRestart.ttf", BitmapFont.class);
		waterTuningLabelStyle = new LabelStyle(font, new Color(0.90f, 0.96f, 1f, 1f));
		waterTuningTitleStyle = new LabelStyle(font, new Color(1f, 0.88f, 0.34f, 1f));
		waterTuningButtonStyle = new TextButton.TextButtonStyle();
		waterTuningButtonStyle.font = font;
		waterTuningButtonStyle.fontColor = Color.WHITE;
		waterTuningButtonStyle.downFontColor = Color.WHITE;
		waterTuningButtonStyle.up = buttonDrawable;
		waterTuningButtonStyle.down = buttonDownDrawable;
		waterTuningButtonStyle.checked = buttonDownDrawable;
	}

	private Label addWaterTuningControl(Table panel, String labelText, final WaterTuningAdjustment adjustment){
		Label valueLabel = waterTuningLabel("", waterTuningLabelStyle);
		panel.add(valueLabel).left().width(132f);
		TextButton minus = waterTuningButton("-");
		minus.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				adjustment.adjust(-1f);
				refreshWaterTuningOverlay();
			}
		});
		panel.add(minus).height(WATER_TUNING_BUTTON_HEIGHT).width(40f);
		TextButton plus = waterTuningButton("+");
		plus.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				adjustment.adjust(1f);
				refreshWaterTuningOverlay();
			}
		});
		panel.add(plus).height(WATER_TUNING_BUTTON_HEIGHT).width(40f);
		Label staticLabel = waterTuningLabel(labelText, waterTuningLabelStyle);
		panel.add(staticLabel).left().width(58f);
		panel.row();
		return valueLabel;
	}

	private Label waterTuningLabel(String text, LabelStyle style){
		Label label = new Label(text, style);
		label.setFontScale(WATER_TUNING_FONT_SCALE);
		label.setAlignment(Align.left);
		return label;
	}

	private TextButton waterTuningButton(String text){
		TextButton button = new TextButton(text, waterTuningButtonStyle);
		button.getLabel().setFontScale(WATER_TUNING_FONT_SCALE);
		return button;
	}

	private void drawWaterTuningOverlay(float delta){
		refreshWaterTuningOverlay();
		waterTuningStage.act(delta);
		waterTuningStage.draw();
	}

	private void refreshWaterTuningOverlay(){
		if(waterTuningStage == null)
			return;
		if(waterTuningDensityLabel != null)
			waterTuningDensityLabel.setText("Density " + decimal(DebugConfig.waterBubbleDensityScale()) + "x");
		if(waterTuningSizeLabel != null)
			waterTuningSizeLabel.setText("Size " + decimal(DebugConfig.waterBubbleSizeScale()) + "x");
		if(waterTuningLifetimeLabel != null)
			waterTuningLifetimeLabel.setText("Lifetime " + decimal(DebugConfig.waterBubbleLifetimeScale()) + "x");
		if(waterTuningFoamLabel != null)
			waterTuningFoamLabel.setText("Foam " + decimal(DebugConfig.waterFoamAmountScale()) + "x");
		if(waterTuningAdaptiveButton != null)
			waterTuningAdaptiveButton.setText(DebugConfig.adaptiveBubbleThrottle ? "On" : "Off");
		if(waterTuningStatusLabel != null && waterSplashSystem != null){
			waterTuningStatusLabel.setText("B " + waterSplashSystem.getRenderedBubbleCount()
					+ " F " + waterSplashSystem.getRenderedSurfaceFoamCount()
					+ " T " + percent(waterSplashSystem.getBubbleSpawnThrottle()));
		}
	}

	private static String decimal(float value){
		return Float.toString(MathUtils.round(value * 10f) / 10f);
	}

	private static String percent(float value){
		return Integer.toString(MathUtils.round(value * 100f)) + "%";
	}

	private interface WaterTuningAdjustment {
		void adjust(float delta);
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
			drawGameplay(false);
			return;
		}

		fbo.begin();
		Gdx.graphics.getGL20().glClearColor(couleurs.getCouleurFond().r,couleurs.getCouleurFond().g,couleurs.getCouleurFond().b,1);
		Gdx.graphics.getGL20().glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
		drawGameplay(false);
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

	private void updateDebugSpringLightProbe(float delta){
		if(!DebugConfig.springLightProbe || springLightSystem == null || lecteurCarte.springs.size == 0
				|| Variables.levelComplete || gameCompleted)
			return;
		debugSpringLightElapsed += delta;
		if(debugSpringLightElapsed >= SpringLightGeometry.FADE_SECONDS){
			debugSpringLightElapsed = 0f;
			Spring spring = lecteurCarte.springs.first();
			springLightSystem.activate(spring);
			DebugConfig.log("spring light probe pulse level=" + Variables.niveauSelectione
					+ " power=" + spring.getPowerX() + "," + spring.getPowerY());
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
		if(keycode == Keys.ESCAPE || keycode == Keys.BACK){
			if(editorReturnScreen != null)
				returnToEditor();
			else
				returnToMainMenu(gameCompleted);
			return true;
		}
		if(gameCompleted && keycode == Keys.SPACE){
			returnToMainMenu(true);
			return true;
		}
		return false;
	}

	@Override
	public boolean touchDown(int screenX, int screenY, int pointer, int button) {
		if(gameCompleted){
			returnToMainMenu(true);
			return true;
		}
		return false;
	}

	private void returnToMainMenu(boolean resetProgress){
		if(resetProgress)
			DebugConfig.log("return to main menu after game complete");
		else
			DebugConfig.log("return to main menu during gameplay level=" + Variables.niveauSelectione);
		DebugConfig.autoAdvanceLevels = false;
		if(resetProgress){
			Data.setLevel(1);
			Variables.niveauSelectione = 1;
		}
		Variables.levelComplete = false;
		Variables.restart = false;
		Variables.fallRestartDelay = 2.136f;
		game.setScreen(new MainMenuScreen(game));
		dispose();
	}

	private void returnToEditor(){
		DebugConfig.log("return to editor from test");
		Variables.levelComplete = false;
		Variables.restart = false;
		Variables.fallRestartDelay = 2.136f;
		if(editorReturnScreen instanceof LevelEditorScreen)
			((LevelEditorScreen)editorReturnScreen).returnFromPlaytest();
		game.setScreen(editorReturnScreen);
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
