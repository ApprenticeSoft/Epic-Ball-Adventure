package editor;

import org.junit.jupiter.api.Test;

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
}
