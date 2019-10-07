package bodies;

import utils.Variables;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.joints.RopeJoint;
import com.badlogic.gdx.physics.box2d.joints.RopeJointDef;
import com.one.button.jam.Couleurs;

public class ObstacleSuspendu extends Obstacle{
	
	private Body bodyAttache;
	private PolygonShape attacheShape;
	public RopeJoint ropeJoint;
	private float épaisseurCorde = 0.2f;
	private float longueurCorde = 5 * Variables.PPT * Variables.WORLD_TO_BOX;
	private float attacheY;
	
	public ObstacleSuspendu(World world, Camera camera,	MapObject rectangleObject, Couleurs couleurs) {
		super(world, camera, rectangleObject, couleurs);
		
		body.setUserData("Contact");
		
		//Couleur
		couleur = couleurs.getCouleurLéger();
		
		//Longeur de la corde
		if(rectangleObject.getProperties().get("Length") != null){
			longueurCorde = Float.parseFloat(rectangleObject.getProperties().get("Length").toString()) * Variables.PPT * Variables.WORLD_TO_BOX;
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
					body.getFixtureList().get(0).getDensity() * 5
			);
			body.resetMassData();
		}
		
		//Position du point d'attache
		if(rectangleObject.getProperties().get("Position") != null)
			attacheY = height * Float.parseFloat(rectangleObject.getProperties().get("Position").toString());
		else
			attacheY = height;
		
		//Création du point d'attache
		bodyDef.type = BodyType.StaticBody;	
		bodyDef.position.y = bodyDef.position.y + longueurCorde;
		
		attacheShape = new PolygonShape();
		attacheShape.setAsBox(0.1f, 0.1f); 	
		fixtureDef.shape = attacheShape;
        fixtureDef.density = 0.0f;  
        fixtureDef.friction = 1.0f;  
        fixtureDef.restitution = 0.0f; 
        fixtureDef.isSensor = true;
		
		bodyAttache = world.createBody(bodyDef);
		bodyAttache.createFixture(fixtureDef).setUserData("Attache");
				
		//Création du Ropejoint
		RopeJointDef ropejDef = new RopeJointDef();
		ropejDef.bodyA = body;
		ropejDef.bodyB = bodyAttache;
		ropejDef.collideConnected = false;
		ropejDef.localAnchorA.set(0, attacheY);
		ropejDef.localAnchorB.set(0, 0);
		
		ropeJoint = (RopeJoint) world.createJoint(ropejDef);
		ropeJoint.setMaxLength(ropeJoint.getAnchorB().sub(ropeJoint.getAnchorA()).len());
		
		attacheShape.dispose();
	}
	
	@Override
	public BodyType getBodyType(){
		return BodyType.DynamicBody;
	}
	
	@Override
	public void draw(SpriteBatch batch, TextureAtlas textureAtlas){
		//Dessin de la corde
		batch.setColor(0,0,0,1);
		batch.draw(textureAtlas.findRegion("WhiteSquare"), 
				(ropeJoint.getAnchorB().x + ropeJoint.getAnchorA().x - épaisseurCorde)/2,
				(ropeJoint.getAnchorB().y + ropeJoint.getAnchorA().y - ropeJoint.getAnchorB().sub(ropeJoint.getAnchorA()).len())/2,
				épaisseurCorde/2,																	//Origine X (pour la rotation)
				ropeJoint.getAnchorB().sub(ropeJoint.getAnchorA()).len()/2,							//Origine Y (pour la rotation)
				épaisseurCorde,																		//Largeur de la corde
				ropeJoint.getAnchorB().sub(ropeJoint.getAnchorA()).len(), 							//Longueur de la corde
				1,
				1,
				ropeJoint.getAnchorB().sub(ropeJoint.getAnchorA()).angle() + 90
				);
		
		//Dessin de la balançoire
		batch.setColor(couleur);
		batch.draw(textureAtlas.findRegion("WhiteSquare"), 
				this.body.getPosition().x - width, 
				this.body.getPosition().y - height,
				width,
				height,
				2 * width,
				2 * height,
				1,
				1,
				body.getAngle()*MathUtils.radiansToDegrees);
	}
	
	
	public void drawOmbre(SpriteBatch batch, TextureAtlas textureAtlas){
		super.drawOmbre(batch, textureAtlas);
		//Dessin de la corde
		batch.draw(textureAtlas.findRegion("WhiteSquare"), 
				(ropeJoint.getAnchorB().x + ropeJoint.getAnchorA().x - épaisseurCorde)/2 + Variables.ombresX,
				(ropeJoint.getAnchorB().y + ropeJoint.getAnchorA().y - ropeJoint.getAnchorB().sub(ropeJoint.getAnchorA()).len())/2 + Variables.ombresY,
				épaisseurCorde/2,																				
				ropeJoint.getAnchorB().sub(ropeJoint.getAnchorA()).len()/2,							
				épaisseurCorde,																					
				ropeJoint.getAnchorB().sub(ropeJoint.getAnchorA()).len(), 							
				1,
				1,
				ropeJoint.getAnchorB().sub(ropeJoint.getAnchorA()).angle() + 90
				);
	}

}
