package utils;

import bodies.Eau;

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
	float waterLocalX;
	float startLength;
	float startThickness;
	float targetLength;
	float targetThickness;
	Eau water;
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
		particle.startLength = particle.length;
		particle.startThickness = particle.thickness;
		particle.targetLength = particle.length;
		particle.targetThickness = particle.thickness;
		particle.lifetime = lifetime;
		particle.alpha = alpha;
		particle.state = SplashParticleState.AIRBORNE;
		return particle;
	}

	static SplashParticle ripple(Eau water, Vector2 position, float radius, float lifetime, float alpha, float angleDegrees){
		SplashParticle particle = new SplashParticle();
		particle.position.set(position);
		particle.radius = radius;
		particle.length = radius;
		particle.thickness = Math.max(0.03f, radius * 0.08f);
		particle.startLength = particle.length;
		particle.startThickness = particle.thickness;
		particle.targetLength = particle.length;
		particle.targetThickness = particle.thickness;
		particle.lifetime = lifetime;
		particle.alpha = alpha;
		particle.angleDegrees = angleDegrees;
		particle.water = water;
		if(water != null)
			particle.waterLocalX = water.getSurfaceLocalX(position);
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
		startLength = length;
		startThickness = thickness;
		targetLength = length;
		targetThickness = thickness;
		angleDegrees = surfaceAngleDegrees;
		alpha = Math.min(alpha, 0.55f);
		water = null;
		state = SplashParticleState.RIPPLE;
	}

	void mergeWithWater(Eau water, float surfaceLocalX){
		mergeWithWater(water, surfaceLocalX, water.getSurfacePoint(surfaceLocalX), water.getSurfaceAngleDegrees());
	}

	void mergeWithWater(Eau water, float surfaceLocalX, Vector2 surfacePoint, float surfaceAngleDegrees){
		this.water = water;
		waterLocalX = surfaceLocalX;
		position.set(surfacePoint);
		velocity.setZero();
		age = 0f;
		lifetime = 0.45f;
		length = Math.max(radius * 4f, 0.25f);
		thickness = Math.max(radius * 0.5f, 0.025f);
		startLength = length;
		startThickness = thickness;
		targetLength = length;
		targetThickness = thickness;
		angleDegrees = surfaceAngleDegrees;
		alpha = Math.min(alpha, water.getCouleur() == null ? 1f : water.getCouleur().a);
		state = SplashParticleState.RIPPLE;
	}

	void flattenOnSurface(Vector2 surfacePoint, Vector2 surfaceNormal){
		position.set(surfacePoint);
		velocity.setZero();
		age = 0f;
		lifetime = 0.7f;
		startLength = radius * 2f;
		startThickness = radius * 2f;
		targetLength = Math.max(radius * 5f, 0.28f);
		targetThickness = Math.max(radius * 0.7f, 0.025f);
		length = targetLength;
		thickness = targetThickness;
		alpha = Math.min(alpha, 0.5f);
		Vector2 tangent = new Vector2(surfaceNormal.y, -surfaceNormal.x);
		if(tangent.isZero())
			tangent.set(1, 0);
		tangent.nor();
		angleDegrees = (float)Math.atan2(tangent.y, tangent.x) * MathUtils.radiansToDegrees;
		water = null;
		state = SplashParticleState.FLATTENED;
	}

	boolean isAirborne(){
		return state == SplashParticleState.AIRBORNE;
	}

	boolean isExpired(){
		if(state == SplashParticleState.AIRBORNE)
			return false;
		return age >= lifetime;
	}

	float progress(){
		if(state == SplashParticleState.AIRBORNE)
			return 0f;
		if(lifetime <= 0f)
			return 1f;
		return MathUtils.clamp(age / lifetime, 0f, 1f);
	}

	float renderAlpha(){
		if(state == SplashParticleState.AIRBORNE)
			return alpha;
		return alpha * (1f - smoothProgress());
	}

	float renderLength(){
		float progress = smoothProgress();
		if(state == SplashParticleState.RIPPLE)
			return length * (1f + progress * 2.4f);
		if(state == SplashParticleState.FLATTENED)
			return MathUtils.lerp(startLength, targetLength, progress);
		return radius * 2f;
	}

	float renderThickness(){
		if(state == SplashParticleState.AIRBORNE)
			return radius * 2f;
		float progress = smoothProgress();
		if(state == SplashParticleState.FLATTENED)
			return Math.max(0.01f, MathUtils.lerp(startThickness, targetThickness, progress));
		return Math.max(0.01f, thickness * (1f - progress * 0.75f));
	}

	private float smoothProgress(){
		float progress = progress();
		return progress * progress * (3f - 2f * progress);
	}
}
