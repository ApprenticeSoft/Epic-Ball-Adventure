package screen;

import editor.EditorFileBridge;
import editor.EditorLevel;
import editor.EditorLevelObject;
import editor.EditorObjectType;
import editor.EditorTiledMapFactory;
import editor.EditorTmxWriter;
import utils.DebugConfig;
import utils.Variables;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.DragListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.one.button.jam.MyGdxGame;

public class LevelEditorScreen extends InputAdapter implements Screen {
	private static final float LEFT_PANEL_WIDTH = 300f;
	private static final float RIGHT_PANEL_WIDTH = 230f;
	private static final float MIN_ZOOM = 0.12f;
	private static final float MAX_ZOOM = 4f;
	private static final float EDITOR_FONT_GENERATED_SIZE = 158f;
	private static final float EDITOR_FONT_PIXEL_SIZE = 15f;
	private static final float EDITOR_FONT_SCALE = EDITOR_FONT_PIXEL_SIZE / EDITOR_FONT_GENERATED_SIZE;
	private static final float TITLE_FONT_SCALE = 1.12f;
	private static final float BUTTON_HEIGHT = 28f;
	private static final float FIELD_HEIGHT = 28f;
	private static final int GRID_MINOR_STEP = EditorLevel.TILE_SIZE;
	private static final int GRID_MAJOR_STEP = EditorLevel.TILE_SIZE * 4;
	private static final Color COLOR_BACKGROUND = new Color(0.08f, 0.09f, 0.10f, 1f);
	private static final Color COLOR_WORLD_BACKGROUND = new Color(0.10f, 0.11f, 0.12f, 1f);
	private static final Color COLOR_LEVEL_BACKGROUND = new Color(0.13f, 0.14f, 0.15f, 1f);
	private static final Color COLOR_GRID_MINOR = new Color(0.20f, 0.22f, 0.24f, 0.50f);
	private static final Color COLOR_GRID_MAJOR = new Color(0.28f, 0.31f, 0.34f, 0.70f);
	private static final Color COLOR_LEVEL_BORDER = new Color(0.46f, 0.55f, 0.60f, 1f);
	private static final Color COLOR_SELECTED = new Color(1f, 0.88f, 0.18f, 1f);
	private static final Color COLOR_OUTLINE = new Color(0f, 0f, 0f, 0.65f);
	private static final Color COLOR_START = new Color(0.95f, 0.95f, 0.95f, 1f);
	private static final Color COLOR_EXIT = new Color(0.20f, 0.70f, 1f, 1f);
	private static final Color COLOR_WATER = new Color(0.16f, 0.45f, 0.82f, 0.68f);
	private static final Color COLOR_DYNAMIC = new Color(0.89f, 0.55f, 0.24f, 0.95f);
	private static final Color COLOR_SPRING = new Color(0.25f, 0.82f, 0.42f, 0.95f);
	private static final Color COLOR_POLYGON = new Color(0.64f, 0.63f, 0.67f, 0.95f);
	private static final Color COLOR_SOLID = new Color(0.72f, 0.74f, 0.78f, 0.95f);

	private final MyGdxGame game;
	private final EditorLevel level = new EditorLevel();
	private final OrthographicCamera uiCamera = new OrthographicCamera();
	private final OrthographicCamera worldCamera = new OrthographicCamera();
	private final ShapeRenderer shapes = new ShapeRenderer();
	private final Stage stage = new Stage(new ScreenViewport());
	private final Vector2 scratch = new Vector2();
	private final Vector2 scratch2 = new Vector2();
	private final Rectangle visibleWorldBounds = new Rectangle();

	private Texture uiTexture;
	private Drawable panelDrawable;
	private Drawable fieldDrawable;
	private Drawable buttonDrawable;
	private Drawable buttonDownDrawable;
	private Drawable cursorDrawable;
	private Drawable selectionDrawable;
	private Label.LabelStyle labelStyle;
	private Label.LabelStyle titleStyle;
	private TextButton.TextButtonStyle buttonStyle;
	private TextField.TextFieldStyle textFieldStyle;
	private BitmapFont editorFont;

	private Table leftTable;
	private Table rightTable;
	private Label statusLabel;
	private TextField fileNameField;
	private TextField widthField;
	private TextField heightField;

	private float cameraX;
	private float cameraY;
	private float zoom = 0.55f;
	private boolean panLeft;
	private boolean panRight;
	private boolean panUp;
	private boolean panDown;
	private boolean draggingObject;
	private float dragOffsetX;
	private float dragOffsetY;
	private EditorLevelObject selectedObject;
	private EditorLevelObject hoveredObject;
	private EditorObjectType activePaletteType;
	private String lastLayoutLog;
	private String lastCameraLog;

	public LevelEditorScreen(final MyGdxGame game){
		this.game = game;
		editorFont = createEditorFont();
		createStyles();
		buildUi();
		resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		DebugConfig.log("level editor opened");
	}

	@Override
	public void show() {
		Gdx.graphics.setContinuousRendering(true);
		Gdx.input.setInputProcessor(new InputMultiplexer(stage, this));
	}

	@Override
	public void render(float delta) {
		updateCamera(delta);
		updateHover();
		Gdx.gl.glClearColor(COLOR_BACKGROUND.r, COLOR_BACKGROUND.g, COLOR_BACKGROUND.b, COLOR_BACKGROUND.a);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		drawWorld();
		stage.act(delta);
		stage.draw();
	}

	@Override
	public void resize(int width, int height) {
		width = Math.max(1, width);
		height = Math.max(1, height);
		uiCamera.setToOrtho(false, width, height);
		uiCamera.update();
		stage.getViewport().update(width, height, true);
		updateWorldCamera();
		logLayout(width, height);
	}

	@Override
	public boolean keyDown(int keycode) {
		if(keycode == Keys.ESCAPE){
			game.setScreen(new MainMenuScreen(game));
			dispose();
			return true;
		}
		if(keycode == Keys.LEFT){
			panLeft = true;
			return true;
		}
		if(keycode == Keys.RIGHT){
			panRight = true;
			return true;
		}
		if(keycode == Keys.UP){
			panUp = true;
			return true;
		}
		if(keycode == Keys.DOWN){
			panDown = true;
			return true;
		}
		if(keycode == Keys.P){
			playLevel();
			return true;
		}
		if(keycode == Keys.DEL || keycode == Keys.FORWARD_DEL){
			deleteSelected();
			return true;
		}
		if(keycode == Keys.PLUS || keycode == Keys.EQUALS){
			zoomAtScreen(Gdx.input.getX(), Gdx.input.getY(), 1.1f);
			return true;
		}
		if(keycode == Keys.MINUS){
			zoomAtScreen(Gdx.input.getX(), Gdx.input.getY(), 1f / 1.1f);
			return true;
		}
		return false;
	}

	@Override
	public boolean keyUp(int keycode) {
		if(keycode == Keys.LEFT)
			panLeft = false;
		else if(keycode == Keys.RIGHT)
			panRight = false;
		else if(keycode == Keys.UP)
			panUp = false;
		else if(keycode == Keys.DOWN)
			panDown = false;
		else
			return false;
		return true;
	}

	@Override
	public boolean scrolled(float amountX, float amountY) {
		zoomAtScreen(Gdx.input.getX(), Gdx.input.getY(), amountY > 0 ? 1f / 1.12f : 1.12f);
		return true;
	}

	@Override
	public boolean touchDown(int screenX, int screenY, int pointer, int button) {
		if(!isWorldScreen(screenX, screenY))
			return false;
		Vector2 world = screenToWorld(screenX, screenY, scratch);
		if(activePaletteType != null){
			placeObject(activePaletteType, world.x, world.y);
			activePaletteType = null;
			return true;
		}
		EditorLevelObject previousSelection = selectedObject;
		selectedObject = level.findAt(world.x, world.y);
		draggingObject = selectedObject != null;
		if(selectedObject != null){
			dragOffsetX = world.x - selectedObject.x;
			dragOffsetY = world.y - selectedObject.y;
		}
		if(previousSelection != selectedObject)
			buildLeftPanel();
		return true;
	}

	@Override
	public boolean touchDragged(int screenX, int screenY, int pointer) {
		if(!draggingObject || selectedObject == null)
			return false;
		Vector2 world = screenToWorld(screenX, screenY, scratch);
		selectedObject.x = snap(world.x - dragOffsetX);
		selectedObject.y = snap(world.y - dragOffsetY);
		return true;
	}

	@Override
	public boolean touchUp(int screenX, int screenY, int pointer, int button) {
		if(draggingObject){
			draggingObject = false;
			buildLeftPanel();
			logCamera();
			return true;
		}
		return false;
	}

	@Override
	public boolean mouseMoved(int screenX, int screenY) {
		EditorLevelObject previousHover = hoveredObject;
		updateHover(screenX, screenY);
		return previousHover != hoveredObject;
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
		shapes.dispose();
		if(editorFont != null)
			editorFont.dispose();
		if(uiTexture != null)
			uiTexture.dispose();
	}

	private BitmapFont createEditorFont(){
		BitmapFont font = new BitmapFont(Gdx.files.internal("Fonts/web_font1_hd.fnt"), false);
		font.getData().setScale(EDITOR_FONT_SCALE);
		font.setUseIntegerPositions(false);
		for(TextureRegion region : font.getRegions())
			region.getTexture().setFilter(TextureFilter.Linear, TextureFilter.Linear);
		return font;
	}

	private void createStyles(){
		Pixmap pixmap = new Pixmap(4, 4, Pixmap.Format.RGBA8888);
		pixmap.setColor(Color.WHITE);
		pixmap.fill();
		uiTexture = new Texture(pixmap);
		pixmap.dispose();
		TextureRegionDrawable white = new TextureRegionDrawable(new TextureRegion(uiTexture));
		panelDrawable = tint(white, new Color(0.13f, 0.15f, 0.17f, 0.96f));
		fieldDrawable = tint(white, new Color(0.07f, 0.08f, 0.09f, 1f));
		buttonDrawable = tint(white, new Color(0.22f, 0.27f, 0.31f, 1f));
		buttonDownDrawable = tint(white, new Color(0.13f, 0.55f, 0.72f, 1f));
		cursorDrawable = tint(white, new Color(0.95f, 0.95f, 0.95f, 1f));
		selectionDrawable = tint(white, new Color(0.13f, 0.55f, 0.72f, 0.65f));

		labelStyle = new Label.LabelStyle(editorFont, new Color(0.90f, 0.93f, 0.95f, 1f));
		titleStyle = new Label.LabelStyle(editorFont, new Color(1f, 0.86f, 0.22f, 1f));

		buttonStyle = new TextButton.TextButtonStyle();
		buttonStyle.font = editorFont;
		buttonStyle.fontColor = Color.WHITE;
		buttonStyle.downFontColor = Color.WHITE;
		buttonStyle.up = buttonDrawable;
		buttonStyle.down = buttonDownDrawable;
		buttonStyle.checked = buttonDownDrawable;

		textFieldStyle = new TextField.TextFieldStyle();
		textFieldStyle.font = editorFont;
		textFieldStyle.fontColor = Color.WHITE;
		textFieldStyle.cursor = cursorDrawable;
		textFieldStyle.selection = selectionDrawable;
		textFieldStyle.background = fieldDrawable;
	}

	private Drawable tint(TextureRegionDrawable drawable, Color color){
		return drawable.tint(color);
	}

	private void buildUi(){
		Table root = new Table();
		root.setFillParent(true);
		root.setTouchable(Touchable.childrenOnly);
		stage.addActor(root);

		leftTable = new Table();
		leftTable.setBackground(panelDrawable);
		leftTable.top();
		ScrollPane leftScroll = new ScrollPane(leftTable);
		leftScroll.setFadeScrollBars(false);

		rightTable = new Table();
		rightTable.setBackground(panelDrawable);
		rightTable.top();
		ScrollPane rightScroll = new ScrollPane(rightTable);
		rightScroll.setFadeScrollBars(false);

		root.add(leftScroll).width(LEFT_PANEL_WIDTH).growY();
		root.add().grow();
		root.add(rightScroll).width(RIGHT_PANEL_WIDTH).growY();

		buildLeftPanel();
		buildRightPanel();
	}

	private void buildLeftPanel(){
		if(leftTable == null)
			return;
		leftTable.clear();
		leftTable.defaults().pad(2f).left().growX();
		addTitle(leftTable, "LEVEL");
		fileNameField = addTextField(leftTable, "File", level.fileName, new TextSetter() {
			@Override
			public void set(String value) {
				level.fileName = value;
			}
		});
		widthField = addIntegerField(leftTable, "Width tiles", level.widthTiles, new IntSetter() {
			@Override
			public void set(int value) {
				level.widthTiles = Math.max(1, value);
			}
		});
		heightField = addIntegerField(leftTable, "Height tiles", level.heightTiles, new IntSetter() {
			@Override
			public void set(int value) {
				level.heightTiles = Math.max(1, value);
			}
		});
		TextButton saveButton = addButton(leftTable, "Save");
		saveButton.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				saveLevel();
			}
		});
		TextButton playButton = addButton(leftTable, "Play");
		playButton.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				playLevel();
			}
		});

		leftTable.row().padTop(10);
		statusLabel = new Label("", labelStyle);
		statusLabel.setWrap(true);
		leftTable.add(statusLabel).growX();

		if(selectedObject != null){
			leftTable.row().padTop(12);
			addTitle(leftTable, selectedObject.type.label.toUpperCase());
			addNumberField(leftTable, "X", selectedObject.x, new NumberSetter() {
				@Override
				public void set(float value) {
					selectedObject.x = value;
				}
			});
			addNumberField(leftTable, "Y", selectedObject.y, new NumberSetter() {
				@Override
				public void set(float value) {
					selectedObject.y = value;
				}
			});
			if(selectedObject.type != EditorObjectType.PLATFORM){
				addNumberField(leftTable, "Width", selectedObject.width, new NumberSetter() {
					@Override
					public void set(float value) {
						selectedObject.width = Math.max(1f, value);
					}
				});
				addNumberField(leftTable, "Height", selectedObject.height, new NumberSetter() {
					@Override
					public void set(float value) {
						selectedObject.height = Math.max(1f, value);
					}
				});
			}
			else{
				addNumberField(leftTable, "End dx", selectedObject.width, new NumberSetter() {
					@Override
					public void set(float value) {
						selectedObject.width = value;
					}
				});
				addNumberField(leftTable, "End dy", selectedObject.height, new NumberSetter() {
					@Override
					public void set(float value) {
						selectedObject.height = value;
					}
				});
			}
			addNumberField(leftTable, "Rotation", selectedObject.rotation, new NumberSetter() {
				@Override
				public void set(float value) {
					selectedObject.rotation = value;
				}
			});
			String[] propertyNames = propertyNames(selectedObject.type);
			for(final String propertyName : propertyNames){
				if(selectedObject.properties.get(propertyName) == null)
					selectedObject.properties.put(propertyName, defaultPropertyValue(propertyName));
				addTextField(leftTable, propertyName, selectedObject.properties.get(propertyName), new TextSetter() {
					@Override
					public void set(String value) {
						selectedObject.properties.put(propertyName, value);
					}
				});
			}
			TextButton duplicateButton = addButton(leftTable, "Duplicate");
			duplicateButton.addListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent event, Actor actor) {
					EditorLevelObject copy = selectedObject.copy();
					level.objects.add(copy);
					selectedObject = copy;
					buildLeftPanel();
				}
			});
			TextButton deleteButton = addButton(leftTable, "Delete");
			deleteButton.addListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent event, Actor actor) {
					deleteSelected();
				}
			});
		}
	}

	private void buildRightPanel(){
		rightTable.clear();
		rightTable.defaults().pad(2f).left().growX();
		addTitle(rightTable, "OBJECTS");
		addPaletteButton(EditorObjectType.START, "Start");
		addPaletteButton(EditorObjectType.SOLID, "Solid");
		addPaletteButton(EditorObjectType.POLYGON, "Polygon");
		addPaletteButton(EditorObjectType.LIGHT, "Light");
		addPaletteButton(EditorObjectType.REVOLVING, "Revolving");
		addPaletteButton(EditorObjectType.SWING, "Swing");
		addPaletteButton(EditorObjectType.BALANCOIRE, "Balancoire");
		addPaletteButton(EditorObjectType.SUSPENDU, "Suspendu");
		addPaletteButton(EditorObjectType.POULIE, "Poulie Pair");
		addPaletteButton(EditorObjectType.WATER, "Water");
		addPaletteButton(EditorObjectType.SPRING, "Spring");
		addPaletteButton(EditorObjectType.EXIT, "Exit");
		addPaletteButton(EditorObjectType.PLATFORM, "Moving platform");
	}

	private void addPaletteButton(final EditorObjectType type, String label){
		TextButton button = addButton(rightTable, label);
		button.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				activePaletteType = type;
				setStatus(type.label + " selected");
			}
		});
		button.addListener(new DragListener() {
			@Override
			public void dragStart(InputEvent event, float x, float y, int pointer) {
				activePaletteType = type;
			}

			@Override
			public void dragStop(InputEvent event, float x, float y, int pointer) {
				Vector2 stagePosition = scratch2.set(x, y);
				event.getListenerActor().localToStageCoordinates(stagePosition);
				if(placeFromStage(stagePosition.x, stagePosition.y))
					activePaletteType = null;
			}
		});
	}

	private void addTitle(Table table, String text){
		Label label = new Label(text, titleStyle);
		label.setFontScale(EDITOR_FONT_SCALE * TITLE_FONT_SCALE);
		label.setAlignment(Align.left);
		table.add(label).height(30f).growX();
		table.row();
	}

	private TextButton addButton(Table table, String text){
		TextButton button = new TextButton(text, buttonStyle);
		button.getLabel().setFontScale(EDITOR_FONT_SCALE);
		button.getLabel().setEllipsis(true);
		table.add(button).height(BUTTON_HEIGHT).growX();
		table.row();
		return button;
	}

	private TextField addTextField(Table table, String labelText, String value, final TextSetter setter){
		Label label = new Label(labelText, labelStyle);
		label.setFontScale(EDITOR_FONT_SCALE * 0.92f);
		table.add(label).height(20f).growX();
		table.row();
		final TextField field = new TextField(value == null ? "" : value, textFieldStyle);
		field.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				setter.set(field.getText());
			}
		});
		table.add(field).height(FIELD_HEIGHT).growX();
		table.row();
		return field;
	}

	private TextField addIntegerField(Table table, String labelText, int value, final IntSetter setter){
		return addTextField(table, labelText, String.valueOf(value), new TextSetter() {
			@Override
			public void set(String value) {
				try{
					setter.set(Integer.parseInt(value));
				}
				catch(NumberFormatException ignored){
				}
			}
		});
	}

	private TextField addNumberField(Table table, String labelText, float value, final NumberSetter setter){
		return addTextField(table, labelText, number(value), new TextSetter() {
			@Override
			public void set(String value) {
				try{
					setter.set(Float.parseFloat(value));
				}
				catch(NumberFormatException ignored){
				}
			}
		});
	}

	private void saveLevel(){
		try{
			String xml = EditorTmxWriter.write(level);
			EditorFileBridge.saveText(level.fileName, xml);
			setStatus("Saved " + level.fileName);
			DebugConfig.log("level editor saved file=" + level.fileName + " objects=" + level.objects.size);
		}
		catch(RuntimeException exception){
			setStatus("Save failed");
			DebugConfig.log("level editor save failed " + exception);
		}
	}

	private void playLevel(){
		if(level.getStart() == null){
			setStatus("Missing Start");
			return;
		}
		if(level.getExit() == null){
			setStatus("Missing Exit");
			return;
		}
		Variables.levelComplete = false;
		Variables.restart = false;
		Variables.fallRestartDelay = 2.136f;
		DebugConfig.log("level editor playtest start objects=" + level.objects.size);
		TiledMap map = EditorTiledMapFactory.build(level);
		game.setScreen(new GameScreen(game, map, this));
	}

	private boolean placeFromStage(float stageX, float stageY){
		int screenX = Math.round(stageX);
		int screenY = Math.round(Gdx.graphics.getHeight() - stageY);
		if(!isWorldScreen(screenX, screenY))
			return false;
		Vector2 world = screenToWorld(screenX, screenY, scratch);
		placeObject(activePaletteType, world.x, world.y);
		return true;
	}

	private void placeObject(EditorObjectType type, float worldX, float worldY){
		if(type == null)
			return;
		if(type == EditorObjectType.POULIE){
			selectedObject = level.createPulleyPair(worldX, worldY).first();
		}
		else{
			selectedObject = level.createObject(type, worldX, worldY);
		}
		buildLeftPanel();
		setStatus(selectedObject.type.label + " added");
	}

	private void deleteSelected(){
		if(selectedObject == null)
			return;
		level.remove(selectedObject);
		selectedObject = null;
		buildLeftPanel();
		setStatus("Deleted");
	}

	private void setStatus(String value){
		if(statusLabel != null)
			statusLabel.setText(value);
	}

	private void updateCamera(float delta){
		boolean cameraMoved = panLeft || panRight || panUp || panDown;
		float speed = 900f * delta / zoom;
		if(panLeft)
			cameraX -= speed;
		if(panRight)
			cameraX += speed;
		if(panUp)
			cameraY += speed;
		if(panDown)
			cameraY -= speed;
		updateWorldCamera();
		if(cameraMoved)
			logCamera();
	}

	private void zoomAtScreen(int screenX, int screenY, float factor){
		Vector2 before = screenToWorld(screenX, screenY, scratch);
		zoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, zoom * factor));
		updateWorldCamera();
		Vector2 after = screenToWorld(screenX, screenY, scratch2);
		cameraX += before.x - after.x;
		cameraY += before.y - after.y;
		updateWorldCamera();
		logCamera();
	}

	private void drawWorld(){
		int viewportX = Math.round(LEFT_PANEL_WIDTH);
		int viewportY = 0;
		int viewportWidth = Math.max(1, Math.round(getWorldScreenWidth()));
		int viewportHeight = Math.max(1, Gdx.graphics.getHeight());

		shapes.setProjectionMatrix(uiCamera.combined);
		Gdx.gl.glEnable(GL20.GL_BLEND);
		shapes.begin(ShapeRenderer.ShapeType.Filled);
		shapes.setColor(COLOR_WORLD_BACKGROUND);
		shapes.rect(LEFT_PANEL_WIDTH, 0, viewportWidth, viewportHeight);
		shapes.end();

		Gdx.gl.glViewport(viewportX, viewportY, viewportWidth, viewportHeight);
		shapes.setProjectionMatrix(worldCamera.combined);
		shapes.begin(ShapeRenderer.ShapeType.Filled);
		shapes.setColor(COLOR_LEVEL_BACKGROUND);
		shapes.rect(0, 0, level.getPixelWidth(), level.getPixelHeight());
		for(EditorLevelObject object : level.objects)
			if(isVisible(object))
				drawObjectFilled(object);
		drawPlacementGhost();
		shapes.end();

		shapes.begin(ShapeRenderer.ShapeType.Line);
		drawGrid();
		shapes.setColor(COLOR_LEVEL_BORDER);
		shapes.rect(0, 0, level.getPixelWidth(), level.getPixelHeight());
		for(EditorLevelObject object : level.objects)
			if(isVisible(object))
				drawObjectOutline(object, object == selectedObject);
		shapes.end();
		Gdx.gl.glDisable(GL20.GL_BLEND);

		Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		drawObjectLabels();
	}

	private void drawGrid(){
		float step = GRID_MINOR_STEP * zoom;
		if(step < 5f)
			return;
		float startX = (float)Math.floor(visibleWorldBounds.x / GRID_MINOR_STEP) * GRID_MINOR_STEP;
		float endX = visibleWorldBounds.x + visibleWorldBounds.width;
		float startY = (float)Math.floor(visibleWorldBounds.y / GRID_MINOR_STEP) * GRID_MINOR_STEP;
		float endY = visibleWorldBounds.y + visibleWorldBounds.height;
		for(float wx = startX; wx <= endX; wx += GRID_MINOR_STEP){
			shapes.setColor(((int)wx) % GRID_MAJOR_STEP == 0 ? COLOR_GRID_MAJOR : COLOR_GRID_MINOR);
			shapes.line(wx, visibleWorldBounds.y, wx, endY);
		}
		for(float wy = startY; wy <= endY; wy += GRID_MINOR_STEP){
			shapes.setColor(((int)wy) % GRID_MAJOR_STEP == 0 ? COLOR_GRID_MAJOR : COLOR_GRID_MINOR);
			shapes.line(visibleWorldBounds.x, wy, endX, wy);
		}
	}

	private void drawObjectFilled(EditorLevelObject object){
		Color color = objectColor(object.type);
		shapes.setColor(color);
		if(object.type == EditorObjectType.START){
			shapes.circle(object.x + object.width / 2f, object.y + object.height / 2f, object.width / 2f, 20);
		}
		else if(object.type == EditorObjectType.POLYGON){
			shapes.triangle(object.x, object.y + object.height, object.x + object.width, object.y + object.height,
					object.x + object.width, object.y);
		}
		else if(object.type == EditorObjectType.PLATFORM){
			shapes.rectLine(object.x, object.y, object.x + object.width, object.y + object.height, 16f);
		}
		else{
			shapes.rect(object.x, object.y, object.width, object.height);
		}
	}

	private void drawObjectOutline(EditorLevelObject object, boolean selected){
		if(selected)
			shapes.setColor(COLOR_SELECTED);
		else
			shapes.setColor(COLOR_OUTLINE);
		if(object.type == EditorObjectType.START){
			shapes.circle(object.x + object.width / 2f, object.y + object.height / 2f, object.width / 2f, 20);
		}
		else if(object.type == EditorObjectType.POLYGON){
			shapes.line(object.x, object.y + object.height, object.x + object.width, object.y + object.height);
			shapes.line(object.x + object.width, object.y + object.height, object.x + object.width, object.y);
			shapes.line(object.x + object.width, object.y, object.x, object.y + object.height);
		}
		else if(object.type == EditorObjectType.PLATFORM){
			shapes.line(object.x, object.y, object.x + object.width, object.y + object.height);
		}
		else{
			shapes.rect(object.x, object.y, object.width, object.height);
		}
	}

	private void drawPlacementGhost(){
		if(activePaletteType == null || !isWorldScreen(Gdx.input.getX(), Gdx.input.getY()))
			return;
		Vector2 world = screenToWorld(Gdx.input.getX(), Gdx.input.getY(), scratch);
		shapes.setColor(1f, 1f, 1f, 0.22f);
		shapes.circle(world.x, world.y, 20f / Math.max(zoom, 0.01f), 20);
	}

	private void drawObjectLabels(){
		EditorLevelObject labelObject = selectedObject != null ? selectedObject : hoveredObject;
		if(labelObject == null || !isVisible(labelObject))
			return;
		game.batch.setProjectionMatrix(uiCamera.combined);
		game.batch.begin();
		editorFont.setColor(0.95f, 0.97f, 1f, 0.95f);
		editorFont.draw(game.batch, labelObject.type.label,
				LEFT_PANEL_WIDTH + (labelObject.x - cameraX) * zoom + 4f,
				(labelObject.y + labelObject.height - cameraY) * zoom + 18f);
		editorFont.setColor(Color.WHITE);
		game.batch.end();
	}

	private Color objectColor(EditorObjectType type){
		if(type == EditorObjectType.START)
			return COLOR_START;
		if(type == EditorObjectType.EXIT)
			return COLOR_EXIT;
		if(type == EditorObjectType.WATER)
			return COLOR_WATER;
		if(type == EditorObjectType.LIGHT || type == EditorObjectType.REVOLVING || type == EditorObjectType.SWING
				|| type == EditorObjectType.BALANCOIRE || type == EditorObjectType.SUSPENDU || type == EditorObjectType.POULIE
				|| type == EditorObjectType.PLATFORM)
			return COLOR_DYNAMIC;
		if(type == EditorObjectType.SPRING)
			return COLOR_SPRING;
		if(type == EditorObjectType.POLYGON)
			return COLOR_POLYGON;
		return COLOR_SOLID;
	}

	private boolean isWorldScreen(int screenX, int screenY){
		return screenX >= LEFT_PANEL_WIDTH && screenX <= Gdx.graphics.getWidth() - RIGHT_PANEL_WIDTH
				&& screenY >= 0 && screenY <= Gdx.graphics.getHeight();
	}

	private Vector2 screenToWorld(int screenX, int screenY, Vector2 out){
		float stageY = Gdx.graphics.getHeight() - screenY;
		return out.set(cameraX + (screenX - LEFT_PANEL_WIDTH) / zoom, cameraY + stageY / zoom);
	}

	private void updateHover(){
		updateHover(Gdx.input.getX(), Gdx.input.getY());
	}

	private void updateHover(int screenX, int screenY){
		if(!isWorldScreen(screenX, screenY)){
			hoveredObject = null;
			return;
		}
		Vector2 world = screenToWorld(screenX, screenY, scratch);
		hoveredObject = level.findAt(world.x, world.y);
	}

	private void updateWorldCamera(){
		float viewportWidth = getWorldScreenWidth() / Math.max(zoom, 0.01f);
		float viewportHeight = Math.max(1f, Gdx.graphics.getHeight()) / Math.max(zoom, 0.01f);
		worldCamera.setToOrtho(false, viewportWidth, viewportHeight);
		worldCamera.position.set(cameraX + viewportWidth / 2f, cameraY + viewportHeight / 2f, 0f);
		worldCamera.update();
		visibleWorldBounds.set(cameraX, cameraY, viewportWidth, viewportHeight);
	}

	private float getWorldScreenWidth(){
		return Math.max(1f, Gdx.graphics.getWidth() - LEFT_PANEL_WIDTH - RIGHT_PANEL_WIDTH);
	}

	private boolean isVisible(EditorLevelObject object){
		float minX = Math.min(object.x, object.x + object.width);
		float maxX = Math.max(object.x, object.x + object.width);
		float minY = Math.min(object.y, object.y + object.height);
		float maxY = Math.max(object.y, object.y + object.height);
		if(object.type == EditorObjectType.PLATFORM){
			minX -= 32f;
			minY -= 32f;
			maxX += 32f;
			maxY += 32f;
		}
		return maxX >= visibleWorldBounds.x && minX <= visibleWorldBounds.x + visibleWorldBounds.width
				&& maxY >= visibleWorldBounds.y && minY <= visibleWorldBounds.y + visibleWorldBounds.height;
	}

	private void logLayout(int width, int height){
		String message = "level editor layout screen=" + width + "x" + height + " panels="
				+ Math.round(LEFT_PANEL_WIDTH) + "," + Math.round(RIGHT_PANEL_WIDTH)
				+ " buttonHeight=" + BUTTON_HEIGHT + " fieldHeight=" + FIELD_HEIGHT
				+ " fontScale=" + number(EDITOR_FONT_SCALE)
				+ " worldViewport=" + Math.round(getWorldScreenWidth()) + "x" + height;
		if(message.equals(lastLayoutLog))
			return;
		lastLayoutLog = message;
		DebugConfig.log(message);
	}

	private void logCamera(){
		String message = "level editor camera x=" + number(cameraX) + " y=" + number(cameraY)
				+ " zoom=" + number(zoom) + " viewport="
				+ number(visibleWorldBounds.width) + "x" + number(visibleWorldBounds.height);
		if(message.equals(lastCameraLog))
			return;
		lastCameraLog = message;
		DebugConfig.log(message);
	}

	private String[] propertyNames(EditorObjectType type){
		if(type == EditorObjectType.LIGHT)
			return new String[]{"Weight"};
		if(type == EditorObjectType.REVOLVING)
			return new String[]{"Speed"};
		if(type == EditorObjectType.SWING)
			return new String[]{"Position", "Weight", "Speed", "Torque", "angleRef", "angleMin", "angleMax", "Contact"};
		if(type == EditorObjectType.BALANCOIRE)
			return new String[]{"AttacheY", "Weight"};
		if(type == EditorObjectType.SUSPENDU)
			return new String[]{"Length", "Position", "Weight"};
		if(type == EditorObjectType.POULIE)
			return new String[]{"Groupe", "Masse", "longueur"};
		if(type == EditorObjectType.SPRING)
			return new String[]{"PowerX", "PowerY"};
		if(type == EditorObjectType.PLATFORM)
			return new String[]{"Speed", "Width", "Loop"};
		return new String[0];
	}

	private String defaultPropertyValue(String name){
		if("Weight".equals(name))
			return "5";
		if("Speed".equals(name))
			return "5";
		if("Torque".equals(name))
			return "1";
		if("Position".equals(name))
			return "0";
		if("AttacheY".equals(name) || "Length".equals(name) || "longueur".equals(name))
			return "5";
		if("Masse".equals(name))
			return "50";
		if("Groupe".equals(name))
			return "1";
		if("PowerY".equals(name))
			return "60";
		if("Loop".equals(name))
			return "false";
		return "0";
	}

	private float snap(float value){
		return Math.round(value / EditorLevel.TILE_SIZE) * EditorLevel.TILE_SIZE;
	}

	private String number(float value){
		if(Math.abs(value - Math.round(value)) < 0.0001f)
			return String.valueOf(Math.round(value));
		return String.valueOf(value);
	}

	private interface TextSetter {
		void set(String value);
	}

	private interface NumberSetter {
		void set(float value);
	}

	private interface IntSetter {
		void set(int value);
	}
}
