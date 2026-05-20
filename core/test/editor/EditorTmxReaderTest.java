package editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.utils.Array;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

public class EditorTmxReaderTest {
	@Test
	public void readsObjectsSpawnPolygonAndPlatform(){
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
				+ "<map width=\"20\" height=\"10\" tilewidth=\"32\" tileheight=\"32\">\n"
				+ " <objectgroup name=\"Objects\">\n"
				+ "  <object id=\"1\" x=\"64\" y=\"96\" width=\"128\" height=\"32\"/>\n"
				+ "  <object id=\"2\" type=\"Spring\" x=\"224\" y=\"96\" width=\"64\" height=\"96\" rotation=\"15\">\n"
				+ "   <properties><property name=\"PowerY\" value=\"60\"/></properties>\n"
				+ "  </object>\n"
				+ "  <object id=\"3\" x=\"320\" y=\"96\"><polygon points=\"0,0 64,0 32,32\"/></object>\n"
				+ "  <object id=\"4\" x=\"64\" y=\"192\">\n"
				+ "   <properties><property name=\"Speed\" value=\"5\"/><property name=\"Width\" value=\"2\"/></properties>\n"
				+ "   <polyline points=\"0,0 128,0 128,64\"/>\n"
				+ "  </object>\n"
				+ " </objectgroup>\n"
				+ " <objectgroup name=\"Spawn\"><object id=\"5\" name=\"Ball\" x=\"32\" y=\"32\" width=\"32\" height=\"32\"/></objectgroup>\n"
				+ "</map>";

		EditorLevel level = EditorTmxReader.read("Imported.tmx", xml);

		assertEquals("Imported.tmx", level.fileName);
		assertEquals(20, level.widthTiles);
		assertEquals(10, level.heightTiles);
		assertNotNull(level.getStart());
		assertEquals(EditorObjectType.SOLID, level.objects.get(0).type);
		assertEquals(EditorObjectType.SPRING, level.objects.get(1).type);
		assertEquals("60", level.objects.get(1).properties.get("PowerY"));
		assertEquals(15f, level.objects.get(1).rotation, 0.001f);
		assertEquals(EditorObjectType.POLYGON, level.objects.get(2).type);
		assertEquals(3, level.objects.get(2).points.size);
		assertEquals(EditorObjectType.PLATFORM, level.objects.get(3).type);
		assertEquals(3, level.objects.get(3).points.size);
	}

	@Test
	public void writerOutputCanBeReadBack(){
		EditorLevel source = new EditorLevel();
		source.fileName = "Roundtrip.tmx";
		source.createObject(EditorObjectType.SPRING, 320, 160);
		EditorLevelObject platform = source.createObject(EditorObjectType.PLATFORM, 640, 320);
		platform.setPlatformPointCount(3);
		platform.setPointWorldPosition(1, platform.x + 128, platform.y + 64);

		EditorLevel loaded = EditorTmxReader.read(source.fileName, EditorTmxWriter.write(source));

		assertEquals(source.widthTiles, loaded.widthTiles);
		assertEquals(source.heightTiles, loaded.heightTiles);
		assertTrue(loaded.objects.size >= source.objects.size);
		assertNotNull(loaded.getStart());
	}

	@Test
	public void readsLegacyAccentedBalancoireType(){
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
				+ "<map width=\"20\" height=\"10\" tilewidth=\"32\" tileheight=\"32\">\n"
				+ " <objectgroup name=\"Objects\">\n"
				+ "  <object id=\"1\" type=\"Balan\u00e7oire\" x=\"64\" y=\"96\" width=\"128\" height=\"32\"/>\n"
				+ " </objectgroup>\n"
				+ "</map>";

		EditorLevel level = EditorTmxReader.read("Legacy.tmx", xml);

		assertEquals(EditorObjectType.BALANCOIRE, level.objects.first().type);
	}

	@Test
	public void importsEveryBundledLevelWithoutDroppingObjects() throws IOException{
		Path levelsDir = Path.of(System.getProperty("assets.dir"), "Levels");
		int importedLevels = 0;
		try(DirectoryStream<Path> files = Files.newDirectoryStream(levelsDir, "Level *.tmx")){
			for(Path file : files){
				String xml = Files.readString(file, StandardCharsets.UTF_8);
				EditorLevel level = EditorTmxReader.read(file.getFileName().toString(), xml);
				Array<String> errors = EditorLevelValidator.validate(level);

				assertEquals(countObjects(xml), level.objects.size, file.toString());
				assertNotNull(level.getStart(), file.toString());
				assertNotNull(level.getExit(), file.toString());
				assertTrue(errors.isEmpty(), file + " validation errors: " + errors);
				assertTrue(EditorTmxWriter.write(level).contains("<objectgroup id=\"2\" name=\"Objects\">"));
				importedLevels++;
			}
		}
		assertEquals(5, importedLevels);
	}

	private static int countObjects(String xml){
		return xml.split("<object ", -1).length - 1;
	}
}
