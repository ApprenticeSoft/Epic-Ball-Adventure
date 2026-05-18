package utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.math.Vector2;
import org.junit.jupiter.api.Test;

public class WaterSplashSystemTest {
	@Test
	public void impactIntensityIncreasesWithSpeedMassAndSize(){
		float baseline = WaterImpact.calculateIntensity(2f, 2f, 0.05f, 0.6f);

		assertTrue(WaterImpact.calculateIntensity(8f, 8f, 0.05f, 0.6f) > baseline);
		assertTrue(WaterImpact.calculateIntensity(2f, 2f, 2f, 0.6f) > baseline);
		assertTrue(WaterImpact.calculateIntensity(2f, 2f, 0.05f, 3f) > baseline);
	}

	@Test
	public void impactIntensityIsClamped(){
		assertEquals(0.4f, WaterImpact.calculateIntensity(0f, 0f, 0f, 0f), 0.0001f);
		assertEquals(18f, WaterImpact.calculateIntensity(100f, 100f, 100f, 100f), 0.0001f);
	}

	@Test
	public void dropletMergesIntoWaterAsRipple(){
		SplashParticle particle = SplashParticle.droplet(new Vector2(1f, 2f), new Vector2(0f, -4f), 0.08f, 1f, 0.8f);

		particle.mergeWithWater(new Vector2(1.5f, 0.5f), 0f);

		assertFalse(particle.isAirborne());
		assertEquals(SplashParticleState.RIPPLE, particle.state);
		assertEquals(1.5f, particle.position.x, 0.0001f);
		assertEquals(0.5f, particle.position.y, 0.0001f);
	}

	@Test
	public void dropletFlattensAlongSurfaceTangent(){
		SplashParticle particle = SplashParticle.droplet(new Vector2(0f, 1f), new Vector2(0f, -2f), 0.1f, 1f, 0.8f);

		particle.flattenOnSurface(new Vector2(2f, 3f), new Vector2(0.70710677f, 0.70710677f));

		assertFalse(particle.isAirborne());
		assertEquals(SplashParticleState.FLATTENED, particle.state);
		assertEquals(-45f, particle.angleDegrees, 0.001f);
	}
}
