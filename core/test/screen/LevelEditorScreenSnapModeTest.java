package screen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class LevelEditorScreenSnapModeTest {
	@Test
	void rotationFieldDoesNotChangeSnapMode() throws IOException{
		String source = levelEditorSource();
		int rotationStart = source.indexOf("addNumberField(leftTable, \"Rotation\"");
		int rotationEnd = source.indexOf("String[] propertyNames", rotationStart);
		String rotationBlock = source.substring(rotationStart, rotationEnd);

		assertFalse(rotationBlock.contains("snapMode"), "Rotation edits must preserve the selected object's snap mode.");
	}

	@Test
	void onlySnapToggleChangesSelectedObjectSnapMode() throws IOException{
		String source = levelEditorSource();
		List<Integer> assignments = assignmentIndexesOf(source, "selectedObject.snapMode");

		assertEquals(1, assignments.size(), "Only the explicit snap toggle should assign selectedObject.snapMode.");
		String snapToggleBlock = source.substring(source.indexOf("private void addSnapButton"), source.indexOf("private TextField addTextField"));
		assertTrue(snapToggleBlock.contains("selectedObject.snapMode ="));
	}

	private static String levelEditorSource() throws IOException{
		for(Path path : List.of(
				Path.of("src/screen/LevelEditorScreen.java"),
				Path.of("core/src/screen/LevelEditorScreen.java"),
				Path.of("../core/src/screen/LevelEditorScreen.java"))){
			if(Files.exists(path))
				return Files.readString(path);
		}
		throw new IOException("LevelEditorScreen.java not found from test working directory.");
	}

	private static List<Integer> assignmentIndexesOf(String source, String fieldName){
		List<Integer> indexes = new ArrayList<Integer>();
		Matcher matcher = Pattern.compile(Pattern.quote(fieldName) + "\\s*=[^=]").matcher(source);
		while(matcher.find())
			indexes.add(matcher.start());
		return indexes;
	}
}
