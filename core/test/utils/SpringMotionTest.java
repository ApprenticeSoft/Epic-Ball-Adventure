package utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import org.junit.jupiter.api.Test;

public class SpringMotionTest {
	@Test
	public void worldVelocityRotatesLocalPowerBySpringBodyAngle(){
		Vector2 output = new Vector2();

		SpringMotion.worldVelocity(0f, 45f, -MathUtils.PI / 2f, output);
		assertEquals(45f, output.x, 0.0001f);
		assertEquals(0f, output.y, 0.0001f);

		SpringMotion.worldVelocity(0f, 45f, MathUtils.PI / 2f, output);
		assertEquals(-45f, output.x, 0.0001f);
		assertEquals(0f, output.y, 0.0001f);
	}

	@Test
	public void localDisplacementMeasuresExtensionInSpringSpace(){
		Vector2 output = new Vector2();

		SpringMotion.localDisplacement(new Vector2(12f, 4f), new Vector2(4f, 4f), -MathUtils.PI / 2f, output);
		assertEquals(0f, output.x, 0.0001f);
		assertEquals(8f, output.y, 0.0001f);
	}
}
