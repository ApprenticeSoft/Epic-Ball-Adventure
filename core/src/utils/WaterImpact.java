package utils;

import bodies.Eau;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.Shape;
import com.badlogic.gdx.physics.box2d.WorldManifold;

public final class WaterImpact {
	public final Vector2 point;
	public final Vector2 surfaceNormal;
	public final Vector2 velocity;
	public final float downwardSpeed;
	public final float totalSpeed;
	public final float mass;
	public final float size;
	public final float intensity;

	private WaterImpact(Vector2 point, Vector2 surfaceNormal, Vector2 velocity,
			float downwardSpeed, float totalSpeed, float mass, float size, float intensity){
		this.point = point;
		this.surfaceNormal = surfaceNormal;
		this.velocity = velocity;
		this.downwardSpeed = downwardSpeed;
		this.totalSpeed = totalSpeed;
		this.mass = mass;
		this.size = size;
		this.intensity = intensity;
	}

	public static WaterImpact fromFixture(Eau water, Fixture impactFixture, Contact contact){
		Body body = impactFixture.getBody();
		Vector2 velocity = new Vector2(body.getLinearVelocity());
		Vector2 surfaceNormal = water.getSurfaceNormal();
		float downwardSpeed = Math.max(0f, -velocity.dot(surfaceNormal));
		float totalSpeed = velocity.len();
		float size = approximateFixtureSize(impactFixture);
		float mass = body.getMass();
		if(mass <= 0f)
			mass = Math.max(impactFixture.getDensity() * size * size, 0.02f);
		Vector2 contactPoint = contactPoint(impactFixture, contact);
		Vector2 surfacePoint = water.getSurfacePoint(contactPoint);
		float intensity = calculateIntensity(downwardSpeed, totalSpeed, mass, size);
		return new WaterImpact(surfacePoint, surfaceNormal, velocity, downwardSpeed, totalSpeed, mass, size, intensity);
	}

	public static float calculateIntensity(float downwardSpeed, float totalSpeed, float mass, float size){
		float safeDownwardSpeed = Math.max(0f, downwardSpeed);
		float safeTotalSpeed = Math.max(0f, totalSpeed);
		float safeMass = Math.max(0.02f, mass);
		float safeSize = Math.max(0.1f, size);
		float speedContribution = safeDownwardSpeed * 0.55f + safeTotalSpeed * 0.12f;
		float massContribution = (float)Math.sqrt(safeMass) * 1.4f;
		float sizeContribution = safeSize * 0.35f;
		return MathUtils.clamp(speedContribution + massContribution + sizeContribution, 0.4f, 18f);
	}

	public static float approximateFixtureSize(Fixture fixture){
		Shape shape = fixture.getShape();
		if(shape.getType() == Shape.Type.Circle)
			return Math.max(0.1f, shape.getRadius() * 2f);
		if(shape.getType() == Shape.Type.Polygon)
			return approximatePolygonSize((PolygonShape)shape);
		return Math.max(0.1f, shape.getRadius() * 2f);
	}

	private static float approximatePolygonSize(PolygonShape polygonShape){
		int vertexCount = polygonShape.getVertexCount();
		if(vertexCount == 0)
			return 0.1f;
		Vector2 vertex = new Vector2();
		polygonShape.getVertex(0, vertex);
		float minX = vertex.x;
		float maxX = vertex.x;
		float minY = vertex.y;
		float maxY = vertex.y;
		for(int i = 1; i < vertexCount; i++){
			polygonShape.getVertex(i, vertex);
			minX = Math.min(minX, vertex.x);
			maxX = Math.max(maxX, vertex.x);
			minY = Math.min(minY, vertex.y);
			maxY = Math.max(maxY, vertex.y);
		}
		return Math.max(0.1f, Math.max(maxX - minX, maxY - minY));
	}

	private static Vector2 contactPoint(Fixture impactFixture, Contact contact){
		if(contact != null){
			WorldManifold manifold = contact.getWorldManifold();
			if(manifold.getNumberOfContactPoints() > 0)
				return new Vector2(manifold.getPoints()[0]);
		}
		return new Vector2(impactFixture.getBody().getPosition());
	}
}
