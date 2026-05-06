package editor;

import com.badlogic.gdx.utils.Array;

public class EditorLevel {
	public static final int TILE_SIZE = 32;

	public String fileName = "Editor Level.tmx";
	public int widthTiles = 80;
	public int heightTiles = 45;
	public final Array<EditorLevelObject> objects = new Array<EditorLevelObject>();

	public EditorLevel(){
		addDefaultObjects();
	}

	public int getPixelWidth(){
		return Math.max(1, widthTiles) * TILE_SIZE;
	}

	public int getPixelHeight(){
		return Math.max(1, heightTiles) * TILE_SIZE;
	}

	public EditorLevelObject createObject(EditorObjectType type, float centerX, float centerY){
		EditorLevelObject object;
		if(type == EditorObjectType.START){
			removeObjectsOfType(EditorObjectType.START);
			object = new EditorLevelObject(type, snap(centerX - 16f), snap(centerY - 16f), 32f, 32f);
		}
		else if(type == EditorObjectType.EXIT){
			object = new EditorLevelObject(type, snap(centerX - 32f), snap(centerY - 32f), 64f, 64f);
		}
		else if(type == EditorObjectType.WATER){
			object = new EditorLevelObject(type, snap(centerX - 128f), snap(centerY - 64f), 256f, 128f);
		}
		else if(type == EditorObjectType.SPRING){
			object = new EditorLevelObject(type, snap(centerX - 32f), snap(centerY - 48f), 64f, 96f);
			object.properties.put("PowerX", "0");
			object.properties.put("PowerY", "60");
		}
		else if(type == EditorObjectType.LIGHT){
			object = new EditorLevelObject(type, snap(centerX - 32f), snap(centerY - 32f), 64f, 64f);
			object.properties.put("Weight", "0.8");
		}
		else if(type == EditorObjectType.REVOLVING){
			object = new EditorLevelObject(type, snap(centerX - 128f), snap(centerY - 16f), 256f, 32f);
			object.properties.put("Speed", "15");
		}
		else if(type == EditorObjectType.SWING){
			object = new EditorLevelObject(type, snap(centerX - 128f), snap(centerY - 16f), 256f, 32f);
			object.properties.put("Position", "0");
			object.properties.put("Weight", "15");
			object.properties.put("Speed", "1");
			object.properties.put("Torque", "1");
		}
		else if(type == EditorObjectType.BALANCOIRE){
			object = new EditorLevelObject(type, snap(centerX - 128f), snap(centerY - 16f), 256f, 32f);
			object.properties.put("AttacheY", "10");
			object.properties.put("Weight", "5");
		}
		else if(type == EditorObjectType.SUSPENDU){
			object = new EditorLevelObject(type, snap(centerX - 32f), snap(centerY - 32f), 64f, 64f);
			object.properties.put("Length", "5");
			object.properties.put("Position", "1");
			object.properties.put("Weight", "5");
		}
		else if(type == EditorObjectType.POULIE){
			object = new EditorLevelObject(type, snap(centerX - 48f), snap(centerY - 16f), 96f, 32f);
			int group = nextPulleyGroup();
			object.properties.put("Groupe", String.valueOf(group));
			object.properties.put("Masse", "50");
			object.properties.put("longueur", "5");
		}
		else if(type == EditorObjectType.PLATFORM){
			object = new EditorLevelObject(type, snap(centerX - 128f), snap(centerY), 256f, 0f);
			object.properties.put("Speed", "5");
			object.properties.put("Width", "2");
		}
		else if(type == EditorObjectType.POLYGON){
			object = new EditorLevelObject(type, snap(centerX - 96f), snap(centerY - 64f), 192f, 128f);
		}
		else{
			object = new EditorLevelObject(type, snap(centerX - 128f), snap(centerY - 16f), 256f, 32f);
		}
		objects.add(object);
		return object;
	}

	public Array<EditorLevelObject> createPulleyPair(float centerX, float centerY){
		int group = nextPulleyGroup();
		Array<EditorLevelObject> pair = new Array<EditorLevelObject>();
		EditorLevelObject left = new EditorLevelObject(EditorObjectType.POULIE, snap(centerX - 144f), snap(centerY), 96f, 32f);
		EditorLevelObject right = new EditorLevelObject(EditorObjectType.POULIE, snap(centerX + 48f), snap(centerY), 96f, 32f);
		configurePulley(left, group);
		configurePulley(right, group);
		objects.add(left);
		objects.add(right);
		pair.add(left);
		pair.add(right);
		return pair;
	}

	private void configurePulley(EditorLevelObject object, int group){
		object.properties.put("Groupe", String.valueOf(group));
		object.properties.put("Masse", "50");
		object.properties.put("longueur", "5");
	}

	public void remove(EditorLevelObject object){
		objects.removeValue(object, true);
	}

	public EditorLevelObject findAt(float worldX, float worldY){
		for(int i = objects.size - 1; i >= 0; i--){
			EditorLevelObject object = objects.get(i);
			if(object.contains(worldX, worldY))
				return object;
		}
		return null;
	}

	public EditorLevelObject getStart(){
		for(EditorLevelObject object : objects)
			if(object.type == EditorObjectType.START)
				return object;
		return null;
	}

	public EditorLevelObject getExit(){
		for(EditorLevelObject object : objects)
			if(object.type == EditorObjectType.EXIT)
				return object;
		return null;
	}

	private void addDefaultObjects(){
		createObject(EditorObjectType.START, 160f, 160f);
		createObject(EditorObjectType.SOLID, 640f, 48f);
		createObject(EditorObjectType.EXIT, 1120f, 192f);
	}

	private void removeObjectsOfType(EditorObjectType type){
		for(int i = objects.size - 1; i >= 0; i--){
			if(objects.get(i).type == type)
				objects.removeIndex(i);
		}
	}

	private int nextPulleyGroup(){
		int max = 0;
		for(EditorLevelObject object : objects){
			String value = object.properties.get("Groupe");
			if(value == null)
				continue;
			try{
				max = Math.max(max, Integer.parseInt(value));
			}
			catch(NumberFormatException ignored){
			}
		}
		return max + 1;
	}

	private float snap(float value){
		return Math.round(value / TILE_SIZE) * TILE_SIZE;
	}
}
