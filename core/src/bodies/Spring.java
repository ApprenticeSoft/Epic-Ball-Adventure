package bodies;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.one.button.jam.Couleurs;

public class Spring extends Obstacle{
	
	private Vector2 limitPosition;
	private float powerX = 0, powerY = 0;

	public Spring(World world, Camera camera, MapObject rectangleObject, Couleurs couleurs) {
		super(world, camera, rectangleObject, couleurs);
		
		couleur = couleurs.getCouleurExit();
		body.setUserData("Spring");
		body.getFixtureList().get(0).setUserData("Spring");

		//initialPosition = new Vector2(body.getPosition());
		//limitPosition = new Vector2(body.getPosition().x + 0.75f * width, body.getPosition().y + 0.75f * height);
		limitPosition = new Vector2(0.75f * width, 0.75f * height);
		
		//Power
		if(rectangleObject.getProperties().get("PowerX") != null){
			powerX = Float.parseFloat(rectangleObject.getProperties().get("PowerX").toString());
		}
		if(rectangleObject.getProperties().get("PowerY") != null){
			powerY = Float.parseFloat(rectangleObject.getProperties().get("PowerY").toString());
		}
		/*
		if(powerX < 0)
			limitPosition.x = body.getPosition().x - 0.75f * width;
		if(powerY < 0)
			limitPosition.y = body.getPosition().y - 0.75f * height;
		*/
	}
	
	@Override
	public BodyType getBodyType(){
		return BodyType.KinematicBody;
	}
	
	@Override
	public Color getCouleur(){
		return couleurs.getCouleurExit();
	}
	
	@Override
	public void actif(){
		body.setLinearVelocity(powerX, powerY);
	}
	
	@Override
	public void activity(){
		if(Math.abs(body.getPosition().x - initialPosition.x) >= limitPosition.x){
			initiate();
		}
		if(Math.abs(body.getPosition().y - initialPosition.y) >= limitPosition.y){
			initiate();
		}
	}
	
	@Override
	public void initiate(){
		body.setLinearVelocity(0, 0);
		body.setAngularVelocity(0);
		body.setTransform(initialPosition, angle);
	}

}
