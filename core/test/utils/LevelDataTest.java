package utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

class LevelDataTest {
	private static final int LEVEL_COUNT = 5;

	@Test
	void everyLevelHasSpawnAndSingleExit() throws Exception {
		Path levelsDir = Path.of(System.getProperty("assets.dir"), "Levels");
		for(int level = 1; level <= LEVEL_COUNT; level++){
			Document document = parse(levelsDir.resolve("Level " + level + ".tmx"));
			assertEquals(1, countObjects(document, "Exit", null), "Level " + level + " must have exactly one Exit");
			assertEquals(1, countObjects(document, null, "Ball"), "Level " + level + " must have exactly one Ball spawn");
			assertTrue(countRectangles(document) > 0, "Level " + level + " must have drawable/collidable rectangles");
		}
	}

	@Test
	void levelThreeDependsOnRectangleDrawing() throws Exception {
		Path level3 = Path.of(System.getProperty("assets.dir"), "Levels", "Level 3.tmx");
		Document document = parse(level3);

		assertEquals(0, document.getElementsByTagName("polygon").getLength(),
				"Level 3 has no polygons, so rectangular obstacle drawing must work");
		assertTrue(countRectangles(document) > 10, "Level 3 should have enough rectangle obstacles to draw the scene");
	}

	private static Document parse(Path path) throws Exception {
		assertTrue(Files.exists(path), "Missing level file: " + path);
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		return factory.newDocumentBuilder().parse(path.toFile());
	}

	private static int countObjects(Document document, String type, String name) {
		int count = 0;
		NodeList objects = document.getElementsByTagName("object");
		for(int i = 0; i < objects.getLength(); i++){
			Element object = (Element)objects.item(i);
			boolean typeMatches = type == null || type.equals(object.getAttribute("type"));
			boolean nameMatches = name == null || name.equals(object.getAttribute("name"));
			if(typeMatches && nameMatches)
				count++;
		}
		return count;
	}

	private static int countRectangles(Document document) {
		int count = 0;
		NodeList objects = document.getElementsByTagName("object");
		for(int i = 0; i < objects.getLength(); i++){
			Element object = (Element)objects.item(i);
			if(object.hasAttribute("width") && object.hasAttribute("height")
					&& object.getElementsByTagName("polygon").getLength() == 0
					&& object.getElementsByTagName("polyline").getLength() == 0)
				count++;
		}
		return count;
	}
}
