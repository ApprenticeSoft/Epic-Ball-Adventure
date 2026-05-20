package utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
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
	public void ballImpactCreatesVisibleWaveAmplitude(){
		float intensity = WaterImpact.calculateIntensity(8f, 8f, 0.05f, 1.6f);

		assertTrue(WaterSplashSystem.calculateWaveAmplitude(8f, 0.05f, 1.6f, intensity) > 0.75f);
	}

	@Test
	public void visibleWaveCountIncreasesWithSpeedMassAndSize(){
		int baseline = WaterSplashSystem.calculateVisibleWaveCount(2f, 0.05f, 0.6f, 1f);

		assertTrue(WaterSplashSystem.calculateVisibleWaveCount(7f, 0.05f, 0.6f, 1f) > baseline);
		assertTrue(WaterSplashSystem.calculateVisibleWaveCount(2f, 4f, 0.6f, 1f) > baseline);
		assertTrue(WaterSplashSystem.calculateVisibleWaveCount(2f, 0.05f, 3f, 1f) > baseline);
	}

	@Test
	public void visibleWaveCountIsClamped(){
		assertEquals(2, WaterSplashSystem.calculateVisibleWaveCount(0f, 0f, 0f, 0f));
		assertEquals(10, WaterSplashSystem.calculateVisibleWaveCount(100f, 100f, 100f, 100f));
	}

	@Test
	public void cinematicSplashScalesWithImpactEnergy(){
		int lightCount = WaterSplashSystem.calculateDropletCount(2f, 2f, 0.05f, 0.6f, 1f);
		int heavyCount = WaterSplashSystem.calculateDropletCount(24f, 28f, 18f, 4f, 18f);

		assertTrue(heavyCount > lightCount * 3);
		assertEquals(96, heavyCount);
		assertTrue(WaterSplashSystem.calculateDropletSpread(24f, 28f, 18f, 4f, 18f)
				> WaterSplashSystem.calculateDropletSpread(2f, 2f, 0.05f, 0.6f, 1f) * 4f);
		assertTrue(WaterSplashSystem.calculateDropletLaunchSpeed(24f, 18f, 4f, 18f)
				> WaterSplashSystem.calculateDropletLaunchSpeed(2f, 0.05f, 0.6f, 1f) * 2f);
		assertTrue(WaterSplashSystem.calculateDropletRadiusBase(18f, 4f, 18f)
				> WaterSplashSystem.calculateDropletRadiusBase(0.05f, 0.6f, 1f));
	}

	@Test
	public void bubblePlumeScalesWithImpactEnergy(){
		int lightCount = WaterSplashSystem.calculateBubblePlumeCount(2f, 2f, 0.05f, 0.6f, 1f);
		int heavyCount = WaterSplashSystem.calculateBubblePlumeCount(24f, 28f, 18f, 4f, 18f);

		assertTrue(heavyCount > lightCount * 4);
		assertEquals(79, heavyCount);
		assertTrue(WaterSplashSystem.calculateBubblePlumeSpread(24f, 28f, 18f, 4f, 18f)
				> WaterSplashSystem.calculateBubblePlumeSpread(2f, 2f, 0.05f, 0.6f, 1f) * 2f);
		assertTrue(WaterSplashSystem.calculateBubbleRadiusBase(18f, 4f, 18f)
				> WaterSplashSystem.calculateBubbleRadiusBase(0.05f, 0.6f, 1f));
	}

	@Test
	public void bubblePlumeCountIsClamped(){
		assertEquals(6, WaterSplashSystem.calculateBubblePlumeCount(0f, 0f, 0f, 0f, 0f));
		assertEquals(80, WaterSplashSystem.calculateBubblePlumeCount(100f, 100f, 100f, 100f, 100f));
	}

	@Test
	public void bubbleRadiusHasReadableMinimum(){
		assertTrue(WaterSplashSystem.calculateBubbleRadiusBase(0.02f, 0.1f, 0f) >= 0.18f);
	}

	@Test
	public void waterParticlesUsePlainCircleRegion(){
		assertEquals("PlainCircle", WaterSplashSystem.particleCircleRegionName());
		assertEquals("BubbleRing", WaterSplashSystem.bubbleRingRegionName());
	}

	@Test
	public void bubblePlumeDepthAvoidsImmediateSurfaceRemoval(){
		float radiusBase = 0.48f;
		float largestSpawnedRadius = radiusBase * 1.35f;
		float surfaceY = 1f;
		float localY = surfaceY - WaterSplashSystem.minimumBubblePlumeDepth(radiusBase);

		assertFalse(WaterSplashSystem.bubbleReachedSurface(localY, surfaceY, largestSpawnedRadius));
	}

	@Test
	public void bubbleTrailRequiresMovementAndScalesWithBody(){
		assertEquals(0f, WaterSplashSystem.calculateBubbleTrailRate(0.2f, 3f, 3f), 0.0001f);
		float lightRate = WaterSplashSystem.calculateBubbleTrailRate(1.2f, 0.05f, 0.6f);
		float heavyRate = WaterSplashSystem.calculateBubbleTrailRate(8f, 12f, 3f);

		assertTrue(lightRate > 0f);
		assertTrue(heavyRate > lightRate * 3f);
	}

	@Test
	public void bubbleSurfaceFadeOnlyDropsNearSurface(){
		assertEquals(1f, WaterSplashSystem.calculateBubbleSurfaceFade(2f, 0.08f), 0.0001f);
		assertTrue(WaterSplashSystem.calculateBubbleSurfaceFade(0.04f, 0.08f) < 0.25f);
		assertTrue(WaterSplashSystem.calculateBubbleSurfaceFade(0.22f, 0.14f) > 0.9f);
		assertTrue(WaterSplashSystem.bubbleReachedSurface(0.98f, 1f, 0.08f));
		assertFalse(WaterSplashSystem.bubbleReachedSurface(0.6f, 1f, 0.08f));
	}

	@Test
	public void bubbleRingColorIsBrightAndAlphaCappedByCaller(){
		Color ring = WaterSplashSystem.setBubbleRingColor(
				WaterSplashSystem.capToWaterAlpha(0.9f, 0.45f), new Color());
		Color shadow = WaterSplashSystem.setBubbleShadowColor(
				new Color(0f, 0.55f, 0.35f, 0.55f),
				WaterSplashSystem.capToWaterAlpha(0.8f, 0.45f), new Color());

		assertTrue(ring.r > 0.9f);
		assertEquals(1f, ring.g, 0.0001f);
		assertEquals(1f, ring.b, 0.0001f);
		assertEquals(0.45f, ring.a, 0.0001f);
		assertTrue(shadow.r < ring.r);
		assertTrue(shadow.g < ring.g);
		assertEquals(0.45f, shadow.a, 0.0001f);
	}

	@Test
	public void airBubbleRisesAndGrows(){
		WaterSplashSystem.AirBubble bubble = WaterSplashSystem.AirBubble.create(null,
				new Vector2(0f, 0f), new Vector2(0f, 0.4f), 0.08f, 0.5f, 4f);
		float initialRadius = bubble.renderRadius();

		bubble.update(0.5f);

		assertTrue(bubble.position.y > 0.2f);
		assertTrue(bubble.renderRadius() > initialRadius);
		assertTrue(bubble.renderAlpha(1f) > 0f);
	}

	@Test
	public void visibleWaveAlphaIsReadableAndCappedByWaterAlpha(){
		WaterSplashSystem.TravelingWave wave =
				new WaterSplashSystem.TravelingWave(0f, 1, 0.35f, 0.5f, 2f, 0.4f, 0f);

		float alpha = WaterSplashSystem.calculateWaveRenderAlpha(wave, 0.55f);

		assertTrue(alpha > 0.25f);
		assertTrue(alpha <= 0.55f);
	}

	@Test
	public void travelingWaveSpawnSpeedIsDoubled(){
		WaterSplashSystem.WaterSurfaceSimulation simulation =
				new WaterSplashSystem.WaterSurfaceSimulation(null, 3f);

		simulation.applyImpact(0f, 0.5f, 1f, 0.5f, 2);

		WaterSplashSystem.TravelingWave wave = simulation.travelingWaves.first();
		float previousSpeed = 1.85f + 0.5f * 1.05f + 1f * 0.08f;
		assertEquals(previousSpeed * 2f, wave.speed, 0.0001f);
	}

	@Test
	public void ballSizedImpactCreatesBroadTravelingWaves(){
		WaterSplashSystem.WaterSurfaceSimulation simulation =
				new WaterSplashSystem.WaterSurfaceSimulation(null, 2f);

		simulation.applyImpact(0f, 0.8f, 1.2f, 0.5f);

		assertTrue(simulation.travelingWaves.first().width >= 0.95f);
	}

	@Test
	public void travelingWaveBounceReducesSpeedAndSize(){
		WaterSplashSystem.TravelingWave wave =
				new WaterSplashSystem.TravelingWave(0.95f, 1, 0.6f, 0.5f, 10f, 0.4f, 0f);

		wave.update(0.02f, 1f);

		assertEquals(-1, wave.direction);
		assertEquals(8.5f, wave.speed, 0.0001f);
		assertEquals(0.368f, wave.width, 0.0001f);
		assertEquals(1, wave.bounceCount);
		assertFalse(wave.isExpired());
	}

	@Test
	public void mergingTravelingWaveKeepsMovingWhileFading(){
		WaterSplashSystem.TravelingWave wave =
				new WaterSplashSystem.TravelingWave(0f, 1, 0.03f, 0.5f, 2f, 0.4f, 0f);

		assertTrue(wave.update(1f / 60f, 2f));
		float mergeStartCenter = wave.centerX;
		float mergeStartAlpha = wave.visualAlpha();

		wave.update(0.5f, 2f);

		assertTrue(wave.centerX > mergeStartCenter);
		assertTrue(wave.visualAlpha() < mergeStartAlpha);
		assertFalse(wave.isExpired());
	}

	@Test
	public void lowAmplitudeTravelingWaveMergesBeforeExpiring(){
		WaterSplashSystem.TravelingWave wave =
				new WaterSplashSystem.TravelingWave(0f, 1, 0.03f, 0.5f, 2f, 0.4f, 0f);

		boolean startedMerge = wave.update(1f / 60f, 2f);
		float initialAlpha = wave.visualAlpha();
		wave.update(0.6f, 2f);

		assertTrue(startedMerge);
		assertFalse(wave.isExpired());
		assertTrue(wave.visualAlpha() < initialAlpha);

		wave.update(0.7f, 2f);

		assertTrue(wave.isExpired());
	}

	@Test
	public void travelingWaveVisualEnvelopeRoundsAndTapersEdges(){
		WaterSplashSystem.TravelingWave wave =
				new WaterSplashSystem.TravelingWave(0f, 1, 0.35f, 0.5f, 2f, 0.4f, 0f);

		assertEquals(1f, wave.visualEnvelopeAt(0f), 0.0001f);
		assertTrue(wave.visualEnvelopeAt(0.35f) > wave.visualEnvelopeAt(0.48f));
		assertEquals(0f, wave.visualEnvelopeAt(wave.visualLength()), 0.0001f);
	}

	@Test
	public void roundedWaveEnvelopeKeepsBroadShoulders(){
		assertEquals(1f, WaterSplashSystem.roundedWaveEnvelope(0f), 0.0001f);
		assertTrue(WaterSplashSystem.roundedWaveEnvelope(0.5f) > 0.6f);
		assertTrue(WaterSplashSystem.roundedWaveEnvelope(0.8f) > 0.2f);
		assertEquals(0f, WaterSplashSystem.roundedWaveEnvelope(1f), 0.0001f);
	}

	@Test
	public void refractionWaveStrengthTracksSurfaceMotion(){
		assertEquals(0f, WaterSplashSystem.calculateRefractionWaveStrength(0f, 0f), 0.0001f);
		assertTrue(WaterSplashSystem.calculateRefractionWaveStrength(0.4f, 0.2f) > 0.25f);
		assertEquals(1f, WaterSplashSystem.calculateRefractionWaveStrength(4f, 4f), 0.0001f);
	}

	@Test
	public void calmRefractionKeepsVisibleDistortion(){
		assertTrue(WaterRefractionRenderer.calculateDistortionPixels(0f) >= 5.5f);
		assertTrue(WaterRefractionRenderer.calculateDistortionPixels(0.6f)
				> WaterRefractionRenderer.calculateDistortionPixels(0f));
	}

	@Test
	public void mergingTravelingWaveTransfersResidualToSpringSurface(){
		WaterSplashSystem.WaterSurfaceSimulation simulation =
				new WaterSplashSystem.WaterSurfaceSimulation(null, 3f);
		simulation.travelingWaves.add(new WaterSplashSystem.TravelingWave(0f, 1, 0.03f, 0.5f, 2f, 0.4f, 0f));
		simulation.energy = 0.03f;

		simulation.update(1f / 60f);

		assertTrue(Math.abs(simulation.springDisplacementAt(0f)) > 0.0001f);
		assertEquals(1, simulation.travelingWaveCount());
	}

	@Test
	public void travelingWaveSpriteOverlayOnlyDrawsCrests(){
		WaterSplashSystem.TravelingWave crest =
				new WaterSplashSystem.TravelingWave(0f, 1, 0.35f, 0.5f, 2f, 0.4f, 0f, 1f);
		WaterSplashSystem.TravelingWave trough =
				new WaterSplashSystem.TravelingWave(0f, 1, 0.35f, 0.5f, 2f, 0.4f, 0f, -1f);

		assertTrue(crest.drawsSpriteOverlay());
		assertFalse(trough.drawsSpriteOverlay());
	}

	@Test
	public void crestOverlayUsesVisibleWaterColor(){
		Color water = new Color(0f, 0.53f, 0.32f, 0.55f);
		Color crest = WaterSplashSystem.setWaveColor(water, true, 0.55f, new Color());

		assertTrue(crest.r > water.r);
		assertTrue(crest.g > water.g);
		assertTrue(crest.b > water.b);
		assertEquals(0.55f, crest.a, 0.0001f);
	}

	@Test
	public void waterSurfaceCollisionUsesDropletRadius(){
		WaterSplashSystem.WaterSurfaceHit hit = new WaterSplashSystem.WaterSurfaceHit();

		boolean collided = WaterSplashSystem.findWaterSurfaceHitLocal(new Vector2(0f, 1.2f),
				new Vector2(0f, 1.05f), 0.1f, 2f, localX -> 1f, hit);

		assertTrue(collided);
		assertEquals(0.6667f, hit.fraction, 0.001f);
		assertEquals(0f, hit.localX, 0.0001f);
	}

	@Test
	public void fastDropletCrossesWaterSurfaceBeforePoolBottom(){
		WaterSplashSystem.WaterSurfaceHit hit = new WaterSplashSystem.WaterSurfaceHit();

		boolean collided = WaterSplashSystem.findWaterSurfaceHitLocal(new Vector2(0f, 4f),
				new Vector2(0f, -2f), 0.08f, 2f, localX -> 1f, hit);

		assertTrue(collided);
		assertTrue(hit.fraction < 0.5f);
		assertEquals(0f, hit.localX, 0.0001f);
	}

	@Test
	public void submergedDescendingDropletMergesAtWaterSurface(){
		WaterSplashSystem.WaterSurfaceHit hit = new WaterSplashSystem.WaterSurfaceHit();

		boolean collided = WaterSplashSystem.findWaterSurfaceHitLocal(new Vector2(0f, 1.05f),
				new Vector2(0f, 0.5f), 0.1f, 2f, localX -> 1f, hit);

		assertTrue(collided);
		assertEquals(0f, hit.fraction, 0.0001f);
		assertEquals(0f, hit.localX, 0.0001f);
	}

	@Test
	public void overlappingDropletMovingAwayFromWaterDoesNotMergeImmediately(){
		WaterSplashSystem.WaterSurfaceHit hit = new WaterSplashSystem.WaterSurfaceHit();

		boolean collided = WaterSplashSystem.findWaterSurfaceHitLocal(new Vector2(0f, 1.05f),
				new Vector2(0f, 1.2f), 0.1f, 2f, localX -> 1f, hit);

		assertFalse(collided);
	}

	@Test
	public void waterSurfaceCollisionUsesFineAdaptiveSampling(){
		assertEquals(8, WaterSplashSystem.waterSurfaceCollisionSteps(0.01f));
		assertEquals(96, WaterSplashSystem.waterSurfaceCollisionSteps(12f));
	}

	private WaterSplashSystem.RippleSegment[] rippleSegments(){
		return new WaterSplashSystem.RippleSegment[]{
				new WaterSplashSystem.RippleSegment(),
				new WaterSplashSystem.RippleSegment(),
				new WaterSplashSystem.RippleSegment()
		};
	}

	@Test
	public void surfaceSimulationImpactDisplacesNearestSamples(){
		WaterSplashSystem.WaterSurfaceSimulation simulation =
				new WaterSplashSystem.WaterSurfaceSimulation(null, 2f);
		int center = simulation.nearestSample(0f);

		simulation.applyImpact(0f, 0.2f, 0.5f, 0.4f);

		assertTrue(simulation.hasMotion());
		assertTrue(simulation.displacement(center) < 0f);
	}

	@Test
	public void surfaceImpactSpawnsTravelingWavesBothDirections(){
		WaterSplashSystem.WaterSurfaceSimulation simulation =
				new WaterSplashSystem.WaterSurfaceSimulation(null, 2f);

		simulation.applyImpact(0f, 0.8f, 1.2f, 0.5f);

		assertTrue(simulation.travelingWaveCount() >= 4);
		assertTrue(simulation.hasTravelingWaveDirection(-1));
		assertTrue(simulation.hasTravelingWaveDirection(1));
	}

	@Test
	public void surfaceImpactSpawnsRequestedVisibleWaveCount(){
		WaterSplashSystem.WaterSurfaceSimulation simulation =
				new WaterSplashSystem.WaterSurfaceSimulation(null, 2f);

		simulation.applyImpact(0f, 0.8f, 1.2f, 0.5f, 8);

		assertEquals(8, simulation.travelingWaveCount());
		assertTrue(simulation.hasTravelingWaveDirection(-1));
		assertTrue(simulation.hasTravelingWaveDirection(1));
	}

	@Test
	public void travelingWavePacketsMoveAwayFromImpact(){
		WaterSplashSystem.WaterSurfaceSimulation simulation =
				new WaterSplashSystem.WaterSurfaceSimulation(null, 2f);

		simulation.applyImpact(0f, 0.8f, 1.2f, 0.5f);
		float initialRightCenter = firstWaveCenter(simulation, 1);
		for(int i = 0; i < 5; i++)
			simulation.update(1f / 60f);

		assertTrue(firstWaveCenter(simulation, 1) > initialRightCenter);
	}

	@Test
	public void travelingWaveReflectsAtWaterBoundary(){
		WaterSplashSystem.TravelingWave wave =
				new WaterSplashSystem.TravelingWave(1.95f, 1, 0.5f, 0.5f, 2.5f, 1.5f, -1.5708f);

		wave.update(0.5f, 2f);

		assertEquals(-1, wave.direction);
		assertTrue(wave.centerX <= 2f);
		assertTrue(wave.centerX >= -2f);
		assertEquals(1, wave.bounceCount);
		assertTrue(wave.amplitude < 0.5f);
	}

	@Test
	public void surfaceSimulationShowsMultipleCrestsAndTroughsAfterImpact(){
		WaterSplashSystem.WaterSurfaceSimulation simulation =
				new WaterSplashSystem.WaterSurfaceSimulation(null, 2f);

		simulation.applyImpact(0f, 0.8f, 1.2f, 0.5f);

		assertTrue(countSurfaceSignChanges(simulation, 0.015f) >= 4);
	}

	@Test
	public void surfaceSimulationImpactCreatesVisibleMeshDisplacement(){
		WaterSplashSystem.WaterSurfaceSimulation simulation =
				new WaterSplashSystem.WaterSurfaceSimulation(null, 2f);
		int center = simulation.nearestSample(0f);

		simulation.applyImpact(0f, 0.8f, 1.6f, 0.5f);

		assertTrue(Math.abs(simulation.displacement(center)) > 0.5f);
	}

	@Test
	public void surfaceSimulationPropagatesToNeighboringSamples(){
		WaterSplashSystem.WaterSurfaceSimulation simulation =
				new WaterSplashSystem.WaterSurfaceSimulation(null, 2f);
		float targetX = simulation.localX(simulation.nearestSample(1.75f));

		simulation.applyImpact(0f, 0.25f, 0.25f, 0.4f);
		float initialTarget = Math.abs(simulation.displacementAt(targetX));
		float peakTarget = initialTarget;
		for(int i = 0; i < 60; i++){
			simulation.update(1f / 60f);
			peakTarget = Math.max(peakTarget, Math.abs(simulation.displacementAt(targetX)));
		}

		assertTrue(peakTarget > initialTarget + 0.025f);
	}

	@Test
	public void surfaceSimulationDampsTowardRest(){
		WaterSplashSystem.WaterSurfaceSimulation simulation =
				new WaterSplashSystem.WaterSurfaceSimulation(null, 2f);

		simulation.applyImpact(0f, 0.25f, 0.25f, 0.4f);
		float initialEnergy = simulation.energy;
		for(int i = 0; i < 180; i++)
			simulation.update(1f / 60f);

		assertTrue(simulation.energy < initialEnergy);
	}

	@Test
	public void surfaceSimulationBouncesSeveralTimesBeforeSettling(){
		WaterSplashSystem.WaterSurfaceSimulation simulation =
				new WaterSplashSystem.WaterSurfaceSimulation(null, 2f);
		int center = simulation.nearestSample(0f);

		simulation.applyImpact(0f, 0.8f, 1.2f, 0.5f);
		float initialPeak = simulation.maxAbsDisplacement();
		int lastSign = Math.round(Math.signum(simulation.displacement(center)));
		int signChanges = 0;
		float latePeak = 0f;
		for(int i = 0; i < 360; i++){
			simulation.update(1f / 60f);
			float current = simulation.displacement(center);
			if(Math.abs(current) > 0.02f){
				int currentSign = Math.round(Math.signum(current));
				if(currentSign != lastSign){
					signChanges++;
					lastSign = currentSign;
				}
			}
			if(i > 180)
				latePeak = Math.max(latePeak, simulation.maxAbsDisplacement());
		}

		assertTrue(signChanges >= 3);
		assertTrue(latePeak < initialPeak);
	}

	@Test
	public void surfaceSimulationStaysVisibleWhileEquilibrating(){
		WaterSplashSystem.WaterSurfaceSimulation simulation =
				new WaterSplashSystem.WaterSurfaceSimulation(null, 2f);

		simulation.applyImpact(0f, 0.45f, 0.8f, 0.5f);
		for(int i = 0; i < 360; i++)
			simulation.update(1f / 60f);

		assertTrue(simulation.hasMotion());
	}

	@Test
	public void surfaceSimulationEventuallyEquilibrates(){
		WaterSplashSystem.WaterSurfaceSimulation simulation =
				new WaterSplashSystem.WaterSurfaceSimulation(null, 2f);

		simulation.applyImpact(0f, 0.45f, 0.8f, 0.5f);
		for(int i = 0; i < 2400; i++)
			simulation.update(1f / 60f);

		assertFalse(simulation.hasMotion());
		assertEquals(0f, simulation.maxAbsDisplacement(), 0.0001f);
	}

	@Test
	public void travelingWavesEventuallyExpire(){
		WaterSplashSystem.WaterSurfaceSimulation simulation =
				new WaterSplashSystem.WaterSurfaceSimulation(null, 2f);

		simulation.applyImpact(0f, 0.8f, 1.2f, 0.5f);
		for(int i = 0; i < 1200; i++)
			simulation.update(1f / 60f);

		assertEquals(0, simulation.travelingWaveCount());
	}

	@Test
	public void surfaceSimulationSamplesStayInsideWaterBounds(){
		WaterSplashSystem.WaterSurfaceSimulation simulation =
				new WaterSplashSystem.WaterSurfaceSimulation(null, 2f);

		assertEquals(-2f, simulation.localX(0), 0.0001f);
		assertEquals(2f, simulation.localX(simulation.sampleCount - 1), 0.0001f);
		assertEquals(simulation.sampleCount - 1, simulation.nearestSample(99f));
	}

	@Test
	public void surfaceSimulationInterpolatesDisplacementAtLocalX(){
		WaterSplashSystem.WaterSurfaceSimulation simulation =
				new WaterSplashSystem.WaterSurfaceSimulation(null, 2f);
		int center = simulation.nearestSample(0f);
		simulation.displacements[center] = 0.2f;
		simulation.displacements[center + 1] = 0.8f;
		float halfwayX = (simulation.localX(center) + simulation.localX(center + 1)) * 0.5f;

		assertEquals(0.5f, simulation.displacementAt(halfwayX), 0.0001f);
	}

	@Test
	public void waterRenderPassPlacesOnlyDynamicBodiesBehindWater(){
		assertTrue(LecteurCarte.isBehindWaterBodyType(BodyType.DynamicBody));
		assertFalse(LecteurCarte.isBehindWaterBodyType(BodyType.StaticBody));
		assertFalse(LecteurCarte.isBehindWaterBodyType(BodyType.KinematicBody));
	}

	@Test
	public void waterSurfaceMeshIsFlatWithoutSimulation(){
		int sampleCount = WaterSplashSystem.calculateSurfaceSampleCount(2f);
		float[] vertices = new float[(sampleCount + 2) * 2];

		int count = WaterSurfaceRenderer.buildLocalPolygonVertices(2f, 1f, null, vertices);

		assertEquals((sampleCount + 2) * 2, count);
		assertEquals(-2f, vertices[0], 0.0001f);
		assertEquals(-1f, vertices[1], 0.0001f);
		assertEquals(2f, vertices[2], 0.0001f);
		assertEquals(-1f, vertices[3], 0.0001f);
		for(int i = 5; i < count; i += 2)
			assertEquals(1f, vertices[i], 0.0001f);
	}

	@Test
	public void waterSurfaceMeshUsesSimulationDisplacement(){
		WaterSplashSystem.WaterSurfaceSimulation simulation =
				new WaterSplashSystem.WaterSurfaceSimulation(null, 2f);
		int center = simulation.nearestSample(0f);
		float[] vertices = new float[(simulation.sampleCount + 2) * 2];

		simulation.applyImpact(0f, 0.8f, 1.2f, 0.5f);
		WaterSurfaceRenderer.buildLocalPolygonVertices(2f, 1f, simulation, vertices);
		int centerVertexIndex = 2 + (simulation.sampleCount - 1 - center);
		float centerSurfaceY = vertices[centerVertexIndex * 2 + 1];

		assertTrue(centerSurfaceY < 1f);
	}

	@Test
	public void waterSurfaceMeshClampsBeforeItCanInvert(){
		WaterSplashSystem.WaterSurfaceSimulation simulation =
				new WaterSplashSystem.WaterSurfaceSimulation(null, 2f);
		float[] vertices = new float[(simulation.sampleCount + 2) * 2];
		simulation.displacements[simulation.nearestSample(0f)] = -20f;

		int count = WaterSurfaceRenderer.buildLocalPolygonVertices(2f, 1f, simulation, vertices);

		for(int i = 5; i < count; i += 2)
			assertTrue(vertices[i] >= 0.35f);
	}

	private float firstWaveCenter(WaterSplashSystem.WaterSurfaceSimulation simulation, int direction){
		for(WaterSplashSystem.TravelingWave wave : simulation.travelingWaves)
			if(wave.direction == direction)
				return wave.centerX;
		return 0f;
	}

	private int countSurfaceSignChanges(WaterSplashSystem.WaterSurfaceSimulation simulation, float threshold){
		int previousSign = 0;
		int signChanges = 0;
		for(int i = 0; i < simulation.sampleCount; i++){
			float displacement = simulation.displacement(i);
			if(Math.abs(displacement) <= threshold)
				continue;
			int sign = displacement > 0f ? 1 : -1;
			if(previousSign != 0 && sign != previousSign)
				signChanges++;
			previousSign = sign;
		}
		return signChanges;
	}
}
