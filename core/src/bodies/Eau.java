package bodies;

import Box2DUtils.BuoyancyController;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.one.button.jam.Couleurs;

public class Eau extends Obstacle{

	public BuoyancyController buoyancyController;

	public Eau(World world, Camera camera, MapObject rectangleObject, Couleurs couleurs) {
		super(world, camera, rectangleObject, couleurs);

		//Couleur
		couleur = couleurs.getCouleurEau();

		body.getFixtureList().get(0).setSensor(true);
		body.getFixtureList().get(0).setUserData("Water");
		body.getFixtureList().get(0).setDensity(0.075f);
		body.setUserData("Water");

		buoyancyController = new BuoyancyController(world, body.getFixtureList().get(0));
	}

	@Override
	public void activity(){
		buoyancyController.step();
	}

	@Override
	public Color getCouleur(){
		return couleurs.getCouleurEau();
	}

	@Override
	public void drawOmbre(SpriteBatch batch, TextureAtlas textureAtlas){
	}

	public BuoyancyController getBuoyancyController(){
		return buoyancyController;
	}

	public boolean containsWorldPoint(Vector2 worldPoint){
		Vector2 localPoint = body.getLocalPoint(worldPoint);
		return Math.abs(localPoint.x) <= width && Math.abs(localPoint.y) <= height;
	}

	public Vector2 getLocalPointCopy(Vector2 worldPoint){
		return new Vector2(body.getLocalPoint(worldPoint));
	}

	public float getSurfaceLocalX(Vector2 worldPoint){
		return body.getLocalPoint(worldPoint).x;
	}

	public boolean containsSurfaceLocalX(float localX){
		return localX >= -width && localX <= width;
	}

	public float getSurfaceHalfWidth(){
		return width;
	}

	public float getSurfaceLocalY(){
		return height;
	}

	public Vector2 getSurfacePoint(Vector2 worldPoint){
		Vector2 localPoint = new Vector2(body.getLocalPoint(worldPoint));
		localPoint.x = MathUtils.clamp(localPoint.x, -width, width);
		localPoint.y = height;
		return new Vector2(body.getWorldPoint(localPoint));
	}

	public Vector2 getSurfacePoint(float localX){
		Vector2 localPoint = new Vector2(MathUtils.clamp(localX, -width, width), height);
		return new Vector2(body.getWorldPoint(localPoint));
	}

	public Vector2 getSurfacePoint(float localX, Vector2 out){
		Vector2 localPoint = out.set(MathUtils.clamp(localX, -width, width), height);
		out.set(body.getWorldPoint(localPoint));
		return out;
	}

	public Vector2 getSurfacePoint(float localX, float localYOffset, Vector2 out){
		Vector2 localPoint = out.set(MathUtils.clamp(localX, -width, width), height + localYOffset);
		out.set(body.getWorldPoint(localPoint));
		return out;
	}

	public Vector2 getSurfaceNormal(){
		return new Vector2(body.getWorldVector(new Vector2(0, 1))).nor();
	}

	public float getSurfaceAngleDegrees(){
		Vector2 normal = getSurfaceNormal();
		Vector2 tangent = new Vector2(normal.y, -normal.x);
		if(tangent.isZero())
			tangent.set(1, 0);
		return (float)Math.atan2(tangent.y, tangent.x) * MathUtils.radiansToDegrees;
	}
}
