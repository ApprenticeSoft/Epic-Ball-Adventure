package bodies;

import utils.Variables;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.joints.PulleyJoint;
import com.badlogic.gdx.physics.box2d.joints.PulleyJointDef;
import com.one.button.jam.Couleurs;

public class ObstaclePoulie extends Obstacle{
	private Body body2;
	private PolygonShape shape;
	private PulleyJoint pulleyJoint;
	private float épaisseurCorde = 0.2f;
	private float longueur1, longueur2;
	private float width2, height2;
	
	public ObstaclePoulie(World world, Camera camera, MapObject rectangleObject, Couleurs couleurs, MapObject rectangleObject2) {
		super(world, camera, rectangleObject, couleurs);
		body.setUserData("Contact");
		
		//Couleur
		couleur = couleurs.getCouleurLéger();

		Rectangle rectangle2 = ((RectangleMapObject) rectangleObject2).getRectangle();
		
		//Création du deuxième body
		width2 = (rectangle2.width/2) * Variables.WORLD_TO_BOX;
		height2 = (rectangle2.height/2) * Variables.WORLD_TO_BOX;
		
		shape = new PolygonShape();
		shape.setAsBox(width2, height2);

		bodyDef.position.set(new Vector2((rectangle2.x + rectangle2.width/2) * Variables.WORLD_TO_BOX, (rectangle2.y + rectangle2.height/2) * Variables.WORLD_TO_BOX));
		
		fixtureDef = new FixtureDef();
		fixtureDef.shape = shape;
        fixtureDef.density = (float)(Variables.DENSITÉ/(5 * width2 * height2));  
        fixtureDef.friction = 0.5f;  
        fixtureDef.restitution = 0.5f;
   
        body2 = world.createBody(bodyDef);
        body2.createFixture(fixtureDef).setUserData("Objet");
        body2.setUserData("Contact");	
        
        shape.dispose();
		
		//Masse
		if(rectangleObject.getProperties().get("Masse") != null){
			body.getFixtureList().get(0).setDensity(
					body.getFixtureList().get(0).getDensity() * Float.parseFloat(rectangleObject.getProperties().get("Masse").toString())
			);
			body.resetMassData();
		}
		else{
			body.getFixtureList().get(0).setDensity(
					body.getFixtureList().get(0).getDensity() * 50
			);
			body.resetMassData();
		}
		if(rectangleObject2.getProperties().get("Masse") != null){
			body2.getFixtureList().get(0).setDensity(
					body2.getFixtureList().get(0).getDensity() * Float.parseFloat(rectangleObject2.getProperties().get("Masse").toString())
			);
			body2.resetMassData();
		}
		else{
			body2.getFixtureList().get(0).setDensity(
					(float)(Variables.DENSITÉ/(5 * width2 * height2)) * 50
			);
			body2.resetMassData();
		}
		
		//Longueur des cordes
		if(rectangleObject.getProperties().get("longueur") != null)
			longueur1 = Float.parseFloat(rectangleObject.getProperties().get("longueur").toString()) * Variables.PPT * Variables.WORLD_TO_BOX;
		else
			longueur1 = 5 * Variables.PPT * Variables.WORLD_TO_BOX;
		
		if(rectangleObject2.getProperties().get("longueur") != null)
			longueur2 = Float.parseFloat(rectangleObject2.getProperties().get("longueur").toString()) * Variables.PPT * Variables.WORLD_TO_BOX;
		else
			longueur2 = 5 * Variables.PPT * Variables.WORLD_TO_BOX;
		
        //Création du PulleyJoint
		PulleyJointDef pjDef = new PulleyJointDef();
		pjDef.bodyA = body;
		pjDef.bodyB = body2;
		pjDef.groundAnchorA.set(body.getPosition().x, body.getPosition().y + longueur1);
		pjDef.localAnchorA.set(0,0);
		pjDef.lengthA = longueur1;
		pjDef.groundAnchorB.set(body2.getPosition().x, body2.getPosition().y + longueur2);
		pjDef.lengthB = longueur2;
		pjDef.localAnchorB.set(0,0);
			
		pulleyJoint = (PulleyJoint) world.createJoint(pjDef);
		
		body.setFixedRotation(true);
		body2.setFixedRotation(true);		
	}
	
	@Override
	public void draw(SpriteBatch batch, TextureAtlas textureAtlas){
		//Dessin de la corde gauche
		batch.setColor(0,0,0,1);
		batch.draw(textureAtlas.findRegion("Barre"), 
					(pulleyJoint.getAnchorA().x + pulleyJoint.getGroundAnchorA().x - épaisseurCorde)/2,
					(pulleyJoint.getAnchorA().y + pulleyJoint.getGroundAnchorA().y - pulleyJoint.getGroundAnchorA().sub(pulleyJoint.getAnchorA()).len())/2,
					épaisseurCorde/2,																		//Origine X (pour la rotation)
					pulleyJoint.getAnchorA().sub(pulleyJoint.getGroundAnchorA()).len()/2,					//Origine Y (pour la rotation)
					épaisseurCorde,																			//Largeur de la corde
					pulleyJoint.getAnchorA().sub(pulleyJoint.getGroundAnchorA()).len(), 					//Longueur de la corde
					1,
					1,
					pulleyJoint.getAnchorA().sub(pulleyJoint.getGroundAnchorA()).angle() + 90
					);
		
		//Dessin de la corde droite
		batch.draw(textureAtlas.findRegion("Barre"), 
				(pulleyJoint.getAnchorB().x + pulleyJoint.getGroundAnchorB().x - épaisseurCorde)/2,
				(pulleyJoint.getAnchorB().y + pulleyJoint.getGroundAnchorB().y - pulleyJoint.getGroundAnchorB().sub(pulleyJoint.getAnchorB()).len())/2,
				épaisseurCorde/2,																		//Origine X (pour la rotation)
				pulleyJoint.getAnchorB().sub(pulleyJoint.getGroundAnchorB()).len()/2,					//Origine Y (pour la rotation)
				épaisseurCorde,																			//Largeur de la corde
				pulleyJoint.getAnchorB().sub(pulleyJoint.getGroundAnchorB()).len(), 					//Longueur de la corde
				1,
				1,
				pulleyJoint.getAnchorB().sub(pulleyJoint.getGroundAnchorB()).angle() + 90
				);
		
		//Dessin de la corde transversalle
		batch.draw(textureAtlas.findRegion("Barre"), 
				(pulleyJoint.getGroundAnchorA().x + pulleyJoint.getGroundAnchorB().x - épaisseurCorde)/2,
				(pulleyJoint.getGroundAnchorA().y + pulleyJoint.getGroundAnchorB().y - pulleyJoint.getGroundAnchorB().sub(pulleyJoint.getGroundAnchorA()).len())/2,
				épaisseurCorde/2,																		//Origine X (pour la rotation)
				pulleyJoint.getGroundAnchorA().sub(pulleyJoint.getGroundAnchorB()).len()/2,				//Origine Y (pour la rotation)
				épaisseurCorde,																			//Largeur de la corde
				pulleyJoint.getGroundAnchorA().sub(pulleyJoint.getGroundAnchorB()).len(), 				//Longueur de la corde
				1,
				1,
				pulleyJoint.getGroundAnchorA().sub(pulleyJoint.getGroundAnchorB()).angle() + 90
				);
		
		//Dessin du plateau gauche
		batch.setColor(getCouleur());
		batch.draw(textureAtlas.findRegion("Barre"), 
				this.body.getPosition().x - width, 
				this.body.getPosition().y - height,
				width,
				height,
				2 * width,
				2 * height,
				1,
				1,
				body.getAngle()*MathUtils.radiansToDegrees);
		
		//Dessin du plateau droite
		batch.draw(textureAtlas.findRegion("Barre"), 
				this.body2.getPosition().x - width2, 
				this.body2.getPosition().y - height2,
				width2,
				height2,
				2 * width2,
				2 * height2,
				1,
				1,
				body2.getAngle()*MathUtils.radiansToDegrees);
	}
	
	@Override
	public void drawOmbre(SpriteBatch batch, TextureAtlas textureAtlas){
		//Dessin de la corde gauche
		batch.setColor(0,0,0,1);
		batch.draw(textureAtlas.findRegion("Barre"), 
					(pulleyJoint.getAnchorA().x + pulleyJoint.getGroundAnchorA().x - épaisseurCorde)/2 + Variables.ombresX,
					(pulleyJoint.getAnchorA().y + pulleyJoint.getGroundAnchorA().y - pulleyJoint.getGroundAnchorA().sub(pulleyJoint.getAnchorA()).len())/2 + Variables.ombresY,
					épaisseurCorde/2,																		//Origine X (pour la rotation)
					pulleyJoint.getAnchorA().sub(pulleyJoint.getGroundAnchorA()).len()/2,					//Origine Y (pour la rotation)
					épaisseurCorde,																			//Largeur de la corde
					pulleyJoint.getAnchorA().sub(pulleyJoint.getGroundAnchorA()).len(), 					//Longueur de la corde
					1,
					1,
					pulleyJoint.getAnchorA().sub(pulleyJoint.getGroundAnchorA()).angle() + 90
					);
		
		//Dessin de la corde droite
		batch.draw(textureAtlas.findRegion("Barre"), 
				(pulleyJoint.getAnchorB().x + pulleyJoint.getGroundAnchorB().x - épaisseurCorde)/2 + Variables.ombresX,
				(pulleyJoint.getAnchorB().y + pulleyJoint.getGroundAnchorB().y - pulleyJoint.getGroundAnchorB().sub(pulleyJoint.getAnchorB()).len())/2 + Variables.ombresY,
				épaisseurCorde/2,																		//Origine X (pour la rotation)
				pulleyJoint.getAnchorB().sub(pulleyJoint.getGroundAnchorB()).len()/2,					//Origine Y (pour la rotation)
				épaisseurCorde,																			//Largeur de la corde
				pulleyJoint.getAnchorB().sub(pulleyJoint.getGroundAnchorB()).len(), 					//Longueur de la corde
				1,
				1,
				pulleyJoint.getAnchorB().sub(pulleyJoint.getGroundAnchorB()).angle() + 90
				);
		
		//Dessin de la corde transversalle
		batch.draw(textureAtlas.findRegion("Barre"), 
				(pulleyJoint.getGroundAnchorA().x + pulleyJoint.getGroundAnchorB().x - épaisseurCorde)/2 + Variables.ombresX,
				(pulleyJoint.getGroundAnchorA().y + pulleyJoint.getGroundAnchorB().y - pulleyJoint.getGroundAnchorB().sub(pulleyJoint.getGroundAnchorA()).len())/2 + Variables.ombresY,
				épaisseurCorde/2,																		//Origine X (pour la rotation)
				pulleyJoint.getGroundAnchorA().sub(pulleyJoint.getGroundAnchorB()).len()/2,				//Origine Y (pour la rotation)
				épaisseurCorde,																			//Largeur de la corde
				pulleyJoint.getGroundAnchorA().sub(pulleyJoint.getGroundAnchorB()).len(), 				//Longueur de la corde
				1,
				1,
				pulleyJoint.getGroundAnchorA().sub(pulleyJoint.getGroundAnchorB()).angle() + 90
				);
		
		//Dessin du plateau gauche
		batch.setColor(getCouleur());
		batch.draw(textureAtlas.findRegion("Barre"), 
				this.body.getPosition().x - width + Variables.ombresX, 
				this.body.getPosition().y - height + Variables.ombresY,
				width,
				height,
				2 * width,
				2 * height,
				1,
				1,
				body.getAngle()*MathUtils.radiansToDegrees);
		
		//Dessin du plateau droite
		batch.draw(textureAtlas.findRegion("Barre"), 
				this.body2.getPosition().x - width2 + Variables.ombresX, 
				this.body2.getPosition().y - height2 + Variables.ombresY,
				width2,
				height2,
				2 * width2,
				2 * height2,
				1,
				1,
				body2.getAngle()*MathUtils.radiansToDegrees);
	}
	
	@Override
	public BodyType getBodyType(){
		return BodyType.DynamicBody;
	}
	
	@Override
	public Color getCouleur(){
		return couleurs.getCouleurLéger();
	}

}
