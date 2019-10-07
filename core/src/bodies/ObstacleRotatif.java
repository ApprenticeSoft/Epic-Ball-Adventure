package bodies;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.one.button.jam.Couleurs;

public class ObstacleRotatif extends Obstacle{
	
	private float vitesse = 90;

	public ObstacleRotatif(World world, Camera camera, MapObject rectangleObject, Couleurs couleurs) {
		super(world, camera, rectangleObject, couleurs);
		
		//Couleur
		couleur = couleurs.getCouleurLéger();
		
		//Vitesse de rotation
		if(rectangleObject.getProperties().get("Speed") != null)
			vitesse = Float.parseFloat((String) rectangleObject.getProperties().get("Speed"));
		
		body.setFixedRotation(false);
		body.setAngularVelocity(vitesse*MathUtils.degreesToRadians);
	}
	
	@Override
	public BodyType getBodyType(){
		return BodyType.KinematicBody;
	}
	
	@Override
	public Color getCouleur(){
		return couleurs.getCouleurLéger();
	}

}
