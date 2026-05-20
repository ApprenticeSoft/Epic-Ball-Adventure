package editor;

import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.PolygonMapObject;
import com.badlogic.gdx.maps.objects.PolylineMapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Polyline;
import com.badlogic.gdx.utils.ObjectMap;

public final class EditorTiledMapFactory {
	private EditorTiledMapFactory(){
	}

	public static TiledMap build(EditorLevel level){
		TiledMap tiledMap = new TiledMap();
		tiledMap.getProperties().put("width", level.widthTiles);
		tiledMap.getProperties().put("height", level.heightTiles);
		tiledMap.getProperties().put("tilewidth", EditorLevel.TILE_SIZE);
		tiledMap.getProperties().put("tileheight", EditorLevel.TILE_SIZE);

		MapLayer objects = new MapLayer();
		objects.setName("Objects");
		MapLayer spawn = new MapLayer();
		spawn.setName("Spawn");

		for(EditorLevelObject object : level.objects){
			if(object.type == EditorObjectType.START)
				spawn.getObjects().add(createSpawn(object));
			else
				objects.getObjects().add(createObject(object));
		}

		tiledMap.getLayers().add(objects);
		tiledMap.getLayers().add(spawn);
		return tiledMap;
	}

	private static MapObject createSpawn(EditorLevelObject object){
		RectangleMapObject rectangle = new RectangleMapObject(object.x, object.y, object.width, object.height);
		rectangle.setName("Ball");
		putRectangleProperties(rectangle, object);
		return rectangle;
	}

	private static MapObject createObject(EditorLevelObject object){
		MapObject mapObject;
		if(object.type == EditorObjectType.POLYGON){
			Polygon polygon = new Polygon(object.pointVertices());
			polygon.setPosition(object.x, object.y);
			mapObject = new PolygonMapObject(polygon);
		}
		else if(object.type == EditorObjectType.PLATFORM){
			Polyline polyline = new Polyline(object.pointVertices());
			polyline.setPosition(object.x, object.y);
			mapObject = new PolylineMapObject(polyline);
		}
		else{
			RectangleMapObject rectangle = new RectangleMapObject(object.x, object.y, object.width, object.height);
			putRectangleProperties(rectangle, object);
			mapObject = rectangle;
		}
		if(object.type.tmxType != null)
			mapObject.getProperties().put("type", object.type.tmxType);
		if(Math.abs(object.rotation) > 0.0001f)
			mapObject.getProperties().put("rotation", object.rotation);
		for(ObjectMap.Entry<String, String> entry : object.properties)
			mapObject.getProperties().put(entry.key, entry.value);
		return mapObject;
	}

	private static void putRectangleProperties(RectangleMapObject rectangle, EditorLevelObject object){
		rectangle.getProperties().put("x", object.x);
		rectangle.getProperties().put("y", object.y);
		rectangle.getProperties().put("width", object.width);
		rectangle.getProperties().put("height", object.height);
	}
}
