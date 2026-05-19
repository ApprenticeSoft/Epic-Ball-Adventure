package utils;

import bodies.Eau;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.RayCastCallback;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;

public class WaterSplashSystem {
	private static final int MAX_PARTICLES = 320;
	private static final float VISUAL_GRAVITY = 34f;
	private static final float MIN_SPLASH_SPEED = 1.2f;
	private static final float MIN_SPLASH_TOTAL_SPEED = 2.4f;
	private static final float MIN_RAY_DISTANCE2 = 0.0004f;
	private static final float MAX_AIRBORNE_AGE = 12f;
	private static final float AIRBORNE_SAFETY_MARGIN = 18f;
	private static final float RIPPLE_BOUNCE_ALPHA = 0.45f;
	private static final float SURFACE_SAMPLE_SPACING = 0.085f;
	private static final float WATER_SURFACE_COLLISION_SAMPLE_SPACING = 0.06f;
	private static final float SURFACE_STIFFNESS = 15f;
	private static final float SURFACE_DAMPING = 0.58f;
	private static final float SURFACE_SPREAD = 20f;
	private static final float SURFACE_EDGE_REFLECTION_DAMPING = 0.82f;
	private static final float SURFACE_SETTLE_THRESHOLD = 0.0018f;
	private static final float SURFACE_MAX_DISPLACEMENT = 1.55f;
	private static final float SURFACE_MAX_VELOCITY = 18f;
	private static final int MIN_VISIBLE_WAVES = 2;
	private static final int MAX_VISIBLE_WAVES = 10;
	private static final int MAX_TRAVELING_WAVES = 48;
	private static final float TRAVELING_WAVE_BASE_SPEED = 1.85f;
	private static final float TRAVELING_WAVE_SPEED_SCALE = 1.05f;
	private static final float TRAVELING_WAVE_SPEED_MULTIPLIER = 2f;
	private static final float TRAVELING_WAVE_BOUNCE_DAMPING = 0.76f;
	private static final float TRAVELING_WAVE_BOUNCE_SPEED_DAMPING = 0.85f;
	private static final float TRAVELING_WAVE_BOUNCE_SIZE_DAMPING = 0.92f;
	private static final float TRAVELING_WAVE_DAMPING_PER_SECOND = 0.935f;
	private static final float TRAVELING_WAVE_MERGE_AMPLITUDE = 0.035f;
	private static final float TRAVELING_WAVE_VISUAL_FADE_AMPLITUDE = 0.09f;
	private static final float TRAVELING_WAVE_MERGE_DURATION = 1.2f;
	private static final float TRAVELING_WAVE_MERGE_TRANSFER = 0.28f;
	private static final float TRAVELING_WAVE_MAX_AGE = 18f;
	private static final float VISUAL_WAVE_MIN_ALPHA = 0.34f;
	private static final float VISUAL_WAVE_MAX_ALPHA = 1f;
	private static final float VISUAL_WAVE_MIN_THICKNESS = 0.075f;
	private static final float VISUAL_WAVE_MAX_THICKNESS = 0.24f;
	private static final float VISUAL_WAVE_SEGMENT_SPACING = SURFACE_SAMPLE_SPACING * 0.45f;
	private static final int MIN_DROPLETS_PER_IMPACT = 8;
	private static final int MAX_DROPLETS_PER_IMPACT = 96;
	private static final float MAX_DROPLET_SPREAD = 28f;
	private static final int MAX_SURFACE_SAMPLES = 192;

	private final World world;
	private final Array<Eau> waters;
	private final Array<SplashParticle> particles = new Array<SplashParticle>();
	private final Array<WaterSurfaceSimulation> surfaceSimulations = new Array<WaterSurfaceSimulation>();
	private final Vector2 previousPosition = new Vector2();
	private final Vector2 rayHitPoint = new Vector2();
	private final Vector2 rayHitNormal = new Vector2();
	private final Vector2 drawPoint = new Vector2();
	private final Vector2 mergeSurfacePoint = new Vector2();
	private final Vector2 rippleStartPoint = new Vector2();
	private final Vector2 rippleEndPoint = new Vector2();
	private final Vector2 surfaceAngleStartPoint = new Vector2();
	private final Vector2 surfaceAngleEndPoint = new Vector2();
	private final Color waveColor = new Color();
	private final RippleSegment[] rippleSegments = new RippleSegment[]{
			new RippleSegment(), new RippleSegment(), new RippleSegment()
	};
	private float rayHitFraction;
	private float waterHitFraction;
	private float waterHitLocalX;
	private boolean rayHit;
	private Eau waterHit;
	private Eau sampledWater;
	private final WaterSurfaceHit localWaterHit = new WaterSurfaceHit();
	private final SurfaceSampler waterSurfaceSampler = new SurfaceSampler() {
		@Override
		public float surfaceLocalY(float localX) {
			return WaterSplashSystem.this.surfaceLocalY(sampledWater, localX);
		}
	};

	private final RayCastCallback hardSurfaceRayCast = new RayCastCallback() {
		@Override
		public float reportRayFixture(Fixture fixture, Vector2 point, Vector2 normal, float fraction) {
			if(!isHardSurface(fixture))
				return -1f;
			if(fraction < rayHitFraction){
				rayHit = true;
				rayHitFraction = fraction;
				rayHitPoint.set(point);
				rayHitNormal.set(normal);
			}
			return fraction;
		}
	};

	public WaterSplashSystem(World world, Array<Eau> waters){
		this.world = world;
		this.waters = waters;
	}

	public void splash(Eau water, Fixture impactFixture, Contact contact){
		if(water == null || impactFixture == null || impactFixture.isSensor())
			return;
		WaterImpact impact = WaterImpact.fromFixture(water, impactFixture, contact);
		if(impact.downwardSpeed < MIN_SPLASH_SPEED && impact.totalSpeed < MIN_SPLASH_TOTAL_SPEED)
			return;
		spawnImpact(impact, water);
	}

	public void update(float delta){
		for(WaterSurfaceSimulation simulation : surfaceSimulations)
			simulation.update(delta);
		for(int i = particles.size - 1; i >= 0; i--){
			SplashParticle particle = particles.get(i);
			if(particle.isAirborne()){
				updateDroplet(particle, delta);
				if(particle.isAirborne() && shouldDiscardAirborne(particle)){
					particles.removeIndex(i);
					continue;
				}
			}
			else{
				particle.updateAge(delta);
			}

			if(particle.isExpired())
				particles.removeIndex(i);
		}
	}

	public void draw(SpriteBatch batch, TextureAtlas textureAtlas, Color waterColor){
		if(particles.size == 0)
			return;
		TextureRegion dropRegion = textureAtlas.findRegion("BallColor");
		TextureRegion flatRegion = textureAtlas.findRegion("WhiteSquare");
		for(SplashParticle particle : particles){
			float alpha = cappedRenderAlpha(particle, waterColor);
			if(alpha <= 0f)
				continue;
			batch.setColor(waterColor.r, waterColor.g, waterColor.b, alpha);
			if(particle.state == SplashParticleState.RIPPLE && flatRegion != null)
				drawRippleParticle(batch, flatRegion, particle, waterColor);
			else if(particle.isAirborne() && dropRegion != null)
				drawDroplet(batch, dropRegion, particle);
			else if(flatRegion != null)
				drawFlatParticle(batch, flatRegion, particle);
		}
		batch.setColor(1, 1, 1, 1);
	}

	public void drawRipples(SpriteBatch batch, TextureAtlas textureAtlas, Color waterColor){
		TextureRegion flatRegion = textureAtlas.findRegion("WhiteSquare");
		if(flatRegion == null)
			return;
		drawTravelingWaves(batch, flatRegion, waterColor);
		for(SplashParticle particle : particles){
			if(particle.state != SplashParticleState.RIPPLE)
				continue;
			drawRippleParticle(batch, flatRegion, particle, waterColor);
		}
		batch.setColor(1, 1, 1, 1);
	}

	public void drawDropletsAndSplats(SpriteBatch batch, TextureAtlas textureAtlas, Color waterColor){
		if(particles.size == 0)
			return;
		TextureRegion dropRegion = textureAtlas.findRegion("BallColor");
		TextureRegion flatRegion = textureAtlas.findRegion("WhiteSquare");
		for(SplashParticle particle : particles){
			if(particle.state == SplashParticleState.RIPPLE)
				continue;
			TextureRegion region = particle.isAirborne() ? dropRegion : flatRegion;
			if(region != null)
				drawParticle(batch, region, particle, waterColor);
		}
		batch.setColor(1, 1, 1, 1);
	}

	public void clear(){
		particles.clear();
		surfaceSimulations.clear();
	}

	int getParticleCount(){
		return particles.size;
	}

	private void spawnImpact(WaterImpact impact, Eau water){
		float waterAlpha = waterAlpha(water);
		applySurfaceImpact(impact, water, waterAlpha);

		int dropletCount = calculateDropletCount(impact.downwardSpeed, impact.totalSpeed, impact.mass, impact.size,
				impact.intensity);
		float radiusBase = calculateDropletRadiusBase(impact.mass, impact.size, impact.intensity);
		float spread = calculateDropletSpread(impact.downwardSpeed, impact.totalSpeed, impact.mass, impact.size,
				impact.intensity);
		float launchSpeed = calculateDropletLaunchSpeed(impact.downwardSpeed, impact.mass, impact.size,
				impact.intensity);
		Vector2 normal = new Vector2(impact.surfaceNormal).nor();
		Vector2 tangent = new Vector2(normal.y, -normal.x);
		if(tangent.isZero())
			tangent.set(1, 0);
		tangent.nor();

		for(int i = 0; i < dropletCount; i++){
			float side = MathUtils.random(-spread, spread) + impact.velocity.dot(tangent) * 0.22f;
			float up = MathUtils.random(launchSpeed * 0.72f, launchSpeed * 1.34f);
			Vector2 velocity = new Vector2(tangent).scl(side).mulAdd(normal, up);
			Vector2 position = new Vector2(impact.point)
					.mulAdd(normal, 0.08f + radiusBase * 0.4f)
					.mulAdd(tangent, MathUtils.random(-impact.size * 0.32f, impact.size * 0.32f));
			float radius = radiusBase * MathUtils.random(0.65f, 1.35f);
			float alpha = randomDropAlpha(waterAlpha, MathUtils.random(0.5f, 1f));
			particles.add(SplashParticle.droplet(position, velocity, radius, MAX_AIRBORNE_AGE, alpha));
		}
		trimParticles();
	}

	private void applySurfaceImpact(WaterImpact impact, Eau water, float waterAlpha){
		float amplitude = calculateWaveAmplitude(impact.downwardSpeed, impact.mass, impact.size, impact.intensity);
		int visibleWaveCount = calculateVisibleWaveCount(impact.downwardSpeed, impact.mass, impact.size,
				impact.intensity);
		float alpha = capToWaterAlpha(waterAlpha * 0.72f, waterAlpha);
		getSurfaceSimulation(water).applyImpact(water.getSurfaceLocalX(impact.point), amplitude, impact.size, alpha,
				visibleWaveCount);
		DebugConfig.log("water impact waves=" + visibleWaveCount
				+ " amplitude=" + number(amplitude)
				+ " speed=" + number(impact.downwardSpeed)
				+ " mass=" + number(impact.mass)
				+ " size=" + number(impact.size));
	}

	private void updateDroplet(SplashParticle particle, float delta){
		previousPosition.set(particle.position);
		particle.integrate(delta, VISUAL_GRAVITY);

		boolean movedEnough = previousPosition.dst2(particle.position) > MIN_RAY_DISTANCE2;
		boolean waterSurfaceHit = movedEnough && findWaterSurfaceHit(previousPosition, particle.position,
				particle.radius);
		boolean canHitHardSurface = particle.age > 0.04f && movedEnough;
		boolean hardSurfaceHit = canHitHardSurface && findHardSurfaceHit(previousPosition, particle.position);

		if(waterSurfaceHit && (!hardSurfaceHit || waterHitFraction <= rayHitFraction)){
			surfacePoint(waterHit, waterHitLocalX, mergeSurfacePoint);
			particle.mergeWithWater(waterHit, waterHitLocalX, mergeSurfacePoint,
					surfaceAngleDegrees(waterHit, waterHitLocalX));
			return;
		}

		if(hardSurfaceHit){
			particle.flattenOnSurface(rayHitPoint, rayHitNormal);
			return;
		}
	}

	private boolean findHardSurfaceHit(Vector2 from, Vector2 to){
		rayHit = false;
		rayHitFraction = 1f;
		world.rayCast(hardSurfaceRayCast, from, to);
		return rayHit;
	}

	private boolean findWaterSurfaceHit(Vector2 from, Vector2 to, float radius){
		waterHit = null;
		waterHitFraction = 1f;
		for(Eau water : waters){
			Vector2 localFrom = water.getLocalPointCopy(from);
			Vector2 localTo = water.getLocalPointCopy(to);
			sampledWater = water;
			if(findWaterSurfaceHitLocal(localFrom, localTo, radius, water.getSurfaceHalfWidth(), waterSurfaceSampler,
					localWaterHit) && localWaterHit.fraction < waterHitFraction){
				waterHit = water;
				waterHitFraction = localWaterHit.fraction;
				waterHitLocalX = localWaterHit.localX;
			}
		}
		sampledWater = null;
		return waterHit != null;
	}

	private boolean shouldDiscardAirborne(SplashParticle particle){
		if(particle.age < MAX_AIRBORNE_AGE)
			return false;
		if(waters.size == 0)
			return true;
		for(Eau water : waters){
			Vector2 localPoint = water.getLocalPointCopy(particle.position);
			if(Math.abs(localPoint.x) <= water.getSurfaceHalfWidth() + AIRBORNE_SAFETY_MARGIN
					&& localPoint.y >= -water.height - AIRBORNE_SAFETY_MARGIN)
				return false;
		}
		return true;
	}

	private boolean isHardSurface(Fixture fixture){
		if(fixture == null || fixture.isSensor())
			return false;
		Object userData = fixture.getUserData();
		if("Water".equals(userData) || "Ball".equals(userData) || "BalleDetecteur".equals(userData))
			return false;
		Object bodyData = fixture.getBody().getUserData();
		return !"Water".equals(bodyData) && !"Balle".equals(bodyData);
	}

	private void drawDroplet(SpriteBatch batch, TextureRegion region, SplashParticle particle){
		float size = particle.radius * 2f;
		batch.draw(region, particle.position.x - particle.radius, particle.position.y - particle.radius,
				particle.radius, particle.radius, size, size, 1, 1, 0);
	}

	private void drawParticle(SpriteBatch batch, TextureRegion region, SplashParticle particle, Color waterColor){
		float alpha = cappedRenderAlpha(particle, waterColor);
		if(alpha <= 0f)
			return;
		batch.setColor(waterColor.r, waterColor.g, waterColor.b, alpha);
		if(particle.state == SplashParticleState.RIPPLE)
			drawRippleParticle(batch, region, particle, waterColor);
		else if(particle.isAirborne())
			drawDroplet(batch, region, particle);
		else
			drawFlatParticle(batch, region, particle);
	}

	private void drawRippleParticle(SpriteBatch batch, TextureRegion region, SplashParticle particle, Color waterColor){
		if(particle.water == null){
			drawFlatParticle(batch, region, particle);
			return;
		}
		int segmentCount = collectRippleSegments(particle.waterLocalX, particle.renderLength(),
				particle.water.getSurfaceHalfWidth(), rippleSegments);
		for(int i = 0; i < segmentCount; i++)
			drawRippleSegment(batch, region, particle, waterColor, rippleSegments[i]);
	}

	private void drawRippleSegment(SpriteBatch batch, TextureRegion region, SplashParticle particle, Color waterColor,
			RippleSegment segment){
		float length = segment.end - segment.start;
		if(length <= 0.01f)
			return;
		float alpha = capToWaterAlpha(particle.renderAlpha() * segment.alphaScale, waterAlpha(waterColor));
		if(alpha <= 0f)
			return;
		batch.setColor(waterColor.r, waterColor.g, waterColor.b, alpha);
		float thickness = particle.renderThickness();
		int subSegmentCount = MathUtils.clamp(MathUtils.ceil(length / SURFACE_SAMPLE_SPACING), 1, 64);
		float step = length / subSegmentCount;
		for(int i = 0; i < subSegmentCount; i++){
			float start = segment.start + step * i;
			float end = i == subSegmentCount - 1 ? segment.end : start + step;
			surfacePoint(particle.water, start, rippleStartPoint);
			surfacePoint(particle.water, end, rippleEndPoint);
			float subLength = rippleStartPoint.dst(rippleEndPoint);
			if(subLength <= 0.01f)
				continue;
			drawPoint.set(rippleStartPoint).add(rippleEndPoint).scl(0.5f);
			float angle = (float)Math.atan2(rippleEndPoint.y - rippleStartPoint.y,
					rippleEndPoint.x - rippleStartPoint.x) * MathUtils.radiansToDegrees;
			batch.draw(region,
					drawPoint.x - subLength * 0.5f,
					drawPoint.y - thickness * 0.5f,
					subLength * 0.5f,
					thickness * 0.5f,
					subLength,
					thickness,
					1,
					1,
					angle);
		}
	}

	private void drawTravelingWaves(SpriteBatch batch, TextureRegion region, Color waterColor){
		if(surfaceSimulations.size == 0)
			return;
		float waterAlpha = waterAlpha(waterColor);
		for(WaterSurfaceSimulation simulation : surfaceSimulations){
			if(simulation.water == null)
				continue;
			for(TravelingWave wave : simulation.travelingWaves)
				drawTravelingWave(batch, region, simulation.water, wave, waterColor, waterAlpha);
		}
	}

	private void drawTravelingWave(SpriteBatch batch, TextureRegion region, Eau water, TravelingWave wave,
			Color waterColor, float waterAlpha){
		float alpha = calculateWaveRenderAlpha(wave, waterAlpha);
		if(alpha <= 0.01f || wave.visualLength() <= 0.01f)
			return;

		float halfLength = wave.visualLength() * 0.5f;
		float start = Math.max(wave.centerX - halfLength, -water.getSurfaceHalfWidth());
		float end = Math.min(wave.centerX + halfLength, water.getSurfaceHalfWidth());
		float length = end - start;
		if(length <= 0.01f)
			return;
		if(!wave.drawsSpriteOverlay())
			return;
		setWaveColor(waterColor, true, alpha, waveColor);
		drawTravelingWaveSegments(batch, region, water, wave, start, end, wave.visualThickness(), waveColor);
	}

	private void drawTravelingWaveSegments(SpriteBatch batch, TextureRegion region, Eau water, TravelingWave wave,
			float start, float end, float thickness, Color color){
		float length = end - start;
		if(length <= 0.01f || thickness <= 0f || color.a <= 0f)
			return;
		int subSegmentCount = MathUtils.clamp(MathUtils.ceil(length / VISUAL_WAVE_SEGMENT_SPACING), 1, 96);
		float step = length / subSegmentCount;
		for(int i = 0; i < subSegmentCount; i++){
			float segmentStart = start + step * i;
			float segmentEnd = i == subSegmentCount - 1 ? end : segmentStart + step;
			float envelope = wave.visualEnvelopeAt((segmentStart + segmentEnd) * 0.5f);
			float alpha = color.a * smoothStep(envelope);
			if(alpha <= 0.002f)
				continue;
			float localThickness = thickness * (0.35f + envelope * 0.65f);
			batch.setColor(color.r, color.g, color.b, alpha);
			surfacePoint(water, segmentStart, rippleStartPoint);
			surfacePoint(water, segmentEnd, rippleEndPoint);
			float subLength = rippleStartPoint.dst(rippleEndPoint);
			if(subLength <= 0.01f)
				continue;
			drawPoint.set(rippleStartPoint).add(rippleEndPoint).scl(0.5f);
			float angle = (float)Math.atan2(rippleEndPoint.y - rippleStartPoint.y,
					rippleEndPoint.x - rippleStartPoint.x) * MathUtils.radiansToDegrees;
			batch.draw(region,
					drawPoint.x - subLength * 0.5f,
					drawPoint.y - localThickness * 0.5f,
					subLength * 0.5f,
					localThickness * 0.5f,
					subLength,
					localThickness,
					1,
					1,
					angle);
		}
	}

	private void drawFlatParticle(SpriteBatch batch, TextureRegion region, SplashParticle particle){
		float length = particle.renderLength();
		float thickness = particle.renderThickness();
		batch.draw(region,
				particle.position.x - length * 0.5f,
				particle.position.y - thickness * 0.5f,
				length * 0.5f,
				thickness * 0.5f,
				length,
				thickness,
				1,
				1,
				particle.angleDegrees);
	}

	private void trimParticles(){
		while(particles.size > MAX_PARTICLES && removeOldestLandedParticle()){
		}
		while(particles.size > MAX_PARTICLES)
			particles.removeIndex(0);
	}

	private boolean removeOldestLandedParticle(){
		for(int i = 0; i < particles.size; i++){
			if(!particles.get(i).isAirborne()){
				particles.removeIndex(i);
				return true;
			}
		}
		return false;
	}

	private WaterSurfaceSimulation getSurfaceSimulation(Eau water){
		for(WaterSurfaceSimulation simulation : surfaceSimulations)
			if(simulation.water == water)
				return simulation;
		WaterSurfaceSimulation simulation = new WaterSurfaceSimulation(water, water.getSurfaceHalfWidth());
		surfaceSimulations.add(simulation);
		return simulation;
	}

	WaterSurfaceSimulation findSurfaceSimulation(Eau water){
		for(WaterSurfaceSimulation simulation : surfaceSimulations)
			if(simulation.water == water)
				return simulation;
		return null;
	}

	float surfaceDisplacement(Eau water, float localX){
		WaterSurfaceSimulation simulation = findSurfaceSimulation(water);
		return simulation == null ? 0f : simulation.displacementAt(localX);
	}

	Vector2 surfacePoint(Eau water, float localX, Vector2 out){
		return water.getSurfacePoint(localX, surfaceLocalY(water, localX) - water.getSurfaceLocalY(), out);
	}

	float surfaceAngleDegrees(Eau water, float localX){
		WaterSurfaceSimulation simulation = findSurfaceSimulation(water);
		if(simulation == null)
			return water.getSurfaceAngleDegrees();
		float offset = Math.max(0.02f, simulation.sampleSpacing * 0.5f);
		surfacePoint(water, localX - offset, surfaceAngleStartPoint);
		surfacePoint(water, localX + offset, surfaceAngleEndPoint);
		if(surfaceAngleStartPoint.dst2(surfaceAngleEndPoint) <= 0.0001f)
			return water.getSurfaceAngleDegrees();
		return (float)Math.atan2(surfaceAngleEndPoint.y - surfaceAngleStartPoint.y,
				surfaceAngleEndPoint.x - surfaceAngleStartPoint.x) * MathUtils.radiansToDegrees;
	}

	private float surfaceLocalY(Eau water, float localX){
		return WaterSurfaceRenderer.clampSurfaceY(water.height, surfaceDisplacement(water, localX));
	}

	static int collectRippleSegments(float centerLocalX, float fullLength, float halfWidth, RippleSegment[] segments){
		if(segments == null || segments.length == 0 || fullLength <= 0f || halfWidth <= 0f)
			return 0;

		int count = 0;
		float start = centerLocalX - fullLength * 0.5f;
		float end = centerLocalX + fullLength * 0.5f;
		count = addClippedRippleSegment(segments, count, start, end, -halfWidth, halfWidth, 1f);

		if(start < -halfWidth){
			float reflectedEnd = Math.min(-halfWidth + (-halfWidth - start), halfWidth);
			count = addClippedRippleSegment(segments, count, -halfWidth, reflectedEnd, -halfWidth, halfWidth,
					RIPPLE_BOUNCE_ALPHA);
		}
		if(end > halfWidth){
			float reflectedStart = Math.max(halfWidth - (end - halfWidth), -halfWidth);
			count = addClippedRippleSegment(segments, count, reflectedStart, halfWidth, -halfWidth, halfWidth,
					RIPPLE_BOUNCE_ALPHA);
		}
		return count;
	}

	static float randomDropAlpha(float waterAlpha, float randomRatio){
		float safeWaterAlpha = MathUtils.clamp(waterAlpha, 0f, 1f);
		return safeWaterAlpha * MathUtils.clamp(randomRatio, 0.5f, 1f);
	}

	static float capToWaterAlpha(float alpha, float waterAlpha){
		return MathUtils.clamp(Math.min(alpha, MathUtils.clamp(waterAlpha, 0f, 1f)), 0f, 1f);
	}

	static float calculateWaveAmplitude(float downwardSpeed, float mass, float size, float intensity){
		float speed = Math.max(0f, downwardSpeed);
		float safeMass = Math.max(0.02f, mass);
		float safeSize = Math.max(0.1f, size);
		float safeIntensity = Math.max(0f, intensity);
		return MathUtils.clamp(speed * 0.052f + (float)Math.sqrt(safeMass) * 0.185f
				+ safeSize * 0.13f + safeIntensity * 0.052f, 0.16f, 1.45f);
	}

	static int calculateVisibleWaveCount(float downwardSpeed, float mass, float size, float intensity){
		float speed = Math.max(0f, downwardSpeed);
		float safeMass = Math.max(0.02f, mass);
		float safeSize = Math.max(0.1f, size);
		float safeIntensity = Math.max(0f, intensity);
		return MathUtils.clamp(MathUtils.round(1.75f + speed * 0.18f + (float)Math.sqrt(safeMass) * 0.72f
				+ safeSize * 0.62f + safeIntensity * 0.2f), MIN_VISIBLE_WAVES, MAX_VISIBLE_WAVES);
	}

	static int calculateDropletCount(float downwardSpeed, float totalSpeed, float mass, float size, float intensity){
		float speed = Math.max(0f, downwardSpeed);
		float travelSpeed = Math.max(speed, Math.max(0f, totalSpeed));
		float safeMass = Math.max(0.02f, mass);
		float safeSize = Math.max(0.1f, size);
		float safeIntensity = Math.max(0f, intensity);
		return MathUtils.clamp(MathUtils.round(7f + safeIntensity * 3.05f + travelSpeed * 0.74f
				+ (float)Math.sqrt(safeMass) * 4.2f + safeSize * 2.25f), MIN_DROPLETS_PER_IMPACT,
				MAX_DROPLETS_PER_IMPACT);
	}

	static float calculateDropletSpread(float downwardSpeed, float totalSpeed, float mass, float size, float intensity){
		float travelSpeed = Math.max(Math.max(0f, downwardSpeed), Math.max(0f, totalSpeed));
		float safeMass = Math.max(0.02f, mass);
		float safeSize = Math.max(0.1f, size);
		float safeIntensity = Math.max(0f, intensity);
		return MathUtils.clamp(0.9f + safeIntensity * 0.84f + travelSpeed * 0.28f
				+ (float)Math.sqrt(safeMass) * 0.95f + safeSize * 0.48f, 1.1f, MAX_DROPLET_SPREAD);
	}

	static float calculateDropletLaunchSpeed(float downwardSpeed, float mass, float size, float intensity){
		float speed = Math.max(0f, downwardSpeed);
		float safeMass = Math.max(0.02f, mass);
		float safeSize = Math.max(0.1f, size);
		float safeIntensity = Math.max(0f, intensity);
		return MathUtils.clamp(3.2f + safeIntensity * 0.54f + speed * 0.22f
				+ (float)Math.sqrt(safeMass) * 0.5f + safeSize * 0.2f, 3.2f, 24f);
	}

	static float calculateDropletRadiusBase(float mass, float size, float intensity){
		float safeMass = Math.max(0.02f, mass);
		float safeSize = Math.max(0.1f, size);
		float safeIntensity = Math.max(0f, intensity);
		return MathUtils.clamp(0.035f + safeIntensity * 0.015f + safeSize * 0.018f
				+ (float)Math.sqrt(safeMass) * 0.006f, 0.04f, 0.36f);
	}

	static float calculateWaveRenderAlpha(TravelingWave wave, float waterAlpha){
		if(wave == null)
			return 0f;
		return capToWaterAlpha(wave.visualAlpha() * MathUtils.clamp(waterAlpha, 0f, 1f), waterAlpha);
	}

	static Color setWaveColor(Color waterColor, boolean crest, float alpha, Color out){
		Color target = out == null ? new Color() : out;
		float waterRed = waterColor == null ? 0f : waterColor.r;
		float waterGreen = waterColor == null ? 0.45f : waterColor.g;
		float waterBlue = waterColor == null ? 0.65f : waterColor.b;
		if(crest){
			target.set(MathUtils.clamp(waterRed + 0.72f, 0f, 1f),
					MathUtils.clamp(waterGreen + 0.62f, 0f, 1f),
					MathUtils.clamp(waterBlue + 0.64f, 0f, 1f),
					MathUtils.clamp(alpha, 0f, 1f));
		}
		else{
			target.set(MathUtils.clamp(waterRed * 0.08f, 0f, 1f),
					MathUtils.clamp(waterGreen * 0.2f, 0f, 1f),
					MathUtils.clamp(waterBlue * 0.35f + 0.05f, 0f, 1f),
					MathUtils.clamp(alpha, 0f, 1f));
		}
		return target;
	}

	private static String number(float value){
		return Float.toString(MathUtils.round(value * 1000f) / 1000f);
	}

	static boolean findWaterSurfaceHitLocal(Vector2 localFrom, Vector2 localTo, float radius, float halfWidth,
			SurfaceSampler surfaceSampler, WaterSurfaceHit out){
		out.clear();
		if(surfaceSampler == null || localTo.y > localFrom.y)
			return false;

		float safeHalfWidth = Math.max(0f, halfWidth);
		float safeRadius = Math.max(0f, radius);
		float previousFraction = 0f;
		float previousX = localFrom.x;
		float previousDistance = surfaceContactDistance(localFrom.y, surfaceSampler.surfaceLocalY(previousX),
				safeRadius);
		float initialDistance = previousDistance;
		int steps = waterSurfaceCollisionSteps(localFrom.dst(localTo));
		for(int step = 1; step <= steps; step++){
			float fraction = step / (float)steps;
			float localX = MathUtils.lerp(localFrom.x, localTo.x, fraction);
			float localY = MathUtils.lerp(localFrom.y, localTo.y, fraction);
			float distance = surfaceContactDistance(localY, surfaceSampler.surfaceLocalY(localX), safeRadius);
			if(previousDistance >= 0f && distance <= 0f){
				float denominator = previousDistance - distance;
				float localFraction = denominator <= 0.0001f ? 0f : previousDistance / denominator;
				localFraction = MathUtils.clamp(localFraction, 0f, 1f);
				float hitFraction = MathUtils.lerp(previousFraction, fraction, localFraction);
				float hitLocalX = MathUtils.lerp(previousX, localX, localFraction);
				if(containsSurfaceLocalX(hitLocalX, safeHalfWidth)){
					out.set(hitFraction, hitLocalX);
					return true;
				}
			}
			previousFraction = fraction;
			previousX = localX;
			previousDistance = distance;
		}

		float finalDistance = previousDistance;
		if(initialDistance <= 0f && containsSurfaceLocalX(localFrom.x, safeHalfWidth)){
			out.set(0f, localFrom.x);
			return true;
		}
		if(finalDistance <= 0f && containsSurfaceLocalX(localTo.x, safeHalfWidth)){
			out.set(1f, localTo.x);
			return true;
		}
		return false;
	}

	static int waterSurfaceCollisionSteps(float localDistance){
		return MathUtils.clamp(MathUtils.ceil(Math.max(0f, localDistance)
				/ WATER_SURFACE_COLLISION_SAMPLE_SPACING), 8, 96);
	}

	private static float surfaceContactDistance(float localY, float surfaceY, float radius){
		return localY - (surfaceY + radius);
	}

	private static boolean containsSurfaceLocalX(float localX, float halfWidth){
		return localX >= -halfWidth && localX <= halfWidth;
	}

	private float cappedRenderAlpha(SplashParticle particle, Color waterColor){
		return capToWaterAlpha(particle.renderAlpha(), waterAlpha(waterColor));
	}

	private float waterAlpha(Eau water){
		return water == null || water.getCouleur() == null ? 1f : water.getCouleur().a;
	}

	private static float waterAlpha(Color waterColor){
		return waterColor == null ? 1f : waterColor.a;
	}

	private static int addClippedRippleSegment(RippleSegment[] segments, int count, float start, float end, float min,
			float max, float alphaScale){
		if(count >= segments.length)
			return count;
		float clippedStart = Math.max(start, min);
		float clippedEnd = Math.min(end, max);
		if(clippedEnd <= clippedStart)
			return count;
		segments[count].set(clippedStart, clippedEnd, alphaScale);
		return count + 1;
	}

	static final class RippleSegment {
		float start;
		float end;
		float alphaScale;

		void set(float start, float end, float alphaScale){
			this.start = start;
			this.end = end;
			this.alphaScale = alphaScale;
		}
	}

	interface SurfaceSampler {
		float surfaceLocalY(float localX);
	}

	static final class WaterSurfaceHit {
		float fraction;
		float localX;

		void set(float fraction, float localX){
			this.fraction = fraction;
			this.localX = localX;
		}

		void clear(){
			fraction = 1f;
			localX = 0f;
		}
	}

	static final class WaterSurfaceSimulation {
		final Eau water;
		final int sampleCount;
		final float halfWidth;
		final float sampleSpacing;
		final float[] displacements;
		final float[] velocities;
		final float[] leftDeltas;
		final float[] rightDeltas;
		final Array<TravelingWave> travelingWaves = new Array<TravelingWave>();
		float alpha;
		float energy;

		WaterSurfaceSimulation(Eau water, float halfWidth){
			this.water = water;
			this.halfWidth = Math.max(0.1f, halfWidth);
			sampleCount = calculateSurfaceSampleCount(this.halfWidth);
			sampleSpacing = (this.halfWidth * 2f) / (sampleCount - 1);
			displacements = new float[sampleCount];
			velocities = new float[sampleCount];
			leftDeltas = new float[sampleCount];
			rightDeltas = new float[sampleCount];
		}

		void applyImpact(float localX, float amplitude, float size, float alpha){
			applyImpact(localX, amplitude, size, alpha, calculateVisibleWaveCount(amplitude * 11f,
					amplitude * amplitude * 4f, size, amplitude * 8f));
		}

		void applyImpact(float localX, float amplitude, float size, float alpha, int visibleWaveCount){
			this.alpha = Math.max(this.alpha, alpha);
			energy = Math.max(energy, amplitude);
			spawnTravelingWaves(localX, amplitude, size, visibleWaveCount);
			int center = nearestSample(localX);
			float radius = Math.max(sampleSpacing * 2.5f, size * 0.8f);
			int sampleRadius = Math.max(1, MathUtils.ceil(radius / sampleSpacing));
			for(int offset = -sampleRadius; offset <= sampleRadius; offset++){
				int index = center + offset;
				if(index < 0 || index >= sampleCount)
					continue;
				float falloff = 1f - Math.abs(offset) / (float)(sampleRadius + 1);
				falloff *= falloff;
				displacements[index] -= amplitude * 0.28f * falloff;
				velocities[index] -= amplitude * 7f * falloff;
			}
			int shoulderRadius = sampleRadius + Math.max(2, MathUtils.ceil(sampleRadius * 0.45f));
			for(int offset = -shoulderRadius; offset <= shoulderRadius; offset++){
				int distance = Math.abs(offset);
				if(distance <= sampleRadius)
					continue;
				int index = center + offset;
				if(index < 0 || index >= sampleCount)
					continue;
				float falloff = 1f - (distance - sampleRadius) / (float)(shoulderRadius - sampleRadius + 1);
				falloff *= falloff;
				displacements[index] += amplitude * 0.08f * falloff;
				velocities[index] += amplitude * 2f * falloff;
			}
			clampMotion();
		}

		void update(float delta){
			if(!hasMotion())
				return;
			float clampedDelta = Math.min(delta, 1f / 30f);
			updateTravelingWaves(clampedDelta);
			for(int i = 0; i < sampleCount; i++){
				float acceleration = -SURFACE_STIFFNESS * displacements[i] - SURFACE_DAMPING * velocities[i];
				velocities[i] += acceleration * clampedDelta;
				displacements[i] += velocities[i] * clampedDelta;
			}
			for(int pass = 0; pass < 8; pass++){
				for(int i = 0; i < sampleCount; i++){
					if(i > 0){
						leftDeltas[i] = SURFACE_SPREAD * clampedDelta * (displacements[i] - displacements[i - 1]);
						velocities[i - 1] += leftDeltas[i];
					}
					if(i < sampleCount - 1){
						rightDeltas[i] = SURFACE_SPREAD * clampedDelta * (displacements[i] - displacements[i + 1]);
						velocities[i + 1] += rightDeltas[i];
					}
				}
				for(int i = 0; i < sampleCount; i++){
					if(i > 0)
						displacements[i - 1] += leftDeltas[i];
					if(i < sampleCount - 1)
						displacements[i + 1] += rightDeltas[i];
				}
			}
			velocities[0] *= SURFACE_EDGE_REFLECTION_DAMPING;
			velocities[sampleCount - 1] *= SURFACE_EDGE_REFLECTION_DAMPING;
			float maxEnergy = 0f;
			for(int i = 0; i < sampleCount; i++){
				displacements[i] = MathUtils.clamp(displacements[i], -SURFACE_MAX_DISPLACEMENT,
						SURFACE_MAX_DISPLACEMENT);
				velocities[i] = MathUtils.clamp(velocities[i], -SURFACE_MAX_VELOCITY, SURFACE_MAX_VELOCITY);
				maxEnergy = Math.max(maxEnergy, Math.abs(displacements[i]) + Math.abs(velocities[i]) * 0.035f);
			}
			for(TravelingWave wave : travelingWaves)
				maxEnergy = Math.max(maxEnergy, wave.energy());
			energy = maxEnergy;
			alpha *= 0.992f;
			if(energy < SURFACE_SETTLE_THRESHOLD && travelingWaves.size == 0){
				for(int i = 0; i < sampleCount; i++){
					displacements[i] = 0f;
					velocities[i] = 0f;
				}
				energy = 0f;
				alpha = 0f;
			}
		}

		boolean hasMotion(){
			return energy > SURFACE_SETTLE_THRESHOLD || alpha > 0.01f || travelingWaves.size > 0;
		}

		float localX(int index){
			return -halfWidth + sampleSpacing * index;
		}

		float displacement(int index){
			return combinedDisplacementAt(localX(index));
		}

		float displacementAt(float localX){
			return combinedDisplacementAt(localX);
		}

		float springDisplacementAt(float localX){
			float normalized = (MathUtils.clamp(localX, -halfWidth, halfWidth) + halfWidth) / (halfWidth * 2f);
			float scaledIndex = normalized * (sampleCount - 1);
			int leftIndex = MathUtils.clamp(MathUtils.floor(scaledIndex), 0, sampleCount - 1);
			int rightIndex = MathUtils.clamp(leftIndex + 1, 0, sampleCount - 1);
			if(leftIndex == rightIndex)
				return displacements[leftIndex];
			return MathUtils.lerp(displacements[leftIndex], displacements[rightIndex], scaledIndex - leftIndex);
		}

		float travelingWaveDisplacementAt(float localX){
			float displacement = 0f;
			for(TravelingWave wave : travelingWaves)
				displacement += wave.displacementAt(localX);
			return displacement;
		}

		float combinedDisplacementAt(float localX){
			return MathUtils.clamp(springDisplacementAt(localX) + travelingWaveDisplacementAt(localX),
					-SURFACE_MAX_DISPLACEMENT, SURFACE_MAX_DISPLACEMENT);
		}

		float maxAbsDisplacement(){
			float max = 0f;
			for(int i = 0; i < sampleCount; i++)
				max = Math.max(max, Math.abs(displacement(i)));
			return max;
		}

		int travelingWaveCount(){
			return travelingWaves.size;
		}

		boolean hasTravelingWaveDirection(int direction){
			for(TravelingWave wave : travelingWaves)
				if(wave.direction == direction)
					return true;
			return false;
		}

		private void spawnTravelingWaves(float localX, float amplitude, float size, int visibleWaveCount){
			int waveCount = MathUtils.clamp(visibleWaveCount, MIN_VISIBLE_WAVES, MAX_VISIBLE_WAVES);
			float startX = MathUtils.clamp(localX, -halfWidth, halfWidth);
			float spacing = MathUtils.clamp(0.3f + size * 0.15f + amplitude * 0.14f, sampleSpacing * 1.9f,
					Math.max(sampleSpacing * 2.4f, halfWidth * 0.35f));
			float width = MathUtils.clamp(spacing * 0.82f, sampleSpacing * 1.55f, halfWidth * 0.24f);
			for(int i = 0; i < waveCount; i++){
				int direction = i % 2 == 0 ? 1 : -1;
				int rank = i / 2;
				float falloff = (float)Math.pow(0.86f, rank);
				float polarity = rank % 2 == 0 ? -0.82f : 1f;
				float waveAmplitude = amplitude * (0.52f + waveCount * 0.026f) * falloff;
				float speed = (TRAVELING_WAVE_BASE_SPEED + amplitude * TRAVELING_WAVE_SPEED_SCALE
						+ size * 0.08f + rank * 0.035f) * TRAVELING_WAVE_SPEED_MULTIPLIER;
				float offset = sampleSpacing * 0.6f + spacing * rank;
				float centerX = startX + direction * offset;
				addTravelingWave(new TravelingWave(reflectIntoBounds(centerX, halfWidth), direction, waveAmplitude,
						spacing * 2f, speed, width, -MathUtils.PI * 0.5f, polarity));
			}
		}

		private static float reflectIntoBounds(float localX, float halfWidth){
			float reflectedX = localX;
			for(int i = 0; i < 8 && (reflectedX < -halfWidth || reflectedX > halfWidth); i++){
				if(reflectedX < -halfWidth)
					reflectedX = -halfWidth + (-halfWidth - reflectedX);
				else if(reflectedX > halfWidth)
					reflectedX = halfWidth - (reflectedX - halfWidth);
			}
			return MathUtils.clamp(reflectedX, -halfWidth, halfWidth);
		}

		private void addTravelingWave(TravelingWave wave){
			travelingWaves.add(wave);
			while(travelingWaves.size > MAX_TRAVELING_WAVES)
				travelingWaves.removeIndex(0);
		}

		private void updateTravelingWaves(float delta){
			for(int i = travelingWaves.size - 1; i >= 0; i--){
				TravelingWave wave = travelingWaves.get(i);
				boolean startedMerge = wave.update(delta, halfWidth);
				if(startedMerge)
					mergeTravelingWaveIntoSpring(wave);
				if(wave.isExpired())
					travelingWaves.removeIndex(i);
			}
		}

		private void mergeTravelingWaveIntoSpring(TravelingWave wave){
			float radius = Math.max(sampleSpacing * 1.5f, wave.width);
			int center = nearestSample(wave.centerX);
			int sampleRadius = Math.max(1, MathUtils.ceil(radius / sampleSpacing));
			for(int offset = -sampleRadius; offset <= sampleRadius; offset++){
				int index = center + offset;
				if(index < 0 || index >= sampleCount)
					continue;
				float localX = localX(index);
				float normalizedDistance = Math.abs(localX - wave.centerX) / radius;
				if(normalizedDistance >= 1f)
					continue;
				float envelope = MathUtils.cos(normalizedDistance * MathUtils.PI * 0.5f);
				envelope *= envelope;
				float residual = wave.displacementAt(localX) * envelope * TRAVELING_WAVE_MERGE_TRANSFER;
				displacements[index] += residual;
				velocities[index] += residual * 1.35f;
			}
			clampMotion();
		}

		int nearestSample(float localX){
			float normalized = (MathUtils.clamp(localX, -halfWidth, halfWidth) + halfWidth) / (halfWidth * 2f);
			return MathUtils.clamp(Math.round(normalized * (sampleCount - 1)), 0, sampleCount - 1);
		}

		private void clampMotion(){
			for(int i = 0; i < sampleCount; i++){
				displacements[i] = MathUtils.clamp(displacements[i], -SURFACE_MAX_DISPLACEMENT,
						SURFACE_MAX_DISPLACEMENT);
				velocities[i] = MathUtils.clamp(velocities[i], -SURFACE_MAX_VELOCITY, SURFACE_MAX_VELOCITY);
			}
		}
	}

	static final class TravelingWave {
		float centerX;
		int direction;
		float amplitude;
		float wavelength;
		float speed;
		float width;
		float phase;
		float polarity;
		float age;
		int bounceCount;
		boolean merging;
		float mergeAge;
		float mergeStartAmplitude;

		TravelingWave(float centerX, int direction, float amplitude, float wavelength, float speed, float width,
				float phase){
			this(centerX, direction, amplitude, wavelength, speed, width, phase, 1f);
		}

		TravelingWave(float centerX, int direction, float amplitude, float wavelength, float speed, float width,
				float phase, float polarity){
			this.centerX = centerX;
			this.direction = direction < 0 ? -1 : 1;
			this.amplitude = Math.max(0f, amplitude);
			this.wavelength = Math.max(0.05f, wavelength);
			this.speed = Math.max(0f, speed);
			this.width = Math.max(0.03f, width);
			this.phase = phase;
			this.polarity = polarity < 0f ? -1f : 1f;
		}

		boolean update(float delta, float halfWidth){
			age += delta;
			centerX += direction * speed * delta;
			for(int i = 0; i < 8 && (centerX < -halfWidth || centerX > halfWidth); i++){
				if(centerX < -halfWidth){
					centerX = -halfWidth + (-halfWidth - centerX);
					direction = 1;
				}
				else if(centerX > halfWidth){
					centerX = halfWidth - (centerX - halfWidth);
					direction = -1;
				}
				amplitude *= TRAVELING_WAVE_BOUNCE_DAMPING;
				speed *= TRAVELING_WAVE_BOUNCE_SPEED_DAMPING;
				width = Math.max(0.03f, width * TRAVELING_WAVE_BOUNCE_SIZE_DAMPING);
				bounceCount++;
			}
			centerX = MathUtils.clamp(centerX, -halfWidth, halfWidth);
			if(merging){
				mergeAge += delta;
				float progress = smoothStep(MathUtils.clamp(mergeAge / TRAVELING_WAVE_MERGE_DURATION, 0f, 1f));
				amplitude = mergeStartAmplitude * (1f - progress);
				return false;
			}
			amplitude *= (float)Math.pow(TRAVELING_WAVE_DAMPING_PER_SECOND, delta);
			if(amplitude <= TRAVELING_WAVE_MERGE_AMPLITUDE || age >= TRAVELING_WAVE_MAX_AGE)
				return startMerging();
			return false;
		}

		private boolean startMerging(){
			if(merging)
				return false;
			merging = true;
			mergeAge = 0f;
			mergeStartAmplitude = amplitude;
			return true;
		}

		float displacementAt(float localX){
			float distance = localX - centerX;
			float normalizedDistance = Math.abs(distance) / width;
			float envelope = roundedWaveEnvelope(normalizedDistance);
			return amplitude * polarity * envelope;
		}

		float energy(){
			return Math.abs(amplitude);
		}

		float visualLength(){
			return width * 2.45f;
		}

		float visualEnvelopeAt(float localX){
			float visualRadius = Math.max(width, visualLength() * 0.5f);
			float normalizedDistance = Math.abs(localX - centerX) / visualRadius;
			return roundedWaveEnvelope(normalizedDistance);
		}

		boolean drawsSpriteOverlay(){
			return polarity > 0f;
		}

		float visualAlpha(){
			float absAmplitude = Math.abs(amplitude);
			float fadeScale = absAmplitude >= TRAVELING_WAVE_VISUAL_FADE_AMPLITUDE ? 1f
					: smoothStep(MathUtils.clamp(absAmplitude / TRAVELING_WAVE_VISUAL_FADE_AMPLITUDE, 0f, 1f));
			float alpha = MathUtils.clamp(0.18f + absAmplitude * 1.15f, VISUAL_WAVE_MIN_ALPHA,
					VISUAL_WAVE_MAX_ALPHA);
			return alpha * fadeScale;
		}

		float visualThickness(){
			return MathUtils.clamp(VISUAL_WAVE_MIN_THICKNESS + Math.abs(amplitude) * 0.14f,
					VISUAL_WAVE_MIN_THICKNESS, VISUAL_WAVE_MAX_THICKNESS);
		}

		boolean isExpired(){
			return merging && mergeAge >= TRAVELING_WAVE_MERGE_DURATION;
		}
	}

	static float smoothStep(float value){
		float t = MathUtils.clamp(value, 0f, 1f);
		return t * t * (3f - 2f * t);
	}

	static float roundedWaveEnvelope(float normalizedDistance){
		float t = MathUtils.clamp(normalizedDistance, 0f, 1f);
		if(t >= 1f)
			return 0f;
		float cosine = Math.max(0f, MathUtils.cos(t * MathUtils.PI * 0.5f));
		float shoulder = 1f - smoothStep(t);
		return MathUtils.clamp(cosine * 0.72f + shoulder * 0.28f, 0f, 1f);
	}

	static int calculateSurfaceSampleCount(float halfWidth){
		return MathUtils.clamp(MathUtils.ceil((Math.max(0.1f, halfWidth) * 2f) / SURFACE_SAMPLE_SPACING) + 1,
				12, MAX_SURFACE_SAMPLES);
	}
}
