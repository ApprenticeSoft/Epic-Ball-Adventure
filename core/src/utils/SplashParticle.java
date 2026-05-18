package utils;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

enum SplashParticleState {
	AIRBORNE,
	RIPPLE,
	FLATTENED
}

final class SplashParticle {
	final Vector2 position = new Vector2();
	final Vector2 velocity = new Vector2();
	float radius;
	float length;
	float thickness;
	float angleDegrees;
	float age;
	float lifetime;
	float alpha;
	SplashParticleState state;

	private SplashParticle(){
	}

	static SplashParticle droplet(Vector2 position, Vector2 velocity, float radius, float lifetime, float alpha){
		SplashParticle particle = new SplashParticle();
		particle.position.set(position);
		particle.velocity.set(velocity);
		particle.radius = radius;
		particle.length = radius * 2f;
		particle.thickness = radius * 2f;
		particle.lifetime = lifetime;
		particle.alpha = alpha;
		particle.state = SplashParticleState.AIRBORNE;
		return particle;
	}

	static SplashParticle ripple(Vector2 position, float radius, float lifetime, float alpha, float angleDegrees){
		SplashParticle particle = new SplashParticle();
		particle.position.set(position);
		particle.radius = radius;
		particle.length = radius;
		particle.thickness = Math.max(0.03f, radius * 0.08f);
		particle.lifetime = lifetime;
		particle.alpha = alpha;
		particle.angleDegrees = angleDegrees;
		particle.state = SplashParticleState.RIPPLE;
		return particle;
	}

	void updateAge(float delta){
		age += delta;
	}

	void integrate(float delta, float gravity){
		velocity.y -= gravity * delta;
		position.mulAdd(velocity, delta);
		updateAge(delta);
	}

	void mergeWithWater(Vector2 surfacePoint, float surfaceAngleDegrees){
		position.set(surfacePoint);
		velocity.setZero();
		age = 0f;
		lifetime = 0.45f;
		length = Math.max(radius * 4f, 0.25f);
		thickness = Math.max(radius * 0.5f, 0.025f);
		angleDegrees = surfaceAngleDegrees;
		alpha = Math.min(alpha, 0.55f);
		state = SplashParticleState.RIPPLE;
	}

	void flattenOnSurface(Vector2 surfacePoint, Vector2 surfaceNormal){
		position.set(surfacePoint);
		velocity.setZero();
		age = 0f;
		lifetime = 0.7f;
		length = Math.max(radius * 5f, 0.28f);
		thickness = Math.max(radius * 0.7f, 0.025f);
		alpha = Math.min(alpha, 0.5f);
		Vector2 tangent = new Vector2(surfaceNormal.y, -surfaceNormal.x);
		if(tangent.isZero())
			tangent.set(1, 0);
		tangent.nor();
		angleDegrees = (float)Math.atan2(tangent.y, tangent.x) * MathUtils.radiansToDegrees;
		state = SplashParticleState.FLATTENED;
	}

	boolean isAirborne(){
		return state == SplashParticleState.AIRBORNE;
	}

	boolean isExpired(){
		return age >= lifetime;
	}

	float progress(){
		if(lifetime <= 0f)
			return 1f;
		return MathUtils.clamp(age / lifetime, 0f, 1f);
	}

	float renderAlpha(){
		return alpha * (1f - progress());
	}

	float renderLength(){
		if(state == SplashParticleState.RIPPLE)
			return length * (1f + progress() * 2.4f);
		if(state == SplashParticleState.FLATTENED)
			return length * (1f + progress() * 1.2f);
		return radius * 2f;
	}

	float renderThickness(){
		if(state == SplashParticleState.AIRBORNE)
			return radius * 2f;
		return Math.max(0.01f, thickness * (1f - progress() * 0.75f));
	}
}
