package editor;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.utils.Array;
import org.junit.jupiter.api.Test;

public class EditorLevelValidatorTest {
	@Test
	public void acceptsDefaultEditorLevel(){
		Array<String> errors = EditorLevelValidator.validate(new EditorLevel());

		assertTrue(errors.isEmpty());
	}

	@Test
	public void rejectsMissingRequiredObjects(){
		EditorLevel level = new EditorLevel(false);

		Array<String> errors = EditorLevelValidator.validate(level);

		assertContains(errors, "Missing Start object");
		assertContains(errors, "Missing Exit object");
	}

	@Test
	public void rejectsBrokenPlatformAndPulleyGroup(){
		EditorLevel level = new EditorLevel();
		EditorLevelObject platform = level.createObject(EditorObjectType.PLATFORM, 320, 320);
		platform.points.clear();
		platform.points.add(new com.badlogic.gdx.math.Vector2(0f, 0f));
		platform.properties.put("Width", "wide");
		EditorLevelObject pulley = level.createObject(EditorObjectType.POULIE, 640, 320);
		pulley.properties.put("Groupe", "7");

		Array<String> errors = EditorLevelValidator.validate(level);

		assertContains(errors, "Moving platform needs at least 2 path points");
		assertContains(errors, "Platform Width must be numeric");
		assertContains(errors, "Poulie group 7 must contain exactly 2 objects");
	}

	@Test
	public void rejectsObjectsOutsideWorldBounds(){
		EditorLevel level = new EditorLevel();
		level.createObject(EditorObjectType.SOLID, -320, 320);

		Array<String> errors = EditorLevelValidator.validate(level);

		assertFalse(errors.isEmpty());
		assertContains(errors, "Solid is outside world bounds");
	}

	private static void assertContains(Array<String> errors, String expected){
		for(String error : errors)
			if(error.equals(expected))
				return;
		throw new AssertionError("Expected error not found: " + expected + " in " + errors);
	}
}
