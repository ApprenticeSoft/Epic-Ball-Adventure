package bodies;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.one.button.jam.Couleurs;

public class ObstacleLéger extends Obstacle{

	public ObstacleLéger(World world, Camera camera, RectangleMapObject rectangleObject, Couleurs couleurs){
		super(world, camera, rectangleObject, couleurs);
		
		body.setUserData("Light");
		body.getFixtureList().get(0).setUserData("Light");
		body.getFixtureList().get(0).setRestitution(0.45f);
		
		//Couleur
		couleur = couleurs.getCouleurLéger();
		
		//Masse
		if(rectangleObject.getProperties().get("Weight") != null){
			body.getFixtureList().get(0).setDensity(
					body.getFixtureList().get(0).getDensity() * Float.parseFloat(rectangleObject.getProperties().get("Weight").toString())
			);
			body.resetMassData();
		}
		else{
			body.getFixtureList().get(0).setDensity(
					body.getFixtureList().get(0).getDensity() * 0.8f
			);
			body.resetMassData();
		}
	}
	
	@Override
	public BodyType getBodyType(){
		return BodyType.DynamicBody;
	}
	
	@Override
	public Color getCouleur(){
		return couleurs.getCouleurLéger();
	}
	
	@Override
	public void initiate(){
		body.setLinearVelocity(0, 0);
		body.setAngularVelocity(0);
		body.setTransform(initialPosition, angle);
	}
}
