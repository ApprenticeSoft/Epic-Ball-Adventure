package utils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import box2dLight.Spinor;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.Pools;

import org.junit.jupiter.api.Test;

public class Box2DLightCompatibilityTest {
	@Test
	public void registersPoolsRequiredByChainLight(){
		Box2DLightCompatibility.ensurePools();

		assertDoesNotThrow(() -> {
			Vector2 vector = Pools.obtain(Vector2.class);
			FloatArray floats = Pools.obtain(FloatArray.class);
			Spinor spinor = Pools.obtain(Spinor.class);
			Pools.free(vector);
			Pools.free(floats);
			Pools.free(spinor);
		});
	}
}
