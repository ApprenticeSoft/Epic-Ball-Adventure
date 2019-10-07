package bodies;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.joints.RevoluteJoint;
import com.badlogic.gdx.physics.box2d.joints.RevoluteJointDef;
import com.one.button.jam.Couleurs;

public class ObstacleBalance extends Obstacle{

	private Body bodyAttache;
	private PolygonShape attacheShape;
	public RevoluteJoint revoluteJoint;
	private float speed = 1f;
	private float torque = 1f;
	private float attacheX = 0;
	private float attacheY = 0;
	private float angleRef = 0;
	private float angleMin, angleMax;
	private boolean limite = false;
	
	public ObstacleBalance(World world, Camera camera, MapObject rectangleObject, Couleurs couleurs) {
		super(world, camera, rectangleObject, couleurs);
		
		//Couleur
		couleur = couleurs.getCouleurLéger();
		
		//Position du point d'attache
		if(width >= height){		
			if(rectangleObject.getProperties().get("Position") != null)
				attacheX = width * Float.parseFloat(rectangleObject.getProperties().get("Position").toString());
			else
				attacheX = width;
		}
		else{	
			if(rectangleObject.getProperties().get("Position") != null)
				attacheY = height * Float.parseFloat(rectangleObject.getProperties().get("Position").toString());
			else
				attacheY = height;
		}	
		
		//Angles de référence, Min et Max
		if(rectangleObject.getProperties().get("angleRef") != null){
			angleRef = Float.parseFloat(rectangleObject.getProperties().get("angleRef").toString()) * MathUtils.degreesToRadians;
			limite = true;
		}
		if(rectangleObject.getProperties().get("angleMin") != null){
			angleMin = Float.parseFloat(rectangleObject.getProperties().get("angleMin").toString()) * MathUtils.degreesToRadians;
			limite = true;
		}
		if(rectangleObject.getProperties().get("angleMax") != null){
			angleMax = Float.parseFloat(rectangleObject.getProperties().get("angleMax").toString()) * MathUtils.degreesToRadians;
			limite = true;	
		}
		//Masse
		if(rectangleObject.getProperties().get("Weight") != null){
			body.getFixtureList().get(0).setDensity(
					body.getFixtureList().get(0).getDensity() * Float.parseFloat(rectangleObject.getProperties().get("Weight").toString())
			);
			body.resetMassData();
		}
		else{
			body.getFixtureList().get(0).setDensity(
					body.getFixtureList().get(0).getDensity() * 40
			);
			body.resetMassData();
		}
			
		//Collision
		if(rectangleObject.getProperties().get("Contact") != null)
			if(rectangleObject.getProperties().get("Contact").toString().equals("oui"))
				body.setUserData("Contact");
		//Vitesse et force
		if(rectangleObject.getProperties().get("Speed") != null)
			speed = Float.parseFloat(rectangleObject.getProperties().get("Speed").toString());
		if(rectangleObject.getProperties().get("Torque") != null)
			torque = Float.parseFloat(rectangleObject.getProperties().get("Torque").toString());
		
		//Création du point d'attache
		bodyDef.type = BodyType.StaticBody;	
		bodyDef.position.x = bodyDef.position.x + attacheX;
		bodyDef.position.y = bodyDef.position.y + attacheY;
		
		attacheShape = new PolygonShape();
		attacheShape.setAsBox(0.1f, 0.1f); 	
		fixtureDef.shape = attacheShape;
        fixtureDef.density = 0.0f;  
        fixtureDef.friction = 1.0f; 
        fixtureDef.restitution = 0.0f; 
        fixtureDef.isSensor = true;
		
		bodyAttache = world.createBody(bodyDef);
		bodyAttache.createFixture(fixtureDef).setUserData("Attache");;
		
		//Création du joint
		RevoluteJointDef rjDef = new RevoluteJointDef();
		rjDef.bodyA = body;
		rjDef.bodyB = bodyAttache;
		rjDef.collideConnected = false;
		rjDef.localAnchorA.set(attacheX, attacheY);
		rjDef.enableMotor = true;
		rjDef.motorSpeed = speed;
		rjDef.maxMotorTorque = torque;
		rjDef.enableLimit = limite;
		rjDef.lowerAngle = angleMin;
		rjDef.upperAngle = angleMax;
		rjDef.referenceAngle = angleRef;
		
		revoluteJoint = (RevoluteJoint) world.createJoint(rjDef);
		attacheShape.dispose();
		
		System.out.println("bodyAttache.getPosition() = " + bodyAttache.getPosition().toString());
		System.out.println("body.getPosition() = " + body.getPosition().toString());

		System.out.println("torque = " + torque);
		System.out.println("speed = " + speed);
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
	public void actif(){
	}
	
	@Override
	public void initiate(){
		body.setLinearVelocity(0, 0);
		body.setAngularVelocity(0);
		body.setTransform(initialPosition, angle);
	}
}
