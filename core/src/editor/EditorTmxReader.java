package editor;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.XmlReader;
import com.badlogic.gdx.utils.XmlReader.Element;

public final class EditorTmxReader {
	private EditorTmxReader(){
	}

	public static EditorLevel read(String fileName, String xml){
		try{
			Element map = new XmlReader().parse(xml);
			EditorLevel level = new EditorLevel(false);
			level.fileName = EditorFileBridge.sanitize(fileName);
			level.widthTiles = Math.max(1, map.getIntAttribute("width", 80));
			level.heightTiles = Math.max(1, map.getIntAttribute("height", 45));
			Array<Element> groups = map.getChildrenByName("objectgroup");
			for(Element group : groups)
				readObjectGroup(level, group);
			return level;
		}
		catch(Exception exception){
			throw new RuntimeException("Invalid TMX: " + exception.getMessage(), exception);
		}
	}

	private static void readObjectGroup(EditorLevel level, Element group){
		String groupName = group.getAttribute("name", "");
		Array<Element> objects = group.getChildrenByName("object");
		for(Element element : objects){
			EditorLevelObject object = readObject(groupName, element);
			if(object != null)
				level.objects.add(object);
		}
	}

	private static EditorLevelObject readObject(String groupName, Element element){
		EditorObjectType type = objectType(groupName, element);
		if(type == null)
			return null;
		float x = element.getFloatAttribute("x", 0f);
		float y = element.getFloatAttribute("y", 0f);
		float width = element.getFloatAttribute("width", defaultWidth(type));
		float height = element.getFloatAttribute("height", defaultHeight(type));
		EditorLevelObject object = new EditorLevelObject(type, x, y, width, height);
		object.rotation = element.getFloatAttribute("rotation", 0f);
		readProperties(object, element);
		Element polygon = element.getChildByName("polygon");
		Element polyline = element.getChildByName("polyline");
		if(polygon != null)
			object.replacePoints(parsePoints(polygon.getAttribute("points", "")));
		else if(polyline != null)
			object.replacePoints(parsePoints(polyline.getAttribute("points", "")));
		return object;
	}

	private static EditorObjectType objectType(String groupName, Element element){
		if("Spawn".equals(groupName) && "Ball".equals(element.getAttribute("name", "")))
			return EditorObjectType.START;
		if(element.getChildByName("polyline") != null)
			return EditorObjectType.PLATFORM;
		if(element.getChildByName("polygon") != null)
			return EditorObjectType.POLYGON;
		String typeName = element.getAttribute("type", "");
		if(typeName.length() == 0)
			typeName = propertyValue(element, "type", "");
		if(typeName.length() == 0)
			return EditorObjectType.SOLID;
		if(isBalancoireType(typeName))
			return EditorObjectType.BALANCOIRE;
		for(EditorObjectType type : EditorObjectType.values()){
			if(type.name().equalsIgnoreCase(typeName) || type.label.equalsIgnoreCase(typeName)
					|| (type.tmxType != null && type.tmxType.equalsIgnoreCase(typeName)))
				return type;
		}
		return EditorObjectType.SOLID;
	}

	private static boolean isBalancoireType(String typeName){
		return "Balancoire".equalsIgnoreCase(typeName) || "Balan\u00e7oire".equalsIgnoreCase(typeName);
	}

	private static void readProperties(EditorLevelObject object, Element element){
		Element properties = element.getChildByName("properties");
		if(properties == null)
			return;
		Array<Element> propertyElements = properties.getChildrenByName("property");
		for(Element property : propertyElements){
			String name = property.getAttribute("name", "");
			if(name.length() == 0 || "type".equals(name))
				continue;
			object.properties.put(name, property.getAttribute("value", property.getText()));
		}
	}

	private static String propertyValue(Element element, String name, String fallback){
		Element properties = element.getChildByName("properties");
		if(properties == null)
			return fallback;
		Array<Element> propertyElements = properties.getChildrenByName("property");
		for(Element property : propertyElements)
			if(name.equals(property.getAttribute("name", "")))
				return property.getAttribute("value", property.getText());
		return fallback;
	}

	private static float[] parsePoints(String value){
		if(value == null || value.trim().length() == 0)
			return new float[0];
		String[] pointTokens = value.trim().split("\\s+");
		float[] vertices = new float[pointTokens.length * 2];
		for(int i = 0; i < pointTokens.length; i++){
			String[] pair = pointTokens[i].split(",");
			if(pair.length != 2)
				throw new RuntimeException("Bad point: " + pointTokens[i]);
			vertices[i * 2] = Float.parseFloat(pair[0]);
			vertices[i * 2 + 1] = Float.parseFloat(pair[1]);
		}
		return vertices;
	}

	private static float defaultWidth(EditorObjectType type){
		if(type == EditorObjectType.START)
			return 32f;
		if(type == EditorObjectType.EXIT)
			return 64f;
		return 1f;
	}

	private static float defaultHeight(EditorObjectType type){
		if(type == EditorObjectType.START)
			return 32f;
		if(type == EditorObjectType.EXIT)
			return 64f;
		return 1f;
	}
}
