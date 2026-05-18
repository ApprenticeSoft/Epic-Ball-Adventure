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
	public void dropletAlphaUsesWaterAlphaRange(){
		assertEquals(0.25f, WaterSplashSystem.randomDropAlpha(0.5f, 0.25f), 0.0001f);
		assertEquals(0.375f, WaterSplashSystem.randomDropAlpha(0.5f, 0.75f), 0.0001f);
		assertEquals(0.5f, WaterSplashSystem.randomDropAlpha(0.5f, 1.5f), 0.0001f);
	}

	@Test
	public void renderedParticleAlphaCannotExceedWaterAlpha(){
		assertEquals(0.45f, WaterSplashSystem.capToWaterAlpha(0.9f, 0.45f), 0.0001f);
		assertEquals(0.2f, WaterSplashSystem.capToWaterAlpha(0.2f, 0.45f), 0.0001f);
		assertEquals(0f, WaterSplashSystem.capToWaterAlpha(-0.1f, 0.45f), 0.0001f);
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
	public void airborneDropletKeepsIntensityUntilContact(){
		SplashParticle particle = SplashParticle.droplet(new Vector2(1f, 2f), new Vector2(0f, -4f), 0.08f, 1f, 0.8f);

		particle.updateAge(1.5f);

		assertTrue(particle.isAirborne());
		assertFalse(particle.isExpired());
		assertEquals(0.8f, particle.renderAlpha(), 0.0001f);
		assertEquals(0.16f, particle.renderLength(), 0.0001f);
	}

	@Test
	public void dropletFlattensAlongSurfaceTangent(){
		SplashParticle particle = SplashParticle.droplet(new Vector2(0f, 1f), new Vector2(0f, -2f), 0.1f, 1f, 0.8f);

		particle.flattenOnSurface(new Vector2(2f, 3f), new Vector2(0.70710677f, 0.70710677f));

		assertFalse(particle.isAirborne());
		assertEquals(SplashParticleState.FLATTENED, particle.state);
		assertEquals(-45f, particle.angleDegrees, 0.001f);
	}

	@Test
	public void flattenedDropletMorphsFromRoundToSurfaceSplat(){
		SplashParticle particle = SplashParticle.droplet(new Vector2(0f, 1f), new Vector2(0f, -2f), 0.1f, 1f, 0.8f);

		particle.flattenOnSurface(new Vector2(2f, 3f), new Vector2(0f, 1f));
		float initialLength = particle.renderLength();
		float initialThickness = particle.renderThickness();
		particle.updateAge(0.35f);

		assertTrue(particle.renderLength() > initialLength);
		assertTrue(particle.renderThickness() < initialThickness);
		assertTrue(particle.renderAlpha() < 0.5f);
	}

	@Test
	public void rippleMainSegmentIsClippedInsideWaterBounds(){
		WaterSplashSystem.RippleSegment[] segments = rippleSegments();

		int count = WaterSplashSystem.collectRippleSegments(-1.8f, 1.2f, 2f, segments);

		assertEquals(2, count);
		assertEquals(-2f, segments[0].start, 0.0001f);
		assertEquals(-1.2f, segments[0].end, 0.0001f);
		assertEquals(1f, segments[0].alphaScale, 0.0001f);
	}

	@Test
	public void rippleBounceSegmentStaysInsideWaterBounds(){
		WaterSplashSystem.RippleSegment[] segments = rippleSegments();

		int count = WaterSplashSystem.collectRippleSegments(1.8f, 1.2f, 2f, segments);

		assertEquals(2, count);
		assertEquals(1.2f, segments[0].start, 0.0001f);
		assertEquals(2f, segments[0].end, 0.0001f);
		assertEquals(1.6f, segments[1].start, 0.0001f);
		assertEquals(2f, segments[1].end, 0.0001f);
		assertTrue(segments[1].alphaScale < segments[0].alphaScale);
	}

	@Test
	public void waveAmplitudeIncreasesWithSpeedMassAndSize(){
		float baseline = WaterSplashSystem.calculateWaveAmplitude(2f, 0.05f, 0.6f, 1f);

		assertTrue(WaterSplashSystem.calculateWaveAmplitude(7f, 0.05f, 0.6f, 1f) > baseline);
		assertTrue(WaterSplashSystem.calculateWaveAmplitude(2f, 2f, 0.6f, 1f) > baseline);
		assertTrue(WaterSplashSystem.calculateWaveAmplitude(2f, 0.05f, 3f, 1f) > baseline);
	}

	@Test
	public void waveBoundsStayInsideWaterBoundsNearEdges(){
		WaterSplashSystem.RippleSegment[] segments = rippleSegments();

		int count = WaterSplashSystem.collectWaveBounds(1.8f, 1.1f, 2f, segments);

		assertEquals(2, count);
		assertEquals(0.7f, segments[0].start, 0.0001f);
		assertEquals(2f, segments[0].end, 0.0001f);
		assertTrue(segments[1].start >= -2f);
		assertTrue(segments[1].end <= 2f);
		assertTrue(segments[1].alphaScale < segments[0].alphaScale);
	}

	private WaterSplashSystem.RippleSegment[] rippleSegments(){
		return new WaterSplashSystem.RippleSegment[]{
				new WaterSplashSystem.RippleSegment(),
				new WaterSplashSystem.RippleSegment(),
				new WaterSplashSystem.RippleSegment()
		};
	}
}
