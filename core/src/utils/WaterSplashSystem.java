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

	private final World world;
	private final Array<Eau> waters;
	private final Array<SplashParticle> particles = new Array<SplashParticle>();
	private final Vector2 previousPosition = new Vector2();
	private final Vector2 rayHitPoint = new Vector2();
	private final Vector2 rayHitNormal = new Vector2();
	private final Vector2 drawPoint = new Vector2();
	private final RippleSegment[] rippleSegments = new RippleSegment[]{
			new RippleSegment(), new RippleSegment(), new RippleSegment()
	};
	private float rayHitFraction;
	private float waterHitFraction;
	private float waterHitLocalX;
	private boolean rayHit;
	private Eau waterHit;

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
			float alpha = particle.renderAlpha();
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
	}

	int getParticleCount(){
		return particles.size;
	}

	private void spawnImpact(WaterImpact impact, Eau water){
		float rippleRadius = MathUtils.clamp(impact.size * 1.2f + impact.intensity * 0.18f, 0.45f, 5f);
		float rippleAlpha = MathUtils.clamp(0.18f + impact.intensity * 0.035f, 0.22f, 0.75f);
		particles.add(SplashParticle.ripple(water, impact.point, rippleRadius, 0.78f, rippleAlpha,
				water.getSurfaceAngleDegrees()));

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
			float alpha = MathUtils.clamp(0.45f + impact.intensity * 0.025f, 0.48f, 0.9f);
			particles.add(SplashParticle.droplet(position, velocity, radius, MAX_AIRBORNE_AGE, alpha));
		}
		trimParticles();
	}

	private void updateDroplet(SplashParticle particle, float delta){
		previousPosition.set(particle.position);
		particle.integrate(delta, VISUAL_GRAVITY);

		boolean canHit = particle.age > 0.04f && previousPosition.dst2(particle.position) > MIN_RAY_DISTANCE2;
		boolean hardSurfaceHit = canHit && findHardSurfaceHit(previousPosition, particle.position);
		boolean waterSurfaceHit = canHit && findWaterSurfaceHit(previousPosition, particle.position);

		if(waterSurfaceHit && (!hardSurfaceHit || waterHitFraction <= rayHitFraction)){
			particle.mergeWithWater(waterHit, waterHitLocalX);
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

	private boolean findWaterSurfaceHit(Vector2 from, Vector2 to){
		waterHit = null;
		waterHitFraction = 1f;
		for(Eau water : waters){
			Vector2 localFrom = water.getLocalPointCopy(from);
			Vector2 localTo = water.getLocalPointCopy(to);
			float surfaceY = water.getSurfaceLocalY();
			if(localFrom.y < surfaceY || localTo.y > surfaceY)
				continue;
			float deltaY = localFrom.y - localTo.y;
			if(Math.abs(deltaY) < 0.0001f)
				continue;
			float fraction = (localFrom.y - surfaceY) / deltaY;
			if(fraction < 0f || fraction > 1f)
				continue;
			float localX = localFrom.x + (localTo.x - localFrom.x) * fraction;
			if(!water.containsSurfaceLocalX(localX))
				continue;
			if(fraction < waterHitFraction){
				waterHit = water;
				waterHitFraction = fraction;
				waterHitLocalX = localX;
			}
		}
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
		float alpha = particle.renderAlpha();
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
		float alpha = particle.renderAlpha() * segment.alphaScale;
		if(alpha <= 0f)
			return;
		particle.water.getSurfacePoint((segment.start + segment.end) * 0.5f, drawPoint);
		batch.setColor(waterColor.r, waterColor.g, waterColor.b, alpha);
		float thickness = particle.renderThickness();
		batch.draw(region,
				drawPoint.x - length * 0.5f,
				drawPoint.y - thickness * 0.5f,
				length * 0.5f,
				thickness * 0.5f,
				length,
				thickness,
				1,
				1,
				particle.water.getSurfaceAngleDegrees());
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
}
