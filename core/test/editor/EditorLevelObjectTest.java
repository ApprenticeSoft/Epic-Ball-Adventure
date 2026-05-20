package editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import org.junit.jupiter.api.Test;

class EditorLevelObjectTest {
	private static final float EPSILON = 0.05f;

	@Test
	void rotatedRectangleUsesGameplayVisualOrigin(){
		EditorLevelObject object = new EditorLevelObject(EditorObjectType.SOLID, 100f, 200f, 80f, 40f);
		object.rotation = 30f;
		float angle = -object.rotation * MathUtils.degreesToRadians;
		float expectedDrawX = object.x - object.width / 2f
				+ object.width / 2f * MathUtils.cos(angle)
				+ object.height / 2f * MathUtils.sin(angle);
		float expectedDrawY = object.y + object.height / 2f
				+ object.width / 2f * MathUtils.sin(angle)
				- object.height / 2f * MathUtils.cos(angle);

		assertEquals(expectedDrawX, object.getVisualDrawX(), EPSILON);
		assertEquals(expectedDrawY, object.getVisualDrawY(), EPSILON);
	}

	@Test
	void rotatedRectangleLocalWorldTransformsRoundTrip(){
		EditorLevelObject object = new EditorLevelObject(EditorObjectType.SPRING, 64f, 96f, 120f, 36f);
		object.rotation = -18f;
		Vector2 world = new Vector2();
		Vector2 local = new Vector2();

		object.visualLocalToWorld(119f, 35f, world);
		object.worldToVisualLocal(world.x, world.y, local);

		assertEquals(119f, local.x, EPSILON);
		assertEquals(35f, local.y, EPSILON);
		assertTrue(object.contains(world.x, world.y));
	}

	@Test
	void copyPreservesSnapMode(){
		EditorLevelObject object = new EditorLevelObject(EditorObjectType.SOLID, 0f, 0f, 32f, 32f);
		object.snapMode = EditorLevelObject.SnapMode.FREE;

		assertEquals(EditorLevelObject.SnapMode.FREE, object.copy().snapMode);
	}
}
