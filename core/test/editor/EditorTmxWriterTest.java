package editor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EditorTmxWriterTest {
	@Test
	public void writesPlayableTmxStructure(){
		EditorLevel level = new EditorLevel();
		level.createObject(EditorObjectType.SPRING, 320, 160);
		level.createPulleyPair(640, 256);
		level.createObject(EditorObjectType.PLATFORM, 480, 320);

		String xml = EditorTmxWriter.write(level);

		assertTrue(xml.contains("<objectgroup id=\"2\" name=\"Objects\">"));
		assertTrue(xml.contains("<objectgroup id=\"3\" name=\"Spawn\">"));
		assertTrue(xml.contains("name=\"Ball\""));
		assertTrue(xml.contains("type=\"Exit\""));
		assertTrue(xml.contains("type=\"Spring\""));
		assertTrue(xml.contains("name=\"PowerY\""));
		assertTrue(xml.contains("type=\"Poulie\""));
		assertTrue(xml.contains("name=\"Groupe\""));
		assertTrue(xml.contains("<polyline points=\"0,0 256,0\"/>"));
	}

	@Test
	public void swingSelectionDoesNotNeedAngleLimitProperties(){
		EditorLevel level = new EditorLevel();
		EditorLevelObject swing = level.createObject(EditorObjectType.SWING, 1200, 500);

		String xml = EditorTmxWriter.write(level);

		assertTrue(xml.contains("type=\"Swing\""));
		assertTrue(xml.contains("name=\"Position\""));
		assertTrue(xml.contains("name=\"Weight\""));
		assertFalse(xml.contains("name=\"angleRef\""));
		assertFalse(xml.contains("name=\"angleMin\""));
		assertFalse(xml.contains("name=\"angleMax\""));
		assertFalse(xml.contains("name=\"Contact\""));
		assertFalse(swing.properties.containsKey("angleRef"));
	}

	@Test
	public void writesEditablePolygonAndPlatformPoints(){
		EditorLevel level = new EditorLevel();
		EditorLevelObject polygon = level.createObject(EditorObjectType.POLYGON, 320, 320);
		polygon.setPolygonVertexCount(5);
		polygon.setPointWorldPosition(2, polygon.x + 240, polygon.y + 64);
		EditorLevelObject platform = level.createObject(EditorObjectType.PLATFORM, 640, 320);
		platform.setPlatformPointCount(3);
		platform.setPointWorldPosition(1, platform.x + 128, platform.y + 96);
		platform.setPointWorldPosition(2, platform.x + 256, platform.y + 0);

		String xml = EditorTmxWriter.write(level);

		assertTrue(xml.contains("<polygon points=\""));
		assertTrue(xml.contains("240,64"));
		assertTrue(xml.contains("<polyline points=\"0,0 128,96 256,0\"/>"));
	}

	@Test
	public void writesTypedBooleanAndRuntimeContactProperties(){
		EditorLevel level = new EditorLevel();
		EditorLevelObject platform = level.createObject(EditorObjectType.PLATFORM, 640, 320);
		platform.properties.put("Loop", "true");
		EditorLevelObject swing = level.createObject(EditorObjectType.SWING, 1200, 500);
		swing.properties.put("Contact", "oui");

		String xml = EditorTmxWriter.write(level);

		assertTrue(xml.contains("<property name=\"Loop\" type=\"bool\" value=\"true\"/>"));
		assertTrue(xml.contains("<property name=\"Contact\" value=\"oui\"/>"));
	}
}
