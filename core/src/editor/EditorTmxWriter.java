package editor;

import com.badlogic.gdx.utils.ObjectMap;

public final class EditorTmxWriter {
	private EditorTmxWriter(){
	}

	public static String write(EditorLevel level){
		StringBuilder xml = new StringBuilder(8192);
		xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
		xml.append("<map version=\"1.0\" orientation=\"orthogonal\" renderorder=\"right-down\"");
		xml.append(" width=\"").append(level.widthTiles).append("\" height=\"").append(level.heightTiles).append("\"");
		xml.append(" tilewidth=\"").append(EditorLevel.TILE_SIZE).append("\" tileheight=\"").append(EditorLevel.TILE_SIZE).append("\"");
		xml.append(" nextobjectid=\"").append(level.objects.size + 1).append("\">\n");
		xml.append(" <tileset firstgid=\"1\" name=\"Start\" tilewidth=\"32\" tileheight=\"32\" tilecount=\"153\" columns=\"17\">\n");
		xml.append("  <image source=\"Start.png\" width=\"544\" height=\"306\"/>\n");
		xml.append(" </tileset>\n");
		appendBlankLayer(xml, level);
		appendObjects(xml, level);
		appendSpawn(xml, level);
		xml.append("</map>\n");
		return xml.toString();
	}

	private static void appendBlankLayer(StringBuilder xml, EditorLevel level){
		xml.append(" <layer id=\"1\" name=\"Calque de Tile 1\" width=\"").append(level.widthTiles)
				.append("\" height=\"").append(level.heightTiles).append("\">\n");
		xml.append("  <data encoding=\"csv\">\n");
		for(int y = 0; y < level.heightTiles; y++){
			xml.append("   ");
			for(int x = 0; x < level.widthTiles; x++){
				xml.append("0");
				if(x < level.widthTiles - 1 || y < level.heightTiles - 1)
					xml.append(",");
			}
			xml.append("\n");
		}
		xml.append("  </data>\n");
		xml.append(" </layer>\n");
	}

	private static void appendObjects(StringBuilder xml, EditorLevel level){
		xml.append(" <objectgroup id=\"2\" name=\"Objects\">\n");
		int id = 1;
		for(EditorLevelObject object : level.objects){
			if(object.type == EditorObjectType.START)
				continue;
			appendObject(xml, object, id++);
		}
		xml.append(" </objectgroup>\n");
	}

	private static void appendSpawn(StringBuilder xml, EditorLevel level){
		xml.append(" <objectgroup id=\"3\" name=\"Spawn\">\n");
		EditorLevelObject start = level.getStart();
		if(start != null){
			xml.append("  <object id=\"").append(level.objects.size).append("\" name=\"Ball\" gid=\"1\"");
			appendCommonAttributes(xml, start);
			xml.append("/>\n");
		}
		xml.append(" </objectgroup>\n");
	}

	private static void appendObject(StringBuilder xml, EditorLevelObject object, int id){
		xml.append("  <object id=\"").append(id).append("\"");
		if(object.type.tmxType != null)
			xml.append(" type=\"").append(escape(object.type.tmxType)).append("\"");
		appendCommonAttributes(xml, object);
		if(object.type == EditorObjectType.POLYGON){
			xml.append(">\n");
			appendProperties(xml, object);
			xml.append("   <polygon points=\"").append(points(object)).append("\"/>\n");
			xml.append("  </object>\n");
		}
		else if(object.type == EditorObjectType.PLATFORM){
			xml.append(">\n");
			appendProperties(xml, object);
			xml.append("   <polyline points=\"").append(points(object)).append("\"/>\n");
			xml.append("  </object>\n");
		}
		else if(object.properties.size > 0){
			xml.append(">\n");
			appendProperties(xml, object);
			xml.append("  </object>\n");
		}
		else{
			xml.append("/>\n");
		}
	}

	private static void appendCommonAttributes(StringBuilder xml, EditorLevelObject object){
		xml.append(" x=\"").append(number(object.x)).append("\"");
		xml.append(" y=\"").append(number(object.y)).append("\"");
		if(object.type != EditorObjectType.PLATFORM && object.type != EditorObjectType.POLYGON){
			xml.append(" width=\"").append(number(object.width)).append("\"");
			xml.append(" height=\"").append(number(object.height)).append("\"");
		}
		if(Math.abs(object.rotation) > 0.0001f)
			xml.append(" rotation=\"").append(number(object.rotation)).append("\"");
	}

	private static void appendProperties(StringBuilder xml, EditorLevelObject object){
		if(object.properties.size == 0)
			return;
		xml.append("   <properties>\n");
		for(ObjectMap.Entry<String, String> entry : object.properties){
			xml.append("    <property name=\"").append(escape(entry.key)).append("\"");
			appendPropertyValue(xml, entry.value);
			xml.append("/>\n");
		}
		xml.append("   </properties>\n");
	}

	private static void appendPropertyValue(StringBuilder xml, String value){
		if("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)){
			xml.append(" type=\"bool\" value=\"").append(value.toLowerCase()).append("\"");
			return;
		}
		if(isNumber(value)){
			xml.append(" type=\"float\" value=\"").append(escape(value)).append("\"");
			return;
		}
		xml.append(" value=\"").append(escape(value)).append("\"");
	}

	private static boolean isNumber(String value){
		if(value == null || value.length() == 0)
			return false;
		try{
			Float.parseFloat(value);
			return true;
		}
		catch(NumberFormatException ignored){
			return false;
		}
	}

	private static String number(float value){
		if(Math.abs(value - Math.round(value)) < 0.0001f)
			return String.valueOf(Math.round(value));
		return String.valueOf(value);
	}

	private static String points(EditorLevelObject object){
		float[] vertices = object.pointVertices();
		StringBuilder builder = new StringBuilder();
		for(int i = 0; i < vertices.length; i += 2){
			if(i > 0)
				builder.append(" ");
			builder.append(number(vertices[i])).append(",").append(number(vertices[i + 1]));
		}
		return builder.toString();
	}

	private static String escape(String value){
		if(value == null)
			return "";
		return value.replace("&", "&amp;")
				.replace("\"", "&quot;")
				.replace("<", "&lt;")
				.replace(">", "&gt;");
	}
}
