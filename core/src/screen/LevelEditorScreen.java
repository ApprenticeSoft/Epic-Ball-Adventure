package screen;

import editor.EditorFileBridge;
import editor.EditorLevel;
import editor.EditorLevelObject;
import editor.EditorLevelObject.SnapMode;
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
import com.badlogic.gdx.math.MathUtils;
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
import com.one.button.jam.Couleurs;
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
	private static final float SCREEN_MARGIN = 6f;
	private static final float PANEL_INNER_PADDING = 8f;
	private static final float HANDLE_SCREEN_RADIUS = 7f;
	private static final float EDGE_SCREEN_TOLERANCE = 9f;
	private static final float MIN_OBJECT_SIZE = 16f;
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
	private static final float OBJECT_ALPHA = 0.62f;
	private static final float START_ALPHA = 0.70f;
	private static final float EXIT_ALPHA = 0.72f;

	private final MyGdxGame game;
	private final EditorLevel level = new EditorLevel();
	private final Couleurs editorColors = new Couleurs(4);
	private final OrthographicCamera uiCamera = new OrthographicCamera();
	private final OrthographicCamera worldCamera = new OrthographicCamera();
	private final ShapeRenderer shapes = new ShapeRenderer();
	private final Stage stage = new Stage(new ScreenViewport());
	private final Vector2 scratch = new Vector2();
	private final Vector2 scratch2 = new Vector2();
	private final Vector2 rectBottomLeft = new Vector2();
	private final Vector2 rectBottomRight = new Vector2();
	private final Vector2 rectTopRight = new Vector2();
	private final Vector2 rectTopLeft = new Vector2();
	private final Rectangle visibleWorldBounds = new Rectangle();
	private final Color objectColorScratch = new Color();

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

	private enum DragMode {
		NONE,
		MOVE,
		RESIZE_RECT,
		DRAG_POINT
	}

	private float cameraX;
	private float cameraY;
	private float zoom = 0.55f;
	private boolean panLeft;
	private boolean panRight;
	private boolean panUp;
	private boolean panDown;
	private DragMode dragMode = DragMode.NONE;
	private float dragOffsetX;
	private float dragOffsetY;
	private int resizeHorizontal;
	private int resizeVertical;
	private int draggedPointIndex = -1;
	private EditorLevelObject selectedObject;
	private EditorLevelObject hoveredObject;
	private EditorObjectType activePaletteType;
	private String lastLayoutLog;
	private String lastCameraLog;
	private boolean hoverDirty = true;
	private int lastHoverScreenX = -1;
	private int lastHoverScreenY = -1;
	private float lastHoverCameraX;
	private float lastHoverCameraY;
	private float lastHoverZoom;

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
		updateHoverIfNeeded();
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
		markHoverDirty();
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
		dragMode = DragMode.NONE;
		draggedPointIndex = -1;
		if(activePaletteType != null){
			placeObject(activePaletteType, world.x, world.y);
			activePaletteType = null;
			markHoverDirty();
			return true;
		}
		if(selectedObject != null){
			int pointIndex = hitPointHandle(selectedObject, world.x, world.y);
			if(pointIndex >= 0){
				dragMode = DragMode.DRAG_POINT;
				draggedPointIndex = pointIndex;
				return true;
			}
			if(hitResizeHandle(selectedObject, world.x, world.y)){
				dragMode = DragMode.RESIZE_RECT;
				return true;
			}
		}
		EditorLevelObject previousSelection = selectedObject;
		selectedObject = level.findAt(world.x, world.y);
		if(selectedObject != null){
			dragMode = DragMode.MOVE;
			dragOffsetX = world.x - selectedObject.x;
			dragOffsetY = world.y - selectedObject.y;
		}
		if(previousSelection != selectedObject)
			buildLeftPanel();
		markHoverDirty();
		return true;
	}

	@Override
	public boolean touchDragged(int screenX, int screenY, int pointer) {
		if(dragMode == DragMode.NONE || selectedObject == null)
			return false;
		Vector2 world = screenToWorld(screenX, screenY, scratch);
		if(dragMode == DragMode.MOVE){
			selectedObject.x = snapForSelected(world.x - dragOffsetX);
			selectedObject.y = snapForSelected(world.y - dragOffsetY);
		}
		else if(dragMode == DragMode.RESIZE_RECT){
			resizeSelectedObject(world.x, world.y);
		}
		else if(dragMode == DragMode.DRAG_POINT){
			selectedObject.setPointWorldPosition(draggedPointIndex, snapForSelected(world.x), snapForSelected(world.y));
		}
		markHoverDirty();
		return true;
	}

	@Override
	public boolean touchUp(int screenX, int screenY, int pointer, int button) {
		if(dragMode != DragMode.NONE){
			dragMode = DragMode.NONE;
			draggedPointIndex = -1;
			resizeHorizontal = 0;
			resizeVertical = 0;
			buildLeftPanel();
			updateHover(screenX, screenY);
			logCamera();
			return true;
		}
		return false;
	}

	@Override
	public boolean mouseMoved(int screenX, int screenY) {
		EditorLevelObject previousHover = hoveredObject;
		if(dragMode == DragMode.NONE)
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
		root.pad(SCREEN_MARGIN);
		stage.addActor(root);

		leftTable = new Table();
		leftTable.setBackground(panelDrawable);
		leftTable.pad(PANEL_INNER_PADDING);
		leftTable.top();
		ScrollPane leftScroll = new ScrollPane(leftTable);
		leftScroll.setFadeScrollBars(false);

		rightTable = new Table();
		rightTable.setBackground(panelDrawable);
		rightTable.pad(PANEL_INNER_PADDING);
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
		leftTable.defaults().pad(3f).left().growX();
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
			addSnapButton(leftTable);
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
			if(selectedObject.type == EditorObjectType.POLYGON){
				selectedObject.ensureDefaultPoints();
				addIntegerField(leftTable, "Edges", selectedObject.points.size, new IntSetter() {
					@Override
					public void set(int value) {
						selectedObject.setPolygonVertexCount(value);
					}
				});
				addNumberField(leftTable, "Width", selectedObject.width, new NumberSetter() {
					@Override
					public void set(float value) {
						selectedObject.scalePointBounds(Math.max(1f, value), selectedObject.height);
					}
				});
				addNumberField(leftTable, "Height", selectedObject.height, new NumberSetter() {
					@Override
					public void set(float value) {
						selectedObject.scalePointBounds(selectedObject.width, Math.max(1f, value));
					}
				});
			}
			else if(selectedObject.type == EditorObjectType.PLATFORM){
				selectedObject.ensureDefaultPoints();
				addIntegerField(leftTable, "Path points", selectedObject.points.size, new IntSetter() {
					@Override
					public void set(int value) {
						selectedObject.setPlatformPointCount(value);
					}
				});
				addNumberField(leftTable, "End dx", selectedObject.points.peek().x, new NumberSetter() {
					@Override
					public void set(float value) {
						selectedObject.setPlatformEnd(value, selectedObject.points.peek().y);
					}
				});
				addNumberField(leftTable, "End dy", selectedObject.points.peek().y, new NumberSetter() {
					@Override
					public void set(float value) {
						selectedObject.setPlatformEnd(selectedObject.points.peek().x, value);
					}
				});
			}
			else if(selectedObject.type != EditorObjectType.START && selectedObject.type != EditorObjectType.EXIT){
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
			addNumberField(leftTable, "Rotation", selectedObject.rotation, new NumberSetter() {
				@Override
				public void set(float value) {
					selectedObject.rotation = value;
					selectedObject.snapMode = SnapMode.FREE;
					markHoverDirty();
				}
			});
			String[] propertyNames = propertyNames(selectedObject.type);
			for(final String propertyName : propertyNames){
				addTextField(leftTable, propertyName, selectedObject.properties.get(propertyName), new TextSetter() {
					@Override
					public void set(String value) {
						if(value == null || value.trim().length() == 0)
							selectedObject.properties.remove(propertyName);
						else
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
		rightTable.defaults().pad(3f).left().growX();
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

	private void addSnapButton(Table table){
		final TextButton snapButton = addButton(table, snapLabel(selectedObject));
		snapButton.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				if(selectedObject == null)
					return;
				selectedObject.snapMode = selectedObject.snapMode == SnapMode.GRID ? SnapMode.FREE : SnapMode.GRID;
				setStatus(selectedObject.snapMode == SnapMode.GRID ? "Snap to grid" : "Free positioning");
				buildLeftPanel();
			}
		});
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
		float placedX = snap(worldX);
		float placedY = snap(worldY);
		if(type == EditorObjectType.POULIE){
			selectedObject = level.createPulleyPair(placedX, placedY).first();
		}
		else{
			selectedObject = level.createObject(type, placedX, placedY);
		}
		buildLeftPanel();
		markHoverDirty();
		setStatus(selectedObject.type.label + " added");
	}

	private void deleteSelected(){
		if(selectedObject == null)
			return;
		level.remove(selectedObject);
		selectedObject = null;
		buildLeftPanel();
		markHoverDirty();
		setStatus("Deleted");
	}

	private void setStatus(String value){
		if(statusLabel != null)
			statusLabel.setText(value);
	}

	private void updateCamera(float delta){
		boolean cameraMoved = panLeft || panRight || panUp || panDown;
		if(!cameraMoved)
			return;
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
		markHoverDirty();
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
		markHoverDirty();
		logCamera();
	}

	private void drawWorld(){
		int viewportX = Math.round(getWorldScreenLeft());
		int viewportY = Math.round(getWorldScreenBottom());
		int viewportWidth = Math.max(1, Math.round(getWorldScreenWidth()));
		int viewportHeight = Math.max(1, Math.round(getWorldScreenHeight()));

		shapes.setProjectionMatrix(uiCamera.combined);
		Gdx.gl.glEnable(GL20.GL_BLEND);
		shapes.begin(ShapeRenderer.ShapeType.Filled);
		shapes.setColor(COLOR_WORLD_BACKGROUND);
		shapes.rect(getWorldScreenLeft(), getWorldScreenBottom(), viewportWidth, viewportHeight);
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
		shapes.begin(ShapeRenderer.ShapeType.Filled);
		drawSelectedHandles();
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
			object.ensureDefaultPoints();
			for(int i = 1; i < object.points.size - 1; i++){
				Vector2 first = object.points.first();
				Vector2 second = object.points.get(i);
				Vector2 third = object.points.get(i + 1);
				shapes.triangle(object.x + first.x, object.y + first.y, object.x + second.x, object.y + second.y,
						object.x + third.x, object.y + third.y);
			}
		}
		else if(object.type == EditorObjectType.PLATFORM){
			object.ensureDefaultPoints();
			for(int i = 0; i < object.points.size - 1; i++){
				Vector2 first = object.points.get(i);
				Vector2 second = object.points.get(i + 1);
				shapes.rectLine(object.x + first.x, object.y + first.y, object.x + second.x, object.y + second.y,
						8f / Math.max(zoom, 0.01f));
			}
		}
		else{
			drawRectangleFilled(object);
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
			object.ensureDefaultPoints();
			for(int i = 0; i < object.points.size; i++){
				Vector2 first = object.points.get(i);
				Vector2 second = object.points.get((i + 1) % object.points.size);
				shapes.line(object.x + first.x, object.y + first.y, object.x + second.x, object.y + second.y);
			}
		}
		else if(object.type == EditorObjectType.PLATFORM){
			object.ensureDefaultPoints();
			for(int i = 0; i < object.points.size - 1; i++){
				Vector2 first = object.points.get(i);
				Vector2 second = object.points.get(i + 1);
				shapes.line(object.x + first.x, object.y + first.y, object.x + second.x, object.y + second.y);
			}
		}
		else{
			drawRectangleOutline(object);
		}
	}

	private void drawRectangleFilled(EditorLevelObject object){
		object.rectangleCorners(rectBottomLeft, rectBottomRight, rectTopRight, rectTopLeft);
		shapes.triangle(rectBottomLeft.x, rectBottomLeft.y, rectBottomRight.x, rectBottomRight.y,
				rectTopRight.x, rectTopRight.y);
		shapes.triangle(rectBottomLeft.x, rectBottomLeft.y, rectTopRight.x, rectTopRight.y,
				rectTopLeft.x, rectTopLeft.y);
	}

	private void drawRectangleOutline(EditorLevelObject object){
		object.rectangleCorners(rectBottomLeft, rectBottomRight, rectTopRight, rectTopLeft);
		shapes.line(rectBottomLeft.x, rectBottomLeft.y, rectBottomRight.x, rectBottomRight.y);
		shapes.line(rectBottomRight.x, rectBottomRight.y, rectTopRight.x, rectTopRight.y);
		shapes.line(rectTopRight.x, rectTopRight.y, rectTopLeft.x, rectTopLeft.y);
		shapes.line(rectTopLeft.x, rectTopLeft.y, rectBottomLeft.x, rectBottomLeft.y);
	}

	private void drawPlacementGhost(){
		if(activePaletteType == null || !isWorldScreen(Gdx.input.getX(), Gdx.input.getY()))
			return;
		Vector2 world = screenToWorld(Gdx.input.getX(), Gdx.input.getY(), scratch);
		shapes.setColor(1f, 1f, 1f, 0.22f);
		shapes.circle(world.x, world.y, 20f / Math.max(zoom, 0.01f), 20);
	}

	private void drawSelectedHandles(){
		if(selectedObject == null || !isVisible(selectedObject))
			return;
		float radius = HANDLE_SCREEN_RADIUS / Math.max(zoom, 0.01f);
		shapes.setColor(COLOR_SELECTED);
		if(selectedObject.type == EditorObjectType.POLYGON || selectedObject.type == EditorObjectType.PLATFORM){
			selectedObject.ensureDefaultPoints();
			for(Vector2 point : selectedObject.points)
				shapes.circle(selectedObject.x + point.x, selectedObject.y + point.y, radius, 12);
			return;
		}
		if(!canResizeByEdges(selectedObject))
			return;
		selectedObject.rectangleCorners(rectBottomLeft, rectBottomRight, rectTopRight, rectTopLeft);
		drawHandle(rectBottomLeft.x, rectBottomLeft.y, radius);
		drawMidHandle(rectBottomLeft, rectBottomRight, radius);
		drawHandle(rectBottomRight.x, rectBottomRight.y, radius);
		drawMidHandle(rectBottomLeft, rectTopLeft, radius);
		drawMidHandle(rectBottomRight, rectTopRight, radius);
		drawHandle(rectTopLeft.x, rectTopLeft.y, radius);
		drawMidHandle(rectTopLeft, rectTopRight, radius);
		drawHandle(rectTopRight.x, rectTopRight.y, radius);
	}

	private void drawHandle(float x, float y, float radius){
		shapes.rect(x - radius, y - radius, radius * 2f, radius * 2f);
	}

	private void drawMidHandle(Vector2 first, Vector2 second, float radius){
		drawHandle((first.x + second.x) / 2f, (first.y + second.y) / 2f, radius);
	}

	private void drawObjectLabels(){
		EditorLevelObject labelObject = selectedObject != null ? selectedObject : hoveredObject;
		if(labelObject == null || !isVisible(labelObject))
			return;
		game.batch.setProjectionMatrix(uiCamera.combined);
		game.batch.begin();
		editorFont.setColor(0.95f, 0.97f, 1f, 0.95f);
		editorFont.draw(game.batch, labelObject.type.label,
				getWorldScreenLeft() + (labelObject.getMinX() - cameraX) * zoom + 4f,
				getWorldScreenBottom() + (labelObject.getMaxY() - cameraY) * zoom + 18f);
		editorFont.setColor(Color.WHITE);
		game.batch.end();
	}

	private Color objectColor(EditorObjectType type){
		if(type == EditorObjectType.START)
			return transparent(editorColors.getCouleurBalle(), START_ALPHA);
		if(type == EditorObjectType.EXIT)
			return transparent(editorColors.getCouleurExit(), EXIT_ALPHA);
		if(type == EditorObjectType.WATER)
			return transparent(editorColors.getCouleurEau(), editorColors.getCouleurEau().a);
		if(type == EditorObjectType.LIGHT || type == EditorObjectType.REVOLVING || type == EditorObjectType.SWING
				|| type == EditorObjectType.BALANCOIRE || type == EditorObjectType.SUSPENDU || type == EditorObjectType.POULIE
				|| type == EditorObjectType.PLATFORM)
			return transparent(editorColors.getCouleurLeger(), OBJECT_ALPHA);
		if(type == EditorObjectType.SPRING)
			return transparent(editorColors.getCouleurExit(), EXIT_ALPHA);
		if(type == EditorObjectType.POLYGON)
			return transparent(editorColors.getCouleurSol(), OBJECT_ALPHA);
		return transparent(editorColors.getCouleurSol(), OBJECT_ALPHA);
	}

	private int hitPointHandle(EditorLevelObject object, float worldX, float worldY){
		if(object.type != EditorObjectType.POLYGON && object.type != EditorObjectType.PLATFORM)
			return -1;
		object.ensureDefaultPoints();
		float tolerance = HANDLE_SCREEN_RADIUS / Math.max(zoom, 0.01f);
		float toleranceSquared = tolerance * tolerance;
		for(int i = 0; i < object.points.size; i++){
			Vector2 point = object.points.get(i);
			if(scratch2.set(object.x + point.x - worldX, object.y + point.y - worldY).len2() <= toleranceSquared)
				return i;
		}
		return -1;
	}

	private boolean hitResizeHandle(EditorLevelObject object, float worldX, float worldY){
		resizeHorizontal = 0;
		resizeVertical = 0;
		if(!canResizeByEdges(object))
			return false;
		float tolerance = EDGE_SCREEN_TOLERANCE / Math.max(zoom, 0.01f);
		Vector2 local = object.worldToVisualLocal(worldX, worldY, scratch2);
		boolean inHorizontalBand = local.x >= -tolerance && local.x <= object.width + tolerance;
		boolean inVerticalBand = local.y >= -tolerance && local.y <= object.height + tolerance;
		boolean nearLeft = Math.abs(local.x) <= tolerance && inVerticalBand;
		boolean nearRight = Math.abs(local.x - object.width) <= tolerance && inVerticalBand;
		boolean nearBottom = Math.abs(local.y) <= tolerance && inHorizontalBand;
		boolean nearTop = Math.abs(local.y - object.height) <= tolerance && inHorizontalBand;
		if(!nearLeft && !nearRight && !nearBottom && !nearTop)
			return false;
		if(nearLeft)
			resizeHorizontal = -1;
		else if(nearRight)
			resizeHorizontal = 1;
		if(nearBottom)
			resizeVertical = -1;
		else if(nearTop)
			resizeVertical = 1;
		return true;
	}

	private void resizeSelectedObject(float worldX, float worldY){
		if(selectedObject == null || !canResizeByEdges(selectedObject))
			return;
		if(selectedObject.usesRotatedRectangleGeometry()){
			resizeRotatedSelectedObject(worldX, worldY);
			return;
		}
		if(resizeHorizontal < 0){
			float right = selectedObject.x + selectedObject.width;
			float left = Math.min(snapForSelected(worldX), right - MIN_OBJECT_SIZE);
			selectedObject.x = left;
			selectedObject.width = right - left;
		}
		else if(resizeHorizontal > 0){
			selectedObject.width = Math.max(MIN_OBJECT_SIZE, snapForSelected(worldX) - selectedObject.x);
		}
		if(resizeVertical < 0){
			float top = selectedObject.y + selectedObject.height;
			float bottom = Math.min(snapForSelected(worldY), top - MIN_OBJECT_SIZE);
			selectedObject.y = bottom;
			selectedObject.height = top - bottom;
		}
		else if(resizeVertical > 0){
			selectedObject.height = Math.max(MIN_OBJECT_SIZE, snapForSelected(worldY) - selectedObject.y);
		}
	}

	private void resizeRotatedSelectedObject(float worldX, float worldY){
		selectedObject.worldToVisualLocal(worldX, worldY, scratch2);
		float localX = snapLocalForSelected(scratch2.x);
		float localY = snapLocalForSelected(scratch2.y);
		if(resizeHorizontal < 0){
			float delta = Math.min(localX, selectedObject.width - MIN_OBJECT_SIZE);
			moveSelectedByVisualLocalDelta(delta, 0f);
			selectedObject.width -= delta;
		}
		else if(resizeHorizontal > 0){
			selectedObject.width = Math.max(MIN_OBJECT_SIZE, localX);
		}
		if(resizeVertical < 0){
			float delta = Math.min(localY, selectedObject.height - MIN_OBJECT_SIZE);
			moveSelectedByVisualLocalDelta(0f, delta);
			selectedObject.height -= delta;
		}
		else if(resizeVertical > 0){
			selectedObject.height = Math.max(MIN_OBJECT_SIZE, localY);
		}
	}

	private void moveSelectedByVisualLocalDelta(float localX, float localY){
		float angle = selectedObject.getVisualRotationDegrees() * MathUtils.degreesToRadians;
		float cos = MathUtils.cos(angle);
		float sin = MathUtils.sin(angle);
		selectedObject.x += localX * cos - localY * sin;
		selectedObject.y += localX * sin + localY * cos;
	}

	private boolean canResizeByEdges(EditorLevelObject object){
		return object.type != EditorObjectType.START && object.type != EditorObjectType.EXIT
				&& object.type != EditorObjectType.POLYGON && object.type != EditorObjectType.PLATFORM;
	}

	private boolean isWorldScreen(int screenX, int screenY){
		float stageY = Gdx.graphics.getHeight() - screenY;
		return screenX >= getWorldScreenLeft() && screenX <= getWorldScreenRight()
				&& stageY >= getWorldScreenBottom() && stageY <= getWorldScreenTop();
	}

	private Vector2 screenToWorld(int screenX, int screenY, Vector2 out){
		float stageY = Gdx.graphics.getHeight() - screenY;
		return out.set(cameraX + (screenX - getWorldScreenLeft()) / zoom,
				cameraY + (stageY - getWorldScreenBottom()) / zoom);
	}

	private void updateHoverIfNeeded(){
		if(dragMode != DragMode.NONE)
			return;
		int screenX = Gdx.input.getX();
		int screenY = Gdx.input.getY();
		if(!hoverDirty && screenX == lastHoverScreenX && screenY == lastHoverScreenY
				&& cameraX == lastHoverCameraX && cameraY == lastHoverCameraY && zoom == lastHoverZoom)
			return;
		updateHover(screenX, screenY);
	}

	private void updateHover(int screenX, int screenY){
		lastHoverScreenX = screenX;
		lastHoverScreenY = screenY;
		lastHoverCameraX = cameraX;
		lastHoverCameraY = cameraY;
		lastHoverZoom = zoom;
		hoverDirty = false;
		if(!isWorldScreen(screenX, screenY)){
			hoveredObject = null;
			return;
		}
		Vector2 world = screenToWorld(screenX, screenY, scratch);
		hoveredObject = level.findAt(world.x, world.y);
	}

	private void markHoverDirty(){
		hoverDirty = true;
	}

	private void updateWorldCamera(){
		float viewportWidth = getWorldScreenWidth() / Math.max(zoom, 0.01f);
		float viewportHeight = getWorldScreenHeight() / Math.max(zoom, 0.01f);
		worldCamera.setToOrtho(false, viewportWidth, viewportHeight);
		worldCamera.position.set(cameraX + viewportWidth / 2f, cameraY + viewportHeight / 2f, 0f);
		worldCamera.update();
		visibleWorldBounds.set(cameraX, cameraY, viewportWidth, viewportHeight);
	}

	private float getWorldScreenLeft(){
		return SCREEN_MARGIN + LEFT_PANEL_WIDTH;
	}

	private float getWorldScreenRight(){
		return Math.max(getWorldScreenLeft() + 1f, Gdx.graphics.getWidth() - SCREEN_MARGIN - RIGHT_PANEL_WIDTH);
	}

	private float getWorldScreenBottom(){
		return SCREEN_MARGIN;
	}

	private float getWorldScreenTop(){
		return Math.max(getWorldScreenBottom() + 1f, Gdx.graphics.getHeight() - SCREEN_MARGIN);
	}

	private float getWorldScreenWidth(){
		return Math.max(1f, getWorldScreenRight() - getWorldScreenLeft());
	}

	private float getWorldScreenHeight(){
		return Math.max(1f, getWorldScreenTop() - getWorldScreenBottom());
	}

	private boolean isVisible(EditorLevelObject object){
		float minX = object.getMinX();
		float maxX = object.getMaxX();
		float minY = object.getMinY();
		float maxY = object.getMaxY();
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
				+ " margin=" + Math.round(SCREEN_MARGIN)
				+ " buttonHeight=" + BUTTON_HEIGHT + " fieldHeight=" + FIELD_HEIGHT
				+ " fontScale=" + number(EDITOR_FONT_SCALE)
				+ " worldViewport=" + Math.round(getWorldScreenWidth()) + "x" + Math.round(getWorldScreenHeight());
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

	private Color transparent(Color base, float alpha){
		return objectColorScratch.set(base.r, base.g, base.b, alpha);
	}

	private String snapLabel(EditorLevelObject object){
		return object.snapMode == SnapMode.GRID ? "Snap: Grid" : "Snap: Free";
	}

	private float snapForSelected(float value){
		return selectedObject != null && selectedObject.snapMode == SnapMode.GRID ? snap(value) : value;
	}

	private float snapLocalForSelected(float value){
		return selectedObject != null && selectedObject.snapMode == SnapMode.GRID ? snap(value) : value;
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
