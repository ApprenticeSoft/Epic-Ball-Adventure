package bodies;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.one.button.jam.Couleurs;
import utils.SpringLightGeometry;
import utils.SpringMotion;

public class Spring extends Obstacle{

	private Vector2 limitPosition;
	private final Vector2 activeVelocity = new Vector2();
	private final Vector2 localDisplacement = new Vector2();
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
		body.setLinearVelocity(SpringMotion.worldVelocity(powerX, powerY, body.getAngle(), activeVelocity));
	}

	public float getPowerX(){
		return powerX;
	}

	public float getPowerY(){
		return powerY;
	}

	public float lightSourceWidth(){
		return SpringLightGeometry.bodyLocalSourceWidth(width, height, powerX, powerY);
	}

	public float lightSourceDepth(){
		return SpringLightGeometry.bodyLocalSourceDepth(width, height, powerX, powerY);
	}

	@Override
	public void activity(){
		SpringMotion.localDisplacement(body.getPosition(), initialPosition, body.getAngle(), localDisplacement);
		if(Math.abs(localDisplacement.x) >= limitPosition.x){
			initiate();
		}
		if(Math.abs(localDisplacement.y) >= limitPosition.y){
			initiate();
		}
	}

	@Override
	public void initiate(){
		resetBodyToInitial();
	}

}
