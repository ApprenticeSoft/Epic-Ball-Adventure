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
	private static final int MAX_PARTICLES = 180;
	private static final float VISUAL_GRAVITY = 34f;
	private static final float MIN_SPLASH_SPEED = 1.2f;
	private static final float MIN_SPLASH_TOTAL_SPEED = 2.4f;
	private static final float MIN_RAY_DISTANCE2 = 0.0004f;
	private static final float MAX_AIRBORNE_AGE = 12f;
	private static final float AIRBORNE_SAFETY_MARGIN = 18f;
	private static final float RIPPLE_BOUNCE_ALPHA = 0.45f;
	private static final float SURFACE_SAMPLE_SPACING = 0.18f;
	private static final float WATER_SURFACE_COLLISION_SAMPLE_SPACING = 0.06f;
	private static final float SURFACE_STIFFNESS = 15f;
	private static final float SURFACE_DAMPING = 0.58f;
	private static final float SURFACE_SPREAD = 20f;
	private static final float SURFACE_EDGE_REFLECTION_DAMPING = 0.82f;
	private static final float SURFACE_SETTLE_THRESHOLD = 0.0018f;
	private static final float SURFACE_MAX_DISPLACEMENT = 1.55f;
	private static final float SURFACE_MAX_VELOCITY = 18f;
	private static final int MAX_TRAVELING_WAVES = 24;
	private static final int TRAVELING_WAVE_MAX_BOUNCES = 8;
	private static final float TRAVELING_WAVE_BASE_SPEED = 1.65f;
	private static final float TRAVELING_WAVE_SPEED_SCALE = 0.9f;
	private static final float TRAVELING_WAVE_BOUNCE_DAMPING = 0.72f;
	private static final float TRAVELING_WAVE_DAMPING_PER_SECOND = 0.9f;
	private static final float TRAVELING_WAVE_MIN_AMPLITUDE = 0.012f;
	private static final float TRAVELING_WAVE_MAX_AGE = 14f;

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
		if(particles.size == 0)
			return;
		TextureRegion flatRegion = textureAtlas.findRegion("WhiteSquare");
		if(flatRegion == null)
			return;
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
		float rippleRadius = MathUtils.clamp(impact.size * 1.2f + impact.intensity * 0.18f, 0.45f, 5f);
		float rippleAlpha = capToWaterAlpha(MathUtils.clamp(0.18f + impact.intensity * 0.035f, 0.22f, 0.75f),
				waterAlpha);
		particles.add(SplashParticle.ripple(water, impact.point, rippleRadius, 0.78f, rippleAlpha,
				water.getSurfaceAngleDegrees()));
		applySurfaceImpact(impact, water, waterAlpha);

		int dropletCount = MathUtils.clamp(Math.round(4f + impact.intensity * 1.25f + impact.size * 1.4f), 5, 34);
		float radiusBase = MathUtils.clamp(0.035f + impact.intensity * 0.011f + impact.size * 0.012f, 0.04f, 0.24f);
		float spread = MathUtils.clamp(impact.intensity * 0.6f + impact.size * 0.25f, 0.7f, 10f);
		Vector2 normal = new Vector2(impact.surfaceNormal).nor();
		Vector2 tangent = new Vector2(normal.y, -normal.x);
		if(tangent.isZero())
			tangent.set(1, 0);
		tangent.nor();

		for(int i = 0; i < dropletCount; i++){
			float side = MathUtils.random(-spread, spread) + impact.velocity.dot(tangent) * 0.12f;
			float up = MathUtils.random(2.4f + impact.intensity * 0.24f, 5.2f + impact.intensity * 0.72f);
			Vector2 velocity = new Vector2(tangent).scl(side).mulAdd(normal, up);
			Vector2 position = new Vector2(impact.point)
					.mulAdd(normal, 0.08f)
					.mulAdd(tangent, MathUtils.random(-impact.size * 0.18f, impact.size * 0.18f));
			float radius = radiusBase * MathUtils.random(0.65f, 1.35f);
			float alpha = randomDropAlpha(waterAlpha, MathUtils.random(0.5f, 1f));
			particles.add(SplashParticle.droplet(position, velocity, radius, MAX_AIRBORNE_AGE, alpha));
		}
		trimParticles();
	}

	private void applySurfaceImpact(WaterImpact impact, Eau water, float waterAlpha){
		float amplitude = calculateWaveAmplitude(impact.downwardSpeed, impact.mass, impact.size, impact.intensity);
		float alpha = capToWaterAlpha(waterAlpha * 0.72f, waterAlpha);
		getSurfaceSimulation(water).applyImpact(water.getSurfaceLocalX(impact.point), amplitude, impact.size, alpha);
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
			this.alpha = Math.max(this.alpha, alpha);
			energy = Math.max(energy, amplitude);
			spawnTravelingWaves(localX, amplitude, size);
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

		private void spawnTravelingWaves(float localX, float amplitude, float size){
			int pairCount = MathUtils.clamp(MathUtils.round(1.45f + amplitude * 1.15f + size * 0.15f), 2, 4);
			float startX = MathUtils.clamp(localX, -halfWidth, halfWidth);
			for(int pair = 0; pair < pairCount; pair++){
				float pairScale = Math.max(0.45f, 1f - pair * 0.18f);
				float waveAmplitude = amplitude * (0.16f - pair * 0.022f) * pairScale;
				float wavelength = MathUtils.clamp(0.36f + size * 0.22f + amplitude * 0.12f + pair * 0.16f,
						sampleSpacing * 3.5f, halfWidth * 0.9f);
				float width = MathUtils.clamp(wavelength * (3f + pair * 0.35f) + size * 0.18f,
						wavelength * 2.4f, halfWidth * 2f);
				float speed = TRAVELING_WAVE_BASE_SPEED + amplitude * TRAVELING_WAVE_SPEED_SCALE + pair * 0.18f;
				float phase = -MathUtils.PI * 0.5f + pair * 0.32f;
				float offset = sampleSpacing * (pair + 1) * 0.35f;
				addTravelingWave(new TravelingWave(MathUtils.clamp(startX - offset, -halfWidth, halfWidth), -1,
						waveAmplitude, wavelength, speed, width, phase));
				addTravelingWave(new TravelingWave(MathUtils.clamp(startX + offset, -halfWidth, halfWidth), 1,
						waveAmplitude, wavelength, speed, width, phase));
			}
		}

		private void addTravelingWave(TravelingWave wave){
			travelingWaves.add(wave);
			while(travelingWaves.size > MAX_TRAVELING_WAVES)
				travelingWaves.removeIndex(0);
		}

		private void updateTravelingWaves(float delta){
			for(int i = travelingWaves.size - 1; i >= 0; i--){
				TravelingWave wave = travelingWaves.get(i);
				wave.update(delta, halfWidth);
				if(wave.isExpired())
					travelingWaves.removeIndex(i);
			}
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
		float age;
		int bounceCount;

		TravelingWave(float centerX, int direction, float amplitude, float wavelength, float speed, float width,
				float phase){
			this.centerX = centerX;
			this.direction = direction < 0 ? -1 : 1;
			this.amplitude = Math.max(0f, amplitude);
			this.wavelength = Math.max(0.05f, wavelength);
			this.speed = Math.max(0f, speed);
			this.width = Math.max(this.wavelength, width);
			this.phase = phase;
		}

		void update(float delta, float halfWidth){
			age += delta;
			centerX += direction * speed * delta;
			if(centerX < -halfWidth){
				centerX = -halfWidth + (-halfWidth - centerX);
				direction = 1;
				amplitude *= TRAVELING_WAVE_BOUNCE_DAMPING;
				bounceCount++;
			}
			else if(centerX > halfWidth){
				centerX = halfWidth - (centerX - halfWidth);
				direction = -1;
				amplitude *= TRAVELING_WAVE_BOUNCE_DAMPING;
				bounceCount++;
			}
			amplitude *= (float)Math.pow(TRAVELING_WAVE_DAMPING_PER_SECOND, delta);
		}

		float displacementAt(float localX){
			float distance = localX - centerX;
			float normalizedDistance = Math.abs(distance) / width;
			if(normalizedDistance >= 1f)
				return 0f;
			float envelope = 1f - normalizedDistance * normalizedDistance;
			envelope *= envelope;
			return amplitude * MathUtils.sin(MathUtils.PI2 * distance / wavelength + phase) * envelope;
		}

		float energy(){
			return Math.abs(amplitude);
		}

		boolean isExpired(){
			return amplitude < TRAVELING_WAVE_MIN_AMPLITUDE || age > TRAVELING_WAVE_MAX_AGE
					|| bounceCount > TRAVELING_WAVE_MAX_BOUNCES;
		}
	}

	static int calculateSurfaceSampleCount(float halfWidth){
		return MathUtils.clamp(MathUtils.ceil((Math.max(0.1f, halfWidth) * 2f) / SURFACE_SAMPLE_SPACING) + 1,
				12, 96);
	}
}
